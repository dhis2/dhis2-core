/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.dataexchange.analytics.service;

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToList;

import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.analytics.AnalyticsDataExchange;
import org.hisp.dhis.dataexchange.analytics.Api;
import org.hisp.dhis.dataexchange.analytics.Filter;
import org.hisp.dhis.dataexchange.analytics.SourceRequest;
import org.hisp.dhis.dataexchange.analytics.TargetRequest;
import org.hisp.dhis.dataexchange.analytics.TargetType;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dataexchange.client.Dhis2Config;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsDataExchangeService
{
    private final AnalyticsService analyticsService;

    private final DataQueryService dataQueryService;

    private final DataValueSetService dataValueSetService;

    public ImportSummaries exchangeData( AnalyticsDataExchange exchange )
    {
        ImportSummaries summaries = new ImportSummaries();

        exchange.getSource().getRequests()
            .forEach( request -> summaries.addImportSummary( exchangeData( exchange, request ) ) );

        return summaries;
    }

    private ImportSummary exchangeData( AnalyticsDataExchange exchange, SourceRequest request )
    {
        DataValueSet dataValueSet = analyticsService.getAggregatedDataValueSet( toDataQueryParams( request ) );

        return exchange.getTarget().getType() == TargetType.INTERNAL ? pushToInternal( exchange, dataValueSet )
            : pushToExternal( exchange, dataValueSet );
    }

    private ImportSummary pushToInternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return dataValueSetService.importDataValueSet( dataValueSet, toImportOptions( exchange ) );
    }

    private ImportSummary pushToExternal( AnalyticsDataExchange exchange, DataValueSet dataValueSet )
    {
        return getDhis2Client( exchange ).saveDataValueSet( dataValueSet );
    }

    private ImportOptions toImportOptions( AnalyticsDataExchange exchange )
    {
        TargetRequest request = exchange.getTarget().getRequest();

        return new ImportOptions()
            .setDataElementIdScheme( getOrDefault( request.getDataElementIdScheme() ) )
            .setOrgUnitIdScheme( getOrDefault( request.getOrgUnitIdScheme() ) )
            .setCategoryOptionComboIdScheme( getOrDefault( request.getCategoryOptionComboIdScheme() ) )
            .setIdScheme( getOrDefault( request.getIdScheme() ) );
    }

    private DataQueryParams toDataQueryParams( SourceRequest request )
    {
        IdScheme inputIdScheme = toIdSchemeOrDefault( request.getInputIdScheme() );

        List<DimensionalObject> filters = mapToList(
            request.getFilters(), f -> toDimensionalObject( f, inputIdScheme ) );

        return DataQueryParams.newBuilder()
            .addDimension( toDimensionalObject( DATA_X_DIM_ID, request.getDx(), inputIdScheme ) )
            .addDimension( toDimensionalObject( PERIOD_DIM_ID, request.getPe(), inputIdScheme ) )
            .addDimension( toDimensionalObject( ORGUNIT_DIM_ID, request.getOu(), inputIdScheme ) )
            .addFilters( filters )
            .build();
    }

    private DimensionalObject toDimensionalObject( String dimension, List<String> items, IdScheme inputIdScheme )
    {
        return dataQueryService.getDimension(
            dimension, items, new Date(), null, null, false, inputIdScheme );
    }

    private DimensionalObject toDimensionalObject( Filter filter, IdScheme inputIdScheme )
    {
        return dataQueryService.getDimension(
            filter.getDimension(), filter.getItems(), new Date(), null, null, false, inputIdScheme );
    }

    /**
     * Returns the ID scheme string or the the default ID scheme if the given ID
     * scheme string is null.
     *
     * @param idScheme the ID scheme string.
     * @return the given ID scheme, or the default ID scheme string if null.
     */
    public static String getOrDefault( String idScheme )
    {
        return ObjectUtils.firstNonNull( idScheme, IdScheme.UID.name() );
    }

    /**
     * Returns the {@link IdScheme} based on the given ID scheme string, or the
     * default ID scheme if the given ID scheme string is null.
     *
     * @param idScheme the ID scheme string.
     * @return the given ID scheme, or the default ID scheme if null.
     */
    public IdScheme toIdSchemeOrDefault( String idScheme )
    {
        return idScheme != null ? IdScheme.from( idScheme ) : IdScheme.UID;
    }

    /**
     * Returns a {@link Dhis2Client} based on the given
     * {@link AnalyticsDataExchange}.
     *
     * @param exchange the {@link AnalyticsDataExchange}.
     * @return a {@link Dhis2Client}.
     */
    public Dhis2Client getDhis2Client( AnalyticsDataExchange exchange )
    {
        Api api = exchange.getTarget().getApi();

        return new Dhis2Client( new Dhis2Config( api.getUrl(), api.getAccessToken() ) );
    }
}
