package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.scheduling.JobType;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class JobConfiguration
{
    private int codeSize = 10;

    private String name;
    private String key;
    private String cronExpression;
    private JobType jobType;

    public JobConfiguration( String name, JobType jobType, String cronExpression )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.key = CodeGenerator.generateCode( codeSize );
        this.jobType = jobType;
    }

    public String toString()
    {
        return "Name: " + name + ", job type: " + jobType.name() + ", cronExpression: " + cronExpression;
    }

    public String getKey()
    {
        return key;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public JobType getJobType()
    {
        return jobType;
    }
}
