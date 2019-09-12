package com.gitlab.mvysny.jdbiorm;

import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import javax.validation.NoProviderFoundException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * Initializes the ORM in the current JVM. By default uses the {@link HikariDataSourceAccessor} which uses [javax.sql.DataSource] pooled with HikariCP.
 * To configure this accessor, just fill in [dataSourceConfig] properly and then call [init] once per JVM. When the database services
 * are no longer needed, call {@link #destroy()} to release all JDBC connections and close the pool.
 *
 * If you're using a customized [DatabaseAccessor], you don't have to fill in the [dataSourceConfig]. Just set proper [databaseAccessorProvider]
 * and then call [init].

 * @author mavi
 */
public final class JdbiOrm {
    private static volatile Validator validator;
    private static volatile DataSource dataSource;

    private static final Logger log = LoggerFactory.getLogger(JdbiOrm.class);
    static {
        try {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        } catch (NoProviderFoundException ex) {
            log.info("JSR 303 Validator Provider was not found on your classpath, disabling entity validation. See https://github.com/mvysny/vok-orm#validations for more details.");
            log.debug("The Validator Provider stacktrace follows", ex);
            validator = new NoopValidator();
        }
    }

    @NotNull
    public static Validator getValidator() {
        return validator;
    }

    public static void setValidator(@NotNull Validator validator) {
        JdbiOrm.validator = Objects.requireNonNull(validator);
    }

    @NotNull
    public static DataSource getDataSource() {
        return Objects.requireNonNull(dataSource, "The data source has not been set. Please call Jdbiorm.get().setDataSource() first.");
    }

    public static void setDataSource(@NotNull DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        destroy();
        JdbiOrm.dataSource = dataSource;
    }

    /**
     * Returns the Jdbi instance. Just static-import this method for easy usage.
     * @return
     */
    @NotNull
    public static Jdbi jdbi() {
        return Jdbi.create(getDataSource());
    }

    /**
     * Closes the current {@link #getDataSource()}. Does nothing if data source is null (has not been set).
     */
    public static void destroy() {
        if (dataSource != null && dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
