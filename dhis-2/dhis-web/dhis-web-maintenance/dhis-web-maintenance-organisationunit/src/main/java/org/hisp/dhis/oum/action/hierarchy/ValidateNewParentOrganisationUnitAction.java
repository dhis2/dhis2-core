package org.hisp.dhis.oum.action.hierarchy;

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

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.ouwt.manager.OrganisationUnitSelectionManager;

import com.opensymphony.xwork2.Action;

/**
 * @author Torgeir Lorange Ostby
 */
public class ValidateNewParentOrganisationUnitAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private OrganisationUnitSelectionManager selectionManager;

    public void setSelectionManager( OrganisationUnitSelectionManager selectionManager )
    {
        this.selectionManager = selectionManager;
    }

    // -------------------------------------------------------------------------
    // Input/output
    // -------------------------------------------------------------------------

    private Integer organisationUnitToMoveId;

    public void setOrganisationUnitToMoveId( Integer organisationUnitToMoveId )
    {
        this.organisationUnitToMoveId = organisationUnitToMoveId;
    }

    private Integer newParentOrganisationUnitId;

    public void setNewParentOrganisationUnitId( Integer newParentOrganisationUnitId )
    {
        this.newParentOrganisationUnitId = newParentOrganisationUnitId;
    }

    private String message;

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        // ---------------------------------------------------------------------
        // Get organisation unit to move
        // ---------------------------------------------------------------------

        OrganisationUnit organisationUnitToMove = organisationUnitService.getOrganisationUnit( organisationUnitToMoveId
            .intValue() );

        // ---------------------------------------------------------------------
        // New parent is null => move to root
        // ---------------------------------------------------------------------

        if ( newParentOrganisationUnitId == null )
        {
            // -----------------------------------------------------------------
            // Get tree root parent
            // -----------------------------------------------------------------

            OrganisationUnit treeRootParent = selectionManager.getRootOrganisationUnitsParent();

            // -----------------------------------------------------------------
            // Check root
            // -----------------------------------------------------------------

            if ( organisationUnitToMove.getParent() == null )
            {
                message = i18n.getString( "the_selected_organisation_unit_is_already_a_root" );

                return INPUT;
            }
            else if ( treeRootParent != null && organisationUnitToMove.getParent().getId() == treeRootParent.getId() )
            {
                message = i18n.getString( "the_selected_organisation_unit_is_already_a_root" );

                return INPUT;
            }
            else
            {
                message = i18n.getString( "the_selected_organisation_unit_will_be_moved_to_root" );

                return SUCCESS;
            }
        }

        // ---------------------------------------------------------------------
        // Get new parent organisation unit
        // ---------------------------------------------------------------------

        OrganisationUnit newParentOrganisationUnit = organisationUnitService
            .getOrganisationUnit( newParentOrganisationUnitId.intValue() );

        // ---------------------------------------------------------------------
        // Check if equal
        // ---------------------------------------------------------------------

        if ( organisationUnitToMove.getId() == newParentOrganisationUnit.getId() )
        {
            message = i18n.getString( "an_organisation_unit_cannot_be_moved_to_itself" );

            return INPUT;
        }

        // ---------------------------------------------------------------------
        // Check if path in wrong direction
        // ---------------------------------------------------------------------

        if ( pathFromParentToChild( organisationUnitToMove, newParentOrganisationUnit ) )
        {
            message = i18n.getString( "an_org_unit_cannot_be_moved_to_be_its_own_child" );

            return INPUT;
        }

        message = i18n.getString( "the_new_parent_org_unit_is_approved" );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private boolean pathFromParentToChild( OrganisationUnit parent, OrganisationUnit child )
    {
        OrganisationUnit tmp = child.getParent();

        while ( tmp != null )
        {
            if ( tmp.getId() == parent.getId() )
            {
                return true;
            }

            tmp = tmp.getParent();
        }

        return false;
    }
}
