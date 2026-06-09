#!/usr/bin/env bash
#
# run.sh - Compila y ejecuta el solver PSO + Bottom-Left para el 2D-SPP.
#
# Uso:
#   ./run.sh                          # procesa todos los .csv de ./testcases
#   ./run.sh <archivo.csv>            # procesa un caso individual
#   ./run.sh <carpeta>                # procesa todos los .csv de una carpeta
#
# Ejemplos:
#   ./run.sh
#   ./run.sh <ruta al archivo.csv>
#
set -euo pipefail

# Ubicarse en el directorio del script (raiz del proyecto).
cd "$(dirname "$0")"

SRC_DIR="src/spp"
OUT_DIR="out"

# Verificar que el JDK esta disponible.
if ! command -v javac >/dev/null 2>&1; then
  echo "ERROR: no se encontro 'javac'. Instala un JDK (java -version)." >&2
  exit 1
fi

echo ">> Compilando..."
mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" "$SRC_DIR"/*.java
echo ">> Compilacion OK."
echo

# Pasa todos los argumentos recibidos a la aplicacion (carpeta o archivo).
echo ">> Ejecutando..."
java -cp "$OUT_DIR" spp.Main "$@"
