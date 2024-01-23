package com.gitlab.mvysny.jdbiorm;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.gitlab.mvysny.jdbiorm.JdbiOrm.jdbi;

public class DaoOfJoin<T> extends DaoOfAny<T> {
    @NotNull
    private final String sql;

    public DaoOfJoin(@NotNull Class<T> entityClass, @NotNull @Language("sql") String sql) {
        super(entityClass);
        this.sql = Objects.requireNonNull(sql);
    }

    @Override
    public @NotNull List<T> findAll(@Nullable String orderBy, @Nullable Long offset, @Nullable Long limit) {
        if (limit != null && limit == 0L) {
            return new ArrayList<>();
        }
        checkOffsetLimit(offset, limit);
        final StringBuilder sql = new StringBuilder(this.sql);
        return jdbi().withHandle(handle -> {
                    if (orderBy != null) {
                        sql.append(" ORDER BY ").append(orderBy);
                    }
                    // H2 requires ORDER BY after LIMIT+OFFSET clauses.
                    appendOffsetLimit(sql, handle, offset, limit, orderBy != null);

                    return handle.createQuery(sql.toString())
                            .map(getRowMapper())
                            .list();
                }
        );
    }
}
