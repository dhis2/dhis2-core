package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.analytics.GridAsserter.KeyValue.tuple;

import java.util.*;

import org.hisp.dhis.analytics.*;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceMetadataTest extends AnalyticsServiceBaseTest
{

    @Test
    @SuppressWarnings("unchecked")
    public void metadataContainsOuLevelData()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
            // DATA ELEMENTS
            .withDataElements( newArrayList( createDataElement( 'A', new CategoryCombo() ) ) ).withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters( Collections.singletonList(
                    new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                    new DimensionalKeywords(
                        Lists.newArrayList(
                                buildOrgUnitLevel( 2, "wjP19dkFeIk", "District", null ),
                                buildOrgUnitLevel( 1, "tTUf91fCytl", "Chiefdom", "OU_12345" ) )
                    ),
                    ImmutableList.of(
                        new OrganisationUnit( "aaa", "aaa", "OU_1", null, null, "c1" ),
                        new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" )
                )    ) )
            )
            .build();

        initMock(params);

        Grid grid = target.getAggregatedDataValues( params );

        GridAsserter.addGrid(grid)
                .metadataItemsHasSize(9)
                .metadataItemWithUidHas("wjP19dkFeIk", tuple("name" , "District" ), tuple("code", null))
                .metadataItemWithUidHas("tTUf91fCytl", tuple("name" , "Chiefdom" ), tuple("code", "OU_12345"))
                .verify();
    }

    @Test
    public void metadataContainsIndicatorGroupMetadata()
    {
        List<DimensionalItemObject> periods = new ArrayList<>();
        periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, 4, 1, 0, 0 ).toDate() ) );

        IndicatorGroup indicatorGroup = new IndicatorGroup( "ANC" );
        indicatorGroup.setCode( "COD_1000" );
        indicatorGroup.setUid( "wjP19dkFeIk" );
        DataQueryParams params = DataQueryParams.newBuilder()
            // DATA ELEMENTS
            .withDimensions( Lists.newArrayList(
                    new BaseDimensionalObject( "pe", DimensionType.PERIOD, periods ),
                    new BaseDimensionalObject( "dx", DimensionType.DATA_X, DISPLAY_NAME_DATA_X,
                    "display name",
                            new DimensionalKeywords( Collections.singletonList( indicatorGroup ) ),
                    Lists.newArrayList( new Indicator(), new Indicator(), createDataElement( 'A', new CategoryCombo() ),
                        createDataElement( 'B', new CategoryCombo() ) ) ) ) )
            .withFilters( Collections.singletonList(
                new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                    ImmutableList.of(   new OrganisationUnit( "aaa", "aaa", "OU_1", null, null, "c1" ),
                        new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" )
                    ) ) ) )
            .withIgnoreLimit( true )
            .withSkipData( true )
            .build();

        initMock(params);

        Grid grid = target.getAggregatedDataValues( params );

        GridAsserter.addGrid(grid)
                .metadataItemsHasSize(10)
                .metadataItemWithUidHas(indicatorGroup.getUid(),
                        tuple("name" , indicatorGroup.getName() ),
                        tuple("code", indicatorGroup.getCode()))
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void metadataContainsOuGroupData()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
            // DATA ELEMENTS
            .withDataElements( newArrayList( createDataElement( 'A', new CategoryCombo() ) ) ).withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters( Collections.singletonList(
                new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                    new DimensionalKeywords(
                        Lists.newArrayList( new BaseNameableObject( "tTUf91fCytl", "OU_12345", "Chiefdom" ) ) ),
                        ImmutableList.of(
                                new OrganisationUnit( "aaa", "aaa", "OU_1", null, null, "c1" ),
                                new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" ) ) ) ) )
            .build();

        initMock(params);

        Grid grid = target.getAggregatedDataValues( params );

        GridAsserter.addGrid(grid)
                .metadataItemsHasSize(8)
                .metadataItemWithUidHas("tTUf91fCytl",
                        tuple("name" , "Chiefdom" ),
                        tuple("code", "OU_12345"))
                .verify();
    }

    @Test
    public void metadataContainsDataElementGroupMetadata()
    {
        List<DimensionalItemObject> periods = new ArrayList<>();
        periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, 4, 1, 0, 0 ).toDate() ) );

        DataElementGroup dataElementGroup = new DataElementGroup( "ANC" );
        dataElementGroup.setCode( "COD_1000" );
        dataElementGroup.setUid( "wjP19dkFeIk" );
        DataQueryParams params = DataQueryParams.newBuilder()
                // DATA ELEMENTS
                .withDimensions( Lists.newArrayList(
                        new BaseDimensionalObject( "pe", DimensionType.PERIOD, periods ),
                        new BaseDimensionalObject( "dx", DimensionType.DATA_X, DISPLAY_NAME_DATA_X,
                                "display name",
                                new DimensionalKeywords( Collections.singletonList( dataElementGroup ) ),
                                Lists.newArrayList(
                                        createDataElement( 'A', new CategoryCombo() ),
                                        createDataElement( 'B', new CategoryCombo() ) ) ) ) )
                .withFilters( Collections.singletonList(
                        new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                                ImmutableList.of( new OrganisationUnit( "aaa", "aaa", "OU_1", null, null, "c1" ) ) ) ) )
                .withIgnoreLimit( true )
                .withSkipData( true )
                .build();

        initMock(params);

        Grid grid = target.getAggregatedDataValues( params );
        GridAsserter.addGrid(grid)
                .metadataItemsHasSize(8)
                .metadataItemWithUidHas(dataElementGroup.getUid(),
                        tuple("name" , dataElementGroup.getName() ),
                        tuple("code", dataElementGroup.getCode()))
                .verify();
    }

    private OrganisationUnitLevel buildOrgUnitLevel( int level, String uid, String name, String code )
    {
        OrganisationUnitLevel oul = new OrganisationUnitLevel( level, name );
        oul.setUid( uid );
        oul.setCode( code );
        return oul;
    }
}
