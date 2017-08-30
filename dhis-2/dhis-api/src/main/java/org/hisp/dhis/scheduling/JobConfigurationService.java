package org.hisp.dhis.scheduling;

import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

import java.util.List;

/**
 * @author Henning Håkonsen
 */
public interface JobConfigurationService
{
    String ID = OrganisationUnitService.class.getName();

    /**
     * Add a job configuration
     * @param jobConfiguration the jobconfiguration to be added
     * @return id
     */
    int addJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Update an existing job configuration
     * @param jobConfiguration the jobconfiguration to be added
     * @return id
     */
    int updateJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * deleta a job configuration
     * @param jobId the id of the job configuration to be deleted
     */
    void deleteJobConfiguration( int jobId );

    /**
     * Get job configuration for given key
     * @param jobId id for job configuration
     * @return Job configuration
     */
    JobConfiguration getJobConfiguration( int jobId );

    /**
     * Get all job configurations
     * @return list of all job configurations in the system
     */
    List<JobConfiguration> getAllJobConfigurations( );

    /**
     * Get a sorted list of all job configurations based on cron expressions
     * and the current time
     * @return list of all job configurations in the system(sorted)
     */
    List<JobConfiguration> getAllJobConfigurationsSorted( );
}
