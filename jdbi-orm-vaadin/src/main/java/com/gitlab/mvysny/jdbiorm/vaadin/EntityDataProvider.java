package com.gitlab.mvysny.jdbiorm.vaadin;

import com.gitlab.mvysny.jdbiorm.DaoOfAny;
import com.gitlab.mvysny.jdbiorm.OrderBy;
import com.gitlab.mvysny.jdbiorm.condition.Condition;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.SerializableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides instances of given entity {@link T}. Accepts filter of type {@link Condition}.
 * Use {@link #withStringFilter} to use this data provider easily from a ComboBox.
 * Use [setSortFields] to set the default record ordering.
 */
public class EntityDataProvider<T> extends AbstractBackEndDataProvider<T, Condition> implements ConfigurableFilterDataProvider<T, Condition, Condition> {

    @NotNull
    private final Class<T> entityClass;
    @Nullable
    private transient DaoOfAny<T> dao;

    @Nullable
    private Condition configuredFilter = null;

    public EntityDataProvider(@NotNull Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass);
    }

    @NotNull
    private DaoOfAny<T> getDao() {
        if (dao == null) {
            dao = new DaoOfAny<>(entityClass);
        }
        return dao;
    }

    @Nullable
    private Condition calculateCondition(@NotNull Query<T, Condition> query) {
        Condition queryFilter = query.getFilter().orElse(null);
        List<Condition> conditions = Stream.of(configuredFilter, queryFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (conditions.isEmpty()) {
            return null;
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        } else {
            return conditions.get(0).and(conditions.get(1));
        }
    }

    @Override
    protected Stream<T> fetchFromBackEnd(@NotNull Query<T, Condition> query) {
        final Condition condition = calculateCondition(query);
        final List<OrderBy> order = query.getSortOrders() == null ? Collections.emptyList() :
                query.getSortOrders().stream().map(VaadinUtils::toOrderBy).collect(Collectors.toList());
        final List<T> list = getDao().findAllBy(condition, order, (long) query.getOffset(), (long) query.getLimit());
        return list.stream();
    }

    @Override
    protected int sizeInBackEnd(@NotNull Query<T, Condition> query) {
        final Condition condition = calculateCondition(query);
        return (int) getDao().countBy(condition);
    }

    @Override
    public void setFilter(@Nullable Condition filter) {
        this.configuredFilter = filter;
        refreshAll();
    }

    /**
     * Allows this data provider to be set to a Vaadin component which performs String-based
     * filtering, e.g. ComboBox. When the user types in something in
     * hopes to filter the items in the dropdown, <code>filterConverter</code> is invoked, to convert the user input
     * into {@link Condition}.
     *
     * @param filterConverter only invoked when the user types in something. The String is guaranteed to be
     *                        non-null, non-blank and trimmed.
     */
    public DataProvider<T, String> withStringFilter(@NotNull SerializableFunction<String, Condition> filterConverter) {
        return withConvertedFilter(filter -> {
            final String postProcessedFilter = filter == null ? "" : filter.trim();
            if (!postProcessedFilter.isEmpty()) {
                return filterConverter.apply(postProcessedFilter);
            } else {
                return null;
            }
        });
    }

    public void setSortFields(OrderBy... fields) {
        if (fields == null) {
            setSortOrders(Collections.emptyList());
        } else {
            setSortOrders(Arrays.stream(fields).map(VaadinUtils::toQuerySortOrder).collect(Collectors.toList()));
        }
    }
}
