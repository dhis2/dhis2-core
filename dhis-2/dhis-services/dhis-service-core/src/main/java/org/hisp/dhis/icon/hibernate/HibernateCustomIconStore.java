/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.icon.hibernate;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hibernate.SessionFactory;
import org.hisp.dhis.icon.CustomIconStore;
import org.hisp.dhis.icon.IconData;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository( "org.hisp.dhis.icon.CustomIconStore" )
@RequiredArgsConstructor
public class HibernateCustomIconStore
    implements CustomIconStore
{

    private final SessionFactory sessionFactory;

    @Override
    public void save( IconData iconData )
    {
        sessionFactory.getCurrentSession().save( iconData );
    }

    @Override
    public void delete( IconData iconData )
    {
        sessionFactory.getCurrentSession().delete( iconData );
    }

    @Override
    public void update( IconData iconData )
    {
        sessionFactory.getCurrentSession().update( iconData );
    }

    @Override
    @Transactional( readOnly = true )
    public List<IconData> getAllIcons()
    {
        String hql = "from IconData";
        return sessionFactory.getCurrentSession().createQuery( hql, IconData.class ).getResultList();
    }

    @Override
    @Transactional( readOnly = true )
    public IconData getIconByKey( String key )
    {
        String hql = "from IconData where key = :key";
        return sessionFactory.getCurrentSession().createQuery( hql, IconData.class ).setParameter( "key", key )
            .getResultList().stream().findFirst().orElse( null );
    }

    @Override
    public List<IconData> getIconsByKeywords( Collection<String> keywords )
    {
        String hql = "select i from IconData i join i.keywords k where k in (:keywordList) group by i.id having count(distinct k) = :numKeywords";
        return sessionFactory.getCurrentSession().createQuery( hql, IconData.class )
            .setParameter( "keywordList", keywords )
            .setParameter( "numKeywords", (long) keywords.size() ).getResultList();
    }

    @Override
    public List<String> getKeywords()
    {
        String hql = "select distinct k from IconData i join i.keywords k";
        return sessionFactory.getCurrentSession().createQuery( hql, String.class ).getResultList();
    }
}