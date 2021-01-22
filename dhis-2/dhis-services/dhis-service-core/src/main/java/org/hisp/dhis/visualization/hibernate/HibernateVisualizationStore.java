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
package org.hisp.dhis.visualization.hibernate;

import static org.hisp.dhis.visualization.VisualizationType.PIVOT_TABLE;

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
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository( "org.hisp.dhis.visualization.VisualizationStore" )
public class HibernateVisualizationStore
    extends HibernateIdentifiableObjectStore<Visualization>
    implements VisualizationStore
{
    private enum VisualizationSet
    {
        CHART,
        PIVOT_TABLE
    }

    public HibernateVisualizationStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        DeletedObjectService deletedObjectService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, Visualization.class, currentUserService, aclService, true );
    }

    @Override
    public List<Visualization> getCharts( final int first, final int max )
    {
        return getVisualizations( first, max, VisualizationSet.CHART );
    }

    @Override
    public List<Visualization> getChartsLikeName( final Set<String> words, final int first, final int max )
    {
        return getVisualizationsLikeName( words, first, max, VisualizationSet.CHART );
    }

    @Override
    public List<Visualization> getPivotTables( final int first, final int max )
    {
        return getVisualizations( first, max, VisualizationSet.PIVOT_TABLE );
    }

    @Override
    public List<Visualization> getPivotTablesLikeName( final Set<String> words, final int first, final int max )
    {
        return getVisualizationsLikeName( words, first, max, VisualizationSet.PIVOT_TABLE );
    }

    @Override
    public int countPivotTablesCreated( final Date startingAt )
    {
        return countVisualizationCreated( startingAt, VisualizationSet.PIVOT_TABLE );
    }

    @Override
    public int countChartsCreated( final Date startingAt )
    {
        return countVisualizationCreated( startingAt, VisualizationSet.CHART );
    }

    private int countVisualizationCreated( final Date startingAt, final VisualizationSet visualizationSet )
    {
        final CriteriaBuilder builder = getCriteriaBuilder();

        final JpaQueryParameters<Visualization> param = new JpaQueryParameters<Visualization>()
            .addPredicates( getSharingPredicates( builder ) )
            .addPredicate( root -> builder.greaterThanOrEqualTo( root.get( "created" ), startingAt ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) );

        if ( visualizationSet == VisualizationSet.PIVOT_TABLE )
        {
            return getCount( builder, param.addPredicate( root -> builder.equal( root.get( "type" ), PIVOT_TABLE ) ) )
                .intValue();
        }
        else
        {
            return getCount( builder,
                param.addPredicate( root -> builder.notEqual( root.get( "type" ), PIVOT_TABLE ) ) ).intValue();
        }
    }

    private List<Visualization> getVisualizations( final int first, final int max,
        final VisualizationSet visualizationSet )
    {
        final CriteriaBuilder builder = getCriteriaBuilder();

        final JpaQueryParameters<Visualization> param = new JpaQueryParameters<Visualization>()
            .addPredicates( getSharingPredicates( builder ) ).addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first ).setMaxResults( max );

        if ( visualizationSet == VisualizationSet.PIVOT_TABLE )
        {
            return getList( builder, param.addPredicate( root -> builder.equal( root.get( "type" ), PIVOT_TABLE ) ) );
        }
        else
        {
            return getList( builder,
                param.addPredicate( root -> builder.notEqual( root.get( "type" ), PIVOT_TABLE ) ) );
        }
    }

    private List<Visualization> getVisualizationsLikeName( final Set<String> words, final int first, final int max,
        final VisualizationSet visualizationSet )
    {
        final CriteriaBuilder builder = getCriteriaBuilder();

        final JpaQueryParameters<Visualization> param = new JpaQueryParameters<Visualization>()
            .addPredicates( getSharingPredicates( builder ) ).addOrder( root -> builder.asc( root.get( "name" ) ) )
            .setFirstResult( first ).setMaxResults( max );

        if ( words.isEmpty() )
        {
            return getList( builder, param );
        }

        final List<Function<Root<Visualization>, Predicate>> conjunction = new ArrayList<>( 1 );

        for ( final String word : words )
        {
            conjunction
                .add( root -> builder.like( builder.lower( root.get( "name" ) ), "%" + word.toLowerCase() + "%" ) );
        }

        param.addPredicate( root -> builder.and( conjunction.stream().map( p -> p.apply( root ) )
            .collect( Collectors.toList() ).toArray( new Predicate[0] ) ) );

        if ( visualizationSet == VisualizationSet.PIVOT_TABLE )
        {
            return getList( builder, param.addPredicate( root -> builder.equal( root.get( "type" ), PIVOT_TABLE ) ) );
        }
        else
        {
            return getList( builder,
                param.addPredicate( root -> builder.notEqual( root.get( "type" ), PIVOT_TABLE ) ) );
        }
    }
}
