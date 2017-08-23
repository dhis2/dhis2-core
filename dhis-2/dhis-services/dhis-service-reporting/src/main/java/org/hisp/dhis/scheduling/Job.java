package org.hisp.dhis.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.joda.time.DateTime;

/**
 * @author Henning Håkonsen
 */
public class Job implements Runnable
{
    private String name;
    private String key;
    private JobType jobType;
    private String cronExpression;
    private boolean activated;
    private DateTime startTime;
    private DateTime endTime;
    private JobStatus status;
    private JobConfiguration jobConfiguration;

    private static final Log log = LogFactory.getLog( Job.class );

    public Job(String name, JobType jobType, String cronExpression, JobConfiguration jobConfiguration) {
        this.name = name;
        this.jobType = jobType;
        this.cronExpression = cronExpression;
        this.jobConfiguration = jobConfiguration;

        this.activated = true;
        this.startTime = null;
        this.endTime = null;
        this.status = JobStatus.SCHEDULED;
        this.key = "TODOKEY";
    }

    public void setJobConfiguration( JobType jobType, JobConfiguration jobConfiguration )
    {
        this.jobType = jobType;
        this.jobConfiguration = jobConfiguration;
    }

    public String toString()
    {
        return "Name: " + name + ", job type: " + jobType.name() + ", cronExpression: " + cronExpression +
            ", status: " + status;
    }


    // Getters and setters

    public String getKey()
    {
        return key;
    }

    public JobType getJobType()
    {
        return jobType;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public DateTime getStartTime()
    {
        return startTime;
    }

    public DateTime getEndTime()
    {
        return endTime;
    }

    @Override
    public void run()
    {
        if( activated && jobConfiguration != null ) {
            // Verify how we want to store start/endTime

            startTime = DateTime.now();

            try {
                this.status = JobStatus.RUNNING;
                jobConfiguration.run();
            } catch (Exception e)
            {
                this.status = JobStatus.FAILED;
                log.error( new Exception(e) );
            } finally
            {
                this.status = JobStatus.COMPLETED;
            }

            endTime = DateTime.now();

        } else {
            log.debug( "Job '" + name + "' not activated" );
        }
    }
}

