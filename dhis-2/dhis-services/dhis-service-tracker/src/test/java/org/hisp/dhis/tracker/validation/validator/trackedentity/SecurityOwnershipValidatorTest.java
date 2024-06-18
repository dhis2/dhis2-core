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
package org.hisp.dhis.tracker.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1003;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1100;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
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

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private TrackerAccessManager trackerAccessManager;

  private User user;

  private Reporter reporter;

  private OrganisationUnit organisationUnit;

  private Program program;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  public void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    user = makeUser("A");
    when(bundle.getUser()).thenReturn(user);

    organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(ORG_UNIT_ID);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setUid(TEI_TYPE_ID);
    Program program = createProgram('A');
    program.setUid(PROGRAM_ID);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid(PS_ID);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator =
        new SecurityOwnershipValidator(aclService, organisationUnitService, trackerAccessManager);
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
    validator.validate(reporter, bundle, trackedEntity);

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

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEINoProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validator.validate(reporter, bundle, trackedEntity);

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

    validator.validate(reporter, bundle, trackedEntity);

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

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithDeleteProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validator.validate(reporter, bundle, trackedEntity);

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

    when(bundle.getUser()).thenReturn(deleteTeiAuthorisedUser());
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());
    when(organisationUnitService.isInUserHierarchyCached(user, organisationUnit)).thenReturn(true);

    validator.validate(reporter, bundle, trackedEntity);

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

    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.DELETE);
    TrackedEntityInstance te = getTEIWithProgramInstances();
    when(preheat.getTrackedEntity(TEI_ID)).thenReturn(te);
    when(trackerAccessManager.canWrite(any(), eq(te))).thenReturn(List.of());

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1100);
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

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1000);
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

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1003);
  }

  private TrackedEntityInstance getTEINoProgramInstances() {
    TrackedEntityInstance trackedEntity = createTrackedEntityInstance(organisationUnit);
    trackedEntity.setUid(TEI_ID);
    trackedEntity.setProgramInstances(Sets.newHashSet());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
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

  private User deleteTeiAuthorisedUser() {
    return makeUser("A", Lists.newArrayList(Authorities.F_TEI_CASCADE_DELETE.getAuthority()));
  }
}