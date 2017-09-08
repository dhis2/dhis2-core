package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.*;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( using = JobConfigurationDeserializer.class )
@JsonSerialize( using = JobConfigurationSerializer.class )
public class JobConfiguration
    extends BaseIdentifiableObject
    implements IdentifiableObject, MetadataObject
{
    private String cronExpression;
    private JobType jobType;
    private JobStatus jobStatus = JobStatus.SCHEDULED;
    private Date lastExecuted;

    private JobParameters jobParameters;

    // Used in JobService for sorting jobConfigurations based on cron expression
    private Date nextExecutionTime;

    public JobConfiguration ()
    {
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
    }

    public String toString()
    {
        return "Name: " + name + ", job type: " + jobType.name() + ", cronExpression: " + cronExpression + ", parameters: " + jobParameters + ", status: " + jobStatus;
    }

    public void setNextExecutionTime()
    {
        this.nextExecutionTime = new CronTrigger( cronExpression ).nextExecutionTime( new SimpleTriggerContext(  ) );
    }

    @JsonSetter
    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    @JsonSetter
    public void setJobType( JobType jobType )
    {
        this.jobType = jobType;
    }

    @JacksonXmlProperty
    @JsonProperty
    public String getCronExpression()
    {
        return cronExpression;
    }

    @JacksonXmlProperty
    @JsonProperty
    public JobType getJobType()
    {
        return jobType;
    }

    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }

    @Override
    public int compareTo( IdentifiableObject jobConfiguration )
    {
        return nextExecutionTime.compareTo( ((JobConfiguration) jobConfiguration).getNextExecutionTime() );
    }

    @JacksonXmlProperty
    @JsonProperty
    public JobParameters getJobParameters()
    {
        return jobParameters;
    }

    @JsonSetter
    public void setJobParameters( JobParameters jobParameters )
    {
        this.jobParameters = jobParameters;
    }

    @JsonSetter
    public void setJobStatus( JobStatus jobStatus )
    {
        this.jobStatus = jobStatus;
    }

    @JacksonXmlProperty
    @JsonProperty
    public JobStatus getJobStatus( )
    {
        return jobStatus;
    }

    @JacksonXmlProperty
    @JsonProperty
    public Date getLastExecuted()
    {
        return lastExecuted;
    }

    @JsonSetter
    public void setLastExecuted( Date lastExecuted )
    {
        this.lastExecuted = lastExecuted;
    }
}
