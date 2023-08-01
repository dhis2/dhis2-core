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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1083;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1102;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class SecurityOwnershipValidatorTest extends DhisConvenienceTest {
  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String TE_ID = "TEI_ID";

  private static final String TE_TYPE_ID = "TEI_TYPE_ID";

  private static final String PROGRAM_ID = "PROGRAM_ID";

  private static final String PS_ID = "PS_ID";

  private SecurityOwnershipValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private AclService aclService;

  @Mock private TrackerOwnershipManager ownershipAccessManager;

  @Mock private OrganisationUnitService organisationUnitService;

  private User user;

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  private TrackerIdSchemeParams idSchemes;

  @BeforeEach
  public void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    user = makeUser("A");
    when(bundle.getUser()).thenReturn(user);

    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TE_TYPE_ID);
    program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);

    programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator =
        new SecurityOwnershipValidator(aclService, ownershipAccessManager, organisationUnitService);
  }

  @Test
  void verifyValidationSuccessForEventUsingDeleteStrategy() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.DELETE);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);

    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);

    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForNonTrackerEventUsingCreateStrategy() {
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForTrackerEventCreation() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForTrackerEventUpdate() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategy() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);
    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategyOutsideCaptureScopeWithBrokenGlass() {
    String enrollmentUid = CodeGenerator.generateUid();
    String eventUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(eventUid)
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);
    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);
    when(preheat.getProgramOwner())
        .thenReturn(
            Collections.singletonMap(
                TE_ID,
                Collections.singletonMap(
                    PROGRAM_ID,
                    new TrackedEntityProgramOwnerOrgUnit(TE_ID, PROGRAM_ID, organisationUnit))));
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    when(ownershipAccessManager.hasAccess(user, TE_ID, organisationUnit, program))
        .thenReturn(false);
    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1102);

    when(ownershipAccessManager.hasAccess(user, TE_ID, organisationUnit, program)).thenReturn(true);

    reporter = new Reporter(idSchemes);
    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategyAndUserWithAuthority() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    when(bundle.getUser()).thenReturn(changeCompletedEventAuthorisedUser());
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);

    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationFailsForTrackerEventCreationAndUserNotInOrgUnitCaptureScope() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(false);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @Test
  void
      verifyValidationFailsForEventCreationThatIsCreatableInSearchScopeAndUserNotInOrgUnitSearchHierarchy() {
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.SCHEDULE)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserSearchHierarchyCached(user, organisationUnit))
        .thenReturn(false);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1000);
  }

  @Test
  void verifyValidationFailsForEventUsingUpdateStrategyAndUserWithoutAuthority() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    Enrollment enrollment = getEnrollment(enrollmentUid);
    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);
    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1083);
  }

  @Test
  void verifySuccessEventValidationWhenEventHasNoOrgUnitAssigned() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    Enrollment enrollment = getEnrollment(enrollmentUid);

    Event preheatEvent = getEvent();
    preheatEvent.setEnrollment(enrollment);
    preheatEvent.setOrganisationUnit(null);

    when(preheat.getEvent(event.getEvent())).thenReturn(preheatEvent);
    when(preheat.getEnrollment(event.getEnrollment())).thenReturn(enrollment);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validator.validate(reporter, bundle, event);

    verify(organisationUnitService, times(0)).isInUserHierarchyCached(user, organisationUnit);

    assertIsEmpty(reporter.getErrors());
  }

  private TrackedEntity teWithNoEnrollments() {
    TrackedEntity trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setUid(TE_ID);
    trackedEntity.setEnrollments(Sets.newHashSet());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  private Enrollment getEnrollment(String enrollmentUid) {
    if (StringUtils.isEmpty(enrollmentUid)) {
      enrollmentUid = CodeGenerator.generateUid();
    }
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(enrollmentUid);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setTrackedEntity(teWithNoEnrollments());
    enrollment.setProgram(program);
    enrollment.setStatus(ProgramStatus.ACTIVE);
    return enrollment;
  }

  private Event getEvent() {
    Event event = new Event();
    event.setProgramStage(programStage);
    event.setOrganisationUnit(organisationUnit);
    event.setEnrollment(new Enrollment());
    event.setStatus(EventStatus.COMPLETED);
    return event;
  }

  private User changeCompletedEventAuthorisedUser() {
    return makeUser("A", Lists.newArrayList("F_UNCOMPLETE_EVENT"));
  }
}
