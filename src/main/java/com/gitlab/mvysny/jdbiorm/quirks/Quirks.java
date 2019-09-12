package com.gitlab.mvysny.jdbiorm.quirks;

import org.jdbi.v3.core.config.Configurable;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * @author mavi
 */
public interface Quirks {
    boolean shouldActivate(@NotNull Connection dataSource) throws SQLException;
    void configure(@NotNull Configurable<?> jdbi);

    Quirks NO_QUIRKS = new Quirks() {
        @Override
        public boolean shouldActivate(@NotNull Connection dataSource) throws SQLException {
            return false;
        }

        @Override
        public void configure(@NotNull Configurable<?> jdbi) {
        }
    };

    /**
     * @todo mavi use service loader
     */
    List<Quirks> ALL_QUIRKS = Arrays.asList(new MySqlQuirks());
}
