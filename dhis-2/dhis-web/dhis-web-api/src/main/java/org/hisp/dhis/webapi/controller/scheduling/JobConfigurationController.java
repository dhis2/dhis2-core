package org.hisp.dhis.webapi.controller.scheduling;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple controller for API endpoints
 *
 * @author Henning Håkonsen
 */
@RestController
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    @RequestMapping( value = "/jobTypesExtended", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    Map<String, Map<String, Property>> getJobTypesExtended()
    {
        return jobConfigurationService.getJobParametersSchema();
    }
}
