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
package org.hisp.dhis.merge.orgunit.handler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

/**
 * Merge handler for metadata entities.
 *
 * @author Lars Helge Overland
 */
@Service
@AllArgsConstructor
public class MetadataOrgUnitMergeHandler
{
    private final UserService userService;

    private final ConfigurationService configService;

    public void mergeDataSets( OrgUnitMergeRequest request )
    {
        Set<DataSet> dataSets = request.getSources().stream()
            .map( OrganisationUnit::getDataSets )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        dataSets.forEach( ds -> {
            ds.addOrganisationUnit( request.getTarget() );
            ds.removeOrganisationUnits( request.getSources() );
        } );
    }

    public void mergePrograms( OrgUnitMergeRequest request )
    {
        Set<Program> programs = request.getSources().stream()
            .map( OrganisationUnit::getPrograms )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        programs.forEach( p -> {
            p.addOrganisationUnit( request.getTarget() );
            p.removeOrganisationUnits( request.getSources() );
        } );
    }

    public void mergeOrgUnitGroups( OrgUnitMergeRequest request )
    {
        Set<OrganisationUnitGroup> groups = request.getSources().stream()
            .map( OrganisationUnit::getGroups )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        groups.forEach( oug -> {
            oug.addOrganisationUnit( request.getTarget() );
            oug.removeOrganisationUnits( request.getSources() );
        } );
    }

    public void mergeCategoryOptions( OrgUnitMergeRequest request )
    {
        Set<CategoryOption> categoryOptions = request.getSources().stream()
            .map( OrganisationUnit::getCategoryOptions )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        categoryOptions.forEach( co -> {
            co.addOrganisationUnit( request.getTarget() );
            co.removeOrganisationUnits( request.getSources() );
        } );
    }

    public void mergeOrganisationUnits( OrgUnitMergeRequest request )
    {
        Set<OrganisationUnit> children = request.getSources().stream()
            .map( OrganisationUnit::getChildren )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        children.forEach(
            c -> c.updateParent( request.getTarget() ) );
    }

    public void mergeUsers( OrgUnitMergeRequest request )
    {
        List<User> dataCaptureUsers = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnUserRoles( true )
            .setOrganisationUnits( request.getSources() ) );

        dataCaptureUsers.forEach( u -> {
            u.addOrganisationUnit( request.getTarget() );
            u.removeOrganisationUnits( request.getSources() );
        } );

        List<User> dataViewUsers = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnUserRoles( true )
            .setDataViewOrganisationUnits( request.getSources() ) );

        dataViewUsers.forEach( u -> {
            u.getDataViewOrganisationUnits().add( request.getTarget() );
            u.getDataViewOrganisationUnits().removeAll( request.getSources() );
        } );

        List<User> teiSearchOrgUnits = userService.getUsers( new UserQueryParams()
            .setCanSeeOwnUserRoles( true )
            .setTeiSearchOrganisationUnits( request.getSources() ) );

        teiSearchOrgUnits.forEach( u -> {
            u.getTeiSearchOrganisationUnits().add( request.getTarget() );
            u.getTeiSearchOrganisationUnits().removeAll( request.getSources() );
        } );
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
