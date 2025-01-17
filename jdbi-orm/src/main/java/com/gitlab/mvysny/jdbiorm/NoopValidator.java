package com.gitlab.mvysny.jdbiorm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import java.util.Collections;
import java.util.Set;

/**
 * No-op validator which always passes all objects.
 * @author mavi
 */
public class NoopValidator implements Validator {
    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return Collections.emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return Collections.emptySet();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return Collections.emptySet();
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        throw new UnsupportedOperationException("unimplemented");
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new ValidationException("unsupported " + type);
    }

    @Override
    public ExecutableValidator forExecutables() {
        throw new UnsupportedOperationException("unimplemented");
    }
}
