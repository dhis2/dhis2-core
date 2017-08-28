package org.hisp.dhis.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.joda.time.DateTime;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
public class DefaultJob
    extends BaseIdentifiableObject
    implements Job
{
    private static int codeSize = 10;

    private String name;
    private String key;
    private JobType jobType;

    private String cronExpression;
    private CronTrigger cronTrigger;
    private long delay;
    private Date nextExecutionTime;

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
        this.delay = -1;
        this.status = JobStatus.SCHEDULED;
        this.key = CodeGenerator.generateCode( codeSize );
    }

    public String toString()
    {
        return "Name: " + name + ", job type: " + jobType.name() + ", cronExpression: " + cronExpression +
            ", status: " + status;
    }


    // Getters and setters
    public void setDelay( long delay )
    {
        this.delay = delay;
    }

    public void setStatus( JobStatus status )
    {
        if( status == null) this.status = JobStatus.SCHEDULED;
        else this.status = status;
    }

    public void setKey( String key )
    {
        if( key.length() == 0) this.key = CodeGenerator.generateCode( codeSize );
        else if( key.length() == codeSize ) this.key = key;
        else {
            log.error( "Given key has incorrect length, " + codeSize + " characters is the expected key length. A key is auto-generated for the job" );
            this.key = CodeGenerator.generateCode( codeSize );
        }
    }

    public void setNextExecutionTime()
    {
        this.cronTrigger = new CronTrigger( cronExpression );

        SimpleTriggerContext triggerContext = new SimpleTriggerContext( null, null, new Date() );
        this.nextExecutionTime = this.cronTrigger.nextExecutionTime(triggerContext);
    }

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
    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }

    public long getDelay()
    {
        return delay;
    }

    @Override
    public Runnable getRunnable()
    {
        return jobConfiguration.getRunnable();
    }

    // Compare
    @Override
    public int compareTo( IdentifiableObject object )
    {
        return nextExecutionTime.compareTo( ((DefaultJob) object).nextExecutionTime );
    }
}

