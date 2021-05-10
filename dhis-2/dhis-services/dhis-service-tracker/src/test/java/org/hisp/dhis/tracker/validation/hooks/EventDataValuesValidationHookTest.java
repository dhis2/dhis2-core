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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class EventDataValuesValidationHookTest
{
    private EventDataValuesValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    private Event event;

    @Mock
    private ProgramStage programStage;

    @Mock
    private DataElement dataElement;

    private static final String programStageUid = "programStageUid";

    private static final String dataElementUid = "dataElement";

    @Before
    public void setUp()
    {
        hookToTest = new EventDataValuesValidationHook();

        DataElement validDataElement = new DataElement();
        validDataElement.setValueType( ValueType.TEXT );

        when( dataElement.getUid() ).thenReturn( dataElementUid );
        when( dataElement.getValueType() ).thenReturn( ValueType.TEXT );
        when( event.getProgramStage() ).thenReturn( programStageUid );
        when( event.getStatus() ).thenReturn( EventStatus.SKIPPED );

        when( validationContext.getBundle() ).thenReturn( TrackerBundle.builder().build() );
        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( dataElement );
        when( validationContext.getProgramStage( anyString() ) ).thenReturn( programStage );
        DataElement dataElement = new DataElement();
        dataElement.setUid( dataElementUid );

        when( programStage.getProgramStageDataElements() ).thenReturn(
                new HashSet<>( Collections.singletonList( new ProgramStageDataElement( programStage, dataElement ) ) ) );
    }

    @Test
    public void successValidationWhenDataElementIsValid()
    {
        // Given
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( getDataValue() ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), empty() );
    }

    @Test
    public void successValidationWhenCreatedAtIsNull()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setCreatedAt( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenUpdatedAtIsNull()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setUpdatedAt( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenDataElementIsInvalid()
    {
        // Given
        DataValue validDataValue = getDataValue();

        // When
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( null );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1304, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenMandatoryDataElementIsNotPresent()
    {
        // Given

        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( "MANDATORY_DE" );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        ProgramStageDataElement stageDataElement = new ProgramStageDataElement();
        DataElement validDataElement = new DataElement();
        validDataElement.setUid( getDataValue().getDataElement() );
        stageDataElement.setDataElement( validDataElement );
        stageDataElement.setCompulsory( true );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement, stageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( getDataValue() ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1303, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failSuccessWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded()
    {
        // Given
        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( "MANDATORY_DE" );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        ProgramStageDataElement stageDataElement = new ProgramStageDataElement();
        DataElement validDataElement = new DataElement();
        validDataElement.setUid( getDataValue().getDataElement() );
        stageDataElement.setDataElement( validDataElement );
        stageDataElement.setCompulsory( true );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement, stageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( getDataValue() ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), empty() );
    }

    @Test
    public void failValidationWhenDataElementIsNotPresentInProgramStage()
    {
        // Given
        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( getDataValue().getDataElement() );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        DataValue notPresentDataValue = getDataValue();
        notPresentDataValue.setDataElement( "de_not_present_in_progam_stage" );

        DataElement notPresentDataElement = new DataElement();
        notPresentDataElement.setValueType( ValueType.TEXT );
        notPresentDataElement.setUid( "de_not_present_in_progam_stage" );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( getDataValue(), notPresentDataValue ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );
        when( validationContext.getDataElement( "de_not_present_in_progam_stage" ) )
                .thenReturn( notPresentDataElement );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1305, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenDataElementValueTypeIsNull()
    {
        // Given
        DataValue validDataValue = getDataValue();

        DataElement invalidDataElement = new DataElement();
        invalidDataElement.setUid( dataElementUid );
        invalidDataElement.setValueType( null );

        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenFileResourceIsNull()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getFileResource( validDataValue.getDataElement() ) ).thenReturn( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1084, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void successValidationWhenFileResourceValueIsNullAndDataElementIsNotCompulsory()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, false );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenFileResourceValueIsNullAndDataElementIsCompulsory()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        String programStageUid = "programStageUid";

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );

        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getProgramStage() ).thenReturn( programStageUid );

        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void validationWhenDataElementValueIsNullAndValidationStrategyOnUpdate()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.TEXT );

        String programStageUid = "programStageUid";

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );

        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getProgramStage() ).thenReturn( programStageUid );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );

        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );

        // When
        reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void validationWhenDataElementValueIsNullAndValidationStrategyOnComplete()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.TEXT );

        String programStageUid = "programStageUid";

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );

        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getProgramStage() ).thenReturn( programStageUid );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );

        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );

        // When
        reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void validationWhenDataElementValueIsNullAndEventStatusSkippedOrScheduled()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.TEXT );

        String programStageUid = "programStageUid";

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );

        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getProgramStage() ).thenReturn( programStageUid );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( event.getStatus() ).thenReturn( EventStatus.SCHEDULE );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );

        when( event.getStatus() ).thenReturn( EventStatus.SKIPPED );

        // When
        reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void successValidationWhenDataElementIsNullAndDataElementIsNotCompulsory()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( null );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.TEXT );

        String programStageUid = "programStageUid";

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, false );

        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );
        when( event.getProgramStage() ).thenReturn( programStageUid );
        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getFileResource( validDataValue.getDataElement() ) ).thenReturn( null );
        when( validationContext.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );
        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenFileResourceIsAlreadyAssigned()
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );

        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getFileResource( validDataValue.getValue() ) ).thenReturn( fileResource );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1009, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenDataElementValueTypeIsInvalid()
    {
        runAndAssertValidationForDataValue( ValueType.NUMBER, "not_a_number" );
        runAndAssertValidationForDataValue( ValueType.UNIT_INTERVAL, "3" );
        runAndAssertValidationForDataValue( ValueType.PERCENTAGE, "1234" );
        runAndAssertValidationForDataValue( ValueType.INTEGER, "10.5" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_POSITIVE, "-10" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_NEGATIVE, "+10" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_ZERO_OR_POSITIVE, "-10" );
        runAndAssertValidationForDataValue( ValueType.BOOLEAN, "not_a_bool" );
        runAndAssertValidationForDataValue( ValueType.TRUE_ONLY, "false" );
        runAndAssertValidationForDataValue( ValueType.DATE, "wrong_date" );
        runAndAssertValidationForDataValue( ValueType.DATETIME, "wrong_date_time" );
        runAndAssertValidationForDataValue( ValueType.COORDINATE, "10" );
        runAndAssertValidationForDataValue( ValueType.URL, "not_valid_url" );
        runAndAssertValidationForDataValue( ValueType.FILE_RESOURCE, "not_valid_uid" );
    }

    @Test
    public void successValidationDataElementOptionValueIsValid()
    {
        DataValue validDataValue = getDataValue();
        validDataValue.setValue( "code" );

        DataElement dataElement = new DataElement();
        dataElement.setUid( dataElementUid );
        dataElement.setValueType( ValueType.TEXT );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );

        Option option1 = new Option();
        option1.setCode( "CODE1" );

        optionSet.setOptions( Arrays.asList( option, option1 ) );

        dataElement.setOptionSet( optionSet );

        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( dataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationDataElementOptionValueIsInValid()
    {
        DataValue validDataValue = getDataValue();
        validDataValue.setDataElement( dataElementUid );
        validDataValue.setValue( "value" );

        DataElement dataElement = new DataElement();
        dataElement.setUid( dataElementUid );
        dataElement.setValueType( ValueType.TEXT );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );

        Option option1 = new Option();
        option1.setCode( "CODE1" );

        optionSet.setOptions( Arrays.asList( option, option1 ) );

        dataElement.setOptionSet( optionSet );

        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( dataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( 1, reporter.getReportList().stream()
                .filter( e -> e.getErrorCode() == TrackerErrorCode.E1125 ).count() );
    }

    private void runAndAssertValidationForDataValue( ValueType valueType, String value )
    {
        // Given
        DataValue validDataValue = getDataValue();
        validDataValue.setDataElement( dataElementUid );
        validDataValue.setValue( value );

        DataElement invalidDataElement = new DataElement();
        invalidDataElement.setUid( dataElementUid );
        invalidDataElement.setValueType( valueType );

        when( validationContext.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    private DataValue getDataValue()
    {
        DataValue dataValue = new DataValue();
        dataValue.setCreatedAt( DateUtils.instantFromDateAsString( "2020-10-10" ) );
        dataValue.setUpdatedAt( DateUtils.instantFromDateAsString( "2020-10-10" ) );
        dataValue.setValue( "text" );
        dataValue.setDataElement( dataElementUid );
        return dataValue;
    }

    private ProgramStage getProgramStage( DataElement validDataElement, String programStageUid, boolean compulsory )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage
                .setProgramStageDataElements( getProgramStageDataElements( validDataElement, programStage, compulsory ) );

        return programStage;
    }

    private HashSet<ProgramStageDataElement> getProgramStageDataElements( DataElement validDataElement,
                                                                          ProgramStage programStage, boolean compulsory )
    {
        return new HashSet<ProgramStageDataElement>()
        {
            {

                ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
                programStageDataElement.setCompulsory( compulsory );
                programStageDataElement.setDataElement( validDataElement );
                programStageDataElement.setProgramStage( programStage );

                add( programStageDataElement );

            }
        };
    }

}
