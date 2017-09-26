package org.hisp.dhis.webapi.controller.scheduling;

import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple controller for API endpoints
 *
 * @author Henning Håkonsen
 */
@Controller
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
    @Autowired
    private FieldFilterService fieldFilterService;

    @RequestMapping( value = "/jobTypes", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    List<JobType> getJobTypes()
    {
        return Arrays.asList( JobType.values() );
    }
}
