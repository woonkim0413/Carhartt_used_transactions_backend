현재 Local 로그인 정상 동작한다

현재 로컬 로그인 코드에서 로그인 할 때에 email을 검증하는 로직을 추가하려고 한다

구현 전 정보는 아래와 같다

------
API 뼈대는 LocalAuthController 내에 있는 아래 두 handler이다 

    @GetMapping("/email/random_code")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> sendRandomCodeToEmail(
            @Valid @RequestBody SendRandomCodeToEmailDto sendRandomCodeToEmailDto
            ) {
        // 구현
    }

    @GetMapping("/email/verification")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> randomCodeVerification(
            @Valid @RequestBody RandomCodeVerificationDto randomCodeVerificationDto
            ) {
        //  구현
    }
-----

email 관련 스프링 의존성은 gradle에 추가했다

implementation 'org.springframework.boot:spring-boot-starter-mail' // mail 인증 의존성

-----

application.properties에는 google SMTP를 사용하기 위한 설정을 추가해놨다

# mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=gamegemos588@gmail.com
spring.mail.password=hivomevsyumxdbzi

spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.auth=true

------

현재 정보를 바탕으로 v1/local/email/random_code, v1/local/email/random_code 코드를 설계해줘

설계한 내용을 /claude/email_summary.md에 저장해줘
