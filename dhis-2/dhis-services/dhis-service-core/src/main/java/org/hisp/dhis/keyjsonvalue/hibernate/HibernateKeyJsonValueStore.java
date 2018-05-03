package org.hisp.dhis.keyjsonvalue.hibernate;

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

import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueStore;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Date;
import java.util.List;

/**
 * @author Stian Sandvold
 */
public class HibernateKeyJsonValueStore
    extends HibernateIdentifiableObjectStore<KeyJsonValue>
    implements KeyJsonValueStore
{
    @Override
    public List<String> getNamespaces()
    {
        String hql = "select distinct namespace from KeyJsonValue";
        
        return getQuery( hql, String.class ).list();
    }

    @Override
    public List<String> getKeysInNamespace( String namespace )
    {
        String hql = "select key from KeyJsonValue where namespace = :namespace";

        return getQuery( hql, String.class ).setParameter( "namespace", namespace ).list();
    }

    @Override
    public List<String> getKeysInNamespace( String namespace, Date lastUpdated )
    {
        String hql = "select key from KeyJsonValue where namespace = :namespace";
        
        if ( lastUpdated != null )
        {
            hql += " and lastupdated >= :lastUpdated ";
        }
        
        Query<String> query = getQuery( hql, String.class ).setParameter( "namespace", namespace );
        
        if ( lastUpdated != null )
        {
            query.setParameter( "lastUpdated", lastUpdated );
        }

        return query.list();
    }

    @Override
    public List<KeyJsonValue> getKeyJsonValueByNamespace( String namespace )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters().addPredicate( root -> builder.equal( root.get( "namespace" ), namespace ) ) );
    }

    @Override
    public KeyJsonValue getKeyJsonValue( String namespace, String key )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getSingleResult( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "namespace" ), namespace ) )
            .addPredicate( root -> builder.equal( root.get( "key" ), key ) ) );
    }
}
