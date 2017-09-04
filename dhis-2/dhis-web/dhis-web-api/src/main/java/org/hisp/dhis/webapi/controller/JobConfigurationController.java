package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Henning Håkonsen
 */
@Controller
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
}
