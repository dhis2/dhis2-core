package org.hisp.dhis.common;

import java.util.List;

import lombok.Data;

@Data
public class DimensionalAggregation
{
    private final List<BaseIdentifiableObject> groupBy;
}
