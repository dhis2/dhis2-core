package org.hisp.dhis.webapi.controller;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserGroupAccessService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.sharing.Sharing;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserGroupAccess;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserGroups;
import org.hisp.dhis.webapi.webdomain.sharing.comparator.SharingUserGroupAccessNameComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SharingController.RESOURCE_PATH, method = RequestMethod.GET )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class SharingController
{
    private static final Log log = LogFactory.getLog( SharingController.class );

    public static final String RESOURCE_PATH = "/sharing";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private UserGroupAccessService userGroupAccessService;

    @Autowired
    private AclService aclService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    @RequestMapping( method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public void getSharing( @RequestParam String type, @RequestParam String id, HttpServletResponse response ) throws IOException, WebMessageException
    {
        if ( !aclService.isShareable( type ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Type " + type + " is not supported." ) );
        }

        Class<? extends IdentifiableObject> klass = aclService.classForType( type );
        IdentifiableObject object = manager.get( klass, id );

        if ( object == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Object of type " + type + " with ID " + id + " was not found." ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canManage( user, object ) )
        {
            throw new AccessDeniedException( "You do not have manage access to this object." );
        }

        Sharing sharing = new Sharing();

        sharing.getMeta().setAllowPublicAccess( aclService.canCreatePublic( user, object.getClass() ) );
        sharing.getMeta().setAllowExternalAccess( aclService.canExternalize( user, object.getClass() ) );

        sharing.getObject().setId( object.getUid() );
        sharing.getObject().setName( object.getDisplayName() );
        sharing.getObject().setDisplayName( object.getDisplayName() );
        sharing.getObject().setExternalAccess( object.getExternalAccess() );

        if ( object.getPublicAccess() == null )
        {
            String access;

            if ( aclService.canCreatePublic( user, klass ) )
            {
                access = AccessStringHelper.newInstance().enable( AccessStringHelper.Permission.READ ).enable( AccessStringHelper.Permission.WRITE ).build();
            }
            else
            {
                access = AccessStringHelper.newInstance().build();
            }

            sharing.getObject().setPublicAccess( access );
        }
        else
        {
            sharing.getObject().setPublicAccess( object.getPublicAccess() );
        }

        if ( object.getUser() != null )
        {
            sharing.getObject().getUser().setId( object.getUser().getUid() );
            sharing.getObject().getUser().setName( object.getUser().getDisplayName() );
        }

        for ( UserGroupAccess userGroupAccess : object.getUserGroupAccesses() )
        {
            SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();
            sharingUserGroupAccess.setId( userGroupAccess.getUserGroup().getUid() );
            sharingUserGroupAccess.setName( userGroupAccess.getUserGroup().getDisplayName() );
            sharingUserGroupAccess.setDisplayName( userGroupAccess.getUserGroup().getDisplayName() );
            sharingUserGroupAccess.setAccess( userGroupAccess.getAccess() );

            sharing.getObject().getUserGroupAccesses().add( sharingUserGroupAccess );
        }

        Collections.sort( sharing.getObject().getUserGroupAccesses(), SharingUserGroupAccessNameComparator.INSTANCE );

        renderService.toJson( response.getOutputStream(), sharing );
    }

    @RequestMapping( method = { RequestMethod.POST, RequestMethod.PUT }, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void setSharing( @RequestParam String type, @RequestParam String id, HttpServletResponse response, HttpServletRequest request ) throws IOException, WebMessageException
    {
        Class<? extends IdentifiableObject> sharingClass = aclService.classForType( type );

        if ( sharingClass == null || !aclService.isShareable( sharingClass ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Type " + type + " is not supported." ) );
        }

        BaseIdentifiableObject object = (BaseIdentifiableObject) manager.get( sharingClass, id );

        if ( object == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Object of type " + type + " with ID " + id + " was not found." ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canManage( user, object ) )
        {
            throw new AccessDeniedException( "You do not have manage access to this object." );
        }

        Sharing sharing = renderService.fromJson( request.getInputStream(), Sharing.class );

        // Ignore externalAccess if user is not allowed to make objects external
        if ( aclService.canExternalize( user, object.getClass() ) )
        {
            object.setExternalAccess( sharing.getObject().hasExternalAccess() );
        }

        // Ignore publicAccess if user is not allowed to make objects public
        if ( aclService.canCreatePublic( user, object.getClass() ) )
        {
            object.setPublicAccess( sharing.getObject().getPublicAccess() );
        }

        if ( object.getUser() == null )
        {
            object.setUser( user );
        }

        Iterator<UserGroupAccess> iterator = object.getUserGroupAccesses().iterator();

        while ( iterator.hasNext() )
        {
            UserGroupAccess userGroupAccess = iterator.next();
            iterator.remove();

            userGroupAccessService.deleteUserGroupAccess( userGroupAccess );
        }

        for ( SharingUserGroupAccess sharingUserGroupAccess : sharing.getObject().getUserGroupAccesses() )
        {
            UserGroupAccess userGroupAccess = new UserGroupAccess();
            userGroupAccess.setAccess( sharingUserGroupAccess.getAccess() );

            UserGroup userGroup = manager.get( UserGroup.class, sharingUserGroupAccess.getId() );

            if ( userGroup != null )
            {
                userGroupAccess.setUserGroup( userGroup );
                userGroupAccessService.addUserGroupAccess( userGroupAccess );

                object.getUserGroupAccesses().add( userGroupAccess );
            }
        }

        manager.updateNoAcl( object );

        StringBuilder builder = new StringBuilder();

        builder.append( "'" ).append( currentUserService.getCurrentUsername() ).append( "'" );
        builder.append( " update sharing on " ).append( object.getClass().getName() );
        builder.append( ", uid: " ).append( object.getUid() ).append( ", name: " ).append( object.getName() );
        builder.append( ", publicAccess: " ).append( object.getPublicAccess() );
        builder.append( ", externalAccess: " ).append( object.getExternalAccess() );

        if ( !object.getUserGroupAccesses().isEmpty() )
        {
            builder.append( ", userGroupAccesses: " );

            for ( UserGroupAccess userGroupAccess : object.getUserGroupAccesses() )
            {
                builder.append( "{ uid: " ).append( userGroupAccess.getUserGroup().getUid() );
                builder.append( ", name: " ).append( userGroupAccess.getUserGroup().getName() );
                builder.append( ", access: " ).append( userGroupAccess.getAccess() );
                builder.append( " } " );
            }
        }

        log.info( builder );

        webMessageService.send( WebMessageUtils.ok( "Access control set" ), response, request );
    }

    @RequestMapping( value = "/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public void searchUserGroups( @RequestParam String key, @RequestParam( required = false ) Integer pageSize,
        HttpServletResponse response ) throws IOException, WebMessageException
    {
        if ( key == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Search key not specified" ) );
        }

        int max = pageSize != null ? pageSize : Integer.MAX_VALUE;

        SharingUserGroups sharingUserGroups = new SharingUserGroups();

        List<UserGroup> userGroups = userGroupService.getUserGroupsBetweenByName( key, 0, max );

        for ( UserGroup userGroup : userGroups )
        {
            SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();

            sharingUserGroupAccess.setId( userGroup.getUid() );
            sharingUserGroupAccess.setName( userGroup.getDisplayName() );
            sharingUserGroupAccess.setDisplayName( userGroup.getDisplayName() );

            sharingUserGroups.getUserGroups().add( sharingUserGroupAccess );
        }

        renderService.toJson( response.getOutputStream(), sharingUserGroups );
    }
}
