package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.user.User;

import java.util.Date;

/**
 * Current status of data approval for a given selection of data from a
 * data set. Returns the approval state and, if approved for this particular
 * selection, approval information.
 *
 * @author Jim Grace
 */
public class DataApprovalStatus
{
    /**
     * State of data approval for a given selection of data from a data set.
     */
    private DataApprovalState state;

    /**
     * If the selection of data is approved, the data approval level object
     * at which it is approved. If the selection is approved at more than
     * one level, this is for the highest level of approval.
     */
    private DataApprovalLevel approvedLevel;

    /**
     * If the selection of data is approved, the ID of the highest organisation
     * unit at which there is approval.
     */
    private int approvedOrgUnitId;

    /**
     * If the selection of data is approved, the approval level (same as above)
     * but if the selection is not approved, the level for this orgUnit at
     * which it could be approved (if any).
     */
    private DataApprovalLevel actionLevel;

    /**
     * If the selection is approved, the OrganisationUnit UID.
     */
    private String organisationUnitUid;
    
    /**
     * If the selection is approved, the OrganisationUnit name.
     */
    private String organisationUnitName;

    /**
     * If the selection is approved, the attribute category option combo UID.
     */
    private String attributeOptionComboUid;

    /**
     * If the selection is approved, whether or not it is accepted
     * at the highest level approved.
     */
    private boolean accepted;

    /**
     * Permissions granted for current user for the this approval state.
     */
    private DataApprovalPermissions permissions;

    /**
     * If the selection is approved, and if present (not always needed),
     * the date at which the highest level of approval was created.
     */
    private Date created;

    /**
     * If the selection is approved, and if present (not always needed),
     * The user who made this approval.
     */
    private User creator;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataApprovalStatus()
    {
    }

    public DataApprovalStatus( DataApprovalState state )
    {
        this.state = state;
    }

    public DataApprovalStatus( DataApprovalState state,
        DataApprovalLevel approvedLevel, int approvedOrgUnitId, DataApprovalLevel actionLevel,
        String organisationUnitUid, String organisationUnitName, String attributeOptionComboUid,
        boolean accepted, DataApprovalPermissions permissions )
    {
        this.state = state;
        this.approvedLevel = approvedLevel;
        this.approvedOrgUnitId = approvedOrgUnitId;
        this.actionLevel = actionLevel;
        this.organisationUnitUid = organisationUnitUid;
        this.organisationUnitName = organisationUnitName;
        this.attributeOptionComboUid = attributeOptionComboUid;
        this.accepted = accepted;
        this.permissions = permissions;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public DataApprovalState getState()
    {
        return state;
    }

    public void setState( DataApprovalState state )
    {
        this.state = state;
    }

    public DataApprovalLevel getApprovedLevel()
    {
        return approvedLevel;
    }

    public void setApprovedLevel( DataApprovalLevel approvedLevel )
    {
        this.approvedLevel = approvedLevel;
    }

    public int getApprovedOrgUnitId()
    {
        return approvedOrgUnitId;
    }

    public void setApprovedOrgUnitId( int approvedOrgUnitId )
    {
        this.approvedOrgUnitId = approvedOrgUnitId;
    }

    public DataApprovalLevel getActionLevel()
    {
        return actionLevel;
    }

    public void setActionLevel( DataApprovalLevel actionLevel )
    {
        this.actionLevel = actionLevel;
    }

    public String getOrganisationUnitUid()
    {
        return organisationUnitUid;
    }

    public void setOrganisationUnitUid( String organisationUnitUid )
    {
        this.organisationUnitUid = organisationUnitUid;
    }

    public String getOrganisationUnitName()
    {
        return organisationUnitName;
    }

    public void setOrganisationUnitName( String organisationUnitName )
    {
        this.organisationUnitName = organisationUnitName;
    }

    public String getAttributeOptionComboUid()
    {
        return attributeOptionComboUid;
    }

    public void setAttributeOptionComboUid( String attributeOptionComboUid )
    {
        this.attributeOptionComboUid = attributeOptionComboUid;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public void setAccepted( boolean accepted )
    {
        this.accepted = accepted;
    }

    public DataApprovalPermissions getPermissions()
    {
        return permissions;
    }

    public void setPermissions( DataApprovalPermissions permissions )
    {
        this.permissions = permissions;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    public User getCreator()
    {
        return creator;
    }

    public void setCreator( User creator )
    {
        this.creator = creator;
    }
}
