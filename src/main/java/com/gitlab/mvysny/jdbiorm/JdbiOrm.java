package com.gitlab.mvysny.jdbiorm;

import com.gitlab.mvysny.jdbiorm.quirks.DatabaseQuirksDetectorJdbiPlugin;
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant;
import com.gitlab.mvysny.jdbiorm.quirks.Quirks;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    /**
     * The validator, used to validate entity when {@link Entity#save(boolean)} is invoked.
     * Defaults to JSR 303 validator; if JSR303 is not available then falls back to {@link NoopValidator}.
     */
    @Nullable
    private static volatile Validator validator;
    @Nullable
    private static volatile DataSource dataSource;
    // beware - we must use a singleton instance of Jdbi. If we don't, the transaction nesting will not work
    // since Jdbi ThreadLocals are not static but bound to the Jdbi instance.
    private static volatile Jdbi jdbi;
    /**
     * If set to non-null, this takes precedence over {@link DatabaseVariant} detection mechanism.
     */
    @Nullable
    public static volatile Quirks quirks = null;
    /**
     * If set to non-null, this takes precedence over {@link DatabaseVariant} detection mechanism.
     * Filled in by {@link #setDataSource(DataSource)}.
     */
    @Nullable
    public static volatile DatabaseVariant databaseVariant = null;

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

    /**
     * Returns the current validator, used to validate entity when {@link Entity#save(boolean)} is invoked.
     * Defaults to JSR 303 validator; if JSR303 is not available then falls back to {@link NoopValidator}.
     * @return the current validator, not null.
     */
    @NotNull
    public static Validator getValidator() {
        return validator;
    }

    /**
     * Sets a new custom validator, used to validate entity when {@link Entity#save(boolean)} is invoked.
     * Defaults to JSR 303 validator; if JSR303 is not available then falls back to {@link NoopValidator}.
     * @param validator the new validator to set, not null.
     */
    public static void setValidator(@NotNull Validator validator) {
        JdbiOrm.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * Returns the data source currently used by {@link #jdbi()}. We highly recommend
     * to use a connection pooler such as HikariCP.
     * @return the data source, not null.
     */
    @NotNull
    public static DataSource getDataSource() {
        return Objects.requireNonNull(dataSource, "The data source has not been set. Please call JdbiOrm.setDataSource() first.");
    }

    /**
     * Sets the data source which will be used by {@link #jdbi()} from now on. We highly recommend
     * to use a connection pooler such as HikariCP.
     * <p></p>
     * Tests the data source and fills in {@link #databaseVariant}.
     * @param dataSource the data source, not null.
     */
    public static void setDataSource(@NotNull DataSource dataSource) {
        if (JdbiOrm.dataSource != dataSource) {
            Objects.requireNonNull(dataSource, "dataSource");
            destroy();
            JdbiOrm.dataSource = dataSource;
            jdbi = Jdbi.create(dataSource);
            jdbi.installPlugin(new DatabaseQuirksDetectorJdbiPlugin());

            // verify the data source and detect the variant
            jdbi().inTransaction(handle -> {
                JdbiOrm.databaseVariant = DatabaseVariant.from(handle);
                return null;
            });
        }
    }

    /**
     * Returns the Jdbi instance. Just static-import this method for easy usage.
     * @return the Jdbi instance, not null.
     */
    @NotNull
    public static Jdbi jdbi() {
        return Objects.requireNonNull(jdbi, "The data source has not been set. Please call JdbiOrm.setDataSource() first.");
    }

    /**
     * Closes the current {@link #getDataSource()}. Does nothing if data source is null (has not been set).
     */
    public static void destroy() {
        jdbi = null;
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
