package org.hisp.dhis.scheduling;

import org.hisp.dhis.common.GenericNameableObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private GenericNameableObjectStore<JobConfiguration> jobConfigurationStore;

    public void setJobConfigurationStore( GenericNameableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Autowired
    private SchedulingManager schedulingManager;

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
}
