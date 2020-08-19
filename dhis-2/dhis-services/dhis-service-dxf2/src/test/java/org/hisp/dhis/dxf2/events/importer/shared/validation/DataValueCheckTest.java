package org.hisp.dhis.dxf2.events.importer.shared.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStageDataElement;
import static org.hisp.dhis.dxf2.events.importer.EventTestUtils.createDataValue;
import static org.hisp.dhis.dxf2.events.importer.EventTestUtils.createEventDataValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ValidationStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

public class DataValueCheckTest extends BaseValidationTest
{
    private DataValueCheck rule;

    private ProgramStage programStageA;

    @Before
    public void setUp()
    {
        rule = new DataValueCheck();
        final Program programA = createProgram( 'A' );
        programStageA = createProgramStage( 'A', programA );
        // THIS TRIGGERS THE MANDATORY DATA ELEMENT VALIDATION
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        when( workContext.getProgramStage( IdScheme.UID, "prgstg1" ) ).thenReturn( programStageA );
    }

    @Test
    public void verifyNoErrorOnNoDataValues()
    {
        assertNoError( rule.check( new ImmutableEvent( event ), this.workContext ) );
    }

    @Test
    public void verifyNoMandatoryCheckHasNoErrors()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        event.setProgramStage( "prgstg1" );

        DataElement de1 = addToDataElementMap( createDataElement( 'A' ) );
        DataElement de2 = addToDataElementMap( createDataElement( 'B' ) );

        DataValue dv1 = createDataValue( de1.getUid(), "1" );
        DataValue dv2 = createDataValue( de2.getUid(), "2" );

        event.setDataValues( Sets.newHashSet( dv1, dv2 ) );
        assertNoError( rule.check( new ImmutableEvent( event ), this.workContext ) );
    }

    @Test
    public void verifyMandatoryCheckFailsOnMandatoryDataElement()
    {
        event.setProgramStage( "prgstg1" );
        DataElement de1 = addToDataElementMap( createDataElement( 'A' ) );
        DataElement de2 = addToDataElementMap( createDataElement( 'B' ) );
        DataElement de3 = addToDataElementMap( createDataElement( 'C' ) );

        programStageA.setProgramStageDataElements( Sets.newHashSet(
            createProgramStageDataElement( programStageA, de1, 1, true ),
            createProgramStageDataElement( programStageA, de2, 2, true ),
            createProgramStageDataElement( programStageA, de3, 3, true ) ) );

        addToDataValueMap( event.getUid(),
            createEventDataValue( de1.getUid(), "1" ),
            createEventDataValue( de2.getUid(), "2" ) );

        DataValue dv1 = createDataValue( de1.getUid(), "1" );
        DataValue dv2 = createDataValue( de2.getUid(), "2" );

        event.setDataValues( Sets.newHashSet( dv1, dv2 ) );
        final ImportSummary summary = rule.check( new ImmutableEvent( event ), this.workContext );
        assertHasError( summary, event, null );
        assertThat( summary.getConflicts(), hasSize( 1 ) );
        assertThat( summary.getConflicts().iterator().next().getValue(), is( "value_required_but_not_provided" ) );
        assertThat( summary.getConflicts().iterator().next().getObject(), is( de3.getUid() ) );
    }

    @Test
    public void verifyMandatoryCheckSucceeds()
    {
        event.setProgramStage( "prgstg1" );
        DataElement de1 = addToDataElementMap( createDataElement( 'A' ) );
        DataElement de2 = addToDataElementMap( createDataElement( 'B' ) );

        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        programStageA.setProgramStageDataElements( Sets.newHashSet(
            createProgramStageDataElement( programStageA, de1, 1, true ),
            createProgramStageDataElement( programStageA, de2, 2, true ) ) );

        addToDataValueMap( event.getUid(),
            createEventDataValue( de1.getUid(), "1" ),
            createEventDataValue( de2.getUid(), "2" ) );

        DataValue dv1 = createDataValue( de1.getUid(), "1" );
        DataValue dv2 = createDataValue( de2.getUid(), "2" );

        event.setDataValues( Sets.newHashSet( dv1, dv2 ) );
        final ImportSummary summary = rule.check( new ImmutableEvent( event ), this.workContext );
        assertNoError( summary );
    }

    @Test
    public void verifyValidationFailOnMissingDataElement()
    {
        event.setProgramStage( "prgstg1" );
        DataElement de1 = addToDataElementMap( createDataElement( 'A' ) );

        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        programStageA.setProgramStageDataElements( Sets.newHashSet(
            createProgramStageDataElement( programStageA, de1, 1, true ) ) );

        addToDataValueMap( event.getUid(),
            createEventDataValue( de1.getUid(), "1" ),
            createEventDataValue( "iDontExist", "2" ) );

        DataValue dv1 = createDataValue( de1.getUid(), "1" );
        DataValue dv2 = createDataValue( "iDontExist", "2" );

        event.setDataValues( Sets.newHashSet( dv1, dv2 ) );
        final ImportSummary summary = rule.check( new ImmutableEvent( event ), this.workContext );
        assertHasError( summary, event, null );
        assertThat( summary.getConflicts(), hasSize( 1 ) );
        assertThat( summary.getConflicts().iterator().next().getValue(),
            is( "iDontExist is not a valid data element" ) );
        assertThat( summary.getConflicts().iterator().next().getObject(), is( "dataElement" ) );
    }

    @Test
    public void verifyValidationFailOnJsonSerializationError()
        throws JsonProcessingException
    {
        ObjectMapper localObjectMapper = mock( ObjectMapper.class );

        when( serviceDelegator.getJsonMapper() ).thenReturn( localObjectMapper );
        when( localObjectMapper.writeValueAsString( Mockito.any() ) )
            .thenThrow( new JsonProcessingException( "Error" )
            {
            } );

        event.setProgramStage( "prgstg1" );
        DataElement de1 = addToDataElementMap( createDataElement( 'A' ) );

        final Program programA = createProgram( 'A' );
        final ProgramStage programStageA = createProgramStage( 'A', programA );
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        programStageA.setProgramStageDataElements( Sets.newHashSet(
            createProgramStageDataElement( programStageA, de1, 1, true ) ) );

        when( workContext.getProgramStage( IdScheme.UID, "prgstg1" ) ).thenReturn( programStageA );

        addToDataValueMap( event.getUid(),
            createEventDataValue( de1.getUid(), "1" ) );

        DataValue dv1 = createDataValue( de1.getUid(), "1" );

        event.setDataValues( Sets.newHashSet( dv1 ) );
        final ImportSummary summary = rule.check( new ImmutableEvent( event ), this.workContext );

        assertHasError( summary, event, null );
        assertThat( summary.getConflicts(), hasSize( 1 ) );
        assertThat( summary.getConflicts().iterator().next().getValue(), is( "Invalid data value found." ) );
        assertThat( summary.getConflicts().iterator().next().getObject(), is( de1.getUid() ) );
    }

}
