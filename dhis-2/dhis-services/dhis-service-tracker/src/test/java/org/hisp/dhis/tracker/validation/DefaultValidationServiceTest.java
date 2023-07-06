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
package org.hisp.dhis.tracker.validation;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1120;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasWarning;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultValidationServiceTest {

  private DefaultValidationService service;

  private TrackerPreheat preheat;

  private TrackerBundle.TrackerBundleBuilder bundleBuilder;

  private TrackerBundle bundle;

  private Validator validator;

  private Validator ruleEngineValidator;

  @BeforeEach
  void setUp() {
    preheat = mock(TrackerPreheat.class);
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());

    validator = mock(Validator.class);
    when(validator.needsToRun(any())).thenReturn(true);
    ruleEngineValidator = mock(Validator.class);

    bundleBuilder = newBundle();
  }

  @Test
  void shouldNotValidateWhenModeIsSkipAndUserIsNull() {
    bundle =
        bundleBuilder
            .validationMode(ValidationMode.SKIP)
            .user(null)
            .trackedEntities(trackedEntities(trackedEntity()))
            .build();
    service = new DefaultValidationService(validator, ruleEngineValidator);

    service.validate(bundle);

    verifyNoInteractions(validator);
  }

  @Test
  void shouldNotValidateWhenModeIsSkipAndUserIsASuperUser() {
    bundle =
        bundleBuilder
            .validationMode(ValidationMode.SKIP)
            .user(superUser())
            .trackedEntities(trackedEntities(trackedEntity()))
            .build();
    service = new DefaultValidationService(validator, ruleEngineValidator);

    service.validate(bundle);

    verifyNoInteractions(validator);
  }

  @Test
  void shouldValidateWhenModeIsNotSkipAndUserIsASuperUser() {
    TrackedEntity trackedEntity = trackedEntity();
    bundle =
        bundleBuilder
            .validationMode(ValidationMode.FULL)
            .user(superUser())
            .trackedEntities(trackedEntities(trackedEntity))
            .build();
    service = new DefaultValidationService(validator, ruleEngineValidator);

    service.validate(bundle);

    verify(validator, times(1)).validate(any(), any(), eq(bundle));
  }

  @Test
  void failFastModePreventsFurtherValidationAfterFirstErrorIsAdded() {
    bundle =
        bundleBuilder
            .validationMode(ValidationMode.FAIL_FAST)
            .trackedEntities(trackedEntities(trackedEntity()))
            .build();

    doThrow(new FailFastException(emptyList())).when(validator).validate(any(), any(), any());
    service = new DefaultValidationService(validator, ruleEngineValidator);

    service.validate(bundle);

    verify(validator, times(1)).validate(any(), any(), any());
    verifyNoInteractions(ruleEngineValidator);
  }

  @Test
  void warningsDoNotInvalidateAndRemoveEntities() {
    TrackedEntity validTrackedEntity = trackedEntity();
    bundle = bundleBuilder.trackedEntities(trackedEntities(validTrackedEntity)).build();

    Validator<TrackerBundle> v1 =
        (r, b, e) -> r.addWarning(validTrackedEntity, ValidationCode.E1120);
    service = new DefaultValidationService(v1, ruleEngineValidator);

    ValidationResult result = service.validate(bundle);

    assertAll(
        "errors and warnings",
        () -> assertFalse(result.hasErrors()),
        () -> assertHasWarning(result, validTrackedEntity, E1120));

    assertTrue(bundle.getTrackedEntities().contains(validTrackedEntity));
  }

  private User superUser() {
    User user = mock(User.class);
    when(user.isSuper()).thenReturn(true);
    return user;
  }

  private TrackedEntity trackedEntity() {
    return TrackedEntity.builder().trackedEntity(CodeGenerator.generateUid()).build();
  }

  private List<TrackedEntity> trackedEntities(TrackedEntity... trackedEntities) {
    return List.of(trackedEntities);
  }

  private TrackerBundle.TrackerBundleBuilder newBundle() {
    return TrackerBundle.builder().preheat(preheat).skipRuleEngine(true);
  }
}
