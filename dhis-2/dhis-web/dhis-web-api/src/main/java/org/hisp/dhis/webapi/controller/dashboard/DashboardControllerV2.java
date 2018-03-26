package org.hisp.dhis.webapi.controller.dashboard;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.V29, DhisApiVersion.DEFAULT } )
@RequestMapping( value = DashboardSchemaDescriptor.API_ENDPOINT )
public class DashboardControllerV2
    extends AbstractCrudController<Dashboard>
{
    @Autowired
    private DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/q/{query}", method = RequestMethod.GET )
    public @ResponseBody DashboardSearchResult search( @PathVariable String query, @RequestParam( required = false ) Set<DashboardItemType> max )
    {
        return dashboardService.search( query, max );
    }

    @RequestMapping( value = "/q", method = RequestMethod.GET )
    public @ResponseBody DashboardSearchResult searchNoFilter( @RequestParam( required = false ) Set<DashboardItemType> max )
    {
        return dashboardService.search( max );
    }

    // -------------------------------------------------------------------------
    // Metadata with dependencies
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/metadata", method = RequestMethod.GET )
    public @ResponseBody RootNode getDataSetWithDependencies( @PathVariable( "uid" ) String dashboardId, HttpServletResponse response )
        throws WebMessageException, IOException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardId );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard not found for uid: " + dashboardId ) );
        }

        return exportService.getMetadataWithDependenciesAsNode( dashboard );
    }
}
