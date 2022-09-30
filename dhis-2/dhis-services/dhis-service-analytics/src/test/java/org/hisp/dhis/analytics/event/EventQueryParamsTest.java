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
package org.hisp.dhis.analytics.event;

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class EventQueryParamsTest extends DhisConvenienceTest
{
    private Option opA;

    private Option opB;

    private Option opC;

    private Option opD;

    private OptionSet osA;

    private OptionSet osB;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private Program prA;

    private Program prB;

    private Program prC;

    private ProgramStage psA;

    private ProgramStage psB;

    private ProgramStage psC;

    private Period peA;

    private Period peB;

    private Period peC;

    @BeforeEach
    void before()
    {
        opA = createOption( 'A' );
        opB = createOption( 'B' );
        opC = createOption( 'C' );
        opD = createOption( 'D' );
        osA = createOptionSet( 'A', opA, opB );
        osB = createOptionSet( 'B', opC, opD );
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deC.setValueType( ValueType.DATE );
        deD = createDataElement( 'D' );
        deD.setValueType( ValueType.ORGANISATION_UNIT );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        psA = createProgramStage( 'A', prA );
        psB = createProgramStage( 'B', prB );
        psC = createProgramStage( 'B', prC );
        // Program Stage A
        psA.addDataElement( deA, 0 );
        psA.addDataElement( deB, 1 );
        psA.addDataElement( deC, 2 );
        psA.addDataElement( deD, 3 );
        // Program Stage B
        psB.addDataElement( deA, 0 );
        psB.addDataElement( deB, 1 );
        // Program Stage C
        psC.addDataElement( deA, 0 );
        prA = createProgram( 'A', Sets.newHashSet( psA ), ouA );
        prB = createProgram( 'B', Sets.newHashSet( psB ), ouA );
        prC = createProgram( 'C', Sets.newHashSet( psC ), ouA );
        TrackedEntityAttribute teA = createTrackedEntityAttribute( 'A', ValueType.ORGANISATION_UNIT );
        teA.setUid( deD.getUid() );
        ProgramTrackedEntityAttribute pteA = createProgramTrackedEntityAttribute( prC, teA );
        prC.setProgramAttributes( Collections.singletonList( pteA ) );
        peA = new MonthlyPeriodType().createPeriod( new DateTime( 2014, 4, 1, 0, 0 ).toDate() );
        peB = new MonthlyPeriodType().createPeriod( new DateTime( 2014, 5, 1, 0, 0 ).toDate() );
        peC = new MonthlyPeriodType().createPeriod( new DateTime( 2014, 6, 1, 0, 0 ).toDate() );
    }

    @Test
    void testGetKey()
    {
        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), osA );
        QueryItem qiB = new QueryItem( deB, null, deB.getValueType(), deB.getAggregationType(), osB );
        EventQueryParams paramsA = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB, peC ) ) )
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA, ouB ) ) )
            .addItem( qiA ).addItem( qiB ).build();
        EventQueryParams paramsB = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB ) ) )
            .addDimension( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA ) ) )
            .addItem( qiA ).addItem( qiB ).withGeometryOnly( true ).build();
        assertNotNull( paramsA.getKey() );
        assertEquals( 40, paramsA.getKey().length() );
        assertNotNull( paramsB.getKey() );
        assertEquals( 40, paramsB.getKey().length() );
        assertNotEquals( paramsA.getKey(), paramsB.getKey() );
    }

    @Test
    void testReplacePeriodsWithStartEndDates()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB, peC ) ) )
            .build();
        assertNull( params.getStartDate() );
        assertNull( params.getEndDate() );
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();
        assertEquals( new DateTime( 2014, 4, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2014, 6, 30, 0, 0 ).toDate(), params.getEndDate() );
        assertEquals( 3, params.getDateRangeList().size() );
    }

    @Test
    void testContinuousDateRangeListGeneratedByReplacingPeriodsWithStartEndDates()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peB, peC ) ) )
            .build();

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals( 3, params.getDateRangeList().size() );
        assertEquals( peA.getStartDate(), params.getDateRangeList().get( 0 ).getStartDate() );
        assertEquals( peA.getEndDate(), params.getDateRangeList().get( 0 ).getEndDate() );
        assertEquals( peB.getStartDate(), params.getDateRangeList().get( 1 ).getStartDate() );
        assertEquals( peB.getEndDate(), params.getDateRangeList().get( 1 ).getEndDate() );
        assertEquals( peC.getStartDate(), params.getDateRangeList().get( 2 ).getStartDate() );
        assertEquals( peC.getEndDate(), params.getDateRangeList().get( 2 ).getEndDate() );
        assertTrue( params.hasContinuousDateRangeList() );
    }

    @Test
    void testNonContinuousDateRangeListGeneratedByReplacingPeriodsWithStartEndDates()
    {
        // Given
        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA, peC ) ) )
            .build();

        // When
        params = new EventQueryParams.Builder( params ).withStartEndDatesForPeriods().build();

        // Then
        assertEquals( 2, params.getDateRangeList().size() );
        assertEquals( peA.getStartDate(), params.getDateRangeList().get( 0 ).getStartDate() );
        assertEquals( peA.getEndDate(), params.getDateRangeList().get( 0 ).getEndDate() );
        assertEquals( peC.getStartDate(), params.getDateRangeList().get( 1 ).getStartDate() );
        assertEquals( peC.getEndDate(), params.getDateRangeList().get( 1 ).getEndDate() );
        assertFalse( params.hasContinuousDateRangeList() );
    }

    @Test
    void testGetItemLegends()
    {
        Legend leA = createLegend( 'A', 0d, 1d );
        Legend leB = createLegend( 'B', 1d, 2d );
        LegendSet lsA = createLegendSet( 'A', leA, leB );
        QueryItem qiA = new QueryItem( deA, lsA, deA.getValueType(), deA.getAggregationType(), null );
        EventQueryParams params = new EventQueryParams.Builder().addItem( qiA ).build();
        Set<Legend> expected = Sets.newHashSet( leA, leB );
        assertEquals( expected, params.getItemLegends() );
    }

    @Test
    void testGetItemOptions()
    {
        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), osA );
        QueryItem qiB = new QueryItem( deB, null, deB.getValueType(), deB.getAggregationType(), osB );
        EventQueryParams params = new EventQueryParams.Builder().addItem( qiA ).addItem( qiB ).build();
        Set<Option> expected = Sets.newHashSet( opA, opB, opC, opD );
        assertEquals( expected, params.getItemOptions() );
    }

    @Test
    void testGetDuplicateQueryItems()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        QueryItem iB = new QueryItem( createDataElement( 'B', new CategoryCombo() ) );
        QueryItem iC = new QueryItem( createDataElement( 'B', new CategoryCombo() ) );
        QueryItem iD = new QueryItem( createDataElement( 'D', new CategoryCombo() ) );
        EventQueryParams params = new EventQueryParams.Builder().addItem( iA ).addItem( iB ).addItem( iC ).addItem( iD )
            .build();
        List<QueryItem> duplicates = params.getDuplicateQueryItems();
        assertEquals( 1, duplicates.size() );
        assertTrue( duplicates.contains( iC ) );
    }

    @Test
    void testIsTimeFieldValid()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA ).withTimeField( deC.getUid() )
            .addItem( iA ).build();
        assertTrue( params.timeFieldIsValid() );
        params = new EventQueryParams.Builder().withProgram( prA ).withTimeField( TimeField.SCHEDULED_DATE.name() )
            .addItem( iA ).build();
        assertTrue( params.timeFieldIsValid() );
        params = new EventQueryParams.Builder().withProgram( prA ).withTimeField( "someInvalidTimeField" ).addItem( iA )
            .build();
        assertFalse( params.timeFieldIsValid() );
    }

    @Test
    void testIsOrgUnitFieldValid()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withOrgUnitField( new OrgUnitField( deD.getUid() ) ).addItem( iA ).build();
        assertTrue( params.orgUnitFieldIsValid() );
        params = new EventQueryParams.Builder().withProgram( prA )
            .withOrgUnitField( new OrgUnitField( "someInvalidOrgUnitField" ) ).addItem( iA ).build();
        assertFalse( params.orgUnitFieldIsValid() );
    }

    @Test
    void testIsOrgUnitFieldValidWithOneProgramIndicator()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', prA, "", "" );
        EventQueryParams params = new EventQueryParams.Builder().withProgram( null )
            .withOrgUnitField( new OrgUnitField( deD.getUid() ) )
            .addItem( iA ).addItemProgramIndicator( programIndicatorA ).build();
        assertTrue( params.orgUnitFieldIsValid() );
    }

    @Test
    void testIsOrgUnitFieldValidWithMultipleProgramIndicator()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', prA, "", "" );
        // this PI has 0 Data Element of type OrgUnit -> test should fail
        ProgramIndicator programIndicatorB = createProgramIndicator( 'B', prB, "", "" );
        EventQueryParams params = new EventQueryParams.Builder().withProgram( null )
            .withOrgUnitField( new OrgUnitField( deD.getUid() ) )
            .addItem( iA ).addItemProgramIndicator( programIndicatorA ).addItemProgramIndicator( programIndicatorB )
            .build();
        assertFalse( params.orgUnitFieldIsValid() );
    }

    @Test
    void testIsOrgUnitFieldValidWithMultipleProgramIndicator2()
    {
        QueryItem iA = new QueryItem( createDataElement( 'A', new CategoryCombo() ) );
        // This PI has a Program that has a Tracked Entity Attribute of type Org
        // Unit
        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', prC, "", "" );
        EventQueryParams params = new EventQueryParams.Builder().withProgram( null )
            .withOrgUnitField( new OrgUnitField( deD.getUid() ) )
            .addItem( iA ).addItemProgramIndicator( programIndicatorA ).build();
        assertTrue( params.orgUnitFieldIsValid() );
    }
}
