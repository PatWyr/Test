package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseQuirksDetectorJdbiPlugin;
import com.gitlab.mvysny.jdbiorm.quirks.Quirks;
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
 * Initializes the ORM in the current JVM. Wraps any given {@link DataSource};
 * just use <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a>.
 * <p></p>
 * To configure JDBI-ORM, simply call {@link #setDataSource(DataSource)} once per JVM. When the database services
 * are no longer needed, call {@link #destroy()} to release all JDBC connections and close the pool.
 * <p></p>
 * See <a href="https://gitlab.com/mvysny/jdbi-orm-playground">JDBI-ORM Playground</a>
 * project for code examples and the quickest way to try out JDBI-ORM.
 * @author mavi
 */
public final class JdbiOrm {
    private JdbiOrm() {}
    private static volatile Validator validator;
    private static volatile DataSource dataSource;
    /**
     * If set to non-null, this takes precedence over {@link DatabaseQuirksDetectorJdbiPlugin} detection mechanism.
     */
    public static volatile Quirks quirks = null;

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
        JdbiOrm.validator = Objects.requireNonNull(validator, "validator");
    }

    @NotNull
    public static DataSource getDataSource() {
        return Objects.requireNonNull(dataSource, "The data source has not been set. Please call Jdbiorm.get().setDataSource() first.");
    }

    public static void setDataSource(@NotNull DataSource dataSource) {
        if (JdbiOrm.dataSource != dataSource) {
            Objects.requireNonNull(dataSource, "dataSource");
            destroy();
            JdbiOrm.dataSource = dataSource;
        }
    }

    /**
     * Returns the Jdbi instance. Just static-import this method for easy usage.
     * @return the Jdbi instance, not null.
     */
    @NotNull
    public static Jdbi jdbi() {
        final Jdbi jdbi = Jdbi.create(getDataSource());
        jdbi.installPlugin(new DatabaseQuirksDetectorJdbiPlugin());
        return jdbi;
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
            dataSource = null;
        }
    }
}
