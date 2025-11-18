JsonUsernamePasswordAuthenticationFilter bean을 생성할 때 authenticationManager가 이미 bean으로 등록되어 있는데
authenticationManager를 등록하라는 에러가 뜨는 것 같아 이유가 뭔지 찾아줘
----

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2025-11-18T09:45:52.398+09:00 ERROR 9188 --- [C_platform] [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.context.ApplicationContextException: Unable to start web server
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:170) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:621) ~[spring-context-6.2.9.jar:6.2.9]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:439) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:318) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1361) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1350) ~[spring-boot-3.5.4.jar:3.5.4]
at com.C_platform.CPlatformApplication.main(CPlatformApplication.java:15) ~[main/:na]
Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:147) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:107) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:517) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:219) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:193) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:167) ~[spring-boot-3.5.4.jar:3.5.4]
... 8 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jsonUsernamePasswordAuthenticationFilter' defined in class path resource [com/C_platform/Member_woonkim/infrastructure/auth/config/LocalAuthConfig.class]: authenticationManager must be specified
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1826) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:607) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:529) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:339) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:373) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:337) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:207) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.boot.web.servlet.ServletContextInitializerBeans.getOrderedBeansOfType(ServletContextInitializerBeans.java:230) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:184) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAsRegistrationBean(ServletContextInitializerBeans.java:179) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.ServletContextInitializerBeans.addAdaptableBeans(ServletContextInitializerBeans.java:164) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.ServletContextInitializerBeans.<init>(ServletContextInitializerBeans.java:96) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.getServletContextInitializerBeans(ServletWebServerApplicationContext.java:271) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.selfInitialize(ServletWebServerApplicationContext.java:245) ~[spring-boot-3.5.4.jar:3.5.4]
at org.springframework.boot.web.embedded.tomcat.TomcatStarter.onStartup(TomcatStarter.java:52) ~[spring-boot-3.5.4.jar:3.5.4]
at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4464) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:164) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1203) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1193) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:749) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:772) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:164) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1203) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1193) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na]
at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:749) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:203) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:164) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.StandardService.startInternal(StandardService.java:412) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:164) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:870) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:164) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.apache.catalina.startup.Tomcat.start(Tomcat.java:438) ~[tomcat-embed-core-10.1.43.jar:10.1.43]
at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:128) ~[spring-boot-3.5.4.jar:3.5.4]
... 13 common frames omitted
Caused by: java.lang.IllegalArgumentException: authenticationManager must be specified
at org.springframework.util.Assert.notNull(Assert.java:181) ~[spring-core-6.2.9.jar:6.2.9]
at org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.afterPropertiesSet(AbstractAuthenticationProcessingFilter.java:199) ~[spring-security-web-6.5.2.jar:6.5.2]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1873) ~[spring-beans-6.2.9.jar:6.2.9]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1822) ~[spring-beans-6.2.9.jar:6.2.9]
... 51 common frames omitted


Process finished with exit code 1
