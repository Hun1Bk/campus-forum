#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-start}"
DETACHED=0
OVERWRITE=0
shift || true

for arg in "$@"; do
  case "$arg" in
    --detached) DETACHED=1 ;;
    --overwrite) OVERWRITE=1 ;;
    *) echo "未知参数：$arg"; exit 1 ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/logs"
ENV_FILE="$ROOT_DIR/.env"
SCHEMA_FILE="$ROOT_DIR/backend/sql/forum_schema.sql"
FRONTEND_DIR="$ROOT_DIR/space"

mkdir -p "$LOG_DIR"

load_env() {
  local file="$1"
  [[ -f "$file" ]] || return 1

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

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

read_with_default() {
  local prompt="$1"
  local default="${2:-}"
  local value

  if [[ -n "$default" ]]; then
    read -r -p "$prompt [$default]: " value
    value="${value:-$default}"
  else
    read -r -p "$prompt: " value
  fi

  printf '%s' "$value"
}

read_secret_with_default() {
  local prompt="$1"
  local default="${2:-}"
  local value

  if [[ -n "$default" ]]; then
    read -r -s -p "$prompt [直接回车使用默认值]: " value
  else
    read -r -s -p "$prompt [可直接回车留空]: " value
  fi
  echo
  value="${value:-$default}"
  printf '%s' "$value"
}

read_yes_no() {
  local prompt="$1"
  local default_yes="${2:-0}"
  local suffix="y/N"
  local answer

  [[ "$default_yes" == "1" ]] && suffix="Y/n"
  read -r -p "$prompt ($suffix): " answer
  if [[ -z "$answer" ]]; then
    [[ "$default_yes" == "1" ]]
    return
  fi
  [[ "${answer,,}" == y* ]]
}

read_port_with_default() {
  local prompt="$1"
  local default="$2"
  local value

  while true; do
    value="$(read_with_default "$prompt" "$default")"
    if [[ "$value" =~ ^[0-9]+$ ]] && (( value >= 1 && value <= 65535 )); then
      printf '%s' "$value"
      return
    fi
    echo "端口必须是 1 到 65535 之间的数字。"
  done
}

dotenv_value() {
  local value="${1:-}"
  value="${value//$'\r'/}"
  value="${value//$'\n'/}"
  printf '%s' "$value"
}

random_secret() {
  if command_exists openssl; then
    openssl rand -base64 48
  else
    date +%s%N | sha256sum | awk '{print $1}'
  fi
}

run_config_wizard() {
  echo "校园论坛首次运行配置向导"
  echo "这个向导会生成 .env 本地配置，不会把密码、授权码写入 Git。"
  echo
  echo "== 1. 环境检查 =="

  command_exists java && echo "[OK] java: $(command -v java)" || echo "[缺少] java：请安装 JDK。"
  command_exists node && echo "[OK] node: $(command -v node)" || echo "[缺少] node：请安装 Node.js。"
  command_exists npm && echo "[OK] npm: $(command -v npm)" || echo "[缺少] npm：请安装 npm。"
  command_exists mysql && echo "[OK] mysql: $(command -v mysql)" || echo "[缺少] mysql：请安装 MySQL，或用图形工具导入 SQL。"

  echo
  echo "== 2. 本地配置 =="
  if [[ -f "$ENV_FILE" && "$OVERWRITE" != "1" ]]; then
    echo "已存在统一配置：$ENV_FILE"
    if ! read_yes_no "是否重新生成 .env"; then
      return
    fi
  elif [[ -f "$ENV_FILE" && "$OVERWRITE" == "1" ]]; then
    echo "将重新生成 .env。"
  fi

  local backend_port frontend_port db_user db_password db_host db_port db_name db_url
  local admin_password jwt_secret mail_host mail_port mail_username mail_password

  backend_port="$(read_port_with_default "后端端口" "3000")"
  while true; do
    frontend_port="$(read_port_with_default "前端端口" "8081")"
    [[ "$frontend_port" != "$backend_port" ]] && break
    echo "前端端口不能和后端端口相同。"
  done

  db_user="$(read_with_default "数据库用户名" "root")"
  db_password="$(read_secret_with_default "数据库密码")"
  db_host="$(read_with_default "数据库主机" "localhost")"
  db_port="$(read_port_with_default "数据库端口" "3306")"
  db_name="$(read_with_default "数据库名称" "kob")"
  db_url="$(read_with_default "数据库连接地址（高级，可直接回车留空）")"
  admin_password="$(read_secret_with_default "默认站长 admin 的初始密码" "admin123456")"
  jwt_secret="$(read_with_default "JWT 密钥" "$(random_secret)")"

  mail_host=""
  mail_port=""
  mail_username=""
  mail_password=""
  if read_yes_no "是否现在配置注册邮箱验证码 SMTP"; then
    mail_host="$(read_with_default "SMTP Host" "smtp.qq.com")"
    mail_port="$(read_with_default "SMTP Port" "465")"
    mail_username="$(read_with_default "发件邮箱账号")"
    mail_password="$(read_secret_with_default "SMTP 授权码")"
  else
    echo "已跳过 SMTP。未配置时，注册邮箱验证码接口会提示邮件服务未配置或发送失败。"
  fi

  {
    echo "BACKEND_PORT=$(dotenv_value "$backend_port")"
    echo "FRONTEND_PORT=$(dotenv_value "$frontend_port")"
    echo
    echo "DB_USERNAME=$(dotenv_value "$db_user")"
    echo "DB_PASSWORD=$(dotenv_value "$db_password")"
    echo "DB_HOST=$(dotenv_value "$db_host")"
    echo "DB_PORT=$(dotenv_value "$db_port")"
    echo "DB_NAME=$(dotenv_value "$db_name")"
    if [[ -n "$db_url" ]]; then
      echo "DB_URL=$(dotenv_value "$db_url")"
    else
      echo "# DB_URL=jdbc:mysql://localhost:3306/kob?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false"
    fi
    echo "ADMIN_PASSWORD=$(dotenv_value "$admin_password")"
    echo "JWT_SECRET=$(dotenv_value "$jwt_secret")"
    echo
    echo "MAIL_HOST=$(dotenv_value "$mail_host")"
    echo "MAIL_PORT=$(dotenv_value "$mail_port")"
    echo "MAIL_USERNAME=$(dotenv_value "$mail_username")"
    echo "MAIL_PASSWORD=$(dotenv_value "$mail_password")"
  } > "$ENV_FILE"

  echo "已生成统一配置：$ENV_FILE"
  echo
  echo "== 3. 数据库准备 =="
  echo "请确认 MySQL 已启动，并导入初始化 SQL："
  echo "mysql -u你的用户名 -p < \"$SCHEMA_FILE\""
  echo "后端启动时还会自动补充缺失字段。"
  echo
  echo "== 4. 前端依赖 =="
  if [[ -d "$FRONTEND_DIR/node_modules" ]]; then
    echo "已检测到 space/node_modules，通常无需重复安装。"
  else
    echo "首次运行前需要安装前端依赖："
    echo "cd space"
    echo "npm install --legacy-peer-deps"
    if command_exists npm && read_yes_no "是否现在执行 npm install --legacy-peer-deps"; then
      (cd "$FRONTEND_DIR" && npm install --legacy-peer-deps)
    fi
  fi
  echo
  echo "== 5. 后续命令 =="
  echo "启动项目：./start-all.sh"
  echo "关闭项目：./start-all.sh stop"
  echo "重新配置：./start-all.sh config"
}

ensure_config() {
  if [[ ! -f "$ENV_FILE" ]]; then
    run_config_wizard
  fi
  load_env "$ENV_FILE" || {
    echo "未找到 .env 配置文件，请先执行：./start-all.sh config"
    exit 1
  }
}

is_running() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1
}

stop_by_pid_file() {
  local name="$1"
  local pid_file="$2"

  if [[ ! -f "$pid_file" ]]; then
    echo "$name pid file not found: $pid_file"
    return
  fi

  local pid
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid"
    echo "Stopped $name. PID: $pid"
  else
    echo "$name is not running. Stale PID: $pid"
  fi
  rm -f "$pid_file"
}

stop_services() {
  stop_by_pid_file "Backend monitor" "$LOG_DIR/monitor.pid"
  stop_by_pid_file "Backend" "$LOG_DIR/backend.pid"
  stop_by_pid_file "Frontend" "$LOG_DIR/frontend.pid"
  echo "Campus Forum services stopped. If a port is still occupied, check it manually with: ss -ltnp"
}

start_local() {
  ensure_config
  BACKEND_PORT="${BACKEND_PORT:-3000}"
  FRONTEND_PORT="${FRONTEND_PORT:-8081}"

  if is_running "$LOG_DIR/backend.pid" || is_running "$LOG_DIR/frontend.pid"; then
    echo "Campus Forum already has running services. Use ./start-all.sh stop first if you need a clean restart."
    return
  fi

  trap 'stop_services >/dev/null 2>&1 || true' EXIT INT TERM HUP

  (
    cd "$ROOT_DIR/backend"
    bash "$ROOT_DIR/backend/run-backend.sh" > "$LOG_DIR/backend.out.log" 2> "$LOG_DIR/backend.err.log" &
    echo $! > "$LOG_DIR/backend.pid"
  )

  (
    cd "$ROOT_DIR/space"
    bash "$ROOT_DIR/space/run-space.sh" > "$LOG_DIR/frontend.out.log" 2> "$LOG_DIR/frontend.err.log" &
    echo $! > "$LOG_DIR/frontend.pid"
  )

  echo
  echo "Backend:  http://localhost:$BACKEND_PORT"
  echo "Frontend: http://localhost:$FRONTEND_PORT"
  echo "Admin:    http://localhost:$FRONTEND_PORT/#/admin"
  echo
  echo "Logs are in: $LOG_DIR"
  echo "Keep this terminal open. Closing it or pressing Ctrl+C will stop backend and frontend."

  backend_pid="$(cat "$LOG_DIR/backend.pid")"
  frontend_pid="$(cat "$LOG_DIR/frontend.pid")"
  while kill -0 "$backend_pid" >/dev/null 2>&1 && kill -0 "$frontend_pid" >/dev/null 2>&1; do
    sleep 2
  done
}

docker_action() {
  local docker_action="$1"

  if [[ "$docker_action" != "docker-stop" ]]; then
    ensure_config
  elif [[ -f "$ENV_FILE" ]]; then
    load_env "$ENV_FILE" || true
  fi

  cd "$ROOT_DIR"
  if [[ "$docker_action" == "docker-stop" ]]; then
    docker compose down
    return
  fi

  if [[ "$docker_action" == "docker-restart" ]]; then
    docker compose down
  fi

  if [[ "$DETACHED" == "1" ]]; then
    docker compose up -d --build
  else
    trap 'docker compose down' EXIT INT TERM HUP
    docker compose up --build
  fi
}

case "$ACTION" in
  config)
    run_config_wizard
    ;;
  start)
    start_local
    ;;
  stop)
    [[ -f "$ENV_FILE" ]] && load_env "$ENV_FILE" || true
    stop_services
    ;;
  restart)
    [[ -f "$ENV_FILE" ]] && load_env "$ENV_FILE" || true
    stop_services
    start_local
    ;;
  docker-start|docker-stop|docker-restart)
    docker_action "$ACTION"
    ;;
  *)
    echo "用法：./start-all.sh [start|stop|restart|config|docker-start|docker-stop|docker-restart] [--detached] [--overwrite]"
    exit 1
    ;;
esac
