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

import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;

/**
 * The {@link ControlledJobProgress} take care of the flow control aspect of
 * {@link JobProgress} API. Additional tracking can be done by wrapping another
 * {@link JobProgress} as {@link #tracker}.
 *
 * The implementation does allow for parallel items but would merge parallel
 * stages or processes. Stages and processes should always be sequential in a
 * main thread.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
public class ControlledJobProgress implements JobProgress
{
    private final JobConfiguration configuration;

    private final JobProgress tracker;

    private final boolean abortOnFailure;

    private final AtomicBoolean cancellationRequested = new AtomicBoolean();

    private final AtomicBoolean abortAfterFailure = new AtomicBoolean();

    private final Deque<Process> processes = new ConcurrentLinkedDeque<>();

    private final AtomicReference<Process> incompleteProcess = new AtomicReference<>();

    private final AtomicReference<Stage> incompleteStage = new AtomicReference<>();

    private final ThreadLocal<Item> incompleteItem = new ThreadLocal<>();

    public void requestCancellation()
    {
        if ( cancellationRequested.compareAndSet( false, true ) )
        {
            processes.forEach( Process::cancel );
        }
    }

    public Deque<Process> getProcesses()
    {
        return processes;
    }

    @Override
    public boolean isCancellationRequested()
    {
        return cancellationRequested.get();
    }

    @Override
    public void startingProcess( String description )
    {
        if ( isCancellationRequested() )
        {
            throw new CancellationException();
        }
        tracker.startingProcess( description );
        incompleteProcess.set( null );
        incompleteStage.set( null );
        incompleteItem.remove();
        addProcessRecord( description );
    }

    @Override
    public void completedProcess( String summary )
    {
        tracker.completedProcess( summary );
        getOrAddLastIncompleteProcess().complete( summary );
    }

    @Override
    public void failedProcess( String error )
    {
        tracker.failedProcess( error );
        if ( processes.getLast().getStatus() != Status.CANCELLED )
        {
            automaticAbort();
            getOrAddLastIncompleteProcess().completeExceptionally( error, null );
        }
    }

    @Override
    public void failedProcess( Exception cause )
    {
        cause = cancellationAsAbort( cause );
        tracker.failedProcess( cause );
        if ( processes.getLast().getStatus() != Status.CANCELLED )
        {
            automaticAbort();
            getOrAddLastIncompleteProcess().completeExceptionally( cause.getMessage(), cause );
        }
    }

    @Override
    public void startingStage( String description, int workItems )
    {
        if ( isCancellationRequested() )
        {
            throw new CancellationException();
        }
        tracker.startingStage( description, workItems );
        addStageRecord( getOrAddLastIncompleteProcess(), description, workItems );
    }

    @Override
    public void completedStage( String summary )
    {
        tracker.completedStage( summary );
        getOrAddLastIncompleteStage().complete( summary );
    }

    @Override
    public void failedStage( String error )
    {
        tracker.failedStage( error );
        automaticAbort();
        getOrAddLastIncompleteStage().completeExceptionally( error, null );
    }

    @Override
    public void failedStage( Exception cause )
    {
        cause = cancellationAsAbort( cause );
        tracker.failedStage( cause );
        automaticAbort();
        getOrAddLastIncompleteStage().completeExceptionally( cause.getMessage(), cause );
    }

    @Override
    public void startingWorkItem( String description )
    {
        tracker.startingWorkItem( description );
        addItemRecord( getOrAddLastIncompleteStage(), description );
    }

    @Override
    public void completedWorkItem( String summary )
    {
        tracker.completedWorkItem( summary );
        getOrAddLastIncompleteItem().complete( summary );
    }

    @Override
    public void failedWorkItem( String error )
    {
        tracker.failedWorkItem( error );
        automaticAbort();
        getOrAddLastIncompleteItem().completeExceptionally( error, null );
    }

    @Override
    public void failedWorkItem( Exception cause )
    {
        tracker.failedProcess( cause );
        automaticAbort();
        getOrAddLastIncompleteItem().completeExceptionally( cause.getMessage(), cause );
    }

    private void automaticAbort()
    {
        if ( abortOnFailure
            // OBS! we only mark abort if we could mark cancellation
            // if we already cancelled manually we do not abort but cancel
            && cancellationRequested.compareAndSet( false, true )
            && abortAfterFailure.compareAndSet( false, true ) )
        {
            processes.forEach( Process::abort );
        }
    }

    private Process getOrAddLastIncompleteProcess()
    {
        Process process = incompleteProcess.get();
        return process != null && !process.isComplete()
            ? process
            : addProcessRecord( null );
    }

    private Process addProcessRecord( String description )
    {
        Process process = new Process( description );
        if ( configuration != null )
        {
            process.setJobId( configuration.getUid() );
        }
        incompleteProcess.set( process );
        processes.add( process );
        return process;
    }

    private Stage getOrAddLastIncompleteStage()
    {
        Stage stage = incompleteStage.get();
        return stage != null && !stage.isComplete()
            ? stage
            : addStageRecord( getOrAddLastIncompleteProcess(), null, -1 );
    }

    private Stage addStageRecord( Process process, String description, int totalItems )
    {
        Deque<Stage> stages = process.getStages();
        Stage stage = new Stage( description, totalItems );
        stages.addLast( stage );
        incompleteStage.set( stage );
        return stage;
    }

    private Item getOrAddLastIncompleteItem()
    {
        Item item = incompleteItem.get();
        return item != null && !item.isComplete()
            ? item
            : addItemRecord( getOrAddLastIncompleteStage(), null );
    }

    private Item addItemRecord( Stage stage, String description )
    {
        Deque<Item> items = stage.getItems();
        Item item = new Item( description );
        items.addLast( item );
        incompleteItem.set( item );
        return item;
    }

    private Exception cancellationAsAbort( Exception cause )
    {
        return cause instanceof CancellationException && abortAfterFailure.get()
            ? new RuntimeException( "processing aborted: " + cause.getMessage() )
            : cause;
    }
}
