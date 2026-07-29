#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
archive="${project_dir}/output/Nicolas_Ceron_Prueba_Tecnica.zip"
pdf="${project_dir}/output/pdf/Nicolas_Ceron_Prueba_Tecnica.pdf"

"${script_dir}/build-docs.sh"

cd "${project_dir}"
mvn --batch-mode --no-transfer-progress verify
mkdir -p output

git archive \
  --format=zip \
  --prefix=seguros-bolivar-challenge/ \
  --output="${archive}" \
  HEAD

zip -q -j "${archive}" "${pdf}"

unzip -t "${archive}" >/dev/null
echo "Created ${archive}"

