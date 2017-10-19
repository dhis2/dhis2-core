package org.hisp.dhis.scheduling;

import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    @Autowired
    private SchedulingManager schedulingManager;

    private GenericNameableObjectStore<JobConfiguration> jobConfigurationStore;

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
        return jobConfiguration.getId();
    }

    @Override
    public int updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        jobConfigurationStore.update( jobConfiguration );
        return jobConfiguration.getId();
    }

    @Override
    public void deleteJobConfiguration( int jobId )
    {
        jobConfigurationStore.delete( jobConfigurationStore.get( jobId ));
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
}
