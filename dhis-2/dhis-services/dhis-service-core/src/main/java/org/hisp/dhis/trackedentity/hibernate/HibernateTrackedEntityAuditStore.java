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
package org.hisp.dhis.trackedentity.hibernate;

import static org.hisp.dhis.system.util.SqlUtils.singleQuote;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.audit.payloads.TrackedEntityAudit;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityAuditStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@Repository( "org.hisp.dhis.trackedentity.TrackedEntityAuditStore" )
public class HibernateTrackedEntityAuditStore
    extends HibernateGenericStore<TrackedEntityAudit>
    implements TrackedEntityAuditStore
{

    private final StatementBuilder statementBuilder;

    public HibernateTrackedEntityAuditStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, TrackedEntityAudit.class, false );
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // TrackedEntityAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    public void addTrackedEntityAudit( TrackedEntityAudit trackedEntityAudit )
    {
        getSession().save( trackedEntityAudit );
    }

    @Override
    public void addTrackedEntityAudit( List<TrackedEntityAudit> trackedEntityAudit )
    {
        final String sql = "INSERT INTO trackedentityinstanceaudit (" +
            "trackedentityinstanceauditid, " +
            "trackedentityinstance, " +
            "created, " +
            "accessedby, " +
            "audittype, " +
            "comment ) VALUES ";

        Function<TrackedEntityAudit, String> mapToString = audit -> {
            StringBuilder sb = new StringBuilder();
            sb.append( "(" );
            sb.append( "nextval('trackedentityinstanceaudit_sequence'), " );
            sb.append( singleQuote( audit.getTrackedEntity() ) ).append( "," );
            sb.append( "now()" ).append( "," );
            sb.append( singleQuote( audit.getAccessedBy() ) ).append( "," );
            sb.append( singleQuote( audit.getAuditType().getValue() ) ).append( "," );
            sb.append(
                StringUtils.isNotEmpty( audit.getComment() ) ? statementBuilder.encode( audit.getComment() ) : "''" );
            sb.append( ")" );
            return sb.toString();
        };

        final String values = trackedEntityAudit.stream().map( mapToString )
            .collect( Collectors.joining( "," ) );

        getSession().createNativeQuery( sql + values ).executeUpdate();
    }

    @Override
    public void deleteTrackedEntityAudit( TrackedEntity trackedEntity )
    {
        String hql = "delete TrackedEntityAudit where trackedEntity = :trackedEntity";
        getSession().createQuery( hql ).setParameter( "trackedEntity", trackedEntity ).executeUpdate();
    }

    @Override
    public List<TrackedEntityAudit> getTrackedEntityAudits(
        TrackedEntityAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<TrackedEntityAudit> jpaParameters = newJpaParameters()
            .addPredicates( getTrackedEntityAuditPredicates( params, builder ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        if ( params.hasPaging() )
        {
            jpaParameters
                .setFirstResult( params.getPager().getOffset() )
                .setMaxResults( params.getPager().getPageSize() );
        }

        return getList( builder, jpaParameters );
    }

    @Override
    public int getTrackedEntityAuditsCount( TrackedEntityAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters()
            .addPredicates( getTrackedEntityAuditPredicates( params, builder ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) ) ).intValue();
    }

    private List<Function<Root<TrackedEntityAudit>, Predicate>> getTrackedEntityAuditPredicates(
        TrackedEntityAuditQueryParams params, CriteriaBuilder builder )
    {
        List<Function<Root<TrackedEntityAudit>, Predicate>> predicates = new ArrayList<>();

        if ( params.hasTrackedEntities() )
        {
            predicates.add( root -> root.get( "trackedEntity" ).in( params.getTrackedEntities() ) );
        }

        if ( params.hasUsers() )
        {
            predicates.add( root -> root.get( "accessedBy" ).in( params.getUsers() ) );
        }

        if ( params.hasAuditTypes() )
        {
            predicates.add( root -> root.get( "auditType" ).in( params.getAuditTypes() ) );
        }

        if ( params.hasStartDate() )
        {
            predicates.add( root -> builder.greaterThanOrEqualTo( root.get( "created" ), params.getStartDate() ) );
        }

        if ( params.hasEndDate() )
        {
            predicates.add( root -> builder.lessThanOrEqualTo( root.get( "created" ), params.getEndDate() ) );
        }

        return predicates;
    }
}
