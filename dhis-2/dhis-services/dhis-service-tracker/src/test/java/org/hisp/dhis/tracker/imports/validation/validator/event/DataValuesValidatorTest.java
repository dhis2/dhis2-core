/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertNoErrors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class DataValuesValidatorTest {

  private DataValuesValidator validator;

  @Mock OptionService optionService;
  @Mock TrackerPreheat preheat;

  private static final String PROGRAM_STAGE_UID = "programStageUid";

  private static final String DATA_ELEMENT_UID = "dataElement";

  private static final String ORGANISATION_UNIT_UID = UID.generate().getValue();

  @Mock private TrackerBundle bundle;

  private Reporter reporter;

  private TrackerIdSchemeParams idSchemes;

  public static Stream<Arguments> transactionsCreatingDataValues() {
    return Stream.of(
        Arguments.of(EventStatus.SCHEDULE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.SCHEDULE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.SCHEDULE, EventStatus.VISITED),
        Arguments.of(EventStatus.OVERDUE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.OVERDUE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.OVERDUE, EventStatus.VISITED),
        Arguments.of(EventStatus.SKIPPED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.SKIPPED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.SKIPPED, EventStatus.VISITED));
  }

  public static Stream<Arguments> transactionsNotCreatingDataValues() {
    return Stream.of(
        Arguments.of(EventStatus.ACTIVE, EventStatus.ACTIVE),
        Arguments.of(EventStatus.ACTIVE, EventStatus.COMPLETED),
        Arguments.of(EventStatus.ACTIVE, EventStatus.VISITED),
        Arguments.of(EventStatus.VISITED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.VISITED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.VISITED, EventStatus.VISITED),
        Arguments.of(EventStatus.COMPLETED, EventStatus.ACTIVE),
        Arguments.of(EventStatus.COMPLETED, EventStatus.COMPLETED),
        Arguments.of(EventStatus.COMPLETED, EventStatus.VISITED));
  }

  @BeforeEach
  public void setUp() {
    validator = new DataValuesValidator(optionService);

    when(bundle.getPreheat()).thenReturn(preheat);

    idSchemes = TrackerIdSchemeParams.builder().build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    reporter = new Reporter(idSchemes);
  }

  @Test
  void successValidationWhenDataElementIsValid() {
    DataElement dataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenDataElementIsInvalid() {
    DataElement dataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID))).thenReturn(null);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1304);
  }

  @Test
  void shouldFailValidationWhenAMandatoryDataElementIsMissingAndStrategyIsCreate() {
    DataElement dataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();
    ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
    DataElement mandatoryElement1 = new DataElement();
    mandatoryElement1.setUid("MANDATORY_DE");
    mandatoryStageElement1.setDataElement(mandatoryElement1);
    mandatoryStageElement1.setCompulsory(true);
    ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
    DataElement mandatoryElement2 = new DataElement();
    mandatoryElement2.setUid(DATA_ELEMENT_UID);
    mandatoryStageElement2.setDataElement(mandatoryElement2);
    mandatoryStageElement2.setCompulsory(true);
    programStage.setProgramStageDataElements(
        Set.of(mandatoryStageElement1, mandatoryStageElement2));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1303);
  }

  @Test
  void succeedsWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded() {
    DataElement dataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();
    programStage.setValidationStrategy(ValidationStrategy.ON_COMPLETE);
    ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
    DataElement mandatoryElement1 = new DataElement();
    mandatoryElement1.setUid("MANDATORY_DE");
    mandatoryStageElement1.setDataElement(mandatoryElement1);
    mandatoryStageElement1.setCompulsory(true);
    ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
    DataElement mandatoryElement2 = new DataElement();
    mandatoryElement2.setUid(DATA_ELEMENT_UID);
    mandatoryStageElement2.setDataElement(mandatoryElement2);
    mandatoryStageElement2.setCompulsory(true);
    programStage.setProgramStageDataElements(
        Set.of(mandatoryStageElement1, mandatoryStageElement2));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @MethodSource("transactionsNotCreatingDataValues")
  void shouldPassValidationWhenAMandatoryDataElementIsMissingAndDataValueIsAlreadyPresentInDB(
      EventStatus savedStatus, EventStatus newStatus) {
    DataElement dataElement = dataElement();
    UID eventUid = UID.generate();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
    DataElement mandatoryElement1 = new DataElement();
    mandatoryElement1.setUid("MANDATORY_DE");
    mandatoryStageElement1.setDataElement(mandatoryElement1);
    mandatoryStageElement1.setCompulsory(true);
    ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
    DataElement mandatoryElement2 = new DataElement();
    mandatoryElement2.setUid(DATA_ELEMENT_UID);
    mandatoryStageElement2.setDataElement(mandatoryElement2);
    mandatoryStageElement2.setCompulsory(true);
    programStage.setProgramStageDataElements(
        Set.of(mandatoryStageElement1, mandatoryStageElement2));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    when(preheat.getTrackerEvent(eventUid))
        .thenReturn(event(eventUid, savedStatus, Set.of("MANDATORY_DE", DATA_ELEMENT_UID)));

    Event event =
        TrackerEvent.builder()
            .event(eventUid)
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(newStatus)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertNoErrors(reporter);
  }

  @ParameterizedTest
  @MethodSource("transactionsCreatingDataValues")
  void shouldFailValidationWhenAMandatoryDataElementIsMissingAndDataValuesAreCreated(
      EventStatus savedStatus, EventStatus newStatus) {
    DataElement dataElement = dataElement();
    UID eventUid = UID.generate();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
    DataElement mandatoryElement1 = new DataElement();
    mandatoryElement1.setUid("MANDATORY_DE");
    mandatoryStageElement1.setDataElement(mandatoryElement1);
    mandatoryStageElement1.setCompulsory(true);
    ProgramStageDataElement mandatoryStageElement2 = new ProgramStageDataElement();
    DataElement mandatoryElement2 = new DataElement();
    mandatoryElement2.setUid(DATA_ELEMENT_UID);
    mandatoryStageElement2.setDataElement(mandatoryElement2);
    mandatoryStageElement2.setCompulsory(true);
    programStage.setProgramStageDataElements(
        Set.of(mandatoryStageElement1, mandatoryStageElement2));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    when(preheat.getTrackerEvent(eventUid)).thenReturn(event(eventUid, savedStatus));

    Event event =
        TrackerEvent.builder()
            .event(eventUid)
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(newStatus)
            .dataValues(Set.of(dataValue()))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1303);
  }

  @Test
  void succeedsWhenMandatoryDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode() {
    TrackerIdSchemeParams params =
        TrackerIdSchemeParams.builder()
            .idScheme(TrackerIdSchemeParam.CODE)
            .programIdScheme(TrackerIdSchemeParam.UID)
            .programStageIdScheme(TrackerIdSchemeParam.UID)
            .dataElementIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(params);

    DataElement dataElement = dataElement();
    dataElement.setCode("DE_424050");
    when(preheat.getDataElement(MetadataIdentifier.ofCode(dataElement.getCode())))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement, true);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue dataValue = dataValue();
    dataValue.setDataElement(MetadataIdentifier.ofCode("DE_424050"));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(dataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenDataElementIsNotPresentInProgramStage() {
    DataElement dataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    DataElement notPresentDataElement = dataElement();
    notPresentDataElement.setUid("de_not_present_in_program_stage");
    when(preheat.getDataElement(MetadataIdentifier.ofUid("de_not_present_in_program_stage")))
        .thenReturn(notPresentDataElement);

    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();
    ProgramStageDataElement mandatoryStageElement1 = new ProgramStageDataElement();
    DataElement mandatoryElement1 = new DataElement();
    mandatoryElement1.setUid(DATA_ELEMENT_UID);
    mandatoryStageElement1.setDataElement(mandatoryElement1);
    mandatoryStageElement1.setCompulsory(true);
    programStage.setProgramStageDataElements(Set.of(mandatoryStageElement1));
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);

    DataValue notPresentDataValue = dataValue();
    notPresentDataValue.setDataElement(MetadataIdentifier.ofUid("de_not_present_in_program_stage"));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(dataValue(), notPresentDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1305);
  }

  @Test
  void succeedsWhenDataElementIsPartOfProgramStageAndIdSchemeIsSetToCode() {
    TrackerIdSchemeParams params =
        TrackerIdSchemeParams.builder()
            .idScheme(TrackerIdSchemeParam.CODE)
            .programIdScheme(TrackerIdSchemeParam.UID)
            .programStageIdScheme(TrackerIdSchemeParam.UID)
            .dataElementIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(params);

    DataElement dataElement = dataElement();
    dataElement.setCode("DE_424050");
    when(preheat.getDataElement(MetadataIdentifier.ofCode(dataElement.getCode())))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue dataValue = dataValue();
    dataValue.setDataElement(MetadataIdentifier.ofCode("DE_424050"));
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(dataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenFileResourceIsNull() {
    DataElement validDataElement = dataElement(ValueType.FILE_RESOURCE);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    DataValue validDataValue = dataValue("QX4LpiTZmUH");
    when(preheat.get(FileResource.class, validDataValue.getValue())).thenReturn(null);

    ProgramStage programStage = programStage(validDataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(validDataValue))
            .build();

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1302);
  }

  @Test
  void successValidationWhenFileResourceValueIsNullAndDataElementIsNotCompulsory() {
    DataElement validDataElement = dataElement(ValueType.FILE_RESOURCE);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, false);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenFileResourceValueIsNullAndDataElementIsCompulsory() {
    DataElement validDataElement = dataElement(ValueType.FILE_RESOURCE);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenDataElementValueNullAndStrategyCreate() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenDataElementValueNullAndStrategyUpdate() {
    DataElement validDataElement = dataElement();
    UID eventUid = UID.generate();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);
    when(preheat.getTrackerEvent(eventUid)).thenReturn(new org.hisp.dhis.program.TrackerEvent());

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(eventUid)
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1076);
  }

  @Test
  void failsOnCompletedEventWithDataElementValueNullAndValidationStrategyOnUpdate() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1076);
  }

  @Test
  void succeedsOnActiveEventWithDataElementValueIsNullAndValidationStrategyOnComplete() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    programStage.setValidationStrategy(ValidationStrategy.ON_COMPLETE);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failsOnCompletedEventWithDataElementValueIsNullAndValidationStrategyOnComplete() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    programStage.setValidationStrategy(ValidationStrategy.ON_COMPLETE);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1076);
  }

  @Test
  void shouldFailWhenScheduledEventHasDataValueDefined() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue("1");
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SCHEDULE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1315);
  }

  @Test
  void shouldFailValidationWhenSkippedEventHasDataValueDefined() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue("1");
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1315);
  }

  @Test
  void shouldFailValidationWhenOverdueEventHasDataValueDefined() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, true);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue("1");
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.OVERDUE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1315);
  }

  @Test
  void successValidationWhenDataElementIsNullAndDataElementIsNotCompulsory() {
    DataElement validDataElement = dataElement();
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement, false);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setValue(null);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.COMPLETED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenFileResourceIsAlreadyAssigned() {
    DataElement validDataElement = dataElement(ValueType.FILE_RESOURCE);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    FileResource fileResource = new FileResource();
    fileResource.setAssigned(true);
    DataValue validDataValue = dataValue("QX4LpiTZmUH");
    when(preheat.get(FileResource.class, validDataValue.getValue())).thenReturn(fileResource);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1009);

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);

    reporter = new Reporter(idSchemes);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void validateFileResourceOwner() {
    DataElement validDataElement = dataElement(ValueType.FILE_RESOURCE);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    ProgramStage programStage = programStage(validDataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    FileResource fileResource = new FileResource();
    fileResource.setAssigned(true);
    DataValue validDataValue = dataValue("QX4LpiTZmUH");
    when(preheat.get(FileResource.class, validDataValue.getValue())).thenReturn(fileResource);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1009);

    event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();
    UID uid = UID.generate();
    fileResource.setFileResourceOwner(uid.getValue());

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);

    reporter = new Reporter(idSchemes);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1009);

    event =
        TrackerEvent.builder()
            .event(uid)
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();
    fileResource.setFileResourceOwner(uid.getValue());

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);

    reporter = new Reporter(idSchemes);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationWhenDataElementValueTypeIsInvalid() {
    runAndAssertValidationForDataValue(ValueType.NUMBER, "not_a_number");
    runAndAssertValidationForDataValue(ValueType.UNIT_INTERVAL, "3");
    runAndAssertValidationForDataValue(ValueType.PERCENTAGE, "1234");
    runAndAssertValidationForDataValue(ValueType.INTEGER, "10.5");
    runAndAssertValidationForDataValue(ValueType.INTEGER_POSITIVE, "-10");
    runAndAssertValidationForDataValue(ValueType.INTEGER_NEGATIVE, "+10");
    runAndAssertValidationForDataValue(ValueType.INTEGER_ZERO_OR_POSITIVE, "-10");
    runAndAssertValidationForDataValue(ValueType.BOOLEAN, "not_a_bool");
    runAndAssertValidationForDataValue(ValueType.TRUE_ONLY, "false");
    runAndAssertValidationForDataValue(ValueType.DATE, "wrong_date");
    runAndAssertValidationForDataValue(ValueType.DATETIME, "wrong_date_time");
    runAndAssertValidationForDataValue(ValueType.COORDINATE, "10");
    runAndAssertValidationForDataValue(ValueType.URL, "not_valid_url");
  }

  @Test
  void successValidationDataElementOptionValueIsValid() {
    DataValue validDataValue = dataValue("CODE");
    DataValue nullDataValue = dataValue(null);

    OptionSet optionSet = new OptionSet();
    Option option = new Option();
    option.setCode("CODE");
    Option option1 = new Option();
    option1.setCode("CODE1");
    optionSet.setOptions(List.of(option, option1));

    DataElement dataElement = dataElement();
    dataElement.setOptionSet(optionSet);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    when(optionService.existsAllOptions(any(), anyList())).thenReturn(true);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue, nullDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationDataElementOptionValueIsInValid() {
    DataValue validDataValue = dataValue("value");
    validDataValue.setDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID));

    OptionSet optionSet = new OptionSet();
    Option option = new Option();
    option.setCode("CODE");
    Option option1 = new Option();
    option1.setCode("CODE1");
    optionSet.setOptions(List.of(option, option1));

    DataElement dataElement = dataElement();
    dataElement.setOptionSet(optionSet);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1125);
  }

  @Test
  void successValidationDataElementMultiTextOptionValueIsValid() {
    DataValue validDataValue = dataValue("CODE,CODE1");
    DataValue nullDataValue = dataValue(null);

    OptionSet optionSet = new OptionSet();
    Option option = new Option();
    option.setCode("CODE");
    Option option1 = new Option();
    option1.setCode("CODE1");
    optionSet.setOptions(List.of(option, option1));

    DataElement dataElement = dataElement(ValueType.MULTI_TEXT);
    dataElement.setOptionSet(optionSet);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    when(optionService.existsAllOptions(any(), anyList())).thenReturn(true);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue, nullDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void failValidationDataElementMultiTextOptionValueIsInValid() {
    DataValue validDataValue = dataValue("CODE1,CODE2");
    validDataValue.setDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID));

    OptionSet optionSet = new OptionSet();
    Option option = new Option();
    option.setCode("CODE");
    Option option1 = new Option();
    option1.setCode("CODE1");
    optionSet.setOptions(List.of(option, option1));

    DataElement dataElement = dataElement(ValueType.MULTI_TEXT);
    dataElement.setOptionSet(optionSet);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(dataElement);

    ProgramStage programStage = programStage(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1125);
  }

  @Test
  void failValidationWhenOrgUnitValueIsInvalid() {
    DataElement validDataElement = dataElement(ValueType.ORGANISATION_UNIT);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    DataValue invalidDataValue = dataValue("invlaid_org_unit");

    ProgramStage programStage = programStage(validDataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(invalidDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1302);
  }

  @Test
  void succeedsValidationWhenOrgUnitValueIsValid() {
    DataElement validDataElement = dataElement(ValueType.ORGANISATION_UNIT);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(validDataElement);

    OrganisationUnit validOrgUnit = organisationUnit();

    DataValue validDataValue = dataValue(validOrgUnit.getUid());
    when(preheat.getOrganisationUnit(validDataValue.getValue())).thenReturn(validOrgUnit);

    ProgramStage programStage = programStage(validDataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.ACTIVE)
            .dataValues(Set.of(validDataValue))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  private void runAndAssertValidationForDataValue(ValueType valueType, String value) {
    DataElement invalidDataElement = dataElement(valueType);
    when(preheat.getDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID)))
        .thenReturn(invalidDataElement);

    ProgramStage programStage = programStage(dataElement());
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(programStage);

    DataValue validDataValue = dataValue();
    validDataValue.setDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID));
    validDataValue.setValue(value);
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .programStage(idSchemes.toMetadataIdentifier(programStage))
            .status(EventStatus.SKIPPED)
            .dataValues(Set.of(validDataValue))
            .build();

    reporter = new Reporter(idSchemes);
    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, ValidationCode.E1302);
  }

  private org.hisp.dhis.program.TrackerEvent event(
      UID uid, EventStatus status, Set<String> dataElements) {
    org.hisp.dhis.program.TrackerEvent event = new org.hisp.dhis.program.TrackerEvent();
    event.setUid(uid.getValue());
    event.setStatus(status);
    event.setEventDataValues(
        dataElements.stream()
            .map(de -> new EventDataValue(de, "value"))
            .collect(Collectors.toSet()));
    return event;
  }

  private org.hisp.dhis.program.TrackerEvent event(UID uid, EventStatus status) {
    org.hisp.dhis.program.TrackerEvent event = new org.hisp.dhis.program.TrackerEvent();
    event.setUid(uid.getValue());
    event.setStatus(status);
    return event;
  }

  private DataElement dataElement(ValueType type) {
    DataElement dataElement = dataElement();
    dataElement.setValueType(type);
    return dataElement;
  }

  private DataElement dataElement() {
    DataElement dataElement = new DataElement();
    dataElement.setValueType(ValueType.TEXT);
    dataElement.setUid(DATA_ELEMENT_UID);
    return dataElement;
  }

  private DataValue dataValue(String value) {
    DataValue dataValue = dataValue();
    dataValue.setValue(value);
    return dataValue;
  }

  private DataValue dataValue() {
    DataValue dataValue = new DataValue();
    dataValue.setValue("text");
    dataValue.setDataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_UID));
    return dataValue;
  }

  private ProgramStage programStage(DataElement dataElement) {
    return programStage(dataElement, false);
  }

  private ProgramStage programStage(DataElement dataElement, boolean compulsory) {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgramStageDataElements(
        getProgramStageDataElements(dataElement, programStage, compulsory));

    return programStage;
  }

  private Set<ProgramStageDataElement> getProgramStageDataElements(
      DataElement dataElement, ProgramStage programStage, boolean compulsory) {
    ProgramStageDataElement programStageDataElement =
        new ProgramStageDataElement(programStage, dataElement);
    programStageDataElement.setCompulsory(compulsory);
    return Set.of(programStageDataElement);
  }

  private OrganisationUnit organisationUnit() {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORGANISATION_UNIT_UID);
    return organisationUnit;
  }
}
