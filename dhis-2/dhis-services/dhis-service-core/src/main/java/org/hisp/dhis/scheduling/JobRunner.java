/*
 * Copyright (c) 2004-2023, University of Oslo
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

/**
 * Motivation of this separate API is purely to decouple and allow for composition of services via
 * spring.
 *
 * <p>The {@link JobSchedulerService#executeNow(String)} needs access to synchronously run a job in
 * a test setup. This is when it wants to call {@link #runDueJob(JobConfiguration)} directly.
 * Whereas otherwise, in a production setup, this method is never called directly but the {@link
 * JobScheduler} will internally run the jobs when they are due from its scheduling loop.
 *
 * @author Jan Bernitt
 */
public interface JobRunner {

  /**
   * During testing the scheduler might not be active in which case this is false. Otherwise, this
   * should always be true in a production environment.
   *
   * @return true, if the scheduler is running a scheduling loop cycle, otherwise false
   */
  boolean isScheduling();

  /**
   * Runs a job if it should now run according to its {@link SchedulingType} and related information
   * like the CRON expression or the delay time.
   *
   * @param config the job to check and potentially run
   */
  void runIfDue(JobConfiguration config);

  /**
   * Manually runs a job. OBS! This bypasses any actual checking if the job is due to run. When this
   * is called the job will run.
   *
   * @param config The job to run.
   */
  void runDueJob(JobConfiguration config);
}
