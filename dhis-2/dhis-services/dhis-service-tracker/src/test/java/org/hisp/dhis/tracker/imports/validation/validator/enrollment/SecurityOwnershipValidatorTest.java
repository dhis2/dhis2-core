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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1040;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1103;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
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

  private static final String ENROLLMENT_ID = "ENROLLMENT_ID";

  private SecurityOwnershipValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackerAccessManager trackerAccessManager;

  private User user;

  private User cascadeUser;

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private Map<String, Map<String, TrackedEntityProgramOwnerOrgUnit>> ownerOrgUnit;

  @BeforeEach
  public void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TE_TYPE_ID);

    program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    user = new User();
    UserRole cascadeRole = new UserRole();
    cascadeRole.setAuthorities(Set.of(Authorities.F_ENROLLMENT_CASCADE_DELETE.name()));
    cascadeUser = new User();
    cascadeUser.setUserRoles(Set.of(cascadeRole));

    TrackedEntityProgramOwnerOrgUnit owner =
        new TrackedEntityProgramOwnerOrgUnit(TE_ID, PROGRAM_ID, organisationUnit);
    ownerOrgUnit = Map.of(TE_ID, Map.of(PROGRAM_ID, owner));

    validator = new SecurityOwnershipValidator(trackerAccessManager);
  }

  @Test
  void shouldSuccessWhenCreateEnrollmentAndUserHasAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenCreateEnrollmentAndUserHasNoAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    when(bundle.getUser()).thenReturn(user);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.CREATE);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_ID))).thenReturn(program);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1040);
  }

  @Test
  void shouldSuccessWhenUpdateEnrollmentAndUserHasAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();
    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.UPDATE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);

    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenUpdateEnrollmentAndUserHasNoAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(user);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.UPDATE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1040);
  }

  @Test
  void shouldSuccessWhenDeleteEnrollmentWithOnlyDeletedEventsAndUserHasAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();
    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of());
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenDeleteEnrollmentWithOnlyDeletedAndUserHasNoAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(user);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of());
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1040);
  }

  @Test
  void shouldSuccessWhenDeleteEnrollmentWithNonDeletedEventsAndCascadeAuthorityAndUserHasAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();
    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(cascadeUser);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of(ENROLLMENT_ID));
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenDeleteEnrollmentWithNonDeletedEventsAndCascadeAuthorityAndUserHasNoAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(cascadeUser);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of(ENROLLMENT_ID));
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1040);
  }

  @Test
  void shouldFailWhenDeleteEnrollmentWithNonDeletedEventsAndNoCascadeAuthorityAndUserHasAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();
    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(user);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of(ENROLLMENT_ID));
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of());

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1103);
  }

  @Test
  void shouldFailWhenDeleteEnrollmentWithNonDeletedEventsAndNoCascadeAuthorityAndUserHasNoAccess() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .enrollment(ENROLLMENT_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntity(TE_ID)
            .program(MetadataIdentifier.ofUid(PROGRAM_ID))
            .build();

    Enrollment dBEnrollment = getEnrollment();

    when(bundle.getUser()).thenReturn(user);
    when(bundle.getStrategy(enrollment)).thenReturn(TrackerImportStrategy.DELETE);
    when(preheat.getEnrollment(ENROLLMENT_ID)).thenReturn(dBEnrollment);
    when(preheat.getProgramOwner()).thenReturn(ownerOrgUnit);
    when(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent()).thenReturn(List.of(ENROLLMENT_ID));
    when(trackerAccessManager.canWrite(any(), eq(program), eq(organisationUnit), eq(TE_ID)))
        .thenReturn(List.of("error"));

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1040);
    assertHasError(reporter, enrollment, E1103);
  }

  private TrackedEntity teWithNoEnrollments() {
    TrackedEntity trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setUid(TE_ID);
    trackedEntity.setEnrollments(Sets.newHashSet());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  private Enrollment getEnrollment() {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_ID);
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.setTrackedEntity(teWithNoEnrollments());
    enrollment.setProgram(program);
    enrollment.setStatus(ProgramStatus.ACTIVE);
    return enrollment;
  }
}
