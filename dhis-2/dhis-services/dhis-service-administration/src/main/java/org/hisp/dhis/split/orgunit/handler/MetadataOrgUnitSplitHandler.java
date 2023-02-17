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
package org.hisp.dhis.split.orgunit.handler;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Service
@RequiredArgsConstructor
public class MetadataOrgUnitSplitHandler
{
    private final UserService userService;

    private final ConfigurationService configService;

    public void splitDataSets( OrgUnitSplitRequest request )
    {
        Sets.newHashSet( request.getSource().getDataSets() ).forEach( ds -> {
            ds.addOrganisationUnits( request.getTargets() );
            ds.removeOrganisationUnit( request.getSource() );
        } );
    }

    public void splitPrograms( OrgUnitSplitRequest request )
    {
        Sets.newHashSet( request.getSource().getPrograms() ).forEach( p -> {
            p.addOrganisationUnits( request.getTargets() );
            p.removeOrganisationUnit( request.getSource() );
        } );
    }

    public void splitOrgUnitGroups( OrgUnitSplitRequest request )
    {
        Sets.newHashSet( request.getSource().getGroups() ).forEach( oug -> {
            oug.addOrganisationUnits( request.getTargets() );
            oug.removeOrganisationUnit( request.getSource() );
        } );
    }

    public void splitCategoryOptions( OrgUnitSplitRequest request )
    {
        Sets.newHashSet( request.getSource().getCategoryOptions() ).forEach( co -> {
            co.addOrganisationUnits( request.getTargets() );
            co.removeOrganisationUnit( request.getSource() );
        } );
    }

    public void splitOrganisationUnits( OrgUnitSplitRequest request )
    {
        Sets.newHashSet( request.getSource().getChildren() ).forEach(
            ou -> ou.updateParent( request.getPrimaryTarget() ) );
    }

    public void splitUsers( OrgUnitSplitRequest request )
    {
        Set<OrganisationUnit> source = Sets.newHashSet( request.getSource() );

        List<User> dataCaptureUsers = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnRoles( true )
            .setOrganisationUnits( source ) );

        dataCaptureUsers.forEach( u -> {
            u.addOrganisationUnits( request.getTargets() );
            u.removeOrganisationUnit( request.getSource() );
        } );

        List<User> dataViewUsers = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnRoles( true )
            .setDataViewOrganisationUnits( source ) );

        dataViewUsers.forEach( u -> {
            u.getDataViewOrganisationUnits().addAll( request.getTargets() );
            u.getDataViewOrganisationUnits().remove( request.getSource() );
        } );

        List<User> teiSearchOrgUnits = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnRoles( true )
            .setTeiSearchOrganisationUnits( source ) );

        teiSearchOrgUnits.forEach( u -> {
            u.getTeiSearchOrganisationUnits().addAll( request.getTargets() );
            u.getTeiSearchOrganisationUnits().remove( request.getSource() );
        } );
    }

    public void splitConfiguration( OrgUnitSplitRequest request )
    {
        Configuration config = configService.getConfiguration();
        OrganisationUnit selfRegistrationOrgUnit = config.getSelfRegistrationOrgUnit();

        if ( selfRegistrationOrgUnit != null && request.getSource().equals( selfRegistrationOrgUnit ) )
        {
            config.setSelfRegistrationOrgUnit( request.getPrimaryTarget() );
            configService.setConfiguration( config );
        }
    }
}
