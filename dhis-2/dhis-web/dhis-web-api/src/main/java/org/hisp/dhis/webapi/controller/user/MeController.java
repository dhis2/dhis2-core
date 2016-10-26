package org.hisp.dhis.webapi.controller.user;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion.Version;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.user.Dashboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = "/me", method = RequestMethod.GET )
@ApiVersion( { Version.V24, Version.V25 } )
public class MeController
{
    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

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

    private static final Set<String> USER_SETTING_NAMES = Sets.newHashSet(
        UserSettingKey.values() ).stream().map( UserSettingKey::getName ).collect( Collectors.toSet() );

    @RequestMapping( value = "", method = RequestMethod.GET )
    public void getCurrentUser( HttpServletResponse response ) throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( User.class, Collections.singletonList( currentUser ), fields );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        RootNode rootNode = NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) );

        rootNode.addChild( new ComplexNode( "settings" ) ).addChildren(
            NodeUtils.createSimples( userSettingService.getUserSettingsWithFallbackByUserAsMap( currentUser, USER_SETTING_NAMES, true ) ) );

        rootNode.addChild( new CollectionNode( "authorities" ) ).addChildren(
            NodeUtils.createSimples( currentUser.getUserCredentials().getAllAuthorities() ) );

        nodeService.serialize( rootNode, "application/json", response.getOutputStream() );
    }

    @RequestMapping( value = "", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public void updateCurrentUser( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        User user = renderService.fromJson( request.getInputStream(), User.class );
        merge( currentUser, user );

        if ( user.getUserCredentials() != null )
        {
            updatePassword( currentUser, user.getUserCredentials().getPassword() );
        }

        manager.update( currentUser );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( User.class, Collections.singletonList( currentUser ), fields );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        nodeService.serialize( NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) ), "application/json", response.getOutputStream() );
    }

    @RequestMapping( value = { "/authorization", "/authorities" } )
    public void getAuthorities( HttpServletResponse response ) throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), currentUser.getUserCredentials().getAllAuthorities() );
    }

    @RequestMapping( value = { "/authorization/{authority}", "/authorities/{authority}" } )
    public void haveAuthority( HttpServletResponse response, @PathVariable String authority ) throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        boolean hasAuthority = currentUser.getUserCredentials().isAuthorized( authority );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), hasAuthority );
    }

    @RequestMapping( value = "/settings" )
    public void getSettings( HttpServletResponse response ) throws IOException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Map<String, Serializable> userSettings = userSettingService.getUserSettingsWithFallbackByUserAsMap(
            currentUser, USER_SETTING_NAMES, true );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), userSettings );
    }

    @RequestMapping( value = "/settings/{key}" )
    public void getSetting( HttpServletResponse response, @PathVariable String key ) throws IOException, WebMessageException, NotAuthenticatedException
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
        renderService.toJson( response.getOutputStream(), value );
    }

    @RequestMapping( value = "/password", method = { RequestMethod.POST, RequestMethod.PUT }, consumes = "text/*" )
    public @ResponseBody RootNode changePassword( @RequestBody String password, HttpServletResponse response )
        throws WebMessageException, NotAuthenticatedException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        if ( !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Password must have at least 8 characters, one digit, one uppercase" ) );
        }

        updatePassword( currentUser, password );
        manager.update( currentUser );

        return null;
    }

    @RequestMapping( value = "/verifyPassword", method = RequestMethod.POST, consumes = "text/*" )
    public @ResponseBody RootNode verifyPasswordText( @RequestBody String password, HttpServletResponse response )
        throws WebMessageException
    {
        return verifyPasswordInternal( password, getCurrentUserOrThrow() );
    }

    @RequestMapping( value = "/verifyPassword", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody RootNode verifyPasswordJson( @RequestBody Map<String, String> body, HttpServletResponse response )
        throws WebMessageException
    {
        return verifyPasswordInternal( body.get( "password" ), getCurrentUserOrThrow() );
    }

    @RequestMapping( value = "/dashboard" )
    public @ResponseBody Dashboard getDashboard( HttpServletResponse response ) throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        Dashboard dashboard = new Dashboard();
        dashboard.setUnreadMessageConversations( messageService.getUnreadMessageConversationCount() );
        dashboard.setUnreadInterpretations( interpretationService.getNewInterpretationCount() );

        return dashboard;
    }

    //------------------------------------------------------------------------------------------------
    // Supportive methods
    //------------------------------------------------------------------------------------------------

    private RootNode verifyPasswordInternal( String password, User currentUser )
        throws WebMessageException
    {
        if ( password == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Required attribute 'password' missing or null." ) );
        }

        boolean valid = passwordManager.matches( password, currentUser.getUserCredentials().getPassword() );

        RootNode rootNode = NodeUtils.createRootNode( "response" );
        rootNode.addChild( new SimpleNode( "isCorrectPassword", valid ) );

        return rootNode;
    }

    private User getCurrentUserOrThrow() throws WebMessageException
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

        if ( user.getBirthday() != null )
        {
            currentUser.setBirthday( user.getBirthday() );
        }

        currentUser.setNationality( stringWithDefault( user.getNationality(), currentUser.getNationality() ) );
        currentUser.setEmployer( stringWithDefault( user.getEmployer(), currentUser.getEmployer() ) );
        currentUser.setEducation( stringWithDefault( user.getEducation(), currentUser.getEducation() ) );
        currentUser.setInterests( stringWithDefault( user.getInterests(), currentUser.getInterests() ) );
        currentUser.setLanguages( stringWithDefault( user.getLanguages(), currentUser.getLanguages() ) );
    }

    private void updatePassword( User currentUser, String password ) throws WebMessageException
    {
        if ( !StringUtils.isEmpty( password ) )
        {
            if ( ValidationUtils.passwordIsValid( password ) )
            {
                userService.encodeAndSetPassword( currentUser.getUserCredentials(), password );
            }
            else
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Invalid password format." ) );
            }
        }
    }

    private String stringWithDefault( String value, String defaultValue )
    {
        return !StringUtils.isEmpty( value ) ? value : defaultValue;
    }
}
