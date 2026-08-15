package com.campus.trade.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    int maxRequests() default 5;

    int seconds() default 60;
}
