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
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@Repository( "org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditStore" )
public class HibernateTrackedEntityInstanceAuditStore
    extends HibernateGenericStore<TrackedEntityInstanceAudit>
    implements TrackedEntityInstanceAuditStore
{

    private final StatementBuilder statementBuilder;

    public HibernateTrackedEntityInstanceAuditStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, StatementBuilder statementBuilder )
    {
        super( sessionFactory, jdbcTemplate, publisher, TrackedEntityInstanceAudit.class, false );
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // TrackedEntityInstanceAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    public void addTrackedEntityInstanceAudit( TrackedEntityInstanceAudit trackedEntityInstanceAudit )
    {
        getSession().save( trackedEntityInstanceAudit );
    }

    @Override
    public void addTrackedEntityInstanceAudit( List<TrackedEntityInstanceAudit> trackedEntityInstanceAudit )
    {
        final String sql = "INSERT INTO trackedentityinstanceaudit (" +
            "trackedentityinstanceauditid, " +
            "trackedentityinstance, " +
            "created, " +
            "accessedby, " +
            "audittype, " +
            "comment ) VALUES ";

        Function<TrackedEntityInstanceAudit, String> mapToString = audit -> {
            StringBuilder sb = new StringBuilder();
            sb.append( "(" );
            sb.append( "nextval('trackedentityinstanceaudit_sequence'), " );
            sb.append( singleQuote( audit.getTrackedEntityInstance() ) ).append( "," );
            sb.append( "now()" ).append( "," );
            sb.append( singleQuote( audit.getAccessedBy() ) ).append( "," );
            sb.append( singleQuote( audit.getAuditType().getValue() ) ).append( "," );
            sb.append(
                StringUtils.isNotEmpty( audit.getComment() ) ? statementBuilder.encode( audit.getComment() ) : "''" );
            sb.append( ")" );
            return sb.toString();
        };

        final String values = trackedEntityInstanceAudit.stream().map( mapToString )
            .collect( Collectors.joining( "," ) );

        getSession().createNativeQuery( sql + values ).executeUpdate();
    }

    @Override
    public void deleteTrackedEntityInstanceAudit( TrackedEntityInstance trackedEntityInstance )
    {
        String hql = "delete TrackedEntityInstanceAudit where trackedEntityInstance = :trackedEntityInstance";
        getSession().createQuery( hql ).setParameter( "trackedEntityInstance", trackedEntityInstance ).executeUpdate();
    }

    @Override
    public List<TrackedEntityInstanceAudit> getTrackedEntityInstanceAudits(
        TrackedEntityInstanceAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<TrackedEntityInstanceAudit> jpaParameters = newJpaParameters()
            .addPredicates( getTrackedEntityInstanceAuditPredicates( params, builder ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        if ( !params.hasPaging() )
        {
            jpaParameters
                .setFirstResult( params.getPager().getOffset() )
                .setMaxResults( params.getPager().getPageSize() );
        }

        return getList( builder, jpaParameters );
    }

    @Override
    public int getTrackedEntityInstanceAuditsCount( TrackedEntityInstanceAuditQueryParams params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getCount( builder, newJpaParameters()
            .addPredicates( getTrackedEntityInstanceAuditPredicates( params, builder ) )
            .count( root -> builder.countDistinct( root.get( "id" ) ) ) ).intValue();
    }

    private List<Function<Root<TrackedEntityInstanceAudit>, Predicate>> getTrackedEntityInstanceAuditPredicates(
        TrackedEntityInstanceAuditQueryParams params, CriteriaBuilder builder )
    {
        List<Function<Root<TrackedEntityInstanceAudit>, Predicate>> predicates = new ArrayList<>();

        if ( params.hasTrackedEntityInstances() )
        {
            predicates.add( root -> root.get( "trackedEntityInstance" ).in( params.getTrackedEntityInstances() ) );
        }

        if ( params.hasUsers() )
        {
            predicates.add( root -> root.get( "accessedBy" ).in( params.getUsers() ) );
        }

        if ( params.hasAuditType() )
        {
            predicates.add( root -> root.get( "auditType" ).in( params.getAuditType() ) );
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
