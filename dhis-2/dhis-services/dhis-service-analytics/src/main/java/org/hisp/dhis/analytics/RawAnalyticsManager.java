package org.hisp.dhis.analytics;

import org.hisp.dhis.common.Grid;

public interface RawAnalyticsManager
{
    Grid getRawDataValues( DataQueryParams params, Grid grid );    
}
