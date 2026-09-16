#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Archive the tested source, never a stale HEAD while edits remain uncommitted.
if [[ -n "$(git status --porcelain)" ]]; then
  echo 'Commit source changes before packaging; the ZIP is built from HEAD.' >&2
  exit 1
fi
mvn -B -ntp verify
bash scripts/build-docs.sh
python3 - <<'PY'
from pathlib import Path
import subprocess, zipfile
root = Path.cwd()
output = root / 'output/Nicolas_Ceron_Prueba_Tecnica.zip'
paths = subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0')
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for name in filter(None, paths):
        archive.write(root / name, 'seguros-bolivar-challenge/' + name)
    archive.write(root / 'output/pdf/Nicolas_Ceron_Prueba_Tecnica.pdf', 'Nicolas_Ceron_Prueba_Tecnica.pdf')
    archive.writestr('COMMIT.txt', subprocess.check_output(['git', 'rev-parse', 'HEAD']))
print(output)
PY
