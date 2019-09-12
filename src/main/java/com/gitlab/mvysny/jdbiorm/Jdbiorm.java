package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.validation.NoProviderFoundException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Objects;

/**
 * Initializes the ORM in the current JVM. By default uses the {@link HikariDataSourceAccessor} which uses [javax.sql.DataSource] pooled with HikariCP.
 * To configure this accessor, just fill in [dataSourceConfig] properly and then call [init] once per JVM. When the database services
 * are no longer needed, call [destroy] to release all JDBC connections and close the pool.
 *
 * If you're using a customized [DatabaseAccessor], you don't have to fill in the [dataSourceConfig]. Just set proper [databaseAccessorProvider]
 * and then call [init].

 * @author mavi
 */
public class Jdbiorm {
    private volatile Validator validator;
    private volatile DataSource dataSource;

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

    @NotNull
    public Validator getValidator() {
        return validator;
    }

    public void setValidator(@NotNull Validator validator) {
        this.validator = Objects.requireNonNull(validator);
    }

    @NotNull
    public DataSource getDataSource() {
        return Objects.requireNonNull(dataSource, "The data source has not been set. Please call Jdbiorm.get().setDataSource() first.");
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns the Jdbi instance. Just static-import this method for easy usage.
     * @return
     */
    @NotNull
    public static Jdbi jdbi() {
        return Jdbi.create(get().getDataSource());
    }
}
