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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
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
class DataValuesValidatorTest
{

    private DataValuesValidator validator;

    @Mock
    TrackerPreheat preheat;

    private static final String programStageUid = "programStageUid";

    private static final String dataElementUid = "dataElement";

    private static final String organisationUnitUid = "organisationUnitUid";

    @Mock
    private TrackerBundle bundle;

    private Reporter reporter;

    private TrackerIdSchemeParams idSchemes;

    @BeforeEach
    public void setUp()
    {
        validator = new DataValuesValidator();

        when( bundle.getPreheat() ).thenReturn( preheat );

        idSchemes = TrackerIdSchemeParams.builder().build();
        when( preheat.getIdSchemes() ).thenReturn( idSchemes );
        reporter = new Reporter( idSchemes );
    }

    @Test
    void successValidationWhenDataElementIsValid()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void successValidationWhenCreatedAtIsNull()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setCreatedAt( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenUpdatedAtIsNull()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setUpdatedAt( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenDataElementIsInvalid()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( null );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) ).build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1304 );
    }

    @Test
    void failValidationWhenAMandatoryDataElementIsMissing()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setAutoFields();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( "MANDATORY_DE" );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
        DataElement mandatoryElement2 = new DataElement();
        mandatoryElement2.setUid( dataElementUid );
        mandatoryStageElement2.setDataElement( mandatoryElement2 );
        mandatoryStageElement2.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1, mandatoryStageElement2 ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( dataValue() ) ).build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1303 );
    }

    @Test
    void succeedsWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setAutoFields();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( "MANDATORY_DE" );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
        DataElement mandatoryElement2 = new DataElement();
        mandatoryElement2.setUid( dataElementUid );
        mandatoryStageElement2.setDataElement( mandatoryElement2 );
        mandatoryStageElement2.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1, mandatoryStageElement2 ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue() ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void succeedsWhenMandatoryDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode()
    {
        TrackerIdSchemeParams params = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .programIdScheme( TrackerIdSchemeParam.UID )
            .programStageIdScheme( TrackerIdSchemeParam.UID )
            .dataElementIdScheme( TrackerIdSchemeParam.CODE )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( params );

        DataElement dataElement = dataElement();
        dataElement.setCode( "DE_424050" );
        when( preheat.getDataElement( MetadataIdentifier.ofCode( dataElement.getCode() ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement, true );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue dataValue = dataValue();
        dataValue.setDataElement( MetadataIdentifier.ofCode( "DE_424050" ) );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( dataValue ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenDataElementIsNotPresentInProgramStage()
    {
        DataElement dataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        DataElement notPresentDataElement = dataElement();
        notPresentDataElement.setUid( "de_not_present_in_program_stage" );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( "de_not_present_in_program_stage" ) ) )
            .thenReturn( notPresentDataElement );

        ProgramStage programStage = new ProgramStage();
        programStage.setAutoFields();
        ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
        DataElement mandatoryElement1 = new DataElement();
        mandatoryElement1.setUid( dataElementUid );
        mandatoryStageElement1.setDataElement( mandatoryElement1 );
        mandatoryStageElement1.setCompulsory( true );
        programStage.setProgramStageDataElements( Set.of( mandatoryStageElement1 ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) )
            .thenReturn( programStage );

        DataValue notPresentDataValue = dataValue();
        notPresentDataValue.setDataElement( MetadataIdentifier.ofUid( "de_not_present_in_program_stage" ) );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue(), notPresentDataValue ) ).build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1305 );
    }

    @Test
    void succeedsWhenDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode()
    {
        TrackerIdSchemeParams params = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .programIdScheme( TrackerIdSchemeParam.UID )
            .programStageIdScheme( TrackerIdSchemeParam.UID )
            .dataElementIdScheme( TrackerIdSchemeParam.CODE )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( params );

        DataElement dataElement = dataElement();
        dataElement.setCode( "DE_424050" );
        when( preheat.getDataElement( MetadataIdentifier.ofCode( dataElement.getCode() ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue dataValue = dataValue();
        dataValue.setDataElement( MetadataIdentifier.ofCode( "DE_424050" ) );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( dataValue ) ).build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenDataElementValueTypeIsNull()
    {
        DataElement dataElement = dataElement();
        DataElement invalidDataElement = dataElement( null );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( dataValue() ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1302 );
    }

    @Test
    void failValidationWhenFileResourceIsNull()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        DataValue validDataValue = dataValue( "QX4LpiTZmUH" );
        when( preheat.get( FileResource.class, validDataValue.getValue() ) ).thenReturn( null );

        ProgramStage programStage = programStage( validDataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1084 );
    }

    @Test
    void successValidationWhenFileResourceValueIsNullAndDataElementIsNotCompulsory()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, false );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenFileResourceValueIsNullAndDataElementIsCompulsory()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1076 );
    }

    @Test
    void failsOnActiveEventWithDataElementValueNullAndValidationStrategyOnUpdate()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1076 );
    }

    @Test
    void failsOnCompletedEventWithDataElementValueNullAndValidationStrategyOnUpdate()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1076 );
    }

    @Test
    void succeedsOnActiveEventWithDataElementValueIsNullAndValidationStrategyOnComplete()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failsOnCompletedEventWithDataElementValueIsNullAndValidationStrategyOnComplete()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1076 );
    }

    @Test
    void succeedsOnScheduledEventWithDataElementValueIsNullAndEventStatusSkippedOrScheduled()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SCHEDULE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void succeedsOnSkippedEventWithDataElementValueIsNullAndEventStatusSkippedOrScheduled()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, true );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void successValidationWhenDataElementIsNullAndDataElementIsNotCompulsory()
    {
        DataElement validDataElement = dataElement();
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement, false );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setValue( null );
        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.COMPLETED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationWhenFileResourceIsAlreadyAssigned()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );
        DataValue validDataValue = dataValue( "QX4LpiTZmUH" );
        when( preheat.get( FileResource.class, validDataValue.getValue() ) ).thenReturn( fileResource );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1009 );

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );

        reporter = new Reporter( idSchemes );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void validateFileResourceOwner()
    {
        DataElement validDataElement = dataElement( ValueType.FILE_RESOURCE );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        ProgramStage programStage = programStage( validDataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );
        DataValue validDataValue = dataValue( "QX4LpiTZmUH" );
        when( preheat.get( FileResource.class, validDataValue.getValue() ) ).thenReturn( fileResource );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.CREATE );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1009 );

        event.setEvent( "XYZ" );
        fileResource.setFileResourceOwner( "ABC" );

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );

        reporter = new Reporter( idSchemes );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1009 );

        event.setEvent( "ABC" );
        fileResource.setFileResourceOwner( "ABC" );

        when( bundle.getStrategy( event ) ).thenReturn( TrackerImportStrategy.UPDATE );

        reporter = new Reporter( idSchemes );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
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
    }

    @Test
    void successValidationDataElementOptionValueIsValid()
    {
        DataValue validDataValue = dataValue( "CODE" );
        DataValue nullDataValue = dataValue( null );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( List.of( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue, nullDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationDataElementOptionValueIsInValid()
    {
        DataValue validDataValue = dataValue( "value" );
        validDataValue.setDataElement( MetadataIdentifier.ofUid( dataElementUid ) );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( List.of( option, option1 ) );

        DataElement dataElement = dataElement();
        dataElement.setOptionSet( optionSet );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1125 );
    }

    @Test
    void successValidationDataElementMultiTextOptionValueIsValid()
    {
        DataValue validDataValue = dataValue( "CODE,CODE1" );
        DataValue nullDataValue = dataValue( null );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( List.of( option, option1 ) );

        DataElement dataElement = dataElement( ValueType.MULTI_TEXT );
        dataElement.setOptionSet( optionSet );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue, nullDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void failValidationDataElementMultiTextOptionValueIsInValid()
    {
        DataValue validDataValue = dataValue( "CODE1,CODE2" );
        validDataValue.setDataElement( MetadataIdentifier.ofUid( dataElementUid ) );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );
        Option option1 = new Option();
        option1.setCode( "CODE1" );
        optionSet.setOptions( List.of( option, option1 ) );

        DataElement dataElement = dataElement( ValueType.MULTI_TEXT );
        dataElement.setOptionSet( optionSet );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( dataElement );

        ProgramStage programStage = programStage( dataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1125 );
    }

    @Test
    void failValidationWhenOrgUnitValueIsInvalid()
    {
        DataElement validDataElement = dataElement( ValueType.ORGANISATION_UNIT );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        DataValue invalidDataValue = dataValue( "invlaid_org_unit" );
        when( preheat.getOrganisationUnit( invalidDataValue.getValue() ) ).thenReturn( null );

        ProgramStage programStage = programStage( validDataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( invalidDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1007 );
    }

    @Test
    void succeedsValidationWhenOrgUnitValueIsValid()
    {
        DataElement validDataElement = dataElement( ValueType.ORGANISATION_UNIT );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( validDataElement );

        OrganisationUnit validOrgUnit = organisationUnit();

        DataValue validDataValue = dataValue( validOrgUnit.getUid() );
        when( preheat.getOrganisationUnit( validDataValue.getValue() ) ).thenReturn( validOrgUnit );

        ProgramStage programStage = programStage( validDataElement );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.ACTIVE )
            .dataValues( Set.of( validDataValue ) )
            .build();

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    private void runAndAssertValidationForDataValue( ValueType valueType, String value )
    {
        DataElement invalidDataElement = dataElement( valueType );
        when( preheat.getDataElement( MetadataIdentifier.ofUid( dataElementUid ) ) ).thenReturn( invalidDataElement );

        ProgramStage programStage = programStage( dataElement() );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStageUid ) ) )
            .thenReturn( programStage );

        DataValue validDataValue = dataValue();
        validDataValue.setDataElement( MetadataIdentifier.ofUid( dataElementUid ) );
        validDataValue.setValue( value );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .programStage( idSchemes.toMetadataIdentifier( programStage ) )
            .status( EventStatus.SKIPPED )
            .dataValues( Set.of( validDataValue ) )
            .build();

        reporter = new Reporter( idSchemes );
        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, ValidationCode.E1302 );
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
        dataValue.setDataElement( MetadataIdentifier.ofUid( dataElementUid ) );
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

    private OrganisationUnit organisationUnit()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setUid( organisationUnitUid );
        return organisationUnit;
    }

}
