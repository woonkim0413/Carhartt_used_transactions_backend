application.properties, application-local.yml, application-prod.yml, application-test.yml에
적힌 내용을 수정해줘
application.properties에는 placeholder 및 공통 설정만 남겨줘
prod는 mysql, redis를 사용하고 현재 application.properties에 적혀져 있는 설정들 (JPA, Hikari 등)
을 사용할거야
local 및 test는 h2를 사용하고 현재 application-test.yml에 적혀져 있는 설정을 베이스로 사용할거야
알맞게 수정해줘