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
package org.hisp.dhis.program.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair Asghar
 */

@Repository( "org.hisp.dhis.program.ProgramNotificationInstanceStore" )
public class HibernateProgramNotificationInstanceStore
    extends HibernateIdentifiableObjectStore<ProgramNotificationInstance>
    implements ProgramNotificationInstanceStore
{
    public HibernateProgramNotificationInstanceStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, ProgramNotificationInstance.class, currentUserService,
            aclService, true );
    }

    @Override
    public List<ProgramNotificationInstance> getProgramNotificationInstances( ProgramNotificationInstanceParam params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<ProgramNotificationInstance> jpaParameters = newJpaParameters()
            .addPredicates( getPredicates( params, builder ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        if ( !params.isSkipPaging() )
        {
            jpaParameters
                .setFirstResult(
                    params.getPage() != null ? params.getPage() : ProgramNotificationInstanceParam.DEFAULT_PAGE )
                .setMaxResults( params.getPageSize() != null ? params.getPageSize()
                    : ProgramNotificationInstanceParam.DEFAULT_PAGE_SIZE );
        }

        return getList( builder, jpaParameters );
    }

    @Override
    public Long countProgramNotificationInstances( ProgramNotificationInstanceParam params )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        JpaQueryParameters<ProgramNotificationInstance> jpaParameters = newJpaParameters()
            .addPredicates( getPredicates( params, builder ) )
            .addOrder( root -> builder.desc( root.get( "created" ) ) );

        return getCount( builder, jpaParameters );
    }

    private List<Function<Root<ProgramNotificationInstance>, Predicate>> getPredicates(
        ProgramNotificationInstanceParam params, CriteriaBuilder builder )
    {
        List<Function<Root<ProgramNotificationInstance>, Predicate>> predicates = new ArrayList<>();

        if ( params.hasEvent() )
        {
            predicates.add( root -> builder.equal( root.get( "event" ),
                params.getEvent() ) );
        }

        if ( params.hasProgramInstance() )
        {
            predicates.add( root -> builder.equal( root.get( "programInstance" ),
                params.getProgramInstance() ) );
        }

        if ( params.hasScheduledAt() )
        {
            predicates.add( root -> builder.equal( root.get( "scheduledAt" ),
                params.getScheduledAt() ) );
        }

        return predicates;
    }
}
