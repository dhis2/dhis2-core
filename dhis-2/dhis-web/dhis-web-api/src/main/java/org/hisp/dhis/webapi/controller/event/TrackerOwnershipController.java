package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Controller
@RequestMapping( value = TrackerOwnershipController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class TrackerOwnershipController
{
    public static final String RESOURCE_PATH = "/tracker/ownership";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipAccessManager;

    @Autowired
    protected FieldFilterService fieldFilterService;

    @Autowired
    protected ContextService contextService;

    @Autowired
    private WebMessageService webMessageService;

    // -------------------------------------------------------------------------
    // 1. Transfer ownership if the logged in user is part of the owner ou.
    // 2. Break the glass and override ownership.
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/transfer", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE )
    public void updateTrackerProgramOwner( @RequestParam String trackedEntityInstance, @RequestParam String program,
        @RequestParam String ou, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        trackerOwnershipAccessManager.transferOwnership( trackedEntityInstance, program, ou, false, false );

        webMessageService.send( WebMessageUtils.ok( "Ownership transferred" ), response, request );
    }

    @RequestMapping( value = "/override", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE )
    public void overrideOwnershipAccess( @RequestParam String trackedEntityInstance, @RequestParam String reason,
        @RequestParam String program, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        trackerOwnershipAccessManager.grantTemporaryOwnership( trackedEntityInstance, program,
            currentUserService.getCurrentUser(), reason );

        webMessageService.send( WebMessageUtils.ok( "Temporary Ownership granted" ), response, request );
    }
}
