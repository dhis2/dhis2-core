package org.hisp.dhis.scheduling;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.schema.Property;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobTypeInfo
{
    private String name;

    private JobType jobType;

    private String key;

    private SchedulingType schedulingType;

    private List<Property> jobParameters = new ArrayList<>();

    public JobTypeInfo()
    {
    }

    public JobTypeInfo( String name, JobType jobType, List<Property> jobParameters )
    {
        this.name = name;
        this.jobType = jobType;
        this.key = jobType.getKey();
        this.schedulingType = jobType.getSchedulingType();
        this.jobParameters = jobParameters;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    @JsonProperty
    public JobType getJobType()
    {
        return jobType;
    }

    @JsonProperty
    public String getKey()
    {
        return key;
    }

    @JsonProperty
    public SchedulingType getSchedulingType()
    {
        return schedulingType;
    }

    @JsonProperty
    public List<Property> getJobParameters()
    {
        return jobParameters;
    }
}
