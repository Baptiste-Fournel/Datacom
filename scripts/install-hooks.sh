#!/usr/bin/env sh
set -e
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit .githooks/pre-push
echo "Hooks git actives : pre-commit (style + regles) et pre-push (verify sans ITs)."
