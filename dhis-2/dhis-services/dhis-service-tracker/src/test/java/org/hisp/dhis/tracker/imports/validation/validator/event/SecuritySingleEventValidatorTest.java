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
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1083;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1091;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1099;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class SecuritySingleEventValidatorTest extends TestBase {
  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String PROGRAM_ID = "PROGRAM_ID";

  private static final String PS_ID = "PS_ID";

  private SecuritySingleEventValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private AclService aclService;

  private final UserDetails user = UserDetails.fromUser(makeUser("A"));

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private Program program;

  private ProgramStage programStage;

  private CategoryOptionCombo categoryOptionCombo;

  private CategoryOption categoryOption;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getUser()).thenReturn(user);
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);
    organisationUnit.updatePath();

    program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);

    programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    categoryOption = createCategoryOption('A');
    categoryOptionCombo = createCategoryOptionCombo('A');
    categoryOptionCombo.setCategoryOptions(Set.of(categoryOption));

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new SecuritySingleEventValidator(aclService);

    when(bundle.getPreheat()).thenReturn(preheat);
  }

  private UserDetails setUpUserWithOrgUnit() {
    User userWithOrgUnit = makeUser("B");
    userWithOrgUnit.setOrganisationUnits(Set.of(organisationUnit));
    UserDetails currentUserDetails = UserDetails.fromUser(userWithOrgUnit);
    when(bundle.getUser()).thenReturn(currentUserDetails);
    return currentUserDetails;
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldFailValidationWhenUserDoNotHaveOrgUnitInCaptureScoreForCreateStrategy(
      TrackerImportStrategy strategy) {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void
      shouldFailValidationWhenUserDoesNotHaveDatabaseOrgUnitInCaptureScopeForUpdateAndDeleteStrategy(
          TrackerImportStrategy strategy) {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    lenient()
        .when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit()))
        .thenReturn(organisationUnit);

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
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    User user = makeUser("B");
    user.setOrganisationUnits(Set.of(organisationUnit));
    UserDetails userDetails = UserDetails.fromUser(user);
    when(bundle.getUser()).thenReturn(userDetails);

    OrganisationUnit outOfScopeOrgUnit = createOrganisationUnit('B');
    outOfScopeOrgUnit.setUid("ORG_UNIT_UID");
    outOfScopeOrgUnit.updatePath();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    lenient()
        .when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit()))
        .thenReturn(outOfScopeOrgUnit);

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
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);

    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(false);

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
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(false);
    lenient()
        .when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit()))
        .thenReturn(organisationUnit);

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
    UID enrollmentUid = UID.generate();
    MetadataIdentifier attributeOptionComboUid =
        MetadataIdentifier.ofUid(categoryOptionCombo.getUid());
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .attributeOptionCombo(attributeOptionComboUid)
            .build();

    UserDetails userDetails = setUpUserWithOrgUnit();
    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo)))
        .thenReturn(categoryOptionCombo);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataWrite(userDetails, categoryOption)).thenReturn(false);

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
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .attributeOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo)))
        .thenReturn(categoryOptionCombo);

    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataWrite(userDetails, categoryOption)).thenReturn(false);
    lenient()
        .when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit()))
        .thenReturn(organisationUnit);
    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1099);
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"UPDATE", "DELETE"})
  void shouldPassValidationWhenDeletingOrUpdatingSingleEvent(TrackerImportStrategy strategy) {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .attributeOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    SingleEvent preheatEvent = getEvent();

    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo)))
        .thenReturn(categoryOptionCombo);

    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataWrite(userDetails, categoryOption)).thenReturn(true);
    lenient()
        .when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit()))
        .thenReturn(organisationUnit);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @ParameterizedTest
  @EnumSource(
      value = TrackerImportStrategy.class,
      mode = EnumSource.Mode.INCLUDE,
      names = {"CREATE"})
  void shouldPassValidationWhenCreatingSingleEvent(TrackerImportStrategy strategy) {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .attributeOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo))
            .build();

    when(bundle.getStrategy(event)).thenReturn(strategy);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo)))
        .thenReturn(categoryOptionCombo);

    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataWrite(userDetails, categoryOption)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailValidationWhenUpdatingCompletedEventAndUserHasNoAuthorityToUncompleteEvent() {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.SingleEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.ACTIVE)
            .build();

    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    SingleEvent preheatEvent = getEvent();
    when(preheat.getSingleEvent(event.getEvent())).thenReturn(preheatEvent);

    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(bundle.getPreheat().getOrganisationUnit(event.getOrgUnit())).thenReturn(organisationUnit);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1083);
  }

  private SingleEvent getEvent() {
    SingleEvent event = new SingleEvent();
    event.setProgramStage(programStage);
    event.setOrganisationUnit(organisationUnit);
    event.setStatus(EventStatus.COMPLETED);
    return event;
  }
}
