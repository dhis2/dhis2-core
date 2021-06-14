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
package org.hisp.dhis.merge.orgunit.handler;

import java.util.Collection;

import lombok.AllArgsConstructor;

import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

/**
 * Merge handler for metadata objects.
 *
 * @author Lars Helge Overland
 */
@Service
@AllArgsConstructor
public class MetadataOrgUnitMergeHandler
{
    private final UserService userService;

    private final InterpretationService interpretationService;

    private final ConfigurationService configService;

    public void mergeDataSets( OrgUnitMergeRequest request )
    {
        request.getSources().stream()
            .map( OrganisationUnit::getDataSets )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( request.getTarget() );
                o.removeOrganisationUnits( request.getSources() );
            } );
    }

    public void mergePrograms( OrgUnitMergeRequest request )
    {
        request.getSources().stream()
            .map( OrganisationUnit::getPrograms )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( request.getTarget() );
                o.removeOrganisationUnits( request.getSources() );
            } );
    }

    public void mergeOrgUnitGroups( OrgUnitMergeRequest request )
    {
        request.getSources().stream()
            .map( OrganisationUnit::getGroups )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( request.getTarget() );
                o.removeOrganisationUnits( request.getSources() );
            } );
    }

    public void mergeCategoryOptions( OrgUnitMergeRequest request )
    {
        request.getSources().stream()
            .map( OrganisationUnit::getCategoryOptions )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( request.getTarget() );
                o.removeOrganisationUnits( request.getSources() );
            } );
    }

    public void mergeUsers( OrgUnitMergeRequest request )
    {
        final ImmutableSet<User> users = ImmutableSet.<User> builder()
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setOrganisationUnits( request.getSources() ) ) )
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setDataViewOrganisationUnits( request.getSources() ) ) )
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setTeiSearchOrganisationUnits( request.getSources() ) ) )
            .build();

        users.forEach( o -> {
            o.addOrganisationUnit( request.getTarget() );
            o.removeOrganisationUnits( request.getSources() );
        } );
    }

    public void mergeInterpretations( OrgUnitMergeRequest request )
    {
        interpretationService.migrateInterpretations( request.getSources(), request.getTarget() );
    }

    public void mergeConfiguration( OrgUnitMergeRequest request )
    {
        Configuration config = configService.getConfiguration();
        OrganisationUnit selfRegistrationOrgUnit = config.getSelfRegistrationOrgUnit();

        if ( selfRegistrationOrgUnit != null && request.getSources().contains( selfRegistrationOrgUnit ) )
        {
            config.setSelfRegistrationOrgUnit( request.getTarget() );
            configService.setConfiguration( config );
        }
    }
}
