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
import org.hisp.dhis.scheduling.*;
import org.hisp.dhis.system.scheduling.SpringScheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.threeten.bp.Duration;
import org.threeten.bp.ZonedDateTime;

import java.util.*;

import static com.cronutils.model.CronType.QUARTZ;

/**
 * @author Henning Håkonsen
 */
public class JobObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    @Autowired
    private CurrentUserService currentUserService;

    private SchedulingManager schedulingManager;

    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private List<ErrorReport> validateCronForJobType( JobConfiguration jobConfiguration, CronParser parser )
    {
        List<ErrorReport> errorReports = new ArrayList<>(  );

        // Validate that the given interval is allowed for the given JobType
        ZonedDateTime now = null;
        Duration timeToNextExecution = null;
        if ( !jobConfiguration.isContinuousExecution() )
        {
            now = ZonedDateTime.now();
            ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(jobConfiguration.getCronExpression()));
            Duration timeFromLastExecution =  executionTime.timeFromLastExecution(now).get();
            timeToNextExecution = executionTime.timeToNextExecution(now).get();

            long timeIntervalInSeconds = timeFromLastExecution.getSeconds() + timeToNextExecution.getSeconds();

            if( timeIntervalInSeconds < jobConfiguration.getJobType().getMinimumFrequencyInSeconds() ) {
                errorReports.add( new ErrorReport( JobConfiguration.class, new ErrorMessage( ErrorCode.E4014, timeIntervalInSeconds, jobConfiguration.getJobType().getMinimumFrequencyInSeconds() ) ) );
            }
        }

        // Make list of all jobs for each job type
        Map<JobType, List<JobConfiguration>> jobConfigurationForJobTypes = new HashMap<>(  );

        jobConfigurationService.getAllJobConfigurations().stream()
            .filter( configuration -> !Objects.equals( configuration.getUid(), jobConfiguration.getUid() ) )
            .forEach( configuration -> {
                List<JobConfiguration> jobConfigurationList = new ArrayList<>();
                List<JobConfiguration> oldList = jobConfigurationForJobTypes.get( configuration.getJobType() );
                if ( oldList != null )
                    jobConfigurationList.addAll( oldList );
                jobConfigurationList.add( configuration );
                jobConfigurationForJobTypes.put( configuration.getJobType(), jobConfigurationList );
            } );

        /*
         *  Validate that there are no other jobs of the same job type which are scheduled within the allowed frequency range.
         *  I.E a job scheduled for 15:40 on monday is not allowed if a job already is scheduled for 15:20 on monday if the
         *  minimum frequency is 30 minutes
         *
         *  Also check if the job is trying to run continuously while other jobs of the same type is running continuously - this should not be allowed
         */
        List<JobConfiguration> listForJobType = jobConfigurationForJobTypes.get( jobConfiguration.getJobType() );

        if ( listForJobType != null )
        {
            for ( JobConfiguration jobConfig : listForJobType )
            {
                if ( jobConfiguration.isContinuousExecution() ) {
                    if ( jobConfig.isContinuousExecution() ) {
                        errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E4016 ) );
                    }
                } else {
                    if ( !jobConfig.isContinuousExecution() ) {
                        if ( jobConfig.getCronExpression().equals( jobConfiguration.getCronExpression() ) ) {
                            errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E4013 ) );
                        }

                        Duration jobConfigTimeToNextExecution = ExecutionTime.forCron(parser.parse(jobConfig.getCronExpression())).timeToNextExecution( now ).get();

                        if ( Math.abs(((timeToNextExecution.getSeconds() - jobConfigTimeToNextExecution.getSeconds()))) < jobConfig.getJobType().getMinimumFrequencyInSeconds() )
                        {
                            errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E4015, jobConfig.getName() ) );
                        }
                    }
                }
            }
        }

        return errorReports;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errorReports = new ArrayList<>(  );
        JobConfiguration jobConfiguration = (JobConfiguration) object;

        // validate the cron expression
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser( cronDefinition );
        Cron quartzCron = parser.parse( jobConfiguration.getCronExpression() );

        quartzCron.validate();

        CronDescriptor cronDescriptor = CronDescriptor.instance( Locale.UK);

        // Validate cron expression with relation to all other jobs
        errorReports.addAll( validateCronForJobType( jobConfiguration, parser ) );
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
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) object;

        jobConfiguration.setJobId( new JobId( JobCategory.valueOf( jobConfiguration.getJobType().toString() ), currentUserService.getCurrentUser().getUid() ) );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( object ) )
        {
            return;
        }

        JobConfiguration jobConfiguration = (JobConfiguration) object;

        jobConfiguration.setJobId( new JobId( JobCategory.valueOf( jobConfiguration.getJobType().toString() ), currentUserService.getCurrentUser().getUid() ) );

        schedulingManager.stopJob( (JobConfiguration) persistedObject );
        sessionFactory.getCurrentSession().update( persistedObject );
    }

    @Override
    public <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle )
    {
        if ( !JobConfiguration.class.isInstance( persistedObject ) )
        {
            return;
        }

        schedulingManager.stopJob( (JobConfiguration) persistedObject );
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
