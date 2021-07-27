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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationComment;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.interpretation.MentionUtils;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.descriptors.InterpretationSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationType;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = InterpretationSchemaDescriptor.API_ENDPOINT )
public class InterpretationController extends AbstractCrudController<Interpretation>
{
    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<Interpretation> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws QueryParserException
    {
        // If custom filter (mentions:in:[username]) in filters -> Remove from
        // filters
        List<String> mentionsFromCustomFilters = MentionUtils.removeCustomFilters( filters );

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, getPaginationData( options ),
            options.getRootJunction() );
        query.setDefaultOrder();
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );
        // If custom filter (mentions:in:[username]) in filters -> Add as
        // disjunction including interpretation mentions and comments mentions
        for ( Disjunction disjunction : (Collection<Disjunction>) getDisjunctionsFromCustomMentions(
            mentionsFromCustomFilters, query.getSchema() ) )
        {
            query.add( disjunction );
        }

        List<Interpretation> entityList;
        if ( options.getOptions().containsKey( "query" ) )
        {
            entityList = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else
        {
            entityList = (List<Interpretation>) queryService.query( query );
        }
        return entityList;
    }

    // -------------------------------------------------------------------------
    // Interpretation create
    // -------------------------------------------------------------------------

    @PostMapping( value = "/reportTable/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeReportTableInterpretation( @PathVariable( "uid" ) String visualizationUid,
        @RequestParam( value = "pe", required = false ) String isoPeriod,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        Visualization visualization = idObjectManager.get( Visualization.class, visualizationUid );

        if ( visualization == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Report table does not exist or is not accessible: " + visualizationUid ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, visualization,
            currentUserService.getCurrentUser() );

        createIntepretation( new Interpretation( visualization, period, orgUnit, text ),
            request, response );
    }

    @PostMapping( value = "/chart/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeChartInterpretation( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        Visualization visualization = idObjectManager.get( Visualization.class, uid );

        if ( visualization == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Chart does not exist or is not accessible: " + uid ) );
        }

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, visualization,
            currentUserService.getCurrentUser() );

        createIntepretation( new Interpretation( visualization, orgUnit, text ), request, response );
    }

    @PostMapping( value = "/visualization/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeVisualizationInterpretation( @PathVariable( "uid" )
    final String uid, @RequestParam( value = "ou", required = false )
    final String orgUnitUid, @RequestBody
    final String text, final HttpServletResponse response, final HttpServletRequest request )
        throws WebMessageException
    {
        final Visualization visualization = idObjectManager.get( Visualization.class, uid );

        if ( visualization == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Visualization does not exist or is not accessible: " + uid ) );
        }

        final OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, visualization,
            currentUserService.getCurrentUser() );

        createIntepretation( new Interpretation( visualization, orgUnit, text ), request, response );
    }

    @PostMapping( value = "/map/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeMapInterpretation( @PathVariable( "uid" ) String uid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        Map map = idObjectManager.get( Map.class, uid );

        if ( map == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Map does not exist or is not accessible: " + uid ) );
        }

        createIntepretation( new Interpretation( map, text ), request, response );
    }

    @PostMapping( value = "/eventReport/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeEventReportInterpretation( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        EventReport eventReport = idObjectManager.get( EventReport.class, uid );

        if ( eventReport == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Event report does not exist or is not accessible: " + uid ) );
        }

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, eventReport,
            currentUserService.getCurrentUser() );

        createIntepretation( new Interpretation( eventReport, orgUnit, text ), request, response );
    }

    @PostMapping( value = "/eventChart/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeEventChartInterpretation( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        EventChart eventChart = idObjectManager.get( EventChart.class, uid );

        if ( eventChart == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Event chart does not exist or is not accessible: " + uid ) );
        }

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, eventChart,
            currentUserService.getCurrentUser() );

        createIntepretation( new Interpretation( eventChart, orgUnit, text ), request, response );
    }

    @PostMapping( value = "/dataSetReport/{uid}", consumes = { "text/html", "text/plain" } )
    public void writeDataSetReportInterpretation( @PathVariable( "uid" ) String dataSetUid,
        @RequestParam( "pe" ) String isoPeriod, @RequestParam( "ou" ) String orgUnitUid, @RequestBody String text,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        DataSet dataSet = idObjectManager.get( DataSet.class, dataSetUid );

        if ( dataSet == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Data set does not exist or is not accessible: " + dataSetUid ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period identifier not valid: " + isoPeriod ) );
        }

        OrganisationUnit orgUnit = idObjectManager.get( OrganisationUnit.class, orgUnitUid );

        if ( orgUnit == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Organisation unit does not exist or is not accessible: " + orgUnitUid ) );
        }

        createIntepretation( new Interpretation( dataSet, period, orgUnit, text ), request, response );
    }

    /**
     * Returns the organisation unit with the given identifier. If not existing,
     * returns the user organisation unit if the analytical object specifies a
     * user organisation unit. If not, returns null.
     */
    private OrganisationUnit getUserOrganisationUnit( String uid, AnalyticalObject analyticalObject, User user )
        throws WebMessageException
    {
        OrganisationUnit unit = null;

        if ( uid != null )
        {
            unit = idObjectManager.get( OrganisationUnit.class, uid );

            if ( unit == null )
            {
                throw new WebMessageException(
                    WebMessageUtils.conflict( "Organisation unit does not exist or is not accessible: " + uid ) );
            }

            return unit;
        }
        else if ( analyticalObject.hasUserOrgUnit() && user.hasOrganisationUnit() )
        {
            unit = user.getOrganisationUnit();
        }

        return unit;
    }

    /**
     * Saves the given interpretation, adds location header and returns a web
     * message response.
     */
    private void createIntepretation( Interpretation interpretation, HttpServletRequest request,
        HttpServletResponse response )
    {
        interpretationService.saveInterpretation( interpretation );

        response.addHeader( "Location", InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );

        webMessageService.send( WebMessageUtils.created( "Interpretation created" ), response, request );
    }

    // -------------------------------------------------------------------------
    // Interpretation update
    // -------------------------------------------------------------------------

    @PutMapping( "/{uid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateInterpretation( @PathVariable( "uid" ) String uid, @RequestBody String text,
        HttpServletResponse response )
        throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Interpretation does not exist: " + uid ) );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getCreatedBy() )
            && !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to update this interpretation." );
        }

        interpretationService.updateInterpretationText( interpretation, text );
    }

    @Override
    public void deleteObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Interpretation does not exist: " + uid ) );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getCreatedBy() )
            && !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to delete this interpretation." );
        }

        interpretationService.deleteInterpretation( interpretation );
    }

    // -------------------------------------------------------------------------
    // Comment
    // -------------------------------------------------------------------------

    @PostMapping( value = "/{uid}/comments", consumes = { "text/html", "text/plain" } )
    public void postComment( @PathVariable( "uid" ) String uid, @RequestBody String text, HttpServletResponse response,
        HttpServletRequest request )
        throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        InterpretationComment comment = interpretationService.addInterpretationComment( uid, text );

        String builder = InterpretationSchemaDescriptor.API_ENDPOINT + "/" + uid + "/comments/" + comment.getUid();

        response.addHeader( "Location", builder );
        webMessageService.send( WebMessageUtils.created( "Commented created" ), response, request );
    }

    @PutMapping( "/{uid}/comments/{cuid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid,
        HttpServletResponse response, @RequestBody String content )
        throws WebMessageException
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
                if ( !currentUserService.getCurrentUser().equals( comment.getCreatedBy() )
                    && !currentUserService.currentUserIsSuper() )
                {
                    throw new AccessDeniedException( "You are not allowed to update this comment." );
                }

                comment.setText( content );
                interpretationService.updateComment( interpretation, comment );

            }
        }
    }

    @DeleteMapping( "/{uid}/comments/{cuid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid,
        HttpServletResponse response )
        throws WebMessageException
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
                if ( !currentUserService.getCurrentUser().equals( comment.getCreatedBy() )
                    && !currentUserService.currentUserIsSuper() )
                {
                    throw new AccessDeniedException( "You are not allowed to delete this comment." );
                }

                iterator.remove();
            }
        }

        interpretationService.updateInterpretation( interpretation );
    }

    // -------------------------------------------------------------------------
    // Likes
    // -------------------------------------------------------------------------

    @PostMapping( "/{uid}/like" )
    public void like( @PathVariable( "uid" ) String uid, HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        boolean like = interpretationService.likeInterpretation( interpretation.getId() );

        if ( like )
        {
            webMessageService.send( WebMessageUtils.created( "Like added to interpretation" ), response, request );
        }
        else
        {
            webMessageService.send(
                WebMessageUtils.conflict( "Could not add like, user had already liked interpretation" ), response,
                request );
        }
    }

    @DeleteMapping( "/{uid}/like" )
    public void unlike( @PathVariable( "uid" ) String uid, HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Interpretation does not exist: " + uid ) );
        }

        boolean like = interpretationService.unlikeInterpretation( interpretation.getId() );

        if ( like )
        {
            webMessageService.send( WebMessageUtils.created( "Like removed from interpretation" ), response, request );
        }
        else
        {
            webMessageService.send(
                WebMessageUtils.conflict( "Could not remove like, user had not previously liked interpretation" ),
                response, request );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Collection<Disjunction> getDisjunctionsFromCustomMentions( List<String> mentions, Schema schema )
    {
        Collection<Disjunction> disjunctions = new ArrayList<Disjunction>();
        for ( String m : mentions )
        {
            Disjunction disjunction = new Disjunction( schema );
            String[] split = m.substring( 1, m.length() - 1 ).split( "," );
            List<String> items = Lists.newArrayList( split );
            disjunction.add( Restrictions.in( "mentions.username", items ) );
            disjunction.add( Restrictions.in( "comments.mentions.username", items ) );
            disjunctions.add( disjunction );
        }
        return disjunctions;
    }

    /**
     * Logic required to keep the backward compatibility with Chart and
     * ReporTable. Otherwise it would always return VISUALIZATION type for any
     * Chart or ReportTable.
     *
     * Only needed during the transition from Chart/ReportTable APIs to
     * Visualization API. Once the Visualization API is fully enabled this logic
     * should be removed.
     *
     * @param interpretations
     * @param options
     * @param parameters
     */
    @Override
    @Deprecated
    protected void postProcessResponseEntities( final List<Interpretation> interpretations, final WebOptions options,
        final java.util.Map<String, String> parameters )
    {
        if ( isNotEmpty( interpretations ) )
        {
            for ( final Interpretation interpretation : interpretations )
            {
                postProcessResponseEntity( interpretation, options, parameters );
            }
        }
    }

    /**
     * Logic required to keep the backward compatibility with Chart and
     * ReporTable. Otherwise it would always return VISUALIZATION type for any
     * Chart or ReportTable.
     *
     * Only needed during the transition from Chart/ReportTable APIs to
     * Visualization API. Once the Visualization API is fully enabled this logic
     * should be removed.
     *
     * @param interpretation
     * @param options
     * @param parameters
     */
    @Override
    @Deprecated
    protected void postProcessResponseEntity( final Interpretation interpretation, final WebOptions options,
        final java.util.Map<String, String> parameters )
    {
        if ( interpretation != null && interpretation.getVisualization() != null )
        {
            final VisualizationType type = interpretation.getVisualization().getType();

            switch ( type )
            {
            case PIVOT_TABLE:
                final ReportTable reportTable = new ReportTable();
                reportTable.setUid( interpretation.getVisualization().getUid() );
                interpretation.setReportTable( reportTable );
                break;
            case AREA:
            case BAR:
            case COLUMN:
            case GAUGE:
            case LINE:
            case PIE:
            case RADAR:
            case SINGLE_VALUE:
            case STACKED_BAR:
            case STACKED_COLUMN:
            case YEAR_OVER_YEAR_COLUMN:
            case YEAR_OVER_YEAR_LINE:
            case SCATTER:
            case BUBBLE:
                final Chart chart = new Chart();
                chart.setUid( interpretation.getVisualization().getUid() );
                interpretation.setChart( chart );
                break;
            }
        }
    }
}
