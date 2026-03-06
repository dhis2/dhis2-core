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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.hisp.dhis.tracker.imports.DefaultTrackerImportService;
import org.hisp.dhis.tracker.imports.ParamsConverter;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar
 */
@ExtendWith(MockitoExtension.class)
class TrackerImporterServiceTest {

  @Mock private TrackerBundleService trackerBundleService;

  @Mock private ValidationService validationService;

  @Mock private TrackerPreprocessService trackerPreprocessService;

  @Mock private ValidationResult validationResult;

  private DefaultTrackerImportService subject;

  private TrackerImportParams params = null;

  private TrackerObjects trackerObjects;

  private final UserDetails user = new SystemUser();

  @BeforeEach
  public void setUp() {
    subject =
        new DefaultTrackerImportService(
            trackerBundleService, validationService, trackerPreprocessService);

    injectSecurityContextNoSettings(user);

    Enrollment enrollment = Enrollment.builder().enrollment(UID.generate()).build();
    final List<Enrollment> enrollments = List.of(enrollment);

    params = TrackerImportParams.builder().build();

    trackerObjects = TrackerObjects.builder().enrollments(enrollments).build();

    when(validationService.validate(any(TrackerBundle.class))).thenReturn(validationResult);
    when(validationService.validateRuleEngine(any(TrackerBundle.class)))
        .thenReturn(validationResult);
    when(trackerPreprocessService.preprocess(any(TrackerBundle.class)))
        .thenReturn(
            ParamsConverter.convert(
                params,
                TrackerObjects.builder().enrollments(enrollments).build(),
                user,
                new TrackerPreheat()));
  }

  @Test
  void testSkipSideEffect() {
    TrackerImportParams parameters = TrackerImportParams.builder().skipSideEffects(true).build();

    TrackerObjects objects =
        TrackerObjects.builder().enrollments(trackerObjects.getEnrollments()).build();

    when(trackerBundleService.create(any(TrackerImportParams.class), any(), any()))
        .thenReturn(ParamsConverter.convert(parameters, objects, user, new TrackerPreheat()));
    when(trackerBundleService.commit(any(TrackerBundle.class)))
        .thenReturn(PersistenceReport.emptyReport());

    subject.importTracker(parameters, trackerObjects, JobProgress.noop());

    verify(trackerBundleService, times(0)).sendNotifications(anyList());
  }

  @Test
  void testWithSideEffects() {
    TrackerObjects objects =
        TrackerObjects.builder().enrollments(trackerObjects.getEnrollments()).build();
    doAnswer(invocationOnMock -> null).when(trackerBundleService).sendNotifications(anyList());
    when(trackerBundleService.create(any(TrackerImportParams.class), any(), any()))
        .thenReturn(ParamsConverter.convert(params, objects, user, new TrackerPreheat()));
    when(trackerBundleService.commit(any(TrackerBundle.class)))
        .thenReturn(PersistenceReport.emptyReport());

    subject.importTracker(params, trackerObjects, JobProgress.noop());

    verify(trackerBundleService, times(1)).sendNotifications(anyList());
  }

  @Test
  void shouldRaiseExceptionWhenExceptionWasThrownInsideAStage() {
    TrackerObjects objects =
        TrackerObjects.builder().enrollments(trackerObjects.getEnrollments()).build();
    when(trackerBundleService.create(any(TrackerImportParams.class), any(), any()))
        .thenReturn(ParamsConverter.convert(params, objects, user, new TrackerPreheat()));
    when(trackerBundleService.commit(any(TrackerBundle.class)))
        .thenThrow(new IllegalArgumentException("ERROR"));

    JobProgress transitory = RecordingJobProgress.transitory();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> subject.importTracker(params, trackerObjects, transitory));
    assertEquals("ERROR", ex.getMessage());
  }
}
