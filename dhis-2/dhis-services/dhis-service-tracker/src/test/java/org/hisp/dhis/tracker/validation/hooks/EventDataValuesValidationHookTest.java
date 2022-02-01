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
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
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
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
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
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
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
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue validDataValue = dataValue();
        validDataValue.setUpdatedAt( null );
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
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( null );

        ProgramStage programStage = programStage( dataElement );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) ).build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1304, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void failValidationWhenAMandatoryDataElementIsMissing()
    {
        setUpIdentifiers();

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
    void succeedsWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded()
    {
        setUpIdentifiers();

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
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue() ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void succeedsWhenMandatoryDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode()
    {
        TrackerIdentifierParams params = TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.CODE )
            .programIdScheme( TrackerIdentifier.UID )
            .programStageIdScheme( TrackerIdentifier.UID )
            .dataElementIdScheme( TrackerIdentifier.CODE )
            .build();
        when( context.getIdentifiers() ).thenReturn( params );

        DataElement dataElement = dataElement();
        dataElement.setCode( "DE_424050" );
        when( context.getDataElement( dataElement.getCode() ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement, true );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue dataValue = dataValue();
        dataValue.setDataElement( "DE_424050" );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( dataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenDataElementIsNotPresentInProgramStage()
    {
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        DataElement notPresentDataElement = dataElement();
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

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue notPresentDataValue = dataValue();
        notPresentDataValue.setDataElement( "de_not_present_in_program_stage" );
        Event event = Event.builder()
            .programStage( "PROGRAM_STAGE" )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue(), notPresentDataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1305, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    void succeedsWhenDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode()
    {
        TrackerIdentifierParams params = TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.CODE )
            .programIdScheme( TrackerIdentifier.UID )
            .programStageIdScheme( TrackerIdentifier.UID )
            .dataElementIdScheme( TrackerIdentifier.CODE )
            .build();
        when( context.getIdentifiers() ).thenReturn( params );

        DataElement dataElement = dataElement();
        dataElement.setCode( "DE_424050" );
        when( context.getDataElement( dataElement.getCode() ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue dataValue = dataValue();
        dataValue.setDataElement( "DE_424050" );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue ) ).build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void failValidationWhenDataElementValueTypeIsNull()
    {
        setUpIdentifiers();

        DataElement dataElement = dataElement();
        DataElement invalidDataElement = dataElement( null );
        when( context.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = programStage( dataElement );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        DataValue validDataValue = dataValue( "QX4LpiTZmUH" );
        when( context.getFileResource( validDataValue.getValue() ) ).thenReturn( null );

        ProgramStage programStage = programStage( validDataElement );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, false );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement();
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, false );
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
        setUpIdentifiers();

        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( context.getDataElement( dataElementUid ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );
        DataValue validDataValue = dataValue( "QX4LpiTZmUH" );
        when( context.getFileResource( validDataValue.getValue() ) ).thenReturn( fileResource );
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
        setUpIdentifiers();

        DataValue validDataValue = dataValue( "code" );
        DataValue nullDataValue = dataValue( null );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( Arrays.asList( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
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
        setUpIdentifiers();

        DataValue validDataValue = dataValue( "value" );
        validDataValue.setDataElement( dataElementUid );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( Arrays.asList( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( context.getDataElement( dataElementUid ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
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
        setUpIdentifiers();

        DataElement invalidDataElement = dataElement( valueType );
        when( context.getDataElement( dataElementUid ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = programStage( dataElement() );
        when( context.getProgramStage( programStageUid ) ).thenReturn( programStage );

        ValidationErrorReporter reporter = new ValidationErrorReporter( context );

        DataValue validDataValue = dataValue();
        validDataValue.setDataElement( dataElementUid );
        validDataValue.setValue( value );
        Event event = Event.builder()
            .programStage( programStage.getUid() )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        hook.validateEvent( reporter, event );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    private void setUpIdentifiers()
    {
        TrackerIdentifierParams params = TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.UID )
            .programIdScheme( TrackerIdentifier.UID )
            .programStageIdScheme( TrackerIdentifier.UID )
            .dataElementIdScheme( TrackerIdentifier.UID )
            .build();
        when( context.getIdentifiers() ).thenReturn( params );
    }

    private DataElement dataElement( ValueType type )
    {
        DataElement dataElement = dataElement();
        dataElement.setValueType( type );
        return dataElement;
    }

    private DataElement dataElement()
    {
        DataElement dataElement = new DataElement();
        dataElement.setValueType( ValueType.TEXT );
        dataElement.setUid( dataElementUid );
        return dataElement;
    }

    private DataValue dataValue( String value )
    {
        DataValue dataValue = dataValue();
        dataValue.setValue( value );
        return dataValue;
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

    private ProgramStage programStage( DataElement dataElement )
    {
        return programStage( dataElement, false );
    }

    private ProgramStage programStage( DataElement dataElement, boolean compulsory )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( programStageUid );
        programStage
            .setProgramStageDataElements( getProgramStageDataElements( dataElement, programStage, compulsory ) );

        return programStage;
    }

    private Set<ProgramStageDataElement> getProgramStageDataElements( DataElement dataElement,
        ProgramStage programStage, boolean compulsory )
    {
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement( programStage, dataElement );
        programStageDataElement.setCompulsory( compulsory );
        return Set.of( programStageDataElement );
    }

}
