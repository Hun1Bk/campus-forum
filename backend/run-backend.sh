#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

load_env() {
  local file="$1"
  [[ -f "$file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "${line:0:1}" == "#" ]] && continue
    [[ "$line" == *"="* ]] || continue

    local key="${line%%=*}"
    local value="${line#*=}"
    key="${key%"${key##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi
    export "$key=$value"
  done < "$file"
}

load_env "$ROOT_DIR/.env"

BACKEND_PORT="${BACKEND_PORT:-3000}"

echo "后端正在启动..."
echo "当前端口：$BACKEND_PORT"

run_args=("--server.port=$BACKEND_PORT")
[[ -n "${DB_USERNAME:-}" ]] && run_args+=("--spring.datasource.username=$DB_USERNAME")
[[ -n "${DB_PASSWORD:-}" ]] && run_args+=("--spring.datasource.password=$DB_PASSWORD")
[[ -n "${DB_URL:-}" ]] && run_args+=("--spring.datasource.url=$DB_URL")

cd "$ROOT_DIR/backend"
if [[ -f "target/backend-0.0.1-SNAPSHOT.jar" ]]; then
  java -jar "target/backend-0.0.1-SNAPSHOT.jar" "${run_args[@]}"
else
  chmod +x ./mvnw
  spring_args="${run_args[*]}"
  ./mvnw spring-boot:run "-Dspring-boot.run.arguments=$spring_args"
fi
