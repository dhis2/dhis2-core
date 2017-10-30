package org.hisp.dhis.webapi.controller.scheduling;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
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

    @Autowired
    private WebMessageService webMessageService;

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject(HttpServletRequest request, HttpServletResponse response )
            throws Exception
    {
    }

    @RequestMapping( value = "/post", method = RequestMethod.POST, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    void postJsonObject(@RequestBody HashMap<String, String> requestData, HttpServletRequest request, HttpServletResponse response) {
        JobConfiguration jobConfiguration = jobConfigurationService.create( requestData );

        List<ErrorReport> errorReports = jobConfigurationService.validate( jobConfiguration );

        ImportReport importReport = new ImportReport();
        if ( errorReports.size() != 0 ) {
            webMessageService.send(  WebMessageUtils.errorReports( errorReports ), response, request );
        } else {
            jobConfigurationService.addJobConfiguration( jobConfiguration );
            webMessageService.send(  WebMessageUtils.objectReport( importReport ), response, request );
        }
    }

    @RequestMapping( value = "/jobTypesExtended", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    Map<String, Map<String, Property>> getJobTypesExtended()
    {
        return jobConfigurationService.getJobParametersSchema();
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.OK )
    public void deleteObject( @PathVariable( "uid" ) String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        jobConfigurationService.deleteJobConfiguration( uid );
        webMessageService.send(  WebMessageUtils.objectReport( new ImportReport() ), response, request );
    }

    @RequestMapping( value = "/put/{uid}", method = RequestMethod.PUT )
    public void putObject( @RequestBody HashMap<String, String> requestData, @PathVariable( "uid" ) String puid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        JobConfiguration jobConfiguration = jobConfigurationService.create( requestData );

        List<ErrorReport> errorReports = jobConfigurationService.putJobConfiguration( jobConfiguration, puid );

        ImportReport importReport = new ImportReport();
        if ( errorReports.size() != 0 ) {
            webMessageService.send(  WebMessageUtils.errorReports( errorReports ), response, request );
        } else {
            webMessageService.send(  WebMessageUtils.objectReport( importReport ), response, request );
        }

    }

}
