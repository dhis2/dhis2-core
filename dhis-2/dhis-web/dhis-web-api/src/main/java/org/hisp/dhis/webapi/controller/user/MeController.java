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
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion.Version;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @RequestMapping( value = "", method = RequestMethod.GET )
    public @ResponseBody RootNode getCurrentUser() throws Exception
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

        return NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) );
    }

    @RequestMapping( value = "", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody RootNode updateCurrentUserJson( HttpServletRequest request ) throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        User user = renderService.fromJson( request.getInputStream(), User.class );
        merge( currentUser, user );
        updatePassword( currentUser, user );
        manager.update( currentUser );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( User.class, Collections.singletonList( currentUser ), fields );

        return NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) );
    }

    @RequestMapping( value = "", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE )
    public @ResponseBody RootNode updateCurrentUserXml( HttpServletRequest request ) throws Exception
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new NotAuthenticatedException();
        }

        User user = renderService.fromXml( request.getInputStream(), User.class );
        merge( currentUser, user );
        updatePassword( currentUser, user );
        manager.update( currentUser );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( User.class, Collections.singletonList( currentUser ), fields );

        return NodeUtils.createRootNode( collectionNode.getChildren().get( 0 ) );
    }

    @RequestMapping( value = "/authorization", produces = { "application/json", "text/*" } )
    public void getAuthorization( HttpServletResponse response ) throws IOException
    {
        User currentUser = currentUserService.getCurrentUser();

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), currentUser.getUserCredentials().getAllAuthorities() );
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

    private void updatePassword( User currentUser, User user ) throws WebMessageException
    {
        if ( user.getUserCredentials() != null && !StringUtils.isEmpty( user.getUserCredentials().getPassword() ) )
        {
            if ( ValidationUtils.passwordIsValid( user.getUserCredentials().getPassword() ) )
            {
                userService.encodeAndSetPassword( currentUser.getUserCredentials(), user.getUserCredentials().getPassword() );
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
