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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class EventDataValuesValidationHookTest
{

    private EventDataValuesValidationHook hook;

    @Mock
    private TrackerImportValidationContext context;

    private static final String programStageUid = "programStageUid";

    private static final String dataElementUid = "dataElement";

    @BeforeEach
    public void setUp()
    {
        hook = new EventDataValuesValidationHook();

        when( context.getBundle() ).thenReturn( TrackerBundle.builder().build() );
    }

    @Test
    void successValidationWhenDataElementIsValid()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void successValidationWhenCreatedAtIsNull()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue validDataValue = dataValue();
        validDataValue.setCreatedAt( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenUpdatedAtIsNull()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setUpdatedAt( null );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenDataElementIsInvalid()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( null );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        DataValue validDataValue = dataValue();

        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) ).build();
        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1304, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failValidationWhenAMandatoryDataElementIsMissing()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( "MANDATORY_DE" );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
        DataElement mandatoryElement2 = new DataElement();
        mandatoryElement2.setUid( dataValue().getDataElement() );
        mandatoryStageElement2.setDataElement( mandatoryElement2 );
        mandatoryStageElement2.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1, mandatoryStageElement2 ) );
        when( context.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( "PROGRAM_STAGE" )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( dataValue() ) ).build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1303, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failSuccessWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( "MANDATORY_DE" );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
        DataElement mandatoryElement2 = new DataElement();
        mandatoryElement2.setUid( dataValue().getDataElement() );
        mandatoryStageElement2.setDataElement( mandatoryElement2 );
        mandatoryStageElement2.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1, mandatoryStageElement2 ) );
        when( context.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        // TODO difference between this test and above is status == ACTIVE
        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( "PROGRAM_STAGE" )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue() ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenDataElementIsNotPresentInProgramStage()
    {
        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        DataElement notPresentDataElement = new DataElement();
        notPresentDataElement.setValueType( ValueType.TEXT );
        notPresentDataElement.setUid( "de_not_present_in_program_stage" );
        when( context.getDataElement( "de_not_present_in_program_stage" ) )
            .thenReturn( notPresentDataElement );

        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( dataValue().getDataElement() );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1 ) );
        when( context.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        DataValue notPresentDataValue = dataValue();
        notPresentDataValue.setDataElement( "de_not_present_in_program_stage" );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( "PROGRAM_STAGE" )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue(), notPresentDataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1305, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failValidationWhenDataElementValueTypeIsNull()
    {
        DataElement dataElement = dataElement();
        DataElement invalidDataElement = dataElement( null );
        when( context.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failValidationWhenFileResourceIsNull()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );
        when( context.getFileResource( validDataValue.getValue() ) ).thenReturn( null );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage
            .setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, validDataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1084, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void successValidationWhenFileResourceValueIsNullAndDataElementIsNotCompulsory()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, false );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenFileResourceValueIsNullAndDataElementIsCompulsory()
    {

        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failsOnActiveEventWithDataElementValueNullAndValidationStrategyOnUpdate()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failsOnCompletedEventWithDataElementValueNullAndValidationStrategyOnUpdate()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void succeedsOnActiveEventWithDataElementValueIsNullAndValidationStrategyOnComplete()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failsOnCompletedEventWithDataElementValueIsNullAndValidationStrategyOnComplete()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1076, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void succeedsOnScheduledEventWithDataElementValueIsNullAndEventStatusSkippedOrScheduled()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SCHEDULE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void succeedsOnSkippedEventWithDataElementValueIsNullAndEventStatusSkippedOrScheduled()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, true );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void successValidationWhenDataElementIsNullAndDataElementIsNotCompulsory()
    {
        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = getProgramStage( validDataElement, programStageUid, false );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenFileResourceIsAlreadyAssigned()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );
        DataValue validDataValue = dataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );
        when( context.getFileResource( validDataValue.getValue() ) ).thenReturn( fileResource );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage
            .setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, validDataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1009, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failValidationWhenDataElementValueTypeIsInvalid()
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
    void successValidationDataElementOptionValueIsValid()
    {
        DataValue validDataValue = dataValue();
        validDataValue.setValue( "code" );
        DataValue nullDataValue = dataValue();
        nullDataValue.setValue( null );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( Arrays.asList( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue, nullDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationDataElementOptionValueIsInValid()
    {
        DataValue validDataValue = dataValue();
        validDataValue.setDataElement( dataElementUid );
        validDataValue.setValue( "value" );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( Arrays.asList( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage.setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1125 ).count() );
    }

    private void runAndAssertValidationForDataValue( ValueType valueType, String value )
    {
        DataElement invalidDataElement = dataElement( valueType );
        when( context.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage
            .setProgramStageDataElements( Set.of( new ProgramStageDataElement( programStage, dataElement() ) ) );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setDataElement( dataElementUid );
        validDataValue.setValue( value );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @NotNull
    private DataElement dataElement( ValueType type )
    {
        DataElement dataElement = dataElement();
        dataElement.setValueType( type );
        return dataElement;
    }

    @NotNull
    private DataElement dataElement()
    {
        DataElement dataElement = new DataElement();
        dataElement.setValueType( ValueType.TEXT );
        dataElement.setUid( dataElementUid );
        return dataElement;
    }

    private DataValue dataValue()
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
        return new HashSet<>()
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
