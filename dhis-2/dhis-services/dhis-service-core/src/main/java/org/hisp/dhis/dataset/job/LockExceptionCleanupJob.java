/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataset.job;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.LockExceptionCleanupJobParameters;
import org.springframework.stereotype.Component;

/**
 * Job to clean-up expired {@link org.hisp.dhis.dataset.LockException}s.
 *
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class LockExceptionCleanupJob implements Job
{
    private static final int DEFAULT_EXPIRY_AFTER_MONTHS = 6;

    private final DataSetService dataSetService;

    @Override
    public JobType getJobType()
    {
        return JobType.LOCK_EXCEPTION_CLEANUP;
    }

    @Override
    public void execute( JobConfiguration config, JobProgress progress )
    {
        progress.startingProcess( "Clean up expired lock exceptions" );

        LockExceptionCleanupJobParameters params = (LockExceptionCleanupJobParameters) config.getJobParameters();
        Integer months = params == null ? null : params.getExpiresAfterMonths();
        int expiryAfterMonth = max( 1, min( 12, months == null ? DEFAULT_EXPIRY_AFTER_MONTHS : months ) );
        ZoneId zoneId = ZoneId.systemDefault();
        Date createdBefore = Date.from(
            LocalDate.now( zoneId ).minusMonths( expiryAfterMonth ).atStartOfDay().atZone( zoneId ).toInstant() );

        progress.startingStage( format( "Clearing lock exceptions created before %1$tY-%1$tm-%1$td", createdBefore ) );
        progress.runStage( 0,
            deletedCount -> format( "%d lock exceptions deleted", deletedCount ),
            () -> dataSetService.deleteExpiredLockExceptions( createdBefore ) );

        progress.completedProcess( null );
    }
}
