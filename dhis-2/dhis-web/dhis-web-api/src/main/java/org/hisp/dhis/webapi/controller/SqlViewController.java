/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.SqlViewSchemaDescriptor;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewQuery;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SqlViewSchemaDescriptor.API_ENDPOINT )
public class SqlViewController
    extends AbstractCrudController<SqlView>
{
    @Autowired
    private SqlViewService sqlViewService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private NodeService nodeService;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/data", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getViewJson( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws WebMessageException
    {
        SqlView sqlView = validateView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, sqlView.getCacheStrategy() );

        return buildResponse( sqlView, query );
    }

    @RequestMapping( value = "/{uid}/data.xml", method = RequestMethod.GET )
    public @ResponseBody RootNode getViewXml( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws WebMessageException
    {
        SqlView sqlView = validateView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, sqlView.getCacheStrategy() );

        return buildResponse( sqlView, query );
    }

    @RequestMapping( value = "/{uid}/data.csv", method = RequestMethod.GET )
    public void getViewCsv( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, SqlView.getCriteria( criteria ), SqlView.getCriteria( var ),
            filters, fields );

        String filename = CodecUtils.filenameEncode( grid.getTitle() ) + ".csv";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, sqlView.getCacheStrategy(), filename,
            true );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{uid}/data.xls", method = RequestMethod.GET )
    public void getViewXls( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, SqlView.getCriteria( criteria ), SqlView.getCriteria( var ),
            filters, fields );

        String filename = CodecUtils.filenameEncode( grid.getTitle() ) + ".xls";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, sqlView.getCacheStrategy(), filename,
            true );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/{uid}/data.html", method = RequestMethod.GET )
    public void getViewHtml( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService
            .getSqlViewGrid( sqlView, SqlView.getCriteria( criteria ), SqlView.getCriteria( var ), filters, fields );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, sqlView.getCacheStrategy() );

        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{uid}/data.html+css", method = RequestMethod.GET )
    public void getViewHtmlCss( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService
            .getSqlViewGrid( sqlView, SqlView.getCriteria( criteria ), SqlView.getCriteria( var ), filters, fields );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, sqlView.getCacheStrategy() );

        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{uid}/data.pdf", method = RequestMethod.GET )
    public void getViewPdf( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, SqlView.getCriteria( criteria ), SqlView.getCriteria( var ),
            filters, fields );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, sqlView.getCacheStrategy() );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/execute", method = RequestMethod.POST )
    public void executeView( @PathVariable( "uid" ) String uid, @RequestParam( required = false ) Set<String> var,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "SQL view does not exist: " + uid ) );
        }

        if ( sqlView.isQuery() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "SQL view is a query, no view to create" ) );
        }

        String result = sqlViewService.createViewTable( sqlView );

        if ( result != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( result ) );
        }
        else
        {
            response.addHeader( "Location", SqlViewSchemaDescriptor.API_ENDPOINT + "/" + sqlView.getUid() );
            webMessageService.send( WebMessageUtils.created( "SQL view created" ), response, request );
        }
    }

    @RequestMapping( value = "/{uid}/refresh", method = RequestMethod.POST )
    public void refreshMaterializedView( @PathVariable( "uid" ) String uid,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "SQL view does not exist: " + uid ) );
        }

        boolean result = sqlViewService.refreshMaterializedView( sqlView );

        if ( !result )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "View could not be refreshed" ) );
        }
        else
        {
            webMessageService.send( WebMessageUtils.ok( "Materialized view refreshed" ), response, request );
        }
    }

    private SqlView validateView( String uid )
        throws WebMessageException
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "SQL view does not exist: " + uid ) );
        }

        return sqlView;
    }

    private RootNode buildResponse( SqlView sqlView, SqlViewQuery query )
        throws WebMessageException
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, SqlView.getCriteria( query.getCriteria() ),
            SqlView.getCriteria( query.getVar() ), filters, fields );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( !query.isSkipPaging() )
        {
            query.setTotal( grid.getHeight() );
            grid.limitGrid( (query.getPage() - 1) * query.getPageSize(),
                Integer.min( query.getPage() * query.getPageSize(), grid.getHeight() ) );
            rootNode.addChild( NodeUtils.createPager( query.getPager() ) );
        }

        rootNode.addChild( nodeService.toNode( grid ) );

        return rootNode;
    }
}
