package org.hisp.dhis.system.scheduling;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

/**
 * Scheduler for managing the scheduling and execution of tasks.
 *
 * @author Lars Helge Overland
 */
public interface Scheduler
{
    String CRON_DAILY_11PM = "0 0 23 * * ?";
    String CRON_DAILY_0AM = "0 0 0 * * ?";
    String CRON_DAILY_2AM = "0 0 2 * * ?";
    String CRON_DAILY_5AM = "0 0 5 * * ?";
    String CRON_DAILY_6AM = "0 0 6 * * ?";
    String CRON_DAILY_7AM = "0 0 7 * * ?";
    String CRON_DAILY_8AM = "0 0 8 * * ?";

    String CRON_EVERY_MIN = "0 0/1 * * * ?";
    String CRON_EVERY_15MIN = "0 0/15 * * * ?";

    String CRON_TEST = "0 * * * * ?";

    /**
     * Execute the given task immediately.
     *
     * @task the task to execute.
     */
    void executeTask( Runnable task );

    /**
     * Execute the given task immediately. The task can be referenced
     * again through the given task key if the current task is not completed. A task cannot be scheduled if another
     * task with the same key is already scheduled.
     *
     * @task the task to execute.
     */
    void executeTask( String taskKey, Runnable task );

    /**
     * Execute the given task immediately and return a ListenableFuture.
     *
     * @param callable the task to execute.
     * @param <T> return type of the supplied callable.
     * @return a ListenableFuture representing the result of the task.
     */
    <T> ListenableFuture<T> executeTask( Callable<T> callable );

    /**
     * Schedule the given task for future execution. The task can be referenced
     * later through the given task key. A task cannot be scheduled if another
     * task with the same key is already scheduled. The task must be unique for
     * the task but can have an arbitrary value.
     *
     * @param key the task key, cannot be null.
     * @param task the task to schedule.
     * @param cronExpr the cron expression to use for the task scheduling.
     * @return true if the task was scheduled for execution as a result of this
     *         operation, false if not.
     */
    boolean scheduleTask( String key, Runnable task, String cronExpr );

    /**
     * Deactivates scheduling of the task with the given key.
     *
     * @param key the task key.
     * @return true if the task was deactivated as a result of this operation,
     *         false if not.
     */
    boolean stopTask( String key );

    /**
     * Stops and starts a task with the given key. If no key exists, still start a new task
     * @param key the task key, cannot be null.
     * @param task the task to schedule
     * @param cronExpr the cronExpression to use for the task scheduling.
     * @return true if the task was scheduled for execution as a result of this
     *         operation, false if not.
     */
    boolean refreshTask( String key, Runnable task, String cronExpr );

    /**
     * Deactivates scheduling for all tasks.
     */
    void stopAllTasks();

    /**
     * Gets the status for the task with the given key.
     *
     * @param key the task key.
     * @return the task status.
     */
    ScheduledTaskStatus getTaskStatus( String key );

    /**
     * Gets the status for the current task with the given key.
     *
     * @param key the task key.
     * @return the task status.
     */
    ScheduledTaskStatus getCurrentTaskStatus( String key );

}
