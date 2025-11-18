아니 나는 아래 코드가 아니라
@Override
protected void successfulAuthentication(...) {
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authResult);
SecurityContextHolder.setContext(context);

        // 🔧 주입된 인스턴스 사용 (매번 생성 안 함)
        securityContextRepository.saveContext(context, request, response);

        log.info("SecurityContext saved to session - email: {}", authResult.getName());

        super.successfulAuthentication(request, response, chain, authResult);
    }
}
아래 코드만 호출해도 정상 동작하기를 원해
super에서 ThreadLocal에 SecurityContext를 저장하고 응답을 보낼 때 이를 세션에 저장한 뒤 header에 실잖아 
왜 자꾸 명시적으로 SecurityContext를 내가 생성하라고 해 이런 코드는 좋지 않은 코드잖아
@Override
protected void successfulAuthentication(...) {
        super.successfulAuthentication(request, response, chain, authResult);
    }
}