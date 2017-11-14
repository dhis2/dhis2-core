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
 * @author Henning Håkonsen
 */
public class JobConfigurationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    private JobConfiguration jobA;

    private JobConfiguration jobB;

    @Override
    protected void setUpTest()
        throws Exception
    {
        String CRON_EVERY_MIN = "0 * * ? * *";
        jobA = new JobConfiguration( "jobA", JobType.TEST, CRON_EVERY_MIN, new TestJobParameters( "test" ), false );
        jobB = new JobConfiguration( "jobB", JobType.DATA_INTEGRITY, CRON_EVERY_MIN, null, false );

        jobConfigurationService.addJobConfiguration( jobA );
        jobConfigurationService.addJobConfiguration( jobB );
    }

    @Test
    public void testGetJob()
    {
        List<JobConfiguration> jobConfigurationList = jobConfigurationService.getAllJobConfigurations();
        assertEquals( "The number of job configurations does not match", 7, jobConfigurationList.size() );

        assertEquals( JobType.TEST, jobConfigurationService.getJobConfigurationByUid( jobA.getUid() ).getJobType() );
        TestJobParameters jobParameters = (TestJobParameters) jobConfigurationService
            .getJobConfigurationByUid( jobA.getUid() ).getJobParameters();

        assertNotNull( jobParameters );
        assertEquals( "test", jobParameters.getMessage() );

        assertEquals( JobType.DATA_INTEGRITY,
            jobConfigurationService.getJobConfigurationByUid( jobB.getUid() ).getJobType() );
        assertNull( jobConfigurationService.getJobConfigurationByUid( jobB.getUid() ).getJobParameters() );
    }

    @Test
    public void testUpdateJob()
    {
        JobConfiguration test = jobConfigurationService.getJobConfigurationByUid( jobA.getUid() );
        test.setName( "testUpdate" );
        jobConfigurationService.updateJobConfiguration( test );

        assertEquals( "testUpdate", jobConfigurationService.getJobConfigurationByUid( jobA.getUid() ).getName() );
    }

    @Test
    public void testDeleteJob()
    {
        jobConfigurationService.deleteJobConfiguration( jobA );

        assertNull( jobConfigurationService.getJobConfigurationByUid( jobA.getUid() ) );
    }

}
