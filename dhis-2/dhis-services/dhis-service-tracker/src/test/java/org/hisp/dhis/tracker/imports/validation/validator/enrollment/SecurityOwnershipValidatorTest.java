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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1091;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1103;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1104;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class SecurityOwnershipValidatorTest extends TestBase {
  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final UID TE_ID = UID.generate();

  private static final String TE_TYPE_ID = "TE_TYPE_ID";

  private static final String PROGRAM_ID = "PROGRAM_ID";

  private static final String PS_ID = "PS_ID";

  private SecurityOwnershipValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private AclService aclService;

  @Mock private TrackerOwnershipManager ownershipAccessManager;

  private UserDetails user;

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private TrackedEntity trackedEntity;

  private Map<UID, Map<String, TrackedEntityProgramOwnerOrgUnit>> ownerOrgUnit;

  @BeforeEach
  public void setUp() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);
    organisationUnit.updatePath();

    User u = makeUser("A");
    user = UserDetails.fromUser(u);

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getUser()).thenReturn(user);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TE_TYPE_ID);
    program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TE_ID.getValue());

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    TrackedEntityProgramOwnerOrgUnit owner =
        new TrackedEntityProgramOwnerOrgUnit(TE_ID.getValue(), PROGRAM_ID, organisationUnit);
    ownerOrgUnit = Map.of(TE_ID, Map.of(PROGRAM_ID, owner));

    validator = new SecurityOwnershipValidator(aclService, ownershipAccessManager);
  }

  private UserDetails setUpUserWithOrgUnit(List<String> auths) {
    User userB = makeUser("B", auths);
    userB.setOrganisationUnits(Set.of(organisationUnit));
    UserDetails userDetails = UserDetails.fromUser(userB);
    when(bundle.getUser()).thenReturn(userDetails);
    return userDetails;
  }

  private UserDetails setUpUserWithOrgUnit() {
    return setUpUserWithOrgUnit(List.of());
  }

  @Test
  void verifyValidationSuccessForEnrollment() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(trackedEntity);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentCreation() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(trackedEntity);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentDeletion() {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            userDetails, enrollment.getTrackedEntity().getValue(), organisationUnit, program))
        .thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentWithoutEventsUsingDeleteStrategy() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            userDetails, enrollment.getTrackedEntity().getValue(), organisationUnit, program))
        .thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentUsingDeleteStrategyAndUserWithCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);

    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails =
        setUpUserWithOrgUnit(List.of(Authorities.F_ENROLLMENT_CASCADE_DELETE.name()));
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(ownershipAccessManager.hasAccess(
            userDetails, enrollment.getTrackedEntity().getValue(), organisationUnit, program))
        .thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void
      verifyValidationFailsForEnrollmentWithoutEventsUsingDeleteStrategyAndUserNotInOrgUnitHierarchy() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1000);
  }

  @Test
  void verifyValidationFailsForEnrollmentUsingDeleteStrategyAndUserWithoutCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(UID.generate())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1103);
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutProgramWriteAccess() {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(false);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(true);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1091);
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutTrackedEntityTypeReadAccess() {
    UID enrollmentUid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    UserDetails userDetails = setUpUserWithOrgUnit();
    when(aclService.canDataWrite(userDetails, program)).thenReturn(true);
    when(aclService.canDataRead(userDetails, program.getTrackedEntityType())).thenReturn(false);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1104);
  }

  private TrackedEntity teWithNoEnrollments() {
    TrackedEntity trackedEntity =
        createTrackedEntity(organisationUnit, createTrackedEntityType('C'));
    trackedEntity.setUid(TE_ID.getValue());
    trackedEntity.setEnrollments(Sets.newHashSet());
    return trackedEntity;
  }

  private Enrollment getEnrollment(UID enrollmentUid) {
    if (enrollmentUid == null) {
      enrollmentUid = UID.generate();
    }
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(enrollmentUid.getValue());
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setTrackedEntity(teWithNoEnrollments());
    enrollment.setProgram(program);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    return enrollment;
  }
}
