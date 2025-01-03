package org.hisp.dhis.analytics.common;

import org.hisp.dhis.common.QueryFilter;

import java.util.List;
import java.util.function.Predicate;

import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.QueryOperator.IN;

/**
 * Mimics the logic of @{@link org.hisp.dhis.common.InQueryFilter} to be used in CTEs
 */
public class InQueryCteFilter {

    private final String filter;

    private final CteDefinition cteDefinition;

    private final String field;

    private final boolean isText;

    public InQueryCteFilter(String field, String encodedFilter, boolean isText, CteDefinition cteDefinition) {
        this.filter = encodedFilter;
        this.field = field;
        this.isText = isText;
        this.cteDefinition = cteDefinition;
    }


    public String getSqlFilter(int offset) {

        List<String> filterItems = QueryFilter.getFilterItems(this.filter);

        StringBuilder condition = new StringBuilder();
        String alias = cteDefinition.getAlias(offset);
        if (hasNonMissingValue(filterItems)) {
            // TODO GIUSEPPE!

            if (hasMissingValue(filterItems)) {

                // TODO GIUSEPPE!
            }
        } else {
            if (hasMissingValue(filterItems)) {
                condition.append("%s.enrollment is not null".formatted(alias));
                condition.append(" and ");
                condition.append("%s.%s is null".formatted(alias, field));
            }
        }

        return condition.toString();

    }

    /**
     * Checks if the filter items contain any non-missing values (values that are not {@link org.hisp.dhis.analytics.QueryKey#NV}).
     * Non-missing values represent actual values that should be included in the SQL IN clause.
     * This method is used to determine if the generated SQL condition needs to include an IN clause.
     *
     * @param filterItems the list of filter items to check for non-missing values
     * @return true if any item in the list is not equal to {@link org.hisp.dhis.analytics.QueryKey#NV}, indicating at least one
     *         actual value that should be included in the SQL IN clause; false if all values are missing
     */
    private boolean hasNonMissingValue(List<String> filterItems) {
        return anyMatch(filterItems, this::isNotMissingItem);
    }

    private boolean isNotMissingItem(String filterItem) {
        return !isMissingItem(filterItem);
    }

    private boolean isMissingItem(String filterItem) {
        return NV.equals(filterItem);
    }

    /**
     * Checks if any item in the list matches the given predicate.
     *
     * @param filterItems the list of items to check
     * @param predi the predicate to test against
     * @return true if any item matches the predicate, false otherwise
     */
    private boolean anyMatch(List<String> filterItems, Predicate<String> predi) {
        return filterItems.stream().anyMatch(predi);
    }

    /**
     * Checks if the filter items contain any missing values represented by the special marker {@link org.hisp.dhis.analytics.QueryKey#NV}.
     * Missing values indicate that the corresponding database field should be treated as NULL in the SQL query.
     * This method is used to determine if the generated SQL condition needs to include an IS NULL clause.
     *
     * @param filterItems the list of filter items to check for missing values
     * @return true if any item in the list equals{@link org.hisp.dhis.analytics.QueryKey#NV}, indicating a missing value that should
     *         be treated as NULL in the SQL query; false otherwise
     * @see org.hisp.dhis.analytics.QueryKey#NV
     */
    private boolean hasMissingValue(List<String> filterItems) {
        return anyMatch(filterItems, this::isMissingItem);
    }
}
