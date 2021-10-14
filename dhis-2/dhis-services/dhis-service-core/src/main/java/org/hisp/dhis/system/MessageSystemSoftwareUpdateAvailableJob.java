package org.hisp.dhis.system;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@AllArgsConstructor
@Component( "messageSystemSoftwareUpdateAvailableJob" )
public class MessageSystemSoftwareUpdateAvailableJob implements Job, Runnable
{
    @Override public void run()
    {
        this.execute( null );
    }

    @Override public JobType getJobType()
    {
        return JobType.SYSTEM_SOFTWARE_UPDATE;
    }

    @Override public void execute( JobConfiguration jobConfiguration )
    {
            // TODO: Fetch updates file

            log.info( "SOMETHING SHOULD HAPPEN HERE" );
    }

    @Override public ErrorReport validate()
    {
        return Job.super.validate();
    }
}


