package org.hisp.dhis.analytics.util.vis;

import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.Map;

public record FoundSubSelect(String name,
                             SubSelect subSelect,
                             String columnReference,
                             Map<String, String> metadata ) {

    public FoundSubSelect(String name, SubSelect subSelect, String columnReference) {
        this(name, subSelect, columnReference, Map.of());
    }
}