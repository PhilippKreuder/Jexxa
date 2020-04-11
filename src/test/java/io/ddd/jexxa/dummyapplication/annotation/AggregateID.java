package io.ddd.jexxa.dummyapplication.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Represents an ApplicationService in tests
 *
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Documented
public @interface AggregateID
{
}