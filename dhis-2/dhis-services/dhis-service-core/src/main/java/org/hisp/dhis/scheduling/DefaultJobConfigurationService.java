package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private JobConfigurationStore jobConfigurationStore;

    public void setJobConfigurationStore( JobConfigurationStore jobConfigurationStore )
    {
        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Autowired
    private SchedulingManager schedulingManager;

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        getAllJobConfigurations().forEach( (jobConfiguration -> schedulingManager.scheduleJob( jobConfiguration )) );
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

        jobConfigurations.forEach( JobConfiguration::setNextExecutionTime );
        Collections.sort( jobConfigurations );

        return jobConfigurations;
    }
}
