#!/usr/bin/env bash
set -euo pipefail

APP_HOME="/home/ubuntu/carhartt_platform"
IMAGE_URI="$(cat "$APP_HOME/IMAGE_URI")"   # git actions가 넣어준 완전한 ECR 이미지 URI
CONTAINER_NAME="carhartt-platform"        # 컨테이너 이름 (원하는 이름)
AWS_REGION="ap-northeast-2"              # 또는 환경/파일로 주입

# 애플리케이션 환경변수 파일 경로
ENV_FILE="$APP_HOME/application.env"

# 환경변수 파일 존재 확인
if [ ! -f "$ENV_FILE" ]; then
    echo "[deploy] ERROR: application.env not found at $ENV_FILE"
    exit 1
fi

echo "[deploy] Using image: $IMAGE_URI"
echo "[deploy] Loading environment variables from application.env"

# 1) ECR 로그인 (EC2 인스턴스 롤에 ecr:GetAuthorizationToken 등 Pull 권한 필수)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"

# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"

# 3) 기존 컨테이너 중지/삭제(있다면)
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# 4) 새 컨테이너 실행 (--env-file로 모든 환경변수 주입)
# log file을 host의 file과 volumes mount
# SPRING_PROFILES_ACTIVE=prod 설정으로 production 프로파일 활성화
docker run -d -v /home/ubuntu/app/logs:/home/ubuntu/app/logs \
  --name "$CONTAINER_NAME" --restart=always -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file "$ENV_FILE" \
  "$IMAGE_URI"

echo "[deploy] Container $CONTAINER_NAME started with environment variables from application.env"
