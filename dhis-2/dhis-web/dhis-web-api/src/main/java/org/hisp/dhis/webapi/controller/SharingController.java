package org.hisp.dhis.webapi.controller;

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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
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
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.sharing.Sharing;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserAccess;
import org.hisp.dhis.webapi.webdomain.sharing.SharingUserGroupAccess;
import org.hisp.dhis.webapi.webdomain.sharing.comparator.SharingUserGroupAccessNameComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = SharingController.RESOURCE_PATH, method = RequestMethod.GET )
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
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SchemaService schemaService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE )
    public void getSharing( @RequestParam String type, @RequestParam String id, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        type = getSharingType( type );

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

        for ( org.hisp.dhis.user.UserGroupAccess userGroupAccess : object.getUserGroupAccesses() )
        {
            UserGroup userGroup = userGroupService.getUserGroup( userGroupAccess.getId() );

            if ( userGroup == null ) continue;

            SharingUserGroupAccess sharingUserGroupAccess = new SharingUserGroupAccess();
            sharingUserGroupAccess.setId( userGroupAccess.getId() );
            sharingUserGroupAccess.setName( userGroup.getDisplayName() );
            sharingUserGroupAccess.setDisplayName( userGroup.getDisplayName() );
            sharingUserGroupAccess.setAccess( userGroupAccess.getAccess() );

            sharing.getObject().getUserGroupAccesses().add( sharingUserGroupAccess );
        }

        for ( org.hisp.dhis.user.UserAccess userAccess : SharingUtils.getDtoUserAccess( object.getSharing() ) )
        {
            User _user = userService.getUser( userAccess.getUid() );

            if ( _user == null ) continue;

            SharingUserAccess sharingUserAccess = new SharingUserAccess();
            sharingUserAccess.setId( userAccess.getId() );
            sharingUserAccess.setName( _user.getDisplayName() );
            sharingUserAccess.setDisplayName( _user.getDisplayName() );
            sharingUserAccess.setAccess( userAccess.getAccess() );

            sharing.getObject().getUserAccesses().add( sharingUserAccess );
        }

        sharing.getObject().getUserGroupAccesses().sort( SharingUserGroupAccessNameComparator.INSTANCE );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJson( response.getOutputStream(), sharing );
    }

    @RequestMapping( method = { RequestMethod.POST, RequestMethod.PUT }, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void setSharing( @RequestParam String type, @RequestParam String id, HttpServletResponse response, HttpServletRequest request ) throws IOException, WebMessageException
    {
        type = getSharingType( type );

        Class<? extends IdentifiableObject> sharingClass = aclService.classForType( type );

        if ( sharingClass == null || !aclService.isClassShareable( sharingClass ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Type " + type + " is not supported." ) );
        }

        BaseIdentifiableObject object = (BaseIdentifiableObject) manager.get( sharingClass, id );

        if ( object == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Object of type " + type + " with ID " + id + " was not found." ) );
        }

        if ( ( object instanceof SystemDefaultMetadataObject ) && ( (SystemDefaultMetadataObject) object ).isDefault() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Sharing settings of system default metadata object of type " + type + " cannot be modified." ) );
        }

        User user = currentUserService.getCurrentUser();

        if ( !aclService.canManage( user, object ) )
        {
            throw new AccessDeniedException( "You do not have manage access to this object." );
        }

        Sharing sharing = renderService.fromJson( request.getInputStream(), Sharing.class );

        if ( !AccessStringHelper.isValid( sharing.getObject().getPublicAccess() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Invalid public access string: " + sharing.getObject().getPublicAccess() ) );
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
                object.getSharing().setPublicAccess( AccessStringHelper.disableDataSharing( object.getSharing().getPublicAccess() ) );
            }
        }

        if ( object.getUser() == null )
        {
            object.setUser( user );
        }

        object.getSharing().getUserGroups().clear();

        for ( SharingUserGroupAccess sharingUserGroupAccess : sharing.getObject().getUserGroupAccesses() )
        {
            UserGroupAccess userGroupAccess = new UserGroupAccess();

            if ( !AccessStringHelper.isValid( sharingUserGroupAccess.getAccess() ) )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Invalid user group access string: " + sharingUserGroupAccess.getAccess() ) );
            }

            if ( !schema.isDataShareable() )
            {
                if ( AccessStringHelper.hasDataSharing( sharingUserGroupAccess.getAccess() ) )
                {
                    sharingUserGroupAccess.setAccess( AccessStringHelper.disableDataSharing( sharingUserGroupAccess.getAccess() ) );
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
                throw new WebMessageException( WebMessageUtils.conflict( "Invalid user access string: " + sharingUserAccess.getAccess() ) );
            }

            if ( !schema.isDataShareable() )
            {
                if ( AccessStringHelper.hasDataSharing( sharingUserAccess.getAccess() ) )
                {
                    sharingUserAccess.setAccess( AccessStringHelper.disableDataSharing( sharingUserAccess.getAccess() ) );
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

        List<SharingUserGroupAccess> userGroupAccesses = getSharingUserGroups( key, max );
        List<SharingUserAccess> userAccesses = getSharingUser( key, max );

        Map<String, Object> output = new HashMap<>();
        output.put( "userGroups", userGroupAccesses );
        output.put( "users", userAccesses );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        response.setHeader( ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue() );
        renderService.toJson( response.getOutputStream(), output );
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

        programStage.setUser( program.getUser() );
        manager.update( programStage );
    }

    /**
     * This method is being used only during the deprecation process of the
     * Pivot/ReportTable API. It must be removed once the process is complete.
     *
     * @return "visualization" if the given type is equals to "reportTable" or
     *         "chart", otherwise it returns the given type itself.
     */
    @Deprecated
    private String getSharingType( final String type )
    {
        if ( "reportTable".equalsIgnoreCase( type ) || "chart".equalsIgnoreCase( type ) )
        {
            return "visualization";
        }
        return type;
    }


}
