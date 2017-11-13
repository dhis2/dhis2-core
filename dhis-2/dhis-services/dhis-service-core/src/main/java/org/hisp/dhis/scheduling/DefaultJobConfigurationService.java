package org.hisp.dhis.scheduling;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.scheduling.JobType.*;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private GenericNameableObjectStore<JobConfiguration> jobConfigurationStore;

    public void setJobConfigurationStore( GenericNameableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    private static final Log log = LogFactory.getLog( DefaultJobConfigurationService.class );

    private boolean scheduledBoot = true;

    /**
     * Reschedule old jobs.
     *
     * Port jobs from the old scheduler if the startup involves server upgrade
     *
     * @param contextRefreshedEvent context event
     */
    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        if ( scheduledBoot && systemSettingManager != null && currentUserService != null )
        {
            Date now = new Date();
            getAllJobConfigurations().forEach( (jobConfig -> {
                if ( !jobConfig.isContinuousExecution() && jobConfig.getNextExecutionTime().compareTo( now ) < 0 )
                {
                    jobConfig.setNextExecutionTime( null );
                    updateJobConfiguration( jobConfig );
                    schedulingManager.executeJob( jobConfig );
                }
                schedulingManager.scheduleJob( jobConfig );
            }) );

            ListMap<String, String> scheduledSystemSettings = (ListMap<String, String>) systemSettingManager.getSystemSetting( "keySchedTasks" );
            handleServerUpgrade( scheduledSystemSettings );

            scheduledBoot = false;
        }
    }

    /**
     * Method which ports the jobs in the system from the old scheduler to the new.
     * Collects all old jobs and adds them. Also adds default jobs.
     */
    private void handleServerUpgrade ( ListMap<String, String> scheduledSystemSettings )
    {
        if ( scheduledSystemSettings != null && scheduledSystemSettings.containsKey( "ported" ) ) {
            log.info( "Scheduler ported" );
            return;
        }

        if (scheduledSystemSettings != null) {
            log.info( "Porting old jobs" );
            JobConfiguration resourceTable = new JobConfiguration("Resource table", RESOURCE_TABLE, null, null, true, false );
            resourceTable.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulResourceTablesUpdate" ) );

            JobConfiguration analytics = new JobConfiguration("Analytics", ANALYTICS_TABLE, null, new AnalyticsJobParameters(null, Sets.newHashSet(), false), true, false );
            analytics.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulAnalyticsTablesUpdate" ) );

            JobConfiguration monitoring = new JobConfiguration("Monitoring", MONITORING, null, null, true, false );
            monitoring.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulMonitoring" ) );

            JobConfiguration dataSync = new JobConfiguration("Data synchronization", DATA_SYNC, null, null, true, false );
            dataSync.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulDataSynch" ) );

            JobConfiguration metadataSync = new JobConfiguration("Metadata sync", META_DATA_SYNC, null, null, true, false );
            metadataSync.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastMetaDataSyncSuccess" ) );

            JobConfiguration sendScheduledMessage = new JobConfiguration("Send scheduled messages", SEND_SCHEDULED_MESSAGE, null, null, true, false );

            JobConfiguration scheduledProgramNotifications = new JobConfiguration("Scheduled program notifications", PROGRAM_NOTIFICATIONS, null, null, true, false );
            scheduledProgramNotifications.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulScheduledProgramNotifications" ) );

            HashMap<String, JobConfiguration> standardJobs = new HashMap<String, JobConfiguration>()
            {{
                put( "resourceTable", resourceTable );
                put( "analytics", analytics );
                put( "monitoring", monitoring );
                put( "dataSynch", dataSync );
                put( "metadataSync", metadataSync );
                put( "sendScheduledMessage", sendScheduledMessage );
                put( "scheduledProgramNotifications", scheduledProgramNotifications );
            }};

            scheduledSystemSettings.forEach( ( cron, jobType ) -> jobType.forEach( type -> {
                for ( Map.Entry<String, JobConfiguration> entry : standardJobs.entrySet() )
                {
                    if ( type.startsWith( entry.getKey() ) )
                    {
                        JobConfiguration jobConfiguration = entry.getValue();

                        if ( jobConfiguration != null )
                        {
                            jobConfiguration.setCronExpression( cron );
                            jobConfiguration.setNextExecutionTime( null );
                            addJobConfiguration( jobConfiguration );

                            schedulingManager.scheduleJob( jobConfiguration );
                        }
                        break;
                    }

                    log.error( "Could not map job type '" + jobType + "' with cron '" + cron + "'" );
                }
            } ) );
        }

        String CRON_DAILY_2AM = "0 0 2 * * ?";
        String CRON_DAILY_7AM = "0 0 7 * * ?";

        log.info( "Setting up default jobs." );
        JobConfiguration fileResourceCleanUp = new JobConfiguration("File resource clean up", FILE_RESOURCE_CLEANUP, CRON_DAILY_2AM, null, true, false );

        JobConfiguration dataStatistics = new JobConfiguration("Data statistics", DATA_STATISTICS, CRON_DAILY_2AM, null, true, false );
        dataStatistics.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "lastSuccessfulDataStatistics" ) );

        JobConfiguration validationResultNotification = new JobConfiguration("Validation result notification", VALIDATION_RESULTS_NOTIFICATION, CRON_DAILY_7AM, null, true, false );
        JobConfiguration credentialsExpiryAlert = new JobConfiguration("Credentials expiry alert", CREDENTIALS_EXPIRY_ALERT, CRON_DAILY_2AM, null, true, false );
        // Dataset notification HH

        List<JobConfiguration> defaultJobs = Lists.newArrayList( fileResourceCleanUp, dataStatistics, validationResultNotification, credentialsExpiryAlert );
        addJobConfigurations( defaultJobs );
        schedulingManager.scheduleJobs( defaultJobs );

        ListMap<String, String> emptySystemSetting = new ListMap<>();
        emptySystemSetting.putValue("ported", "");

        log.info( "Porting to new scheduler finished. Setting system settings key 'keySchedTasks' to 'ported'.");
        systemSettingManager.saveSystemSetting( "keySchedTasks", emptySystemSetting);
    }

    @Override
    public int addJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.save( jobConfiguration );
        return jobConfiguration.getId( );
    }

    @Override
    public void addJobConfigurations( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( jobConfiguration -> jobConfigurationStore.save( jobConfiguration ));
    }

    @Override
    public int updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.update( jobConfiguration );
        return jobConfiguration.getId();
    }

    @Override
    public void deleteJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.delete( jobConfigurationStore.getByUid( jobConfiguration.getUid() ) );
    }

    @Override
    public JobConfiguration getJobConfigurationWithUid( String uid )
    {
        return jobConfigurationStore.getByUid( uid );
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

        for ( JobType jobType : values() )
        {
            Map<String, Property> jobParameters = Maps.newHashMap();

            if ( jobType.getKey() == null )
                continue;

            Class clazz = jobType.getClazz();
            if ( clazz == null )
            {
                propertyMap.put( jobType.name(), null );
                continue;
            }

            for ( Field field : clazz.getDeclaredFields() )
            {
                if ( Arrays.stream( field.getAnnotations() )
                    .anyMatch( f -> f.annotationType().getSimpleName().equals( "Property" ) ) )
                {
                    Property property = new Property( Primitives.wrap( field.getType() ), null, null );
                    property.setName( field.getName() );
                    property.setFieldName( prettyPrint( field.getName() ) );

                    String relativeApiElements = jobType.getRelativeApiElements() != null ?
                        jobType.getRelativeApiElements().get( field.getName() ) : "";
                    if ( relativeApiElements != null && !relativeApiElements.equals( "" ) )
                        property.setRelativeApiEndpoint( relativeApiElements );

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
        List<String> fieldStrings = Arrays.stream( field.split( "(?=[A-Z])" ) ).map( String::toLowerCase )
            .collect( Collectors.toList() );

        fieldStrings
            .set( 0, fieldStrings.get( 0 ).substring( 0, 1 ).toUpperCase() + fieldStrings.get( 0 ).substring( 1 ) );

        return String.join( " ", fieldStrings );
    }
}
