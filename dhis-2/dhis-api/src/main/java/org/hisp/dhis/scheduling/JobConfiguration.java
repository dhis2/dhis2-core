/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.scheduling;

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import java.util.Date;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.scheduling.parameters.jackson.JobConfigurationSanitizer;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This class defines configuration for a job in the system. The job is defined
 * with general identifiers, as well as job specific, such as jobType
 * {@link JobType}.
 * <p>
 * All system jobs should be included in JobType enum and can be
 * scheduled/executed with {@link SchedulingManager}.
 * <p>
 * The class uses a custom deserializer to handle several potential
 * {@link JobParameters}.
 *
 * Note that this class uses {@link JobConfigurationSanitizer} for serialization
 * which needs to be update when new properties are added.
 *
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( converter = JobConfigurationSanitizer.class )
public class JobConfiguration
    extends BaseIdentifiableObject implements SecondaryMetadataObject
{
    // -------------------------------------------------------------------------
    // Externally configurable properties
    // -------------------------------------------------------------------------

    /**
     * The type of job.
     */
    private JobType jobType;

    /**
     * The cron expression used for scheduling the job. Relevant for scheduling
     * type {@link SchedulingType#CRON}.
     */
    private String cronExpression;

    /**
     * The delay in seconds between the completion of one job execution and the
     * start of the next. Relevant for scheduling type
     * {@link SchedulingType#FIXED_DELAY}.
     */
    private Integer delay;

    /**
     * Parameters of the job. Jobs can use their own implementation of the
     * {@link JobParameters} class.
     */
    private JobParameters jobParameters;

    /**
     * Indicates whether this job is currently enabled or disabled.
     */
    private boolean enabled = true;

    // -------------------------------------------------------------------------
    // Internally managed properties
    // -------------------------------------------------------------------------

    private JobStatus jobStatus;

    private Date nextExecutionTime;

    private JobStatus lastExecutedStatus = JobStatus.NOT_STARTED;

    private Date lastExecuted;

    private String lastRuntimeExecution;

    private boolean inMemoryJob = false;

    private String userUid;

    private boolean leaderOnlyJob = false;

    private String queueName;

    private Integer queuePosition;

    public JobConfiguration()
    {
    }

    /**
     * Constructor.
     *
     * @param name the job name.
     * @param jobType the {@link JobType}.
     * @param userUid the user UID.
     * @param inMemoryJob whether this is an in-memory job.
     */
    public JobConfiguration( String name, JobType jobType, String userUid, boolean inMemoryJob )
    {
        this.name = name;
        this.jobType = jobType;
        this.userUid = userUid;
        this.inMemoryJob = inMemoryJob;
        init();
    }

    /**
     * Constructor which implies enabled true and in-memory job false.
     *
     * @param name the job name.
     * @param jobType the {@link JobType}.
     * @param cronExpression the cron expression.
     * @param jobParameters the job parameters.
     */
    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters )
    {
        this( name, jobType, cronExpression, jobParameters, true, false );
    }

    /**
     * Constructor.
     *
     * @param name the job name.
     * @param jobType the {@link JobType}.
     * @param cronExpression the cron expression.
     * @param jobParameters the job parameters.
     * @param enabled whether this job is enabled.
     * @param inMemoryJob whether this is an in-memory job.
     */
    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters,
        boolean enabled, boolean inMemoryJob )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
        this.enabled = enabled;
        this.inMemoryJob = inMemoryJob;
        setJobStatus( enabled ? SCHEDULED : DISABLED );
        init();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    private void init()
    {
        if ( inMemoryJob )
        {
            setAutoFields();
        }
    }

    /**
     * Checks if this job has changes compared to the specified job
     * configuration that are only allowed for configurable jobs.
     *
     * @param other the job configuration that should be checked.
     * @return <code>true</code> if this job configuration has changes in fields
     *         that are only allowed for configurable jobs, <code>false</code>
     *         otherwise.
     */
    public boolean hasNonConfigurableJobChanges( @Nonnull JobConfiguration other )
    {
        if ( this.jobType != other.getJobType() )
        {
            return true;
        }
        if ( this.jobStatus != other.getJobStatus() )
        {
            return true;
        }
        if ( this.jobParameters != other.getJobParameters() )
        {
            return true;
        }
        return this.enabled != other.isEnabled();
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public boolean isConfigurable()
    {
        return jobType.isConfigurable();
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public SchedulingType getSchedulingType()
    {
        return jobType.getSchedulingType();
    }

    public boolean hasCronExpression()
    {
        return cronExpression != null && !cronExpression.isEmpty();
    }

    @Override
    public String toString()
    {
        return "JobConfiguration{" +
            "uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", jobType=" + jobType +
            ", cronExpression='" + cronExpression + '\'' +
            ", delay='" + delay + '\'' +
            ", jobParameters=" + jobParameters +
            ", enabled=" + enabled +
            ", inMemoryJob=" + inMemoryJob +
            ", lastRuntimeExecution='" + lastRuntimeExecution + '\'' +
            ", userUid='" + userUid + '\'' +
            ", leaderOnlyJob=" + leaderOnlyJob +
            ", jobStatus=" + jobStatus +
            ", nextExecutionTime=" + nextExecutionTime +
            ", lastExecutedStatus=" + lastExecutedStatus +
            ", lastExecuted=" + lastExecuted + '}';
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @JacksonXmlProperty
    @JsonProperty
    public JobType getJobType()
    {
        return jobType;
    }

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

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    @JacksonXmlProperty
    @JsonProperty
    public Integer getDelay()
    {
        return delay;
    }

    public void setDelay( Integer delay )
    {
        this.delay = delay;
    }

    /**
     * The sub type names refer to the {@link JobType} enumeration. Defaults to
     * null for unmapped job types.
     */
    @JacksonXmlProperty
    @JsonProperty
    @Property( value = PropertyType.COMPLEX, required = FALSE )
    @JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "jobType", defaultImpl = java.lang.Void.class )
    @JsonSubTypes( value = {
        @JsonSubTypes.Type( value = AnalyticsJobParameters.class, name = "ANALYTICS_TABLE" ),
        @JsonSubTypes.Type( value = ContinuousAnalyticsJobParameters.class, name = "CONTINUOUS_ANALYTICS_TABLE" ),
        @JsonSubTypes.Type( value = MonitoringJobParameters.class, name = "MONITORING" ),
        @JsonSubTypes.Type( value = PredictorJobParameters.class, name = "PREDICTOR" ),
        @JsonSubTypes.Type( value = PushAnalysisJobParameters.class, name = "PUSH_ANALYSIS" ),
        @JsonSubTypes.Type( value = SmsJobParameters.class, name = "SMS_SEND" ),
        @JsonSubTypes.Type( value = MetadataSyncJobParameters.class, name = "META_DATA_SYNC" ),
        @JsonSubTypes.Type( value = EventProgramsDataSynchronizationJobParameters.class, name = "EVENT_PROGRAMS_DATA_SYNC" ),
        @JsonSubTypes.Type( value = TrackerProgramsDataSynchronizationJobParameters.class, name = "TRACKER_PROGRAMS_DATA_SYNC" ),
        @JsonSubTypes.Type( value = DataSynchronizationJobParameters.class, name = "DATA_SYNC" ),
        @JsonSubTypes.Type( value = DisableInactiveUsersJobParameters.class, name = "DISABLE_INACTIVE_USERS" ),
        @JsonSubTypes.Type( value = TrackerTrigramIndexJobParameters.class, name = "TRACKER_SEARCH_OPTIMIZATION" ),
        @JsonSubTypes.Type( value = DataIntegrityJobParameters.class, name = "DATA_INTEGRITY" ),
        @JsonSubTypes.Type( value = AggregateDataExchangeJobParameters.class, name = "AGGREGATE_DATA_EXCHANGE" ),
        @JsonSubTypes.Type( value = TestJobParameters.class, name = "TEST" )
    } )
    public JobParameters getJobParameters()
    {
        return jobParameters;
    }

    public void setJobParameters( JobParameters jobParameters )
    {
        this.jobParameters = jobParameters;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public JobStatus getJobStatus()
    {
        return jobStatus;
    }

    public void setJobStatus( JobStatus jobStatus )
    {
        this.jobStatus = jobStatus;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }

    /**
     * Only set next execution time if the job is not continuous.
     */
    public void setNextExecutionTime( Date nextExecutionTime )
    {
        if ( cronExpression == null || cronExpression.equals( "" ) || cronExpression.equals( "* * * * * ?" ) )
        {
            return;
        }

        if ( nextExecutionTime != null )
        {
            this.nextExecutionTime = nextExecutionTime;
        }
        else
        {
            this.nextExecutionTime = new CronTrigger( cronExpression ).nextExecutionTime( new SimpleTriggerContext() );
        }
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public Date getLastExecuted()
    {
        return lastExecuted;
    }

    public void setLastExecuted( Date lastExecuted )
    {
        this.lastExecuted = lastExecuted;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public JobStatus getLastExecutedStatus()
    {
        return lastExecutedStatus;
    }

    public void setLastExecutedStatus( JobStatus lastExecutedStatus )
    {
        this.lastExecutedStatus = lastExecutedStatus;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public String getLastRuntimeExecution()
    {
        return lastRuntimeExecution;
    }

    public void setLastRuntimeExecution( String lastRuntimeExecution )
    {
        this.lastRuntimeExecution = lastRuntimeExecution;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isLeaderOnlyJob()
    {
        return leaderOnlyJob;
    }

    public void setLeaderOnlyJob( boolean leaderOnlyJob )
    {
        this.leaderOnlyJob = leaderOnlyJob;
    }

    public boolean isInMemoryJob()
    {
        return inMemoryJob;
    }

    public void setInMemoryJob( boolean inMemoryJob )
    {
        this.inMemoryJob = inMemoryJob;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public String getUserUid()
    {
        return userUid;
    }

    public void setUserUid( String userUid )
    {
        this.userUid = userUid;
    }

    public String getQueueIdentifier()
    {
        return getQueueName() == null ? getUid() : getQueueName();
    }

    public String getQueueName()
    {
        return queueName;
    }

    public void setQueueName( String name )
    {
        this.queueName = name;
    }

    public Integer getQueuePosition()
    {
        return queuePosition;
    }

    public void setQueuePosition( Integer position )
    {
        this.queuePosition = position;
    }

    /**
     * @return true if this configuration is part of a queue, false otherwise
     */
    public boolean isUsedInQueue()
    {
        return getQueueName() != null;
    }
}
