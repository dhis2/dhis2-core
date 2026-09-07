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

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1091;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1099;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.acl.ErrorMessage;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class SecuritySingleEventValidatorTest extends TrackerTestBase {
  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String PROGRAM_ID = "PROGRAM_ID";

  private static final String PS_ID = "PS_ID";

  private SecuritySingleEventValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackerAccessManager trackerAccessManager;

  private final UserDetails user = UserDetails.fromUser(makeUser("A"));

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getUser()).thenReturn(user);

    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);
    organisationUnit.updatePath();

    Program program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
    validator = new SecuritySingleEventValidator(trackerAccessManager);

    lenient()
        .when(preheat.getProgramStage(MetadataIdentifier.ofUid(PS_ID)))
        .thenReturn(programStage);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldFailValidationWhenUserDoNotHaveOrgUnitInCaptureScoreForCreateStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(trackerAccessManager.canCreate(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1000, user.getUid(), List.of(user.getUid(), ORG_UNIT_ID))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void shouldFailValidationWhenUserDoesNotHaveOrgUnitInCaptureScopeForUpdateAndDeleteStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbSingleEvent());
    lenient()
        .when(
            trackerAccessManager.canUpdate(
                any(UserDetails.class),
                any(SingleEvent.class),
                any(OrganisationUnit.class),
                any(CategoryOptionCombo.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1000, user.getUid(), List.of(user.getUid(), ORG_UNIT_ID))));
    lenient()
        .when(trackerAccessManager.canDelete(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1000, user.getUid(), List.of(user.getUid(), ORG_UNIT_ID))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE"})
  void shouldFailValidationWhenUserDoesNotHavePayloadOrgUnitInCaptureScopeForUpdateStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbSingleEvent());
    when(preheat.getOrganisationUnit(event.getOrgUnit())).thenReturn(organisationUnit);
    when(trackerAccessManager.canUpdate(
            any(UserDetails.class),
            any(SingleEvent.class),
            any(OrganisationUnit.class),
            any(CategoryOptionCombo.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1000, user.getUid(), List.of(user.getUid(), ORG_UNIT_ID))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldFailValidationWhenUserDoNotHaveWriteAccessToProgramForCreateStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(trackerAccessManager.canCreate(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1091, user.getUid(), List.of(user.getUid(), PROGRAM_ID))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1091);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void shouldFailValidationWhenUserDoNotHaveWriteAccessToProgramForUpdateAndDeleteStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbSingleEvent());
    lenient()
        .when(
            trackerAccessManager.canUpdate(
                any(UserDetails.class),
                any(SingleEvent.class),
                any(OrganisationUnit.class),
                any(CategoryOptionCombo.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1091, user.getUid(), List.of(user.getUid(), PROGRAM_ID))));
    lenient()
        .when(trackerAccessManager.canDelete(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1091, user.getUid(), List.of(user.getUid(), PROGRAM_ID))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1091);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldFailValidationWhenUserDoNotHaveWriteAccessToCategoryOptionForCreateStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(trackerAccessManager.canCreate(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1099, user.getUid(), List.of(user.getUid(), "catOptUid"))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1099);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void shouldFailValidationWhenUserDoNotHaveWriteAccessToCategoryOptionForUpdateAndDeleteStrategy(
      TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbSingleEvent());
    lenient()
        .when(
            trackerAccessManager.canUpdate(
                any(UserDetails.class),
                any(SingleEvent.class),
                any(OrganisationUnit.class),
                any(CategoryOptionCombo.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1099, user.getUid(), List.of(user.getUid(), "catOptUid"))));
    lenient()
        .when(trackerAccessManager.canDelete(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(
            List.of(new ErrorMessage(E1099, user.getUid(), List.of(user.getUid(), "catOptUid"))));

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1099);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void shouldPassValidationWhenDeletingOrUpdatingSingleEvent(TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbSingleEvent());
    lenient()
        .when(
            trackerAccessManager.canUpdate(
                any(UserDetails.class),
                any(SingleEvent.class),
                any(OrganisationUnit.class),
                any(CategoryOptionCombo.class)))
        .thenReturn(List.of());
    lenient()
        .when(trackerAccessManager.canDelete(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldPassValidationWhenCreatingSingleEvent(TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(trackerAccessManager.canCreate(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassDatabaseAocToCanUpdateWhenPayloadOmitsAoc() {
    org.hisp.dhis.tracker.imports.domain.Event event = singleEvent();
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    CategoryOptionCombo dbAoc = createCategoryOptionCombo('A');
    SingleEvent dbEvent = dbSingleEvent();
    dbEvent.setAttributeOptionCombo(dbAoc);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbEvent);
    when(trackerAccessManager.canUpdate(
            any(UserDetails.class),
            any(SingleEvent.class),
            any(OrganisationUnit.class),
            any(CategoryOptionCombo.class)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, event);

    ArgumentCaptor<CategoryOptionCombo> aocCaptor =
        ArgumentCaptor.forClass(CategoryOptionCombo.class);
    verify(trackerAccessManager)
        .canUpdate(
            any(UserDetails.class),
            any(SingleEvent.class),
            any(OrganisationUnit.class),
            aocCaptor.capture());
    assertEquals(dbAoc, aocCaptor.getValue());
  }

  @Test
  void shouldPassPayloadAocToCanUpdateWhenPayloadSpecifiesAoc() {
    CategoryOptionCombo payloadAoc = createCategoryOptionCombo('B');
    MetadataIdentifier aocId = MetadataIdentifier.ofUid(payloadAoc.getUid());
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .attributeOptionCombo(aocId)
            .build();
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    CategoryOptionCombo dbAoc = createCategoryOptionCombo('A');
    SingleEvent dbEvent = dbSingleEvent();
    dbEvent.setAttributeOptionCombo(dbAoc);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbEvent);
    when(preheat.getCategoryOptionCombo(aocId)).thenReturn(payloadAoc);
    when(trackerAccessManager.canUpdate(
            any(UserDetails.class),
            any(SingleEvent.class),
            any(OrganisationUnit.class),
            any(CategoryOptionCombo.class)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, event);

    ArgumentCaptor<CategoryOptionCombo> aocCaptor =
        ArgumentCaptor.forClass(CategoryOptionCombo.class);
    verify(trackerAccessManager)
        .canUpdate(
            any(UserDetails.class),
            any(SingleEvent.class),
            any(OrganisationUnit.class),
            aocCaptor.capture());
    assertEquals(payloadAoc, aocCaptor.getValue());
  }

  @Test
  void shouldPassDatabaseAocToCanDeleteRegardlessOfPayloadAoc() {
    CategoryOptionCombo payloadAoc = createCategoryOptionCombo('B');
    MetadataIdentifier aocId = MetadataIdentifier.ofUid(payloadAoc.getUid());
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .attributeOptionCombo(aocId)
            .build();
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.DELETE);
    CategoryOptionCombo dbAoc = createCategoryOptionCombo('A');
    SingleEvent dbEvent = dbSingleEvent();
    dbEvent.setAttributeOptionCombo(dbAoc);
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(dbEvent);
    when(trackerAccessManager.canDelete(any(UserDetails.class), any(SingleEvent.class)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, event);

    ArgumentCaptor<SingleEvent> captor = ArgumentCaptor.forClass(SingleEvent.class);
    verify(trackerAccessManager).canDelete(any(UserDetails.class), captor.capture());
    assertEquals(dbAoc, captor.getValue().getAttributeOptionCombo());
  }

  private org.hisp.dhis.tracker.imports.domain.Event singleEvent() {
    return org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
        .event(UID.generate())
        .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
        .programStage(MetadataIdentifier.ofUid(PS_ID))
        .program(MetadataIdentifier.ofUid(PROGRAM_ID))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
        .build();
  }

  private SingleEvent dbSingleEvent() {
    SingleEvent event = new SingleEvent();
    event.setOrganisationUnit(organisationUnit);
    event.setAttributeOptionCombo(createCategoryOptionCombo('Z'));
    return event;
  }
}
