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
package org.hisp.dhis.eventvisualization.hibernate;

import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateAnalyticalObjectStore;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @deprecated Needed to keep EventChart and EventReports backward compatible
 *             with the new entity EventVisualization.
 *
 * @author maikel arabori
 */
@Deprecated
@Repository( "org.hisp.dhis.eventvisualization.EventVisualizationStore" )
public class HibernateEventVisualizationStore extends
    HibernateAnalyticalObjectStore<EventVisualization>
    implements
    EventVisualizationStore
{
    private enum EventVisualizationSet
    {
        EVENT_CHART,
        EVENT_REPORT,
        EVENT_LINE_LIST
    }

    public HibernateEventVisualizationStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, EventVisualization.class, currentUserService,
            aclService, true );
    }

    @Override
    public List<EventVisualization> getCharts( int first, int max )
    {
        return getEventVisualizations( first, max, EventVisualizationSet.EVENT_CHART, true );
    }

    @Override
    public List<EventVisualization> getChartsLikeName( Set<String> words, int first, int max )
    {
        return getEventVisualizationsLikeName( words, first, max, EventVisualizationSet.EVENT_CHART, true );
    }

    @Override
    public List<EventVisualization> getReports( int first, int max )
    {
        return getEventVisualizations( first, max, EventVisualizationSet.EVENT_REPORT, true );
    }

    @Override
    public List<EventVisualization> getReportsLikeName( Set<String> words, int first, int max )
    {
        return getEventVisualizationsLikeName( words, first, max, EventVisualizationSet.EVENT_REPORT, true );
    }

    @Override
    public int countReportsCreated( Date startingAt )
    {
        return countEventVisualizationCreated( startingAt, EventVisualizationSet.EVENT_REPORT, true );
    }

    @Override
    public int countChartsCreated( Date startingAt )
    {
        return countEventVisualizationCreated( startingAt, EventVisualizationSet.EVENT_CHART, true );
    }

    @Override
    public int countEventVisualizationsCreated( Date startingAt )
    {
        return countEventVisualizationCreated( startingAt, EventVisualizationSet.EVENT_LINE_LIST, false );
    }

    @Override
    public List<EventVisualization> getLineLists( int first, int max )
    {
        return getEventVisualizations( first, max, EventVisualizationSet.EVENT_LINE_LIST, null );
    }

    @Override
    public List<EventVisualization> getLineListsLikeName( Set<String> words, int first, int max )
    {
        return getEventVisualizationsLikeName( words, first, max, EventVisualizationSet.EVENT_LINE_LIST, null );
    }

    private int countEventVisualizationCreated( Date startingAt, EventVisualizationSet eventVisualizationSet,
        boolean legacy )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<EventVisualization> params = new JpaQueryParameters<EventVisualization>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "created" ), startingAt ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        setCorrectPredicates( eventVisualizationSet, builder, params, legacy );

        return getCount( builder, params ).intValue();
    }

    private List<EventVisualization> getEventVisualizations( int first, int max,
        EventVisualizationSet eventVisualizationSet, Boolean legacy )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<EventVisualization> params = new JpaQueryParameters<EventVisualization>()
            .addPredicates( getSharingPredicates( builder ) ).addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first ).setMaxResults( max );

        setCorrectPredicates( eventVisualizationSet, builder, params, legacy );

        return getList( builder, params );
    }

    private List<EventVisualization> getEventVisualizationsLikeName( Set<String> words, int first, int max,
        EventVisualizationSet eventVisualizationSet, Boolean legacy )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<EventVisualization> params = new JpaQueryParameters<EventVisualization>()
            .addPredicates( getSharingPredicates( builder ) ).addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first ).setMaxResults( max );

        if ( words.isEmpty() )
        {
            return getList( builder, params );
        }

        List<Function<Root<EventVisualization>, Predicate>> conjunction = new ArrayList<>( 1 );

        for ( String word : words )
        {
            conjunction
                .add( root -> builder.like( builder.lower( root.get( "name" ) ), "%" + word.toLowerCase() + "%" ) );
        }

        params.addPredicate( root -> builder.and( conjunction.stream().map( p -> p.apply( root ) )
            .collect( Collectors.toList() ).toArray( new Predicate[0] ) ) );

        setCorrectPredicates( eventVisualizationSet, builder, params, legacy );

        return getList( builder, params );
    }

    private void setCorrectPredicates( EventVisualizationSet eventVisualizationSet, CriteriaBuilder builder,
        JpaQueryParameters<EventVisualization> params, Boolean legacy )
    {
        if ( eventVisualizationSet == EventVisualizationSet.EVENT_CHART )
        {
            params.addPredicate( root -> builder.and( builder.notEqual( root.get( "type" ), PIVOT_TABLE ),
                builder.notEqual( root.get( "type" ), LINE_LIST ) ) );
        }
        else if ( eventVisualizationSet == EventVisualizationSet.EVENT_REPORT )
        {
            params.addPredicate( root -> builder.or( builder.equal( root.get( "type" ), PIVOT_TABLE ),
                builder.equal( root.get( "type" ), LINE_LIST ) ) );
        }
        else
        {
            params.addPredicate( root -> builder.equal( root.get( "type" ), LINE_LIST ) );
        }

        if ( legacy != null )
        {
            params.addPredicate( root -> builder.equal( root.get( "legacy" ), legacy ) );
        }
    }
}
