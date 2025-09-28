#!/usr/bin/env bash
set -e

REPOSITORY="/home/ubuntu/carhartt_platform"
LOG="$REPOSITORY/server_log"

cd "$REPOSITORY"

# 권한 정리(필요 시)
chown -R ubuntu:ubuntu "$REPOSITORY"

# 로그 파일 보장
touch "$LOG"
chmod 664 "$LOG"

echo "> Build 파일 복사"
cp ./build/libs/*.jar "$REPOSITORY"/

echo "> 현재 구동중인 애플리케이션 pid 확인"
PID="$(pgrep -f "$REPOSITORY/.*\.jar" || true)"
if [ -n "$PID" ]; then
  echo "> kill -15 $PID"
  kill -15 "$PID" || true
  sleep 5
  ps -p "$PID" > /dev/null 2>&1 && { echo "> kill -9 $PID"; kill -9 "$PID" || true; }
else
  echo "> 종료할것 없음."
fi

echo "> 새 어플리케이션 배포"
JAR_PATH="$(ls -tr "$REPOSITORY"/*.jar | tail -n 1)"
echo "> JAR: $JAR_PATH"

echo "> 실행권한 추가"
chmod +x "$JAR_PATH"

echo "> 실행 시작"
nohup java -jar "$JAR_PATH" >> "$LOG" 2>&1 < /dev/null &
echo "> started (pid $!)"
