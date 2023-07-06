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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1002;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1063;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1114;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class ExistenceValidatorTest {
  private static final String SOFT_DELETED_TEI_UID = "SoftDeletedTEIId";

  private static final String TEI_UID = "TEIId";

  private static final String NOT_PRESENT_TEI_UID = "NotPresentTEIId";

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private ExistenceValidator validator;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);

    validator = new ExistenceValidator();
  }

  @Test
  void verifyTrackedEntityValidationSuccessWhenIsCreateAndTeiIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(NOT_PRESENT_TEI_UID)
            .build();
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyTrackedEntityValidationSuccessWhenTeiIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(NOT_PRESENT_TEI_UID)
            .build();
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.TrackedEntity.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyTrackedEntityValidationSuccessWhenIsUpdate() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity(TEI_UID).build();
    when(preheat.getTrackedEntity(TEI_UID)).thenReturn(getTei());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.TrackedEntity.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyTrackedEntityValidationFailsWhenIsSoftDeleted() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(SOFT_DELETED_TEI_UID)
            .build();
    when(preheat.getTrackedEntity(SOFT_DELETED_TEI_UID)).thenReturn(getSoftDeletedTei());
    when(bundle.getStrategy(any(org.hisp.dhis.tracker.imports.domain.TrackedEntity.class)))
        .thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1114);
  }

  @Test
  void verifyTrackedEntityValidationFailsWhenIsCreateAndTEIIsAlreadyPresent() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity(TEI_UID).build();
    when(preheat.getTrackedEntity(TEI_UID)).thenReturn(getTei());
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1002);
  }

  @Test
  void verifyTrackedEntityValidationFailsWhenIsUpdateAndTEIIsNotPresent() {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(NOT_PRESENT_TEI_UID)
            .build();
    when(bundle.getStrategy(trackedEntity)).thenReturn(TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, trackedEntity);

    assertHasError(reporter, trackedEntity, E1063);
  }

  private TrackedEntity getSoftDeletedTei() {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(SOFT_DELETED_TEI_UID);
    trackedEntity.setDeleted(true);
    return trackedEntity;
  }

  private TrackedEntity getTei() {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TEI_UID);
    trackedEntity.setDeleted(false);
    return trackedEntity;
  }
}
