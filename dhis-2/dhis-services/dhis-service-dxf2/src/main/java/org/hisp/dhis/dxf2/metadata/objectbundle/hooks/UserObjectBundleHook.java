/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class UserObjectBundleHook
    extends AbstractObjectBundleHook
{
    private final UserService userService;

    private final FileResourceService fileResourceService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    public UserObjectBundleHook( UserService userService, FileResourceService fileResourceService,
        CurrentUserService currentUserService, AclService aclService )
    {
        checkNotNull( userService );
        checkNotNull( fileResourceService );
        checkNotNull( currentUserService );
        checkNotNull( aclService );
        this.userService = userService;
        this.fileResourceService = fileResourceService;
        this.currentUserService = currentUserService;
        this.aclService = aclService;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( !(object instanceof User) )
        {
            return new ArrayList<>();
        }

        ArrayList<ErrorReport> errorReports = new ArrayList<>();
        User user = (User) object;

        if ( user.getWhatsApp() != null && !ValidationUtils.validateWhatsapp( user.getWhatsApp() ) )
        {
            errorReports.add( new ErrorReport( User.class, ErrorCode.E4027, user.getWhatsApp(), "Whatsapp" ) );
        }

        return errorReports;
    }

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( object ) || ((User) object).getUserCredentials() == null )
            return;

        User user = (User) object;

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            user.getUserCredentials().getCogsDimensionConstraints().addAll(
                currentUser.getUserCredentials().getCogsDimensionConstraints() );

            user.getUserCredentials().getCatDimensionConstraints().addAll(
                currentUser.getUserCredentials().getCatDimensionConstraints() );
        }

        bundle.putExtras( user, "uc", user.getUserCredentials() );
        user.setUserCredentials( null );
    }

    @Override
    public void postCreate( IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( persistedObject ) || !bundle.hasExtras( persistedObject, "uc" ) )
            return;

        User user = (User) persistedObject;
        final UserCredentials userCredentials = (UserCredentials) bundle.getExtras( persistedObject, "uc" );

        if ( !StringUtils.isEmpty( userCredentials.getPassword() ) )
        {
            userService.encodeAndSetPassword( userCredentials, userCredentials.getPassword() );
        }

        if ( user.getAvatar() != null )
        {
            FileResource fileResource = fileResourceService.getFileResource( user.getAvatar().getUid() );
            fileResource.setAssigned( true );
            fileResourceService.updateFileResource( fileResource );
        }

        userCredentials.setUserInfo( user );
        preheatService.connectReferences( userCredentials, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        sessionFactory.getCurrentSession().save( userCredentials );
        user.setUserCredentials( userCredentials );
        sessionFactory.getCurrentSession().update( user );
        bundle.removeExtras( persistedObject, "uc" );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( object ) || ((User) object).getUserCredentials() == null )
            return;
        User user = (User) object;
        bundle.putExtras( user, "uc", user.getUserCredentials() );

        User persisted = (User) persistedObject;

        if ( persisted.getAvatar() != null
            && (user.getAvatar() == null || !persisted.getAvatar().getUid().equals( user.getAvatar().getUid() )) )
        {
            FileResource fileResource = fileResourceService.getFileResource( persisted.getAvatar().getUid() );
            fileResourceService.updateFileResource( fileResource );

            if ( user.getAvatar() != null )
            {
                fileResource = fileResourceService.getFileResource( user.getAvatar().getUid() );
                fileResource.setAssigned( true );
                fileResourceService.updateFileResource( fileResource );
            }
        }
    }

    @Override
    public void postUpdate( IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !User.class.isInstance( persistedObject ) || !bundle.hasExtras( persistedObject, "uc" ) )
            return;

        User user = (User) persistedObject;
        final UserCredentials userCredentials = (UserCredentials) bundle.getExtras( persistedObject, "uc" );
        final UserCredentials persistedUserCredentials = bundle.getPreheat().get( bundle.getPreheatIdentifier(),
            UserCredentials.class, user );

        if ( !StringUtils.isEmpty( userCredentials.getPassword() ) )
        {
            userService.encodeAndSetPassword( persistedUserCredentials, userCredentials.getPassword() );
        }

        mergeService.merge(
            new MergeParams<>( userCredentials, persistedUserCredentials ).setMergeMode( bundle.getMergeMode() ) );
        preheatService.connectReferences( persistedUserCredentials, bundle.getPreheat(),
            bundle.getPreheatIdentifier() );

        persistedUserCredentials.setUserInfo( user );
        user.setUserCredentials( persistedUserCredentials );

        sessionFactory.getCurrentSession().update( user.getUserCredentials() );
        bundle.removeExtras( persistedObject, "uc" );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void postCommit( ObjectBundle bundle )
    {
        if ( !bundle.getObjectMap().containsKey( User.class ) )
            return;

        List<IdentifiableObject> objects = bundle.getObjectMap().get( User.class );
        Map<String, Map<String, Object>> userReferences = bundle.getObjectReferences( User.class );
        Map<String, Map<String, Object>> userCredentialsReferences = bundle
            .getObjectReferences( UserCredentials.class );

        if ( userReferences == null || userReferences.isEmpty() || userCredentialsReferences == null
            || userCredentialsReferences.isEmpty() )
        {
            return;
        }

        for ( IdentifiableObject identifiableObject : objects )
        {
            User user = (User) identifiableObject;
            handleNoAccessRoles( user, bundle );

            user = bundle.getPreheat().get( bundle.getPreheatIdentifier(), user );
            Map<String, Object> userReferenceMap = userReferences.get( identifiableObject.getUid() );

            if ( userReferenceMap == null || userReferenceMap.isEmpty() )
            {
                continue;
            }

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
            user.setDataViewOrganisationUnits(
                (Set<OrganisationUnit>) userReferenceMap.get( "dataViewOrganisationUnits" ) );
            userCredentials.setUser( (User) userCredentialsReferenceMap.get( "user" ) );
            userCredentials.setUserInfo( user );

            if ( userCredentials.getUser() == null )
            {
                userCredentials.setUser( bundle.getUser() );
            }

            userCredentials.setLastUpdatedBy( bundle.getUser() );

            preheatService.connectReferences( user, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            preheatService.connectReferences( userCredentials, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            user.setUserCredentials( userCredentials );
            sessionFactory.getCurrentSession().update( user );
        }
    }

    /**
     * If currentUser doesn't have read access to a UserRole and it is included
     * in the payload, then that UserRole should not be removed from updating
     * User.
     *
     * @param user the updating User.
     * @param bundle the ObjectBundle.
     */
    private void handleNoAccessRoles( User user, ObjectBundle bundle )
    {
        Set<String> preHeatedRoles = bundle.getPreheat().get( PreheatIdentifier.UID, user )
            .getUserCredentials().getUserAuthorityGroups().stream().map( role -> role.getUid() )
            .collect( Collectors.toSet() );

        user.getUserCredentials().getUserAuthorityGroups().stream()
            .filter( role -> !preHeatedRoles.contains( role.getUid() ) )
            .forEach( role -> {
                UserAuthorityGroup persistedRole = bundle.getPreheat().get( PreheatIdentifier.UID, role );

                if ( persistedRole == null )
                {
                    persistedRole = manager.getNoAcl( UserAuthorityGroup.class, role.getUid() );
                }

                if ( !aclService.canRead( bundle.getUser(), persistedRole ) )
                {
                    bundle.getPreheat().get( PreheatIdentifier.UID, user ).getUserCredentials().getUserAuthorityGroups()
                        .add( persistedRole );
                    bundle.getPreheat().put( PreheatIdentifier.UID, persistedRole );
                }
            } );
    }
}
