package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
public class Job
{
    private String name;
    private JobType jobType;
    private String cronExpression;
    private boolean activate;
    private Date startTime;
    private Date endTime;
    private JobStatus status;
    private JobConfiguration jobConfiguration;

    public Job(String name, JobType jobType, String cronExpression) {
        this.name = name;
        this.jobType = jobType;
        this.cronExpression = cronExpression;

        this.activate = true;
        this.startTime = null;
        this.endTime = null;
        this.status = JobStatus.SCHEDULED;
    }

    public String toString()
    {
        return "Name: " + name + ", job type: " + jobType.name() + ", cronExpression: " + cronExpression + ", status: " + status;
    }

    public String getName()
    {
        return name;
    }

    public JobType getJobType()
    {
        return jobType;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public boolean isActivate()
    {
        return activate;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public JobConfiguration getJobConfiguration()
    {
        return jobConfiguration;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setJobType( JobType jobType )
    {
        this.jobType = jobType;
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public void setActivate( boolean activate )
    {
        this.activate = activate;
    }

    public void setStartTime( Date startTime )
    {
        this.startTime = startTime;
    }

    public void setEndTime( Date endTime )
    {
        this.endTime = endTime;
    }

    public void setStatus( JobStatus status )
    {
        this.status = status;
    }

    public void setJobConfiguration( JobConfiguration jobConfiguration )
    {
        this.jobConfiguration = jobConfiguration;
    }
}

