package org.hisp.dhis.common;

import java.util.List;

public class DimensionalAggregation
{
    private final List<BaseIdentifiableObject> groupBy;

    public DimensionalAggregation(List<BaseIdentifiableObject> groupBy) {
        this.groupBy = groupBy;
    }

    public List<BaseIdentifiableObject> getGroupBy() {
        return groupBy;
    }
}
