#!/bin/bash

currentPath=$(cd "$(dirname "$0")" && pwd)
xmlFile="${currentPath}/haruhibot_service.xml"
SERVICE_NAME=$(sed -n 's/.*<id>\(.*\)<\/id>.*/\1/p' "${xmlFile}" | tr -d '\000-\037\177-\377' | sed 's/？//g')
ARGUMENTS=$(sed -n 's/.*<arguments>\(.*\)<\/arguments>.*/\1/p' "${xmlFile}" | sed 's/？//g')

PIDFILE="${currentPath}/${SERVICE_NAME}.pid"

# 检查 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    echo "错误: JAVA_HOME 未设置，请先 export JAVA_HOME=/path/to/jdk"
    exit 1
fi
JAVA_BIN="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_BIN" ]; then
    echo "错误: 找不到 java 可执行文件: $JAVA_BIN"
    exit 1
fi

start() {
    if [ -f "$PIDFILE" ] && kill -0 $(cat "$PIDFILE") 2>/dev/null; then
        echo "服务 $SERVICE_NAME 已在运行 (PID: $(cat $PIDFILE))"
        exit 1
    fi
    echo "启动服务 $SERVICE_NAME ..."
    cd "$currentPath"
    nohup "$JAVA_BIN" $ARGUMENTS > /dev/null 2>&1 &
    echo $! > "$PIDFILE"
    echo "服务已启动，PID: $(cat $PIDFILE)"
}

stop() {
    if [ ! -f "$PIDFILE" ]; then
        echo "服务 $SERVICE_NAME 未运行 (找不到 PID 文件)"
        exit 1
    fi
    PID=$(cat "$PIDFILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "停止服务 $SERVICE_NAME (PID: $PID) ..."
        kill "$PID"
        rm -f "$PIDFILE"
        echo "服务已停止"
    else
        echo "PID $PID 不存在，可能已停止"
        rm -f "$PIDFILE"
    fi
}

status() {
    if [ -f "$PIDFILE" ] && kill -0 $(cat "$PIDFILE") 2>/dev/null; then
        echo "服务 $SERVICE_NAME 正在运行 (PID: $(cat $PIDFILE))"
    else
        echo "服务 $SERVICE_NAME 未运行"
    fi
}

restart() {
    stop
    sleep 2
    start
}

case "$1" in
    start) start ;;
    stop) stop ;;
    restart) restart ;;
    status) status ;;
    *) echo "用法: $0 {start|stop|restart|status}" ;;
esac