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
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <h3>Tracking</h3>
 *
 * The {@link JobProgress} API is mainly contains methods to track the progress
 * of long-running jobs on three levels:
 * <ol>
 * <li>Process: Outermost bracket around the entire work done by a job</li>
 * <li>Stage: A logical step within the entire process of the job. A process is
 * a strict sequence of stages. Stages do not run in parallel.</li>
 * <li>(Work) Item: Performing an "non-interruptable" unit of work within a
 * stage. Items can be processed in parallel or strictly sequential. Usually
 * this is the function called in some form of a loop.</li>
 * </ol>
 *
 * For each of the three levels a new node is announced up front by calling the
 * corresponding {@link #startingProcess(String)},
 * {@link #startingStage(String)} or {@link #startingWorkItem(String)} method.
 *
 * The process will now expect a corresponding completion, for example
 * {@link #completedWorkItem(String)} in case of success or
 * {@link #failedWorkItem(String)} in case of an error. The different
 * {@link #runStage(Stream, Function, Consumer)} or
 * {@link #runStageInParallel(int, Collection, Function, Consumer)} helpers can
 * be used to do the error handling correctly and make sure the work items are
 * completed in both success and failure scenarios.
 *
 * For stages that do not have work items {@link #runStage(Runnable)} and
 * {@link #runStage(Object, Callable)} can be used to make sure completion is
 * handled correctly.
 *
 * For stages with work items the number of items should be announced using
 * {@link #startingStage(String, int)}. This is a best-effort estimation of the
 * actual items to allow observers a better understanding how much progress has
 * been made and how much work is left to do. For stages where this is not known
 * up-front the estimation is given as -1.
 *
 * <h3>Flow-Control</h3>
 *
 * The second part of the {@link JobProgress} is control flow. This is all based
 * on a single method {@link #isCancellationRequested()}. The coordination is
 * cooperative. This means cancellation of the running process might be
 * requested externally at any point or as a consequence of a failing work item.
 * This would flip the state returned by {@link #isCancellationRequested()}
 * which is/should be checked before starting a new stage or work item.
 *
 * A process should only continue starting new work as long as cancellation is
 * not requested. When cancellation is requested ongoing work items are finished
 * and the process exists cooperatively by not starting any further work.
 *
 * When a stage is cancelled the run-methods usually return false to give caller
 * a chance to react if needed. The next call to {@link #startingStage(String)}
 * will then throw a {@link CancellationException} and thereby short-circuit the
 * rest of the process.
 *
 * @author Jan Bernitt
 */
public interface JobProgress
{
    /*
     * Flow Control API:
     */

    /**
     * @return true, if the job got cancelled and requests the processing thread
     *         to terminate, else false to continue processing the job
     */
    boolean isCancellationRequested();

    /*
     * Tracking API:
     */

    void startingProcess( String description );

    default void startingProcess()
    {
        startingProcess( null );
    }

    void completedProcess( String summary );

    void failedProcess( String error );

    default void failedProcess( Exception cause )
    {
        String message = cause.getMessage();
        failedProcess( "Process failed: " + (isNotEmpty( message ) ? message : cause.getClass().getSimpleName()) );
    }

    /**
     * Announce start of a new stage.
     *
     * @param description describes the work done
     * @param workItems number of work items in the stage, -1 if unknown
     * @throws CancellationException in case cancellation has been requested
     *         before this stage had started
     */
    void startingStage( String description, int workItems )
        throws CancellationException;

    default void startingStage( String description )
    {
        startingStage( description, 0 );
    }

    void completedStage( String summary );

    void failedStage( String error );

    default void failedStage( Exception cause )
    {
        failedStage( cause.getMessage() );
    }

    void startingWorkItem( String description );

    default void startingWorkItem( int i )
    {
        startingWorkItem( "#" + (i + 1) );
    }

    void completedWorkItem( String summary );

    void failedWorkItem( String error );

    default void failedWorkItem( Exception cause )
    {
        failedWorkItem( cause.getMessage() );
    }

    /*
     * Running work items within a stage
     */

    /**
     * Runs {@link Runnable} work items as sequence.
     *
     * @param items the work items to run in the sequence to run them
     * @return true if all items were processed successful, otherwise false
     *
     * @see #runStage(Collection, Function, Consumer)
     */
    default boolean runStage( Collection<Runnable> items )
    {
        return runStage( items, item -> null, Runnable::run );
    }

    /**
     * Runs {@link Runnable} work items as sequence.
     *
     * @param items the work items to run in the sequence to run them with the
     *        keys used as item description. Items are processed in map
     *        iteration order.
     * @return true if all items were processed successful, otherwise false
     *
     * @see #runStage(Collection, Function, Consumer)
     */
    default boolean runStage( Map<String, Runnable> items )
    {
        return runStage( items.entrySet(), Entry::getKey, entry -> entry.getValue().run() );
    }

    /**
     * Run work items as sequence using a {@link Collection} of work item inputs
     * and an execution work {@link Consumer} function.
     *
     * @param items the work item inputs to run in the sequence to run them
     * @param description function to extract a description for a work item, may
     *        return {@code null}
     * @param work function to execute the work of a single work item input
     * @param <T> type of work item input
     * @return true if all items were processed successful, otherwise false
     *
     * @see #runStage(Collection, Function, Consumer)
     */
    default <T> boolean runStage( Collection<T> items, Function<T, String> description, Consumer<T> work )
    {
        return runStage( items.stream(), description, work );
    }

    /**
     * Run work items as sequence using a {@link Stream} of work item inputs and
     * an execution work {@link Consumer} function.
     *
     * @see #runStage(Stream, Function, Consumer,BiFunction)
     */
    default <T> boolean runStage( Stream<T> items, Function<T, String> description, Consumer<T> work )
    {
        return runStage( items, description, work, ( success, failed ) -> null );
    }

    /**
     * Run work items as sequence using a {@link Stream} of work item inputs and
     * an execution work {@link Consumer} function.
     * <p>
     * The entire stage only is considered failed in case a failing work item
     * caused a change of the cancellation requested status.
     *
     * @param items stream of inputs to execute a work item
     * @param description function to extract a description for a work item, may
     *        return {@code null}
     * @param work function to execute the work of a single work item input
     * @param summary accepts number of successful and failed items to compute a
     *        summary, may return {@code null}
     * @param <T> type of work item input
     * @return true if all items were processed successful, otherwise false
     */
    default <T> boolean runStage( Stream<T> items, Function<T, String> description, Consumer<T> work,
        BiFunction<Integer, Integer, String> summary )
    {
        int i = 0;
        int failed = 0;
        for ( Iterator<T> it = items.iterator(); it.hasNext(); )
        {
            T item = it.next();
            if ( isCancellationRequested() )
            {
                failedStage( new CancellationException(
                    format( "cancelled after %d successful and %d failed items", i - failed, failed ) ) );
                return false; // ends the stage immediately
            }
            String desc = description.apply( item );
            if ( desc == null )
            {
                startingWorkItem( i );
            }
            else
            {
                startingWorkItem( desc );
            }
            i++;
            try
            {
                work.accept( item );
                completedWorkItem( null );
            }
            catch ( RuntimeException ex )
            {
                failed++;
                boolean cancellationRequestedBefore = isCancellationRequested();
                failedWorkItem( ex );
                if ( !cancellationRequestedBefore && isCancellationRequested() )
                {
                    failedStage(
                        new CancellationException( "cancelled as failing item caused request for cancellation" ) );
                    return false;
                }
            }
        }
        completedStage( summary.apply( i - failed, failed ) );
        return true;
    }

    /**
     * Run a stage with no individual work items but a single work
     * {@link Runnable} with proper completion wrapping.
     * <p>
     * If the work task throws an {@link Exception} the stage is considered
     * failed otherwise it is considered complete when done.
     *
     * @param work work for the entire stage
     * @return true, if completed successful, false if completed exceptionally
     */
    default boolean runStage( Runnable work )
    {
        return runStage( false, () -> {
            work.run();
            return true;
        } );
    }

    /**
     * Run a stage with no individual work items but a single work
     * {@link Runnable} with proper completion wrapping.
     * <p>
     * If the work task throws an {@link Exception} the stage is considered
     * failed otherwise it is considered complete when done.
     *
     * @param errorValue the value returned in case the work throws an
     *        {@link Exception}
     * @param work work for the entire stage
     * @return the value returned by work task when successful or the errorValue
     *         in case the task threw an {@link Exception}
     */
    default <T> T runStage( T errorValue, Callable<T> work )
    {
        try
        {
            T res = work.call();
            completedStage( null );
            return res;
        }
        catch ( Exception ex )
        {
            failedStage( ex );
            return errorValue;
        }
    }

    /**
     * Runs the work items of a stage with the given parallelism. At most a
     * parallelism equal to the number of available processor cores is used.
     * <p>
     * If the parallelism is smaller or equal to 1 the items are processed
     * sequentially using {@link #runStage(Collection, Function, Consumer)}.
     * <p>
     * While the items are processed in parallel this method is synchronous for
     * the caller and will first return when all work is done.
     * <p>
     * If cancellation is requested work items might be skipped entirely.
     *
     * @param parallelism number of items that at maximum should be processed in
     *        parallel
     * @param items work item inputs to be processed in parallel
     * @param description function to extract a description for a work item, may
     *        return {@code null}
     * @param work function to execute the work of a single work item input
     * @param <T> type of work item input
     * @return true if all items were processed successful, otherwise false
     */
    default <T> boolean runStageInParallel( int parallelism, Collection<T> items, Function<T, String> description,
        Consumer<T> work )
    {
        if ( parallelism <= 1 )
        {
            return runStage( items, description, work );
        }
        int cores = Runtime.getRuntime().availableProcessors();
        boolean useCustomPool = parallelism >= cores;

        Callable<Boolean> task = () -> items.parallelStream().map( item -> {
            if ( isCancellationRequested() )
            {
                return false;
            }
            startingWorkItem( description.apply( item ) );
            try
            {
                work.accept( item );
                completedWorkItem( null );
                return true;
            }
            catch ( Exception ex )
            {
                failedWorkItem( ex );
                return false;
            }
        } ).reduce( Boolean::logicalAnd ).orElse( false );

        ForkJoinPool pool = useCustomPool ? new ForkJoinPool( parallelism ) : null;
        try
        {
            // this might not be obvious but running a parallel stream
            // as task in a FJP makes the stream use the pool
            boolean allSuccessful = pool == null
                ? task.call()
                : pool.submit( task ).get();
            if ( allSuccessful )
            {
                completedStage( null );
            }
            else if ( isCancellationRequested() )
            {
                failedStage( new CancellationException( "cancelled parallel processing" ) );
            }
            else
            {
                failedStage( (String) null );
            }
            return allSuccessful;
        }
        catch ( InterruptedException ex )
        {
            failedStage( ex );
            Thread.currentThread().interrupt();
        }
        catch ( Exception ex )
        {
            failedStage( ex );
        }
        finally
        {
            if ( pool != null )
            {
                pool.shutdown();
            }
        }
        return false;
    }

    /*
     * Model (for representing progress as data)
     */

    enum Status
    {
        RUNNING,
        SUCCESS,
        ERROR,
        CANCELLED
    }

    @Getter
    abstract class Node implements Serializable
    {
        @JsonProperty
        private String error;

        @JsonProperty
        private String summary;

        private Exception cause;

        @JsonProperty
        protected Status status = Status.RUNNING;

        @JsonProperty
        private Date completedTime;

        @JsonProperty
        public abstract Date getStartedTime();

        @JsonProperty
        public long getDuration()
        {
            return completedTime == null
                ? System.currentTimeMillis() - getStartedTime().getTime()
                : completedTime.getTime() - getStartedTime().getTime();
        }

        @JsonProperty
        public boolean isComplete()
        {
            return status != Status.RUNNING;
        }

        public void complete( String summary )
        {
            this.summary = summary;
            this.status = Status.SUCCESS;
            this.completedTime = new Date();
        }

        public void completeExceptionally( String error, Exception cause )
        {
            this.error = error;
            this.cause = cause;
            this.status = cause instanceof CancellationException ? Status.CANCELLED : Status.ERROR;
            this.completedTime = new Date();
        }
    }

    @Getter
    @RequiredArgsConstructor
    final class Process extends Node
    {
        public static Date startedTime( Collection<Process> job, Date defaultValue )
        {
            return job.isEmpty() ? defaultValue : job.iterator().next().getStartedTime();
        }

        private final Date startedTime = new Date();

        @JsonProperty
        private final String description;

        @JsonProperty
        private final Deque<Stage> stages = new ConcurrentLinkedDeque<>();

        @Setter
        @JsonProperty
        private String jobId;

        @JsonProperty
        private Date cancelledTime;

        public void cancel()
        {
            this.cancelledTime = new Date();
            this.status = Status.CANCELLED;
        }

        public void abort()
        {
            completeExceptionally( "aborted after failed stage or item", null );
        }
    }

    @Getter
    @RequiredArgsConstructor
    final class Stage extends Node
    {
        private final Date startedTime = new Date();

        @JsonProperty
        private final String description;

        /**
         * This is the number of expected items, negative when unknown, zero
         * when the stage has no items granularity
         */
        @JsonProperty
        private final int totalItems;

        @JsonProperty
        private final Deque<Item> items = new ConcurrentLinkedDeque<>();
    }

    @Getter
    @AllArgsConstructor
    final class Item extends Node
    {
        private final Date startedTime = new Date();

        @JsonProperty
        private final String description;
    }
}
