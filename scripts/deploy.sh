#!/usr/bin/env bash
set -euo pipefail

APP_HOME="/home/ubuntu/carhartt_platform"
IMAGE_URI="$(cat "$APP_HOME/IMAGE_URI")"        # CI가 넣어준 완전한 ECR 이미지 URI
CONTAINER_NAME="carhartt-platform"              # 컨테이너 이름(원하는 이름)
AWS_REGION="ap-northeast-2"                     # 또는 환경/파일로 주입

echo "[deploy] Using image: $IMAGE_URI"

# 1) ECR 로그인 (EC2 인스턴스 롤에 ecr:GetAuthorizationToken 등 Pull 권한 필수)
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"

# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"

# 3) 기존 컨테이너 중지/삭제(있다면)
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# 4) 새 컨테이너 실행 (포트/환경변수는 서비스에 맞게 조정)
docker run -d --name "$CONTAINER_NAME" --restart=always -p 8080:8080 "$IMAGE_URI"

echo "[deploy] Container $CONTAINER_NAME started."
