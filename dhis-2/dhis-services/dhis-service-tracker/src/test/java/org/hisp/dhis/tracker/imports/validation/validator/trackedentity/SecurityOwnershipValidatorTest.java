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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1001;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1003;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1100;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
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

  @Mock private TrackerAccessManager trackerAccessManager;

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private Program program;

  private UserDetails user;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  void setUp() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);
    organisationUnit.updatePath();

    User userA = makeUser("A");
    userA.addOrganisationUnit(organisationUnit);
    user = UserDetails.fromUser(userA);

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

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new SecurityOwnershipValidator(trackerAccessManager);
  }

  @Test
  void shouldSuccessWhenUpdateTEAndUserHasWriteAccess() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    TrackedEntity te = teWithEnrollments();
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.UPDATE);
    when(trackerAccessManager.canUpdate(any(), eq(te))).thenReturn(List.of());
    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenDeleteTEWithNoEnrollmentsAndUserHasWriteAccessAndOUInCaptureScope() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntity te = teWithNoEnrollments();
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);
    when(trackerAccessManager.canDelete(any(), eq(te))).thenReturn(List.of());

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenCreateTEAndUserCorrectCaptureScope() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSuccessWhenDeleteTEWithDeletedEnrollmentsAndUserHasWriteAccessAndOUInCaptureScope() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntity te = teWithDeleteEnrollments();
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);
    when(trackerAccessManager.canDelete(any(), eq(te))).thenReturn(List.of());

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void
      shouldSuccessWhenDeleteTEWithEnrollmentsAndUserHasWriteAccessAndOUInCaptureScopeAndDeleteCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntity te = teWithEnrollments();
    UserDetails userDetails = deleteTeiAuthorisedUser();

    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);

    when(trackerAccessManager.canDelete(userDetails, te)).thenReturn(List.of());

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenDeleteTEWithEnrollmentsAndUserHasWriteAccessAndNoDeleteCascadeAuthority() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntity te = teWithEnrollments();
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);
    when(trackerAccessManager.canDelete(any(), eq(te))).thenReturn(List.of());

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1100);
  }

  @Test
  void shouldFailWhenCreateTEAndUserHasNoCaptureScopeAccess() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);
    User authorizedUser = makeUser("B");
    UserDetails userDetails = UserDetails.fromUser(authorizedUser);
    when(bundle.getUser()).thenReturn(userDetails);

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1000);
  }

  @Test
  void shouldFailWhenCreateTEAndUserHasNoWriteAccessToTET() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(preheat.getTrackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID)))
        .thenReturn(trackedEntityType);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);
    TrackedEntity te = TrackerObjectsMapper.map(preheat, trackedEntity, user);
    when(trackerAccessManager.canCreate(any(), eq(te))).thenReturn(List.of("error"));

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1001);
  }

  @Test
  void shouldFailWhenUpdateTEAndUserHasNoWriteAccess() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    when(bundle.getPreheat()).thenReturn(preheat);
    TrackedEntity te = teWithNoEnrollments();
    when(preheat.getTrackedEntity(TE_ID)).thenReturn(te);
    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID)))
        .thenReturn(organisationUnit);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(trackerAccessManager.canUpdate(any(), eq(te))).thenReturn(List.of("error"));

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1003);
  }

  @Test
  void shouldFailWhenDeleteTEAndUserHasNoCaptureScopeAccess() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(TE_ID)
            .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
            .trackedEntityType(MetadataIdentifier.ofUid(TE_TYPE_ID))
            .build();

    TrackedEntity te = TrackerObjectsMapper.map(preheat, trackedEntity, user);
    te.setOrganisationUnit(organisationUnit);

    when(bundle.getPreheat()).thenReturn(preheat);
    when(bundle.getPreheat().getTrackedEntity(trackedEntity.getTrackedEntity())).thenReturn(te);
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    User authorizedUser = makeUser("B");
    UserDetails userDetails = UserDetails.fromUser(authorizedUser);
    when(bundle.getUser()).thenReturn(userDetails);

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1000);
  }

  private TrackedEntity teWithNoEnrollments() {
    TrackedEntity trackedEntity =
        createTrackedEntity(organisationUnit, createTrackedEntityType('Z'));
    trackedEntity.setUid(TE_ID.getValue());
    trackedEntity.setEnrollments(Sets.newHashSet());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  private TrackedEntity teWithDeleteEnrollments() {
    Enrollment enrollment = new Enrollment();
    enrollment.setDeleted(true);

    TrackedEntity trackedEntity =
        createTrackedEntity(organisationUnit, createTrackedEntityType('B'));
    trackedEntity.setUid(TE_ID.getValue());
    trackedEntity.setEnrollments(Sets.newHashSet(enrollment));
    return trackedEntity;
  }

  private TrackedEntity teWithEnrollments() {
    TrackedEntity trackedEntity =
        createTrackedEntity(organisationUnit, createTrackedEntityType('R'));
    trackedEntity.setUid(TE_ID.getValue());
    trackedEntity.setEnrollments(Sets.newHashSet(new Enrollment()));
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  private UserDetails deleteTeiAuthorisedUser() {
    User authorizedUser =
        makeUser("A", Lists.newArrayList(Authorities.F_TEI_CASCADE_DELETE.name()));
    authorizedUser.setOrganisationUnits(Set.of(organisationUnit));
    UserDetails userDetails = UserDetails.fromUser(authorizedUser);
    when(bundle.getUser()).thenReturn(userDetails);
    return userDetails;
  }
}
