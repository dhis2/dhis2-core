package org.hisp.dhis.oum.action.organisationunitgroupset;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class ValidateGroupSetAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private Collection<Integer> selectedGroups = new HashSet<>();

    public void setSelectedGroups( Collection<Integer> selectedGroups )
    {
        this.selectedGroups = selectedGroups;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;

    public String getMessage()
    {
        return message;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        // ---------------------------------------------------------------------
        // Validate values
        // ---------------------------------------------------------------------

        if ( name != null && !name.trim().isEmpty() )
        {
            List<OrganisationUnitGroupSet> organisationUnitGroupSets = organisationUnitGroupService.getOrganisationUnitGroupSetByName( name );

            if ( !organisationUnitGroupSets.isEmpty() && (id == null || organisationUnitGroupSets.get( 0 ).getId() != id) )
            {
                message = i18n.getString( "name_in_use" );

                return ERROR;
            }
        }

        // ---------------------------------------------------------------------
        // When adding or updating an exclusive group set any unit in the
        // selected groups can not be a member of more than one group
        // ---------------------------------------------------------------------

        if ( !this.selectedGroups.isEmpty() )
        {
            List<OrganisationUnit> units = new ArrayList<>();

            for ( Integer groupId : selectedGroups )
            {
                units.addAll( organisationUnitGroupService.getOrganisationUnitGroup( groupId ).getMembers() );
            }

            Collection<OrganisationUnit> duplicates = ListUtils.getDuplicates( units, IdentifiableObjectNameComparator.INSTANCE );

            if ( duplicates.size() > 0 )
            {
                message = i18n.getString( "the_group_set_can_not_be_creat_bec_it_is_exc_and" ) + " "
                    + duplicates.iterator().next().getShortName() + " "
                    + i18n.getString( "is_a_member_of_more_than_one_selected_group" );

                return ERROR;
            }
        }

        message = "OK";

        return SUCCESS;
    }
}
