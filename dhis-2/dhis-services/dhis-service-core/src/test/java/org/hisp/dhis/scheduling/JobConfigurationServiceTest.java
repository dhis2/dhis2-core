package org.hisp.dhis.scheduling;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by henninghakonsen on 30/10/2017.
 * Project: dhis-2.
 */
public class JobConfigurationServiceTest
        extends DhisSpringTest
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    String CRON_EVERY_MIN = "0 * * ? * *";

    JobConfiguration jobA;
    JobConfiguration jobB;

    @Override
    protected void setUpTest() throws Exception {
        jobA = new JobConfiguration( "jobA", JobType.TEST, CRON_EVERY_MIN, new TestJobParameters( "test" ), true, false, null );
        jobB = new JobConfiguration( "jobB", JobType.DATA_INTEGRITY, CRON_EVERY_MIN, null, true, false, null );

        jobConfigurationService.addJobConfiguration( jobA );
        jobConfigurationService.addJobConfiguration( jobB );
    }

    @Test
    public void testGetJob()
    {
        List<JobConfiguration> jobConfigurationList = jobConfigurationService.getAllJobConfigurations();
        assertEquals(  "The number of job configurations does not match",2, jobConfigurationList.size() );

        assertEquals( JobType.TEST, jobConfigurationService.getJobConfigurationWithUid( jobA.getUid() ).getJobType() );
        TestJobParameters jobParameters = (TestJobParameters) jobConfigurationService.getJobConfigurationWithUid( jobA.getUid() ).getJobParameters();

        assertNotNull( jobParameters );
        assertEquals( "test", jobParameters.getMessage() );

        assertEquals( JobType.DATA_INTEGRITY, jobConfigurationService.getJobConfigurationWithUid( jobB.getUid() ).getJobType() );
        assertNull( jobConfigurationService.getJobConfigurationWithUid( jobB.getUid() ).getJobParameters() );
    }

    @Test
    public void testUpdateJob()
    {
        JobConfiguration test = jobConfigurationService.getJobConfigurationWithUid( jobA.getUid() );
        test.setName( "testUpdate" );
        jobConfigurationService.updateJobConfiguration( test );

        assertEquals( "testUpdate", jobConfigurationService.getJobConfigurationWithUid( jobA.getUid() ).getName() );
    }

    @Test
    public void testDeleteJob()
    {
        jobConfigurationService.deleteJobConfiguration( jobA.getUid() );

        assertNull( jobConfigurationService.getJobConfigurationWithUid( jobA.getUid() ) );
    }

}
