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
package org.hisp.dhis.dataexchange.aggregate;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Main service class for analytics data exchange.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
public class AggregateDataExchangeService
{
    private final AnalyticsService analyticsService;

    private final AggregateDataExchangeStore aggregateDataExchangeStore;

    private final DataQueryService dataQueryService;

    private final DataValueSetService dataValueSetService;

    private final PBEStringCleanablePasswordEncryptor encryptor;

    public AggregateDataExchangeService( AnalyticsService analyticsService,
        AggregateDataExchangeStore aggregateDataExchangeStore,
        DataQueryService dataQueryService,
        DataValueSetService dataValueSetService,
        @Qualifier( AES_128_STRING_ENCRYPTOR ) PBEStringCleanablePasswordEncryptor encryptor )
    {
        this.analyticsService = analyticsService;
        this.aggregateDataExchangeStore = aggregateDataExchangeStore;
        this.dataQueryService = dataQueryService;
        this.dataValueSetService = dataValueSetService;
        this.encryptor = encryptor;
    }

    /**
     * Runs the analytics data exchange with the given identifier.
     *
     * @param uid the {@link AggregateDataExchange} identifier.
     * @param progress {@link JobProgress} to track progress when running in a
     *        job context
     * @return an {@link ImportSummaries}.
     */
    public ImportSummaries exchangeData( String uid, JobProgress progress )
    {
        AggregateDataExchange exchange = aggregateDataExchangeStore.loadByUid( uid );

        return exchangeData( exchange, progress );
    }

    /**
     * Runs the given analytics data exchange.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @param progress {@link JobProgress} to track progress when running in a
     *        job context
     * @return an {@link ImportSummaries}.
     */
    public ImportSummaries exchangeData( AggregateDataExchange exchange, JobProgress progress )
    {
        ImportSummaries summaries = new ImportSummaries();

        progress.startingStage( format( "exchange aggregate data %s to %s target %s", exchange.getName(),
            exchange.getTarget().getType().name().toLowerCase(), exchange.getTarget().getApi().getUrl() ),
            FailurePolicy.SKIP_ITEM );
        progress.runStage( exchange.getSource().getRequests().stream(),
            request -> format( "dx: %s; pe: %s; ou: %s", join( ",", request.getDx() ), join( ",", request.getPe() ),
                join( ",", request.getOu() ) ),
            request -> summaries.addImportSummary( exchangeData( exchange, request ) ),
            ( success, failed ) -> format( "Aggregate data exchange completed (%d/%d): '%s', type: '%s'",
                success, (success + failed), exchange.getUid(), exchange.getTarget().getType() ) );

        return summaries;
    }

    /**
     * Returns the source data for the analytics data exchange with the given
     * identifier.
     *
     * @param uid the {@link AggregateDataExchange} identifier.
     * @return the source data for the analytics data exchange.
     */
    public List<Grid> getSourceData( String uid )
    {
        AggregateDataExchange exchange = aggregateDataExchangeStore.loadByUid( uid );

        return mapToList( exchange.getSource().getRequests(),
            request -> analyticsService.getAggregatedDataValues( toDataQueryParams( request ) ) );
    }

    /**
     * Exchanges data from the source as defined by the given
     * {@link SourceRequest} to the target as defined by the given
     * {@link AggregateDataExchange}.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @param request the {@link SourceRequest}.
     * @return an {@link ImportSummary} describing the outcome of the exchange.
     */
    ImportSummary exchangeData( AggregateDataExchange exchange, SourceRequest request )
    {
        DataValueSet dataValueSet = analyticsService.getAggregatedDataValueSet( toDataQueryParams( request ) );

        return exchange.getTarget().getType() == TargetType.INTERNAL ? pushToInternal( exchange, dataValueSet )
            : pushToExternal( exchange, dataValueSet );
    }

    /**
     * Imports the given {@link DataValueSet} to this instance of DHIS 2.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @param dataValueSet the {@link DataValueSet}.
     * @return an {@link ImportSummary} describing the outcome of the exchange.
     */
    ImportSummary pushToInternal( AggregateDataExchange exchange, DataValueSet dataValueSet )
    {
        return dataValueSetService.importDataValueSet( dataValueSet, toImportOptions( exchange ) );
    }

    /**
     * Exchanges the given {@link DataValueSet} to an external instance of DHIS
     * 2. The location and credentials of the target DHIS 2 instance and the
     * import options to use for the data exchange are specified by the target
     * API of the given {@link AggregateDataExchange}.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @param dataValueSet the {@link DataValueSet}.
     * @return an {@link ImportSummary} describing the outcome of the exchange.
     */
    ImportSummary pushToExternal( AggregateDataExchange exchange, DataValueSet dataValueSet )
    {
        return getDhis2Client( exchange ).saveDataValueSet( dataValueSet, toImportOptions( exchange ) );
    }

    /**
     * Converts the {@link TargetRequest} of the given
     * {@link AggregateDataExchange} to an {@link ImportOptions}.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @return an {@link ImportOptions}.
     */
    ImportOptions toImportOptions( AggregateDataExchange exchange )
    {
        TargetRequest request = exchange.getTarget().getRequest();

        return new ImportOptions()
            .setDataElementIdScheme( getOrDefault( request.getDataElementIdScheme() ) )
            .setOrgUnitIdScheme( getOrDefault( request.getOrgUnitIdScheme() ) )
            .setCategoryOptionComboIdScheme( getOrDefault( request.getCategoryOptionComboIdScheme() ) )
            .setIdScheme( getOrDefault( request.getIdScheme() ) );
    }

    /**
     * Retrieves and creates a {@link DataQueryParams} based on the given
     * {@link SourceRequest}.
     *
     * @param request the {@link SourceRequest}.
     * @return the {@link DataQueryParams}.
     */
    DataQueryParams toDataQueryParams( SourceRequest request )
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

    /**
     * Creates a dimensional object based on the given dimension, items and
     * input ID scheme.
     *
     * @param dimension the dimension.
     * @param items the list of dimension items.
     * @param inputIdScheme the {@link IdScheme}.
     * @return a {@link DimensionalObject}.
     */
    DimensionalObject toDimensionalObject( String dimension, List<String> items, IdScheme inputIdScheme )
    {
        return dataQueryService.getDimension(
            dimension, items, new Date(), null, null, false, inputIdScheme );
    }

    /**
     * Creates a dimensional object based on the given filter and ID scheme.
     *
     * @param filter the {@link Filter}.
     * @param inputIdScheme the {@link IdScheme}.
     * @return a {@link DimensionalObject}.
     */
    DimensionalObject toDimensionalObject( Filter filter, IdScheme inputIdScheme )
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
    String getOrDefault( String idScheme )
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
    IdScheme toIdSchemeOrDefault( String idScheme )
    {
        return idScheme != null ? IdScheme.from( idScheme ) : IdScheme.UID;
    }

    /**
     * Returns a {@link Dhis2Client} based on the given
     * {@link AggregateDataExchange}.
     *
     * @param exchange the {@link AggregateDataExchange}.
     * @return a {@link Dhis2Client}.
     * @throws IllegalStateException
     */
    Dhis2Client getDhis2Client( AggregateDataExchange exchange )
    {
        Api api = exchange.getTarget().getApi();

        if ( api.isAccessTokenAuth() )
        {
            return Dhis2Client.withAccessTokenAuth(
                api.getUrl(), encryptor.decrypt( api.getAccessToken() ) );
        }
        else if ( api.isBasicAuth() )
        {
            return Dhis2Client.withBasicAuth(
                api.getUrl(), api.getUsername(), encryptor.decrypt( api.getPassword() ) );
        }

        throw new IllegalStateException( "DHIS 2 client authentication not configured" );
    }
}
