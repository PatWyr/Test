package com.gitlab.mvysny.jdbiorm.vaadin;

import com.gitlab.mvysny.jdbiorm.OrderBy;
import com.gitlab.mvysny.jdbiorm.Property;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import org.jetbrains.annotations.NotNull;

public class VaadinUtils {
    @NotNull
    public static QuerySortOrder toQuerySortOrder(@NotNull OrderBy orderBy) {
        return new QuerySortOrder(orderBy.getName().getName(), orderBy.getOrder() == OrderBy.Order.ASC ? SortDirection.ASCENDING : SortDirection.DESCENDING);
    }

    @NotNull
    public static OrderBy toOrderBy(@NotNull QuerySortOrder querySortOrder) {
        return new OrderBy(new Property.Name(querySortOrder.getSorted()), querySortOrder.getDirection() == SortDirection.ASCENDING ? OrderBy.Order.ASC : OrderBy.Order.DESC);
    }
}
