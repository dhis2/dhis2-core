/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.event.data.queryItem;

import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.PROGRAMSTAGE_SEP;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.DefaultQueryItemLocator;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.*;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class QueryItemLocatorTest
{
    @Mock
    private ProgramStageService programStageService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private LegendSetService legendSetService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    private QueryItemLocator subject;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Program programA;

    private String dimension;

    private String programStageUid;

    @Before
    public void setUp()
    {
        programA = createProgram( 'A' );

        dimension = CodeGenerator.generateUid();
        programStageUid = CodeGenerator.generateUid();

        subject = new DefaultQueryItemLocator( programStageService, dataElementService, attributeService,
            programIndicatorService, legendSetService, relationshipTypeService );
    }

    @Test
    public void verifyExceptionOnEmptyDimension()
    {
        exception.expect( IllegalQueryException.class );
        exception.expectMessage(
            "Item identifier does not reference any data element, attribute or indicator part of the program" );

        subject.getQueryItemFromDimension( "", programA, EventOutputType.ENROLLMENT );
    }

    @Test
    public void verifyExceptionOnEmptyProgram()
    {
        exception.expect( NullPointerException.class );
        exception.expectMessage( "Program can not be null" );

        subject.getQueryItemFromDimension( dimension, null, EventOutputType.ENROLLMENT );
    }

    @Test
    public void verifyDimensionReturnsDataElementForEventQuery()
    {
        DataElement dataElementA = createDataElement( 'A' );

        ProgramStage programStageA = createProgramStage( 'A', programA );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( createProgramStageDataElement( programStageA, dataElementA, 1 ) ) );

        programA.setProgramStages( Sets.newHashSet( programStageA ) );

        when( dataElementService.getDataElement( dimension ) ).thenReturn( dataElementA );

        QueryItem queryItem = subject.getQueryItemFromDimension( dimension, programA, EventOutputType.EVENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( dataElementA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );

    }

    @Test
    public void verifyDimensionFailsWhenProgramStageIsMissingForEnrollmentQuery()
    {
        exception.expect( IllegalQueryException.class );
        exception
            .expectMessage( "Program stage is mandatory for data element dimensions in enrollment analytics queries" );

        DataElement dataElementA = createDataElement( 'A' );

        ProgramStage programStageA = createProgramStage( 'A', programA );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( createProgramStageDataElement( programStageA, dataElementA, 1 ) ) );

        programA.setProgramStages( Sets.newHashSet( programStageA ) );

        when( dataElementService.getDataElement( dimension ) ).thenReturn( dataElementA );

        subject.getQueryItemFromDimension( dimension, programA, EventOutputType.ENROLLMENT );
    }

    @Test
    public void verifyDimensionReturnsDataElementForEnrollmentQuery()
    {
        DataElement dataElementA = createDataElement( 'A' );

        ProgramStage programStageA = createProgramStage( 'A', programA );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( createProgramStageDataElement( programStageA, dataElementA, 1 ) ) );

        programA.setProgramStages( Sets.newHashSet( programStageA ) );

        when( dataElementService.getDataElement( dimension ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageUid ) ).thenReturn( programStageA );

        QueryItem queryItem = subject.getQueryItemFromDimension( programStageUid + PROGRAMSTAGE_SEP + dimension,
            programA, EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( dataElementA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( programStageA ) );
    }

    @Test
    public void verifyDimensionWithLegendsetReturnsDataElement()
    {
        String legendSetUid = CodeGenerator.generateUid();

        DataElement dataElementA = createDataElement( 'A' );

        ProgramStage programStageA = createProgramStage( 'A', programA );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( createProgramStageDataElement( programStageA, dataElementA, 1 ) ) );

        programA.setProgramStages( Sets.newHashSet( programStageA ) );

        LegendSet legendSetA = createLegendSet( 'A' );

        when( dataElementService.getDataElement( dimension ) ).thenReturn( dataElementA );
        when( legendSetService.getLegendSet( legendSetUid ) ).thenReturn( legendSetA );

        QueryItem queryItem = subject.getQueryItemFromDimension( dimension + ITEM_SEP + legendSetUid, programA,
            EventOutputType.EVENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( dataElementA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );
        assertThat( queryItem.getLegendSet(), is( legendSetA ) );
    }

    @Test
    public void verifyDimensionWithLegendsetAndProgramStageReturnsDataElement()
    {
        String legendSetUid = CodeGenerator.generateUid();

        DataElement dataElementA = createDataElement( 'A' );

        ProgramStage programStageA = createProgramStage( 'A', programA );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( createProgramStageDataElement( programStageA, dataElementA, 1 ) ) );

        programA.setProgramStages( Sets.newHashSet( programStageA ) );

        LegendSet legendSetA = createLegendSet( 'A' );

        when( dataElementService.getDataElement( dimension ) ).thenReturn( dataElementA );
        when( legendSetService.getLegendSet( legendSetUid ) ).thenReturn( legendSetA );
        when( programStageService.getProgramStage( programStageUid ) ).thenReturn( programStageA );

        // programStageUid.dimensionUid-legendSetUid
        QueryItem queryItem = subject.getQueryItemFromDimension(
            programStageUid + PROGRAMSTAGE_SEP + dimension + ITEM_SEP + legendSetUid, programA,
            EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( dataElementA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( programStageA ) );
        assertThat( queryItem.getLegendSet(), is( legendSetA ) );

        verifyNoMoreInteractions( attributeService );
        verifyNoMoreInteractions( programIndicatorService );
    }

    @Test
    public void verifyDimensionReturnsTrackedEntityAttribute()
    {
        OptionSet optionSetA = createOptionSet( 'A' );

        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttribute.setUid( dimension );
        trackedEntityAttribute.setOptionSet( optionSetA );

        ProgramTrackedEntityAttribute programTrackedEntityAttribute = createProgramTrackedEntityAttribute( programA,
            trackedEntityAttribute );

        programA.setProgramAttributes( Lists.newArrayList( programTrackedEntityAttribute ) );

        when( attributeService.getTrackedEntityAttribute( dimension ) ).thenReturn( trackedEntityAttribute );

        QueryItem queryItem = subject.getQueryItemFromDimension( dimension, programA, EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( trackedEntityAttribute ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );
        assertThat( queryItem.getLegendSet(), is( nullValue() ) );
        assertThat( queryItem.getOptionSet(), is( optionSetA ) );
        verifyNoMoreInteractions( programIndicatorService );
    }

    @Test
    public void verifyDimensionReturnsProgramIndicator()
    {
        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', programA, "", "" );
        programIndicatorA.setUid( dimension );

        programA.setProgramIndicators( Sets.newHashSet( programIndicatorA ) );
        when( programIndicatorService.getProgramIndicatorByUid( programIndicatorA.getUid() ) )
            .thenReturn( programIndicatorA );

        QueryItem queryItem = subject.getQueryItemFromDimension( dimension, programA, EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( programIndicatorA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );
        assertThat( queryItem.getLegendSet(), is( nullValue() ) );
    }

    @Test
    public void verifyDimensionReturnsProgramIndicatorWithRelationship()
    {
        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', programA, "", "" );
        programIndicatorA.setUid( dimension );

        RelationshipType relationshipType = createRelationshipType();

        programA.setProgramIndicators( Sets.newHashSet( programIndicatorA ) );
        when( programIndicatorService.getProgramIndicatorByUid( programIndicatorA.getUid() ) )
            .thenReturn( programIndicatorA );
        when( relationshipTypeService.getRelationshipType( relationshipType.getUid() ) ).thenReturn( relationshipType );

        QueryItem queryItem = subject.getQueryItemFromDimension(
            relationshipType.getUid() + PROGRAMSTAGE_SEP + dimension, programA, EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( programIndicatorA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );
        assertThat( queryItem.getLegendSet(), is( nullValue() ) );
        assertThat( queryItem.getRelationshipType(), is( relationshipType ) );
    }

    @Test
    public void verifyForeignProgramIndicatorWithoutRelationshipIsNotAccepted()
    {

        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', programA, "", "" );
        programIndicatorA.setUid( dimension );

        when( programIndicatorService.getProgramIndicatorByUid( programIndicatorA.getUid() ) )
            .thenReturn( programIndicatorA );

        exception.expect( IllegalQueryException.class );
        exception.expectMessage(
            "Item identifier does not reference any data element, attribute or indicator part of the program" );

        subject.getQueryItemFromDimension( dimension, programA, EventOutputType.ENROLLMENT );
    }

    @Test
    public void verifyForeignProgramIndicatorWithRelationshipIsAccepted()
    {

        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', programA, "", "" );
        programIndicatorA.setUid( dimension );

        RelationshipType relationshipType = createRelationshipType();
        when( programIndicatorService.getProgramIndicatorByUid( programIndicatorA.getUid() ) )
            .thenReturn( programIndicatorA );
        when( relationshipTypeService.getRelationshipType( relationshipType.getUid() ) ).thenReturn( relationshipType );
        QueryItem queryItem = subject.getQueryItemFromDimension(
            relationshipType.getUid() + PROGRAMSTAGE_SEP + dimension, programA, EventOutputType.ENROLLMENT );

        assertThat( queryItem, is( notNullValue() ) );
        assertThat( queryItem.getItem(), is( programIndicatorA ) );
        assertThat( queryItem.getProgram(), is( programA ) );
        assertThat( queryItem.getProgramStage(), is( nullValue() ) );
        assertThat( queryItem.getLegendSet(), is( nullValue() ) );
        assertThat( queryItem.getRelationshipType(), is( relationshipType ) );
    }

    private RelationshipType createRelationshipType()
    {
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( CodeGenerator.generateUid() );
        return relationshipType;
    }
}