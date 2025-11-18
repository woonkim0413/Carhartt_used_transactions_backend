# Project: Carhartt 중고 거래 플랫폼
## 프로젝트 개요
## Tools
## 아키텍처
<img width="1531" height="1005" alt="Image" src="https://github.com/user-attachments/assets/64ce5bab-2761-4c54-83e5-b4e0b0010eea" />

<img width="2320" height="1342" alt="Image" src="https://github.com/user-attachments/assets/ecee9151-e87b-408c-972a-4997a027f277" />
## ERD
## 기능 소개
## 멤버
## 팀 주요 논의
### 공통
#### S3 이미지 업로드 서버리스 구조 채택
**Situation**
FE에서 상품 이미지를 S3에 업로드할 때, 리사이징 처리를 백엔드(Spring)에서 수행해 달라는  요청이 있었습니다. 하지만 해당 방법이 최선인지에 대한 고민이 있었습니다.
 **Action**
 다음 세 가지 대안을 검토했습니다.
1. 백엔드(Spring) 라이브러리 기반 리사이징 후 S3 업로드
2. 서버를 거쳐 AWS Lambda에서 이미지 리사이징 수행
3. 서버리스 방식으로 FE에서 직접 S3에 업로드, 업로드 전 FE 라이브러리로 리사이징
세 가지 방안을 검토한 결과, 서버 경유는 불필요한 지연과 비용이 발생한다는 결론을 내렸고 **FE 서버리스 방식**을 채택했습니다. 
### 개인
## 회고
