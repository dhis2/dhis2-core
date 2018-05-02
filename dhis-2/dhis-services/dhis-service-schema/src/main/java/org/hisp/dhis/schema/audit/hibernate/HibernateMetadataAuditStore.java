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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditQuery;
import org.hisp.dhis.schema.audit.MetadataAuditStore;
import org.springframework.beans.factory.annotation.Autowired;

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
        Criteria criteria = buildCriteria( query );
        return ((Number) criteria.setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MetadataAudit> query( MetadataAuditQuery query )
    {
        Criteria criteria = buildCriteria( query );

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            criteria.setFirstResult( pager.getOffset() );
            criteria.setMaxResults( pager.getPageSize() );
        }

        return criteria.list();
    }

    private Criteria buildCriteria( MetadataAuditQuery query )
    {
        Criteria criteria = getCurrentSession().createCriteria( MetadataAudit.class );

        if ( query.getKlass().isEmpty() )
        {
            Disjunction disjunction = Restrictions.disjunction();

            if ( !query.getUid().isEmpty() )
            {
                disjunction.add( Restrictions.in( "uid", query.getUid() ) );
            }

            if ( !query.getCode().isEmpty() )
            {
                disjunction.add( Restrictions.in( "code", query.getCode() ) );
            }

            criteria.add( disjunction );
        }
        else if ( query.getUid().isEmpty() && query.getCode().isEmpty() )
        {
            criteria.add( Restrictions.in( "klass", query.getKlass() ) );
        }
        else
        {
            Disjunction disjunction = Restrictions.disjunction();

            if ( !query.getUid().isEmpty() )
            {
                Conjunction conjunction = Restrictions.conjunction();
                conjunction.add( Restrictions.in( "klass", query.getKlass() ) );
                conjunction.add( Restrictions.in( "uid", query.getUid() ) );
                disjunction.add( conjunction );
            }

            if ( !query.getCode().isEmpty() )
            {
                Conjunction conjunction = Restrictions.conjunction();
                conjunction.add( Restrictions.in( "klass", query.getKlass() ) );
                conjunction.add( Restrictions.in( "code", query.getCode() ) );
                disjunction.add( conjunction );
            }

            criteria.add( disjunction );
        }

        if ( query.getCreatedAt() != null )
        {
            criteria.add( Restrictions.ge( "createdAt", query.getCreatedAt() ) );
        }

        if ( query.getCreatedBy() != null )
        {
            criteria.add( Restrictions.eq( "createdBy", query.getCreatedBy() ) );
        }

        if ( query.getType() != null )
        {
            criteria.add( Restrictions.eq( "type", query.getType() ) );
        }

        return criteria;
    }

    private Session getCurrentSession()
    {
        return sessionFactory.getCurrentSession();
    }
}
