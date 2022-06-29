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
package org.hisp.dhis.dataexchange.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.i18n.I18nFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class AnalyticsDataExchangeServiceTest
{
    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private AnalyticsDataExchangeStore analyticsDataExchangeStore;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private DataValueSetService dataValueSetService;

    private AnalyticsDataExchangeService service;

    @BeforeEach
    void beforeEach()
    {
        service = new AnalyticsDataExchangeService(
            analyticsService, analyticsDataExchangeStore, dataQueryService, dataValueSetService );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    void testExchangeData()
    {
        when( analyticsService.getAggregatedDataValueSet( any( DataQueryParams.class ) ) )
            .thenReturn( new DataValueSet() );
        when( dataQueryService.getDimension( eq( DimensionalObject.DATA_X_DIM_ID ), any(), any( Date.class ),
            nullable( List.class ), nullable( I18nFormat.class ), anyBoolean(), any( IdScheme.class ) ) )
                .thenReturn( new BaseDimensionalObject(
                    DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, List.of() ) );
        when( dataQueryService.getDimension( eq( DimensionalObject.PERIOD_DIM_ID ), any(), any( Date.class ),
            nullable( List.class ), nullable( I18nFormat.class ), anyBoolean(), any( IdScheme.class ) ) )
                .thenReturn( new BaseDimensionalObject(
                    DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, List.of() ) );
        when( dataQueryService.getDimension( eq( DimensionalObject.ORGUNIT_DIM_ID ), any(), any( Date.class ),
            nullable( List.class ), nullable( I18nFormat.class ), anyBoolean(), any( IdScheme.class ) ) )
                .thenReturn( new BaseDimensionalObject(
                    DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of() ) );
        when( dataValueSetService.importDataValueSet( any( DataValueSet.class ), any( ImportOptions.class ) ) )
            .thenReturn( new ImportSummary( ImportStatus.SUCCESS ) );

        SourceRequest sourceRequest = new SourceRequest()
            .setDx( List.of( "Vz0C3i4Wy3M", "ToaOToReol6" ) )
            .setPe( List.of( "202101", "202102" ) )
            .setOu( List.of( "lGgJFgRkZui", "pvINfKxtqyN" ) );
        Source source = new Source()
            .setRequests( List.of( sourceRequest ) );
        TargetRequest request = new TargetRequest()
            .setDataElementIdScheme( "code" )
            .setOrgUnitIdScheme( "code" )
            .setIdScheme( "uid" );
        Target target = new Target()
            .setType( TargetType.INTERNAL )
            .setApi( new Api() )
            .setRequest( request );
        AnalyticsDataExchange exchange = new AnalyticsDataExchange()
            .setSource( source )
            .setTarget( target );

        ImportSummaries summaries = service.exchangeData( exchange );

        assertNotNull( summaries );
        assertEquals( 1, summaries.getImportSummaries().size() );

        ImportSummary summary = summaries.getImportSummaries().get( 0 );

        assertNotNull( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }

    @Test
    void testToImportOptions()
    {
        TargetRequest request = new TargetRequest()
            .setDataElementIdScheme( "code" )
            .setOrgUnitIdScheme( "code" )
            .setIdScheme( "uid" );
        Target target = new Target()
            .setType( TargetType.EXTERNAL )
            .setApi( new Api() )
            .setRequest( request );
        AnalyticsDataExchange exchange = new AnalyticsDataExchange()
            .setTarget( target );

        ImportOptions options = service.toImportOptions( exchange );

        assertEquals( IdScheme.CODE, options.getIdSchemes().getDataElementIdScheme() );
        assertEquals( IdScheme.CODE, options.getIdSchemes().getOrgUnitIdScheme() );
        assertEquals( IdScheme.UID, options.getIdSchemes().getCategoryOptionComboIdScheme() );
        assertEquals( IdScheme.UID, options.getIdSchemes().getIdScheme() );
    }

    @Test
    void testGetOrDefault()
    {
        assertEquals( "CODE", service.getOrDefault( "CODE" ) );
        assertEquals( "UID", service.getOrDefault( null ) );
    }

    @Test
    void testToIdSchemeOrDefault()
    {
        assertEquals( IdScheme.CODE, service.toIdSchemeOrDefault( "code" ) );
        assertEquals( IdScheme.UID, service.toIdSchemeOrDefault( "UID" ) );
        assertEquals( IdScheme.UID, service.toIdSchemeOrDefault( null ) );
    }

    @Test
    void testGetDhis2Client()
    {
        Api api = new Api()
            .setUrl( "https://play.dhis2.org/demo" )
            .setUsername( "admin" )
            .setPassword( "district" );

        Target target = new Target()
            .setType( TargetType.EXTERNAL )
            .setApi( api );

        AnalyticsDataExchange exchange = new AnalyticsDataExchange()
            .setTarget( target );

        Dhis2Client client = service.getDhis2Client( exchange );

        assertEquals( "https://play.dhis2.org/demo", client.getUrl() );
    }
}
