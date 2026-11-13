package com.mtg.mtgwalletbe.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {

    /**
     * Set to false when the method arguments contain secrets or personal data
     * (tokens, links with tokens, email addresses). The request is then stored as redacted,
     * and exception details are reduced to the exception class name.
     */
    boolean logArgs() default true;

    /**
     * Only used when logArgs is false: indexes of arguments that are safe to log as-is.
     */
    int[] safeArgs() default {};

    /**
     * Only used when logArgs is false: indexes of email arguments to log masked (j***@example.com).
     */
    int[] emailArgs() default {};

    /**
     * Set to false when the return value contains secrets or personal data.
     */
    boolean logResult() default true;
}
