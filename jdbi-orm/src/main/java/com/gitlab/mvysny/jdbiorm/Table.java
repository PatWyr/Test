package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation which allows you to change the database table name.
 * <p></p>
 * Use the {@link ColumnName} annotation to change the name of the column.
 * @author mavi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    /**
     * The database table name; defaults to an empty string which will use the {@link Class#getSimpleName()} as the table name.
     */
    String value() default "";
}
