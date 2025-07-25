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
package org.hisp.dhis.tracker.imports.validation;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1120;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasWarning;
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
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultValidationServiceTest extends TestBase {

  private DefaultValidationService service;

  private TrackerPreheat preheat;

  private TrackerBundle.TrackerBundleBuilder bundleBuilder;

  private TrackerBundle bundle;

  private Validator<TrackerBundle> validator;

  private Validator<TrackerBundle> ruleEngineValidator;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    preheat = mock(TrackerPreheat.class);
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());

    validator = mock(Validator.class);
    when(validator.needsToRun(any())).thenReturn(true);
    ruleEngineValidator = mock(Validator.class);

    User userA = makeUser("A");
    UserDetails user = UserDetails.fromUser(userA);

    bundleBuilder = TrackerBundle.builder().user(user).preheat(preheat).skipRuleEngine(true);
  }

  @Test
  void shouldNotValidateWhenModeIsSkipAndUserIsASuperUser() {
    bundle =
        bundleBuilder
            .validationMode(ValidationMode.SKIP)
            .trackedEntities(trackedEntities(trackedEntity()))
            .user(new SystemUser())
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

  private TrackedEntity trackedEntity() {
    return TrackedEntity.builder().trackedEntity(UID.generate()).build();
  }

  private List<TrackedEntity> trackedEntities(TrackedEntity... trackedEntities) {
    return List.of(trackedEntities);
  }
}
