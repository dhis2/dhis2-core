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
package org.hisp.dhis.webapi.controller.user;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.MergeParams;
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
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
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
    private UserControllerUtils userControllerUtils;

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
        List<Order> orders )
        throws QueryParserException
    {
        UserQueryParams params = makeUserQueryParams( options );

        String ou = options.get( "ou" );

        if ( ou != null )
        {
            params.addOrganisationUnit( organisationUnitService.getOrganisationUnit( ou ) );
        }

        if ( options.isManage() )
        {
            params.setCanManage( true );
            params.setAuthSubset( true );
        }

        boolean hasUserGroupFilter = filters.stream().anyMatch( f -> f.startsWith( "userGroups." ) );
        params.setPrefetchUserGroups( hasUserGroupFilter );

        if ( filters.isEmpty() && options.hasPaging() )
        {
            metadata.setPager( makePager( options, params ) );
        }

        Query query = makeQuery( options, filters, orders, params );

        return (List<User>) queryService.query( query );
    }

    private Pager makePager( WebOptions options, UserQueryParams params )
    {
        long count = userService.getUserCount( params );

        Pager pager = new Pager( options.getPage(), count, options.getPageSize() );
        params.setFirst( pager.getOffset() );
        params.setMax( pager.getPageSize() );

        return pager;
    }

    private Query makeQuery( WebOptions options, List<String> filters,
        List<Order> orders, UserQueryParams params )
    {
        Pagination pagination = CollectionUtils.isEmpty( filters ) ? new Pagination() : getPaginationData( options );

        List<String> ordersAsString = (orders == null) ? null
            : orders.stream()
                .map( Order::toOrderString )
                .collect( Collectors.toList() );

        /*
         * Keep the memory query on the result
         */
        Query query = queryService
            .getQueryFromUrl( getEntityClass(), filters, orders, pagination, options.getRootJunction() );

        // Fetches all users if there are no query, i.e only filters...
        List<User> users = userService.getUsers( params, ordersAsString );

        query.setObjects( users );
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );
        query.setDefaultOrder();

        return query;
    }

    private UserQueryParams makeUserQueryParams( WebOptions options )
    {
        UserQueryParams params = new UserQueryParams();
        params.setQuery( StringUtils.trimToNull( options.get( "query" ) ) );
        params.setPhoneNumber( StringUtils.trimToNull( options.get( "phoneNumber" ) ) );
        params.setCanManage( options.isTrue( "canManage" ) );
        params.setAuthSubset( options.isTrue( "authSubset" ) );
        params.setLastLogin( options.getDate( "lastLogin" ) );
        params.setInactiveMonths( options.getInt( "inactiveMonths" ) );
        params.setInactiveSince( options.getDate( "inactiveSince" ) );
        params.setSelfRegistered( options.isTrue( "selfRegistered" ) );
        params.setInvitationStatus( UserInvitationStatus.fromValue( options.get( "invitationStatus" ) ) );
        params.setUserOrgUnits( options.isTrue( "userOrgUnits" ) );
        params.setIncludeOrgUnitChildren( options.isTrue( "includeChildren" ) );

        return params;
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

    @Override
    @GetMapping( "/{uid}/{property}" )
    public @ResponseBody RootNode getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        HttpServletResponse response )
        throws Exception
    {
        if ( !"dataApprovalWorkflows".equals( pvProperty ) )
        {
            return super.getObjectProperty( pvUid, pvProperty, rpParameters, translateParams, response );
        }

        User user = userService.getUser( pvUid );

        if ( user == null || user.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + pvUid ) );
        }

        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canRead( currentUser, user ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to access this user." );
        }

        return userControllerUtils.getUserDataApprovalWorkflows( user );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Override
    @PostMapping( consumes = { "application/xml", "text/xml" } )
    public void postXmlObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postObject( request, response, renderService.fromXml( request.getInputStream(), getEntityClass() ) );
    }

    @Override
    @PostMapping( consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postObject( request, response, renderService.fromJson( request.getInputStream(), getEntityClass() ) );
    }

    private void postObject( HttpServletRequest request, HttpServletResponse response, User user )
        throws WebMessageException
    {
        User currentUser = currentUserService.getCurrentUser();

        validateCreateUser( user, currentUser );

        postObject( request, response, getObjectReport( createUser( user, currentUser ) ) );
    }

    @PostMapping( value = INVITE_PATH, consumes = "application/json" )
    public void postJsonInvite( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postInvite( request, response, renderService.fromJson( request.getInputStream(), getEntityClass() ) );
    }

    @PostMapping( value = INVITE_PATH, consumes = { "application/xml", "text/xml" } )
    public void postXmlInvite( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postInvite( request, response, renderService.fromXml( request.getInputStream(), getEntityClass() ) );
    }

    private void postInvite( HttpServletRequest request, HttpServletResponse response, User user )
        throws WebMessageException
    {
        User currentUser = currentUserService.getCurrentUser();

        validateInviteUser( user, currentUser );

        postObject( request, response, inviteUser( user, currentUser, request ) );
    }

    @PostMapping( value = BULK_INVITE_PATH, consumes = "application/json" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postJsonInvites( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postInvites( request, renderService.fromJson( request.getInputStream(), Users.class ) );
    }

    @PostMapping( value = BULK_INVITE_PATH, consumes = { "application/xml",
        "text/xml" } )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postXmlInvites( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        postInvites( request, renderService.fromXml( request.getInputStream(), Users.class ) );
    }

    private void postInvites( HttpServletRequest request, Users users )
        throws WebMessageException
    {
        User currentUser = currentUserService.getCurrentUser();

        for ( User user : users.getUsers() )
        {
            validateInviteUser( user, currentUser );
        }

        for ( User user : users.getUsers() )
        {
            inviteUser( user, currentUser, request );
        }
    }

    @PostMapping( value = "/{id}" + INVITE_PATH )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void resendInvite( @PathVariable String id, HttpServletRequest request )
        throws Exception
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

        RestoreOptions restoreOptions = isInviteUsername ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE
            : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        if ( !securityService
            .sendRestoreOrInviteMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ),
                restoreOptions ) )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed to send invite message" ) );
        }
    }

    @PostMapping( "/{id}/reset" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void resetToInvite( @PathVariable String id, HttpServletRequest request )
        throws Exception
    {
        User user = userService.getUser( id );
        if ( user == null )
        {
            throw NotFoundException.notFoundUid( id );
        }
        String valid = securityService.validateRestore( user.getUserCredentials() );
        if ( valid != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( valid ) );
        }
        User currentUser = currentUserService.getCurrentUser();
        if ( !aclService.canUpdate( currentUser, user ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }
        if ( !userService.canAddOrUpdateUser( getUids( user.getGroups() ), currentUser ) )
        {
            throw new UpdateAccessDeniedException(
                "You must have permissions manage at least one user group for the user." );
        }

        securityService.prepareUserForInvite( user );
        securityService.sendRestoreOrInviteMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ),
            RestoreOptions.RECOVER_PASSWORD_OPTION );
    }

    @SuppressWarnings( "unchecked" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_REPLICATE_USER')" )
    @PostMapping( "/{uid}/replica" )
    public void replicateUser( @PathVariable String uid,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        User existingUser = userService.getUser( uid );

        if ( existingUser == null || existingUser.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User not found: " + uid ) );
        }

        User currentUser = currentUserService.getCurrentUser();

        validateCreateUser( existingUser, currentUser );

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
            throw new WebMessageException(
                WebMessageUtils.conflict( "Password must have at least 8 characters, one digit, one uppercase" ) );
        }

        User userReplica = new User();
        mergeService.merge( new MergeParams<>( existingUser, userReplica )
            .setMergeMode( MergeMode.MERGE ) );
        copyAttributeValues( userReplica );
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
        userGroupService.addUserToGroups( userReplica, getUids( existingUser.getGroups() ),
            currentUser );

        // ---------------------------------------------------------------------
        // Replicate user settings
        // ---------------------------------------------------------------------

        List<UserSetting> settings = userSettingService.getUserSettings( existingUser );

        for ( UserSetting setting : settings )
        {
            Optional<UserSettingKey> key = UserSettingKey.getByName( setting.getName() );
            key.ifPresent( userSettingKey -> userSettingService.saveUserSetting( userSettingKey, setting.getValue(),
                userReplica ) );
        }

        response.addHeader( "Location", UserSchemaDescriptor.API_ENDPOINT + "/" + userReplica.getUid() );
        webMessageService.send( WebMessageUtils.created( "User replica created" ), response, request );
    }

    @PostMapping( "/{uid}/enabled" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void enableUser( @PathVariable( "uid" ) String uid )
        throws Exception
    {
        setDisabled( uid, false );
    }

    @PostMapping( "/{uid}/disabled" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void disableUser( @PathVariable( "uid" ) String uid )
        throws Exception
    {
        setDisabled( uid, true );
    }

    @PostMapping( "/{uid}/expired" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void expireUser( @PathVariable( "uid" ) String uid, @RequestParam( "date" ) Date accountExpiry )
        throws Exception
    {
        setExpires( uid, accountExpiry );
    }

    @PostMapping( "/{uid}/unexpired" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    public void unexpireUser( @PathVariable( "uid" ) String uid )
        throws Exception
    {
        setExpires( uid, null );
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    @Override
    @PutMapping( value = "/{uid}", consumes = { "application/xml", "text/xml" } )
    public void putXmlObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        User parsed = renderService.fromXml( request.getInputStream(), getEntityClass() );

        ImportReport importReport = updateUser( pvUid, parsed );

        response.setContentType( "application/xml" );
        renderService.toXml( response.getOutputStream(), importReport );
    }

    @Override
    @PutMapping( value = "/{uid}", consumes = "application/json" )
    public void putJsonObject( @PathVariable( "uid" ) String pvUid, HttpServletRequest request,
        HttpServletResponse response )
        throws Exception
    {
        User parsed = renderService.fromJson( request.getInputStream(), getEntityClass() );

        ImportReport importReport = updateUser( pvUid, parsed );

        response.setContentType( "application/json" );
        renderService.toJson( response.getOutputStream(), importReport );
    }

    protected ImportReport updateUser( String userUid, User parsedUserObject )
        throws WebMessageException
    {
        List<User> users = getEntity( userUid, NO_WEB_OPTIONS );

        if ( users.isEmpty() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( getEntityName() + " does not exist: " + userUid ) );
        }

        User currentUser = currentUserService.getCurrentUser();
        // TODO: Can we disallow currentUser == NULL ?
        if ( !aclService.canUpdate( currentUser, users.get( 0 ) ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this user." );
        }

        // force initialization of all authorities of current user in order to
        // prevent cases where user must be reloaded later
        // (in case it gets detached)
        if ( currentUser != null )
        {
            currentUser.getUserCredentials().getAllAuthorities();
        }

        parsedUserObject.setUid( userUid );
        parsedUserObject = mergeLastLoginAttribute( users.get( 0 ), parsedUserObject );

        boolean isPasswordChangeAttempt = parsedUserObject.getUserCredentials() != null &&
            parsedUserObject.getUserCredentials().getPassword() != null;

        List<String> groupsUids = getUids( parsedUserObject.getGroups() );

        if ( !userService.canAddOrUpdateUser( groupsUids, currentUser )
            || !currentUser.getUserCredentials().canModifyUser( users.get( 0 ).getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "You must have permissions to create user, " +
                    "or ability to manage at least one user group for the user." ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setImportReportMode( ImportReportMode.FULL );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( parsedUserObject );

        ImportReport importReport = importService.importMetadata( params );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            updateUserGroups( userUid, parsedUserObject, currentUser );

            // If it was a pw change attempt (input.pw != null) and update was
            // success we assume password has changed...
            // We chose to expire the special case if password is set to the
            // same. i.e. no before & after equals pw check
            if ( isPasswordChangeAttempt )
            {
                userService.expireActiveSessions( parsedUserObject.getUserCredentials() );
            }
        }

        return importReport;
    }

    protected void updateUserGroups( String pvUid, User parsed, User currentUser )
    {
        User user = userService.getUser( pvUid );

        // current user may have been changed and detached and must become
        // managed again
        // TODO: what is this doing? I don't understand how this is possible.
        if ( currentUser != null && currentUser.getId() == user.getId() )
        {
            currentUser = currentUserService.getCurrentUser();
        }

        userGroupService.updateUserGroups( user, getUids( parsed.getGroups() ), currentUser );
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    @Override
    protected void prePatchEntity( User entity )
        throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !userService.canAddOrUpdateUser( getUids( entity.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( entity.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }
    }

    @Override
    protected void postPatchEntity( User entity )
    {
        UserCredentials credentials = entity.getUserCredentials();

        // Make sure we always expire all of the user's active sessions if we
        // have disabled the user.
        if ( credentials != null && credentials.isDisabled() )
        {
            userService.expireActiveSessions( credentials );
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    protected void preDeleteEntity( User entity )
        throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !userService.canAddOrUpdateUser( getUids( entity.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( entity.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
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
    private void validateCreateUser( User user, User currentUser )
        throws WebMessageException
    {
        if ( !aclService.canCreate( currentUser, getEntityClass() ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to create this object." );
        }

        if ( !userService.canAddOrUpdateUser( getUids( user.getGroups() ), currentUser ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }

        List<String> uids = getUids( user.getGroups() );

        for ( String uid : uids )
        {
            if ( !userGroupService.canAddOrRemoveMember( uid, currentUser ) )
            {
                throw new WebMessageException(
                    WebMessageUtils.conflict( "You don't have permissions to add user to user group: " + uid ) );
            }
        }
    }

    /**
     * Creates a user.
     *
     * @param user user object parsed from the POST request.
     */
    private ImportReport createUser( User user, User currentUser )
    {
        MetadataImportParams importParams = new MetadataImportParams()
            .setImportReportMode( ImportReportMode.FULL )
            .setImportStrategy( ImportStrategy.CREATE )
            .addObject( user );

        ImportReport importReport = importService.importMetadata( importParams );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getCreated() == 1 )
        {
            userGroupService.addUserToGroups( user, getUids( user.getGroups() ), currentUser );
        }

        return importReport;
    }

    /**
     * Validates whether a user can be invited / created.
     *
     * @param user the user.
     */
    private void validateInviteUser( User user, User currentUser )
        throws WebMessageException
    {
        validateCreateUser( user, currentUser );

        UserCredentials credentials = user.getUserCredentials();

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User credentials is not present" ) );
        }

        credentials.setUserInfo( user );

        String validateMessage = securityService.validateInvite( user.getUserCredentials() );

        if ( validateMessage != null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( validateMessage ) );
        }
    }

    /**
     * Creates a user invitation and invites the user.
     *
     * @param user user object parsed from the POST request.
     */
    private ObjectReport inviteUser( User user, User currentUser, HttpServletRequest request )
    {
        RestoreOptions restoreOptions = user.getUsername() == null || user.getUsername().isEmpty()
            ? RestoreOptions.INVITE_WITH_USERNAME_CHOICE
            : RestoreOptions.INVITE_WITH_DEFINED_USERNAME;

        securityService.prepareUserForInvite( user );

        ImportReport importReport = createUser( user, currentUser );
        ObjectReport objectReport = getObjectReport( importReport );

        if ( importReport.getStatus() == Status.OK &&
            importReport.getStats().getCreated() == 1 &&
            objectReport != null )
        {
            securityService
                .sendRestoreOrInviteMessage( user.getUserCredentials(), ContextUtils.getContextPath( request ),
                    restoreOptions );

            log.info( String.format( "An invite email was successfully sent to: %s", user.getEmail() ) );
        }

        return objectReport;
    }

    private static ObjectReport getObjectReport( ImportReport importReport )
    {
        return importReport.getFirstObjectReport();
    }

    /**
     * Make a copy of any existing attribute values so they can be saved as new
     * attribute values. Don't copy unique values.
     *
     * @param userReplica user for which to copy attribute values.
     */
    private void copyAttributeValues( User userReplica )
    {
        if ( userReplica.getAttributeValues() == null )
        {
            return;
        }

        Set<AttributeValue> newAttributeValues = new HashSet<>();

        for ( AttributeValue oldValue : userReplica.getAttributeValues() )
        {
            if ( !oldValue.getAttribute().isUnique() )
            {
                AttributeValue newValue = new AttributeValue( oldValue.getValue(), oldValue.getAttribute() );

                newAttributeValues.add( newValue );
            }
        }

        if ( newAttributeValues.isEmpty() )
        {
            userReplica.setAttributeValues( null );
        }

        userReplica.setAttributeValues( newAttributeValues );
    }

    private User mergeLastLoginAttribute( User source, User target )
    {
        if ( target.getUserCredentials() == null )
        {
            return target;
        }

        if ( target.getUserCredentials().getLastLogin() != null )
        {
            return target;
        }

        if ( source.getUserCredentials() != null && source.getUserCredentials().getLastLogin() != null )
        {
            target.getUserCredentials().setLastLogin( source.getUserCredentials().getLastLogin() );
        }

        return target;
    }

    /**
     * Either disable or enable a user account
     *
     * @param uid the unique id of the user to enable or disable
     * @param disable boolean value, true for disable, false for enable
     * @throws WebMessageException thrown if "current" user is not allowed to
     *         modify the user
     */
    private void setDisabled( String uid, boolean disable )
        throws WebMessageException
    {
        User userToModify = userService.getUser( uid );
        checkCurrentUserCanModify( userToModify );

        UserCredentials credentials = userToModify.getUserCredentials();
        if ( credentials.isDisabled() != disable )
        {
            credentials.setDisabled( disable );
            userService.updateUserCredentials( credentials );
        }

        if ( disable )
        {
            userService.expireActiveSessions( credentials );
        }
    }

    private void checkCurrentUserCanModify( User userToModify )
        throws WebMessageException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( currentUser, userToModify ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !userService.canAddOrUpdateUser( getUids( userToModify.getGroups() ), currentUser )
            || !currentUser.getUserCredentials().canModifyUser( userToModify.getUserCredentials() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }
    }

    private void setExpires( String uid, Date accountExpiry )
        throws WebMessageException
    {
        User userToModify = userService.getUser( uid );
        checkCurrentUserCanModify( userToModify );

        UserCredentials credentials = userToModify.getUserCredentials();
        credentials.setAccountExpiry( accountExpiry );
        userService.updateUserCredentials( credentials );

        if ( userService.isAccountExpired( credentials ) )
        {
            userService.expireActiveSessions( credentials );
        }
    }
}
