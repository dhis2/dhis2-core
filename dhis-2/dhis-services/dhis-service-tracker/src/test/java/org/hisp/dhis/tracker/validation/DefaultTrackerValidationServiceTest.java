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

import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasError;
import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasWarning;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Builder;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultTrackerValidationServiceTest {

  private DefaultTrackerValidationService service;

  private TrackerPreheat preheat;

  private TrackerBundle.TrackerBundleBuilder bundleBuilder;

  private TrackerBundle bundle;

  private TrackerValidationHook hook1;

  private TrackerValidationHook hook2;

  @BeforeEach
  void setUp() {
    preheat = mock(TrackerPreheat.class);
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());

    hook1 = mock(TrackerValidationHook.class);
    hook2 = mock(TrackerValidationHook.class);

    bundleBuilder = newBundle();
  }

  @Test
  void shouldNotValidateWhenModeIsSkipAndUserIsNull() {
    bundle = bundleBuilder.validationMode(ValidationMode.SKIP).user(null).build();
    service = new DefaultTrackerValidationService(List.of(hook1), Collections.emptyList());

    service.validate(bundle);

    verifyNoInteractions(hook1);
  }

  @Test
  void shouldNotValidateWhenModeIsSkipAndUserIsASuperUser() {
    bundle = bundleBuilder.validationMode(ValidationMode.SKIP).user(superUser()).build();
    service = new DefaultTrackerValidationService(List.of(hook1), Collections.emptyList());

    service.validate(bundle);

    verifyNoInteractions(hook1);
  }

  @Test
  void shouldValidateWhenModeIsNotSkipAndUserIsASuperUser() {
    bundle = bundleBuilder.validationMode(ValidationMode.FULL).user(superUser()).build();
    service = new DefaultTrackerValidationService(List.of(hook1, hook2), Collections.emptyList());

    service.validate(bundle);

    verify(hook1, times(1)).validate(any(), any());
    verify(hook2, times(1)).validate(any(), any());
  }

  @Test
  void skipOnErrorHookPreventsFurtherValidationOfInvalidEntityEvenInFullValidationMode() {

    // Test shows
    // 1. Hooks with skipOnError==true will prevent subsequent hooks from
    // validating an invalid entity
    // 2. DefaultValidationService removes invalid entities from the
    // TrackerBundle

    Event validEvent = event();
    Event invalidEvent = event();

    bundle = bundleBuilder.events(events(invalidEvent, validEvent)).build();

    ValidationHook skipOnError =
        ValidationHook.builder()
            .skipOnError(true)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    // using default TrackerValidationHook.skipOnError==false
    ValidationHook doNotSkipOnError =
        ValidationHook.builder()
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E9999))
            .build();
    service =
        new DefaultTrackerValidationService(
            List.of(skipOnError, doNotSkipOnError), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertTrue(report.hasErrors());
    assertEquals(1, report.getErrors().size(), "only skip on error hook should add 1 error");
    assertHasError(report, TrackerErrorCode.E1032, invalidEvent);

    assertFalse(bundle.getEvents().contains(invalidEvent));
    assertTrue(bundle.getEvents().contains(validEvent));
  }

  @Test
  void fullValidationModeAddsAllErrorsToReport() {

    // Test shows
    // in ValidationMode==FULL all hooks are called even with entities that
    // are already invalid (i.e. have an error
    // in the validation report)

    Event validEvent = event();
    Event invalidEvent = event();

    bundle = bundleBuilder.events(events(invalidEvent, validEvent)).build();

    ValidationHook hook1 =
        ValidationHook.builder()
            .skipOnError(false)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    ValidationHook hook2 =
        ValidationHook.builder()
            .skipOnError(false)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E9999))
            .build();
    service = new DefaultTrackerValidationService(List.of(hook1, hook2), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertTrue(report.hasErrors());
    assertEquals(2, report.getErrors().size(), "both hooks should add 1 error each");
    assertHasError(report, TrackerErrorCode.E1032, invalidEvent);
    assertHasError(report, TrackerErrorCode.E9999, invalidEvent);

    assertFalse(bundle.getEvents().contains(invalidEvent));
    assertTrue(bundle.getEvents().contains(validEvent));
  }

  @Test
  void failFastModePreventsFurtherValidationAfterFirstErrorIsAdded() {

    Event validEvent = event();
    Event invalidEvent = event();

    bundle =
        bundleBuilder
            .validationMode(ValidationMode.FAIL_FAST)
            .events(events(invalidEvent, validEvent))
            .build();

    ValidationHook hook1 =
        ValidationHook.builder()
            .skipOnError(false)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    TrackerValidationHook hook2 = mock(TrackerValidationHook.class);
    service = new DefaultTrackerValidationService(List.of(hook1, hook2), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertTrue(report.hasErrors());
    assertHasError(report, TrackerErrorCode.E1032, invalidEvent);

    assertFalse(bundle.getEvents().contains(invalidEvent));
    assertTrue(bundle.getEvents().contains(validEvent));

    verifyNoInteractions(hook2);
  }

  @Test
  void needsToRunPreventsHookExecutionOnImportStrategyDeleteByDefault() {
    Event invalidEvent = event();

    bundle =
        bundleBuilder
            .importStrategy(TrackerImportStrategy.DELETE)
            .events(events(invalidEvent))
            .build();
    // StrategyPreProcessor sets the ImportStrategy in the bundle for every
    // dto
    bundle.setStrategy(invalidEvent, TrackerImportStrategy.DELETE);

    ValidationHook hook1 =
        ValidationHook.builder()
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    service = new DefaultTrackerValidationService(List.of(hook1), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertFalse(report.hasErrors());
  }

  @Test
  void needsToRunPreventsHookExecutionIfReturnsFalse() {
    Event invalidEvent = event();

    bundle = bundleBuilder.events(events(invalidEvent)).build();

    ValidationHook hook1 =
        ValidationHook.builder()
            .needsToRun(false)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    service = new DefaultTrackerValidationService(List.of(hook1), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertFalse(report.hasErrors());
  }

  @Test
  void needsToRunExecutesHookIfReturnsTrue() {
    Event invalidEvent = event();

    bundle = bundleBuilder.events(events(invalidEvent)).build();

    ValidationHook hook1 =
        ValidationHook.builder()
            .needsToRun(true)
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    service = new DefaultTrackerValidationService(List.of(hook1), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertTrue(report.hasErrors());
    assertHasError(report, TrackerErrorCode.E1032, invalidEvent);
  }

  @Test
  void warningsDoNotInvalidateAndRemoveEntities() {

    Event validEvent = event();

    bundle = bundleBuilder.events(events(validEvent)).build();

    ValidationHook hook =
        ValidationHook.builder()
            .validateEvent(
                (reporter, event) -> {
                  if (validEvent.equals(event)) {
                    reporter.addWarning(event, TrackerErrorCode.E1120);
                  }
                })
            .build();
    service = new DefaultTrackerValidationService(List.of(hook), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertFalse(report.hasErrors());
    assertTrue(report.hasWarnings());
    assertEquals(1, report.getWarnings().size());
    assertHasWarning(report, TrackerErrorCode.E1120, validEvent);

    assertTrue(bundle.getEvents().contains(validEvent));
  }

  @Test
  void childEntitiesOfInvalidParentsAreStillValidated() {

    // Test shows
    // the children of a tracked entity will still be validated even if it
    // as a parent is invalid

    TrackedEntity invalidTrackedEntity = trackedEntity();
    Enrollment invalidEnrollment = enrollment();
    invalidTrackedEntity.setEnrollments(enrollments(invalidEnrollment));
    Event invalidEvent = event();
    invalidEnrollment.setEvents(events(invalidEvent));

    bundle =
        bundleBuilder
            .validationMode(ValidationMode.FULL)
            .trackedEntities(trackedEntities(invalidTrackedEntity))
            .enrollments(invalidTrackedEntity.getEnrollments())
            .events(invalidEnrollment.getEvents())
            .build();

    ValidationHook hook =
        ValidationHook.builder()
            .validateTrackedEntity(addErrorIfMatches(invalidTrackedEntity, TrackerErrorCode.E1090))
            .validateEnrollment(addErrorIfMatches(invalidEnrollment, TrackerErrorCode.E1069))
            .validateEvent(addErrorIfMatches(invalidEvent, TrackerErrorCode.E1032))
            .build();
    service = new DefaultTrackerValidationService(List.of(hook), Collections.emptyList());

    TrackerValidationReport report = service.validate(bundle);

    assertTrue(report.hasErrors());
    assertEquals(3, report.getErrors().size());
    assertHasError(report, TrackerErrorCode.E1090, invalidTrackedEntity);
    assertHasError(report, TrackerErrorCode.E1069, invalidEnrollment);
    assertHasError(report, TrackerErrorCode.E1032, invalidEvent);

    assertTrue(bundle.getTrackedEntities().isEmpty());
    assertTrue(bundle.getEnrollments().isEmpty());
    assertTrue(bundle.getEvents().isEmpty());
  }

  private User superUser() {
    User user = mock(User.class);
    when(user.isSuper()).thenReturn(true);
    return user;
  }

  private static <T extends TrackerDto> BiConsumer<ValidationErrorReporter, T> addErrorIfMatches(
      TrackerDto expected, TrackerErrorCode code) {
    return (reporter, actual) -> reporter.addErrorIf(() -> actual.equals(expected), actual, code);
  }

  @Builder
  private static class ValidationHook implements TrackerValidationHook {
    private Boolean skipOnError;

    private Boolean needsToRun;

    private BiConsumer<ValidationErrorReporter, TrackedEntity> validateTrackedEntity;

    private BiConsumer<ValidationErrorReporter, Enrollment> validateEnrollment;

    private BiConsumer<ValidationErrorReporter, Event> validateEvent;

    @Override
    public void validateTrackedEntity(
        ValidationErrorReporter reporter, TrackerBundle bundle, TrackedEntity trackedEntity) {
      if (this.validateTrackedEntity != null) {
        this.validateTrackedEntity.accept(reporter, trackedEntity);
      }
    }

    @Override
    public void validateEnrollment(
        ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment) {
      if (this.validateEnrollment != null) {
        this.validateEnrollment.accept(reporter, enrollment);
      }
    }

    @Override
    public void validateEvent(ValidationErrorReporter reporter, TrackerBundle bundle, Event event) {
      if (this.validateEvent != null) {
        this.validateEvent.accept(reporter, event);
      }
    }

    @Override
    public boolean skipOnError() {
      // using boxed Boolean, so we can test the default skipOnError
      // behavior of the AbstractTrackerDtoValidationHook
      // by default we delegate to AbstractTrackerDtoValidationHook
      return Objects.requireNonNullElseGet(
          this.skipOnError, TrackerValidationHook.super::skipOnError);
    }

    @Override
    public boolean needsToRun(TrackerImportStrategy strategy) {
      // using boxed Boolean, so we can test the default needsToRun
      // behavior of the AbstractTrackerDtoValidationHook
      // by default we delegate to AbstractTrackerDtoValidationHook
      return Objects.requireNonNullElseGet(
          this.needsToRun, () -> TrackerValidationHook.super.needsToRun(strategy));
    }
  }

  private TrackedEntity trackedEntity() {
    return TrackedEntity.builder().trackedEntity(CodeGenerator.generateUid()).build();
  }

  private Enrollment enrollment() {
    return Enrollment.builder().enrollment(CodeGenerator.generateUid()).build();
  }

  private Event event() {
    String enrollment = CodeGenerator.generateUid();
    when(preheat.exists(
            argThat(
                t ->
                    t != null
                        && t.getTrackerType() == TrackerType.ENROLLMENT
                        && enrollment.equals(t.getUid()))))
        .thenReturn(true);
    return Event.builder().event(CodeGenerator.generateUid()).enrollment(enrollment).build();
  }

  private List<TrackedEntity> trackedEntities(TrackedEntity... trackedEntities) {
    return List.of(trackedEntities);
  }

  private List<Enrollment> enrollments(Enrollment... enrollments) {
    return List.of(enrollments);
  }

  private List<Event> events(Event... events) {
    return List.of(events);
  }

  private TrackerBundle.TrackerBundleBuilder newBundle() {
    return TrackerBundle.builder().preheat(preheat).skipRuleEngine(true);
  }
}
