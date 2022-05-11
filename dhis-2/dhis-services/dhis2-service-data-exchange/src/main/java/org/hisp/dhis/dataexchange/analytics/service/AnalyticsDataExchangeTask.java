package org.hisp.dhis.dataexchange.analytics.service;

import java.io.IOException;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.analytics.model.AnalyticsDataExchange;
import org.hisp.dhis.dataexchange.analytics.model.SourceRequest;
import org.hisp.dhis.dataexchange.analytics.model.TargetRequest;
import org.hisp.dhis.dataexchange.analytics.model.TargetType;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnalyticsDataExchangeTask
{
    private AnalyticsService analyticsService;

    private DataValueSetService dataValueSetService;

    private ObjectMapper objectMapper;

    public AnalyticsDataExchangeTask()
    {

    }

    private void exhangeData( AnalyticsDataExchange exchange )
        throws IOException
    {
        DataQueryParams params = toDataQueryParams( exchange );

        DataValueSet dataValueSet = analyticsService.getAggregatedDataValueSet( params );

        ImportSummary summary = exchange.getTarget().getType() == TargetType.INTERNAL ?
            pushToInternal( exchange, dataValueSet ) : pushToExternal( exchange, dataValueSet );
    }

    private ImportSummary pushToInternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
        throws IOException
    {
        ImportOptions importOptions = toImportOptions( exchange );

        return dataValueSetService.importDataValueSet( dataValueSet, importOptions );
    }

    private ImportSummary pushToExternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return null;
    }

    private ImportOptions toImportOptions( AnalyticsDataExchange exchange )
    {
        TargetRequest request = exchange.getTarget().getRequest();

        return new ImportOptions()
            .setDataElementIdScheme( toName( request.getDataElementIdScheme() ) )
            .setOrgUnitIdScheme( toName( request.getOrgUnitIdScheme() ) )
            .setCategoryOptionComboIdScheme( toName( request.getCategoryOptionComboIdScheme() ) )
            .setIdScheme( toName( request.getIdScheme() ) );
    }

    private DataQueryParams toDataQueryParams( AnalyticsDataExchange exchange )
    {
        SourceRequest request = exchange.getSource().getRequest();

        return DataQueryParams.newBuilder()
            .withDataElements( null )
            .build();
    }

    public static String toName( IdScheme idScheme )
    {
        return idScheme != null ? idScheme.name() : null;
    }
}
