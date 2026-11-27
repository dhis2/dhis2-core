/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.scheduling;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.setting.SessionUserSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests that {@link JobScheduler#runDueJob(JobEntry)} does not carry the user or the user settings
 * of one job over to the next job that runs on the same, reused worker thread.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class JobSchedulerThreadContextTest {

  private static final String NO_USER = "<none>";

  private final UID userX = UID.generate();
  private final UID userY = UID.generate();
  private final UID deletedUser = UID.generate();

  private final JobSchedulerLoopService service = mock(JobSchedulerLoopService.class);
  private final JobService jobService = mock(JobService.class);
  private final JobScheduler scheduler =
      new JobScheduler(jobService, service, mock(SystemSettingsService.class));

  /** A single worker thread, so that every job runs on the same thread like a reused pool thread */
  private final ExecutorService worker = Executors.newSingleThreadExecutor();

  /** What the scheduler steps and the job saw, by step and job ID */
  private final Map<String, String> observed = new ConcurrentHashMap<>();

  @BeforeEach
  void setUp() throws Exception {
    SessionUserSettings.put(userX.getValue(), settings("fr"));
    SessionUserSettings.put(userY.getValue(), settings("es"));

    when(service.tryRun(any()))
        .thenAnswer(
            call -> {
              observed.put("tryRun " + call.getArgument(0), currentUser());
              return true;
            });
    when(service.startRun(any(), any(), any()))
        .thenAnswer(
            call -> {
              UID user = call.getArgument(1);
              if (deletedUser.equals(user)) throw new NotFoundException(User.class, user);
              loginAs(user.getValue());
              return JobProgress.noop();
            });
    doAnswer(
            call -> {
              observed.put("finishRunFail " + call.getArgument(0), currentUser());
              return null;
            })
        .when(service)
        .finishRunFail(any(), any());
    when(service.finishRunSuccess(any())).thenReturn(true);

    Job job =
        new Job() {
          @Override
          public JobType getJobType() {
            return JobType.TEST;
          }

          @Override
          public void execute(JobEntry config, JobProgress progress) {
            String locale = UserSettings.getCurrentSettings().getUserDbLocale().language();
            observed.put("execute " + config.id(), currentUser() + " " + locale);
          }
        };
    when(jobService.getJob(JobType.TEST)).thenReturn(job);
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    SessionUserSettings.clear(userX.getValue());
    SessionUserSettings.clear(userY.getValue());
    SecurityContextHolder.clearContext();
    ThreadUserSettings.clear();
    worker.shutdownNow();
    worker.awaitTermination(10, TimeUnit.SECONDS);
  }

  @Test
  void testNextJobOnSameThreadDoesNotSeePreviousUserOrSettings() throws Exception {
    JobEntry jobA = createJob(userX);
    JobEntry jobB = createJob(userY);

    runOnWorker(jobA);
    runOnWorker(jobB);

    assertAll(
        () -> assertEquals(userX.getValue() + " fr", observed.get("execute " + jobA.id())),
        () -> assertEquals(NO_USER, observed.get("tryRun " + jobB.id()), "user at tryRun of B"),
        () -> assertEquals(userY.getValue() + " es", observed.get("execute " + jobB.id())));
  }

  @Test
  void testJobLeavesNoUserOrSettingsOnThread() throws Exception {
    runOnWorker(createJob(userX));

    String userAfterJob = onWorker(JobSchedulerThreadContextTest::currentUser);
    String localeReadByY = onWorker(() -> readSettingsLocaleAs(userY.getValue()));

    assertAll(
        () -> assertEquals(NO_USER, userAfterJob, "user left on the thread"),
        () -> assertEquals("es", localeReadByY, "settings left on the thread"));
  }

  @Test
  void testFailedLoginOfNextJobIsNotHandledAsPreviousUser() throws Exception {
    JobEntry jobA = createJob(userX);
    JobEntry jobB = createJob(deletedUser);

    runOnWorker(jobA);
    runOnWorker(jobB);

    assertAll(
        () -> assertEquals(NO_USER, observed.get("tryRun " + jobB.id()), "user at tryRun"),
        () -> assertEquals(NO_USER, observed.get("finishRunFail " + jobB.id()), "user at fail"),
        () -> assertEquals(NO_USER, onWorker(JobSchedulerThreadContextTest::currentUser)));
  }

  @Test
  void testCallerUserIsRestoredWhenJobRunsOnCallerThread() {
    loginAs("caller");
    SecurityContext callerContext = SecurityContextHolder.getContext();
    JobEntry jobA = createJob(userX);

    scheduler.runDueJob(jobA);

    assertAll(
        () -> assertEquals(NO_USER, observed.get("tryRun " + jobA.id()), "user at tryRun"),
        () -> assertEquals(userX.getValue() + " fr", observed.get("execute " + jobA.id())),
        () -> assertSame(callerContext, SecurityContextHolder.getContext()));
  }

  @Test
  void testCallerLocaleOverrideIsRestoredWhenJobRunsOnCallerThread() {
    loginAs("caller");
    // like a request with ?locale=de
    ThreadUserSettings.put(Map.of("keyDbLocale", "de"));
    JobEntry jobA = createJob(userX);

    scheduler.runDueJob(jobA);

    assertAll(
        () -> assertEquals(userX.getValue() + " fr", observed.get("execute " + jobA.id())),
        () -> assertEquals("caller", currentUser()),
        () -> assertEquals("de", UserSettings.getCurrentSettings().getUserDbLocale().language()));
  }

  private void runOnWorker(JobEntry job) throws Exception {
    worker.submit(() -> scheduler.runDueJob(job)).get(10, TimeUnit.SECONDS);
  }

  private <T> T onWorker(Callable<T> task) throws Exception {
    return worker.submit(task).get(10, TimeUnit.SECONDS);
  }

  private static String readSettingsLocaleAs(String username) {
    loginAs(username);
    try {
      return UserSettings.getCurrentSettings().getUserDbLocale().language();
    } finally {
      ThreadUserSettings.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private static JobEntry createJob(UID executedBy) {
    UID id = UID.generate();
    return new JobEntry(
        id,
        JobType.TEST,
        SchedulingType.ONCE_ASAP,
        id.getValue(),
        JobStatus.SCHEDULED,
        executedBy,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static UserSettings settings(String locale) {
    return UserSettings.of(Map.of("keyDbLocale", locale, "keyUiLocale", locale));
  }

  /** Same as the login that {@code startRun} does, with a plain user as principal */
  private static void loginAs(String username) {
    org.springframework.security.core.userdetails.User principal =
        new org.springframework.security.core.userdetails.User(username, "", List.of());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    SecurityContextHolder.setContext(context);
  }

  private static String currentUser() {
    return CurrentUserUtil.hasCurrentUser() ? CurrentUserUtil.getCurrentUsername() : NO_USER;
  }
}
