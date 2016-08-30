package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UserObjectBundleHook extends AbstractObjectBundleHook
{
    @Autowired
    private UserService userService;

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( object ) || ((User) object).getUserCredentials() == null ) return;

        User user = (User) object;
        bundle.putExtras( user, "uc", user.getUserCredentials() );
        user.setUserCredentials( null );
    }

    @Override
    public void postCreate( IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( persistedObject ) || !bundle.hasExtras( persistedObject, "uc" ) ) return;

        User user = (User) persistedObject;
        final UserCredentials userCredentials = (UserCredentials) bundle.getExtras( persistedObject, "uc" );

        if ( !StringUtils.isEmpty( userCredentials.getPassword() ) )
        {
            userService.encodeAndSetPassword( userCredentials, userCredentials.getPassword() );
        }

        preheatService.connectReferences( userCredentials, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        sessionFactory.getCurrentSession().save( userCredentials );
        user.setUserCredentials( userCredentials );
        sessionFactory.getCurrentSession().update( user );
        bundle.removeExtras( persistedObject, "uc" );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( object ) || ((User) object).getUserCredentials() == null ) return;
        User user = (User) object;
        bundle.putExtras( user, "uc", user.getUserCredentials() );
    }

    @Override
    public void postUpdate( IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( persistedObject ) || !bundle.hasExtras( persistedObject, "uc" ) ) return;

        User user = (User) persistedObject;
        final UserCredentials userCredentials = (UserCredentials) bundle.getExtras( persistedObject, "uc" );
        final UserCredentials persistedUserCredentials = bundle.getPreheat().get( bundle.getPreheatIdentifier(), UserCredentials.class, user );

        if ( !StringUtils.isEmpty( userCredentials.getPassword() ) )
        {
            userService.encodeAndSetPassword( userCredentials, userCredentials.getPassword() );
        }

        persistedUserCredentials.mergeWith( userCredentials, bundle.getMergeMode() );
        preheatService.connectReferences( persistedUserCredentials, bundle.getPreheat(), bundle.getPreheatIdentifier() );

        persistedUserCredentials.setUserInfo( user );
        user.setUserCredentials( persistedUserCredentials );

        sessionFactory.getCurrentSession().update( user.getUserCredentials() );
        bundle.removeExtras( persistedObject, "uc" );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void postImport( ObjectBundle bundle )
    {
        if ( !bundle.getObjectMap().containsKey( User.class ) ) return;

        List<IdentifiableObject> objects = bundle.getObjectMap().get( User.class );
        Map<String, Map<String, Object>> userReferences = bundle.getObjectReferences( User.class );
        Map<String, Map<String, Object>> userCredentialsReferences = bundle.getObjectReferences( UserCredentials.class );

        if ( userReferences == null || userReferences.isEmpty() || userCredentialsReferences == null || userCredentialsReferences.isEmpty() )
        {
            return;
        }

        for ( IdentifiableObject identifiableObject : objects )
        {
            identifiableObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );
            Map<String, Object> userReferenceMap = userReferences.get( identifiableObject.getUid() );

            if ( userReferenceMap == null || userReferenceMap.isEmpty() )
            {
                continue;
            }

            User user = (User) identifiableObject;
            UserCredentials userCredentials = user.getUserCredentials();

            if ( userCredentials == null )
            {
                continue;
            }

            Map<String, Object> userCredentialsReferenceMap = userCredentialsReferences.get( userCredentials.getUid() );

            if ( userCredentialsReferenceMap == null || userCredentialsReferenceMap.isEmpty() )
            {
                continue;
            }

            user.setOrganisationUnits( (Set<OrganisationUnit>) userReferenceMap.get( "organisationUnits" ) );
            user.setDataViewOrganisationUnits( (Set<OrganisationUnit>) userReferenceMap.get( "dataViewOrganisationUnits" ) );
            userCredentials.setUser( (User) userCredentialsReferenceMap.get( "user" ) );
            userCredentials.setUserInfo( user );

            preheatService.connectReferences( user, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            preheatService.connectReferences( userCredentials, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            user.setUserCredentials( userCredentials );
            sessionFactory.getCurrentSession().update( user );
        }
    }
}
