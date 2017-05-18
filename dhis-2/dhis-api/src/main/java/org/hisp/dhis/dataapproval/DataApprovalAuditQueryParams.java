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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.MoreObjects;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Jim Grace
 */
public class DataApprovalAuditQueryParams
{
    /**
     * Approval levels to include.
     */
    private Set<DataApprovalLevel> levels = new HashSet<>();

    /**
     * Workflows to include.
     */
    private Set<DataApprovalWorkflow> workflows = new HashSet<>();

    /**
     * OrganisationUnits to include.
     */
    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    /**
     * AttributeOptionCombos to include.
     */
    private Set<DataElementCategoryOptionCombo> attributeOptionCombos = new HashSet<>();

    /**
     * Starting date.
     */
    private Date startDate = null;

    /**
     * Ending date.
     */
    private Date endDate = null;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DataApprovalAuditQueryParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasWorkflows()
    {
        return workflows != null && !workflows.isEmpty();
    }

    public boolean hasLevels()
    {
        return levels != null && !levels.isEmpty();
    }

    public boolean hasOrganisationUnits()
    {
        return organisationUnits != null && !organisationUnits.isEmpty();
    }

    public boolean hasAttributeOptionCombos()
    {
        return attributeOptionCombos != null && !attributeOptionCombos.isEmpty();
    }

    public boolean hasStartDate()
    {
        return startDate != null;
    }

    public boolean hasEndDate()
    {
        return endDate != null;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this ).
            add( "levels", levels ).
            add( "workflows", workflows ).
            add( "organisationUnits", organisationUnits ).
            add( "attributeOptionCombos", attributeOptionCombos ).
            add( "startDate", startDate ).
            add( "endDate", endDate ).toString();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Set<DataApprovalLevel> getLevels()
    {
        return levels;
    }

    public void setLevels( Set<DataApprovalLevel> levels )
    {
        this.levels = levels;
    }

    public Set<DataApprovalWorkflow> getWorkflows()
    {
        return workflows;
    }

    public void setWorkflows( Set<DataApprovalWorkflow> workflows )
    {
        this.workflows = workflows;
    }

    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    public Set<DataElementCategoryOptionCombo> getAttributeOptionCombos()
    {
        return attributeOptionCombos;
    }

    public void setAttributeOptionCombos( Set<DataElementCategoryOptionCombo> attributeOptionCombos )
    {
        this.attributeOptionCombos = attributeOptionCombos;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }
}
