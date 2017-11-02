package org.hisp.dhis.scheduling;

import org.hisp.dhis.schema.Property;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Map;

/**
 * Simple service for {@link JobConfiguration} objects.
 *
 * @author Henning Håkonsen
 */
public interface JobConfigurationService
{
    String ID = JobConfiguration.class.getName();

    /**
     * This method is called when the context is ready or updated. Since this can be called several times, the schedulingManager checks if the job is already scheduled.
     * The method also checks if there are jobs which should have been run while the system was down. If the server crashed or something unexpected happened, we want to rerun these jobs.
     *
     * @param event the new context
     */
    @EventListener
    void handleContextRefresh( ContextRefreshedEvent event);

    /**
     * Add a job configuration
     *
     * @param jobConfiguration the job configuration to be added
     * @return id
     */
    int addJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Add a collection of job configurations
     * @param jobConfigurations the job configurations to add
     */
    void addJobConfigurations( List<JobConfiguration> jobConfigurations );

    /**
     * Update an existing job configuration
     *
     * @param jobConfiguration the job configuration to be added
     * @return id
     */
    int updateJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Delete a job configuration
     *
     * @param jobConfiguration the id of the job configuration to be deleted
     */
    void deleteJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Get job configuration for given id
     *
     * @param jobId id for job configuration
     * @return Job configuration
     */
    JobConfiguration getJobConfiguration( int jobId );

    /**
     * Get a job configuration for given uid
     *
     * @param uid uid to search for
     * @return job configuration
     */
    JobConfiguration getJobConfigurationWithUid( String uid );

    /**
     * Get all job configurations
     *
     * @return list of all job configurations in the system
     */
    List<JobConfiguration> getAllJobConfigurations( );

    /**
     * Get a sorted list of all job configurations based on cron expressions
     * and the current time
     *
     * @return list of all job configurations in the system(sorted)
     */
    List<JobConfiguration> getAllJobConfigurationsSorted( );

    /**
     * Get a map of parameter classes with appropriate properties
     * This can be used for a frontend app or for other appropriate applications which needs information about the jobs
     * in the system.
     *
     * It uses {@link JobType}.
     *
     * @return map with parameters classes
     */
    Map<String, Map<String, Property>> getJobParametersSchema( );
}
