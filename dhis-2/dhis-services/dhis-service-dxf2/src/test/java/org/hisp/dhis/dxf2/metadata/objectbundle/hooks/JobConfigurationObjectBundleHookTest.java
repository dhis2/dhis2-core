package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

/**
 * Unit tests for {@link JobConfigurationObjectBundleHook}.
 *
 * @author Volker Schmidt
 */
public class JobConfigurationObjectBundleHookTest
{
    @Mock
    private JobConfigurationService jobConfigurationService;

    @Mock
    private SchedulingManager schedulingManager;

    @Mock
    private Job job;

    @InjectMocks
    private JobConfigurationObjectBundleHook hook;

    private JobConfiguration previousJobConfiguration;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        previousJobConfiguration = new JobConfiguration();
        previousJobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        previousJobConfiguration.setEnabled( true );
        previousJobConfiguration.setContinuousExecution( true );
    }

    @Test
    public void validateInternalNonConfigurableChangeError()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( previousJobConfiguration );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.ANALYTICSTABLE_UPDATE ) ) )
            .thenReturn( job );

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        jobConfiguration.setEnabled( false );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 1, errorReports.size() );
        Assert.assertEquals( ErrorCode.E7003, errorReports.get( 0 ).getErrorCode() );
    }

    @Test
    public void validateInternalNonConfigurableChange()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( previousJobConfiguration );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.ANALYTICSTABLE_UPDATE ) ) )
            .thenReturn( job );

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 0, errorReports.size() );
    }

    @Test
    public void validateInternalNonConfigurableShownValidationErrorNonE7010()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( previousJobConfiguration );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.ANALYTICSTABLE_UPDATE ) ) )
            .thenReturn( job );
        Mockito.when( job.validate() ).thenReturn( new ErrorReport( Class.class, ErrorCode.E7000 ) );

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 1, errorReports.size() );
        Assert.assertEquals( ErrorCode.E7000, errorReports.get( 0 ).getErrorCode() );
    }

    @Test
    public void validateInternalNonConfigurableShownValidationErrorE7010Configurable()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( previousJobConfiguration );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.DATA_SYNC ) ) )
            .thenReturn( job );
        Mockito.when( job.validate() ).thenReturn( new ErrorReport( Class.class, ErrorCode.E7010 ) );

        previousJobConfiguration.setJobType( JobType.DATA_SYNC );
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.DATA_SYNC );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 1, errorReports.size() );
        Assert.assertEquals( ErrorCode.E7010, errorReports.get( 0 ).getErrorCode() );
    }

    @Test
    public void validateInternalNonConfigurableShownValidationErrorE7010NoPrevious()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( null );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.ANALYTICSTABLE_UPDATE ) ) )
            .thenReturn( job );
        Mockito.when( job.validate() ).thenReturn( new ErrorReport( Class.class, ErrorCode.E7010 ) );

        previousJobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 1, errorReports.size() );
        Assert.assertEquals( ErrorCode.E7010, errorReports.get( 0 ).getErrorCode() );
    }

    @Test
    public void validateInternalNonConfigurableIgnoredValidationErrorE7010()
    {
        Mockito.when( jobConfigurationService.getJobConfigurationByUid( Mockito.eq( "jsdhJSJHD" ) ) )
            .thenReturn( previousJobConfiguration );
        Mockito.when( schedulingManager.getJob( Mockito.eq( JobType.ANALYTICSTABLE_UPDATE ) ) )
            .thenReturn( job );
        Mockito.when( job.validate() ).thenReturn( new ErrorReport( Class.class, ErrorCode.E7010 ) );

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setUid( "jsdhJSJHD" );
        jobConfiguration.setJobType( JobType.ANALYTICSTABLE_UPDATE );
        jobConfiguration.setEnabled( true );
        jobConfiguration.setContinuousExecution( true );

        List<ErrorReport> errorReports = hook.validateInternal( jobConfiguration );
        Assert.assertEquals( 0, errorReports.size() );
    }
}