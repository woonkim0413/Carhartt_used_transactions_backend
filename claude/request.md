application.properties, env_values/.env를 참조하여 deploy.yml 및 scripts/deploy.sh를
어떻게 수정해야 하는지 response_properties.md에 작성해줘 (기존 파일을 수정해야 할거야)

env_values/.env는 현재 깃허브 시크릿 레포에 저장되어 있으니 끌어와서 deploy agent에게
script를 건네줄 때 함께 건네준 다음 컨테이너에서 .jar을 실행할 때 --env-file을 사용하여
환경변수로써 주입해야 해

