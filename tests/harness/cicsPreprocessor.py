#!/usr/bin/env python3
"""cicsPreprocessor.py

Lightweight EXEC CICS / EXEC SQL -> CALL translator for the CBSA
isolated-test harness.

GnuCOBOL (cobc) does not understand IBM's ``EXEC CICS`` / ``EXEC SQL``
verbs. Rather than run the full z/OS CICS+Db2 translator, this
preprocessor rewrites the *specific* verbs used by the CBSA programs
under test into plain ``CALL`` statements that target small COBOL stub
modules (see ``tests/harness/shims``). The original ``.cbl`` sources are
never modified; the translated copy is written to stdout and compiled
from the build directory, so the very same sources still build unchanged
for the mainframe.

Design goals:
  * Fixed-format friendly. Every in-place substitution (e.g.
    ``DFHRESP(...)``) is padded to the exact width of the text it
    replaces so that no column positions shift. Whole
    ``EXEC ... END-EXEC`` blocks are replaced by freshly generated lines
    that we control, kept within column 72.
  * Minimal + explicit. Only the verbs these programs actually use are
    handled; an unknown verb raises so the gap is obvious instead of
    being silently faked.

Supported EXEC CICS verbs (see README for how to add more):
  RETURN, DELAY, GET CONTAINER, PUT CONTAINER, LINK,
  ASSIGN (APPLID/PROGRAM/ABCODE), ABEND, ASKTIME, FORMATTIME,
  HANDLE (ABEND), SYNCPOINT, and the VSAM/KSDS file verbs READ,
  READ UPDATE, REWRITE, WRITE, STARTBR, READNEXT, READPREV, ENDBR
  (all routed to the CICSVSAM in-memory KSDS double).

Supported EXEC SQL statements (Db2 ACCOUNT table double, see DB2ACC):
  INCLUDE SQLCA / INCLUDE <table copybook>, DECLARE CURSOR,
  SELECT .. INTO (keyed and "ORDER BY .. DESC FETCH FIRST 1"),
  UPDATE, OPEN, FETCH, CLOSE.

Usage:
  python3 cicsPreprocessor.py <path-to-source.cbl>  > translated.cbl
"""

import re
import sys

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# DFHRESP(name) -> numeric condition value. Only NORMAL is used by the
# programs under test; extend this map as new programs are added.
DFHRESP_MAP = {
    "NORMAL": 0,
    "NOTFND": 13,
    "INVREQ": 16,
    "DUPREC": 14,
    "DUPKEY": 15,
    "ENDFILE": 20,
    "SYSIDERR": 53,
    "NOTFINISHED": 82,
}

# DFHVALUE(name) -> numeric CVDA value. Used by CRECUST's FETCH ANY
# COMPSTATUS EVALUATE. The absolute values are irrelevant off-CICS as long
# as they are internally consistent with what the CICSASYN double sets, so
# NORMAL is 0 (CICSASYN returns 0 for a good completion) and the rest are
# distinct sentinels.
DFHVALUE_MAP = {
    "NORMAL": 0,
    "ABEND": 1,
    "SECERROR": 2,
}

# EXEC CICS file-control verbs routed to the CICSVSAM KSDS test double.
VSAM_FILE_VERBS = (
    "READ", "REWRITE", "WRITE", "DELETE",
    "STARTBR", "READNEXT", "READPREV", "ENDBR",
)

# Indentation (Area B, column 12) for generated statements.
INDENT = " " * 11
CONT = " " * 16  # continuation indent for USING operands

# Column at which Area A/B code begins in fixed-format (1-based col 8 -> idx 7).
CODE_START = 7
# Right margin of the source area in fixed-format COBOL.
MARGIN_R = 72

# The programs under test hold their 12 Db2 ACCOUNT host variables in a
# group with this name; the SQL translation passes that group as the row
# buffer to DB2ACC. New programs added to the SQL harness must follow the
# same convention (see tests/harness/README.md).
ROW_GROUP = "HOST-ACCOUNT-ROW"

# The three key host variables the DB2ACC calling convention passes. They
# exist (with these names/widths) in every program that uses the ACCOUNT
# SQL double.
KEY_SORTCODE = "HV-ACCOUNT-SORTCODE"
KEY_ACCNO = "HV-ACCOUNT-ACC-NO"
KEY_CUSTNO = "HV-ACCOUNT-CUST-NO"

# The DB2ACC / DB2PROC calling convention passes the program's whole SQL
# communications area (the SQLCA group) as the final BY REFERENCE operand,
# so the double can set both SQLCODE and, for the XFRFUN -911 deadlock
# path, SQLERRD(3). Every program that uses the SQL doubles INCLUDEs SQLCA.
SQLCA_FIELD = "SQLCA"

# PROCTRAN audit-table programs hold their nine Db2 PROCTRAN host variables
# in a group with this name; the INSERT translation passes it to DB2PROC.
PROC_ROW_GROUP = "HOST-PROCTRAN-ROW"


def _operands(text):
    """Return an ordered dict-like list of ``KEYWORD(value)`` operands."""
    pairs = re.findall(r"([A-Za-z0-9]+)\s*\(([^)]*)\)", text)
    return {k.upper(): v.strip() for k, v in pairs}


def _call(module, refs=None, contents=None, period=False):
    """Build CALL lines. ``contents`` are passed BY CONTENT, ``refs`` BY REFERENCE."""
    lines = []
    head = "{}CALL '{}' USING".format(INDENT, module)
    parts = []
    for c in contents or []:
        parts.append(("CONTENT", c))
    for r in refs or []:
        parts.append(("REFERENCE", r))
    # Emit head, then one operand per continuation line (keeps within col 72).
    lines.append(head)
    for mode, val in parts:
        lines.append("{}BY {} {}".format(CONT, mode, val))
    if period:
        lines[-1] = lines[-1] + "."
    return lines


def _vsam_call(verb, body, ops, period):
    """Route an EXEC CICS file-control verb to the CICSVSAM KSDS double.

    Uniform signature (positional):
        CALL 'CICSVSAM' USING BY CONTENT op(8) file(8)
             BY REFERENCE ridfld|OMITTED record|OMITTED resp resp2
    Operands a given verb does not carry (e.g. RIDFLD on REWRITE/ENDBR, or
    the record on STARTBR/ENDBR) are passed OMITTED; the shim only touches
    the ones relevant to that op.
    """
    op = verb
    if verb == "READ" and re.search(r"\bUPDATE\b", body.upper()):
        op = "RDUPD"
    fileLit = ops.get("FILE", "'CUSTOMER'")
    rid = ops.get("RIDFLD", "OMITTED")
    if verb in ("READ", "READNEXT", "READPREV"):
        rec = ops.get("INTO", "OMITTED")
    elif verb in ("REWRITE", "WRITE"):
        rec = ops.get("FROM", "OMITTED")
    else:
        rec = "OMITTED"
    return _call("CICSVSAM",
                 contents=["'{}'".format(op.ljust(8)), fileLit],
                 refs=[rid, rec,
                       ops.get("RESP", "OMITTED"),
                       ops.get("RESP2", "OMITTED")],
                 period=period)


def _db2_call(op, period):
    """Build a CALL to the DB2ACC (ACCOUNT table) double.

    Fixed positional signature:
        CALL 'DB2ACC' USING BY CONTENT op(8)
             BY REFERENCE sortcode accno custno HOST-ACCOUNT-ROW SQLCA
    """
    return _call(
        "DB2ACC",
        contents=["'{}'".format(op.ljust(8))],
        refs=[KEY_SORTCODE, KEY_ACCNO, KEY_CUSTNO, ROW_GROUP, SQLCA_FIELD],
        period=period,
    )


def _db2proc_call(op, period):
    """Build a CALL to the DB2PROC (PROCTRAN audit-table) double.

    Fixed positional signature:
        CALL 'DB2PROC' USING BY CONTENT op(8)
             BY REFERENCE HOST-PROCTRAN-ROW SQLCA
    """
    return _call(
        "DB2PROC",
        contents=["'{}'".format(op.ljust(8))],
        refs=[PROC_ROW_GROUP, SQLCA_FIELD],
        period=period,
    )


def _translate_block(text, period):
    """Translate a single flattened ``EXEC CICS ...`` block into COBOL lines."""
    body = text.split("CICS", 1)[1].strip()
    verb = body.split("(", 1)[0].split()[0].upper()
    ops = _operands(body)

    if verb in VSAM_FILE_VERBS:
        return _vsam_call(verb, body, ops, period)

    if verb == "HANDLE":
        # HANDLE ABEND / HANDLE CONDITION: no equivalent off-CICS. The error
        # paths these guard are not exercised by the unit tests, so disable
        # the handler (a no-op) instead of faking a branch.
        return ["{}CONTINUE{}".format(INDENT, "." if period else "")]

    if verb == "SYNCPOINT":
        return _call("CICSSYNC",
                     refs=[ops["RESP"], ops["RESP2"]],
                     period=period)

    if verb == "RETURN":
        # RETURN ends the CICS task -> terminate the run unit.
        return ["{}GOBACK{}".format(INDENT, "." if period else "")]

    if verb == "DELAY":
        return _call("CICSDLAY",
                     refs=[ops["SECONDS"],
                           ops.get("RESP", "OMITTED"),
                           ops.get("RESP2", "OMITTED")],
                     period=period)

    if verb == "GET" and "CONTAINER" in ops:
        return _call("CICSCONT",
                     contents=["'GET'"],
                     refs=[ops["CONTAINER"], ops["CHANNEL"], ops["INTO"],
                           ops["FLENGTH"], ops["RESP"], ops["RESP2"]],
                     period=period)

    if verb == "PUT" and "CONTAINER" in ops:
        return _call("CICSCONT",
                     contents=["'PUT'"],
                     refs=[ops["CONTAINER"], ops["CHANNEL"], ops["FROM"],
                           ops["FLENGTH"], ops["RESP"], ops["RESP2"]],
                     period=period)

    if verb == "ENQ":
        # ENQ/DEQ serialise the CUSTOMER named counter. Off-CICS there is a
        # single run unit, so a no-op that always reports NORMAL is faithful.
        return _call("CICSENQ",
                     contents=["'ENQ'"],
                     refs=[ops.get("RESOURCE", "OMITTED"),
                           ops.get("RESP", "OMITTED"),
                           ops.get("RESP2", "OMITTED")],
                     period=period)

    if verb == "DEQ":
        return _call("CICSENQ",
                     contents=["'DEQ'"],
                     refs=[ops.get("RESOURCE", "OMITTED"),
                           ops.get("RESP", "OMITTED"),
                           ops.get("RESP2", "OMITTED")],
                     period=period)

    if verb == "RUN":
        # RUN TRANSID(..) CHANNEL(..) CHILD(..): the CICS Async API. Emulated
        # synchronously by CICSASYN (see the shim + README) - it records the
        # child token/channel and overlays the scripted credit score into the
        # channel container so the parent's later GET CONTAINER reads it. The
        # uniform positional signature is shared with FETCH ANY (operands a
        # given verb lacks are passed OMITTED):
        #   op(8) transid channel child anytkn compstatus abcode resp resp2
        return _call("CICSASYN",
                     contents=["'RUN     '"],
                     refs=[ops["TRANSID"], ops["CHANNEL"], ops["CHILD"],
                           "OMITTED", "OMITTED", "OMITTED",
                           ops.get("RESP", "OMITTED"),
                           ops.get("RESP2", "OMITTED")],
                     period=period)

    if verb == "FETCH" and "ANY" in ops:
        # FETCH ANY(..): hands back one completed child at a time, then
        # RESP=NOTFND/RESP2=1 once all scripted replies are consumed.
        return _call("CICSASYN",
                     contents=["'FETCH   '"],
                     refs=["OMITTED", ops["CHANNEL"], "OMITTED",
                           ops["ANY"], ops["COMPSTATUS"], ops["ABCODE"],
                           ops.get("RESP", "OMITTED"),
                           ops.get("RESP2", "OMITTED")],
                     period=period)

    if verb == "LINK":
        # PROGRAM may be a literal ('INQCUST ') or a data name; pass BY
        # CONTENT so both work. COMMAREA is a data name (BY REFERENCE) so
        # a stub can return values into it.
        return _call("CICSLINK",
                     contents=[ops["PROGRAM"]],
                     refs=[ops.get("COMMAREA", "OMITTED")],
                     period=period)

    if verb == "ASSIGN":
        # ASSIGN APPLID(x) / PROGRAM(x) / ABCODE(x): pass field name + receiver.
        for key in ("APPLID", "PROGRAM", "ABCODE"):
            if key in ops:
                return _call("CICSASGN",
                             contents=["'{}'".format(key.ljust(8))],
                             refs=[ops[key]],
                             period=period)
        raise ValueError("Unsupported EXEC CICS ASSIGN operands: {!r}".format(body))

    if verb == "ABEND":
        # EXEC CICS ABEND terminates the task. Record the abend code in the
        # resident CICSABND double (so a driver can read it back) and then
        # GOBACK so control does not fall through into code the real abend
        # would never reach.
        lines = _call("CICSABND",
                      contents=["'ABEND   '", ops["ABCODE"]],
                      period=False)
        lines.append("{}GOBACK{}".format(INDENT, "." if period else ""))
        return lines

    if verb == "ASKTIME":
        return _call("CICSTIME", refs=[ops["ABSTIME"]], period=period)

    if verb == "FORMATTIME":
        return _call("CICSFTIM",
                     refs=[ops["ABSTIME"], ops["DDMMYYYY"], ops["TIME"]],
                     period=period)

    raise ValueError("Unsupported EXEC CICS verb: {!r}".format(verb))


def _comment(text):
    """Return a fixed-format comment line carrying ``text`` (kept < col 72)."""
    return ("      *" + " HARNESS: " + text)[:MARGIN_R]


def _translate_sql_block(text, period, ctx):
    """Translate a single flattened ``EXEC SQL ...`` block into COBOL lines."""
    body = text.split("SQL", 1)[1].strip()
    upper = body.upper()
    verb = upper.split()[0]

    if verb == "INCLUDE":
        member = upper.split()[1]
        if member == "SQLCA":
            # The SQL communications area -> harness copybook.
            return ["{}COPY SQLCA.".format(INDENT)]
        # A DECLARE TABLE copybook (e.g. ACCDB2): purely declarative for
        # the Db2 precompiler, no runtime effect off-Db2 -> drop it.
        return [_comment("dropped EXEC SQL INCLUDE {}".format(member))]

    if verb == "DECLARE" and "CURSOR" in upper:
        # Cursor declaration: no runtime action, but capture the WHERE
        # predicate columns so OPEN knows which filter to apply.
        where = upper.split("WHERE", 1)[1] if "WHERE" in upper else ""
        where = where.split("FOR FETCH", 1)[0]
        ctx["cursor_mode"] = "C" if "CUSTOMER_NUMBER" in where else "A"
        return [_comment("DECLARE CURSOR captured (mode {})".format(
            ctx["cursor_mode"]))]

    if verb == "SELECT" and "INTO" in upper:
        # Two shapes: a keyed single-row SELECT, or the "last account"
        # SELECT (ORDER BY ACCOUNT_NUMBER DESC FETCH FIRST 1 ROW).
        if "ORDER BY" in upper and "DESC" in upper:
            return _db2_call("SELMAX", period)
        return _db2_call("SELKEY", period)

    if verb == "UPDATE":
        return _db2_call("UPDATE", period)

    if verb == "DELETE":
        # DELETE FROM ACCOUNT (keyed) -> remove the row from the DB2ACC double.
        return _db2_call("DELKEY", period)

    if verb == "INSERT":
        # INSERT INTO PROCTRAN -> append an audit row to the DB2PROC double.
        if "PROCTRAN" in upper:
            return _db2proc_call("INSERT", period)
        raise ValueError("Unsupported EXEC SQL INSERT target: {!r}".format(body))

    if verb == "OPEN":
        op = "OPENC" if ctx.get("cursor_mode") == "C" else "OPENA"
        return _db2_call(op, period)

    if verb == "FETCH":
        return _db2_call("FETCH", period)

    if verb == "CLOSE":
        return _db2_call("CLOSE", period)

    raise ValueError("Unsupported EXEC SQL statement: {!r}".format(body))


def _replace_dfhresp(line):
    """Replace DFHRESP(NAME) with its numeric value, padded to equal width."""
    def repl(m):
        name = m.group(1).upper()
        val = DFHRESP_MAP.get(name, 0)
        return str(val).ljust(len(m.group(0)))
    return re.sub(r"DFHRESP\(([A-Za-z0-9]+)\)", repl, line)


def _replace_dfhvalue(line):
    """Replace DFHVALUE(NAME) with its numeric value, padded to equal width."""
    def repl(m):
        name = m.group(1).upper()
        val = DFHVALUE_MAP.get(name, 0)
        return str(val).ljust(len(m.group(0)))
    return re.sub(r"DFHVALUE\(([A-Za-z0-9]+)\)", repl, line)


def _is_comment(line):
    return len(line) > CODE_START and line[CODE_START - 1] in ("*", "/")


def _code(line):
    """Return the Area A/B text (cols 8..72) of a fixed-format line."""
    if len(line) <= CODE_START:
        return ""
    return line[CODE_START:MARGIN_R]


def translate(lines):
    out = []
    ctx = {"cursor_mode": "A"}

    uses_eib = any(
        (not _is_comment(ln)) and re.search(r"\bEIB[A-Z0-9]*\b", _code(ln))
        for ln in lines
    )
    # A DFHCOMMAREA declared in LINKAGE is addressed automatically by the
    # real CICS translator. Off-CICS we must bind it as a PROCEDURE DIVISION
    # parameter so the driver's CALL ... USING commarea connects it.
    has_commarea = any(
        (not _is_comment(ln))
        and re.match(r"01\s+DFHCOMMAREA\b", _code(ln).strip().upper())
        for ln in lines
    )

    i = 0
    n = len(lines)
    while i < n:
        line = lines[i].rstrip("\n")

        if _is_comment(line):
            out.append(line)
            i += 1
            continue

        # Fixed-format debugging lines ('D'/'d' in the indicator area) are
        # ignored by the compiler unless WITH DEBUGGING MODE is active (it is
        # not here). Pass them through verbatim so we never try to translate
        # an EXEC embedded in a debugging line (e.g. CRECUST's START-DEQ one).
        if len(line) >= CODE_START and line[CODE_START - 1] in ("D", "d"):
            out.append(line)
            i += 1
            continue

        code = _code(line)
        stripped = code.strip()
        upper = stripped.upper()

        # Comment out compiler-directing statements (CBL / PROCESS).
        if upper.startswith("CBL ") or upper == "CBL" or upper.startswith("PROCESS"):
            out.append("      *" + line[CODE_START:])
            i += 1
            continue

        # Detect start of an EXEC CICS / EXEC SQL block.
        exec_match = re.match(r"EXEC\s+(CICS|SQL)\b", upper)
        if exec_match:
            block = [code]
            j = i
            while "END-EXEC" not in _code(lines[j]).upper():
                j += 1
                block.append(_code(lines[j]))
            # Does a period follow END-EXEC on the closing line?
            tail = _code(lines[j]).upper().split("END-EXEC", 1)[1]
            period = "." in tail
            flat = " ".join(seg.strip() for seg in block)
            flat = flat.split("END-EXEC", 1)[0]
            if exec_match.group(1) == "SQL":
                out.extend(_translate_sql_block(flat, period, ctx))
            else:
                out.extend(_translate_block(flat, period))
            i = j + 1
            continue

        # Insert EIB shim copybook right after WORKING-STORAGE SECTION.
        if uses_eib and re.match(r"WORKING-STORAGE\s+SECTION\.", upper):
            out.append(line)
            out.append("{}COPY DFHEIBLK.".format(INDENT))
            i += 1
            continue

        # PROCEDURE DIVISION: inject USING DFHCOMMAREA when needed, and the
        # EIB shim init when the program references the EIB.
        if re.match(r"PROCEDURE\s+DIVISION", upper):
            header = line
            if has_commarea and "USING" not in upper:
                fixed = _code(line).rstrip()
                if fixed.endswith("."):
                    fixed = fixed[:-1].rstrip() + " USING DFHCOMMAREA."
                else:
                    fixed = fixed + " USING DFHCOMMAREA"
                header = line[:CODE_START] + fixed
            out.append(header)
            if uses_eib:
                out.append("       CBSA-SHIM-INIT SECTION.")
                out.append("       CBSA-SHIM-INIT-P.")
                out.append("{}CALL 'CICSINIT' USING DFHEIB-SHIM.".format(INDENT))
            i += 1
            continue

        # Ordinary line: perform in-place, width-preserving substitutions.
        if "DFHRESP(" in code or "DFHVALUE(" in code:
            fixed = _replace_dfhvalue(_replace_dfhresp(code))
            line = line[:CODE_START] + fixed + line[MARGIN_R:]
        out.append(line)
        i += 1

    return out


def main(argv):
    if len(argv) != 2:
        sys.stderr.write("usage: cicsPreprocessor.py <source.cbl>\n")
        return 2
    with open(argv[1], "r") as fh:
        lines = fh.readlines()
    for ln in translate(lines):
        sys.stdout.write(ln.rstrip("\n") + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
