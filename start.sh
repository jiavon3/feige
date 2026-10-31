#!/usr/bin/env bash
# 用法:
#   ./start.sh              # 在项目根目录打包后启动，默认 8080
#   ./start.sh 9090         # 指定端口
#   PORT=9090 ./start.sh    # 环境变量指定端口
# 无论从哪个目录调用，都会回到 start.sh 所在的项目根目录执行。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [[ $# -ge 1 ]]; then
  PORT="$1"
fi
PORT="${PORT:-8080}"

if ! command -v java >/dev/null 2>&1; then
  echo "找不到 java，请先安装 JDK 8+ 并加入 PATH" >&2
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "找不到 mvn，请先安装 Maven 并加入 PATH" >&2
  exit 1
fi
if [[ ! -f "$ROOT/pom.xml" ]]; then
  echo "找不到 ${ROOT}/pom.xml，请把 start.sh 放在项目根目录" >&2
  exit 1
fi

echo "打包飞鸽（${ROOT}）…"
mvn -f "$ROOT/pom.xml" -DskipTests package

JAR="${ROOT}/target/feige.jar"
if [[ ! -f "$JAR" ]]; then
  echo "打包完成但找不到 ${JAR}" >&2
  exit 1
fi

echo "启动飞鸽，端口 ${PORT} …"
exec java -jar "$JAR" --server.port="$PORT"
