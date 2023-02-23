/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.scheduling;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.schema.Property;

/**
 * Simple service for {@link JobConfiguration} objects.
 *
 * @author Henning Håkonsen
 */
public interface JobConfigurationService
{
    String ID = JobConfiguration.class.getName();

    /**
     * Add a job configuration
     *
     * @param jobConfiguration the job configuration to be added
     * @return id
     */
    long addJobConfiguration( JobConfiguration jobConfiguration );

    /**
     * Add a collection of job configurations
     *
     * @param jobConfigurations the job configurations to add
     */
    void addJobConfigurations( List<JobConfiguration> jobConfigurations );

    /**
     * Update an existing job configuration
     *
     * @param jobConfiguration the job configuration to be added
     * @return id
     */
    long updateJobConfiguration( JobConfiguration jobConfiguration );

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
    JobConfiguration getJobConfiguration( long jobId );

    /**
     * Get a job configuration for given uid
     *
     * @param uid uid to search for
     * @return job configuration
     */
    JobConfiguration getJobConfigurationByUid( String uid );

    /**
     * Get all job configurations
     *
     * @return list of all job configurations in the system
     */
    List<JobConfiguration> getAllJobConfigurations();

    /**
     * Get all job configurations for a specific {@link JobType}.
     *
     * @param type to select
     * @return all configuration for the given {@link JobType}
     */
    List<JobConfiguration> getJobConfigurations( JobType type );

    /**
     * Get a map of parameter classes with appropriate properties This can be
     * used for a frontend app or for other appropriate applications which needs
     * information about the jobs in the system.
     * <p>
     * It uses {@link JobType}.
     *
     * @return map with parameters classes
     */
    Map<String, Map<String, Property>> getJobParametersSchema();

    /**
     * Returns a list of all configurable and available job types.
     *
     * @return a list of {@link JobTypeInfo}.
     */
    List<JobTypeInfo> getJobTypeInfo();

    /**
     * Update the state of the jobConfiguration.
     *
     * @param jobConfiguration
     */
    void refreshScheduling( JobConfiguration jobConfiguration );
}
