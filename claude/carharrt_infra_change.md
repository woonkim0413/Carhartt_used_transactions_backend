
# *Tags :* 
# *linked file :* 
# *reference address :*
---
# < 내용 >
### <font color="#de7802">::: Reference</font>
[[지식 정리(단발적, 지엽적)/Docker (드림코딩)(Docs)|Docker (드림코딩)(Docs)]]
[[강의, 플젝 정리(장기적, 거시적)/학교 수업들/시스템프로그래밍/채팅 프로그램 (시프과제)/wsl2 + docker test를 하며 정리한 네트워크|wsl2 + docker test를 하며 정리한 네트워크]]
[[지식 정리(단발적, 지엽적)/(책) 그림과 실습으로 배우는 도커 & 쿠버네티스|(책) 그림과 실습으로 배우는 도커 & 쿠버네티스]]

https://www.44bits.io/ko/post/easy-deploy-with-docker <-(아주 꼼꼼히 설명된 블로그)

https://sundaland.tistory.com/519 <- (AWS EC2 docker 설치)

https://adjh54.tistory.com/420 <- (spring boot docker file 생성)

https://tytydev.tistory.com/45 <- (MySQL과 spring boot container 통신 정리)

https://rlaehddnd0422.tistory.com/217 <- (cicd + docker)

https://wildeveloperetrain.tistory.com/401#google_vignette
https://wildeveloperetrain.tistory.com/402 <- (cide + docker 구축 시리즈)

https://velog.io/@sontulip/how-to-set-up-infra#:~:text=IP%EB%A1%9C%20%EC%97%B4%EC%96%B4%EC%A4%8D%EB%8B%88%EB%8B%A4.-,%EB%A7%9D%EB%B6%84%EB%A6%AC%EB%A1%9C%20%EB%B3%B4%EC%95%88%EC%9D%84%20%EB%8D%94%20%ED%8A%BC%ED%8A%BC%ED%9E%88,-%ED%8F%AC%ED%8A%B8%EC%99%80%20%ED%8F%AC%ED%8A%B8%EC%97%90%20%EC%A0%91%EA%B7%BC <- (인프라에 대한 고찰 (<font color="#d094db">읽어보기</font>))
### <font color="#de7802">::: 구조 변경 전 참고</font>
<font color="#00b0f0">현재 상황</font>
**instance (t4g.small)** : 네트워크 대역폭 5Gbps, RAM 2GiB (크래딧 적립 X), 
CPU는 vCPU 2개에 idle 상태일 땐 CPU 크래딧을 적립

**instance 주요 프로그램** : Nginx + docker container 1개 (.jar) + RDS
처음에는 .jar과 MySQL을 docker container에 각각 담아서 테스트를 하려고 했다
하지만 2GB에 container을 두 개 띄우는 구조는 조금만 트래픽이 생겨도 EC2가
down될 수 있다고 판단하여 db를 MySQL container -> RDS로 변경하기로 했다

**프로그램 RAM 사용량 (추정치) :** 
- Nginx : 10 ~ 100MB (decoupled 구조라 정적 파일 serving이 없기에 거의 10MB)
- Docker Engine : 100 ~ 250 MB
- MySQL Container Image : 500 ~ 700MB (사용 안 함)
- spring boot (.jar) Container Image : 600 ~ 800MB
- Ubuntu Kernel / 기본데몬 (sshd 등) / 캐시 (네트워크, 파일) : 300 ~ 400MB

**RAM 최적화를 위한 작업 :**
- JVM 튜닝
- ssd swap

### <font color="#de7802">::: 현재 CI/CD 구조 분석</font>
<font color="#00b0f0">reference</font>
[[강의, 플젝 정리(장기적, 거시적)/프로젝트/칼하트 중고거래/인프라/CI-CD 구축 (swagger 목적) (github actions 사용)|CI-CD 구축 (swagger 목적) (github actions 사용)]] <- (더 자세한 CICD 구축 정리 글)


<font color="#00b0f0">기존 CI/CD 흐름</font> (<font color="#d094db">매우 중요</font>)
1)
main push 인지 -> 

2)
Runner (임시VM)에 repo code 가져옴 (`actions/checkout@v2` 명령어) -> 

3)
 JDK 17 settings -> 

4)
repo에서 가져온 application.properties를 지우고 <font color="#92d050">gitachtion secret에서</font> 가져옴 -> 

5)
스크립트에 실행 권한 주고 dos2unix로 개행 통일 (os에 따라 개행이 다를 수 있음) -> 

6)
`./gradlew build -x test` 명령어를 실행하여 .jar 생성 (<font color="#92d050">이름은 settings.gradle에서 지정</font>) -> 

7)
빌드 산출물을 포함하여 github repo 전체를 GITHUB_SHA.zip으로 압축 -> 
(GITHUB_SHA는 git actions에서 제공하는 환경 변수임)

8)
`aws-actions/configure-aws-credentials@v4` 를 사용하여 <font color="#92d050">aws IAM 권한을 사용할 수 있는</font>
<font color="#92d050">고정 액세스키</font>를 (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 등) 런타임 환경 변수에 
저장한다 (다음 step에서 aws권한이 필요할 때 사용한다) ->

9)
`aws s3 cp --region ap-northeast-2 ./$GITHUB_SHA.zip s3://$BUCKET_NAME/$PROJECT_NAME/$GITHUB_SHA.zip` 
위 명령어를 사용하여 s3에 파일을 업로드 한다 
(<font color="#92d050">s3는 path가 prefix개념이라 (상위 폴더 없음)</font> BUCKET_NAME만 존재하면 무조건 넣어짐) ->

10)
\* <font color="#92d050">S3 접근이 가능한 원리</font>
위 명령어에서 접근 권한을 획득한 IAM과 S3는 같은 콘솔 계정에 위치한다  
IAM Rule(Policy)에 `AmazonS3FullAccess`를 추가했기에 같은 계정에 있는 S3접근이
허용된다 그렇기에 IAM 권한을 얻은 것 만으로도 S3접근이 가능한 것이다

11)
`aws deploy create-deployment --application-name $CODE_DEPLOY_APP_NAME --deployment-config-name CodeDeployDefault.OneAtATime --deployment-group-name $DEPLOYMENT_GROUP_NAME --s3-location bucket=$BUCKET_NAME,bundleType=zip,key=$PROJECT_NAME/$GITHUB_SHA.zip` 명령어를
통해서 aws에서 설정해 놓은 CodeDeployment application group에 배포 명령을 내린다 
(이때 agent에게 <font color="#92d050">butket 이름, 파일 type, 파일 위치 (key)를 건네줌</font>으로써 s3에 있는 파일을
ec2로 가져올 수 있도록 한다) ->

12)
Deploymnet agent가 해당 명령어를 인지하고 root에 존재하는 appspec.yml를 읽는다 ->

13)
source는 압축을 풀어놓은 번들의 루트를 가리킨다 (/이기에 번들 파일 전체)
destination은 bundle파일을 둘 위치를 지정한다 
(host의 `/home/ubuntu/carhartt_platform` path 아래에 압축을 푼 번들 파일이 위치됨) ->
``` java
version: 0.0  
os: linux
files:  
  - source:  /  
    destination: /home/ubuntu/carhartt_platform
    overwrite: true // 기존 파일 덮어씌움
```

14)
scripts/nginx_setup.sh와 scripts/deploy.sh가 순서대로 실행된다 ->
(아래는 scripts/deploy.sh에 적힌 명령어들)

15)
`cp ./build/libs/*.jar "$REPOSITORY"/` 명령어로 build된 .jar을 REPOSITORY로 위치 시킨다
(`REPOSITORY="/home/ubuntu/carhartt_platform"`) ->

16)
서버에 이미 구동 중인 application이 있다면 kill -9 (or -15)로 종료시킨다 ->

17)
아래 명령어를 통해 새로 bulid한 프로그램을 실행시킨다 (최종)
`nohup java -jar "$JAR_PATH" >> "$LOG" 2>&1 < /dev/null &` 


### <font color="#de7802">::: CICD 구조 변경 방향</font>
<font color="#00b0f0">기존 .zip 변경 + docker hub 사용</font> (<font color="#d094db">중요</font>)
.zip을 S3에 올려서 EC2에 전달하는 구조는 기존 .jar을 전달하는 방식에서 
appspec.yml, deploy.sh, nginx_setup.sh, docker-compose.yml와 같이
<font color="#92d050">EC2에서 사용할 스크립트만 전달하는 목적으로 사용</font>하고, 
<font color="#92d050">.jar docker image는 aws Elastic Container Registry(ECR)</font>로 EC2에 전달하는 구조로 변경한다
(.jar을 담은 image를 만드는 docker file을 미리 project에 포함시켜야 한다)

(<font color="#d094db">해당 내용은 db를 RDS로 변경함으로써 구현 x</font>)
MySQL은 보통 <font color="#92d050">커스텀 image를 사용하지 않고 공식 mysql:8 image를 사용</font>한다
그렇기에 git actions에선 할 것이 없고 EC2에서 docker hub에서 pull하여 가져온다

docker-compose.yml에 EC2에서 컨테이너를 어떻게 띄울지 정의한다
(컨테이너 종류, port, 계정 key, 리소스 제한, 네트워크 등)

<font color="#00b0f0">구조 구축</font>
**Nginx :** aws EC2 host에 위치 (localhost:127.0.0.1:8080을 통해 docker springboot로 reverse)
**spring boot :** docker container에 위치 (host와 8080:8080 forwarding)
**RDS :** 
\* 처음엔 .jar과 MySQL을 같은 docker dridge network에 포함시켜 통신하려고 했음

\* <font color="#00b0f0">프로젝트 구조가 커지면 ECS 및 ECR 사용 고려</font>
ECS (Elastic Container Service) : 컨테이너 오케스트레이터 (Fargate은 서버리스 엔진)
ECR (Elastic Container Registry) : AWS 컨테이너 창고

\* <font color="#ffff00">TODO : </font>docker hub의 namespace와 docker contaimer에 고유 공간을 제공할 수 있는
이론적 토대인 linux kernel namespace 비교 공부

### <font color="#de7802">::: CICD + Docker + RDS 구조에 대해 설명한 블로그 글 공부</font>
<font color="#de7802">::: 블로그 (1) (CICD)</font>
https://wildeveloperetrain.tistory.com/402#google_vignette
<font color="#00b0f0">docker image 생성</font> 
docker image를 생성하기 위한 docker file은 사전에 만들어서 repo에 포함되어 있어야 한다
dokcer hub login -> docker file을 이용하여 docker image 생성 -> docker hub에 push
``` yml
    # 1-6. 도커 이미지 빌드 및 이미지 게시
    - name: build docker image & push image to docker hub
      run: |
        docker login -u ${{ secrets.DOCKER_USERNAME }} -p ${{ 
        secrets.DOCKER_PASSWORD }}
        docker build -f Dockerfile -t ${{ secrets.DOCKER_USERNAME }}/cicdtest 
        docker push ${{ secrets.DOCKER_USERNAME }}/cicdtest
```

<font color="#00b0f0">git actions에서 직접 EC2 접근</font>
(<font color="#d094db">CodeDeploy와 비교</font>)
CodeDeploy 구조를 사용하지 않고 <font color="#92d050">git actions에서 직접 ssh 통신을 통해서</font>
<font color="#92d050">EC2에 접근</font>하여 docker hub에서 image를 pull하고 container 배포 환경을
구성하게 할 수도 있다
다만, 해당 방식은 단일 EC2 환경 및 초기 서비스 구축 단계에서 자주 사용하고,
서비스 고도화 단계에선 무중단 배포, 보안, 다중 EC2 관리 목적으로 CI/CD의
CD단계는 CodeDeploy를 사용하는 것이 일반적이다
(deployment agent 동작은 <font color="#92d050">.zip에 appspec.yml 및 .sh를 넣어서 s3로 전달하는 것이 일반적</font>)
(git actions에서 직접 SSH로 EC2에 접근하는 script code)
``` java
    steps:
    # 2-1. EC2 서버 접속 및 도커 작업 처리
    - name: SSH로 EC2에 접속하여 도커 작업 처리하기
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_IP }}
        username: ${{ secrets.SSH_USER }}
        key: ${{ secrets.SSH_PRIVATE_KEY }}
```

(<font color="#d094db">.sh, .yml 위치</font>)
CodeDeploy agent를 사용하여 CD를 구축하는 경우, .Zip에 넣을 파일들 위치는
<font color="#92d050">appspec.yml은 root</font>에, <font color="#92d050">다른 sh는 appspec.yml의 location에 지정한 위치</font>에 있어야 한다


<font color="#de7802">::: 블로그 (2) (CICD)</font>
https://rlaehddnd0422.tistory.com/217
<font color="#00b0f0">gradle cache 코드</font>
이전 runner에서 다운 받은 gradle 의존성을 GitHub Actions 캐시 서비스에
저장하여 다음 실행에 사용한다 -> git actions workflow 시간 크게 단축된다
``` java
- name: Setup Java JDK 17  
  uses: actions/setup-java@v4 // @v1을 @v4로 변경
  with:  
    distribution: 'temurin'  
    java-version: '17'

// 기본값으로 Gradle User Home 캐시 처리됨  
- name: Setup Gradle (with caching)  
  uses: gradle/actions/setup-gradle@v4
```


#### <font color="#de7802">::: RDS(MySQL을 담은 instance)를 EC2와 연결</font>
<font color="#00b0f0">블로그 Reference</font>
https://inpa.tistory.com/entry/AWS-%F0%9F%93%9A-RDS-%EA%B0%9C%EB%85%90-%EC%95%84%ED%82%A4%ED%85%8D%EC%B3%90-%EC%A0%95%EB%A6%AC-%EC%9D%B4%EB%A1%A0%ED%8E%B8 (이론)
https://taehoon9393.tistory.com/415 (설치1)
https://taehoon9393.tistory.com/416 (설치2)
https://taehoon9393.tistory.com/417 (설치3)

<font color="#00b0f0">RDS(MySQL 선택) 생성</font>
(환경 설정 매우 많음 위 블로그 참조 필수)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023005821045.webp]]

<font color="#00b0f0">security groups 설정 (EC2 -> Security Groups)</font>
RDS의 inbounds rule에서 칼하트 EC2의 접근을 허용할 수 있게 설정했다 
허용 범위로 carhartt EC2에 적용한 보안 그룹을 사용했다
(RDS instance에 적용한 보안 그룹의 inbound rules)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023195024290.webp]]
(EC2 instance에 적용한 보안 그룹의 inbound rules, <font color="#92d050">SSH는 내 PC IP로 제한 필요</font>)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023195745865.webp]]
(<font color="#ffff00">TODO: </font>다른 EC2 보안 그룹을 사용할 수도 있든데 해당 기능에 대해 더 찾아보기)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023145951289.webp]]

\* <font color="#00b0f0">VPC 설정</font>
만약, RDS Instance와 EC2가 같은 VPC에 있으면 EC2와 RDS는 private ip로 통신한다고 한다
VPC는 Vertual Private Cloud로 사용자 전용의 논리적 격리 네트워크를 지칭한다
같은 aws 계정이라고 해당 계정에서 생성한 EC2나 RDS, S3가 같은 VPC에 위치하는 것은
아니고, EC2나 RDS를 만들 때 동일한 VPC에 속하도록 직접 설정해야 한다
AWS VPC는 직접 만드는 것도 가능하다
(사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023162334090.webp]]
(사진2)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023162622726.webp]]

<font color="#00b0f0">RDS parameter groups 생성 -> actions -> edit로 들어가서 환경 설정</font>
character, time_zone, collation 등에 대한 설정을 하였다 (설정 사진은 너무 많아서 안 찍음)
(<font color="#ffff00">TODO :</font> 해당 공부도 필요)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023011644183.webp]]
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023143648937.webp]]
	
<font color="#00b0f0">database -> modify에 들어가서 셋팅한 parameter group로 재설정</font>
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023143811807.webp]]


<font color="#00b0f0">Application.properties 옵션 (git actions secrets)</font>
- spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver : <font color="#92d050">MySQL 8용 JDBC 드라이버 클래스를 명시</font>
- spring.datasource.url=jdbc:mysql://carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com:3306/carhartt_rds?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4 : <font color="#92d050">DB 접속 URL</font>
- spring.datasource.username : <font color="#92d050">db 이름</font>
- spring.datasource.password : <font color="#92d050">db password</font>
- spring.jpa.hibernate.ddl-auto=update : <font color="#92d050">테이블 자동 동기화</font>
- spring.jpa.show-sql=true : <font color="#92d050">실행 SQL을 로그에 출력(디버깅 편함)</font>
- spring.jpa.properties.hibernate.format_sql=true : SQL을 줄바꿈/들여쓰기해서 **가독성↑**
- spring.jpa.properties.hibernate.highlight_sql=true : <font color="#92d050">콘솔에 ANSI 컬러 하이라이트(키워드 강조)</font>
- spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect : Hibernate가 **MySQL 8에 최적화된 SQL**을 생성하도록 방언 명시
- spring.datasource.hikari.maximum-pool-size=10 : 동시에 빌려줄 수 있는 커넥션 최대치. 메모리/트래픽에 맞춰 조정
- spring.datasource.hikari.minimum-idle=2 : 유휴 상태로 유지할 커넥션 수. 급격한 트래픽 시 **풀 예열** 효과

\* MySQL은 h2처럼 spring 내장 browser console을 지원하지 않는다
대신 <font color="#92d050">MySQL WorkBench를 통해서 db를 확인</font>한다

(h2 -> mysql로 변경한 코드)
``` yml
# MySQL (완성되면 H2 삭제)
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com:3306/carhartt_rds?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4
spring.datasource.username=admin
spring.datasource.password=gjsxjsms123!

# JPA
# ddl-auto 옵션은 validate 권장
spring.jpa.hibernate.ddl-auto=update     
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.highlight_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Hikari (선택)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
```


<font color="#00b0f0">build.gradle Dependency 추가</font>
runtimeOnly 'com.mysql:mysql-connector-j' // EC2/prod에서 사용 
runtimeOnly 'com.h2database:h2' // local/dev에서 사용

\* runtimeOnly option을 사용하면 compile time에는 해당 의존성을 사용하지 않으며
runtime에만 사용된다


<font color="#00b0f0">EC2에 MySQL client 설치 및 연결 테스트 (성공)</font>
(TODO : EC2에 CA 인증서를 발급받은 다음 VERIFY_IDENTITY로 접속하기)
``` java
// 설치 코드
sudo apt-get update
sudo apt-get install -y mysql-client

// 설치 확인
mysql --version

// 접속 연결 확인, REQUIRED는 암호화는 하나 통신하는 서버가 내가 통신하려는 서버인지 검증은 하지 않음 CA인증서를 EC2에 발급 받은 뒤에 VERIFY_IDENTITY로 변경하기
mysql -h carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com \
      -u admin -p -P 3306 \
      --ssl-mode=REQUIRED \
      --ssl-ca=/etc/ssl/certs/ca-certificates.crt
```
(EC2에서 MySQL로 접속한 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023213920303.webp]]
(application.properites에도 ssl-mode=REQUIRED 적용 완료)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251023214532214.webp]]


### <font color="#de7802">::: .jar Docker image 만들고 CI(deploy.yml) 재구축</font>
<font color="#de7802">(Dockerfile 생성, aws ECR 생성, deploy.yml 수정, IAM role 수정)</font>
<font color="#00b0f0">docker file 작성</font>
최대한 오류 없이 범용적으로 사용할 수 있는 Docker file 코드이다
이름은 정확히 "Dockerfile"이어야 하며, 위치는 프로젝트 root로 지정했다
bootJar로 build를 하는 경우 .jar의 이름이 app.jar이 되도록 build.gradle에 고정했다
``` java
// build.gradle
tasks.named('bootJar') {  
    archiveFileName = 'app.jar'  
}
```

해당 docker file은 build에는 gradle:8.9-jdk17, 실행을 위한 container 환경 구축엔
eclipse-temurin:17-jre-jammy라는 base image를 사용한다
base image는 `docker build -t c_platform:latest .` 를 사용하면 자동으로 pull된다
(관련 코드)
``` java
# ---- Build stage ----
FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew
RUN gradle --version
RUN ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# bootJar 결과물이 app.jar로 고정되어 있으므로 정확히 지정
COPY --from=build /workspace/build/libs/app.jar /app/app.jar

# 가장 보수적인 기본값만
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```


<font color="#00b0f0">ECR 생성</font>
**ECR Repository 생성**
https://idea9329.tistory.com/620 <- (ECR 생성 blog1)
https://velog.io/@wonn23/ECR-%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0
<- (ECR 생성 blog2)

all service -> amazon ECR -> create repository 클릭 후 ECR 설정 후 create를 눌러 생성한다
(사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024161900716.webp]]

**EC2 rules 설정**
EC2 Instnace에서 ECR에 접근 및 처리할 수 있도록 rules에 AmazonEC2ContainerRegistryPowerUser permission을 추가했다
(사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024234334427.webp]]

<font color="#00b0f0">docker_compose.yml 작성</font>
docker_compose.yml은 여러 컨테이너를 한 번에 띄우는 경우,
실행 옵션 등을 (port, restart 정책, env 등) 선언적으로 설정하고 싶은 경우,
`docker compose up / down`의 명령어 하나로 docker build 및 run을 처리하고
싶은 경우 사용한다

현재 프로젝트에선 docker을 하나만 쓰기에 사용할 필요 없다
(연습으로 쓸까 했지만 굳이 큰 가치가 없을 것 같아서 사용 x)

<font color="#00b0f0">.github/workflows/deploy.yml 수정 (docker hub -> aws ECR 사용 결정)</font> (<font color="#d094db">중요</font>)
<font color="#92d050">기존 deploy.yml</font>
기존 CICD의 deploy.yml는 .jar을 포함한 github main repo를
모두 .zip으로 압축하여 s3에 전달하는 구조였다
(기존 코드)
``` yml
name: Build and Deploy Spring Boot to AWS EC2  
  
on:  
  push:  
    branches: [ main ]  
# 추가  
env:  
  PROJECT_NAME: Carhartt_platform  
  BUCKET_NAME: storage-for-ci-cd  
  CODE_DEPLOY_APP_NAME: for_CICD  
  DEPLOYMENT_GROUP_NAME: for_CICD  
# ---------------------  
jobs:  
  build:  
    runs-on: ubuntu-latest  
    steps:  
      - name: Checkout  
        uses: actions/checkout@v2  
  
      - name: Setup Java JDK 17  
        uses: actions/setup-java@v4  
        with:  
          distribution: 'temurin'  
          java-version: '17'  
  
      # 기본값으로 Gradle User Home 캐시 처리됨  
      - name: Setup Gradle (with caching)  
        uses: gradle/actions/setup-gradle@v4  
  
      # - name: Setup MySQL  
      #   uses: samin/mysql-action@v1      
      #   with:      
      #    character set server: 'utf8'      
      #    mysql database: 'rds'      
      #    mysql user: ${{user}}      
      #    mysql password: ${{password}}  
      
      - name: Grant execute permission for gradlew  
        run: chmod +x ./gradlew  
        shell: bash  
  
      - name: Remove test application.properties (for main branch only)  
        if: contains(github.ref, 'main') # main 브랜치에서만 실행  
        run: |  
          cd ./src/main/resources  
          if [ -f application.properties ]; then  
            rm application.properties  
            echo "✅ Deleted test application.properties"  
          else  
            echo "ℹ️ No test application.properties found"  
          fi  
        shell: bash  
  
      - name: Make application-prod.properties  
        if: contains(github.ref, 'main') # branch가 main 일 때  
        run: |  
          cd ./src/main/resources  
          touch ./application.properties  
          echo "${{ secrets.APPLICATION_PROPERTIES }}" > ./application.properties  
        shell: bash  
  
      # 스크립트 권한 부여  
      - name: Make scripts executable (and fix line endings)  
        run: |  
          sudo apt-get update && sudo apt-get install -y dos2unix  
          chmod +x scripts/*.sh  
          dos2unix scripts/*.sh  
        shell: bash  
  
      - name: Build with Gradle  
        run: ./gradlew build -x test  
        shell: bash 
         
      # 추가  
      - name: Make Zip File  
        run: zip -qq -r ./$GITHUB_SHA.zip .  
        shell: bash  
  
      - name: Configure AWS credentials  
        uses: aws-actions/configure-aws-credentials@v4  
        with:  
          aws-access-key-id: ${{ secrets.ACCESS_KEY_ID }}  
          aws-secret-access-key: ${{ secrets.SECRET_ACCESS_KEY }}  
          aws-region: ap-northeast-2  
  
      - name: Upload to S3  
        run: aws s3 cp --region ap-northeast-2 ./$GITHUB_SHA.zip s3://$BUCKET_NAME/$PROJECT_NAME/$GITHUB_SHA.zip  
  
      - name: Code Deploy  
        run: aws deploy create-deployment --application-name $CODE_DEPLOY_APP_NAME --deployment-config-name CodeDeployDefault.OneAtATime --deployment-group-name $DEPLOYMENT_GROUP_NAME --s3-location bucket=$BUCKET_NAME,bundleType=zip,key=$PROJECT_NAME/$GITHUB_SHA.zip  
# ---------------------
```

<font color="#92d050">변경 후 deploy.yml</font>
**전체 변경 사항**
변경된 deploy.yml은 Dockerfile을 Docker image로 build한 다음
aws ECR로 push하고, CD에서 사용할 appspec.yml 및 스크립트들만
.zip으로 압축하여 s3에 보냄으로써 deployment agent가 사용할 수 있도록 했다
(변경된 전체 deploy.yml 코드)
``` YML
name: Build and Deploy Spring Boot to AWS EC2  
  
on:  
  push:  
    branches: [ main ]  
# 추가  
env:  
  PROJECT_NAME: Carhartt_platform  
  BUCKET_NAME: storage-for-ci-cd  
  CODE_DEPLOY_APP_NAME: for_CICD  
  DEPLOYMENT_GROUP_NAME: for_CICD  
  AWS_REGION: ap-northeast-2  
  # 🔧 ECR 리포지토리 이름  
  ECR_REPOSITORY: c-platform  
  # 커밋 SHA를 이미지/배포 태그로 사용  
  COMMIT_SHA: ${{ github.sha }}  
  
# ------------------------------- 기본 셋팅 ------------------------------------  
jobs:  
  build:  
    runs-on: ubuntu-latest  
    steps:  
      - name: Checkout  
        uses: actions/checkout@v2  
  
      - name: Setup Java JDK 17  
        uses: actions/setup-java@v4  
        with:  
          distribution: 'temurin'  
          java-version: '17'  
  
      # 기본값으로 Gradle User Home 캐시 처리됨  
      - name: Setup Gradle (with caching)  
        uses: gradle/actions/setup-gradle@v4  
  
      # - name: Setup MySQL  
      #   uses: samin/mysql-action@v1      #   with:      #    character set server: 'utf8'      #    mysql database: 'rds'      #    mysql user: ${{user}}      #    mysql password: ${{password}}  
      - name: Grant execute permission for gradlew  
        run: chmod +x ./gradlew  
        shell: bash  
  
      # secret action variable에 저장된 .properties를 쓰기 위해 기존꺼 삭제  
      - name: Remove test application.properties (for main branch only)  
        if: contains(github.ref, 'main') # main 브랜치에서만 실행  
        run: |  
          cd ./src/main/resources  
          if [ -f application.properties ]; then  
            rm application.properties  
            echo "✅ Deleted test application.properties"  
          else  
            echo "ℹ️ No test application.properties found"  
          fi  
        shell: bash  
  
      # secret action variable에서 .properties를 가져옴  
      - name: Make application-prod.properties  
        if: contains(github.ref, 'main') # branch가 main 일 때  
        run: |  
          cd ./src/main/resources  
          touch ./application.properties  
          echo "${{ secrets.APPLICATION_PROPERTIES }}" > ./application.properties  
        shell: bash  
  
      # AWS 접근 권한 부여 (계정 rule)      - name: Configure AWS credentials  
        uses: aws-actions/configure-aws-credentials@v4  
        with:  
          aws-access-key-id: ${{ secrets.ACCESS_KEY_ID }}  
          aws-secret-access-key: ${{ secrets.SECRET_ACCESS_KEY }}  
          aws-region: ap-northeast-2  
  
      # ------------------------------- ECR에 Docker Image push ------------------------------------  
      # ECR 로그인 (레지스트리 URI를 outputs로 제공)  
      - name: Login to Amazon ECR  
        id: login-ecr  
        uses: aws-actions/amazon-ecr-login@v2  
  
      # Docker 이미지 빌드 & 태깅  
      - name: Build Docker image  
        run: |  
          ECR_REGISTRY=${{ steps.login-ecr.outputs.registry }}  
          docker build -t $ECR_REGISTRY/${ECR_REPOSITORY}:${COMMIT_SHA} .  
        shell: bash  
  
      # ECR로 푸시  
      - name: Push Docker image to ECR  
        run: |  
          ECR_REGISTRY=${{ steps.login-ecr.outputs.registry }}  
          docker push $ECR_REGISTRY/${ECR_REPOSITORY}:${COMMIT_SHA}  
        shell: bash  
  
      # ------------------------------- CD 사용 bundle s3에 전송 ------------------------------------  
      # 스크립트 권한 부여 / 개행 표준화  
      # dos2unix는 줄바꿈 형식을 unix가 사용하는 LF로 표준화  
      # 이후 step을 위해 zip Package도 설치  
      - name: Make scripts executable (and fix line endings)  
        run: |  
          sudo apt-get update && sudo apt-get install -y dos2unix zip  
          chmod +x scripts/*.sh  
          dos2unix scripts/*.sh  
        shell: bash  
  
	# 디렉토리 구성 (appspec.yml + 필요한 스크립트만 수집)  
	# CD에서 사용하기 위해 COMMIT_SHA, IMAGE_URI 각각의 값을 저장하고 있는 파일생성 
	- name: Stage bundle (appspec.yml + scripts)  
	  run: |  
	    mkdir -p bundle/scripts  
	    cp ./appspec.yml bundle/appspec.yml  
	    cp ./scripts/deploy.sh bundle/scripts/deploy.sh  
	    cp ./scripts/nginx_setup.sh bundle/scripts/nginx_setup.sh  
	      
	    echo "${COMMIT_SHA}" > bundle/COMMIT_SHA  
	    echo "${{ steps.login-ecr.outputs.registry }}/${ECR_REPOSITORY}:${COMMIT_SHA}" > bundle/IMAGE_URI  
	  shell: bash 
  
      # ZIP 생성  
      - name: Create bundle zip  
        run: |  
          cd bundle  
          zip -qq -r ${COMMIT_SHA}.zip .  
          mv ./${COMMIT_SHA}.zip ../${COMMIT_SHA}.zip  
        shell: bash  
  
      - name: Upload to S3  
        run: aws s3 cp --region ap-northeast-2 ./${COMMIT_SHA}.zip s3://$BUCKET_NAME/$PROJECT_NAME/${COMMIT_SHA}.zip  
  
      - name: Code Deploy  
        run: aws deploy create-deployment --application-name $CODE_DEPLOY_APP_NAME --deployment-config-name CodeDeployDefault.OneAtATime --deployment-group-name $DEPLOYMENT_GROUP_NAME --s3-location bucket=$BUCKET_NAME,bundleType=zip,key=$PROJECT_NAME/${COMMIT_SHA}.zip  
# ---------------------
```

**commit SHA**
아래 코드를 사용하면 CI/CD를 트리거 한 PR의 commit sha 값이
자동으로 IMAGE_TAG에 들어간다
사용할 때엔 {{ env.IMAGE_TAG }} 코드를 사용하면 된다
``` yml
env:  
  // ~
  # 이미지 태그는 커밋 SHA 사용  
  IMAGE_TAG: ${{ github.sha }}
```

**ECR에 연결**
ECR에 docker image를 push할 수 있도록 연결 해주는 step이다
이전 스탭에서 `aws-actions/configure-aws-credentials@v4`를 통해서
aws에 대한 인증을 마친 상태에서 사용해야 한다
<font color="#ffc000">id는 해당 step에 대한 식별 값으로, 해당 step의 outputs 값을</font>
<font color="#ffc000">다음 step에서 사용할 때에 사용</font>된다
(`aws-actions/amazon-ecr-login@v2`는 outputs 값으로 Registry URL을 반환한다
ex : 703671891666.dkr.ecr.ap-northeast-2.amazonaws.com)
``` yml
# ECR 로그인 (레지스트리 URI를 outputs로 제공)  
- name: Login to Amazon ECR  
  id: login-ecr  
  uses: aws-actions/amazon-ecr-login@v2  
```

**Docker Image build**
Dockerfile을 사용해서 Docker Image를 build하는 step이다
<font color="#ffc000">ECR에 image를 push하기 위해선 image를 아래와 같은 형식</font>으로 naming해야 한다
`[레지스트리 URL]/[리포지토리]:[태그]`
ex: `703671891666.dkr.ecr.ap-northeast-2.amazonaws.com/c-platform:abc123`

`{{ steps.login-ecr.outputs.registry }}`를 사용하면 이전에 실행한
Login to Amazon ECR step의 outputs를 가져온다
`${{ env.~ }}, $~ `는 이전에 정의해둔 환경 변수 값을 가져온다

해당 step에서 생성된 docker image는 사용자가 접근할 수 있는 파일 구조가 아니라
git actions runner에 설치된 <font color="#ffc000">docker demon이 캐시에 이름:tag 구조로 저장하여 관리</font>한다
``` yml
# Docker 이미지 빌드 & 태깅  
- name: Build Docker image  
  run: |  
    ECR_REGISTRY=${{ steps.login-ecr.outputs.registry }}  
    docker build -t $ECR_REGISTRY/${{ env.ECR_REPOSITORY }}:${{ env.IMAGE_TAG }} .  
  shell: bash
```

**aws ECR로 Image push**
생성한 image 이름을 가져와서 ECR에 push한다
``` yml
      # ECR로 푸시
      - name: Push Docker image to ECR
        run: |
          ECR_REGISTRY=${{ steps.login-ecr.outputs.registry }}
          docker push $ECR_REGISTRY/${{ env.ECR_REPOSITORY }}:${{ env.IMAGE_TAG }}
        shell: bash
```

**S3에 전달할 bundle 디렉토리 구성**
/appspec.yml과 /scripts/deploy.sh, /scripts/nginx_setup.sh를 /bundle에 담는다
/bundle 및 /bundle/scripts는 해당 step의 실행 중에 생성한다
CD에서 <font color="#ffc000">deploy.sh가 ECR에서 docker Image를 pull할 때 사용해야 할 IMAGE_URI 값</font>도
파일에 담아서 /bundle에 위치 시켰다 (EC2에 전달해 주기 위해)
이후에 필요할지도 모르기에 COMMIT_SHA값도 파일에 담아서 /bundle에 위치 시켰다
``` yml
# 디렉토리 구성 (appspec.yml + 필요한 스크립트만 수집)
# CD에서 사용하기 위해 COMMIT_SHA, IMAGE_URI 각각의 값을 저장하고 있는 파일 생성
- name: Stage bundle (appspec.yml + scripts)  
  run: |  
    mkdir -p bundle/scripts  
    cp ./appspec.yml bundle/appspec.yml  
    cp ./scripts/deploy.sh bundle/scripts/deploy.sh  
    cp ./scripts/nginx_setup.sh bundle/scripts/nginx_setup.sh 
    echo "${COMMIT_SHA}" > bundle/COMMIT_SHA  
	echo "${{ steps.login-ecr.outputs.registry }}
		/${ECR_REPOSITORY}:${COMMIT_SHA}" > bundle/IMAGE_URI 
  shell: bash
```

\* git actions에서 각 step은 서로 다른 shell process에서 실행된다
그렇기에 cd, export, set -e와 같은 값들은 다음 step에선 초기화 된다
디렉토리 구조 변경과 같은 작업들은 초기화 되지 않는다


<font color="#00b0f0">AWS IAM Users, Roles 수정</font>
**내가 만든 IAM User에 permission 추가** 
git actions에서 사용하고 있는 github-action-for-ci-cd IAM User에
AmazonEC2ContainerRegistryPowerUser permission을 추가했다
aws-actions/amazon-ecr-login@v2를 사용하기 위해 필요하다
(github-action-for-ci-cd IAM User dashborad 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025025242231.webp]]


**내가 만든 custom role 사진** 
IAM -> Roles에 내가 생성한 custom Roles를 수정하려고 한다
(custom Role인 codeDeployRole, S3Acess_and_DeployControl 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024203954274.webp]]

**CodeDeploy Group(Agent가 사용)에 적용할 role**
codeDeployRole는 for_CICD CodeDeploy Application가 가지고 있는
for_CICD CodeDeploy Gruops에 적용하고 있는 Role이다
CodeDeploy Gruops는 AWSCodeDeployRole만 적용시키면 된다
(for_CICD CodeDeploy Gruops dashboard 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024204627026.webp]]
(내가 만든 custom codeDeployRole role 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024213139145.webp]]

**EC2에 적용한 role**
S3Aceess_and_DeployControl는 EC2에 적용하고 있는 Role이다
EC2는 ECR에서 image를 pull해야 하기에 기존 permissions에 추가로
AmazonEC2ContainerRegistryPoserUser role을 추가했다
(EC2 dashborad 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024213840736.webp]]
(내가 만든 custom S3Access_and_DeployControl role 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251024213531597.webp]]


\* <font color="#00b0f0">CodeDeploy Group agent가 EC2 및 S3 파일 위치를 특정할 수 있는 원리</font> (<font color="#d094db">중요</font>)
git actions에서 `aws deploy create-deployment`를 호출하여 agent에게 배포 명령을
내릴 때, 인자 값으로 S3 위치를 명시한다
(upload를 할 때 사용한 값인 `s3://$BUCKET_NAME/$PROJECT_NAME/${COMMIT_SHA}.zip`를
모두 건네 줌 : <font color="#92d050">BUCKET_NAME : $BUCKET_NAME , key=$PROJECT_NAME/${COMMIT_SHA}.zip</font>)

또한 EC2는 CodeDeploy Group 창에서 Environment configuration: Amazon EC2 instances에
<font color="#92d050">특정 EC2가 가지고 있는 tag를 넣음으로써 특정</font>한다

Deploy agent가 <font color="#92d050">s3에서 가져온 파일을 EC2의 어디에 위치시킬지는 appsec.yml에 명시</font>되어 있다
``` java
- source: / // S3에 들어있는 파일 위치 지정 (루트를 지정했으니 전부)  
  destination: /home/ubuntu/carhartt_platform // S3 파일을 EC2 어디에 둘지 지정
  overwrite: true // 기존 파일 덮어씌움
```


### <font color="#de7802">::: Deploy Agent가 사용할 CD bundle 수정</font>
<font color="#00b0f0">이전에 appspec.yml에서 실행한 deploy.sh 코드</font>
기존 deploy.sh는 S3에서 받은 .jar을 실행하는 동작을 책임졌다
``` yml
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
  
echo "> JAR: $JAR_PATH"  
JAR_PATH="$(ls -tr "$REPOSITORY"/*.jar | tail -n 1)"  
  
echo "> 실행권한 추가"  
chmod +x "$JAR_PATH"  
  
echo "> 실행 시작"  
nohup java -jar "$JAR_PATH" >> "$LOG" 2>&1 < /dev/null &  
echo "> started (pid $!)"
```

<font color="#00b0f0">변경한 deploy.sh 코드</font>
변경된 deploy.sh는 aws ECR에서 Docker Image를 pull한 뒤에
해당 Image로 contaimer을 띄우는 동작을 책임진다
(변경된 전체 deploy.sh code)
``` yml
#!/usr/bin/env bash
set -euo pipefail

APP_HOME="/home/ubuntu/carhartt_platform"
IMAGE_URI="$(cat "$APP_HOME/IMAGE_URI")"        # CI가 넣어준 완전한 ECR 이미지 URI
CONTAINER_NAME="carhartt-platform"              # 컨테이너 이름(원하는 이름)
AWS_REGION="ap-northeast-2"                     # 또는 환경/파일로 주입

echo "[deploy] Using image: $IMAGE_URI"

# 1) ECR 로그인 (EC2 인스턴스 롤에 ecr:GetAuthorizationToken 등 Pull 권한 필수)
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"

# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"

# 3) 기존 컨테이너 중지/삭제(있다면)
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# 4) 새 컨테이너 실행 (포트/환경변수는 서비스에 맞게 조정)
docker run -d --name "$CONTAINER_NAME" \
  --restart=always \
  -p 8080:8080 \
  "$IMAGE_URI"

echo "[deploy] Container $CONTAINER_NAME started."
```

<font color="#00b0f0">Image URL 저장 (ECR Image URL 형식 : [레지스트리 URL]/[리포지토리]:[태그])</font>
`cat "$APP_HOME/IMAGE_URI"` 를 사용하면 /home/ubuntu/carhartt_platform/MAGE_URI에 저장되어 있는 IMAGE_URI 값이 표준 출력 된다
$(~)을 사용하면 ~값이 변수에 담기게 된다
즉, cat으로 출력한 IMAGE_URI 값(CI에서 준)이 최종적으로 IMAGE_URI 변수에 담긴다
마지막으로 "~"에 감싸서 특수문자나 공백이 있어도 한 문자열로 인식하게 한다
``` JAVA
APP_HOME="/home/ubuntu/carhartt_platform"
IMAGE_URI="$(cat "$APP_HOME/IMAGE_URI")" 
```

<font color="#00b0f0">aws ECR 로그인</font>
아래 코드를 통해서 aws ECR에 임시로 접근할 수 있는 권한을 얻는다

`aws ecr get-login-password --region "$AWS_REGION"`를 실행하면 ECR에
접근할 수 있는 임시 token이 생성된다

아래 코드는 생성된 token을 파이프로 받아서 특정 ECR에 접속한다
`docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"`

`"$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"`를 실행하면 IMAGE_URI에서 REPOSITORY_URI만 추출할 수 있고, 이를 통해서 특정 ECR Registry를 특정할 수 있다  
``` JAVA
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$(echo "$IMAGE_URI" | awk -F/ '{print $1}')"
```

<font color="#00b0f0">Image Pull한 뒤 Application을 실행하는 Docker container 생성</font>
ECR에서 이미지를 다운 받은 뒤에 기존 컨테이너를 중지/삭제한다
이후 ECR에서 받은 Image를 사용하여 새로운 container을 실행한다
``` yml
# 2) 최신 이미지 Pull
docker pull "$IMAGE_URI"  
  
# 3) 기존 컨테이너 중지/삭제(있다면)  
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true  
  
# 4) 새 컨테이너 실행 (포트/환경변수는 서비스에 맞게 조정)  
docker run -d --name "$CONTAINER_NAME" --restart=always -p 8080:8080 "$IMAGE_URI"
```

\* <font color="#00b0f0">Docker Container Application Log 보기</font>
java -jar로 .jar을 실행할 때엔 표준 출력 및 표준 에러를 server_log.txt로
리다이렉트하여 log를 저장했다
Docker Container에서 실행한 Application의 log는 아래 명령어로 확인할 수 있다
``docker logs <컨테이너 이름 또는 ID>``

### <font color="#de7802">::: EC2 환경 셋팅</font>
<font color="#00b0f0">AWS CLI v2 설치</font>
AWS 서비스를 터미널에서 제어할 수 있게 해주는 툴이다
deploy.sh에서` aws ecr get-login-password --region "$AWS_REGION"` 명령어를
사용하고 있기에 aws CLI tool 설치가 필요하다
(설치한 후 version 확인한 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025000545654.webp]]

<font color="#00b0f0">codedeploy-agent 활성화 확인</font>
`sudo service codedeploy-agent status` 명령어를 통해서 확인하였다
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025000928310.webp]]

<font color="#00b0f0">EC2에 docker 엔진 설치 및 ubuntu 사용자가 docker 권한</font>
(계정 출력 및 ECR 로그인 성공)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025012329956.webp]]

<font color="#00b0f0">이미지 아키텍처 불일치 문제 해결</font>
https://yunja.tistory.com/24 <- (해당 문제 해결한 블로그, runner을 arm64로 바꿨음)
git actions Runner 환경도 ubuntu이고 EC2도 ubuntu이다
그러나 CPU 아키텍쳐가 EC2는 arm64(aarch64), GitHub Runner은 amd64이기에
amd64에서 build한 Docker image는 EC2의 arm64환경에서 실행되지 않는다

git actions Runner 환경을 amd64 -> arm64로 바꾸는 방식도 가능하다
그러나 시간이 없으니 gpt가 생성한 steps를 추가하는 방식으로 해결했다
Set up QEMU, Set up Docker Buildx는 다른 아키텍처로 빌드하기 위한 도구를 설치하는
step이며, Build and Push (arm64)는 arm64로 docker image를 생성 후 바로 push한다
``` java
// ECR 로그인 뒤에 추가
- name: Set up QEMU
  uses: docker/setup-qemu-action@v3

- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3
  
  ------------------
  
  // Docker Image를 build 후 바로 push
  - name: Build and Push (arm64)  
    run: |  
    ECR_REGISTRY=${{ steps.login-ecr.outputs.registry }}  
    docker buildx build --platform linux/arm64 -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${COMMIT_SHA} --push .
  
```


### <font color="#de7802">::: 트러블 슈팅</font>
<font color="#de7802">Application과 MySQL 연결 문제</font>
아래 명령어를 사용하니 컨테이너는 살아 있었다
log를 보니 db와 연결하기 위한 url 설정에서 characterEncoding=utf8mb4를 사용하여
에러가 뜨고 있었다 (java 표준 character는 UTF-8, MySQL 표준이 utf8mb4임)

아래 코드처럼 아예 characterEncoding를 지정하지 않고 빼버리니 정상 작동됐다
`spring.datasource.url=jdbc:mysql://carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com:3306/carhartt_rds?sslMode=REQUIRED&serverTimezone=UTC`
``` java
// 컨테이너가 살아 있는지
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
// container 내에서 발생한 표준 출력/에러 log 확인
sudo docker logs --tail=200 carhartt-platform
```

<font color="#de7802">application 재배포 시에 data가 초기화 되는 문제</font>
기분 탓이었는지 모르겠지만 CICD를 할 때 마다 db data가 변경되는 것 같은 느낌을 받았다
그래서 RDS가 실제로 재부팅 되고 있는지 확인해 보았다 (10.22일에 RDS를 처음 실행함)

아래 명령어를 사용하면 duration 20160분 (14일) 내에서
RDS에서 발생한 Event를 확인할 수 있다 (Restart, create, backup, finish)
(RDS event확인 명령어)
``` java
aws rds describe-events \
  --source-identifier carharttdb \
  --source-type db-instance \
  --duration 20160 \
  --region ap-northeast-2 \
  --query "Events[*].[Date,Message]" \
  --output table
```

해당 명령어를 EC2 terminal에서 사용하기 위해선
EC2 Role에 RDS관련 permissions를 추가해야 한다
(AmazonRDSReadOnlyAccess 추가)
![[칼하트 인프라 변경 (docker, RDS, ECR 사용)-20251029130559460.webp]]

해당 permissions를 추가한 뒤에 명령어를 통해 EC2 로그를 확인하였다
확인 결과 RDS는 처음 Start한 뒤로 한 번도 restart를 하지 않고 있다
(RDS event 로그)
![[칼하트 인프라 변경 (docker, RDS, ECR 사용)-20251029124821767.webp]]

<font color="#00b0f0">원인 파악 완료 후 문제 해결</font>
이번엔 Application log를 확인해 보았다
확인 결과, <font color="#92d050">Hibernate에서 db table을 drop</font>하고 있는 것을 확인하였다
application.properties엔 DDL(Data Definition Language)가 아래처럼 설정되어 있다
`spring.jpa.hibernate.ddl-auto=update`
create, creat-drop만 table을 drop후 다시 만들고 update는 기존 table을 유지하는 속성이다
즉, application.properties 단에선 제대로 설정을 하였으나 어딘가에서 해당 옵션을
create, creat-drop로 덮어 씌우고 있다고 판단했다
(application 사진)
![[칼하트 인프라 변경 (docker, RDS, ECR 사용)-20251106145644726.webp]]

확인 결과 <font color="#92d050">application-oauth2-prod.yml에서 ddl-auto를 create로 덮어 씌우고</font> 있었다
application.properties 설정만을 사용하기 위해 아래 설정을(jap: 부분) 지우고 테스트했다
그 결과, 정상적으로 데이터가 유지되는 것을 확인할 수 있었다
(application-oauth2-prod.yml 사진)
![[칼하트 인프라 변경 (docker, RDS, ECR 사용)-20251106153237652.webp|575]]

<font color="#00b0f0">기존 데이터 복구</font>
drop된 기존 data에 대해선 <font color="#92d050">RDS Snapshot을 사용하여 복구</font>할 수 있는지 알아보았다
찾아본 결과, AWS는 Snapshot을 사용하여 현재 사용 중인 RDS를 덮어 씌우는 기능을
아예 지원하지 않는다 (안전성 측면에서)
오직 <font color="#92d050">Snapshot으로 새로운 RDS instance를 만들고 이를 Application의 db로 재연결</font>하는
방식만 지원한다
해당 방식은 추가 비용이 발생하기에 (RDS instance를 만들기에) 포기하였다

-> RDS 자체가 아니라 RDS에 띄우고 있는 MySQL의 data를 `mysqldump` 명령어를 
사용하여 복사할 수 있다
<font color="#92d050">demon crom 프로그램을 사용하여 이틀에 한 번 자동으로 MySQL data를 복사</font>하도록
설정하면 필요할 때 RDS snapshot을 사용하지 않아도 mysql dump파일을 사용하여
이전 data로 백업할 수 있다
(MySQL 복구 명령어)
`mysqldump -h "${DB_HOST}" -u "${DB_USER}" -p"${DB_PASS}" --single-transaction --routines --events "${DB_NAME}" | gzip > "${FILE_PATH}"
`

### <font color="#de7802">::: 구조 변경 후 Swagger UI 접속 성공 및 Container 기반 배포 성공 사진</font>
 characterEncoding=utf8mb4를 변경하고 다시 배포하니 접속 성공했다
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025033505240.webp]]
(docker container ls)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025123224364.webp]]

<font color="#92d050">container 내로 들어가는 명령어</font> : `docker exec -it carhartt-platform bash
(container 내부 process 및 container를 종료하지 않고 나오는 명령어 : <font color="#92d050">ctrl + D</font>)`
<font color="#92d050">8080 점유 aplication 확인 :</font> sudo lsof -i :8080
\* EC2 호스트에서 `sudo lsof -i :8080`을 사용해도 container 내의 application 확인 가능
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025124320070.webp]]

<font color="#92d050">docker container 내에서 발생한 로그 확인</font> : `sudo docker logs --tail=200 carhartt-platform`
\* `sudo docker logs -f carhartt-platform` 사용하면 실시간 로그 확인 가능

(lsof를 사용하기 위해 설치한 패키지)
``` java
apt-get update -y
apt-get install -y --no-install-recommends lsof
rm -rf /var/lib/apt/lists/*
```
(로그 사진)

![[칼하트 인프라 변경 (docker, RDS, ECR 사용)-20251106145535636.webp]]


### <font color="#de7802">::: RDS MySQL data 확인</font>
<font color="#00b0f0">CLI로 RDS MySQL database를 확인</font> 
아래 명령어를 통해 CLI 접근을 사용할 수 있다
```java
// mysql client 설치
sudo apt-get update -y
sudo apt-get install -y mysql-client

// mysql에 접속 ; 비밀번호 gjsxjsms123!
mysql -h carharttdb.cl4eesq4iow3.ap-northeast-2.rds.amazonaws.com \
      -P 3306 -u admin -p \
      --ssl-mode=REQUIRED carhartt_rds
```

접속 후 아래 명령어로 database 조회 가능하다
``` java
SHOW DATABASES; // database 종류

USE carhartt_rds; // 칼하트 프로젝트에서 쓰는 DB로 전환

SHOW TABLES; // 테이블 목록 조회

SELECT * FROM member LIMIT 10; // member 레코드 조회
```
(조회 성공한 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025035701664.webp]]

<font color="#00b0f0">Local에서 MySQL Workbrench로 RDS 조회</font>
local의 Mysql Workbranch로도 RDS의 MySQL에 접근하는 것이 가능하다
접근 경로는 Local -> EC2 -> RDS 이다
먼저 SSH로 EC2에 접근하기 위해 EC2 Elastic IP와 Username(ubuntu)를 기입한다
SSH key file도 넣는다
MySQL hostname과 port, Username도 기입한다 (SSH 접근 후 MySQL에 접근할 때 사용)
이렇게 기입하고 Test Connection을 클릭하면 MySQL password를 입력하라는 창이 뜬다
비밀번호 입력하면 정상 접근 된다
(Workbrench에서 RDS로 접근하기 위해 설정하는 창 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025045722677.webp]]
(MySQL workbranch로 RDS에 접근한 사진)
![[형식파일들/이미지/aws에 docker hub 사용 (+CICD)-20251025045727718.webp]]




---
**더 찾아볼 것 - 생각해볼 것 :**
## 출처 : 