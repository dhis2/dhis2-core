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
import java.util.Set;

import lombok.AllArgsConstructor;

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

    public void mergeDataSets( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getDataSets )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( target );
                o.removeOrganisationUnits( sources );
            } );
    }

    public void mergePrograms( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getPrograms )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( target );
                o.removeOrganisationUnits( sources );
            } );
    }

    public void mergeOrgUnitGroups( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getGroups )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( target );
                o.removeOrganisationUnits( sources );
            } );
    }

    public void mergeCategoryOptions( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getCategoryOptions )
            .flatMap( Collection::stream )
            .forEach( o -> {
                o.addOrganisationUnit( target );
                o.removeOrganisationUnits( sources );
            } );
    }

    public void mergeUsers( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        final ImmutableSet<User> users = ImmutableSet.<User> builder()
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setOrganisationUnits( sources ) ) )
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setDataViewOrganisationUnits( sources ) ) )
            .addAll( userService.getUsers( new UserQueryParams()
                .setCanSeeOwnUserAuthorityGroups( true )
                .setTeiSearchOrganisationUnits( sources ) ) )
            .build();

        users.forEach( o -> {
            o.addOrganisationUnit( target );
            o.removeOrganisationUnits( sources );
        } );
    }
}
