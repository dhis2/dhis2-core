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

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.schema.descriptors.UserRoleSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( { "user", "management" } )
@Controller
@RequestMapping( value = UserRoleSchemaDescriptor.API_ENDPOINT )
public class UserRoleController
    extends AbstractCrudController<UserRole>
{
    @Autowired
    private UserService userService;

    @Override
    protected List<UserRole> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws BadRequestException
    {
        List<UserRole> entityList = super.getEntityList( metadata, options, filters, orders );

        if ( options.getOptions().containsKey( "canIssue" )
            && Boolean.parseBoolean( options.getOptions().get( "canIssue" ) ) )
        {
            userService.canIssueFilter( entityList );
        }

        return entityList;
    }

    @RequestMapping( value = "/{id}/users/{userId}", method = { RequestMethod.POST, RequestMethod.PUT } )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void addUserToRole( @PathVariable( value = "id" ) String pvId, @PathVariable( "userId" ) String pvUserId,
        @CurrentUser User currentUser, HttpServletResponse response )
        throws NotFoundException,
        ForbiddenException
    {
        UserRole userRole = userService.getUserRole( pvId );

        if ( userRole == null )
        {
            throw new NotFoundException( getEntityClass(), pvId );
        }

        User user = userService.getUser( pvUserId );

        if ( user == null )
        {
            throw new NotFoundException( "User does not exist: " + pvUserId );
        }

        if ( !aclService.canUpdate( currentUser, userRole ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to update this object." );
        }

        if ( !user.getUserRoles().contains( userRole ) )
        {
            user.getUserRoles().add( userRole );
            userService.updateUser( user );
        }
    }

    @DeleteMapping( "/{id}/users/{userId}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeUserFromRole( @PathVariable( value = "id" ) String pvId,
        @PathVariable( "userId" ) String pvUserId, @CurrentUser User currentUser, HttpServletResponse response )
        throws NotFoundException,
        ForbiddenException
    {
        UserRole userRole = userService.getUserRole( pvId );

        if ( userRole == null )
        {
            throw new NotFoundException( getEntityClass(), pvId );
        }

        User user = userService.getUser( pvUserId );

        if ( user == null )
        {
            throw new NotFoundException( "User does not exist: " + pvUserId );
        }

        if ( !aclService.canUpdate( currentUser, userRole ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to delete this object." );
        }

        if ( user.getUserRoles().contains( userRole ) )
        {
            user.getUserRoles().remove( userRole );
            userService.updateUser( user );
        }
    }
}
