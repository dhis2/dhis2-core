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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.schema.descriptors.LegendSetSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( "ui" )
@Controller
@RequestMapping( value = LegendSetSchemaDescriptor.API_ENDPOINT )
public class LegendSetController
    extends AbstractCrudController<LegendSet>
{
    @Override
    @PreAuthorize( "hasRole('F_LEGEND_SET_PUBLIC_ADD') or hasRole('F_LEGEND_SET_PRIVATE_ADD') or hasRole('ALL')" )
    public WebMessage postJsonObject( HttpServletRequest request )
        throws ForbiddenException,
        ConflictException,
        IOException,
        HttpRequestMethodNotSupportedException,
        NotFoundException
    {
        return super.postJsonObject( request );
    }

    @Override
    @PreAuthorize( "hasRole('F_LEGEND_SET_PUBLIC_ADD') or hasRole('F_LEGEND_SET_PRIVATE_ADD')  or hasRole('ALL')" )
    public WebMessage putJsonObject( @PathVariable String uid, @CurrentUser User currentUser,
        HttpServletRequest request )
        throws ForbiddenException,
        ConflictException,
        NotFoundException,
        IOException,
        HttpRequestMethodNotSupportedException
    {
        return super.putJsonObject( uid, currentUser, request );
    }

    @Override
    @PreAuthorize( "hasRole('F_LEGEND_SET_DELETE') or hasRole('ALL')" )
    public WebMessage deleteObject( @PathVariable String uid, @CurrentUser User currentUser, HttpServletRequest request,
        HttpServletResponse response )
        throws ForbiddenException,
        ConflictException,
        NotFoundException,
        HttpRequestMethodNotSupportedException
    {
        return super.deleteObject( uid, currentUser, request, response );
    }
}
