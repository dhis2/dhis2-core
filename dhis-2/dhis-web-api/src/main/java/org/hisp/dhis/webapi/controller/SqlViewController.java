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

import static java.lang.String.format;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.sqlview.SqlView.getCriteria;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridResponse;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.SqlViewUpdateParameters;
import org.hisp.dhis.schema.descriptors.SqlViewSchemaDescriptor;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewQuery;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
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
@OpenApi.Tags( "system" )
@Controller
@RequestMapping( value = SqlViewSchemaDescriptor.API_ENDPOINT )
@RequiredArgsConstructor
public class SqlViewController
    extends AbstractCrudController<SqlView>
{
    private final SqlViewService sqlViewService;

    private final JobConfigurationService jobConfigurationService;

    private final ContextUtils contextUtils;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @GetMapping( value = "/{uid}/data", produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody GridResponse getViewJson( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws NotFoundException
    {
        SqlView sqlView = getExistingSQLView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, sqlView.getCacheStrategy() );

        return buildResponse( sqlView, query );
    }

    @GetMapping( "/{uid}/data.xml" )
    public void getViewXml( @PathVariable( "uid" ) String uid,
        SqlViewQuery query, HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        SqlView sqlView = getExistingSQLView( uid );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, sqlView.getCacheStrategy() );

        GridUtils.toXml( buildResponse( sqlView, query ), response.getOutputStream() );
    }

    @GetMapping( "/{uid}/data.csv" )
    public void getViewCsv( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria,
        @RequestParam( name = "var", required = false ) Set<String> vars,
        HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        Grid grid = querySQLView( uid, criteria, vars, response, ContextUtils.CONTENT_TYPE_CSV, ".csv" );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    @GetMapping( "/{uid}/data.xls" )
    public void getViewXls( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria,
        @RequestParam( name = "var", required = false ) Set<String> vars,
        HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        Grid grid = querySQLView( uid, criteria, vars, response, ContextUtils.CONTENT_TYPE_EXCEL, ".xls" );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @GetMapping( "/{uid}/data.html" )
    public void getViewHtml( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria,
        @RequestParam( name = "var", required = false ) Set<String> vars,
        HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        Grid grid = querySQLView( uid, criteria, vars, response, ContextUtils.CONTENT_TYPE_HTML );

        GridUtils.toHtml( grid, response.getWriter() );
    }

    @GetMapping( "/{uid}/data.html+css" )
    public void getViewHtmlCss( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria,
        @RequestParam( name = "var", required = false ) Set<String> vars,
        HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        Grid grid = querySQLView( uid, criteria, vars, response, ContextUtils.CONTENT_TYPE_HTML );

        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @GetMapping( "/{uid}/data.pdf" )
    public void getViewPdf( @PathVariable( "uid" ) String uid,
        @RequestParam( required = false ) Set<String> criteria,
        @RequestParam( name = "var", required = false ) Set<String> vars,
        HttpServletResponse response )
        throws NotFoundException,
        IOException
    {
        Grid grid = querySQLView( uid, criteria, vars, response, ContextUtils.CONTENT_TYPE_PDF );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    private Grid querySQLView( String uid, Set<String> criteria, Set<String> vars, HttpServletResponse response,
        String contentType )
        throws NotFoundException
    {
        SqlView sqlView = getExistingSQLView( uid );
        Grid grid = querySQLView( criteria, vars, sqlView );

        contextUtils.configureResponse( response, contentType, sqlView.getCacheStrategy() );
        return grid;
    }

    private Grid querySQLView( String uid, Set<String> criteria, Set<String> vars, HttpServletResponse response,
        String contentTypeCsv, String fileExtension )
        throws NotFoundException
    {
        SqlView sqlView = getExistingSQLView( uid );
        Grid grid = querySQLView( criteria, vars, sqlView );

        String filename = CodecUtils.filenameEncode( grid.getTitle() ) + fileExtension;
        contextUtils.configureResponse( response, contentTypeCsv, sqlView.getCacheStrategy(), filename, true );
        return grid;
    }

    private Grid querySQLView( Set<String> criteria, Set<String> vars, SqlView sqlView )
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        return sqlViewService.getSqlViewGrid( sqlView, getCriteria( criteria ), getCriteria( vars ), filters, fields );
    }

    // -------------------------------------------------------------------------
    // Post
    // -------------------------------------------------------------------------

    @PostMapping( "/{uid}/execute" )
    @ResponseBody
    public WebMessage executeView( @PathVariable( "uid" ) String uid )
        throws NotFoundException,
        ConflictException
    {
        SqlView sqlView = getExistingSQLView( uid );

        if ( sqlView.isQuery() )
        {
            throw new ConflictException( "SQL view is a query, no view to create" );
        }

        String result = sqlViewService.createViewTable( sqlView );
        if ( result != null )
        {
            throw new ConflictException( result );
        }
        return created( "SQL view created" )
            .setLocation( SqlViewSchemaDescriptor.API_ENDPOINT + "/" + sqlView.getUid() );
    }

    @PostMapping( "/{uid}/refresh" )
    @ResponseBody
    public WebMessage refreshMaterializedView( @PathVariable( "uid" ) String uid )
        throws NotFoundException,
        ConflictException
    {
        SqlView sqlView = getExistingSQLView( uid );

        if ( !sqlViewService.refreshMaterializedView( sqlView ) )
        {
            throw new ConflictException( "View could not be refreshed" );
        }
        return ok( "Materialized view refreshed" );
    }

    private SqlView getExistingSQLView( String uid )
        throws NotFoundException
    {
        SqlView sqlView = sqlViewService.getSqlViewByUid( uid );

        if ( sqlView == null )
        {
            throw new NotFoundException( SqlView.class, uid );
        }

        return sqlView;
    }

    private GridResponse buildResponse( SqlView sqlView, SqlViewQuery query )
    {
        Grid grid = querySQLView( query.getCriteria(), query.getVar(), sqlView );

        if ( !query.isSkipPaging() )
        {
            query.setTotal( grid.getHeight() );
            grid.limitGrid( (query.getPage() - 1) * query.getPageSize(),
                Integer.min( query.getPage() * query.getPageSize(), grid.getHeight() ) );
        }
        return new GridResponse( query.isSkipPaging() ? null : query.getPager(), grid );
    }

    @Override
    protected void preCreateEntity( SqlView entity )
        throws ConflictException
    {
        checkJobConfigurationExists( entity );
    }

    @Override
    protected void postCreateEntity( SqlView entity )
    {
        if ( entity.getUpdateJobId() != null )
        {
            addAndUpdateIfNeeded( entity );
        }
    }

    @Override
    protected void preUpdateEntity( SqlView entity, SqlView newEntity )
        throws ConflictException
    {
        removeAndUpdateIfNeeded( newEntity );
        if ( newEntity.getUpdateJobId() != null )
        {
            checkJobConfigurationExists( newEntity ); // validate
            addAndUpdateIfNeeded( newEntity );
        }
    }

    @Override
    protected void prePatchEntity( SqlView entity, SqlView newEntity )
        throws ConflictException
    {
        preUpdateEntity( entity, newEntity );
    }

    @Override
    protected void preDeleteEntity( SqlView entity )
    {
        removeAndUpdateIfNeeded( entity );
    }

    private void checkJobConfigurationExists( SqlView entity )
        throws ConflictException
    {
        String jobId = entity.getUpdateJobId();
        if ( jobId == null )
        {
            return;
        }
        JobConfiguration config = jobConfigurationService.getJobConfigurationByUid( jobId );
        if ( config == null )
        {
            throw new ConflictException( "Update job does not exist: " + jobId );
        }
        if ( config.getJobType() != JobType.MATERIALIZED_SQL_VIEW_UPDATE )
        {
            throw new ConflictException(
                format( "Update job must be of type %s but was: %s", JobType.MATERIALIZED_SQL_VIEW_UPDATE,
                    config.getJobType() ) );
        }
    }

    private void addAndUpdateIfNeeded( SqlView entity )
    {
        JobConfiguration config = jobConfigurationService.getJobConfigurationByUid( entity.getUpdateJobId() );
        if ( config == null )
        {
            return;
        }
        List<String> sqlViews = getSqlViews( config );
        if ( !sqlViews.contains( entity.getUid() ) )
        {
            sqlViews.add( entity.getUid() );
            jobConfigurationService.updateJobConfiguration( config );
        }
    }

    private void removeAndUpdateIfNeeded( SqlView entity )
    {
        String jobId = entity.getUpdateJobId();
        // OBS! the job is a non-persistent property => clear all possible job configurations
        for ( JobConfiguration config : jobConfigurationService
            .getJobConfigurations( JobType.MATERIALIZED_SQL_VIEW_UPDATE ) )
        {
            if ( jobId == null || !jobId.equals( config.getUid() ) )
            {
                List<String> sqlViews = getSqlViews( config );
                if ( sqlViews.contains( entity.getUid() ) )
                {
                    sqlViews.remove( entity.getUid() );
                    jobConfigurationService.updateJobConfiguration( config );
                }
            }
        }
    }

    private List<String> getSqlViews( JobConfiguration config )
    {
        SqlViewUpdateParameters params = (SqlViewUpdateParameters) config.getJobParameters();
        if ( params == null )
        {
            params = new SqlViewUpdateParameters();
            config.setJobParameters( params );
        }
        return params.getSqlViews();
    }
}
