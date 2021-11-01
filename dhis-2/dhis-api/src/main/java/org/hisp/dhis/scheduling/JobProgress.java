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
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface JobProgress
{
    JobProgress IGNORANT = new JobProgress()
    {

        @Override
        public boolean isCancellationRequested()
        {
            return false;
        }

        @Override
        public void startingProcess( String description )
        {

        }

        @Override
        public void completedProcess( String summary )
        {

        }

        @Override
        public void failedProcess( String error )
        {

        }

        @Override
        public void startingStage( String description, int workItems )
        {

        }

        @Override
        public void completedStage( String summary )
        {

        }

        @Override
        public void failedStage( String error )
        {

        }

        @Override
        public void startingWorkItem( String description )
        {

        }

        @Override
        public void completedWorkItem( String summary )
        {

        }

        @Override
        public void failedWorkItem( String error )
        {

        }
    };

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

    // it could be part of the job config to select if the progress should also
    // be forwarded to the notifier - this allows to keep updating the notifier
    // but also to replace notifier updates with pure in-memory progress
    // tracking state

    // the job config ID does not need to be passed around as the progress
    // instance is created for a particular job and job config so it can already
    // know the ID internally

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

    default <T> boolean runStageInParallel( int parallelism, Collection<T> items, Consumer<T> work )
    {
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
            }
            catch ( Exception ex )
            {
                failedWorkItem( ex );
            }
            return true;
        } ).reduce( Boolean::logicalAnd ).orElse( false );

        ForkJoinPool fjp2 = new ForkJoinPool( parallelism );
        try
        {
            boolean allDone = fjp2.submit( task ).get();
            if ( allDone )
            {
                completedStage( null );
            }
            else
            {
                failedStage( (String) null );
            }
            return allDone;
        }
        catch ( InterruptedException e )
        {
            fjp2.shutdown();
            Thread.currentThread().interrupt();
            return false;
        }
        catch ( ExecutionException ex )
        {
            fjp2.shutdown();
            return false;
        }
    }
}
