package org.hisp.dhis.validation.hibernate;
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
 */

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;

import java.util.List;

/**
 * @author Stian Sandvold
 */
public class HibernateValidationResultStore
    extends HibernateGenericStore<ValidationResult>
    implements ValidationResultStore
{
    @Override
    @SuppressWarnings( "unchecked" )
    public List<ValidationResult> getAllUnreportedValidationResults()
    {
        return getQuery( "from ValidationResult where notificationSent is false" ).list();
    }

    @Override
    public ValidationResult getById( int id )
    {
        return (ValidationResult) getQuery( "from ValidationResult where id = :id" ).setInteger( "id", id )
            .uniqueResult();
    }

    @Override
    public List<ValidationResult> query( ValidationResultQuery query )
    {

        Criteria criteria = getCurrentSession().createCriteria( ValidationResult.class );

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            criteria.setFirstResult( pager.getOffset() );
            criteria.setMaxResults( pager.getPageSize() );
        }

        return criteria.list();
    }

    @Override
    public int count( ValidationResultQuery query )
    {
        Criteria criteria = getCurrentSession().createCriteria( DeletedObject.class );

        return criteria.list().size();
    }

    private Session getCurrentSession()
    {
        return sessionFactory.getCurrentSession();
    }
}
