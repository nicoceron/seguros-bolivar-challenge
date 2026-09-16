#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p docs/build output/pdf
if command -v pdflatex >/dev/null 2>&1; then
  for pass in 1 2; do
    pdflatex -interaction=nonstopmode -halt-on-error -output-directory=docs/build docs/technical-assessment.tex
  done
elif command -v tectonic >/dev/null 2>&1; then
  tectonic --outdir docs/build docs/technical-assessment.tex
else
  echo 'Install TeX Live (LaTeX, recommended fonts, Spanish, latex-extra) or Tectonic.' >&2
  exit 1
fi
cp docs/build/technical-assessment.pdf output/pdf/Nicolas_Ceron_Prueba_Tecnica.pdf
printf 'PDF: output/pdf/Nicolas_Ceron_Prueba_Tecnica.pdf\n'
