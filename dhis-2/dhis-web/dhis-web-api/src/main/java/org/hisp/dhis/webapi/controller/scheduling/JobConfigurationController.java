package org.hisp.dhis.webapi.controller.scheduling;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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

    @Override
    @RequestMapping( method = RequestMethod.POST, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    void postJsonObject( HttpServletRequest request, HttpServletResponse response) throws IOException {
        JobConfiguration jobConfiguration = renderService.fromJson( request.getInputStream(), getEntityClass() );
        jobConfiguration.setJobId( new JobId( JobType.valueOf( jobConfiguration.getJobType().toString() ), currentUserService.getCurrentUser().getUid() ) );

        List<ErrorReport> errorReports = jobConfigurationService.validate( jobConfiguration );

        ImportReport importReport = new ImportReport();
        if ( errorReports.size() != 0 ) {
            webMessageService.send(  WebMessageUtils.errorReports( errorReports ), response, request );
        } else {
            jobConfigurationService.addJobConfiguration( jobConfiguration );
            webMessageService.send(  WebMessageUtils.objectReport( importReport ), response, request );
        }
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.OK )
    public void deleteObject( @PathVariable( "uid" ) String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        jobConfigurationService.deleteJobConfiguration( uid );
        webMessageService.send(  WebMessageUtils.objectReport( new ImportReport() ), response, request );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        JobConfiguration jobConfiguration = renderService.fromJson( request.getInputStream(), getEntityClass() );
        jobConfiguration.setJobId( new JobId( JobType.valueOf( jobConfiguration.getJobType().toString() ), currentUserService.getCurrentUser().getUid() ) );

        List<ErrorReport> errorReports = jobConfigurationService.putJobConfiguration( jobConfiguration, pvUid );

        ImportReport importReport = new ImportReport();
        if ( errorReports.size() != 0 ) {
            webMessageService.send(  WebMessageUtils.errorReports( errorReports ), response, request );
        } else {
            webMessageService.send(  WebMessageUtils.objectReport( importReport ), response, request );
        }

    }

    // Supportive API endpoints
    @RequestMapping( value = "/jobTypesExtended", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    Map<String, Map<String, Property>> getJobTypesExtended()
    {
        return jobConfigurationService.getJobParametersSchema();
    }
}
