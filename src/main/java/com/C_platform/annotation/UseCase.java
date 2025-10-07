package com.C_platform.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface UseCase {
    @AliasFor(annotation = Component.class, attribute = "value") // @UseCase("beanName") 처럼 이름 지정 가능하게 별칭 제공
    String value() default "";
}