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
package org.hisp.dhis.webapi.controller.user;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importReport;
import static org.hisp.dhis.user.User.populateUserCredentialsDtoCopyOnlyChanges;
import static org.hisp.dhis.user.User.populateUserCredentialsDtoFields;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;
import org.hisp.dhis.commons.jackson.jsonpatch.operations.AddOperation;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
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
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.User;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( { "user", "management" } )
@Slf4j
@Controller
@RequestMapping( value = UserSchemaDescriptor.API_ENDPOINT )
@RequiredArgsConstructor
public class UserController
    extends AbstractCrudController<User>
{
    public static final String INVITE_PATH = "/invite";

    public static final String BULK_INVITE_PATH = "/invites";

    protected final DbmsManager dbmsManager;

    private final UserService userService;

    private final UserGroupService userGroupService;

    private final UserControllerUtils userControllerUtils;

    private final SecurityService securityService;

    private final OrganisationUnitService organisationUnitService;

    private final UserSettingService userSettingService;

    private final PasswordValidationService passwordValidationService;

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
        params.setOrgUnitBoundary( UserOrgUnitType.fromValue( options.get( "orgUnitBoundary" ) ) );

        return params;
    }

    @Override
    @Nonnull
    protected User getEntity( String uid, WebOptions options )
        throws NotFoundException
    {
        User user = userService.getUser( uid );
        if ( user == null )
        {
            throw new NotFoundException( User.class, uid );
        }
        return user;
    }

    @Override
    @GetMapping( "/{uid}/{property}" )
    public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
        @PathVariable( "uid" ) String pvUid, @PathVariable( "property" ) String pvProperty,
        @RequestParam Map<String, String> rpParameters,
        TranslateParams translateParams,
        @CurrentUser User currentUser,
        HttpServletResponse response )
        throws ForbiddenException,
        NotFoundException
    {
        if ( !"dataApprovalWorkflows".equals( pvProperty ) )
        {
            return super.getObjectProperty( pvUid, pvProperty, rpParameters, translateParams, currentUser, response );
        }

        User user = userService.getUser( pvUid );

        if ( user == null
            // TODO: To remove when we remove old UserCredentials compatibility
            || user.getUserCredentials() == null )
        {
            throw new NotFoundException( "User not found: " + pvUid );
        }

        if ( !aclService.canRead( currentUser, user ) )
        {
            throw new CreateAccessDeniedException( "You don't have the proper permissions to access this user." );
        }

        return ResponseEntity.ok( userControllerUtils.getUserDataApprovalWorkflows( user ) );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Override
    @PostMapping( consumes = { APPLICATION_XML_VALUE, TEXT_XML_VALUE } )
    @ResponseBody
    public WebMessage postXmlObject( HttpServletRequest request )
        throws IOException,
        ForbiddenException,
        ConflictException
    {
        return postObject( renderService.fromXml( request.getInputStream(), getEntityClass() ) );
    }

    @Override
    @PostMapping( consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage postJsonObject( HttpServletRequest request )
        throws IOException,
        ForbiddenException,
        ConflictException
    {
        return postObject( renderService.fromJson( request.getInputStream(), getEntityClass() ) );
    }

    private WebMessage postObject( User user )
        throws ForbiddenException,
        ConflictException
    {
        // TODO: To remove when we remove old UserCredentials compatibility
        populateUserCredentialsDtoFields( user );

        User currentUser = currentUserService.getCurrentUser();

        validateCreateUser( user, currentUser );

        return postObject( getObjectReport( createUser( user, currentUser ) ) );
    }

    @PostMapping( value = INVITE_PATH, consumes = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage postJsonInvite( HttpServletRequest request )
        throws ForbiddenException,
        ConflictException,
        IOException
    {
        User user = renderService.fromJson( request.getInputStream(), getEntityClass() );
        return postInvite( request, user );
    }

    @PostMapping( value = INVITE_PATH, consumes = { APPLICATION_XML_VALUE, TEXT_XML_VALUE } )
    @ResponseBody
    public WebMessage postXmlInvite( HttpServletRequest request )
        throws IOException,
        ForbiddenException,
        ConflictException
    {
        User user = renderService.fromXml( request.getInputStream(), getEntityClass() );
        return postInvite( request, user );
    }

    private WebMessage postInvite( HttpServletRequest request, User user )
        throws ForbiddenException,
        ConflictException
    {
        // TODO: To remove when we remove old UserCredentials compatibility
        populateUserCredentialsDtoFields( user );

        User currentUser = currentUserService.getCurrentUser();

        validateInviteUser( user, currentUser );

        return postObject( inviteUser( user, currentUser, request ) );
    }

    @PostMapping( value = BULK_INVITE_PATH, consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postJsonInvites( HttpServletRequest request )
        throws Exception
    {
        Users users = renderService.fromJson( request.getInputStream(), Users.class );
        postInvites( request, users );
    }

    @PostMapping( value = BULK_INVITE_PATH, consumes = { APPLICATION_XML_VALUE,
        TEXT_XML_VALUE } )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void postXmlInvites( HttpServletRequest request )
        throws Exception
    {
        Users users = renderService.fromXml( request.getInputStream(), Users.class );
        postInvites( request, users );
    }

    private void postInvites( HttpServletRequest request, Users users )
        throws ForbiddenException,
        ConflictException
    {
        User currentUser = currentUserService.getCurrentUser();

        // TODO: To remove when we remove old UserCredentials compatibility
        for ( User user : users.getUsers() )
        {
            populateUserCredentialsDtoFields( user );
        }

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
        throws NotFoundException,
        ConflictException,
        WebMessageException
    {
        User user = userService.getUser( id );
        if ( user == null )
        {
            throw new NotFoundException( User.class, id );
        }

        if ( !user.isInvitation() )
        {
            throw new ConflictException( "User account is not an invitation: " + id );
        }

        ErrorCode errorCode = securityService.validateRestore( user );
        if ( errorCode != null )
        {
            throw new ConflictException( errorCode );
        }

        if ( !securityService
            .sendRestoreOrInviteMessage( user, ContextUtils.getContextPath( request ),
                securityService.getRestoreOptions( user.getRestoreToken() ) ) )
        {
            throw new WebMessageException( error( "Failed to send invite message" ) );
        }
    }

    @PostMapping( "/{id}/reset" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void resetToInvite( @PathVariable String id, HttpServletRequest request )
        throws NotFoundException,
        ForbiddenException,
        ConflictException
    {
        User user = userService.getUser( id );
        if ( user == null )
        {
            throw new NotFoundException( User.class, id );
        }
        ErrorCode errorCode = securityService.validateRestore( user );
        if ( errorCode != null )
        {
            throw new ConflictException( errorCode );
        }
        User currentUser = currentUserService.getCurrentUser();
        if ( !aclService.canUpdate( currentUser, user ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to update this user." );
        }
        if ( !userService.canAddOrUpdateUser( getUids( user.getGroups() ), currentUser ) )
        {
            throw new ForbiddenException(
                "You must have permissions manage at least one user group for the user." );
        }

        securityService.prepareUserForInvite( user );
        securityService.sendRestoreOrInviteMessage( user, ContextUtils.getContextPath( request ),
            RestoreOptions.RECOVER_PASSWORD_OPTION );
    }

    @SuppressWarnings( "unchecked" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_REPLICATE_USER')" )
    @PostMapping( "/{uid}/replica" )
    @ResponseBody
    public WebMessage replicateUser( @PathVariable String uid,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException,
        ForbiddenException,
        ConflictException
    {
        User existingUser = userService.getUser( uid );
        if ( existingUser == null )
        {
            return conflict( "User not found: " + uid );
        }

        User currentUser = currentUserService.getCurrentUser();

        validateCreateUser( existingUser, currentUser );

        Map<String, String> auth = renderService.fromJson( request.getInputStream(), Map.class );

        String username = StringUtils.trimToNull( auth != null ? auth.get( "username" ) : null );
        String password = StringUtils.trimToNull( auth != null ? auth.get( "password" ) : null );

        if ( auth == null || username == null )
        {
            return conflict( "Username must be specified" );
        }

        if ( !ValidationUtils.usernameIsValid( username, false ) )
        {
            return conflict( "Username is not valid" );
        }

        if ( userService.getUserByUsername( username ) != null )
        {
            return conflict( "Username already taken: " + username );
        }

        if ( password == null )
        {
            return conflict( "Password must be specified" );
        }

        CredentialsInfo credentialsInfo = new CredentialsInfo( username, password,
            existingUser.getEmail() != null ? existingUser.getEmail() : "", false );

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        if ( !result.isValid() )
        {
            return conflict( result.getErrorMessage() );
        }

        User userReplica = new User();
        mergeService.merge( new MergeParams<>( existingUser, userReplica )
            .setMergeMode( MergeMode.MERGE ) );
        copyAttributeValues( userReplica );
        userReplica.setId( 0 );
        userReplica.setUuid( UUID.randomUUID() );
        userReplica.setUid( CodeGenerator.generateUid() );
        userReplica.setCode( null );
        userReplica.setCreated( new Date() );
        userReplica.setLdapId( null );
        userReplica.setOpenId( null );
        userReplica.setUsername( username );
        userReplica.setLastLogin( null );
        userService.encodeAndSetPassword( userReplica, password );

        userService.addUser( userReplica );

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

        return created( "User replica created" )
            .setLocation( UserSchemaDescriptor.API_ENDPOINT + "/" + userReplica.getUid() );
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

    /**
     * "Disable two-factor authentication for the user with the given uid."
     * <p>
     *
     * @param uid The uid of the user to disable two-factor authentication for.
     * @param currentUser This is the user that is currently logged in.
     *
     * @return A WebMessage object.
     */
    @PostMapping( "/{uid}/twoFA/disabled" )
    @ResponseBody
    public WebMessage disableTwoFa( @PathVariable( "uid" ) String uid, @CurrentUser User currentUser )
    {
        List<ErrorReport> errors = new ArrayList<>();
        userService.privilegedTwoFactorDisable( currentUser, uid, errors::add );

        if ( errors.isEmpty() )
        {
            return WebMessageUtils.ok();
        }

        return WebMessageUtils.errorReports( errors );
    }

    // -------------------------------------------------------------------------
    // PUT
    // -------------------------------------------------------------------------

    @Override
    @PutMapping( value = "/{uid}", consumes = { APPLICATION_XML_VALUE,
        TEXT_XML_VALUE }, produces = APPLICATION_XML_VALUE )
    @ResponseBody
    public WebMessage putXmlObject( @PathVariable( "uid" ) String pvUid, @CurrentUser User currentUser,
        HttpServletRequest request,
        HttpServletResponse response )
        throws IOException,
        ForbiddenException,
        ConflictException,
        NotFoundException
    {
        User parsed = renderService.fromXml( request.getInputStream(), getEntityClass() );

        return importReport( updateUser( pvUid, parsed ) )
            .withPlainResponseBefore( DhisApiVersion.V38 );
    }

    @Override
    @PutMapping( value = "/{uid}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage putJsonObject( @PathVariable( "uid" ) String pvUid, @CurrentUser User currentUser,
        HttpServletRequest request )
        throws IOException,
        ConflictException,
        ForbiddenException,
        NotFoundException
    {
        User inputUser = renderService.fromJson( request.getInputStream(), getEntityClass() );

        User user = getEntity( pvUid );

        // TODO: To remove when we remove old UserCredentials compatibility
        populateUserCredentialsDtoCopyOnlyChanges( user, inputUser );

        return importReport( updateUser( pvUid, inputUser ) )
            .withPlainResponseBefore( DhisApiVersion.V38 );
    }

    protected ImportReport updateUser( String userUid, User inputUser )
        throws ConflictException,
        ForbiddenException,
        NotFoundException
    {
        User user = getEntity( userUid );

        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canUpdate( currentUser, user ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to update this user." );
        }

        // force initialization of all authorities of current user in order to
        // prevent cases where user must be reloaded later
        // (in case it gets detached)
        currentUser.getAllAuthorities();

        inputUser.setId( user.getId() );
        inputUser.setUid( userUid );
        mergeLastLoginAttribute( user, inputUser );

        boolean isPasswordChangeAttempt = inputUser.getPassword() != null;

        List<String> groupsUids = getUids( inputUser.getGroups() );

        if ( !userService.canAddOrUpdateUser( groupsUids, currentUser )
            || !currentUser.canModifyUser( user ) )
        {
            throw new ConflictException(
                "You must have permissions to create user, " +
                    "or ability to manage at least one user group for the user." );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );
        params.setImportReportMode( ImportReportMode.FULL );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.addObject( inputUser );

        ImportReport importReport = importService.importMetadata( params );

        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            updateUserGroups( userUid, inputUser, currentUser );

            // If it was a pw change attempt (input.pw != null) and update was
            // success we assume password has changed...
            // We chose to expire the special case if password is set to the
            // same. i.e. no before & after equals pw check
            if ( isPasswordChangeAttempt )
            {
                userService.expireActiveSessions( inputUser );
            }
        }

        return importReport;
    }

    protected void updateUserGroups( String userUid, User parsed, User currentUser )
    {
        User user = userService.getUser( userUid );

        if ( currentUser != null && currentUser.getId() == user.getId() )
        {
            currentUser = currentUserService.getCurrentUser();
        }

        List<String> uids = getUids( parsed.getGroups() );

        userGroupService.updateUserGroups( user, uids, currentUser );
    }

    // -------------------------------------------------------------------------
    // PATCH
    // -------------------------------------------------------------------------

    @Override
    protected void prePatchEntity( User oldEntity, User newEntity )
    {
        // TODO: To remove when we remove old UserCredentials compatibility
        populateUserCredentialsDtoCopyOnlyChanges( oldEntity, newEntity );
    }

    @Override
    protected void postPatchEntity( JsonPatch patch, User entityAfter )
    {
        // Make sure we always expire all the user's active sessions if we
        // have disabled the user.
        if ( entityAfter != null && entityAfter.isDisabled() )
        {
            userService.expireActiveSessions( entityAfter );
        }

        updateUserGroups( patch, entityAfter );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    protected void preDeleteEntity( User entity )
        throws ConflictException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !userService.canAddOrUpdateUser( getUids( entity.getGroups() ), currentUser )
            || !currentUser.canModifyUser( entity ) )
        {
            throw new ConflictException(
                "You must have permissions to create user, or ability to manage at least one user group for the user." );
        }

        if ( userService.isLastSuperUser( entity ) )
        {
            throw new ConflictException( "Can not remove the last super user." );
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
        throws ForbiddenException,
        ConflictException
    {
        if ( !aclService.canCreate( currentUser, getEntityClass() ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to create this object." );
        }

        if ( !userService.canAddOrUpdateUser( getUids( user.getGroups() ), currentUser ) )
        {
            throw new ConflictException(
                "You must have permissions to create user, or ability to manage at least one user group for the user." );
        }

        List<String> uids = getUids( user.getGroups() );

        for ( String uid : uids )
        {
            if ( !userGroupService.canAddOrRemoveMember( uid, currentUser ) )
            {
                throw new ConflictException( "You don't have permissions to add user to user group: " + uid );
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
        throws ForbiddenException,
        ConflictException
    {
        if ( user == null )
        {
            throw new ConflictException( "User is not present" );
        }

        validateCreateUser( user, currentUser );

        ErrorCode errorCode = securityService.validateInvite( user );

        if ( errorCode != null )
        {
            throw new IllegalQueryException( errorCode );
        }
    }

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
                .sendRestoreOrInviteMessage( user, ContextUtils.getContextPath( request ),
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
     * Make a copy of any existing attribute values, so they can be saved as new
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
        if ( target == null )
        {
            return target;
        }

        if ( target.getLastLogin() != null )
        {
            return target;
        }

        if ( source != null && source.getLastLogin() != null )
        {
            target.setLastLogin( source.getLastLogin() );
        }

        return target;
    }

    /**
     * Either disable or enable a user account
     *
     * @param uid the unique id of the user to enable or disable
     * @param disable boolean value, true for disable, false for enable
     *
     * @throws WebMessageException thrown if "current" user is not allowed to
     *         modify the user
     */
    private void setDisabled( String uid, boolean disable )
        throws WebMessageException
    {
        User userToModify = userService.getUser( uid );
        checkCurrentUserCanModify( userToModify );

        if ( userToModify.isDisabled() != disable )
        {
            userToModify.setDisabled( disable );
            userService.updateUser( userToModify );
        }

        if ( disable )
        {
            userService.expireActiveSessions( userToModify );
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
            || !currentUser.canModifyUser( userToModify ) )
        {
            throw new WebMessageException( conflict(
                "You must have permissions to create user, or ability to manage at least one user group for the user." ) );
        }
    }

    private void setExpires( String uid, Date accountExpiry )
        throws WebMessageException
    {
        User userToModify = userService.getUser( uid );
        checkCurrentUserCanModify( userToModify );

        User user = userToModify;
        user.setAccountExpiry( accountExpiry );
        userService.updateUser( user );

        if ( userService.isAccountExpired( user ) )
        {
            userService.expireActiveSessions( user );
        }
    }

    /**
     * Support patching user.userGroups relation which User is not the owner
     */
    private void updateUserGroups( JsonPatch patch, User user )
    {
        if ( ObjectUtils.anyNull( patch, user ) )
        {
            return;
        }

        for ( JsonPatchOperation op : patch.getOperations() )
        {
            JsonPointer userGroups = op.getPath().matchProperty( "userGroups" );
            if ( userGroups == null )
            {
                continue;
            }

            String opName = op.getOp();
            if ( StringUtils.equalsAny( opName, JsonPatchOperation.ADD_OPERATION,
                JsonPatchOperation.REPLACE_OPERATION ) )
            {
                List<String> groupIds = new ArrayList<>();
                ((AddOperation) op).getValue().elements()
                    .forEachRemaining( node -> groupIds.add( node.get( "id" ).asText() ) );
                userGroupService.updateUserGroups( user, groupIds, currentUserService.getCurrentUser() );
            }
        }
    }
}
