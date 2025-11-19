
# *Tags :* 
# *linked file :* 
# *reference address :*

https://about-tikkle.tistory.com/18 , https://about-tikkle.tistory.com/25 <- 해당 블로그를 보고 작성한 node

---
# < 내용 >
# swagger 설명
swagger은 자동으로 API 명세서를 작성해주는 tool이다

Springdoc(OpenAPI) 라이브러리가 application 기동 시점에 controller class 내의 요청 mapping이 붙은 method를 전부 인식한다

해당 method의 URL 패턴과 이름을 인식한다 또한 return type의 내부 구조를 Reflection으로 인식하여 json schema를 만들어준다

swagger UI가 application을 인식하여 생성한 API 명세서를 url page로 띄우기 위해서는 application이 인터넷에 띄어져 있어야 한다
그렇기에 <font color="#92d050">swagger을 사용하기 위해서는 aws에 ci/cd를 구축해 놓아야 한다</font>

좀 더 구체적으론 특정 branch(보통 main)에 push가 발생하면 자동으로 application이 재배포 되게 함으로써 이에 따라 swagger UI가 생성하는
API 명세서도 update되도록 한다
(통용적으로 swagger는 사용 tool을 가리키며 openAPI는 API specification를 가리킴)

swagger을 사용하면 api 명세서를 자동으로 생성할 수 있을 뿐만 아니라 postman을 사용하지 않고 <font color="#92d050">swagger ui page에서 API를 test 해볼 수 있다</font>

# CI/CD 구축
swagger ui에 실시간으로 API의 변경점을 반영하도록 하기 위해선
EC2에 서비스를 배포해 놓고 main에 push가 발생할 때마다 git actions에서 이를 인식하여 재배포 하도록 세팅해야 한다
이를 위해서는 프로젝트를 CI/CD로 배포해야 한다

<font color="#de7802">CI/CD 과정 요약</font>
main에 push 발생 -> yaml에 정의된 event에 의해(if main branch 조건) workflow가 생성됨 -> VM에 ubuntu 설정, repo code 가져옴, gradle로 build함 -> .jar 파일 생성 -> .jar을 s3에 저장 -> s3의 .jar을 EC2에 가져옴 -> 배포 -> 배포된 프로젝트 code를 swwagger UI가 인식하고 이에 맞게 API 명세서를 자동으로 수정

# CI/CD를 위한 github actions .yaml 설명 
### <font color="#de7802">yaml 구조 키워드</font>
github actions는 yml에 정의된 트리거가 발생하면 yml에 정의된 동작을 수행한다
트리거는 <font color="#92d050">yml에서 on키워드를 사용</font>해서 정의한다

github action는 크게 <font color="#92d050">workflows > jobs > steps로 구분</font>한다
<font color="#92d050">workflow는</font> 자동화 파이프라인의 단위로써 yml하나 당 하나의 workflow가 생성된다

<font color="#92d050">job는</font> workflow안에 있는 실행 단위로 병렬 실행, 의존성 실행 등의 설정이 가능하다
yml에서 job은 jobs keyward로 정의한다 job은 몇 개의 step으로 이루어져 있다

<font color="#92d050">step는</font> job 내부에서 실행 단계를 정의하는 단위이다
yml에서 step은 steps keyward로 정의한다
![[github actions 설명-20250818131550869.webp|565]]

<font color="#92d050">event는</font> workflow를 실행시키는 trigger이다
event의 종류로는 특정 branch의 push나 PR, hook과 같은 동작이 있다

<font color="#92d050">Action은 </font>복잡하고 자주 반복되는 작업을 정의한 커스텀 어플리케이션이다
<font color="#ffc000">반복되는 코드를 재사용 할 수 있도록 도와주는 역할</font>을 한다
(일종의 함수)

github의 action은 <font color="#ffc000">공식 action과 서브파트 action으로 나뉜다</font>
공식 action은 github에서 공식적으로 지원하는 action을 뜻하고
서브파트 action은 누구나 만들어서 배포할 수 있는 action을 뜻한다

- **공식**: `actions/checkout`, `actions/setup-node` (GitHub에서 관리)
- **서드파티**: `samin/mysql-action`, `appleboy/ssh-action` (개인/회사에서 만든 액션)

action은 yml에서 <font color="#ffc000">uses 키워드를 통해서 사용할 수 있</font>다

### <font color="#de7802">CI/CD yml 문법 분석</font>
<font color="#de7802">github actions Runner (VM) 초기 셋팅</font>
<font color="#92d050">runs-on: </font>은 github actions에서 VM(러너) 내부를 어떤 실행 환경(os)으로 구축할지 지정하는 키워드이다

<font color="#92d050">uses: actions/checkout@vN</font>는 VM(러너)위에 올리는 node.js의 버전과 보안 방식, Repo에 코드를 끌어오는 과정(git init, git clone, git pull 등)을 지정한다 N에 따라 노드 버전 및 보안 방식 등에서 차이를 보인다

<font color="#92d050">uses: samin/mysql-action@v1</font>은 VM에 mysql을 설치해주는 키워드이다
블로그에선 해당 code를 사용했지만 현재 칼하트 프로젝트에선 mysql을
사용하지 않기에 해당 부분을 주석 처리했다
``` java
jobs: 
	build: 
		runs-on: ubuntu-latest 
		steps: 
			- name: Checkout 
			  uses: actions/checkout@v2

			- name: Setup Java JDK 17
			  uses: actions/setup-java@v3
			  with:
				java-version: '17'
			    distribution: 'temurin' // 또는 'zulu'
			    cache: gradle   // Gradle 캐시 자동 관리
			/*	현재는 MySQL을 사용하지 않기에 주석 처리
			- name: Setup MySQL 
		      uses: samin/mysql-action@v1
		      with: 
			      character set server: 'utf8' 
			      mysql database: 'rds' 
			      mysql user: ${{user}} 
			      mysql password: ${{password}}
			      */
```

<font color="#de7802">gradlew의 권한을 변경한 뒤 buile하는 코드 스냅</font>
<font color="#92d050">branch가 main인 경우만 동작</font>하며 <font color="#92d050">Application file에는</font>
<font color="#92d050">패스워드, 계정 정보, API key 등이 적혀져 있</font>기에 secret으로 관리하고
workflow 실행 시점에 주입한다
`gradlew build -x test`를 실행하면 <font color="#92d050">test를 생략하고 build</font>하기에
초기 베포 과정에서 빠르게 build 결과물을 생성한다
``` c
      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew
        shell: bash

      - name: Make application-prod.properties
        if: contains(github.ref, 'main') // main일 때만 실행함 사실 main branch에서만 .yml이 실행되기에 중복임
        run: |
          cd ./src/main/resources
          touch ./application-prod.properties
          echo "${{ secrets.APPLICATION }}" > ./application-prod.properties
        shell: bash

      - name: Build with Gradle
        run: ./gradlew build -x test
        shell: bash
```

<font color="#de7802">workflows 전체 압축</font>
github actions Runner(VM)에서 생성한 .jar file을 .zip으로 압축한다
.zip의 이름으로 commit SHA number을 사용한다

<font color="#92d050">.jar이 아니라 전체 파일을 압축</font>해야 배포를 위한 환경 및 설정들을
온전히 전달할 수 있으며 파일을 다른 환경으로 전송 및 다운로드 하기에도 더 용이하다
``` c
	- name: Make Zip File
        run: zip -qq -r ./$GITHUB_SHA.zip .
        shell: bash
```

<font color="#de7802">AWS 인증 설정 및 .ZIP을 S3에 업로드</font>
 <font color="#92d050">aws-actions/configure-aws-credentials@v4 액션을 사용</font>하여 workflows에서 aws를 컨트롤 할 수 있도록 인증 절차를 수행함
이때, 미리 aws에서 만들어 놓은 IAM User access key를 secrets에 등록해 놓고 이를 통해서 aws 접근 권한을 얻음
  
이후 <font color="#92d050">aws s3의 ap-northeast-2 region(대한민국)에 띄어져 있는 S3에 프로젝트 zip file을 전송</font>함
``` c
name: Build and Deploy Spring Boot to AWS EC2

on:
  push:
    branches: [ main ]
# 추가
env:
  PROJECT_NAME: Carhartt_platform // 프로젝트에 맞는 이름
  BUCKET_NAME: storage-for-ci-cd // S3 이름
  CODE_DEPLOY_APP_NAME: for_CICD // CodeDeploy application 이름
  DEPLOYMENT_GROUP_NAME: for_CICD // CodeDeploy application group 이름
  -------------------
  // 생략
  -------------------
  
  - name: Configure AWS credentials // aws 접근 권한 획득
  uses: aws-actions/configure-aws-credentials@v4  
  with:  
    aws-access-key-id: ${{ secrets.ACCESS_KEY_ID }}  
    aws-secret-access-key: ${{ secrets.SECRET_ACCESS_KEY }}  
    aws-region: ap-northeast-2
  
    - name: Upload to S3
    run: aws s3 cp --region ap-northeast-2 
    ./$GITHUB_SHA.zip 
    s3://$BUCKET_NAME/$PROJECT_NAME/$GITHUB_SHA.zip

```
![[CI-CD 구축 (swagger 목적) (github actions 사용)-20250822211619383.webp]]

<font color="#de7802">CodeDeploy에게 배포 요청</font>
배포 트리거가 되는 step으로 aws CodeDeploy에게 배포 명령을 내린다
이때, CodeDeploy에게 <font color="#92d050">CodeDeploy Application/Group 이름</font>, <font color="#92d050">S3 Bucket 이름 및 파일 path</font>, 
<font color="#92d050">배포 방식 등을 전달</font>한다
`--deployment-config-name CodeDeployDefault.OneAtATime` : 배포 그룹 내에 속한 EC2중 
무작위 내부 순서에 따라 하나의 EC2를 선택하여 배포하라는 디렉티브다
`bundleType=zip` : S3에서 가져올 파일이 압축되어 있음으로 가져와서 압축을 풀라는 디렉티브다

CodeDeploy가 EC2 내부에 설치되어 있는 CodeDeploy Agent에게 배포 명령을 하달하면 <font color="#92d050">Agent가 appspec.yml에 따라 배포를 진행</font>한다

`$CODE_DEPLOY_APP_NAME, $DEPLOYMENT_GROUP_NAME, $BUCKET_NAME, $PROJECT_NAME` 등은 deploy.yaml 상단에 정의해 두었다
`$GITHUB_SHA.zip`은 github에서 제공하는 환경 변수로, github commit SHA 숫자를 사용하여 ZIP 파일을 만들었다 
(deployment agent에 배포 명령을 전달하는 코드)
``` c
	   - name: Code Deploy
        run: aws deploy create-deployment --application-name $CODE_DEPLOY_APP_NAME --deployment-config-name CodeDeployDefault.OneAtATime --deployment-group-name $DEPLOYMENT_GROUP_NAME --s3-location bucket=$BUCKET_NAME,bundleType=zip,key=$PROJECT_NAME/$GITHUB_SHA.zip
```

### <font color="#de7802">appspec.yml 작성</font>
AWS CodeDeploy Agent가 수행할 작업을 지정하는 설계서로 어디서 파일을 가져와서 어떤 스크립트를 실행하여 배포할지에 관한 정보를 포함한다
project directory의 root에 위치 시켜야 한다
``` c
version: 0.0
os: linux
files:
  - source:  /
    destination: /home/ubuntu/carhartt_platform
    overwrite: yes // 기존 파일 덮어씌움

// 현재는 학습 목적이니 root 계정으로 spring boot 배포 파일을
// 실행하지만 실무에선 각 역할마다 전용 계정을 만들어서 필요한
// 권한만 부여한 후 사용하는 것이 일반적이다
// sudo adduser springboot --disabled-password --gecos ""
permissions:
  - object: /
    pattern: "**"
    owner: root
    group: root

hooks:
  AfterInstall:
    - location: scripts/deploy.sh // 실행할 스크립트
      timeout: 60
      runas: root
```

### <font color="#de7802">scripts/deploy.sh 작성</font>
appspec.yml가 트리거가 되어서 실행되는 스크립트이다
``` c
#!/usr/bin/env bash

REPOSITORY=/home/ubuntu/carhartt_platform
cd $REPOSITORY

echo "> Build 파일 복사"
cp ./build/libs /*.jar $REPOSITORY/ */

echo "> 현재 구동중인 애플리케이션 pid 확인"
CURRENT_PID=$(pgrep -fl carhartt_platform)
echo "$CURRENT_PID"

if [ -z $CURRENT_PID ]
then
  echo "> 종료할것 없음."
else
  echo "> kill -15 $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi

echo "> 새 어플리케이션 배포"
JAR_NAME=$(ls -tr $REPOSITORY/ | grep jar | tail -n 1)

echo "> JAR Name: $JAR_NAME"

echo "> $JAR_NAME에 실행권한 추가"
chmod +x $JAR_NAME

echo ">$JAR_NAME 실행"

nohup java -jar $JAR_NAME > /dev/null 2> /dev/null < /dev/null &
```

## CI/CD를 위한 AWS 설정
### <font color="#de7802">aws 용어</font>
<font color="#92d050">AWS CodeDeploy :</font> AWS가 제공하는 service
<font color="#92d050">CodeDeploy Agent :</font> 개별 EC2에 설치함
<font color="#92d050">EC2 :</font> Elastic Computer Cloud의 약자(C가 2개)로 aws가 제공하는 가상 컴퓨팅 시스템이다
<font color="#92d050">S3 :</font> Simple Storage Service의 약자로 aws에서 제공하는 객체 스토리지 서비스이다
<font color="#92d050">EBS :</font> Elastic Block Service의 약자로 EC2에서 persistent storage로 사용되는 서비스이다

### <font color="#de7802">EC2 생성 및 ssh client 연결</font>
[[4장(베포)(+칼하트 프로젝트)]]

### <font color="#de7802">S3 생성</font>
전체 services 목록 -> S3 -> Create buckets
![[CI-CD(swagger 목적) + github actions 설명-20250820221842709.webp]]

### <font color="#de7802">IAM 사용자 생성</font>
<font color="#de7802">IAM 사용자 설명</font>
IAM사용자는 <font color="#92d050">root 계정과 다르게 특정 권한만 가지고 있는 계정</font>이다
root 계정 하위에는 여러 개의 IAM사용자가 생성될 수 있으며 IAM사용자는
root 계정을 가지고 있는 사용자가 생성한다

IAM사용자를 사용하면 <font color="#92d050">협업 시에 팀원의 역할에 맞게 IAM사용자를 생성하여</font>
<font color="#92d050">나눠줌으로써 EC2 및 S3 등의 service를 나 포함 팀원들이 유지 및 관리할 수 있게 세팅</font>할 수 있다
(root계정 - IAM사용자 구조)
![[CI-CD(swagger 목적) + github actions 설명-20250820223035395.webp]]

\* <font color="#00b0f0">Permissions policies vs Permissions boundary</font>
<font color="#92d050">Permissions policies :</font> IAM 사용자에게 권한 부여
<font color="#92d050">Permissions boundary :</font> IAM 사용자가 갖을 수 있는 권한 바운더리 설정, 대규모 시스템에선 CI/CD 등 자동화 시스템에서 IAM 사용자가 생성되기도 하는데 이때 Permissions policies를 잘 못 주어 오버 권한을 갖는 IAM 사용자가 생성되는 것을 방지하기 위해 설정
(IAM 권한 정책 사진)
![[CI-CD(swagger 목적) + github actions 설명-20250820224844936.webp]]

<font color="#de7802">IAM 사용자 생성</font>
<font color="#92d050">github actions에서 E2C 및 S3에 접근하기 위해선 이에 맞는 IAM 사용자를 생성</font>해야 한다 

전체 services 목록 -> IAM -> Users -> Create user (IAM 사용자 생성)
![[CI-CD(swagger 목적) + github actions 설명-20250820223636919.webp]]

생성한 github-action-for-ci-cd IAM 사용자 클릭 -> Permissions policies -> add permissions -> create inline policy -> AmazonS3FullAccess, AWSCodeDeploy 추가 (github actions에서 S3 및 CodeDeploy에 접근 허용 목적)

<font color="#de7802">IAM 사용자 access key 생성</font> (<font color="#d094db">사용 이유 중요</font>)
아래 방식으로 생성한 Access keys는 <font color="#92d050">외부에서 해당 IAM 사용자에 대한 사용 권한을 획득할 때 </font>
<font color="#92d050">사용</font>된다 
IAM -> User -> 생성한 IAM 사용자 클릭 -> security credentials -> Access keys -> create access keys

\* <font color="#00b0f0">root access key 생성</font>
아래 방식으로 생성한 Access keys는 외부에서 해당 계정에 대한 root 사용 권한을 획득할 때 
사용한다 (<font color="#92d050">git actions에서 s3에 폴더를 올리고 deployment agent에게 배포 명령할 때 사용</font>)
프로필 -> security credentials -> create access keys
![[CI-CD(swagger 목적) + github actions 설명-20250821142947990.webp|362]]

\* <font color="#00b0f0">별 다른 설정 없이 S3 접근이 가능한 원리</font> (<font color="#d094db">중요, 쉽게 헷갈림</font>)
위 명령어에서 접근 권한을 획득한 IAM과 S3는 같은 콘솔 계정에 위치한다  
IAM Rule(Policy)에 `AmazonS3FullAccess`를 추가했기에 같은 계정에 있는 S3접근이
허용된다 그렇기에 IAM 권한을 얻은 것 만으로도 S3접근이 가능한 것이다

### <font color="#de7802">EC2에 IAM 권한 부여(IAM 사용자와 다름)</font>
github actions가 CodeDeploy 접근 및 S3에 file을 upload하기 위해서 IAM 사용자 권한을 사용한 것처럼,
(access key를 secret에 저장한 뒤 이를 사용하여 권한 획득) 

<font color="#92d050">EC2가 CodeDeploy의 명령을 전달 받고 S3에서 file을 dounload하기</font>
<font color="#92d050">위해서도 IAM 권한이 필요</font>하다 (<font color="#d094db">중요한 개념</font>)
(AWSCodeDeployFullAccess 권한은 필요 없을지 모르나 추가함)

<font color="#92d050">EC2에 IAM role을 추가하는 방법은 아래</font>와 같다
EC2 Instance -> Action -> Security -> Modify IAM role -> create New IAM role -> role 생성 후 드랍 박스 클릭하여 생성한 role 선택
![[CI-CD(swagger 목적) + github actions 설명-20250821152327713.webp]]
![[CI-CD(swagger 목적) + github actions 설명-20250821152333227.webp]]

### <font color="#de7802">EC2에 codeDeploy agent 설치</font>
codeDeploy service의 명령어를 인식하고 EC2에서 배포를 자동으로 실행하기 위해선 codeDeploy agent가 필요하다

codeDeploy agent는 <font color="#92d050">.zip에 들어있는 appspec.yml를 보고 배포를 진행</font>한다

아래 command를 EC2에서 차례로 실행하면  codeDeploy agent를 정상적으로 설치할 수 있다
``` c
sudo apt-get update && sudo apt-get upgrade

// jdk 17 다운로드, 프로젝트 jdk 버전과 통일시킴
sudo apt-get install openjdk-17-jdk

// URL에서 파일을 다운로드할 수 있는 CLI 도구
sudo apt install wget

// codeDeploy agent 파일 다운로드
sudo wget https:/\/aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install

sudo chmod +x ./install

sudo apt-get install awscli

// codeDeploy agent 생성
sudo ./install auto

// 서비스 시작/부팅연결
sudo service codedeploy-agent start
sudo systemctl enable codedeploy-agent

// 상태 확인
sudo service codedeploy-agent status

```
(codeDeploy agent가 active된 모습 사진)
![[CI-CD(swagger 목적) + github actions 설명-20250821164045764.webp]]

### <font color="#de7802">aws CodeDeploy Application + CodeDeploy group 생성</font>
<font color="#de7802">CodeDeploy Application 생성</font>
전체 service 목록 -> Developer Tools -> CodeDeploy -> 
Application -> Create application 클릭

<font color="#de7802">CodeDeploy group 생성</font>
배포할 때 instance를 중단한 뒤에 재배포 할지, 두 개의 instance를 사용하여 기존 blue instance를 유지하고 green에 배포 과정을 수행한 다음 배포 과정이 완전히 완료되면 blue를 stop하고 green으로 배포함으로써 무중단 배포할지 선택하는 option이다 
![[CI-CD(swagger 목적) + github actions 설명-20250821194211609.webp]]

deployment group에 속한 EC2에 대해서<font color="#92d050"> CodeDeploy agent를 자동으로 생성해 주고 최신 버전으로 관리</font> 해준다 - 수동으로 괜히 했다 ,,,
![[CI-CD(swagger 목적) + github actions 설명-20250821194232999.webp]]

<font color="#92d050">Load balancer 설정, 재배포 및 수정 과정에서 무중단 배포를 지원</font>한다
현재 프로젝트에선 load balance 기능이 필요하지 않으니 아래 체크된 "enable load balancing" box를 해제하는 것이 옳다
![[CI-CD(swagger 목적) + github actions 설명-20250821195039665.webp]]

<font color="#92d050">Deployment service가 어떤 EC2에게 배포 명령을 내릴지 지정</font>하는 option이다 (<font color="#d094db">중요</font>)
현재 나는 tag 기반 EC2탐색 방식을 선택했다 (key=name, value=karhart_paltform)
instance에도 똑같은 tag를 생성하면 Deployment service가 외부에서 배포 명령을 받을 시 
해당 EC2에 배포 명령을 하달한다
(설정 사진)
![[CI-CD(swagger 목적) + github actions 설명-20250821204738369.webp]]
(for_CICD group이 가진 instance tag 사진)
![[형식파일들/이미지/CI-CD 구축 (swagger 목적) (github actions 사용)-20251021155830133.webp]]
(instance에 설정한 tag 사진, groups tag와 갖기에 deployment agent의 배포 대상)
![[형식파일들/이미지/CI-CD 구축 (swagger 목적) (github actions 사용)-20251021155859365.webp]]


\* <font color="#00b0f0">CodeDeploy Application vs CodeDeploy group vs Deployment agnet차이</font>
<font color="#92d050">CodeDeploy Application는</font> 배포를 담는 논리적 그릇으로 서비스 당 하나를 사용한다
하위에 여러 CodeDeploy group이 담긴다
<font color="#92d050">CodeDeploy group은</font> 어떤 ec2에 어떤 동작으로 배포할지를 지정한다
하나의 CodeDeploy gruop는 여러 대상 EC2 집합으로 구성되어, 트리거 하나에 여러
ec2에 배포 동작을 가할 수 있다
<font color="#92d050">Deployment agent는</font> push가 group의 특정 ec2를 트리거 하면 실제 배포 스크립트를 
실행하는 주체이다

### <font color="#de7802">CI/CD + swagger 현재 구축 상황 </font>
<font color="#00b0f0">ci/cd 현재 수행 완료한 목록 :</font>
- aws instance (EC2) 생성
- 보안그룹 인바운드 규칙 생성 (8080, 22, 443, 80 port 열어둠) 
- S3 생성
- Codedeployment application 생성
- Codedeploy group 생성 후 tags를 배포할 ec2와 동일하게 설정
- ec2랑 github actions에는 s3 접근 및 codeDeploy에게 trigger을 줄 수 있도록 관련 권한을 갖는 IAM User을 생성 후 붙여줌
- github actions에서 사용할 yaml 및 CodeDeploy agent가 사용할 appspec.yaml 생성
- 민감한 파일 (application.properties, IAM access key 등) github secrets에 등록
- aws 명령어가 실행될 regions 명시 (S3, EC2, CodeDeploy regions 명시)

<font color="#00b0f0">ci/cd 현재 수행 완료한 목록 :</font>
- swagger 의존성 추가
  -> gradle에 `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")`를 추가함

<font color="#00b0f0">모든 세팅이 완료 되면 :</font>
swagger 의존성을 넣은 뒤에 http://<EC2_IP>:8080/swagger-ui/index.html 경로로 들어가면 swagger ui가 만들어준 api 명세서 페이지가 뜬다

### <font color="#de7802">CI/CD 구축 완료</font>
(http://3.35.168.54:8080/swagger-ui/index.html <- 정상적으로 뜸)
![[CI-CD(swagger 목적) + github actions 설명-20250822194922827.webp]]
(github actions build jop steps 모두 완료된 상태)
![[CI-CD(swagger 목적) + github actions 설명-20250822194959972.webp]]

### <font color="#de7802">참고 자료들</font>
https://velog.io/@jonghyun3668/SpringBoot-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-EC2-%EB%B0%B0%ED%8F%AC%ED%95%98%EA%B8%B0#3-1-%EA%B9%83-%EC%84%A4%EC%B9%98%ED%95%98%EA%B8%B0aws <- aws 배포 블로그

https://innovation123.tistory.com/191 <- swagger를 위한 CI/CD (docker 사용)

https://rlaehddnd0422.tistory.com/217 <- 실제 운영 중 사용할 수 있는 CI/CD 구축 블로그

https://back-stead.tistory.com/101 <- swagger 설정을 도와주는 블로그 (1)
gradle 의존성 추가 (springdoc) -> application.yaml 설정 추가
-> SwaggerConfig 파일 생성 -> Contoller method에 annotation 추가
-> swagger UI에 API 명세서 생성 

https://brunch.co.kr/@jamescompany/66 <- swagger 정보 블로그 (2)



---
**더 찾아볼 것 - 생각해볼 것 :**
## 출처 : 