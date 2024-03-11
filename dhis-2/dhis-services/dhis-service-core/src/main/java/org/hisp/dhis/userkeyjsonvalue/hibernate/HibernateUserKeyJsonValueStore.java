package org.hisp.dhis.userkeyjsonvalue.hibernate;

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

import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.user.User;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValue;
import org.hisp.dhis.userkeyjsonvalue.UserKeyJsonValueStore;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Stian Sandvold
 */
public class HibernateUserKeyJsonValueStore
    extends HibernateIdentifiableObjectStore<UserKeyJsonValue>
    implements UserKeyJsonValueStore
{

    @Override
    public UserKeyJsonValue getUserKeyJsonValue( User user, String namespace, String key )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getSingleResult( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "user" ), user ) )
            .addPredicate( root -> builder.equal( root.get( "namespace" ), namespace ) )
            .addPredicate( root -> builder.equal( root.get( "key" ), key ) ) );
    }

    @Override
    public List<String> getNamespacesByUser( User user )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "user" ), user ) ) )
            .stream().map( UserKeyJsonValue::getNamespace  ).distinct().collect( Collectors.toList() );
    }

    @Override
    public List<String> getKeysByUserAndNamespace( User user, String namespace )
    {
        return (getUserKeyJsonValueByUserAndNamespace( user, namespace )).stream().map( UserKeyJsonValue::getKey )
            .collect( Collectors.toList() );
    }

    @Override
    public List<UserKeyJsonValue> getUserKeyJsonValueByUserAndNamespace( User user, String namespace )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "user" ), user ) )
            .addPredicate( root -> builder.equal( root.get( "namespace" ), namespace ) ) );
    }
}
