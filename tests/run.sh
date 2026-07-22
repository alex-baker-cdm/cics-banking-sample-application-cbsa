#!/usr/bin/env bash
#
# run.sh - build and run the CBSA COBOL isolated-test harness with GnuCOBOL.
#
# Pipeline per program under test:
#   1. cicsPreprocessor.py rewrites EXEC CICS verbs -> CALLs to shim modules
#      (original .cbl sources are never modified).
#   2. cobc compiles the translated program + the shim modules as dynamically
#      loadable modules (.so).
#   3. cobc compiles each test driver as an executable that CALLs the program
#      (and the shims) at run time, resolved via COB_LIBRARY_PATH.
#
# Exit code is non-zero if any test fails, so it is CI-friendly.
#
# Usage:  tests/run.sh            (from anywhere)
#         make -C tests test
#
set -euo pipefail

scriptDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repoRoot="$(cd "${scriptDir}/.." && pwd)"

harnessDir="${scriptDir}/harness"
shimDir="${harnessDir}/shims"
unitDir="${scriptDir}/unit"
buildDir="${scriptDir}/build"
moduleDir="${buildDir}/modules"
genSrcDir="${buildDir}/gensrc"
binDir="${buildDir}/bin"

cobolSrc="${repoRoot}/src/base/cobol_src"
cobolCopy="${repoRoot}/src/base/cobol_copy"
harnessCopy="${harnessDir}/copy"

preprocessor="${harnessDir}/cicsPreprocessor.py"

# Programs under test (source base name = PROGRAM-ID = module file name).
programsUnderTest=(CRDTAGY1 GETCOMPY GETSCODE UPDACC INQACC INQACCCU)

# Deterministic seed for RANDOM (via EIBTASKN) and a no-op DELAY.
export CBSA_TEST_TASKN="${CBSA_TEST_TASKN:-1}"
export CBSA_TEST_DELAY_MODE="${CBSA_TEST_DELAY_MODE:-stub}"

cobc="${COBC:-cobc}"
copyOpts=(-I "${harnessCopy}" -I "${cobolCopy}")

rm -rf "${buildDir}"
mkdir -p "${moduleDir}" "${genSrcDir}" "${binDir}"

echo "== GnuCOBOL =="
"${cobc}" --version | head -1
echo

echo "== Building shim modules =="
for shim in "${shimDir}"/*.cbl; do
    name="$(basename "${shim}" .cbl)"
    echo "   shim  ${name}"
    "${cobc}" -m "${copyOpts[@]}" -o "${moduleDir}/${name}.so" "${shim}"
done
echo

echo "== Preprocessing + building programs under test =="
for prog in "${programsUnderTest[@]}"; do
    echo "   prog  ${prog}"
    python3 "${preprocessor}" "${cobolSrc}/${prog}.cbl" > "${genSrcDir}/${prog}.cbl"
    "${cobc}" -m "${copyOpts[@]}" -o "${moduleDir}/${prog}.so" \
        "${genSrcDir}/${prog}.cbl"
done
echo

export COB_LIBRARY_PATH="${moduleDir}"

echo "== Building + running unit tests =="
failures=0
total=0
for driver in "${unitDir}"/*.cbl; do
    name="$(basename "${driver}" .cbl)"
    total=$((total + 1))
    "${cobc}" -x "${copyOpts[@]}" -o "${binDir}/${name}" "${driver}"
    echo "-----------------------------------------------------------"
    if "${binDir}/${name}"; then
        :
    else
        echo "   >> ${name} returned non-zero"
        failures=$((failures + 1))
    fi
done
echo "-----------------------------------------------------------"

echo
if [ "${failures}" -eq 0 ]; then
    echo "ALL TESTS PASSED (${total}/${total})"
    exit 0
else
    echo "TESTS FAILED: ${failures} of ${total}"
    exit 1
fi
