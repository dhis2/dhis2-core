package org.hisp.dhis.scheduling.Configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
public class JobConfiguration
    extends BaseIdentifiableObject
    implements IdentifiableObject
{
    private int codeSize = 10;

    private String name;
    private String key;
    private String cronExpression;
    private JobType jobType;

    // Used in JobService for sorting jobConfigurations based on cron expression
    private Date nextExecutionTime;

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

    public void setNextExecutionTime()
    {
        this.nextExecutionTime = new CronTrigger( cronExpression ).nextExecutionTime( new SimpleTriggerContext(  ) );
    }

    public void setKey( String key )
    {
        if ( !key.equals( "" ) && key.length() == codeSize )
        {
            this.key = key;
        } else
        {
            this.key = CodeGenerator.generateCode( codeSize );
        }
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

    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }



    @Override
    public int compareTo( IdentifiableObject jobConfiguration )
    {
        return nextExecutionTime.compareTo( ((JobConfiguration) jobConfiguration).getNextExecutionTime() );
    }
}
