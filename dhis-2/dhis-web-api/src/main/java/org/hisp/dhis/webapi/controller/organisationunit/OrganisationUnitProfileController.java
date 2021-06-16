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
package org.hisp.dhis.webapi.controller.organisationunit;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hisp.dhis.orgunitprofile.OrgUnitProfile;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileData;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( value = "/orgUnitProfile" )
public class OrganisationUnitProfileController
{
    private final OrgUnitProfileService orgUnitProfileService;

    public OrganisationUnitProfileController( OrgUnitProfileService orgUnitProfileService )
    {
        checkNotNull( orgUnitProfileService );

        this.orgUnitProfileService = orgUnitProfileService;
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ORG_UNIT_PROFILE_ADD')" )
    @PostMapping( consumes = "application/json" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveProfile( @RequestBody OrgUnitProfile profile )
    {
        orgUnitProfileService.saveOrgUnitProfile( profile );
    }

    @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE )
    public OrgUnitProfile getProfile()
    {
        return orgUnitProfileService.getOrgUnitProfile();
    }

    @GetMapping( value = "/data/{uid}", produces = MediaType.APPLICATION_JSON_VALUE )
    public OrgUnitProfileData getProfileData( @PathVariable( value = "uid" ) String uid,
        @RequestParam( value = "period", required = false ) String isoPeriod )
    {
        return orgUnitProfileService.getOrgUnitProfileData( uid, isoPeriod );
    }
}
