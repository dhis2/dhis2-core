package org.hisp.dhis.schema.audit.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditQuery;
import org.hisp.dhis.schema.audit.MetadataAuditStore;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateMetadataAuditStore
    implements MetadataAuditStore
{
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public int save( MetadataAudit audit )
    {
        return (int) getCurrentSession().save( audit );
    }

    @Override
    public void delete( MetadataAudit audit )
    {
        getCurrentSession().delete( audit );
    }

    @Override
    public int count( MetadataAuditQuery query )
    {
        CriteriaBuilder builder = getCurrentSession().getCriteriaBuilder();

        CriteriaQuery<Long> criteriaQuery = builder.createQuery( Long.class );

        Root<MetadataAudit> root = criteriaQuery.from( MetadataAudit.class );

        criteriaQuery.select( builder.countDistinct( root.get( "id" ) ) );

        criteriaQuery.where( buildCriteria( builder, root, query ).toArray( new Predicate[0] ) );

        return getCurrentSession().createQuery( criteriaQuery ).getSingleResult().intValue();
    }

    @Override
    public List<MetadataAudit> query( MetadataAuditQuery query )
    {
        CriteriaBuilder builder = getCurrentSession().getCriteriaBuilder();

        CriteriaQuery<MetadataAudit> criteriaQuery = builder.createQuery( MetadataAudit.class );

        Root<MetadataAudit> root = criteriaQuery.from( MetadataAudit.class );

        criteriaQuery.where( buildCriteria( builder, root, query ).toArray( new Predicate[0] ) );

        Query<MetadataAudit> typedQuery = getCurrentSession().createQuery( criteriaQuery );

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            typedQuery.setFirstResult( pager.getOffset() );
            typedQuery.setMaxResults( pager.getPageSize() );
        }

        return typedQuery.getResultList();
    }

    private List<Predicate> buildCriteria( CriteriaBuilder builder, Root<MetadataAudit> root, MetadataAuditQuery query )
    {
        List<Predicate> predicates = new ArrayList<>();

        if ( query.getKlass().isEmpty() )
        {
            Predicate disjunction = builder.disjunction();

            if ( !query.getUid().isEmpty() )
            {
                 disjunction.getExpressions().add( root.get( "uid" ).in( query.getUid() ) );
            }

            if ( !query.getCode().isEmpty() )
            {
                disjunction.getExpressions().add( root.get( "code" ).in( query.getCode() ) );
            }

            predicates.add( disjunction );
        }
        else if ( query.getUid().isEmpty() && query.getCode().isEmpty() )
        {
            predicates.add( root.get( "klass" ).in( query.getKlass() ) );
        }
        else
        {
            Predicate disjunction = builder.disjunction();

            if ( !query.getUid().isEmpty() )
            {
                Predicate conjunction = builder.and( root.get( "klass" ).in( query.getKlass() ), root.get( "uid" ).in( query.getUid() ) );
                disjunction.getExpressions().add( conjunction );
            }

            if ( !query.getCode().isEmpty() )
            {
                Predicate conjunction = builder.and( root.get( "klass" ).in( query.getKlass() ), root.get( "code" ).in( query.getUid() ) );
                disjunction.getExpressions().add( conjunction );
            }

            predicates.add( disjunction );
        }

        if ( query.getCreatedAt() != null )
        {
            predicates.add( builder.greaterThanOrEqualTo( root.get( "createdAt" ), query.getCreatedAt() ) );
        }

        if ( query.getCreatedBy() != null )
        {
            predicates.add( builder.equal( root.get( "createdBy" ), query.getCreatedBy() ) );
        }

        if ( query.getType() != null )
        {
            predicates.add( builder.equal( root.get( "type" ), query.getType() ) );
        }

        return predicates;
    }

    private Session getCurrentSession()
    {
        return sessionFactory.getCurrentSession();
    }
}
