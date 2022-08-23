/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.schema.descriptors.SqlViewSchemaDescriptor;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewQuery;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @GetMapping( value = "/{uid}/data", produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody Grid getViewJson( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws WebMessageException
    {
        SqlView sqlView = validateView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, sqlView.getCacheStrategy() );

        return getViewGrid( sqlView, query );
    }

    @GetMapping( "/{uid}/data.xml" )
    public void getViewXml( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws Exception
    {
        SqlView sqlView = validateView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, sqlView.getCacheStrategy() );

        Grid grid = getViewGrid( sqlView, query );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @GetMapping( "/{uid}/data.csv" )
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

    @GetMapping( "/{uid}/data.xls" )
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

    @GetMapping( "/{uid}/data.html" )
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

    @GetMapping( "/{uid}/data.html+css" )
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

    @GetMapping( "/{uid}/data.pdf" )
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

    @PostMapping( "/{uid}/execute" )
    @ResponseBody
    public WebMessage executeView( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> var )
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            return notFound( "SQL view does not exist: " + uid );
        }

        if ( sqlView.isQuery() )
        {
            return conflict( "SQL view is a query, no view to create" );
        }

        String result = sqlViewService.createViewTable( sqlView );
        if ( result != null )
        {
            return conflict( result );
        }
        return created( "SQL view created" )
            .setLocation( SqlViewSchemaDescriptor.API_ENDPOINT + "/" + sqlView.getUid() );
    }

    @PostMapping( "/{uid}/refresh" )
    @ResponseBody
    public WebMessage refreshMaterializedView( @PathVariable( "uid" ) String uid )
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            return notFound( "SQL view does not exist: " + uid );
        }

        if ( !sqlViewService.refreshMaterializedView( sqlView ) )
        {
            return conflict( "View could not be refreshed" );
        }
        return ok( "Materialized view refreshed" );
    }

    private SqlView validateView( String uid )
        throws WebMessageException
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            throw new WebMessageException( notFound( "SQL view does not exist: " + uid ) );
        }

        return sqlView;
    }

    private Grid getViewGrid( SqlView sqlView, SqlViewQuery query )
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, SqlView.getCriteria( query.getCriteria() ),
            SqlView.getCriteria( query.getVar() ), filters, fields );

        if ( !query.isSkipPaging() )
        {
            query.setTotal( grid.getHeight() );
            grid.limitGrid( (query.getPage() - 1) * query.getPageSize(),
                Integer.min( query.getPage() * query.getPageSize(), grid.getHeight() ) );
        }
        return grid;
    }
}
