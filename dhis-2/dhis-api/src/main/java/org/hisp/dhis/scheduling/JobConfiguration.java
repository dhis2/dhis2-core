package org.hisp.dhis.scheduling;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import java.util.Date;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.jackson.JobConfigurationSanitizer;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This class defines configuration for a job in the system. The job is defined with general identifiers, as well as job
 * specific, such as jobType {@link JobType}.
 * <p>
 * All system jobs should be included in JobType enum and can be scheduled/executed with {@link SchedulingManager}.
 * <p>
 * The class uses a custom deserializer to handle several potential {@link JobParameters}.
 *
 * The configurable property in the configurable property in the method based on the job type we are adding.
 *
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( converter = JobConfigurationSanitizer.class )
public class JobConfiguration
    extends BaseIdentifiableObject implements SecondaryMetadataObject
{
    private JobType jobType;

    /**
     * The cron expression used for scheduling the job. Relevant for scheduling
     * type {@link SchedulingType#CRON}.
     */
    private String cronExpression;

    /**
     * The delay in seconds between the completion of one job execution and the
     * start of the next. Relevant for scheduling type {@link SchedulingType#FIXED_DELAY}.
     */
    private Integer delay;

    private JobStatus jobStatus;

    private Date nextExecutionTime;

    private JobStatus lastExecutedStatus = JobStatus.NOT_STARTED;

    private Date lastExecuted;

    private String lastRuntimeExecution;

    private JobParameters jobParameters;

    private boolean continuousExecution = false;

    private boolean enabled = true;

    private boolean inMemoryJob = false;

    private String userUid;

    private boolean leaderOnlyJob = false;

    public JobConfiguration()
    {
    }

    public JobConfiguration( String name, JobType jobType, String userUid, boolean inMemoryJob )
    {
        this.name = name;
        this.jobType = jobType;
        this.userUid = userUid;
        this.inMemoryJob = inMemoryJob;
        init();
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters,
        boolean continuousExecution, boolean enabled )
    {
        this( name, jobType, cronExpression, null, jobParameters, continuousExecution, enabled, false );
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, Integer delay, JobParameters jobParameters,
        boolean continuousExecution, boolean enabled, boolean inMemoryJob )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.delay = delay;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
        this.continuousExecution = continuousExecution;
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
     * Checks if this job has changes compared to the specified job configuration that are only
     * allowed for configurable jobs.
     *
     * @param jobConfiguration the job configuration that should be checked.
     * @return <code>true</code> if this job configuration has changes in fields that are only
     * allowed for configurable jobs, <code>false</code> otherwise.
     */
    public boolean hasNonConfigurableJobChanges( @Nonnull JobConfiguration jobConfiguration )
    {
        if ( jobType != jobConfiguration.getJobType() )
        {
            return true;
        }
        if ( jobStatus != jobConfiguration.getJobStatus() )
        {
            return true;
        }
        if ( jobParameters != jobConfiguration.getJobParameters() )
        {
            return true;
        }
        if ( continuousExecution != jobConfiguration.isContinuousExecution() )
        {
            return true;
        }
        return enabled != jobConfiguration.isEnabled();
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public boolean isConfigurable()
    {
        return jobType.isConfigurable();
    }

    @Override
    public String toString()
    {
        return "JobConfiguration{" +
            "uid='" + uid + '\'' +
            ", displayName='" + displayName + '\'' +
            ", jobType=" + jobType +
            ", cronExpression='" + cronExpression + '\'' +
            ", delay='" + delay + '\'' +
            ", jobParameters=" + jobParameters +
            ", enabled=" + enabled +
            ", continuousExecution=" + continuousExecution +
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
    @JsonTypeId
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
    @JsonTypeId
    public Integer getDelay()
    {
        return delay;
    }

    public void setDelay( Integer delay )
    {
        this.delay = delay;
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
    @Property( required = FALSE )
    @JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "jobType" )
    @JsonSubTypes( value = {
        @JsonSubTypes.Type( value = AnalyticsJobParameters.class, name = "ANALYTICS_TABLE" ),
        @JsonSubTypes.Type( value = MonitoringJobParameters.class, name = "MONITORING" ),
        @JsonSubTypes.Type( value = PredictorJobParameters.class, name = "PREDICTOR" ),
        @JsonSubTypes.Type( value = PushAnalysisJobParameters.class, name = "PUSH_ANALYSIS" ),
        @JsonSubTypes.Type( value = SmsJobParameters.class, name = "SMS_SEND" ),
        @JsonSubTypes.Type( value = MetadataSyncJobParameters.class, name = "META_DATA_SYNC" ),
        @JsonSubTypes.Type( value = EventProgramsDataSynchronizationJobParameters.class, name = "EVENT_PROGRAMS_DATA_SYNC" ),
        @JsonSubTypes.Type( value = TrackerProgramsDataSynchronizationJobParameters.class, name = "TRACKER_PROGRAMS_DATA_SYNC" ),
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
    public boolean isContinuousExecution()
    {
        return continuousExecution;
    }

    public void setContinuousExecution( boolean continuousExecution )
    {
        this.continuousExecution = continuousExecution;
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
}
