package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.util.Date;

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

/**
 * This class defines configuration for a job in the system. The job is defined with general identifiers, as well as job
 * specific, such as jobType {@link JobType}.
 *
 * All system jobs should be included in JobType enum and can be scheduled/executed with {@link SchedulingManager}.
 *
 * The class uses a custom deserializer to handle several potential {@link JobParameters}.
 * 
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( using = JobConfigurationDeserializer.class )
public class JobConfiguration
        extends BaseIdentifiableObject
        implements MetadataObject
{
    private String cronExpression;

    private JobType jobType;

    private JobStatus jobStatus = JobStatus.SCHEDULED;

    private Date lastExecuted;

    private JobStatus lastExecutedStatus = JobStatus.SCHEDULED;

    private JobParameters jobParameters = null;

    private boolean enabled;

    private boolean continuousExecution;

    private Date nextExecutionTime;

    private JobId jobId;

    public JobConfiguration ()
    {
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters, boolean enabled, boolean continuousExecution, JobId jobId )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
        this.enabled = enabled;
        this.continuousExecution = continuousExecution;
        this.jobId = jobId;
        setNextExecutionTime( null );
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public void setJobType( JobType jobType )
    {
        this.jobType = jobType;
    }

    public void setJobStatus( JobStatus jobStatus )
    {
        this.jobStatus = jobStatus;
    }

    public void setLastExecuted( Date lastExecuted )
    {
        this.lastExecuted = lastExecuted;
    }

    public void setLastExecutedStatus( JobStatus lastExecutedStatus )
    {
        this.lastExecutedStatus = lastExecutedStatus;
    }

    public void setJobParameters( JobParameters jobParameters )
    {
        this.jobParameters = jobParameters;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setNextExecutionTime( Date nextExecutionTime )
    {
        if( nextExecutionTime != null) this.nextExecutionTime = nextExecutionTime;
        else {
            if( !isContinuousExecution() && !cronExpression.equals( "" ) ) this.nextExecutionTime = new CronTrigger( cronExpression ).nextExecutionTime( new SimpleTriggerContext(  ) );
        }
    }

    public void setJobId(JobId jobId) {
        this.jobId = jobId;
    }

    public void setContinuousExecution(boolean continuousExecution) {
        this.continuousExecution = continuousExecution;
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

    @JacksonXmlProperty
    @JsonProperty
    public JobStatus getLastExecutedStatus()
    {
        return lastExecutedStatus;
    }

    @JacksonXmlProperty
    @JsonProperty
    @Property( required = FALSE )
    public JobParameters getJobParameters()
    {
        return jobParameters;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean getEnabled()
    {
        return enabled;
    }

    @JacksonXmlProperty
    @JsonProperty
    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isContinuousExecution() {
        return continuousExecution;
    }

    @Override
    public int compareTo( IdentifiableObject jobConfiguration  )
    {
        return nextExecutionTime.compareTo( ((JobConfiguration) jobConfiguration).getNextExecutionTime() );
    }

    @JacksonXmlProperty
    @JsonProperty
    public JobId getJobId() {
        return jobId;
    }

    @Override
    public String toString() {
        return "Name: " + name +
                "\nType: " + jobType +
                "\nEnabled: " + enabled +
                "\nCron expression: " + cronExpression +
                "\nNext execution time: " + nextExecutionTime;
    }
}
