package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationComment;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.schema.descriptors.InterpretationSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = InterpretationSchemaDescriptor.API_ENDPOINT )
public class InterpretationController
    extends AbstractCrudController<Interpretation>
{
    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private ReportTableService reportTableService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private MappingService mappingService;

    @Override
    protected List<Interpretation> getEntityList( WebMetaData metaData, WebOptions options, List<String> filters, List<Order> orders )
    {
        List<Interpretation> entityList;

        if ( options.getOptions().containsKey( "query" ) )
        {
            entityList = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else if ( options.hasPaging() )
        {
            int count = manager.getCount( getEntityClass() );

            Pager pager = new Pager( options.getPage(), count );
            metaData.setPager( pager );

            entityList = new ArrayList<>( interpretationService.getInterpretations( pager.getOffset(), pager.getPageSize() ) );
        }
        else
        {
            entityList = new ArrayList<>( interpretationService.getInterpretations() );
        }

        return entityList;
    }

    @RequestMapping( value = "/chart/{uid}", method = RequestMethod.POST, consumes = { "text/html", "text/plain" } )
    public void shareChartInterpretation(
        @PathVariable( "uid" ) String chartUid,
        @RequestBody String text, HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        Chart chart = chartService.getChart( chartUid );

        if ( chart == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Chart identifier not valid: " + chartUid ) );
        }

        User user = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // When chart has user org unit, store current user org unit with
        // interpretation so chart will refer to the original org unit later
        // ---------------------------------------------------------------------

        OrganisationUnit unit = chart.hasUserOrgUnit() && user.hasOrganisationUnit() ? user.getOrganisationUnit() : null;

        Interpretation interpretation = new Interpretation( chart, unit, text );

        interpretationService.saveInterpretation( interpretation );

        response.addHeader( "Location", InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Interpretation created" ), response, request );
    }

    @RequestMapping( value = "/map/{uid}", method = RequestMethod.POST, consumes = { "text/html", "text/plain" } )
    public void shareMapInterpretation(
        @PathVariable( "uid" ) String mapUid,
        @RequestBody String text, HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        Map map = mappingService.getMap( mapUid );

        if ( map == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Map identifier not valid: " + mapUid ) );
        }

        Interpretation interpretation = new Interpretation( map, text );

        interpretationService.saveInterpretation( interpretation );

        response.addHeader( "Location", InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Interpretation created" ), response, request );
    }

    @RequestMapping( value = "/reportTable/{uid}", method = RequestMethod.POST, consumes = { "text/html", "text/plain" } )
    public void shareReportTableInterpretation(
        @PathVariable( "uid" ) String reportTableUid,
        @RequestParam( value = "pe", required = false ) String isoPeriod,
        @RequestParam( value = "ou", required = false ) String orgUnitUid,
        @RequestBody String text, HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        ReportTable reportTable = reportTableService.getReportTable( reportTableUid );

        if ( reportTable == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Report table identifier not valid: " + reportTableUid ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        OrganisationUnit orgUnit = null;

        if ( orgUnitUid != null )
        {
            orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );

            if ( orgUnit == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit identifier not valid: " + orgUnitUid ) );
            }
        }

        Interpretation interpretation = new Interpretation( reportTable, period, orgUnit, text );

        interpretationService.saveInterpretation( interpretation );

        response.addHeader( "Location", InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Interpretation created" ), response, request );
    }

    @RequestMapping( value = "/dataSetReport/{uid}", method = RequestMethod.POST, consumes = { "text/html", "text/plain" } )
    public void shareDataSetReportInterpretation(
        @PathVariable( "uid" ) String dataSetUid,
        @RequestParam( "pe" ) String isoPeriod,
        @RequestParam( "ou" ) String orgUnitUid,
        @RequestBody String text, HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        DataSet dataSet = dataSetService.getDataSet( dataSetUid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set identifier not valid: " + dataSetUid ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period identifier not valid: " + isoPeriod ) );
        }

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );

        if ( orgUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit identifier not valid: " + orgUnitUid ) );
        }

        Interpretation interpretation = new Interpretation( dataSet, period, orgUnit, text );

        interpretationService.saveInterpretation( interpretation );

        response.addHeader( "Location", InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );
        webMessageService.send( WebMessageUtils.created( "Interpretation created" ), response, request );
    }

    @Override
    public void deleteObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Interpretation does not exist: " + uid ) );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getUser() ) &&
            !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to delete this interpretation." );
        }

        interpretationService.deleteInterpretation( interpretation );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT )
    public void updateInterpretation( @PathVariable( "uid" ) String uid, HttpServletResponse response,
        @RequestBody String content ) throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Interpretation does not exist: " + uid ) );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getUser() ) &&
            !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to update this interpretation." );
        }

        interpretation.setText( content );

        interpretationService.updateInterpretation( interpretation );
    }

    @RequestMapping( value = "/{uid}/comments/{cuid}", method = RequestMethod.DELETE )
    public void deleteComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid, HttpServletResponse response ) throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        Iterator<InterpretationComment> iterator = interpretation.getComments().iterator();

        while ( iterator.hasNext() )
        {
            InterpretationComment comment = iterator.next();

            if ( comment.getUid().equals( cuid ) )
            {
                if ( !currentUserService.getCurrentUser().equals( comment.getUser() ) &&
                    !currentUserService.currentUserIsSuper() )
                {
                    throw new AccessDeniedException( "You are not allowed to delete this comment." );
                }

                iterator.remove();
            }
        }

        interpretationService.updateInterpretation( interpretation );
    }

    @RequestMapping( value = "/{uid}/comments/{cuid}", method = RequestMethod.PUT )
    public void updateComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid, HttpServletResponse response,
        @RequestBody String content ) throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        for ( InterpretationComment comment : interpretation.getComments() )
        {
            if ( comment.getUid().equals( cuid ) )
            {
                if ( !currentUserService.getCurrentUser().equals( comment.getUser() ) &&
                    !currentUserService.currentUserIsSuper() )
                {
                    throw new AccessDeniedException( "You are not allowed to update this comment." );
                }

                comment.setText( content );
            }
        }

        interpretationService.updateInterpretation( interpretation );
    }

    @RequestMapping( value = { "/{uid}/comment", "/{uid}/comments" }, method = RequestMethod.POST, consumes = { "text/html", "text/plain" } )
    public void postComment(
        @PathVariable( "uid" ) String uid,
        @RequestBody String text, HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        InterpretationComment comment = interpretationService.addInterpretationComment( uid, text );

        StringBuilder builder = new StringBuilder();
        builder.append( InterpretationSchemaDescriptor.API_ENDPOINT ).append( "/" ).append( uid );
        builder.append( "/comments/" ).append( comment.getUid() );

        response.addHeader( "Location", builder.toString() );
        webMessageService.send( WebMessageUtils.created( "Commented created" ), response, request );
    }
}
