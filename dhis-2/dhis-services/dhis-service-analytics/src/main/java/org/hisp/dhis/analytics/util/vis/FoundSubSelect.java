package org.hisp.dhis.analytics.util.vis;

import net.sf.jsqlparser.statement.select.SubSelect;

public record FoundSubSelect(String name, SubSelect subSelect, String columnReference) { }