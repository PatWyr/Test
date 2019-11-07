package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.annotation.Unmappable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field with this to exclude it from being mapped into a database table column.
 * Similar to {@link Unmappable} but this annotation targets fields.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
}
