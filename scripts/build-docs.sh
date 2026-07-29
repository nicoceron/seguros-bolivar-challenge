#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
build_dir="${project_dir}/docs/build"
output_dir="${project_dir}/output/pdf"
source_file="${project_dir}/docs/technical-assessment.tex"
output_file="${output_dir}/Nicolas_Ceron_Prueba_Tecnica.pdf"

mkdir -p "${build_dir}" "${output_dir}"

tectonic \
  --keep-logs \
  --outdir "${build_dir}" \
  "${source_file}"

cp "${build_dir}/technical-assessment.pdf" "${output_file}"

pdfinfo "${output_file}" | grep -E '^(Pages|Page size|Encrypted):'
echo "Created ${output_file}"

