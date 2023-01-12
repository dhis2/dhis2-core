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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengis.geometry.primitive.Point;

/**
 * This class only tests the "shared" code of AbstractAnalyticsService, which
 * includes Grid header generation and Grid Metadata
 *
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class AbstractAnalyticsServiceTest
{
    private Period peA;

    private OrganisationUnit ouA;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DummyAnalyticsService dummyAnalyticsService;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private EventQueryValidator eventQueryValidator;

    @Mock
    private SchemaIdResponseMapper schemaIdResponseMapper;

    @BeforeEach
    public void setUp()
    {
        dummyAnalyticsService = new DummyAnalyticsService( securityManager, eventQueryValidator,
            schemaIdResponseMapper );

        peA = MonthlyPeriodType.getPeriodFromIsoString( "201701" );
        ouA = createOrganisationUnit( 'A' );
        deA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE );

        deB = createDataElement( 'B', ValueType.ORGANISATION_UNIT, AggregationType.NONE );
        deC = createDataElement( 'C', ValueType.NUMBER, AggregationType.COUNT );
    }

    @Test
    void verifyHeaderCreationBasedOnQueryItemsAndDimensions()
    {
        DimensionalObject periods = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
            List.of( peA ) );

        DimensionalObject orgUnits = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, "ouA", List.of( ouA ) );

        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), null );
        QueryItem qiB = new QueryItem( deB, null, deB.getValueType(), deB.getAggregationType(), null );
        QueryItem qiC = new QueryItem( deC, null, deC.getValueType(), deC.getAggregationType(), null );

        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( periods )
            .addDimension( orgUnits )
            .addItem( qiA ).addItem( qiB ).addItem( qiC )
            .withCoordinateFields( List.of( deB.getUid() ) )
            .withSkipData( true )
            .withSkipMeta( false )
            .withApiVersion( DhisApiVersion.V33 )
            .build();

        when( securityManager.withUserConstraints( any( EventQueryParams.class ) ) ).thenReturn( params );

        Grid grid = dummyAnalyticsService.getGrid( params );

        List<GridHeader> headers = grid.getHeaders();
        assertThat( headers, is( notNullValue() ) );
        assertThat( headers, hasSize( 4 ) );

        assertHeader( headers.get( 0 ), "ou", "ouA", ValueType.TEXT, String.class.getName() );
        assertHeader( headers.get( 1 ), deA.getUid(), deA.getName(), ValueType.TEXT, String.class.getName() );
        assertHeader( headers.get( 2 ), deB.getUid(), deB.getName(), ValueType.COORDINATE, Point.class.getName() );
        assertHeader( headers.get( 3 ), deC.getUid(), deC.getName(), ValueType.NUMBER, Double.class.getName() );
    }

    private void assertHeader( GridHeader expected, String name, String column, ValueType valueType, String type )
    {
        assertThat( "Header name does not match", expected.getName(), is( name ) );
        assertThat( "Header column name does not match", expected.getColumn(), is( column ) );
        assertThat( "Header value type does not match", expected.getValueType(), is( valueType ) );
        assertThat( "Header type does not match", expected.getType(), is( type ) );
    }
}

class DummyAnalyticsService extends AbstractAnalyticsService
{
    public DummyAnalyticsService( AnalyticsSecurityManager securityManager, EventQueryValidator queryValidator,
        SchemaIdResponseMapper schemaIdResponseMapper )
    {
        super( securityManager, queryValidator, schemaIdResponseMapper );
    }

    @Override
    protected Grid createGridWithHeaders( EventQueryParams params )
    {
        return new ListGrid();
    }

    @Override
    protected long addEventData( Grid grid, EventQueryParams params )
    {
        return 0;
    }
}
