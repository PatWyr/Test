package com.gitlab.mvysny.jdbiorm;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return jdbi().withHandle(handle -> handle.createQuery(sql)
                .define("WHERE", "")
                .define("ORDER", orderBy == null ? "" : orderBy)
                .define("PAGING", getOffsetLimit(handle, offset, limit, orderBy != null))
                .map(getRowMapper())
                .list()
        );
    }
}
