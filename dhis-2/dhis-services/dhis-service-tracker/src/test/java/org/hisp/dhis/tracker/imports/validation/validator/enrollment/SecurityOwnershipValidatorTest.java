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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1091;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1103;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1104;
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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
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

  private static final String TEI_ID = "TEI_ID";

  private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

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

  @BeforeEach
  public void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

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

    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator =
        new SecurityOwnershipValidator(aclService, ownershipAccessManager, organisationUnitService);
  }

  @Test
  void verifyValidationSuccessForEnrollmentWhenEnrollmentHasNoOrgUnitAssigned() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.UPDATE);

    Enrollment preheatEnrollment = getEnrollment(enrollment.getEnrollment());
    preheatEnrollment.setOrganisationUnit(null);

    when(preheat.getEnrollment(enrollment.getEnrollment())).thenReturn(preheatEnrollment);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
    verify(organisationUnitService, times(0)).isInUserHierarchyCached(user, organisationUnit);

    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
    verify(organisationUnitService, times(0)).isInUserHierarchyCached(user, organisationUnit);
  }

  @Test
  void verifyValidationSuccessForEnrollment() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentCreation() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentDeletion() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
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
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyCaptureScopeIsCheckedForEnrollmentProgramWithoutRegistration() {
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(enrollmentUid)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentWithoutEventsUsingDeleteStrategy() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyValidationSuccessForEnrollmentUsingDeleteStrategyAndUserWithCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(bundle.getUser()).thenReturn(deleteEnrollmentAuthorisedUser());
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void
      verifyValidationFailsForEnrollmentWithoutEventsUsingDeleteStrategyAndUserNotInOrgUnitHierarchy() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(Collections.emptyList());
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(false);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1000);
  }

  @Test
  void verifyValidationFailsForEnrollmentUsingDeleteStrategyAndUserWithoutCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TEI_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent())
        .thenReturn(Collections.singletonList(enrollment.getEnrollment()));
    when(preheat.getEnrollment(enrollment.getEnrollment()))
        .thenReturn(getEnrollment(enrollment.getEnrollment()));
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);
    when(aclService.canDataWrite(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1103);
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutProgramWriteAccess() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
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

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1091);
  }

  @Test
  void verifyValidationFailsForEnrollmentDeletionAndUserWithoutTrackedEntityTypeReadAccess() {
    String enrollmentUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
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

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1104);
  }

  private TrackedEntity getTEIWithNoEnrollments() {
    TrackedEntity trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setUid(TEI_ID);
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
    enrollment.setTrackedEntity(getTEIWithNoEnrollments());
    enrollment.setProgram(program);
    enrollment.setStatus(ProgramStatus.ACTIVE);
    return enrollment;
  }

  private User deleteEnrollmentAuthorisedUser() {
    return makeUser(
        "A", Lists.newArrayList(Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority()));
  }
}
