package org.hisp.dhis.scheduling;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.system.scheduling.SpringScheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.cronutils.model.CronType.QUARTZ;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private GenericNameableObjectStore<JobConfiguration> jobConfigurationStore;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JobConfigurationService jobConfigurationService;

    @Autowired
    private CurrentUserService currentUserService;

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    public void setJobConfigurationStore( GenericNameableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        this.jobConfigurationStore = jobConfigurationStore;
    }

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        Date now = new Date();
        getAllJobConfigurations().forEach( (jobConfiguration -> {
            if( !jobConfiguration.isContinuousExecution() && jobConfiguration.getNextExecutionTime().compareTo( now ) < 0 ) {
                jobConfiguration.setNextExecutionTime( null );
                updateJobConfiguration( jobConfiguration );
                schedulingManager.executeJob( jobConfiguration );
            }
            schedulingManager.scheduleJob( jobConfiguration );
        }) );
    }

    @Override
    public int addJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.save( jobConfiguration );
        schedulingManager.scheduleJob( jobConfiguration );
        return jobConfiguration.getId();
    }

    @Override
    public int updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.update( jobConfiguration );
        return jobConfiguration.getId();
    }

    public List<ErrorReport> putJobConfiguration( JobConfiguration jobConfiguration, String puid ) {
        System.out.println("put");
        // Temporarily set same uid for validation purposes
        jobConfiguration.setUid( puid );
        List<ErrorReport> errorReports = validate( jobConfiguration );

        JobConfiguration oldJobConfiguration = getJobConfigurationWithUid( puid );
        if (oldJobConfiguration == null)
        {
            errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7002 ) );
        }

        if( errorReports.size() > 0 )
        {
            return errorReports;
        }

        oldJobConfiguration.setName( jobConfiguration.getName() );
        oldJobConfiguration.setCronExpression( jobConfiguration.getCronExpression() );
        oldJobConfiguration.setJobType( jobConfiguration.getJobType() );
        oldJobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        oldJobConfiguration.setJobParameters( jobConfiguration.getJobParameters() );
        oldJobConfiguration.setEnabled( jobConfiguration.getEnabled() );
        oldJobConfiguration.setContinuousExecution( jobConfiguration.isContinuousExecution() );
        oldJobConfiguration.setNextExecutionTime( jobConfiguration.getNextExecutionTime() );
        oldJobConfiguration.setJobId( jobConfiguration.getJobId() );

        schedulingManager.refreshJob( oldJobConfiguration );
        updateJobConfiguration( oldJobConfiguration );

        return errorReports;
    }

    @Override
    public void deleteJobConfiguration( String uid )
    {
        schedulingManager.stopJob( jobConfigurationStore.getByUid( uid ) );
        jobConfigurationStore.delete( jobConfigurationStore.getByUid( uid ) );
    }

    @Override
    public JobConfiguration getJobConfigurationWithUid( String uid )
    {
        return jobConfigurationStore.getByUid( uid );
    }

    @Override
    public List<JobConfiguration> getJobConfigurationsForCron( String cron )
    {
        return getAllJobConfigurations().stream().filter( jobConfiguration -> jobConfiguration.getCronExpression().equals( cron ) ).collect( Collectors
            .toList());
    }

    @Override
    public JobConfiguration getJobConfiguration( int jobId )
    {
        return jobConfigurationStore.get( jobId );
    }

    @Override
    public List<JobConfiguration> getAllJobConfigurations()
    {
        return jobConfigurationStore.getAll();
    }

    @Override
    public List<JobConfiguration> getAllJobConfigurationsSorted()
    {
        List<JobConfiguration> jobConfigurations = getAllJobConfigurations();

        Collections.sort( jobConfigurations );

        return jobConfigurations;
    }

    @Override
    public Map<String, Map<String, Property>> getJobParametersSchema()
    {
        Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

        for ( JobType jobType : JobType.values() )
        {
            Map<String, Property> jobParameters = Maps.newHashMap();

            Class clazz = jobType.getClazz();
            if ( clazz == null )
            {
                propertyMap.put( jobType.name(), null );
                continue;
            }

            for ( Field field : clazz.getDeclaredFields() )
            {
                if( Arrays.stream( field.getAnnotations() ).anyMatch(f -> f.annotationType().getSimpleName().equals( "Property" ) ) )
                {
                    Property property = new Property( Primitives.wrap( field.getType() ), null, null );
                    property.setName( field.getName() );
                    property.setFieldName( prettyPrint( field.getName() ) );

                    String relativeApiElements = jobType.getRelativeApiElements() != null ? jobType.getRelativeApiElements().get( field.getName() ) : "";
                    if( relativeApiElements != null && !relativeApiElements.equals( "" ) ) property.setRelativeApiEndpoint( relativeApiElements );

                    if ( Collection.class.isAssignableFrom( field.getType() ) )
                    {
                        property = new NodePropertyIntrospectorService().setPropertyIfCollection( property, field, clazz );
                    }

                    jobParameters.put( property.getName(), property );
                }
            }
            propertyMap.put( jobType.name(), jobParameters );
        }

        return propertyMap;
    }

    private String prettyPrint( String field )
    {
        List<String> fieldStrings = Arrays.stream(field.split("(?=[A-Z])")).map(String::toLowerCase).collect(Collectors.toList());

        fieldStrings.set(0, fieldStrings.get(0).substring(0, 1).toUpperCase() + fieldStrings.get(0).substring(1));

        return String.join(" ", fieldStrings);
    }

    private List<ErrorReport> validateCronForJobType( JobConfiguration jobConfiguration )
    {
        List<ErrorReport> errorReports = new ArrayList<>(  );

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
         *  Validate that there are no other jobs of the same job type which are scheduled with the same cron.
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
                        errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7001 ) );
                    }
                } else {
                    if ( jobConfig.getCronExpression().equals(jobConfiguration.getCronExpression() ) ) {
                        errorReports.add( new ErrorReport( JobConfiguration.class, ErrorCode.E7000 ) );
                    }
                }
            }
        }

        return errorReports;
    }

    public List<ErrorReport> validate( JobConfiguration jobConfiguration )
    {
        List<ErrorReport> errorReports = new ArrayList<>(  );

        // validate the cron expression
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser( cronDefinition );
        Cron quartzCron = parser.parse( jobConfiguration.getCronExpression() );

        quartzCron.validate();

        CronDescriptor cronDescriptor = CronDescriptor.instance( Locale.UK );

        // Validate cron expression with relation to all other jobs
        errorReports.addAll( validateCronForJobType( jobConfiguration ) );
        if( errorReports.size() == 0 )
        {
            log.info( "Validation of '" + jobConfiguration.getName() + "' succeeded with cron description '" + cronDescriptor.describe( quartzCron ) + "'" );
        } else
        {
            log.info( "Validation of '" + jobConfiguration.getName() + "' failed." );
        }

        return errorReports;
    }

    public JobConfiguration create( HashMap<String, String> requestJobConfiguration )
    {
        JobConfiguration jobConfiguration;
        if ( requestJobConfiguration != null ) {
            jobConfiguration = mapper.convertValue( requestJobConfiguration, JobConfiguration.class );
        } else
        {
            return null;
        }

        jobConfiguration.setJobId( new JobId( JobType.valueOf( jobConfiguration.getJobType().toString() ), currentUserService.getCurrentUser().getUid() ) );

        return jobConfiguration;
    }

    /*@Override
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
    }*/
}
