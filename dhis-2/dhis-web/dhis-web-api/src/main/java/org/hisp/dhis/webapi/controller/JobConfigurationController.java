package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationStore;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
    @Autowired
    private JobConfigurationStore jobConfigurationStore;

    @Override
    public RootNode getObject( String pvUid, Map<String, String> rpParameters, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        JobConfiguration testJobConfiguration = new JobConfiguration( "test", JobType.ANALYTICS_TABLE, "test" );//new AnalyticsJobConfiguration( "analyticsJob", JobType.ANALYTICS_TABLE, "test", 2, null, null, false );

        jobConfigurationStore.save( testJobConfiguration );

        return null;
    }
}
