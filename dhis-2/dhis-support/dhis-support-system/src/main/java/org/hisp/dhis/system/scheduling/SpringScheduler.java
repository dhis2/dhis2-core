package org.hisp.dhis.system.scheduling;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * {@link Scheduler} implementation for use within the Spring framework.
 * @author Lars Helge Overland
 */
public class SpringScheduler
    implements Scheduler
{
    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    private Map<String, ScheduledFuture<?>> futures = new HashMap<>();

    private Map<String, ListenableFuture<?>> currentTasks = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private TaskScheduler taskScheduler;

    public void setTaskScheduler( TaskScheduler taskScheduler )
    {
        this.taskScheduler = taskScheduler;
    }

    private AsyncListenableTaskExecutor taskExecutor;

    public void setTaskExecutor( AsyncListenableTaskExecutor taskExecutor )
    {
        this.taskExecutor = taskExecutor;
    }

    // -------------------------------------------------------------------------
    // Scheduler implementation
    // -------------------------------------------------------------------------

    @Override
    public void executeTask( Runnable task )
    {
        taskExecutor.execute( task );
    }

    @Override
    public void executeTask( String taskKey, Runnable task )
    {
        ListenableFuture<?> future = taskExecutor.submitListenable( task );
        currentTasks.put( taskKey, future );
    }

    @Override
    public <T> ListenableFuture<T> executeTask( Callable<T> callable )
    {
        return taskExecutor.submitListenable( callable );
    }

    @Override
    public boolean scheduleTask( String key, Runnable task, String cronExpr )
    {
        if ( key != null && !futures.containsKey( key ) )
        {
            ScheduledFuture<?> future = taskScheduler.schedule( task, new CronTrigger( cronExpr ) );

            futures.put( key, future );

            log.info( "Scheduled task with key: " + key + " and cron: " + cronExpr );

            return true;
        }

        return false;
    }

    @Override
    public boolean stopTask( String key )
    {
        if ( key != null )
        {
            ScheduledFuture<?> future = futures.get( key );

            boolean result = future != null && future.cancel( true );

            futures.remove( key );

            log.info( "Stopped task with key: " + key + " successfully: " + result );

            return result;
        }

        return false;
    }

    @Override
    public boolean refreshTask( String key, Runnable task, String cronExpr )
    {
        if( getTaskStatus( key ) != ScheduledTaskStatus.NOT_STARTED )
        {
            stopTask( key );
        }

        return scheduleTask( key, task, cronExpr );
    }

    @Override
    public void stopAllTasks()
    {
        Iterator<String> keys = futures.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next();

            ScheduledFuture<?> future = futures.get( key );

            boolean result = future != null && future.cancel( true );

            keys.remove();

            log.info( "Stopped task with key: " + key + " successfully: " + result );
        }
    }

    @Override
    public ScheduledTaskStatus getTaskStatus( String key )
    {
        ScheduledFuture<?> future = futures.get( key );

        if ( future == null )
        {
            return ScheduledTaskStatus.NOT_STARTED;
        }
        else if ( future.isCancelled() )
        {
            return ScheduledTaskStatus.STOPPED;
        }
        else if ( future.isDone() )
        {
            return ScheduledTaskStatus.DONE;
        }
        else
        {
            return ScheduledTaskStatus.RUNNING;
        }
    }

    @Override
    public ScheduledTaskStatus getCurrentTaskStatus( String key )
    {
        ListenableFuture<?> future = currentTasks.get( key );

        if ( future == null )
        {
            return ScheduledTaskStatus.NOT_STARTED;
        }
        else if ( future.isCancelled() )
        {
            return ScheduledTaskStatus.STOPPED;
        }
        else if ( future.isDone() )
        {
            return ScheduledTaskStatus.DONE;
        }
        else
        {
            return ScheduledTaskStatus.RUNNING;
        }
    }

}
