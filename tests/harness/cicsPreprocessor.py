#!/usr/bin/env python3
"""cicsPreprocessor.py

Lightweight EXEC CICS -> CALL translator for the CBSA isolated-test harness.

GnuCOBOL (cobc) does not understand IBM's ``EXEC CICS`` / ``EXEC SQL`` verbs.
Rather than run the full z/OS CICS translator, this preprocessor rewrites the
*specific* CICS verbs used by the CBSA Layer-0 programs (CRDTAGY1, GETCOMPY,
GETSCODE) into plain ``CALL`` statements that target small COBOL stub modules
(see ``tests/harness/shims``). The original ``.cbl`` sources are never modified;
the translated copy is written to stdout and compiled from the build directory,
so the very same sources still build unchanged for the mainframe.

Design goals:
  * Fixed-format friendly. Every in-place substitution (e.g. ``DFHRESP(...)``)
    is padded to the exact width of the text it replaces so that no column
    positions shift. Whole ``EXEC CICS ... END-EXEC`` blocks are replaced by
    freshly generated lines that we control, kept within column 72.
  * Minimal + explicit. Only the verbs these programs actually use are handled;
    an unknown verb raises so the gap is obvious instead of silently faked.

Supported verbs (see README for how to add more):
  RETURN, DELAY, GET CONTAINER, PUT CONTAINER, LINK, ASSIGN, ABEND,
  ASKTIME, FORMATTIME, HANDLE ABEND, SYNCPOINT, and the VSAM/KSDS file
  verbs READ, READ UPDATE, REWRITE, WRITE, STARTBR, READNEXT, READPREV,
  ENDBR (all routed to the CICSVSAM in-memory KSDS double).

Usage:
  python3 cicsPreprocessor.py <path-to-source.cbl>  > translated.cbl
"""

import re
import sys

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# DFHRESP(name) -> numeric condition value. Only NORMAL is used by the Layer-0
# programs; extend this map as new programs are added.
DFHRESP_MAP = {
    "NORMAL": 0,
    "NOTFND": 13,
    "DUPREC": 14,
    "DUPKEY": 15,
    "ENDFILE": 20,
    "SYSIDERR": 53,
}

# EXEC CICS file-control verbs routed to the CICSVSAM KSDS test double.
VSAM_FILE_VERBS = (
    "READ", "REWRITE", "WRITE", "STARTBR", "READNEXT", "READPREV", "ENDBR",
)

# Indentation (Area B, column 12) for generated statements.
INDENT = " " * 11
CONT = " " * 16  # continuation indent for USING operands

# Column at which Area A/B code begins in fixed-format (1-based col 8 -> idx 7).
CODE_START = 7
# Right margin of the source area in fixed-format COBOL.
MARGIN_R = 72


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
                 refs=[rid, rec, ops["RESP"], ops["RESP2"]],
                 period=period)


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

    if verb == "LINK":
        return _call("CICSLINK",
                     refs=[ops["PROGRAM"], ops.get("COMMAREA", "OMITTED")],
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
        return _call("CICSABND", contents=[ops["ABCODE"]], period=period)

    if verb == "ASKTIME":
        return _call("CICSTIME", refs=[ops["ABSTIME"]], period=period)

    if verb == "FORMATTIME":
        return _call("CICSFTIM",
                     refs=[ops["ABSTIME"], ops["DDMMYYYY"], ops["TIME"]],
                     period=period)

    raise ValueError("Unsupported EXEC CICS verb: {!r}".format(verb))


def _replace_dfhresp(line):
    """Replace DFHRESP(NAME) with its numeric value, padded to equal width."""
    def repl(m):
        name = m.group(1).upper()
        val = DFHRESP_MAP.get(name, 0)
        return str(val).ljust(len(m.group(0)))
    return re.sub(r"DFHRESP\(([A-Za-z0-9]+)\)", repl, line)


def _is_comment(line):
    return len(line) > CODE_START and line[CODE_START - 1] in ("*", "/")


def _code(line):
    """Return the Area A/B text (cols 8..72) of a fixed-format line."""
    if len(line) <= CODE_START:
        return ""
    return line[CODE_START:MARGIN_R]


def translate(lines):
    out = []
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

        code = _code(line)
        stripped = code.strip()
        upper = stripped.upper()

        # Comment out compiler-directing statements (CBL / PROCESS).
        if upper.startswith("CBL ") or upper == "CBL" or upper.startswith("PROCESS"):
            out.append("      *" + line[CODE_START:])
            i += 1
            continue

        # Detect start of an EXEC CICS block.
        if re.match(r"EXEC\s+CICS\b", upper):
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
            out.extend(_translate_block(flat, period))
            i = j + 1
            continue

        # Insert EIB shim copybook right after WORKING-STORAGE SECTION.
        if uses_eib and re.match(r"WORKING-STORAGE\s+SECTION\.", upper):
            out.append(line)
            out.append("{}COPY DFHEIBLK.".format(INDENT))
            i += 1
            continue

        # Bind DFHCOMMAREA and inject the EIB shim init at the procedure top.
        if re.match(r"PROCEDURE\s+DIVISION", upper):
            if has_commarea and "USING" not in upper:
                out.append(line[:CODE_START]
                           + "PROCEDURE DIVISION USING DFHCOMMAREA.")
            else:
                out.append(line)
            if uses_eib:
                out.append("       CBSA-SHIM-INIT SECTION.")
                out.append("       CBSA-SHIM-INIT-P.")
                out.append("{}CALL 'CICSINIT' USING DFHEIB-SHIM.".format(INDENT))
            i += 1
            continue

        # Ordinary line: perform in-place, width-preserving substitutions.
        if "DFHRESP(" in code:
            fixed = _replace_dfhresp(code)
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
