package com.gitlab.mvysny.jdbiorm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.NoProviderFoundException;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Initializes the ORM in the current JVM. By default uses the [HikariDataSourceAccessor] which uses [javax.sql.DataSource] pooled with HikariCP.
 * To configure this accessor, just fill in [dataSourceConfig] properly and then call [init] once per JVM. When the database services
 * are no longer needed, call [destroy] to release all JDBC connections and close the pool.
 *
 * If you're using a customized [DatabaseAccessor], you don't have to fill in the [dataSourceConfig]. Just set proper [databaseAccessorProvider]
 * and then call [init].

 * @author mavi
 */
public class Jdbiorm {
    private Validator validator;
    private static final Logger log = LoggerFactory.getLogger(Jdbiorm.class);
    private Jdbiorm() {
        try {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        } catch (NoProviderFoundException ex) {
            log.info("JSR 303 Validator Provider was not found on your classpath, disabling entity validation. See https://github.com/mvysny/vok-orm#validations for more details.");
            log.debug("The Validator Provider stacktrace follows", ex);
            validator = new NoopValidator();
        }
    }

    private static final Jdbiorm INSTANCE = new Jdbiorm();

    public static Jdbiorm get() {
        return INSTANCE;
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }
}
