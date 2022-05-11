package org.hisp.dhis.dataexchange.analytics.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.analytics.model.AnalyticsDataExchange;
import org.hisp.dhis.dataexchange.analytics.model.Filter;
import org.hisp.dhis.dataexchange.analytics.model.SourceRequest;
import org.hisp.dhis.dataexchange.analytics.model.TargetRequest;
import org.hisp.dhis.dataexchange.analytics.model.TargetType;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AnalyticsDataExchangeTask
{
    private AnalyticsService analyticsService;

    private DataValueSetService dataValueSetService;

    public ImportSummary exhangeData( AnalyticsDataExchange exchange )
        throws IOException
    {
        DataQueryParams params = toDataQueryParams( exchange );

        DataValueSet dataValueSet = analyticsService.getAggregatedDataValueSet( params );

        return exchange.getTarget().getType() == TargetType.INTERNAL ?
            pushToInternal( exchange, dataValueSet ) : pushToExternal( exchange, dataValueSet );
    }

    private ImportSummary pushToInternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
        throws IOException
    {
        return dataValueSetService.importDataValueSet( dataValueSet, toImportOptions( exchange ) );
    }

    private ImportSummary pushToExternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return null; //TODO
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

        List<DimensionalObject> filters = request.getFilters().stream()
            .map( this::toDimensionalObject )
            .collect( Collectors.toList() );

        //TODO

        return DataQueryParams.newBuilder()
            .withDataElements( null )
            .withPeriods( null )
            .withOrganisationUnits( null )
            .addFilters( filters )
            .build();
    }

    private String toName( IdScheme idScheme )
    {
        return idScheme != null ? idScheme.name() : null;
    }

    private DimensionalObject toDimensionalObject( Filter filter )
    {
        return new BaseDimensionalObject(); //TODO
    }
}
