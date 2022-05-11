package org.hisp.dhis.dataexchange.analytics.service;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.dataexchange.analytics.model.AnalyticsDataExchange;
import org.hisp.dhis.dataexchange.analytics.model.TargetType;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsDataExchangeTask
{
    private AnalyticsService analyticsService;

    private void exhangeData( AnalyticsDataExchange exchange )
    {
        DataQueryParams params = toDataQueryParams( exchange );

        DataValueSet dataValueSet = analyticsService.getAggregatedDataValueSet( params );

        WebMessage response = exchange.getTarget().getType() == TargetType.INTERNAL ?
            pushToInternal( exchange, dataValueSet ) : pushToExternal( exchange, dataValueSet );
    }

    private WebMessage pushToInternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return null;
    }

    private WebMessage pushToExternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return null;
    }

    private DataQueryParams toDataQueryParams( AnalyticsDataExchange exchange )
    {
        return null;
    }
}
