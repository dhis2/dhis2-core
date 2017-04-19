package org.hisp.dhis.deletedobject.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectStore;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateDeletedObjectStore
    implements DeletedObjectStore
{
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public int save( DeletedObject deletedObject )
    {
        return (int) getCurrentSession().save( deletedObject );
    }

    @Override
    public void delete( DeletedObject deletedObject )
    {
        getCurrentSession().delete( deletedObject );
    }

    @Override
    public void delete( DeletedObjectQuery query )
    {
        query.setSkipPaging( false );
        query( query ).forEach( this::delete );
    }

    @Override
    public List<DeletedObject> getByKlass( String klass )
    {
        DeletedObjectQuery query = new DeletedObjectQuery();
        query.getKlass().add( klass );

        return query( query );
    }

    @Override
    public int count( DeletedObjectQuery query )
    {
        Criteria criteria = getCurrentSession().createCriteria( DeletedObject.class );

        if ( !query.getKlass().isEmpty() )
        {
            criteria.add( Restrictions.in( "klass", query.getKlass() ) );
        }

        if ( query.getDeletedAt() != null )
        {
            criteria.add( Restrictions.ge( "deletedAt", query.getDeletedAt() ) );
        }

        return criteria.list().size();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DeletedObject> query( DeletedObjectQuery query )
    {
        Criteria criteria = getCurrentSession().createCriteria( DeletedObject.class );

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

        if ( query.getDeletedAt() != null )
        {
            criteria.add( Restrictions.ge( "deletedAt", query.getDeletedAt() ) );
        }

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            criteria.setFirstResult( pager.getOffset() );
            criteria.setMaxResults( pager.getPageSize() );
        }

        return criteria.list();
    }

    private Session getCurrentSession()
    {
        return sessionFactory.getCurrentSession();
    }
}
