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
package org.hisp.dhis.scheduling;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 *
 *
 * <h3>Tracking</h3>
 *
 * The {@link JobProgress} API is mainly contains methods to track the progress of long-running jobs
 * on three levels:
 *
 * <ol>
 *   <li>Process: Outermost bracket around the entire work done by a job
 *   <li>Stage: A logical step within the entire process of the job. A process is a strict sequence
 *       of stages. Stages do not run in parallel.
 *   <li>(Work) Item: Performing an "non-interruptable" unit of work within a stage. Items can be
 *       processed in parallel or strictly sequential. Usually this is the function called in some
 *       form of a loop.
 * </ol>
 *
 * For each of the three levels a new node is announced up front by calling the corresponding {@link
 * #startingProcess(String)}, {@link #startingStage(String)} or {@link #startingWorkItem(String)}
 * method.
 *
 * <p>The process will now expect a corresponding completion, for example {@link
 * #completedWorkItem(String)} in case of success or {@link #failedWorkItem(String)} in case of an
 * error. The different {@link #runStage(Stream, Function, Consumer)} or {@link
 * #runStageInParallel(int, Collection, Function, Consumer)} helpers can be used to do the error
 * handling correctly and make sure the work items are completed in both success and failure
 * scenarios.
 *
 * <p>For stages that do not have work items {@link #runStage(Runnable)} and {@link
 * #runStage(Object, Callable)} can be used to make sure completion is handled correctly.
 *
 * <p>For stages with work items the number of items should be announced using {@link
 * #startingStage(String, int)}. This is a best-effort estimation of the actual items to allow
 * observers a better understanding how much progress has been made and how much work is left to do.
 * For stages where this is not known up-front the estimation is given as -1.
 *
 * <h3>Flow-Control</h3>
 *
 * The second part of the {@link JobProgress} is control flow. This is all based on a single method
 * {@link #isCancellationRequested()}. The coordination is cooperative. This means cancellation of
 * the running process might be requested externally at any point or as a consequence of a failing
 * work item. This would flip the state returned by {@link #isCancellationRequested()} which
 * is/should be checked before starting a new stage or work item.
 *
 * <p>A process should only continue starting new work as long as cancellation is not requested.
 * When cancellation is requested ongoing work items are finished and the process exists
 * cooperatively by not starting any further work.
 *
 * <p>When a stage is cancelled the run-methods usually return false to give caller a chance to
 * react if needed. The next call to {@link #startingStage(String)} will then throw a {@link
 * CancellationException} and thereby short-circuit the rest of the process.
 *
 * @author Jan Bernitt
 */
public interface JobProgress {
  /*
   * Flow Control API:
   */

  /**
   * @return true, if the job got cancelled and requests the processing thread to terminate, else
   *     false to continue processing the job
   */
  boolean isCancellationRequested();

  /**
   * Note that this indication resets to false once another stage is started.
   *
   * @return true, if the currently running stage should be skipped. By default, this is only the
   *     case if cancellation was requested.
   */
  default boolean isSkipCurrentStage() {
    return isCancellationRequested();
  }

  /*
   * Tracking API:
   */

  void startingProcess(String description);

  default void startingProcess() {
    startingProcess(null);
  }

  void completedProcess(String summary);

  void failedProcess(String error);

  default void failedProcess(Exception cause) {
    failedProcess("Process failed: " + getMessage(cause));
  }

  default void endingProcess(boolean success) {
    if (success) {
      completedProcess(null);
    } else {
      failedProcess((String) null);
    }
  }

  /**
   * Announce start of a new stage.
   *
   * @param description describes the work done
   * @param workItems number of work items in the stage, -1 if unknown
   * @param onFailure what to do should the stage or one of its items fail
   * @throws CancellationException in case cancellation has been requested before this stage had
   *     started
   */
  void startingStage(String description, int workItems, FailurePolicy onFailure)
      throws CancellationException;

  default void startingStage(String description, int workItems) {
    startingStage(description, workItems, FailurePolicy.PARENT);
  }

  default void startingStage(String description, FailurePolicy onFailure) {
    startingStage(description, 0, onFailure);
  }

  default void startingStage(String description) {
    startingStage(description, FailurePolicy.PARENT);
  }

  void completedStage(String summary);

  void failedStage(String error);

  default void failedStage(Exception cause) {
    failedStage(getMessage(cause));
  }

  default void startingWorkItem(String description) {
    startingWorkItem(description, FailurePolicy.PARENT);
  }

  void startingWorkItem(String description, FailurePolicy onFailure);

  default void startingWorkItem(int i) {
    startingWorkItem("#" + (i + 1));
  }

  void completedWorkItem(String summary);

  void failedWorkItem(String error);

  default void failedWorkItem(Exception cause) {
    failedWorkItem(getMessage(cause));
  }

  /*
   * Running work items within a stage
   */

  /**
   * Runs {@link Runnable} work items as sequence.
   *
   * @param items the work items to run in the sequence to run them
   * @see #runStage(Collection, Function, Consumer)
   */
  default void runStage(Collection<Runnable> items) {
    runStage(items, item -> null, Runnable::run);
  }

  /**
   * Runs {@link Runnable} work items as sequence.
   *
   * @param items the work items to run in the sequence to run them with the keys used as item
   *     description. Items are processed in map iteration order.
   * @see #runStage(Collection, Function, Consumer)
   */
  default void runStage(Map<String, Runnable> items) {
    runStage(items.entrySet(), Entry::getKey, entry -> entry.getValue().run());
  }

  /**
   * Run work items as sequence using a {@link Collection} of work item inputs and an execution work
   * {@link Consumer} function.
   *
   * @param items the work item inputs to run in the sequence to run them
   * @param description function to extract a description for a work item, may return {@code null}
   * @param work function to execute the work of a single work item input
   * @param <T> type of work item input
   * @see #runStage(Collection, Function, Consumer)
   */
  default <T> void runStage(
      Collection<T> items, Function<T, String> description, Consumer<T> work) {
    runStage(items.stream(), description, work);
  }

  /**
   * Run work items as sequence using a {@link Stream} of work item inputs and an execution work
   * {@link Consumer} function.
   *
   * @see #runStage(Stream, Function, Consumer,BiFunction)
   */
  default <T> void runStage(Stream<T> items, Function<T, String> description, Consumer<T> work) {
    runStage(
        items,
        description,
        work,
        (success, failed) -> format("%d successful and %d failed items", success, failed));
  }

  /**
   * Run work items as sequence using a {@link Stream} of work item inputs and an execution work
   * {@link Consumer} function.
   *
   * <p>
   *
   * @param items stream of inputs to execute a work item
   * @param description function to extract a description for a work item, may return {@code null}
   * @param work function to execute the work of a single work item input
   * @param summary accepts number of successful and failed items to compute a summary, may return
   *     {@code null}
   * @param <T> type of work item input
   */
  default <T> void runStage(
      Stream<T> items,
      Function<T, String> description,
      Consumer<T> work,
      BiFunction<Integer, Integer, String> summary) {
    runStage(
        items,
        description,
        null,
        item -> {
          work.accept(item);
          return item;
        },
        summary);
  }

  /**
   * Run work items as sequence using a {@link Stream} of work item inputs and an execution work
   * {@link Consumer} function.
   *
   * <p>
   *
   * @param items stream of inputs to execute a work item
   * @param description function to extract a description for a work item, may return {@code null}
   * @param result function to extract a result summary for a successful work item, may return
   *     {@code null}
   * @param work function to execute the work of a single work item input
   * @param summary accepts number of successful and failed items to compute a summary, may return
   *     {@code null}
   * @param <T> type of work item input
   */
  default <T, R> void runStage(
      Stream<T> items,
      Function<T, String> description,
      Function<R, String> result,
      Function<T, R> work,
      BiFunction<Integer, Integer, String> summary) {
    int i = 0;
    int failed = 0;
    for (Iterator<T> it = items.iterator(); it.hasNext(); ) {
      T item = it.next();
      // check for async cancel
      if (autoSkipStage(summary, i - failed, failed)) {
        return; // ends the stage immediately
      }
      String desc = description.apply(item);
      if (desc == null) {
        startingWorkItem(i);
      } else {
        startingWorkItem(desc);
      }
      i++;
      try {
        R res = work.apply(item);
        completedWorkItem(result == null ? null : result.apply(res));
      } catch (RuntimeException ex) {
        failed++;
        failedWorkItem(ex);
        if (autoSkipStage(summary, i - failed, failed)) {
          return; // ends the stage immediately
        }
      }
    }
    completedStage(summary == null ? null : summary.apply(i - failed, failed));
  }

  /**
   * Automatically complete a stage as failed based on the {@link #isSkipCurrentStage()} state.
   *
   * <p>This completes the stage either with a {@link CancellationException} in case {@link
   * #isCancellationRequested()} is true, or with just a summary text if it is false.
   *
   * @param summary optional callback to produce a summary
   * @param success number of successful items
   * @param failed number of failed items
   * @return true, if stage is/was skipped (complected as failed), false otherwise
   */
  default boolean autoSkipStage(
      BiFunction<Integer, Integer, String> summary, int success, int failed) {
    if (isSkipCurrentStage()) {
      String text = summary == null ? "" : summary.apply(success, failed);
      if (isCancellationRequested()) {
        failedStage(new CancellationException("skipped stage, failing item caused abort. " + text));
      } else {
        failedStage("skipped stage. " + text);
      }
      return true;
    }
    return false;
  }

  /**
   * @see #runStage(Function, Runnable)
   */
  default boolean runStage(Runnable work) {
    return runStage(null, work);
  }

  /**
   * Run a stage with no individual work items but a single work {@link Runnable} with proper
   * completion wrapping.
   *
   * <p>If the work task throws an {@link Exception} the stage is considered failed otherwise it is
   * considered complete when done.
   *
   * @param summary used when stage completes successfully (return value to summary)
   * @param work work for the entire stage
   * @return true, if completed successful, false if completed exceptionally
   */
  default boolean runStage(Function<Boolean, String> summary, Runnable work) {
    return runStage(
        false,
        summary,
        () -> {
          work.run();
          return true;
        });
  }

  /**
   * @see #runStage(Object, Function, Callable)
   */
  default <T> T runStage(T errorValue, Callable<T> work) {
    return runStage(errorValue, null, work);
  }

  /**
   * Run a stage with no individual work items but a single work {@link Runnable} with proper
   * completion wrapping.
   *
   * <p>If the work task throws an {@link Exception} the stage is considered failed otherwise it is
   * considered complete when done.
   *
   * @param errorValue the value returned in case the work throws an {@link Exception}
   * @param summary used when stage completes successfully (return value to summary)
   * @param work work for the entire stage
   * @return the value returned by work task when successful or the errorValue in case the task
   *     threw an {@link Exception}
   */
  default <T> T runStage(T errorValue, Function<T, String> summary, Callable<T> work) {
    try {
      T res = work.call();
      completedStage(summary == null ? null : summary.apply(res));
      return res;
    } catch (Exception ex) {
      failedStage(ex);
      return errorValue;
    }
  }

  /**
   * Runs the work items of a stage with the given parallelism. At most a parallelism equal to the
   * number of available processor cores is used.
   *
   * <p>If the parallelism is smaller or equal to 1 the items are processed sequentially using
   * {@link #runStage(Collection, Function, Consumer)}.
   *
   * <p>While the items are processed in parallel this method is synchronous for the caller and will
   * first return when all work is done.
   *
   * <p>If cancellation is requested work items might be skipped entirely.
   *
   * @param parallelism number of items that at maximum should be processed in parallel
   * @param items work item inputs to be processed in parallel
   * @param description function to extract a description for a work item, may return {@code null}
   * @param work function to execute the work of a single work item input
   * @param <T> type of work item input
   */
  default <T> void runStageInParallel(
      int parallelism, Collection<T> items, Function<T, String> description, Consumer<T> work) {
    if (parallelism <= 1) {
      runStage(items, description, work);
      return;
    }
    AtomicInteger success = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();

    Callable<Boolean> task =
        () ->
            items.parallelStream()
                .map(
                    item -> {
                      if (isSkipCurrentStage()) {
                        return false;
                      }
                      startingWorkItem(description.apply(item));
                      try {
                        work.accept(item);
                        completedWorkItem(null);
                        success.incrementAndGet();
                        return true;
                      } catch (Exception ex) {
                        failedWorkItem(ex);
                        failed.incrementAndGet();
                        return false;
                      }
                    })
                .reduce(Boolean::logicalAnd)
                .orElse(false);

    ForkJoinPool pool = new ForkJoinPool(parallelism);
    try {
      // this might not be obvious but running a parallel stream
      // as task in a FJP makes the stream use the pool
      boolean allSuccessful = pool.submit(task).get();
      if (allSuccessful) {
        completedStage(null);
      } else {
        autoSkipStage(
            (s, f) ->
                format("parallel processing aborted after %d successful and %d failed items", s, f),
            success.get(),
            failed.get());
      }
    } catch (InterruptedException ex) {
      failedStage(ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      failedStage(ex);
    } finally {
      pool.shutdown();
    }
  }

  /*
   * Model (for representing progress as data)
   */

  enum Status {
    RUNNING,
    SUCCESS,
    ERROR,
    CANCELLED
  }

  /**
   * How to behave when an item or stage fails. By default, a failure means the process is aborted.
   * Using a {@link FailurePolicy} allows to customise this behaviour on a stage or item basis.
   *
   * <p>The implementation of {@link FailurePolicy} is done by affecting {@link
   * #isSkipCurrentStage()} and {@link #isCancellationRequested()} acordingly after the failure
   * occured and has been tracked using one of the {@link #failedStage(String)} or {@link
   * #failedWorkItem(String)} methods.
   */
  enum FailurePolicy {
    /**
     * Default used to "inherit" the behaviour from the node level above. If the root is not
     * specified the behaviour is {@link #FAIL}.
     */
    PARENT,
    /** Fail and abort processing as soon as possible. This is the effective default. */
    FAIL,
    /**
     * When an item or stage fails the entire stage is skipped/ignored unconditionally. This means
     * no further items are processed in the stage.
     */
    SKIP_STAGE,
    /** When an item fails it is simply skipped/ignored unconditionally. */
    SKIP_ITEM,
    /**
     * Same as {@link #SKIP_ITEM} but only if there has been a successfully completed item before.
     * Otherwise, behaves like {@link #FAIL}.
     *
     * <p>This option is useful to only skip when it has been proven that the processing in general
     * works but some items just have issues.
     */
    SKIP_ITEM_OUTLIER
  }

  @Getter
  abstract class Node implements Serializable {
    @JsonProperty private String error;

    @JsonProperty private String summary;

    private Exception cause;

    @JsonProperty protected Status status = Status.RUNNING;

    @JsonProperty private Date completedTime;

    @JsonProperty
    public abstract Date getStartedTime();

    @JsonProperty
    public abstract String getDescription();

    @JsonProperty
    public long getDuration() {
      return completedTime == null
          ? System.currentTimeMillis() - getStartedTime().getTime()
          : completedTime.getTime() - getStartedTime().getTime();
    }

    @JsonProperty
    public boolean isComplete() {
      return status != Status.RUNNING;
    }

    public void complete(String summary) {
      this.summary = summary;
      this.completedTime = new Date();
      if (status == Status.RUNNING) {
        this.status = Status.SUCCESS;
      }
    }

    public void completeExceptionally(String error, Exception cause) {
      this.error = error;
      this.cause = cause;
      this.completedTime = new Date();
      if (status == Status.RUNNING) {
        this.status = cause instanceof CancellationException ? Status.CANCELLED : Status.ERROR;
      }
    }
  }

  @Getter
  @RequiredArgsConstructor
  final class Process extends Node {
    public static Date startedTime(Collection<Process> job, Date defaultValue) {
      return job.isEmpty() ? defaultValue : job.iterator().next().getStartedTime();
    }

    private final Date startedTime = new Date();

    @JsonProperty private final String description;

    @JsonProperty private final Deque<Stage> stages = new ConcurrentLinkedDeque<>();

    @Setter @JsonProperty private String jobId;

    @JsonProperty private Date cancelledTime;

    public void cancel() {
      this.cancelledTime = new Date();
      this.status = Status.CANCELLED;
    }
  }

  @Getter
  @RequiredArgsConstructor
  final class Stage extends Node {
    private final Date startedTime = new Date();

    @JsonProperty private final String description;

    /**
     * This is the number of expected items, negative when unknown, zero when the stage has no items
     * granularity
     */
    @JsonProperty private final int totalItems;

    @JsonProperty private final FailurePolicy onFailure;

    @JsonProperty private final Deque<Item> items = new ConcurrentLinkedDeque<>();
  }

  @Getter
  @AllArgsConstructor
  final class Item extends Node {
    private final Date startedTime = new Date();

    @JsonProperty private final String description;

    @JsonProperty private final FailurePolicy onFailure;
  }

  static String getMessage(Exception cause) {
    String msg = cause.getMessage();
    return msg == null || msg.isBlank() ? cause.getClass().getName() : msg;
  }
}
