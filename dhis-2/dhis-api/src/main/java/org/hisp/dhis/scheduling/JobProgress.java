/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static java.util.stream.StreamSupport.stream;

import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    void completedProcess( String summary );

    void failedProcess( String error );

    default void failedProcess( Exception cause )
    {
        failedProcess( "Process failed: " + cause.getMessage() );
    }

    /**
     *
     * @param description describes the work done
     * @param workItems number of work items in the stage, -1 if unknown
     */
    void startingStage( String description, int workItems );

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

    default boolean runStage( Iterable<Runnable> items )
    {
        return runStage( stream( items.spliterator(), false ) );
    }

    default <T> boolean runStage( Collection<T> items, Consumer<T> work )
    {
        return runStage( items.stream(), work );
    }

    default <T> boolean runStage( Stream<T> items, Consumer<T> work )
    {
        return runStage( items.map( item -> () -> work.accept( item ) ) );
    }

    default boolean runStage( Stream<Runnable> items )
    {
        int i = 0;
        for ( Iterator<Runnable> it = items.iterator(); it.hasNext(); )
        {
            Runnable item = it.next();
            if ( isCancellationRequested() )
            {
                return false; // ends the stage immediately
            }
            startingWorkItem( i++ );
            try
            {
                item.run();
                completedWorkItem( null );
            }
            catch ( RuntimeException ex )
            {
                boolean cancellationRequestedBefore = isCancellationRequested();
                failedWorkItem( ex );
                if ( !cancellationRequestedBefore && isCancellationRequested() )
                {
                    failedStage( ex );
                    return false;
                }
            }
        }
        completedStage( null );
        return true;
    }

    default boolean runStage( Runnable work )
    {
        return runStage( false, () -> {
            work.run();
            return true;
        } );
    }

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

    default <T> boolean runStageInParallel( int parallelism, Collection<T> items, Consumer<T> work )
    {
        if ( parallelism <= 1 )
        {
            return runStage( items, work );
        }
        int cores = Runtime.getRuntime().availableProcessors();
        boolean useCustomPool = parallelism >= cores;

        Callable<Boolean> task = () -> items.parallelStream().map( item -> {
            if ( isCancellationRequested() )
            {
                return false;
            }
            startingWorkItem( null );
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
        ERROR
    }

    @Getter
    abstract class Node
    {
        @JsonProperty
        private String error;

        @JsonProperty
        private String summary;

        @JsonProperty
        private Exception cause;

        @JsonProperty
        private Status status = Status.RUNNING;

        @JsonProperty
        private Date completedTime;

        @JsonProperty
        public abstract Date getStartedTime();

        @JsonProperty
        public long getDurationMillis()
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
            this.status = Status.ERROR;
            this.completedTime = new Date();
        }
    }

    @Getter
    @RequiredArgsConstructor
    final class Process extends Node
    {
        private final Date startedTime = new Date();

        @JsonProperty
        private final String description;

        @JsonProperty
        private final Deque<Stage> stages = new ConcurrentLinkedDeque<>();
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
