package org.hisp.dhis.scheduling;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JobConfiguration}.
 *
 * @author Volker Schmidt
 */
public class JobConfigurationTest
{
    private JobParameters jobParameters;

    private JobConfiguration jobConfiguration;

    @Before
    public void setUp()
    {
        jobParameters = new MockJobParameters();

        jobConfiguration = new JobConfiguration();
        jobConfiguration.setJobType( JobType.ANALYTICS_TABLE );
        jobConfiguration.setJobStatus( JobStatus.COMPLETED );
        jobConfiguration.setJobParameters( jobParameters );
        jobConfiguration.setContinuousExecution( true );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setLeaderOnlyJob( true );
        jobConfiguration.setCronExpression( "0 0 6 * * ?" );
    }

    @Test
    public void hasNonConfigurableJobChangesFalse()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( true );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( false );
        Assert.assertFalse( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableJobChangesCron()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( true );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( true );
        jc.setCronExpression( "0 0 12 * * ?" );
        Assert.assertFalse( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableEnabled()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( true );
        jc.setEnabled( false );
        jc.setLeaderOnlyJob( true );
        Assert.assertTrue( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableJobChangesJobType()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.DATA_INTEGRITY );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( true );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( true );
        Assert.assertTrue( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableJobChangesJobStatus()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.STOPPED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( true );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( true );
        Assert.assertTrue( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableJobChangesJobParameters()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( new MockJobParameters() );
        jc.setContinuousExecution( true );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( true );
        Assert.assertTrue( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }

    @Test
    public void hasNonConfigurableJobChangesContinousExecution()
    {
        final JobConfiguration jc = new JobConfiguration();
        jc.setJobType( JobType.ANALYTICS_TABLE );
        jc.setJobStatus( JobStatus.COMPLETED );
        jc.setJobParameters( jobParameters );
        jc.setContinuousExecution( false );
        jc.setEnabled( true );
        jc.setLeaderOnlyJob( true );
        Assert.assertTrue( jobConfiguration.hasNonConfigurableJobChanges( jc ) );
    }
}