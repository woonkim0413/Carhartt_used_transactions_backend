
# *Tags :* 
# *linked file :* 
# *reference address :*
---
# < 내용 >
<font color="#de7802">설명 (칼하트 중고 거래 프로젝트 중)</font>
EC2와 local은 사용하는 db가 다르고, URL이 다르다
(EC2는 mySQL 사용 예정, local은 H2 사용)
(EC2는 public IP 사용, local은 localhost:8080 사용)

또한 EC2의 .propetes에는 S3 key가 실려 있으나,
local의 .propertes는 github에 올라가기에 뺐다

그러므로 설정을 분리할 필요성을 느꼈다


<font color="#de7802">물리적으로 .propeties 분리 (칼하트 프로젝트에 적용)</font>
칼하트 프로젝트에선 물리적으로 .properties를 분리했다

local properitse는 .ignore에 포함시켜 local에만 사용하고 github에는
안 올라가도록 설정했다

EC2 .properties는 github settings -> Secrets and Variable -> Actions에
업로드하여 CI/CD를 할 때 적재되도록 하였다

이를 통해 local 및 EC2 환경에 따라 .properties를 완전히 분리하였다


<font color="#de7802">논리적으로 .propertis 분리 (동희님이 알려주신 방법)</font>
<font color="#00b0f0">배경 지식</font>
스프링은 런타임에 ConfigurableEnvironment라는 환경설정 파일을 생성한다
환경설정 파일의 값은 스프링이
<font color="#92d050">1)</font> --K=V > <font color="#92d050">2)</font> -K=V > <font color="#92d050">3)</font> 환경 변수 <font color="#92d050">4)</font> .properties 우선순위 순으로 읽어서 작성한다

`java --spring.profiles.active=dev -jar my_Application.jar`처럼 
--를 두 개를 사용하면 @springApplication에서 실행한 main method의 
arg로 전달된다 

`java -Dspring.profiles.active=dev -jar my_Application.jar`처럼 
-를 한 개 사용하면 JVM의 시스템 속성(System Properties) 테이블에 전달된다
spring이 환경설정 파일을 만들 때 `System.getProperty("spring.profiles.active")`를
호출하여 값을 꺼낸다

\* Dspring의 D는 Define의 약자다

<font color="#00b0f0">환경설정 파일 분리</font>
**로컬 실행**
default로는 .properites에 `srping.profiles.active=local`로 설정한다
--나 -을 사용하여 환경 변수 값을 건네주지 않는 이상 local로 실행된다

**EC2 실행**
CI/CD를 진행할 때엔 github/workflows/deploy.yml에 
`./gradlew clean build -x test. -PjarName=myapp.jar` 
해당 명령어로 build 파일을 만들고 생성된 .jar 파일을 S3로 전송 후에,
aws Deployment agent가 사용하는 deploy.sh에 아래 명령어를 넣어 실행한다
`java -Dspring.profiles.active=dev -jar myapp.jar`

\* build 명령어에서 clean은 ~/build/ 아래 파일들을 지우는 명령어다
(이전 build 산출물 모두 삭제)
<font color="#92d050">-x test는</font> 작성한 단위 테스트 코드를 실행하지 않는다는 명령어다
<font color="#92d050">-P는</font> gradle에 값을 건네주는 명령어다 (build파일 이름을 명령어 실행 시점에 건네줌)
<font color="#92d050">-D는</font> JVM에 값을 건네주는 명령어다

**환경설정 파일 분기**
java/Resource 아래에 application-main.properties, application-oauth2-main.yml
, application-local.properties, application-oauth2-local.properties, application.properties
총 5개의 환경 설정 파일을 위치시킨다

application.properties에 아래 두 줄을 추가한다
해당 줄을 추가하면 .jar을 실행할 때 JVM에 어떤 값을 건네주는 지에 따라
사용되는 환경 파일이 분기 된다

`java -Dapp.env=local -jar myapp.jar`로 실행하면
application-local.properties와 application-oauth2-local.yml이 사용된다 
`java -Dapp.env=main -jar myapp.jar`로 실행하면
application-main.properties와 application-main-local.yml이 사용된다
(application.properites 파일 코드)
``` properties
# 환경 변수 callback 문법; -D로 건네주면 그 값 사용, 없다면 default로 local
# app.emv로 local, main 값을 사용
app.env=${app.env:local}

# app.env에 담긴 값을 사용
spring.profiles.active=${app.env},oauth2-${app.env}
```

\* **spring.profiles.active 값에 따른 파일 읽기 규칙**
spring.profiles.active 값에 따라 spring은 선택적으로 .properties/.yml을 읽는다

`application-{spring.profiles.active}.yml` 구조로 하이폰 다음에
active 값을 넣어야 spring이 해당 파일을 profiles에 해당하는 파일로 인식하여
사용한다

application.properties는 default값으로 active와 상관 없이 읽는다
(그렇기에 환경 설정 분기의 시작점으로 사용)


<font color="#de7802">활성화 된 .properites(or .yml)에서 값 가져오기</font>
==Reference from "SecurityConfig(Oauth+local 로그인 고려)"==
아래와 같이 @Value Annotation에 설정 파일 내 path를 넣음으로써,
활성화 된 설정 파일에 작성된 값을 가져올 수 있다 
(엄밀하게 말하면<font color="#92d050"> 활성화 된 설정 파일 값이 ConfigurableEnvironment에 들어가고</font>
<font color="#92d050">해당 코드는 이렇게 만들어진 통합 설정 파일에서 값을 가져온다</font>)
application-oauth2-local.yml의 app.identifier엔 local이,
application-oauth2-prod.yml의 app.identifier엔 pord가 작성되어 있다면
indentifier의 값도 어떤 설정 파일이 활성화 되었냐에 따라 local 및 prod를 가진다
(코드) ^a01936
``` java
@Value("${app.identifier}")  
private String identifier;

---------------------------------------------

@Value("${app.base-url}")
private String baseUrl;
```


---
**더 찾아볼 것 - 생각해볼 것 :**
## 출처 : 