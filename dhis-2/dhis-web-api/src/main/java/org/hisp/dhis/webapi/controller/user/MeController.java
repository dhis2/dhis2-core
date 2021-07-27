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

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.webdomain.Dashboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( "/me" )
public class MeController
{
    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserControllerUtils userControllerUtils;

    @Autowired
    protected ContextService contextService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private PasswordManager passwordManager;

    @Autowired
    private MessageService messageService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataApprovalLevelService approvalLevelService;

    private static final Set<UserSettingKey> USER_SETTING_KEYS = new HashSet<>(
        Sets.newHashSet( UserSettingKey.values() ) );

    @GetMapping
    public void getCurrentUser( HttpServletResponse response )
        throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( User.class,
            new FieldFilterParams( Collections.singletonList( user ), fields ) );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );

        RootNode rootNode = NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) );

        if ( fieldsContains( "settings", fields ) )
        {
            rootNode.addChild( new ComplexNode( "settings" ) ).addChildren(
                NodeUtils.createSimples(
                    userSettingService.getUserSettingsWithFallbackByUserAsMap( user, USER_SETTING_KEYS, true ) ) );
        }

        if ( fieldsContains( "authorities", fields ) )
        {
            rootNode.addChild( new CollectionNode( "authorities" ) ).addChildren(
                NodeUtils.createSimples( user.getUserCredentials().getAllAuthorities() ) );
        }

        if ( fieldsContains( "programs", fields ) )
        {
            rootNode.addChild( new CollectionNode( "programs" ) ).addChildren(
                NodeUtils.createSimples( programService.getUserPrograms().stream()
                    .map( BaseIdentifiableObject::getUid )
                    .collect( Collectors.toList() ) ) );
        }

        if ( fieldsContains( "dataSets", fields ) )
        {
            rootNode.addChild( new CollectionNode( "dataSets" ) ).addChildren(
                NodeUtils.createSimples( dataSetService.getUserDataRead( user ).stream()
                    .map( BaseIdentifiableObject::getUid )
                    .collect( Collectors.toList() ) ) );
        }

        nodeService.serialize( rootNode, "application/json", response.getOutputStream() );
    }

    private boolean fieldsContains( String key, List<String> fields )
    {
        for ( String field : fields )
        {
            if ( field.contains( key ) || field.equals( "*" ) || field.startsWith( ":" ) )
            {
                return true;
            }
        }

        return false;
    }

    @GetMapping( "/dataApprovalWorkflows" )
    public void getCurrentUserDataApprovalWorkflows( HttpServletResponse response )
        throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw new NotAuthenticatedException();
        }

        RootNode rootNode = userControllerUtils.getUserDataApprovalWorkflows( user );

        nodeService.serialize( rootNode, "application/json", response.getOutputStream() );
    }

    @PutMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void updateCurrentUser( HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        User user = renderService.fromJson( request.getInputStream(), User.class );
        merge( currentUser, user );

        if ( user.getWhatsApp() != null && !ValidationUtils.validateWhatsapp( user.getWhatsApp() ) )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Invalid format for WhatsApp value '" + user.getWhatsApp() + "'" ) );
        }

        manager.update( currentUser );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        CollectionNode collectionNode = fieldFilterService.toCollectionNode( User.class,
            new FieldFilterParams( Collections.singletonList( currentUser ), fields ) );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        nodeService.serialize( NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) ), "application/json",
            response.getOutputStream() );
    }

    @GetMapping( value = { "/authorization", "/authorities" } )
    public void getAuthorities( HttpServletResponse response )
        throws IOException,
        NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );
        renderService.toJson( response.getOutputStream(), currentUser.getUserCredentials().getAllAuthorities() );
    }

    @GetMapping( value = { "/authorization/{authority}", "/authorities/{authority}" } )
    public void hasAuthority( HttpServletResponse response, @PathVariable String authority )
        throws IOException,
        NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        boolean hasAuthority = currentUser.getUserCredentials().isAuthorized( authority );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );
        renderService.toJson( response.getOutputStream(), hasAuthority );
    }

    @GetMapping( "/settings" )
    public void getSettings( HttpServletResponse response )
        throws IOException,
        NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Map<String, Serializable> userSettings = userSettingService.getUserSettingsWithFallbackByUserAsMap(
            currentUser, USER_SETTING_KEYS, true );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );
        renderService.toJson( response.getOutputStream(), userSettings );
    }

    @GetMapping( "/settings/{key}" )
    public void getSetting( HttpServletResponse response, @PathVariable String key )
        throws IOException,
        WebMessageException,
        NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Optional<UserSettingKey> keyEnum = UserSettingKey.getByName( key );

        if ( !keyEnum.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Key is not supported: " + key ) );
        }

        Serializable value = userSettingService.getUserSetting( keyEnum.get(), currentUser );

        if ( value == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "User setting not found for key: " + key ) );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );
        renderService.toJson( response.getOutputStream(), value );
    }

    @PutMapping( value = "/changePassword", consumes = { "text/*", "application/*" } )
    @ResponseStatus( HttpStatus.ACCEPTED )
    public void changePassword( @RequestBody Map<String, String> body, HttpServletResponse response )
        throws WebMessageException,
        NotAuthenticatedException,
        IOException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        String oldPassword = body.get( "oldPassword" );
        String newPassword = body.get( "newPassword" );

        if ( StringUtils.isEmpty( oldPassword ) || StringUtils.isEmpty( newPassword ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "OldPassword and newPassword must be provided" ) );
        }

        boolean valid = passwordManager.matches( oldPassword, currentUser.getUserCredentials().getPassword() );

        if ( !valid )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "OldPassword is incorrect" ) );
        }

        updatePassword( currentUser, newPassword );
        manager.update( currentUser );

        userService.expireActiveSessions( currentUser.getUserCredentials() );
    }

    @PostMapping( value = "/verifyPassword", consumes = "text/*" )
    public @ResponseBody RootNode verifyPasswordText( @RequestBody String password, HttpServletResponse response )
        throws WebMessageException
    {
        return verifyPasswordInternal( password, getCurrentUserOrThrow() );
    }

    @PostMapping( value = "/validatePassword", consumes = "text/*" )
    public @ResponseBody RootNode validatePasswordText( @RequestBody String password, HttpServletResponse response )
        throws WebMessageException
    {
        return validatePasswordInternal( password, getCurrentUserOrThrow() );
    }

    @PostMapping( value = "/verifyPassword", consumes = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody RootNode verifyPasswordJson( @RequestBody Map<String, String> body,
        HttpServletResponse response )
        throws WebMessageException
    {
        return verifyPasswordInternal( body.get( "password" ), getCurrentUserOrThrow() );
    }

    @GetMapping( "/dashboard" )
    public @ResponseBody Dashboard getDashboard( HttpServletResponse response )
        throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setUnreadMessageConversations( messageService.getUnreadMessageConversationCount() );
        dashboard.setUnreadInterpretations( interpretationService.getNewInterpretationCount() );

        setNoStore( response );
        return dashboard;
    }

    @PostMapping( value = "/dashboard/interpretations/read" )
    @ResponseStatus( value = HttpStatus.NO_CONTENT )
    @ApiVersion( include = { DhisApiVersion.ALL, DhisApiVersion.DEFAULT } )
    public void updateInterpretationsLastRead()
    {
        interpretationService.updateCurrentUserLastChecked();
    }

    @GetMapping( value = "/dataApprovalLevels", produces = { "application/json", "text/*" } )
    public void getApprovalLevels( HttpServletResponse response )
        throws IOException
    {
        List<DataApprovalLevel> approvalLevels = approvalLevelService
            .getUserDataApprovalLevels( currentUserService.getCurrentUser() );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        setNoStore( response );
        renderService.toJson( response.getOutputStream(), approvalLevels );
    }

    // ------------------------------------------------------------------------------------------------
    // Supportive methods
    // ------------------------------------------------------------------------------------------------

    private RootNode verifyPasswordInternal( String password, User currentUser )
        throws WebMessageException
    {
        if ( password == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Required attribute 'password' missing or null." ) );
        }

        boolean valid = passwordManager.matches( password, currentUser.getUserCredentials().getPassword() );

        RootNode rootNode = NodeUtils.createRootNode( "response" );
        rootNode.addChild( new SimpleNode( "isCorrectPassword", valid ) );

        return rootNode;
    }

    private RootNode validatePasswordInternal( String password, User currentUser )
        throws WebMessageException
    {
        if ( password == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Required attribute 'password' missing or null." ) );
        }

        CredentialsInfo credentialsInfo = new CredentialsInfo( currentUser.getUsername(), password,
            currentUser.getEmail(), false );

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        RootNode rootNode = NodeUtils.createRootNode( "response" );
        rootNode.addChild( new SimpleNode( "isValidPassword", result.isValid() ) );

        if ( !result.isValid() )
        {
            rootNode.addChild( new SimpleNode( "errorMessage", result.getErrorMessage() ) );
        }

        return rootNode;
    }

    private User getCurrentUserOrThrow()
        throws WebMessageException
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null || user.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.unathorized( "Not authenticated" ) );
        }

        return user;
    }

    private void merge( User currentUser, User user )
    {
        currentUser.setFirstName( stringWithDefault( user.getFirstName(), currentUser.getFirstName() ) );
        currentUser.setSurname( stringWithDefault( user.getSurname(), currentUser.getSurname() ) );
        currentUser.setEmail( stringWithDefault( user.getEmail(), currentUser.getEmail() ) );
        currentUser.setPhoneNumber( stringWithDefault( user.getPhoneNumber(), currentUser.getPhoneNumber() ) );
        currentUser.setJobTitle( stringWithDefault( user.getJobTitle(), currentUser.getJobTitle() ) );
        currentUser.setIntroduction( stringWithDefault( user.getIntroduction(), currentUser.getIntroduction() ) );
        currentUser.setGender( stringWithDefault( user.getGender(), currentUser.getGender() ) );

        currentUser.setAvatar( user.getAvatar() != null ? user.getAvatar() : currentUser.getAvatar() );

        currentUser.setSkype( stringWithDefault( user.getSkype(), currentUser.getSkype() ) );
        currentUser.setFacebookMessenger(
            stringWithDefault( user.getFacebookMessenger(), currentUser.getFacebookMessenger() ) );
        currentUser.setTelegram( stringWithDefault( user.getTelegram(), currentUser.getTelegram() ) );
        currentUser.setWhatsApp( stringWithDefault( user.getWhatsApp(), currentUser.getWhatsApp() ) );
        currentUser.setTwitter( stringWithDefault( user.getTwitter(), currentUser.getTwitter() ) );

        if ( user.getBirthday() != null )
        {
            currentUser.setBirthday( user.getBirthday() );
        }

        currentUser.setNationality( stringWithDefault( user.getNationality(), currentUser.getNationality() ) );
        currentUser.setEmployer( stringWithDefault( user.getEmployer(), currentUser.getEmployer() ) );
        currentUser.setEducation( stringWithDefault( user.getEducation(), currentUser.getEducation() ) );
        currentUser.setInterests( stringWithDefault( user.getInterests(), currentUser.getInterests() ) );
        currentUser.setLanguages( stringWithDefault( user.getLanguages(), currentUser.getLanguages() ) );

        if ( user.getUserCredentials() != null && currentUser.getUserCredentials() != null )
        {
            UserCredentials userCredentials = user.getUserCredentials();
            currentUser.getUserCredentials().setTwoFA( userCredentials.isTwoFA() );
        }
    }

    private void updatePassword( User currentUser, String password )
        throws WebMessageException
    {
        if ( !StringUtils.isEmpty( password ) )
        {
            CredentialsInfo credentialsInfo = new CredentialsInfo( currentUser.getUsername(), password,
                currentUser.getEmail(), false );

            PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

            if ( result.isValid() )
            {
                userService.encodeAndSetPassword( currentUser.getUserCredentials(), password );
            }
            else
            {
                throw new WebMessageException( WebMessageUtils.conflict( result.getErrorMessage() ) );
            }
        }
    }

    private String stringWithDefault( String value, String defaultValue )
    {
        return !StringUtils.isEmpty( value ) ? value : defaultValue;
    }
}
