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

import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1003;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1083;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1091;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1100;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1103;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1104;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4020;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.RelationshipTrackerConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
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
class PreCheckSecurityOwnershipValidationHookTest extends DhisConvenienceTest {

  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String TEI_ID = "TEI_ID";

  private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

  private static final String PROGRAM_ID = "PROGRAM_ID";

  private static final String PS_ID = "PS_ID";

  private PreCheckSecurityOwnershipValidationHook validatorToTest;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private AclService aclService;

  @Mock private TrackerOwnershipManager ownershipAccessManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private RelationshipTrackerConverterService converterService;

  private User user;

  private ValidationErrorReporter reporter;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  private Relationship relationship;

  private org.hisp.dhis.relationship.Relationship convertedRelationship;

  private TrackerIdSchemeParams idSchemes;

  private Map<String, Map<String, TrackedEntityProgramOwnerOrgUnit>> ownerOrgUnit;

  @BeforeEach
  public void setUp() {
    user = makeUser("A");
    when(bundle.getUser()).thenReturn(user);

    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TEI_TYPE_ID);
    program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);

    programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new ValidationErrorReporter(idSchemes);

    relationship = new Relationship();
    relationship.setRelationship("relationshipUid");

    convertedRelationship = new org.hisp.dhis.relationship.Relationship();

    TrackedEntityProgramOwnerOrgUnit owner =
        new TrackedEntityProgramOwnerOrgUnit(TEI_ID, PROGRAM_ID, organisationUnit);
    ownerOrgUnit = Map.of(TEI_ID, Map.of(PROGRAM_ID, owner));

    validatorToTest =
        new PreCheckSecurityOwnershipValidationHook(
            aclService,
            ownershipAccessManager,
            trackerAccessManager,
            organisationUnitService,
            converterService);
  }

  private void setUpUser(List<String> auths, Set<OrganisationUnit> units) {
    User u = makeUser("A", auths);
    u.setOrganisationUnits(units);
    when(bundle.getUser()).thenReturn(user);
  }

  private void setUpUserWithOrgUnit(List<String> auths) {
    setUpUser(auths, Set.of(organisationUnit));
  }

  private void setUpUserWithOrgUnit() {
    setUpUserWithOrgUnit(List.of());
  }

  @Test
  void shouldSuccessWhenUpdateTEAndUserHasWriteAccess() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    TrackedEntityInstance te = getTEIWithProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.UPDATE);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenDeleteTEWithNoEnrollmentsAndUserHasWriteAccessAndOUInCaptureScope() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEINoProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenCreateTEAndUserCorrectCaptureScope() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, trackedEntityType)).thenReturn(true);

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenDeleteTEWithDeletedEnrollmentsAndUserHasWriteAccessAndOUInCaptureScope() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithDeleteProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void
      shouldSuccessWhenDeleteTEWithEnrollmentsAndUserHasWriteAccessAndOUInCaptureScopeAndDeleteCascadeAuthority() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getUser()).thenReturn(deleteTeiAuthorisedUser());
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenDeleteTEWithEnrollmentsAndUserHasWriteAccessAndNoDeleteCascadeAuthority() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    hasTrackerError(reporter, E1100, TrackerType.TRACKED_ENTITY, trackedEntity.getUid());
  }

  @Test
  void shouldFailWhenCreateTEAndUserHasNoCorrectCaptureScope() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(false);
    when(aclService.canDataWrite(user, trackedEntityType)).thenReturn(true);

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    hasTrackerError(reporter, E1000, TrackerType.TRACKED_ENTITY, trackedEntity.getUid());
  }

  @Test
  void shouldFailWhenUpdateTEAndUserHasNoWriteAccess() {
    org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.domain.TrackedEntity.builder()
            .trackedEntity(TEI_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    TrackedEntityInstance te = getTEINoProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TEI_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of("error"));

    validatorToTest.validateTrackedEntity(reporter, bundle, trackedEntity);

    hasTrackerError(reporter, E1003, TrackerType.TRACKED_ENTITY, trackedEntity.getUid());
  }

  @Test
  void verifyValidationSuccessForEnrollment() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
    trackedEntityInstance.setUid(TEI_ID);
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(trackedEntityInstance);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentCreation() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
    trackedEntityInstance.setUid(TEI_ID);
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(trackedEntityInstance);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentDeletion() {
    String enrollmentUid = CodeGenerator.generateUid();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            user, enrollment.getTrackedEntity(), organisationUnit, program))
        .thenReturn(true);
    setUpUserWithOrgUnit();
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentWithoutEventsUsingDeleteStrategy() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            user, enrollment.getTrackedEntity(), organisationUnit, program))
        .thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentUsingDeleteStrategyAndUserWithCascadeAuthority() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(bundle.getUser()).thenReturn(deleteEnrollmentAuthorisedUser());
    when(preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            user, enrollment.getTrackedEntity(), organisationUnit, program))
        .thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void
      verifyValidationFailsForEnrollmentWithoutEventsUsingDeleteStrategyAndUserNotInOrgUnitHierarchy() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(false);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    hasTrackerError(reporter, E1000, TrackerType.ENROLLMENT, enrollment.getUid());
  }

  @Test
  void verifyValidationFailsForEnrollmentUsingDeleteStrategyAndUserWithoutCascadeAuthority() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    hasTrackerError(reporter, E1103, TrackerType.ENROLLMENT, enrollment.getUid());
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutProgramWriteAccess() {
    String enrollmentUid = CodeGenerator.generateUid();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(aclService.canDataWrite(user, program)).thenReturn(false);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    setUpUserWithOrgUnit();

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    hasTrackerError(reporter, E1091, TrackerType.ENROLLMENT, enrollment.getUid());
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutTrackedEntityTypeReadAccess() {
    String enrollmentUid = CodeGenerator.generateUid();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(false);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    setUpUserWithOrgUnit();

    validatorToTest.validateEnrollment(reporter, bundle, enrollment);

    hasTrackerError(reporter, E1104, TrackerType.ENROLLMENT, enrollment.getUid());
  }

  @Test
  void verifyValidationSuccessForEventUsingDeleteStrategy() {
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.DELETE);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);

    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);

    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForNonTrackerEventUsingCreateStrategy() {
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForTrackerEventCreation() {
    Event event =
        Event.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForTrackerEventUpdate() {
    Event event =
        Event.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategy() {
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);
    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategyOutsideCaptureScopeWithBrokenGlass() {
    String enrollmentUid = CodeGenerator.generateUid();
    String eventUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
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
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);
    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);
    when(preheat.getProgramOwner())
        .thenReturn(
            Collections.singletonMap(
                TEI_ID,
                Collections.singletonMap(
                    PROGRAM_ID,
                    new TrackedEntityProgramOwnerOrgUnit(TEI_ID, PROGRAM_ID, organisationUnit))));
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    when(ownershipAccessManager.hasAccess(user, TEI_ID, organisationUnit, program))
        .thenReturn(false);
    validatorToTest.validateEvent(reporter, bundle, event);

    hasTrackerError(reporter, TrackerErrorCode.E1102, TrackerType.EVENT, event.getUid());

    when(ownershipAccessManager.hasAccess(user, TEI_ID, organisationUnit, program))
        .thenReturn(true);

    reporter = new ValidationErrorReporter(idSchemes);
    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationSuccessForEventUsingUpdateStrategyAndUserWithAuthority() {
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    when(bundle.getUser()).thenReturn(changeCompletedEventAuthorisedUser());
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);

    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void verifyValidationFailsForTrackerEventCreationAndUserNotInOrgUnitCaptureScope() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(false);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    hasTrackerError(reporter, E1000, TrackerType.EVENT, event.getUid());
  }

  @Test
  void
      verifyValidationFailsForEventCreationThatIsCreatableInSearchScopeAndUserNotInOrgUnitSearchHierarchy() {
    Event event =
        Event.builder()
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
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(getEnrollment(null));
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserSearchHierarchyCached(user, organisationUnit))
        .thenReturn(false);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    hasTrackerError(reporter, E1000, TrackerType.EVENT, event.getUid());
  }

  @Test
  void verifyValidationFailsForEventUsingUpdateStrategyAndUserWithoutAuthority() {
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);
    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);
    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    hasTrackerError(reporter, E1083, TrackerType.EVENT, event.getUid());
  }

  @Test
  void verifySuccessEventValidationWhenProgramStageInstanceHasNoOrgUnitAssigned() {
    String enrollmentUid = CodeGenerator.generateUid();
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .programStage(MetadataIdentifier.ofUid(PS_ID))
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .status(EventStatus.COMPLETED)
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(event)).thenReturn(TrackerImportStrategy.UPDATE);
    ProgramInstance programInstance = getEnrollment(enrollmentUid);

    ProgramStageInstance programStageInstance = getEvent();
    programStageInstance.setProgramInstance(programInstance);
    programStageInstance.setOrganisationUnit(null);

    when(bundle.getProgramStageInstance(event.getEvent())).thenReturn(programStageInstance);
    when(bundle.getProgramInstance(event.getEnrollment())).thenReturn(programInstance);

    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataWrite(user, programStage)).thenReturn(true);

    validatorToTest.validateEvent(reporter, bundle, event);

    verify(organisationUnitService, times(0)).isInUserHierarchyCached(user, organisationUnit);

    assertFalse(reporter.hasErrors());
  }

  @Test
  void shouldCreateWhenUserHasWriteAccessToRelationship() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(converterService.from(preheat, relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canWrite(any(), eq(convertedRelationship))).thenReturn(List.of());

    validatorToTest.validateRelationship(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailToCreateWhenUserHasNoWriteAccessToRelationship() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(relationship)).thenReturn(CREATE);
    when(converterService.from(preheat, relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canWrite(any(), eq(convertedRelationship)))
        .thenReturn(List.of("error"));

    validatorToTest.validateRelationship(reporter, bundle, relationship);

    hasTrackerError(reporter, E4020, TrackerType.RELATIONSHIP, relationship.getUid());
  }

  @Test
  void shouldDeleteWhenUserHasWriteAccessToRelationship() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(relationship)).thenReturn(DELETE);
    when(preheat.getRelationship(relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canDelete(any(), eq(convertedRelationship))).thenReturn(List.of());

    validatorToTest.validateRelationship(reporter, bundle, relationship);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailToDeleteWhenUserHasNoWriteAccessToRelationship() {
    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(relationship)).thenReturn(DELETE);
    when(preheat.getRelationship(relationship)).thenReturn(convertedRelationship);
    when(trackerAccessManager.canDelete(any(), eq(convertedRelationship)))
        .thenReturn(List.of("error"));

    validatorToTest.validateRelationship(reporter, bundle, relationship);

    hasTrackerError(reporter, E4020, TrackerType.RELATIONSHIP, relationship.getUid());
  }

  private TrackedEntityInstance getTEINoProgramInstances() {
    TrackedEntityInstance trackedEntity = createTrackedEntityInstance(organisationUnit);
    trackedEntity.setUid(TEI_ID);
    trackedEntity.setProgramInstances(Sets.newHashSet());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  private TrackedEntityInstance getTEIWithNoProgramInstances() {
    TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance(organisationUnit);
    trackedEntityInstance.setUid(TEI_ID);
    trackedEntityInstance.setProgramInstances(Sets.newHashSet());
    trackedEntityInstance.setTrackedEntityType(trackedEntityType);

    return trackedEntityInstance;
  }

  private TrackedEntityInstance getTEIWithDeleteProgramInstances() {
    ProgramInstance programInstance = new ProgramInstance();
    programInstance.setDeleted(true);

    TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance(organisationUnit);
    trackedEntityInstance.setUid(TEI_ID);
    trackedEntityInstance.setProgramInstances(Sets.newHashSet(programInstance));
    trackedEntityInstance.setTrackedEntityType(trackedEntityType);

    return trackedEntityInstance;
  }

  private TrackedEntityInstance getTEIWithProgramInstances() {
    TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance(organisationUnit);
    trackedEntityInstance.setUid(TEI_ID);
    trackedEntityInstance.setProgramInstances(Sets.newHashSet(new ProgramInstance()));
    trackedEntityInstance.setTrackedEntityType(trackedEntityType);

    return trackedEntityInstance;
  }

  private ProgramInstance getEnrollment(String enrollmentUid) {
    if (StringUtils.isEmpty(enrollmentUid)) {
      enrollmentUid = CodeGenerator.generateUid();
    }
    ProgramInstance programInstance = new ProgramInstance();
    programInstance.setUid(enrollmentUid);
    programInstance.setOrganisationUnit(organisationUnit);
    programInstance.setEntityInstance(getTEIWithNoProgramInstances());
    programInstance.setProgram(program);
    programInstance.setStatus(ProgramStatus.ACTIVE);
    return programInstance;
  }

  private ProgramStageInstance getEvent() {
    ProgramStageInstance programStageInstance = new ProgramStageInstance();
    programStageInstance.setProgramStage(programStage);
    programStageInstance.setOrganisationUnit(organisationUnit);
    programStageInstance.setProgramInstance(new ProgramInstance());
    programStageInstance.setStatus(EventStatus.COMPLETED);
    return programStageInstance;
  }

  private User deleteTeiAuthorisedUser() {
    return makeUser("A", Lists.newArrayList(Authorities.F_TEI_CASCADE_DELETE.getAuthority()));
  }

  private User deleteEnrollmentAuthorisedUser() {
    return makeUser(
        "A", Lists.newArrayList(Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority()));
  }

  private User changeCompletedEventAuthorisedUser() {
    return makeUser("A", Lists.newArrayList("F_UNCOMPLETE_EVENT"));
  }
}
