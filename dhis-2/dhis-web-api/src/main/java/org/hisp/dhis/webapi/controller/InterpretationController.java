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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
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
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.descriptors.InterpretationSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "ui" )
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

    @PostMapping( value = "/visualization/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    public WebMessage writeVisualizationInterpretation(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid,
        @CurrentUser User currentUser,
        @RequestBody String text )
        throws WebMessageException
    {
        final Visualization visualization = idObjectManager.get( Visualization.class, uid );

        if ( visualization == null )
        {
            return conflict( "Visualization does not exist or is not accessible: " + uid );
        }

        final OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, visualization, currentUser );

        return createInterpretation( new Interpretation( visualization, orgUnit, text ) );
    }

    @PostMapping( value = "/eventVisualization/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    public WebMessage writeEventVisualizationInterpretation( @PathVariable( "uid" ) final String uid,
        @RequestParam( value = "ou", required = false ) final String orgUnitUid, @RequestBody final String text )
        throws WebMessageException
    {
        final EventVisualization eventVisualization = idObjectManager.get( EventVisualization.class, uid );

        if ( eventVisualization == null )
        {
            return conflict( "EventVisualization does not exist or is not accessible: " + uid );
        }

        final OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, eventVisualization,
            currentUserService.getCurrentUser() );

        /*
         * This is needed until the deprecated entities (EventChart and
         * EventReport) are completely removed from the code base. Until there
         * all interpretations saved on the new EventVisualization entity should
         * also be available for the deprecated ones.
         */
        switch ( eventVisualization.getType() )
        {
        case LINE_LIST:
        case PIVOT_TABLE:
            final EventReport eventReport = idObjectManager.get( EventReport.class,
                eventVisualization.getUid() );
            return createInterpretation( new Interpretation( eventVisualization, eventReport, orgUnit, text ) );
        default:
            final EventChart eventChart = idObjectManager.get( EventChart.class, eventVisualization.getUid() );
            return createInterpretation( new Interpretation( eventVisualization, eventChart, orgUnit, text ) );
        }
    }

    @PostMapping( value = "/map/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    public WebMessage writeMapInterpretation( @PathVariable( "uid" ) String uid, @RequestBody String text )
    {
        Map map = idObjectManager.get( Map.class, uid );

        if ( map == null )
        {
            return conflict( "Map does not exist or is not accessible: " + uid );
        }

        return createInterpretation( new Interpretation( map, text ) );
    }

    @PostMapping( value = "/eventReport/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    @Deprecated
    public WebMessage writeEventReportInterpretation( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @CurrentUser User currentUser,
        @RequestBody String text )
        throws WebMessageException
    {
        EventReport eventReport = idObjectManager.get( EventReport.class, uid );

        if ( eventReport == null )
        {
            return conflict( "Event report does not exist or is not accessible: " + uid );
        }

        final EventVisualization eventVisualization = idObjectManager.get( EventVisualization.class,
            eventReport.getUid() );

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, eventReport, currentUser );

        return createInterpretation( new Interpretation( eventVisualization, eventReport, orgUnit, text ) );
    }

    @PostMapping( value = "/eventChart/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    @Deprecated
    public WebMessage writeEventChartInterpretation( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnitUid, @CurrentUser User currentUser,
        @RequestBody String text )
        throws WebMessageException
    {
        EventChart eventChart = idObjectManager.get( EventChart.class, uid );

        if ( eventChart == null )
        {
            return conflict( "Event chart does not exist or is not accessible: " + uid );
        }

        final EventVisualization eventVisualization = idObjectManager.get( EventVisualization.class,
            eventChart.getUid() );

        OrganisationUnit orgUnit = getUserOrganisationUnit( orgUnitUid, eventChart, currentUser );

        return createInterpretation( new Interpretation( eventVisualization, eventChart, orgUnit, text ) );
    }

    @PostMapping( value = "/dataSetReport/{uid}", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    public WebMessage writeDataSetReportInterpretation( @PathVariable( "uid" ) String dataSetUid,
        @RequestParam( "pe" ) String isoPeriod, @RequestParam( "ou" ) String orgUnitUid, @RequestBody String text )
    {
        DataSet dataSet = idObjectManager.get( DataSet.class, dataSetUid );

        if ( dataSet == null )
        {
            return conflict( "Data set does not exist or is not accessible: " + dataSetUid );
        }

        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period == null )
        {
            return conflict( "Period identifier not valid: " + isoPeriod );
        }

        OrganisationUnit orgUnit = idObjectManager.get( OrganisationUnit.class, orgUnitUid );

        if ( orgUnit == null )
        {
            return conflict( "Organisation unit does not exist or is not accessible: " + orgUnitUid );
        }

        return createInterpretation( new Interpretation( dataSet, period, orgUnit, text ) );
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
                    conflict( "Organisation unit does not exist or is not accessible: " + uid ) );
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
    private WebMessage createInterpretation( Interpretation interpretation )
    {
        interpretationService.saveInterpretation( interpretation );

        return created( "Interpretation created" )
            .setLocation( InterpretationSchemaDescriptor.API_ENDPOINT + "/" + interpretation.getUid() );
    }

    // -------------------------------------------------------------------------
    // Interpretation update
    // -------------------------------------------------------------------------

    @PutMapping( "/{uid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @ResponseBody
    public WebMessage updateInterpretation( @PathVariable( "uid" ) String uid, @RequestBody String text )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return notFound( "Interpretation does not exist: " + uid );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getCreatedBy() )
            && !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to update this interpretation." );
        }

        interpretationService.updateInterpretationText( interpretation, text );
        return null;
    }

    @Override
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public WebMessage deleteObject( @PathVariable String uid, @CurrentUser User currentUser, HttpServletRequest request,
        HttpServletResponse response )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return notFound( "Interpretation does not exist: " + uid );
        }

        if ( !currentUserService.getCurrentUser().equals( interpretation.getCreatedBy() )
            && !currentUserService.currentUserIsSuper() )
        {
            throw new AccessDeniedException( "You are not allowed to delete this interpretation." );
        }

        interpretationService.deleteInterpretation( interpretation );
        return null;
    }

    // -------------------------------------------------------------------------
    // Comment
    // -------------------------------------------------------------------------

    @PostMapping( value = "/{uid}/comments", consumes = { "text/html", "text/plain" } )
    @ResponseBody
    public WebMessage postComment( @PathVariable( "uid" ) String uid, @RequestBody String text,
        HttpServletResponse response )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return conflict( "Interpretation does not exist: " + uid );
        }

        InterpretationComment comment = interpretationService.addInterpretationComment( uid, text );

        return created( "Commented created" )
            .setLocation( InterpretationSchemaDescriptor.API_ENDPOINT + "/" + uid + "/comments/" + comment.getUid() );
    }

    @PutMapping( "/{uid}/comments/{cuid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @ResponseBody
    public WebMessage updateComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid,
        @RequestBody String content )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return conflict( "Interpretation does not exist: " + uid );
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
        return null;
    }

    @DeleteMapping( "/{uid}/comments/{cuid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @ResponseBody
    public WebMessage deleteComment( @PathVariable( "uid" ) String uid, @PathVariable( "cuid" ) String cuid,
        HttpServletResponse response )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return conflict( "Interpretation does not exist: " + uid );
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
        return null;
    }

    // -------------------------------------------------------------------------
    // Likes
    // -------------------------------------------------------------------------

    @PostMapping( "/{uid}/like" )
    @ResponseBody
    public WebMessage like( @PathVariable( "uid" ) String uid )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return conflict( "Interpretation does not exist: " + uid );
        }

        if ( interpretationService.likeInterpretation( interpretation.getId() ) )
        {
            return created( "Like added to interpretation" );
        }
        return conflict( "Could not add like, user had already liked interpretation" );
    }

    @DeleteMapping( "/{uid}/like" )
    @ResponseBody
    public WebMessage unlike( @PathVariable( "uid" ) String uid )
    {
        Interpretation interpretation = interpretationService.getInterpretation( uid );

        if ( interpretation == null )
        {
            return conflict( "Interpretation does not exist: " + uid );
        }

        if ( interpretationService.unlikeInterpretation( interpretation.getId() ) )
        {
            return created( "Like removed from interpretation" );
        }
        return conflict( "Could not remove like, user had not previously liked interpretation" );
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
}
