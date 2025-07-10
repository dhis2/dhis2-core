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
package org.hisp.dhis.scheduling;

import static org.hisp.dhis.scheduling.JobProgress.getMessage;

import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.CurrentUserUtil;

/**
 * The {@link RecordingJobProgress} take care of the flow control aspect of {@link JobProgress} API.
 * Additional tracking can be done by wrapping another {@link JobProgress} as {@link #tracker}.
 *
 * <p>The implementation does allow for parallel items but would merge parallel stages or processes.
 * Stages and processes should always be sequential in a main thread.
 *
 * @author Jan Bernitt
 */
@Slf4j
public class RecordingJobProgress implements JobProgress {

  /**
   * To get the same behaviour the state still needs to be tracked, but it does not need to be
   * accumulated. Thus, when recoding completed objects can be discarded. This means as soon as a
   * new object on the same level is started the previous one is discarded.
   *
   * @return progress instance that behaves like the recording one except that it discards completed
   *     recoding objects
   */
  public static JobProgress transitory() {
    return transitory(null, null);
  }

  public static JobProgress transitory(JobConfiguration job, Notifier notifier) {
    JobProgress track =
        notifier == null
            ? JobProgress.noop()
            : new NotifierJobProgress(notifier, job, NotificationLevel.INFO);
    return new RecordingJobProgress(null, null, track, true, () -> {}, false, true);
  }

  @CheckForNull private final MessageService messageService;
  @CheckForNull private final JobConfiguration configuration;
  private final JobProgress tracker;
  private final boolean abortOnFailure;
  private final Runnable observer;
  private final boolean logOnDebug;
  private final boolean skipRecording;
  private final UID user;

  private final AtomicBoolean cancellationRequested = new AtomicBoolean();
  private final AtomicBoolean abortAfterFailure = new AtomicBoolean();
  private final AtomicBoolean skipCurrentStage = new AtomicBoolean();
  @Getter private final Progress progress = new Progress();
  private final AtomicReference<Process> incompleteProcess = new AtomicReference<>();
  private final AtomicReference<Stage> incompleteStage = new AtomicReference<>();
  private final ThreadLocal<Item> incompleteItem = new ThreadLocal<>();
  private final boolean usingErrorNotification;

  private int bucketingSize;
  private int bucketed;

  public RecordingJobProgress(JobConfiguration configuration) {
    this(null, configuration, JobProgress.noop(), true, () -> {}, false, false);
  }

  public RecordingJobProgress(
      @CheckForNull MessageService messageService,
      @CheckForNull JobConfiguration configuration,
      JobProgress tracker,
      boolean abortOnFailure,
      Runnable observer,
      boolean logOnDebug,
      boolean skipRecording) {
    this.messageService = messageService;
    this.configuration = configuration;
    this.tracker = tracker;
    this.abortOnFailure = abortOnFailure;
    this.observer = observer;
    this.logOnDebug = logOnDebug;
    this.skipRecording = skipRecording;
    this.usingErrorNotification =
        messageService != null
            && configuration != null
            && configuration.getJobType().isUsingErrorNotification();
    this.user =
        CurrentUserUtil.hasCurrentUser()
            ? UID.of(CurrentUserUtil.getCurrentUserDetails().getUid())
            : null;
  }

  /**
   * @return the exception that likely caused the job to abort
   */
  @CheckForNull
  public Exception getCause() {
    Process process = progress.sequence.peekLast();
    return process == null ? null : process.getCause();
  }

  public void requestCancellation() {
    if (cancellationRequested.compareAndSet(false, true)) {
      progress.sequence.forEach(
          p -> {
            p.cancel();
            logWarn(p, "cancelled", "cancellation requested by user");
          });
    }
  }

  @Override
  public boolean isSuccessful() {
    autoComplete();
    return !isCancelled()
        && progress.sequence.stream().allMatch(p -> p.getStatus() == JobProgress.Status.SUCCESS);
  }

  public void autoComplete() {
    Process process = progress.sequence.peekLast();
    if (process != null && process.getStatus() == Status.RUNNING) {
      // automatically complete processes that were not cleanly
      // complected by calling completedProcess at the end of the job
      completedProcess("(process completed implicitly)");
    }
  }

  @Override
  public boolean isCancelled() {
    return cancellationRequested.get();
  }

  @Override
  public boolean isAborted() {
    return abortAfterFailure.get();
  }

  @Override
  public boolean isSkipCurrentStage() {
    return skipCurrentStage.get() || isCancelled();
  }

  @Override
  public void addError(
      @Nonnull ErrorCode code,
      @CheckForNull String uid,
      @Nonnull String type,
      @Nonnull List<String> args) {
    addError(code.name(), uid, type, args);
  }

  @Override
  public void addError(
      @Nonnull ValidationCode code,
      @CheckForNull String uid,
      @Nonnull String type,
      @Nonnull List<String> args) {
    addError(code.name(), uid, type, args);
  }

  private void addError(
      @Nonnull String code,
      @CheckForNull String uid,
      @Nonnull String type,
      @Nonnull List<String> args) {
    try {
      // Note: we use empty string in case the UID is not known/defined yet to allow use in maps
      progress.addError(new Error(code, uid == null ? "" : uid, type, args));
    } catch (Exception ex) {
      log.error(format("Failed to add error: {} {} {} {}", code, uid, type, args), ex);
    }
  }

  @Override
  public void startingProcess(String description, Object... args) {
    observer.run();

    if (isCancelled()) throw cancellationException(false);
    String message = format(description, args);
    tracker.startingProcess(format(message, args));
    incompleteProcess.set(null);
    incompleteStage.set(null);
    incompleteItem.remove();
    Process process = addProcessRecord(message);
    logInfo(process, "started", message);
  }

  @Nonnull
  private RuntimeException cancellationException(boolean failedPostCondition) {
    Exception cause = getCause();
    if (skipRecording && cause instanceof RuntimeException rex) throw rex;
    CancellationException ex =
        failedPostCondition
            ? new CancellationException(postConditionFailureMessage())
            : new CancellationException();
    ex.initCause(cause);
    return ex;
  }

  private String postConditionFailureMessage() {
    String msg = "Non-null post-condition failed after: ";
    Process p = incompleteProcess.get();
    if (p != null) {
      msg += p.getDescription();
      Stage s = incompleteStage.get();
      if (s != null) {
        msg += "\n  => " + s.getDescription();
        Item i = incompleteItem.get();
        if (i != null) msg += "\n     => " + i.getDescription();
      }
    }
    return msg;
  }

  @Override
  public void completedProcess(String summary, Object... args) {
    observer.run();

    String message = format(summary, args);
    tracker.completedProcess(message);
    Process process = getOrAddLastIncompleteProcess();
    process.complete(message);
    logInfo(process, "completed", format(message, args));
  }

  @Override
  public void failedProcess(@CheckForNull String error, Object... args) {
    observer.run();

    String message = format(error, args);
    tracker.failedProcess(message);
    Process process = progress.sequence.peekLast();
    if (process == null || process.getCompletedTime() != null) {
      return;
    }
    if (process.getStatus() != Status.CANCELLED) {
      automaticAbort(false, message, null);
      process.completeExceptionally(message, null);
      logError(process, null, message);
    } else {
      process.completeExceptionally(message, null);
    }
  }

  @Override
  public void failedProcess(@Nonnull Exception cause) {
    observer.run();

    tracker.failedProcess(cause);
    Process process = progress.sequence.peekLast();
    if (process == null || process.getCompletedTime() != null) {
      return;
    }
    if (process.getStatus() != Status.CANCELLED) {
      cause = cancellationAsAbort(cause);
      String message = getMessage(cause);
      automaticAbort(false, message, cause);
      process.completeExceptionally(message, cause);
      sendErrorNotification(process, cause);
      logError(process, cause, message);
    } else {
      process.completeExceptionally(getMessage(cause), cause);
    }
  }

  @Override
  public void startingStage(
      @Nonnull String description, int workItems, @Nonnull FailurePolicy onFailure) {
    observer.run();

    if (isCancelled()) throw cancellationException(false);
    skipCurrentStage.set(false);
    tracker.startingStage(description, workItems);
    incompleteItem.remove();
    bucketingSize = 1;
    bucketed = 0;
    Stage stage =
        addStageRecord(getOrAddLastIncompleteProcess(), description, workItems, onFailure);
    logInfo(stage, "", description);
  }

  @Nonnull
  @Override
  public <T> T nonNullStagePostCondition(@CheckForNull T value) throws CancellationException {
    observer.run();
    if (value == null) throw cancellationException(true);
    return value;
  }

  @Override
  public void completedStage(String summary, Object... args) {
    observer.run();

    String message = format(summary, args);
    tracker.completedStage(message);
    Stage stage = getOrAddLastIncompleteStage();
    autoCompleteWorkItemBucket();
    stage.complete(message);
    logInfo(stage, "completed", message);
  }

  @Override
  public void failedStage(@Nonnull String error, Object... args) {
    observer.run();

    String message = format(error, args);
    tracker.failedStage(message);
    Stage stage = getOrAddLastIncompleteStage();
    autoCompleteWorkItemBucket();
    stage.completeExceptionally(message, null);
    if (stage.getOnFailure() != FailurePolicy.SKIP_STAGE) {
      automaticAbort(message, null);
    }
    logError(stage, null, message);
  }

  @Override
  public void failedStage(@Nonnull Exception cause) {
    observer.run();

    cause = cancellationAsAbort(cause);
    tracker.failedStage(cause);
    String message = getMessage(cause);
    Stage stage = getOrAddLastIncompleteStage();
    autoCompleteWorkItemBucket();
    stage.completeExceptionally(message, cause);
    if (stage.getOnFailure() != FailurePolicy.SKIP_STAGE) {
      automaticAbort(message, cause);
      sendErrorNotification(stage, cause);
    }
    logError(stage, cause, message);
  }

  @Override
  public void setWorkItemBucketing(int size) {
    bucketingSize = Math.max(1, size);
  }

  @Override
  public void startingWorkItem(@Nonnull String description, @Nonnull FailurePolicy onFailure) {
    if (bucketed % bucketingSize == 0) {
      observer.run();

      tracker.startingWorkItem(description, onFailure);
      Item item = addItemRecord(getOrAddLastIncompleteStage(), description, onFailure);
      logDebug(item, "started", description);
    }
    bucketed++;
  }

  @Override
  public void completedWorkItem(String summary, Object... args) {
    if (bucketed % bucketingSize == 0) {
      completeWorkItemBucket(summary, args);
    }
  }

  private void completeWorkItemBucket(String summary, Object... args) {
    observer.run();

    Item item = getOrAddLastIncompleteItem();
    String message =
        summary == null && bucketingSize > 1 ? getBucketSummary(item) : format(summary, args);
    tracker.completedWorkItem(message);
    item.complete(message);
    logDebug(item, "completed", message);
  }

  @Nonnull
  private String getBucketSummary(Item item) {
    int n = bucketed % bucketingSize;
    return item.getDescription() + " +" + (n == 0 ? bucketingSize : n);
  }

  private void autoCompleteWorkItemBucket() {
    if (bucketingSize > 1) {
      Item item = incompleteItem.get();
      if (item != null && !item.isComplete()) completeWorkItemBucket(null);
    }
  }

  @Override
  public void failedWorkItem(@Nonnull String error, Object... args) {
    bucketed = 0; // reset to restart bucketing on next item
    observer.run();

    String message = format(error, args);
    tracker.failedWorkItem(message);
    Item item = getOrAddLastIncompleteItem();
    item.completeExceptionally(message, null);
    if (!isSkipped(item)) {
      automaticAbort(message, null);
    }
    logError(item, null, message);
  }

  @Override
  public void failedWorkItem(@Nonnull Exception cause) {
    bucketed = 0; // reset to restart bucketing on next item
    observer.run();

    tracker.failedWorkItem(cause);
    String message = getMessage(cause);
    Item item = getOrAddLastIncompleteItem();
    item.completeExceptionally(message, cause);
    if (!isSkipped(item)) {
      automaticAbort(message, cause);
      sendErrorNotification(item, cause);
    }
    logError(item, cause, message);
  }

  private boolean isSkipped(Item item) {
    FailurePolicy onFailure = item.getOnFailure();
    if (onFailure == FailurePolicy.SKIP_STAGE) {
      skipCurrentStage.set(true);
      return true;
    }
    return onFailure == FailurePolicy.SKIP_ITEM
        || onFailure == FailurePolicy.SKIP_ITEM_OUTLIER
            && incompleteStage.get().getItems().stream().anyMatch(i -> i.status == Status.SUCCESS);
  }

  private void automaticAbort(String error, Exception cause) {
    automaticAbort(true, error, cause);
  }

  private void automaticAbort(boolean abortProcess, String error, Exception cause) {
    if (abortOnFailure
        // OBS! we only mark abort if we could mark cancellation
        // if we already cancelled manually we do not abort but cancel
        && cancellationRequested.compareAndSet(false, true)
        && abortAfterFailure.compareAndSet(false, true)
        && abortProcess) {
      progress.sequence.forEach(
          process -> {
            if (!process.isComplete()) {
              process.completeExceptionally(error, cause);
              logWarn(process, "aborted", "aborted after error: " + error);
            }
          });
    }
  }

  private Process getOrAddLastIncompleteProcess() {
    Process process = incompleteProcess.get();
    return process != null && !process.isComplete() ? process : addProcessRecord(null);
  }

  private Process addProcessRecord(String description) {
    Process process = new Process(description);
    if (configuration != null) {
      process.setJobId(configuration.getUid());
    }
    if (user != null) {
      process.setUserId(user.getValue());
    }
    incompleteProcess.set(process);
    if (skipRecording) progress.sequence.clear();
    progress.sequence.add(process);
    return process;
  }

  private Stage getOrAddLastIncompleteStage() {
    Stage stage = incompleteStage.get();
    return stage != null && !stage.isComplete()
        ? stage
        : addStageRecord(getOrAddLastIncompleteProcess(), null, -1, FailurePolicy.PARENT);
  }

  private Stage addStageRecord(
      Process process, String description, int totalItems, FailurePolicy onFailure) {
    Deque<Stage> stages = process.getStages();
    Stage stage =
        new Stage(
            description,
            totalItems,
            onFailure == FailurePolicy.PARENT ? FailurePolicy.FAIL : onFailure);
    if (skipRecording) stages.clear();
    stages.addLast(stage);
    incompleteStage.set(stage);
    return stage;
  }

  private Item getOrAddLastIncompleteItem() {
    Item item = incompleteItem.get();
    return item != null && !item.isComplete()
        ? item
        : addItemRecord(getOrAddLastIncompleteStage(), null, FailurePolicy.PARENT);
  }

  private Item addItemRecord(Stage stage, String description, FailurePolicy onFailure) {
    Deque<Item> items = stage.getItems();
    Item item =
        new Item(description, onFailure == FailurePolicy.PARENT ? stage.getOnFailure() : onFailure);
    if (skipRecording) items.clear();
    items.addLast(item);
    incompleteItem.set(item);
    return item;
  }

  private Exception cancellationAsAbort(Exception cause) {
    return cause instanceof CancellationException
            && (abortAfterFailure.get() || skipCurrentStage.get())
        ? new RuntimeException("processing aborted: " + getMessage(cause))
        : cause;
  }

  private void sendErrorNotification(Node node, Exception cause) {
    if (usingErrorNotification) {
      String subject = node.getClass().getSimpleName() + " failed: " + node.getDescription();
      try {
        messageService.asyncSendSystemErrorNotification(subject, cause);
      } catch (Exception ex) {
        log.debug("Failed to send error notification for failed job processing");
      }
    }
  }

  private void logError(Node failed, Exception cause, String message) {
    if (log.isErrorEnabled()) {
      String msg = formatLogMessage(failed, "failed", message);
      if (cause != null) {
        log.error(msg, cause);
      } else {
        log.error(msg);
      }
    }
  }

  private void logDebug(Node source, String action, String message) {
    if (log.isDebugEnabled()) {
      log.debug(formatLogMessage(source, action, message));
    }
  }

  private void logInfo(Node source, String action, String message) {
    if (logOnDebug) {
      if (log.isDebugEnabled()) {
        log.debug(formatLogMessage(source, action, message));
      }
    } else if (log.isInfoEnabled()) {
      log.info(formatLogMessage(source, action, message));
    }
  }

  private void logWarn(Node source, String action, String message) {
    if (log.isWarnEnabled()) {
      log.warn(formatLogMessage(source, action, message));
    }
  }

  private String formatLogMessage(Node source, String action, String message) {
    String duration =
        source.isComplete()
            ? " after "
                + Duration.ofMillis(source.getDuration()).toString().substring(2).toLowerCase()
            : "";
    String msg = message == null ? "" : ": " + message;
    String type = source instanceof Stage ? "" : source.getClass().getSimpleName() + " ";
    if (configuration == null) return format("{}{}{}{}", type, action, duration, msg);
    String jobType = configuration.getJobType().name();
    String uid = configuration.getUid();
    return format("[{} {}] {}{}{}{}", jobType, uid, type, action, duration, msg);
  }
}
