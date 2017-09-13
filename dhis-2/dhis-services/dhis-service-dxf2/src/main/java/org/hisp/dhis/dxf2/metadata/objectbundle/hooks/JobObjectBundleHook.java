package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.system.scheduling.SpringScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.threeten.bp.Duration;
import org.threeten.bp.ZonedDateTime;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.cronutils.model.CronType.QUARTZ;

/**
 * @author Henning Håkonsen
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    private SchedulingManager schedulingManager;

    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errorReports;
        JobConfiguration jobConfiguration = (JobConfiguration) object;

        // validate the cron expression
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser( cronDefinition );
        Cron quartzCron = parser.parse( jobConfiguration.getCronExpression() );

        quartzCron.validate();

        CronDescriptor cronDescriptor = CronDescriptor.instance( Locale.UK);

        // Validate that no other jobs of the same JobType has the same cron expression
        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();
        errorReports = jobConfigurations.stream()
            .filter( jobConfiguration1 -> !jobConfiguration.getUid().equals( jobConfiguration1.getUid() ) )
            .filter( jobConfiguration1 -> jobConfiguration.getJobType() == jobConfiguration1.getJobType() )
            .filter( jobConfiguration1 -> jobConfiguration.getCronExpression().equals( jobConfiguration1.getCronExpression() ) )
            .map( jobConfiguration1 -> new ErrorReport( JobConfiguration.class, ErrorCode.E4013 ) )
            .collect( Collectors.toList() );

        // Validate that the given interval is allowed for the given JobType
        ZonedDateTime now = ZonedDateTime.now();
        ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(jobConfiguration.getCronExpression()));
        Duration timeFromLastExecution = executionTime.timeFromLastExecution(now).get();
        Duration timeToNextExecution = executionTime.timeToNextExecution(now).get();

        long timeIntervalInSeconds = timeFromLastExecution.getSeconds() + timeToNextExecution.getSeconds();

        if( timeIntervalInSeconds < jobConfiguration.getJobType().getMinimumFrequencyInSeconds() ) {
            errorReports.add( new ErrorReport( JobConfiguration.class, new ErrorMessage( ErrorCode.E4014, timeIntervalInSeconds, jobConfiguration.getJobType().getMinimumFrequencyInSeconds() ) ) );
        }

        if( errorReports.size() == 0 )
        {
            log.info( "Validation of '" + jobConfiguration.getName() + "' succeeded with cron description '" + cronDescriptor.describe( quartzCron ) + "'" );
        } else
        {
            log.info( "Validation of '" + jobConfiguration.getName() + "' failed." );
        }

        return errorReports;
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        schedulingManager.stopJob( persistedObject.getUid() );
        sessionFactory.getCurrentSession().update( persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        schedulingManager.stopJob( persistedObject.getUid() );
        sessionFactory.getCurrentSession().delete( persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        schedulingManager.scheduleJob( (JobConfiguration) persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        schedulingManager.scheduleJob( (JobConfiguration) persistedObject );
    }
}
