package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemShape;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.DashboardItemSchemaDescriptor;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DashboardItemSchemaDescriptor.API_ENDPOINT )
public class DashboardItemController
    extends AbstractCrudController<DashboardItem>
{
    @Autowired
    private DashboardService dashboardService;

    @Override
    protected List<DashboardItem> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        List<DashboardItem> entityList;

        if ( options.hasPaging() )
        {
            int count = manager.getCount( getEntityClass() );

            Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
            metadata.setPager( pager );

            entityList = Lists.newArrayList( manager.getBetween( getEntityClass(), pager.getOffset(), pager.getPageSize() ) );
        }
        else
        {
            entityList = Lists.newArrayList( manager.getAll( getEntityClass() ) );
        }

        return entityList;
    }

    @RequestMapping( value = "/{uid}/shape/{shape}", method = RequestMethod.PUT )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void putDashboardItemShape( @PathVariable String uid, @PathVariable DashboardItemShape shape,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        DashboardItem item = dashboardService.getDashboardItem( uid );

        if ( item == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard item does not exist: " + uid ) );
        }

        Dashboard dashboard = dashboardService.getDashboardFromDashboardItem( item );

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        item.setShape( shape );

        dashboardService.updateDashboardItem( item );
    }
}
