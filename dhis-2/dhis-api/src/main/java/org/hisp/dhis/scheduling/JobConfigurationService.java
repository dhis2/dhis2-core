package org.hisp.dhis.scheduling;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Property;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
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
     * Update an existing job configuration
     *
     * @param jobConfiguration the job configuration to be added
     * @return id
     */
    int updateJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Delete a job configuration
     *
     * @param uid the id of the job configuration to be deleted
     */
    void deleteJobConfiguration( String uid );

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
     * Get a list of job configurations with specific cron expression
     *
     * @param cron cron expression to search for
     * @return list of job configuration
     */
    List<JobConfiguration> getJobConfigurationsForCron( String cron );

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
     *
     * @return map with parameters classes
     */
    Map<String, Map<String, Property>> getJobParametersSchema();

    JobConfiguration create( HashMap<String, String> requestJobConfiguration );

    List<ErrorReport> validate( JobConfiguration jobConfiguration );
}
