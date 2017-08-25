package org.hisp.dhis.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.joda.time.DateTime;

/**
 * @author Henning Håkonsen
 */
public class DefaultJob
    extends BaseIdentifiableObject
    implements IdentifiableObject, Job
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

    private static final Log log = LogFactory.getLog( DefaultJob.class );

    public DefaultJob(String name, JobType jobType, String cronExpression, JobConfiguration jobConfiguration) {
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
    public Runnable getRunnable()
    {
        return jobConfiguration.getRunnable();
    }
}

