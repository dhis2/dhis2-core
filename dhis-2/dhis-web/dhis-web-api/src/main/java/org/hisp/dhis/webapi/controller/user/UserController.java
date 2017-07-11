package org.hisp.dhis.webapi.controller.user;

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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.UserSchemaDescriptor;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.user.Users;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = UserSchemaDescriptor.API_ENDPOINT )
public class UserController
    extends AbstractCrudController<User>
{
    public static final String INVITE_PATH = "/invite";
    public static final String BULK_INVITE_PATH = "/invites";

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserSettingService userSettingService;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<User> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders ) throws QueryParserException
    {
        UserQueryParams params = new UserQueryParams();
        params.setQuery( options.get( "query" ) );
        params.setPhoneNumber( options.get( "phoneNumber" ) );
        params.setCanManage( options.isTrue( "canManage" ) );
        params.setAuthSubset( options.isTrue( "authSubset" ) );
        params.setLastLogin( options.getDate( "lastLogin" ) );
        params.setInactiveMonths( options.getInt( "inactiveMonths" ) );
        params.setInactiveSince( options.getDate( "inactiveSince" ) );
        params.setSelfRegistered( options.isTrue( "selfRegistered" ) );
        params.setInvitationStatus( UserInvitationStatus.fromValue( options.get( "invitationStatus" ) ) );

        String ou = options.get( "ou" );
        boolean includeOrgUnitChildren = Boolean.valueOf( options.get( "includeChildren" ) );

        if ( ou != null )
        {
            params.setOrganisationUnit( organisationUnitService.getOrganisationUnit( ou ) );
            params.setIncludeOrgUnitChildren( includeOrgUnitChildren );
        }

        if ( options.isManage() )
        {
            params.setCanManage( true );
            params.setAuthSubset( true );
        }

        int count = userService.getUserCount( params );

        if ( options.hasPaging() && filters.isEmpty() )
        {
            Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
            metadata.setPager( pager );
            params.setFirst( pager.getOffset() );
            params.setMax( pager.getPageSize() );
        }

        List<User> users = userService.getUsers( params );

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, options.getRootJunction() );
        query.setDefaultOrder();
        query.setObjects( users );

        return (List<User>) queryService.query( query );
    }

    @Override
    protected List<User> getEntity( String uid, WebOptions options )
    {
        List<User> users = Lists.newArrayList();
        Optional<User> user = Optional.ofNullable( userService.getUser( uid ) );

        if ( user.isPresent() )
        {
            users.add( user.get() );
        }

        return users;
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromXml( request.getInputStream(), getEntityClass() );

        User currentUser = currentUserService.getCurrentUser();

        if ( !validateCreateUser( user, currentUser ) )
        {
            return;
        }

        renderService.toXml( response.getOutputStream(), createUser( user, currentUser ) );
    }

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromJson( request.getInputStream(), getEntityClass() );

        User currentUser = currentUserService.getCurrentUser();

        if ( !validateCreateUser( user, currentUser ) )
        {
            return;
        }

        renderService.toJson( response.getOutputStream(), createUser( user, currentUser ) );
    }

    @RequestMapping( value = INVITE_PATH, method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonInvite( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromJson( request.getInputStream(), getEntityClass() );

        User currentUser = currentUserService.getCurrentUser();

        if ( !validateInviteUser( user, currentUser ) )
        {
            return;
        }

        renderService.toJson( response.getOutputStream(), inviteUser( user, currentUser, request ) );
    }

    @RequestMapping( value = BULK_INVITE_PATH, method = RequestMethod.POST, consumes = "application/json" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postJsonInvites( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Users users = renderService.fromJson( request.getInputStream(), Users.class );

        User currentUser = currentUserService.getCurrentUser();

        for ( User user : users.getUsers() )
        {
            if ( !validateInviteUser( user, currentUser ) )
            {
                return;
            }
        }

        for ( User user : users.getUsers() )
        {
            inviteUser( user, currentUser, request );
        }
    }

    @RequestMapping( value = INVITE_PATH, method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    public void postXmlInvite( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = renderService.fromXml( request.getInputStream(), getEntityClass() );

        User currentUser = currentUserService.getCurrentUser();

        if ( !validateInviteUser( user, currentUser ) )
        {
            return;
        }

        renderService.toXml( response.getOutputStream(), inviteUser( user, currentUser, request ) );
    }

    @RequestMapping( value = BULK_INVITE_PATH, method = RequestMethod.POST, consumes = { "application/xml", "text/xml" } )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postXmlInvites( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Users users = renderService.fromXml( request.getInputStream(), Users.class );

        User currentUser = currentUserService.getCurrentUser();

        for ( User user : users.getUsers() )
        {
            if ( !validateInviteUser( user, currentUser ) )
            {
                return;
            }
        }

        for ( User user : users.getUsers() )
        {
            inviteUser( user, currentUser, request );
        }
    }

    @RequestMapping( value = "/{id}" + INVITE_PATH, method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void resendInvite( @PathVariable String id, HttpServletRequest request ) throws Exception
    {
        User user = userService.getUser( id );

        if ( user == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + id ) );
        }

        if ( user.getUserCredentials() == null || !user.getUserCredentials().isInvitation() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User account is not an invitation: " + id ) );
        }

        String valid = securityService.validateRestore( user.getUserCredentials() );

        if ( valid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( valid ) );
        }

        boolean isInviteUsername = securityService.isInviteUsername( user.getUsername() );

        RestoreOptions restoreOptions = isInviteUsername ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        securityService.sendRestoreMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ), restoreOptions );
    }

    @SuppressWarnings( "unchecked" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_REPLICATE_USER')" )
    @RequestMapping( value = "/{uid}/replica", method = RequestMethod.POST )
    public void replicateUser( @PathVariable String uid,
        HttpServletRequest request, HttpServletResponse response ) throws IOException, WebMessageException
    {
        User existingUser = userService.getUser( uid );

        if ( existingUser == null || existingUser.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + uid ) );
        }

        User currentUser = currentUserService.getCurrentUser();

        if ( !validateCreateUser( existingUser, currentUser ) )
        {
            return;
        }

        Map<String, String> auth = renderService.fromJson( request.getInputStream(), Map.class );

        String username = StringUtils.trimToNull( auth != null ? auth.get( KEY_USERNAME ) : null );
        String password = StringUtils.trimToNull( auth != null ? auth.get( KEY_PASSWORD ) : null );

        if ( auth == null || username == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Username must be specified" ) );
        }

        if ( userService.getUserCredentialsByUsername( username ) != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Username already taken: " + username ) );
        }

        if ( password == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Password must be specified" ) );
        }

        if ( !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Password must have at least 8 characters, one digit, one uppercase" ) );
        }

        User userReplica = new User();
        mergeService.merge( new MergeParams<>( existingUser, userReplica )
            .setMergeMode( MergeMode.MERGE ) );
        userReplica.setUid( CodeGenerator.generateUid() );
        userReplica.setCode( null );
        userReplica.setCreated( new Date() );

        UserCredentials credentialsReplica = new UserCredentials();
        mergeService.merge( new MergeParams<>( existingUser.getUserCredentials(), credentialsReplica )
            .setMergeMode( MergeMode.MERGE ) );
        credentialsReplica.setUid( CodeGenerator.generateUid() );
        credentialsReplica.setCode( null );
        credentialsReplica.setCreated( new Date() );
        credentialsReplica.setLdapId( null );
        credentialsReplica.setOpenId( null );

        credentialsReplica.setUsername( username );
        userService.encodeAndSetPassword( credentialsReplica, password );

        userReplica.setUserCredentials( credentialsReplica );
        credentialsReplica.setUserInfo( userReplica );

        userService.addUser( userReplica );
        userService.addUserCredentials( credentialsReplica );
        userGroupService.addUserToGroups( userReplica, IdentifiableObjectUtils.getUids( existingUser.getGroups() ), currentUser );

        // ---------------------------------------------------------------------
        // Replicate user settings
        // ---------------------------------------------------------------------

        List<UserSetting> settings = userSettingService.getUserSettings( existingUser );

        for ( UserSetting setting : settings )
        {
            Optional<UserSettingKey> key = UserSettingKey.getByName( setting.getName() );
            key.ifPresent( userSettingKey -> userSettingService.saveUserSetting( userSettingKey, setting.getValue(), userReplica ) );
        }

        response.addHeader( "Location", UserSchemaDescriptor.API_ENDPOINT + "/" + userReplica.getUid() );
        webMessageService.send( WebMessageUtils.created( "User replica created" ), response, request );
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { "application/xml", "text/xml" } )
    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<User> users = getEntity( pvUid, NO_WEB_OPTIONS );

        if ( users.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( getEntityName() + " does not exist: " + pvUid ) );
        }

        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( currentUser, users.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }

        User parsed = renderService.fromXml( request.getInputStream(), getEntityClass() );
        parsed.setUid( pvUid );

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( parsed.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( users.get( 0 ).getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setImportReportMode( ImportReportMode.FULL );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            User user = userService.getUser( pvUid );
            userGroupService.updateUserGroups( user, IdentifiableObjectUtils.getUids( parsed.getGroups() ), currentUser );
        }

        renderService.toXml( response.getOutputStream(), importReport );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<User> users = getEntity( pvUid, NO_WEB_OPTIONS );

        if ( users.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( getEntityName() + " does not exist: " + pvUid ) );
        }

        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( currentUser, users.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }

        User parsed = renderService.fromJson( request.getInputStream(), getEntityClass() );
        parsed.setUid( pvUid );

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( parsed.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( users.get( 0 ).getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setImportReportMode( ImportReportMode.FULL );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( parsed );

        ImportReport importReport = importService.importMetadata( params );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            User user = userService.getUser( pvUid );

            userGroupService.updateUserGroups( user, IdentifiableObjectUtils.getUids( parsed.getGroups() ), currentUser );
        }

        renderService.toJson( response.getOutputStream(), importReport );
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    @Override
    protected void prePatchEntity( User entity, User newEntity ) throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( entity.getGroups() ), currentUser )
            || !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( newEntity.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( entity.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    protected void preDeleteEntity( User entity ) throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( entity.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( entity.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        if ( userService.isLastSuperUser( entity.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Can not remove the last super user." ) );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Validates whether the given user can be created.
     *
     * @param user the user.
     */
    private boolean validateCreateUser( User user, User currentUser ) throws WebMessageException
    {
        if ( !aclService.canCreate( currentUser, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        if ( !userService.canAddOrUpdateUser( IdentifiableObjectUtils.getUids( user.getGroups() ), currentUser ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        List<String> uids = IdentifiableObjectUtils.getUids( user.getGroups() );

        for ( String uid : uids )
        {
            if ( !userGroupService.canAddOrRemoveMember( uid, currentUser ) )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "You don't have permissions to add user to user group: " + uid ) );
            }
        }

        return true;
    }

    /**
     * Creates a user.
     *
     * @param user user object parsed from the POST request.
     */
    private ImportReport createUser( User user, User currentUser ) throws Exception
    {
        user.getUserCredentials().getCogsDimensionConstraints().addAll(
            currentUser.getUserCredentials().getCogsDimensionConstraints() );

        user.getUserCredentials().getCatDimensionConstraints().addAll(
            currentUser.getUserCredentials().getCatDimensionConstraints() );

        MetadataImportParams importParams = new MetadataImportParams()
            .setImportReportMode( ImportReportMode.FULL )
            .setImportStrategy( ImportStrategy.CREATE )
            .addObject( user );

        ImportReport importReport = importService.importMetadata( importParams );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getCreated() == 1 )
        {
            userGroupService.addUserToGroups( user, IdentifiableObjectUtils.getUids( user.getGroups() ), currentUser );
        }

        return importReport;
    }

    /**
     * Validates whether a user can be invited / created.
     *
     * @param user the user.
     */
    private boolean validateInviteUser( User user, User currentUser ) throws WebMessageException
    {
        if ( !validateCreateUser( user, currentUser ) )
        {
            return false;
        }

        UserCredentials credentials = user.getUserCredentials();

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User credentials is not present" ) );
        }

        credentials.setUserInfo( user );

        String valid = securityService.validateInvite( user.getUserCredentials() );

        if ( valid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( valid + ": " + user.getUserCredentials() ) );
        }

        return true;
    }

    /**
     * Creates a user invitation and invites the user.
     *
     * @param user user object parsed from the POST request.
     */
    private ObjectReport inviteUser( User user, User currentUser, HttpServletRequest request ) throws Exception
    {
        RestoreOptions restoreOptions = user.getUsername() == null || user.getUsername().isEmpty() ?
            RestoreOptions.INVITE_WITH_USERNAME_CHOICE : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        securityService.prepareUserForInvite( user );

        ImportReport importReport = createUser( user, currentUser );
        ObjectReport objectReport = getObjectReport( importReport );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getCreated() == 1 )
        {
            securityService.sendRestoreMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ), restoreOptions );
        }

        return objectReport;
    }

    private ObjectReport getObjectReport( ImportReport importReport )
    {
        if ( !importReport.getTypeReports().isEmpty() )
        {
            TypeReport typeReport = importReport.getTypeReports().get( 0 );

            if ( !typeReport.getObjectReports().isEmpty() )
            {
                return typeReport.getObjectReports().get( 0 );
            }
        }

        return null;
    }
}
