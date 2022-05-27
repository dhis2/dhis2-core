/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.CacheControl.noCache;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.util.SharingUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.sharing.Sharing;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserAccess;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserGroupAccess;
import org.hisp.dhis.webapi.webdomain.sharing.comparator.SharingUserGroupAccessNameComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SharingController.RESOURCE_PATH )
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SharingController
{
    public static final String RESOURCE_PATH = "/sharing";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SchemaService schemaService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<Sharing> getSharing( @RequestParam String type, @RequestParam String id )
        throws WebMessageException
    {
        if ( !aclService.isShareable( type ) )
        {
            throw new WebMessageException( conflict( "Type " + type + " is not supported." ) );
        }

        Class<? extends IdentifiableObject> klass = aclService.classForType( type );
        IdentifiableObject object = manager.getNoAcl( klass, id );

        if ( object == null )
        {
            throw new WebMessageException(
                notFound( "Object of type " + type + " with ID " + id + " was not found." ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canRead( user, object ) )
        {
            throw new AccessDeniedException( "You do not have manage access to this object." );
        }

        Sharing sharing = new Sharing();

        sharing.getMeta().setAllowPublicAccess( aclService.canMakePublic( user, object ) );
        sharing.getMeta().setAllowExternalAccess( aclService.canMakeExternal( user, object ) );

        sharing.getObject().setId( object.getUid() );
        sharing.getObject().setName( object.getDisplayName() );
        sharing.getObject().setDisplayName( object.getDisplayName() );
        sharing.getObject().setExternalAccess( object.getExternalAccess() );

        if ( object.getPublicAccess() == null )
        {
            String access;

            if ( aclService.canMakeClassPublic( user, klass ) )
            {
                access = AccessStringHelper.newInstance().enable( AccessStringHelper.Permission.READ )
                    .enable( AccessStringHelper.Permission.WRITE ).build();
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

        if ( object.getCreatedBy() != null )
        {
            sharing.getObject().getUser().setId( object.getCreatedBy().getUid() );
            sharing.getObject().getUser().setName( object.getCreatedBy().getDisplayName() );
        }

        for ( org.hisp.dhis.user.UserGroupAccess userGroupAccess : SharingUtils
            .getDtoUserGroupAccesses( object.getUserGroupAccesses(), object.getSharing() ) )
        {
            String userGroupDisplayName = userGroupService.getDisplayName( userGroupAccess.getId() );

            if ( userGroupDisplayName == null )
            {
                continue;
            }

            SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();
            sharingUserGroupAccess.setId( userGroupAccess.getId() );
            sharingUserGroupAccess.setName( userGroupDisplayName );
            sharingUserGroupAccess.setDisplayName( userGroupDisplayName );
            sharingUserGroupAccess.setAccess( userGroupAccess.getAccess() );

            sharing.getObject().getUserGroupAccesses().add( sharingUserGroupAccess );
        }

        for ( org.hisp.dhis.user.UserAccess userAccess : SharingUtils.getDtoUserAccesses( object.getUserAccesses(),
            object.getSharing() ) )
        {
            String userDisplayName = userService.getDisplayName( userAccess.getUid() );

            if ( userDisplayName == null )
                continue;

            SharingUserAccess sharingUserAccess = new SharingUserAccess();
            sharingUserAccess.setId( userAccess.getId() );
            sharingUserAccess.setName( userDisplayName );
            sharingUserAccess.setDisplayName( userDisplayName );
            sharingUserAccess.setAccess( userAccess.getAccess() );

            sharing.getObject().getUserAccesses().add( sharingUserAccess );
        }

        sharing.getObject().getUserGroupAccesses().sort( SharingUserGroupAccessNameComparator.INSTANCE );

        return ResponseEntity.ok().cacheControl( noCache() ).body( sharing );
    }

    @PutMapping( consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage putSharing( @RequestParam String type, @RequestParam String id,
        HttpServletRequest request )
        throws Exception
    {
        return postSharing( type, id, request );
    }

    @PostMapping( consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage postSharing( @RequestParam String type, @RequestParam String id, HttpServletRequest request )
        throws Exception
    {
        Class<? extends IdentifiableObject> sharingClass = aclService.classForType( type );

        if ( sharingClass == null || !aclService.isClassShareable( sharingClass ) )
        {
            return conflict( "Type " + type + " is not supported." );
        }

        BaseIdentifiableObject object = (BaseIdentifiableObject) manager.getNoAcl( sharingClass, id );

        if ( object == null )
        {
            return notFound( "Object of type " + type + " with ID " + id + " was not found." );
        }

        if ( (object instanceof SystemDefaultMetadataObject) && ((SystemDefaultMetadataObject) object).isDefault() )
        {
            return conflict(
                "Sharing settings of system default metadata object of type " + type + " cannot be modified." );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canManage( user, object ) )
        {
            throw new AccessDeniedException( "You do not have manage access to this object." );
        }

        Sharing sharing = renderService.fromJson( request.getInputStream(), Sharing.class );

        if ( !AccessStringHelper.isValid( sharing.getObject().getPublicAccess() ) )
        {
            return conflict( "Invalid public access string: " + sharing.getObject().getPublicAccess() );
        }

        // ---------------------------------------------------------------------
        // Ignore externalAccess if user is not allowed to make objects external
        // ---------------------------------------------------------------------

        if ( aclService.canMakeExternal( user, object ) )
        {
            object.setExternalAccess( sharing.getObject().hasExternalAccess() );
        }

        // ---------------------------------------------------------------------
        // Ignore publicAccess if user is not allowed to make objects public
        // ---------------------------------------------------------------------

        Schema schema = schemaService.getDynamicSchema( sharingClass );

        if ( aclService.canMakePublic( user, object ) )
        {
            object.setPublicAccess( sharing.getObject().getPublicAccess() );
        }

        if ( !schema.isDataShareable() )
        {
            if ( AccessStringHelper.hasDataSharing( object.getSharing().getPublicAccess() ) )
            {
                object.getSharing()
                    .setPublicAccess( AccessStringHelper.disableDataSharing( object.getSharing().getPublicAccess() ) );
            }
        }

        if ( object.getCreatedBy() == null )
        {
            object.setCreatedBy( user );
        }

        object.getSharing().getUserGroups().clear();

        for ( SharingUserGroupAccess sharingUserGroupAccess : sharing.getObject().getUserGroupAccesses() )
        {
            UserGroupAccess userGroupAccess = new UserGroupAccess();

            if ( !AccessStringHelper.isValid( sharingUserGroupAccess.getAccess() ) )
            {
                return conflict( "Invalid user group access string: " + sharingUserGroupAccess.getAccess() );
            }

            if ( !schema.isDataShareable() )
            {
                if ( AccessStringHelper.hasDataSharing( sharingUserGroupAccess.getAccess() ) )
                {
                    sharingUserGroupAccess
                        .setAccess( AccessStringHelper.disableDataSharing( sharingUserGroupAccess.getAccess() ) );
                }
            }

            userGroupAccess.setAccess( sharingUserGroupAccess.getAccess() );

            UserGroup userGroup = manager.get( UserGroup.class, sharingUserGroupAccess.getId() );

            if ( userGroup != null )
            {
                userGroupAccess.setUserGroup( userGroup );
                object.getSharing().addUserGroupAccess( userGroupAccess );
            }
        }

        object.getSharing().getUsers().clear();

        for ( SharingUserAccess sharingUserAccess : sharing.getObject().getUserAccesses() )
        {
            UserAccess userAccess = new UserAccess();

            if ( !AccessStringHelper.isValid( sharingUserAccess.getAccess() ) )
            {
                return conflict( "Invalid user access string: " + sharingUserAccess.getAccess() );
            }

            if ( !schema.isDataShareable() )
            {
                if ( AccessStringHelper.hasDataSharing( sharingUserAccess.getAccess() ) )
                {
                    sharingUserAccess
                        .setAccess( AccessStringHelper.disableDataSharing( sharingUserAccess.getAccess() ) );
                }
            }

            userAccess.setAccess( sharingUserAccess.getAccess() );

            User sharingUser = manager.get( User.class, sharingUserAccess.getId() );

            if ( sharingUser != null )
            {
                userAccess.setUser( sharingUser );
                object.getSharing().addUserAccess( userAccess );
            }
        }

        manager.updateNoAcl( object );

        if ( Program.class.isInstance( object ) )
        {
            syncSharingForEventProgram( (Program) object );
        }

        log.info( sharingToString( object ) );

        return ok( "Access control set" );
    }

    @GetMapping( value = "/search", produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<Map<String, Object>> searchUserGroups( @RequestParam String key,
        @RequestParam( required = false ) Integer pageSize )
        throws WebMessageException
    {
        if ( key == null )
        {
            throw new WebMessageException( conflict( "Search key not specified" ) );
        }

        int max = pageSize != null ? pageSize : Pager.DEFAULT_PAGE_SIZE;

        List<SharingUserGroupAccess> userGroupAccesses = getSharingUserGroups( key, max );
        List<SharingUserAccess> userAccesses = getSharingUser( key, max );

        Map<String, Object> output = new HashMap<>();
        output.put( "userGroups", userGroupAccesses );
        output.put( "users", userAccesses );

        return ResponseEntity.ok().cacheControl( noCache() ).body( output );
    }

    private List<SharingUserAccess> getSharingUser( String key, int max )
    {
        List<SharingUserAccess> sharingUsers = new ArrayList<>();
        List<User> users = userService.getAllUsersBetweenByName( key, 0, max );

        for ( User user : users )
        {
            SharingUserAccess sharingUserAccess = new SharingUserAccess();
            sharingUserAccess.setId( user.getUid() );
            sharingUserAccess.setName( user.getDisplayName() );
            sharingUserAccess.setDisplayName( user.getDisplayName() );
            sharingUserAccess.setUsername( user.getUsername() );

            sharingUsers.add( sharingUserAccess );
        }

        return sharingUsers;
    }

    private List<SharingUserGroupAccess> getSharingUserGroups( @RequestParam String key, int max )
    {
        List<SharingUserGroupAccess> sharingUserGroupAccesses = new ArrayList<>();
        List<UserGroup> userGroups = userGroupService.getUserGroupsBetweenByName( key, 0, max );

        for ( UserGroup userGroup : userGroups )
        {
            SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();

            sharingUserGroupAccess.setId( userGroup.getUid() );
            sharingUserGroupAccess.setName( userGroup.getDisplayName() );
            sharingUserGroupAccess.setDisplayName( userGroup.getDisplayName() );

            sharingUserGroupAccesses.add( sharingUserGroupAccess );
        }

        return sharingUserGroupAccesses;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String sharingToString( BaseIdentifiableObject object )
    {
        StringBuilder builder = new StringBuilder()
            .append( "'" ).append( currentUserService.getCurrentUsername() ).append( "'" )
            .append( " update sharing on " ).append( object.getClass().getName() )
            .append( ", uid: " ).append( object.getUid() )
            .append( ", name: " ).append( object.getName() )
            .append( ", publicAccess: " ).append( object.getPublicAccess() )
            .append( ", externalAccess: " ).append( object.getExternalAccess() );

        if ( !object.getUserGroupAccesses().isEmpty() )
        {
            builder.append( ", userGroupAccesses: " );

            for ( org.hisp.dhis.user.UserGroupAccess userGroupAccess : object.getUserGroupAccesses() )
            {
                builder.append( "{uid: " ).append( userGroupAccess.getUserGroup().getUid() )
                    .append( ", name: " ).append( userGroupAccess.getUserGroup().getName() )
                    .append( ", access: " ).append( userGroupAccess.getAccess() )
                    .append( "} " );
            }
        }

        if ( !object.getUserAccesses().isEmpty() )
        {
            builder.append( ", userAccesses: " );

            for ( org.hisp.dhis.user.UserAccess userAccess : object.getUserAccesses() )
            {
                builder.append( "{uid: " ).append( userAccess.getUser().getUid() )
                    .append( ", name: " ).append( userAccess.getUser().getName() )
                    .append( ", access: " ).append( userAccess.getAccess() )
                    .append( "} " );
            }
        }

        return builder.toString();
    }

    private void syncSharingForEventProgram( Program program )
    {
        if ( ProgramType.WITH_REGISTRATION == program.getProgramType()
            || program.getProgramStages().isEmpty() )
        {
            return;
        }

        ProgramStage programStage = program.getProgramStages().iterator().next();
        AccessStringHelper.copySharing( program, programStage );

        programStage.setCreatedBy( program.getCreatedBy() );
        manager.update( programStage );
    }
}
