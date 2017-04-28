package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.hibernate.Session;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Order( 0 )
public class IdentifiableObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public void preCreate( IdentifiableObject identifiableObject, ObjectBundle bundle )
    {
        ((BaseIdentifiableObject) identifiableObject).setAutoFields();

        Schema schema = schemaService.getDynamicSchema( identifiableObject.getClass() );
        Session session = sessionFactory.getCurrentSession();
        handleAttributeValues( session, identifiableObject, bundle, schema );
        handleUserGroupAccesses( session, identifiableObject, bundle, schema );
        handleUserAccesses( session, identifiableObject, bundle, schema );
        handleObjectTranslations( session, identifiableObject, bundle, schema );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        ((BaseIdentifiableObject) object).setAutoFields();

        Schema schema = schemaService.getDynamicSchema( object.getClass() );
        Session session = sessionFactory.getCurrentSession();
        handleAttributeValues( session, object, bundle, schema );
        handleUserGroupAccesses( session, object, bundle, schema );
        handleUserAccesses( session, object, bundle, schema );
        handleObjectTranslations( session, object, bundle, schema );
    }

    private void handleAttributeValues( Session session, IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "attributeValues" ) ) return;

        Iterator<AttributeValue> iterator = identifiableObject.getAttributeValues().iterator();

        while ( iterator.hasNext() )
        {
            AttributeValue attributeValue = iterator.next();

            // if value null or empty, just skip it
            if ( StringUtils.isEmpty( attributeValue.getValue() ) )
            {
                iterator.remove();
                continue;
            }

            Attribute attribute = bundle.getPreheat().get( bundle.getPreheatIdentifier(), attributeValue.getAttribute() );

            if ( attribute == null )
            {
                iterator.remove();
                continue;
            }

            attributeValue.setAttribute( attribute );
            session.save( attributeValue );
            session.flush();
        }
    }

    private void handleUserGroupAccesses( Session session, IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "userGroupAccesses" ) ) return;

        if ( bundle.isSkipSharing() )
        {
            identifiableObject.getUserGroupAccesses().clear();
            return;
        }

        Iterator<UserGroupAccess> userGroupAccessIterator = identifiableObject.getUserGroupAccesses().iterator();

        while ( userGroupAccessIterator.hasNext() )
        {
            UserGroupAccess userGroupAccess = userGroupAccessIterator.next();
            UserGroup userGroup = bundle.getPreheat().get( bundle.getPreheatIdentifier(), userGroupAccess.getUserGroup() );

            if ( userGroup != null )
            {
                userGroupAccess.setUserGroup( userGroup );
                session.save( userGroupAccess );
            }
            else
            {
                userGroupAccessIterator.remove();
            }
        }
    }

    private void handleUserAccesses( Session session, IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "userAccesses" ) ) return;

        if ( bundle.isSkipSharing() )
        {
            identifiableObject.getUserAccesses().clear();
            return;
        }

        Iterator<UserAccess> userAccessIterator = identifiableObject.getUserAccesses().iterator();

        while ( userAccessIterator.hasNext() )
        {
            UserAccess userAccess = userAccessIterator.next();
            User user = bundle.getPreheat().get( bundle.getPreheatIdentifier(), userAccess.getUser() );

            if ( user != null )
            {
                userAccess.setUser( user );
                session.save( userAccess );
            }
            else
            {
                userAccessIterator.remove();
            }
        }
    }

    private void handleObjectTranslations( Session session, IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema )
    {
        if ( !schema.havePersistedProperty( "translations" ) ) return;
        identifiableObject.getTranslations().forEach( session::save );
    }
}
