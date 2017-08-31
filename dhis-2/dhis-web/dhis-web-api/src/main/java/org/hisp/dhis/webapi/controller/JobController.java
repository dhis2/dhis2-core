package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Henning Håkonsen
 */
public class JobController
    extends AbstractCrudController<JobConfiguration>
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    @Autowired
    private SchedulingManager schedulingManager;
}
