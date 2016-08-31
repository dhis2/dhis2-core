package org.hisp.dhis.webapi.controller.user;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.schema.descriptors.UserRoleSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = UserRoleSchemaDescriptor.API_ENDPOINT )
public class UserRoleController
    extends AbstractCrudController<UserAuthorityGroup>
{
    @Autowired
    private UserService userService;

    @Override
    protected List<UserAuthorityGroup> getEntityList( WebMetaData metaData, WebOptions options, List<String> filters, List<Order> orders )
    {
        List<UserAuthorityGroup> entityList = super.getEntityList( metaData, options, filters, orders );

        if ( options.getOptions().containsKey( "canIssue" ) && Boolean.parseBoolean( options.getOptions().get( "canIssue" ) ) )
        {
            userService.canIssueFilter( entityList );
        }

        return entityList;
    }

    @RequestMapping( value = "/{id}/users/{userId}", method = { RequestMethod.POST, RequestMethod.PUT } )
    public void addUserToRole( @PathVariable( value = "id" ) String pvId, @PathVariable( "userId" ) String pvUserId, HttpServletResponse response ) throws WebMessageException
    {
        UserAuthorityGroup userAuthorityGroup = userService.getUserAuthorityGroup( pvId );

        if ( userAuthorityGroup == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "UserRole does not exist: " + pvId ) );
        }

        User user = userService.getUser( pvUserId );

        if ( user == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "User does not exist: " + pvId ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), userAuthorityGroup ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this object." );
        }

        if ( !user.getUserCredentials().getUserAuthorityGroups().contains( userAuthorityGroup ) )
        {
            user.getUserCredentials().getUserAuthorityGroups().add( userAuthorityGroup );
            userService.updateUserCredentials( user.getUserCredentials() );
        }

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }

    @RequestMapping( value = "/{id}/users/{userId}", method = RequestMethod.DELETE )
    public void removeUserFromRole( @PathVariable( value = "id" ) String pvId, @PathVariable( "userId" ) String pvUserId, HttpServletResponse response ) throws WebMessageException
    {
        UserAuthorityGroup userAuthorityGroup = userService.getUserAuthorityGroup( pvId );

        if ( userAuthorityGroup == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "UserRole does not exist: " + pvId ) );
        }

        User user = userService.getUser( pvUserId );

        if ( user == null || user.getUserCredentials() == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "User does not exist: " + pvId ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), userAuthorityGroup ) )
        {
            throw new DeleteAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        if ( user.getUserCredentials().getUserAuthorityGroups().contains( userAuthorityGroup ) )
        {
            user.getUserCredentials().getUserAuthorityGroups().remove( userAuthorityGroup );
            userService.updateUserCredentials( user.getUserCredentials() );
        }

        response.setStatus( HttpServletResponse.SC_NO_CONTENT );
    }
}
