package com.gitlab.mvysny.jdbiorm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field with this to exclude it from being mapped into a database table column.
 * @deprecated replaced by JdbiProperty(map = false)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated()
public @interface Ignore {
}
