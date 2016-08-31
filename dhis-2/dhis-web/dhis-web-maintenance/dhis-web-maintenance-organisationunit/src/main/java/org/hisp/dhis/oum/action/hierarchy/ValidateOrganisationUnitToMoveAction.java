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
public class ValidateOrganisationUnitToMoveAction
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
        // Get tree root parent
        // ---------------------------------------------------------------------

        OrganisationUnit treeRootParent = selectionManager.getRootOrganisationUnitsParent();

        // ---------------------------------------------------------------------
        // If the organisation unit to move is the single root displayed in the
        // organisation unit tree, it cannot be moved.
        // ---------------------------------------------------------------------

        if ( organisationUnitToMove.getParent() == null )
        {
            int numberOfSiblings = organisationUnitService.getRootOrganisationUnits().size();

            if ( numberOfSiblings == 1 )
            {
                message = i18n.getString( "this_org_unit_cannot_moved" );

                return INPUT;
            }
        }
        else if ( treeRootParent != null && organisationUnitToMove.getParent().getId() == treeRootParent.getId() )
        {
            int numberOfSiblings = treeRootParent.getChildren().size();

            if ( numberOfSiblings == 1 )
            {
                message = i18n.getString( "this_org_unit_cannot_moved" );

                return INPUT;
            }
        }

        message = i18n.getString( "the_org_unit_to_move_is_approved" );

        return SUCCESS;
    }
}
