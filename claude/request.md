2025-11-18T17:14:04.678+09:00  INFO 18068 --- [C_platform] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2025-11-18T17:14:05.023+09:00  INFO 18068 --- [C_platform] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2025-11-18T17:14:05.060+09:00  INFO 18068 --- [C_platform] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2025-11-18T17:14:05.280+09:00  INFO 18068 --- [C_platform] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:testdb user=SA
2025-11-18T17:14:05.282+09:00  INFO 18068 --- [C_platform] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2025-11-18T17:14:05.308+09:00  WARN 18068 --- [C_platform] [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2025-11-18T17:14:05.330+09:00  INFO 18068 --- [C_platform] [           main] org.hibernate.orm.connections.pooling    : HHH10001005: Database info:
Database JDBC URL [Connecting through datasource 'HikariDataSource (HikariPool-1)']
Database driver: undefined/unknown
Database version: 2.3.232
Autocommit mode: undefined/unknown
Isolation level: undefined/unknown
Minimum pool size: undefined/unknown
Maximum pool size: undefined/unknown
2025-11-18T17:14:06.642+09:00  INFO 18068 --- [C_platform] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2025-11-18T17:14:06.701+09:00  INFO 18068 --- [C_platform] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2025-11-18T17:14:06.952+09:00  INFO 18068 --- [C_platform] [           main] o.s.d.j.r.query.QueryEnhancerFactory     : Hibernate is in classpath; If applicable, HQL parser will be used.
2025-11-18T17:14:06.966+09:00  INFO 18068 --- [C_platform] [           main] r$InitializeUserDetailsManagerConfigurer : Global AuthenticationManager configured with UserDetailsService bean with name localUserDetailsService
2025-11-18T17:14:07.394+09:00  WARN 18068 --- [C_platform] [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'OAuth2UseCase' defined in file [C:\Users\User\IntellijProjects\carhartt\C_platform\build\classes\java\main\com\C_platform\Member_woonkim\application\useCase\OAuth2UseCase.class]: Unsatisfied dependency expressed through constructor parameter 4: Error creating bean with name 'oauthClientRegister' defined in file [C:\Users\User\IntellijProjects\carhartt\C_platform\build\classes\java\main\com\C_platform\Member_woonkim\infrastructure\register_and_oauthClients\OauthClientRegister.class]: Unsatisfied dependency expressed through constructor parameter 0: Error creating bean with name 'kakaoOAuthClient' defined in file [C:\Users\User\IntellijProjects\carhartt\C_platform\build\classes\java\main\com\C_platform\Member_woonkim\infrastructure\register_and_oauthClients\KakaoOAuthClient.class]: Unsatisfied dependency expressed through constructor parameter 1: No qualifying bean of type 'org.springframework.web.client.RestTemplate' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
2025-11-18T17:14:07.395+09:00  INFO 18068 --- [C_platform] [           main] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2025-11-18T17:14:07.398+09:00  INFO 18068 --- [C_platform] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2025-11-18T17:14:07.401+09:00  INFO 18068 --- [C_platform] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
2025-11-18T17:14:07.402+09:00  INFO 18068 --- [C_platform] [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2025-11-18T17:14:07.414+09:00  INFO 18068 --- [C_platform] [           main] .s.b.a.l.ConditionEvaluationReportLogger :

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2025-11-18T17:14:07.440+09:00 ERROR 18068 --- [C_platform] [           main] o.s.b.d.LoggingFailureAnalysisReporter   :

***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 1 of constructor in com.C_platform.Member_woonkim.infrastructure.register_and_oauthClients.KakaoOAuthClient required a bean of type 'org.springframework.web.client.RestTemplate' that could not be found.


Action:

Consider defining a bean of type 'org.springframework.web.client.RestTemplate' in your configuration.


Process finished with exit code 1
