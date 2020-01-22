package org.hisp.dhis.webapi.webdomain;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.scheduling.JobTypeInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobTypes
{
    private List<JobTypeInfo> jobTypes = new ArrayList<>();

    public JobTypes()
    {
    }

    public JobTypes( List<JobTypeInfo> jobTypes )
    {
        this.jobTypes = jobTypes;
    }

    @JsonProperty
    public List<JobTypeInfo> getJobTypes()
    {
        return jobTypes;
    }

    public void setJobTypes( List<JobTypeInfo> jobTypes )
    {
        this.jobTypes = jobTypes;
    }
}
