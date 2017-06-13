package org.hisp.dhis.dxf2.dataset;

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

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Halvdan Hoem Grelland
 */
public class ExportParams
{
    private Set<DataSet> dataSets = new HashSet<>();

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<OrganisationUnitGroup> organisationUnitGroups = new HashSet<>();

    private boolean includeChildren = false;

    private Set<Period> periods = new HashSet<>();

    private Date startDate;

    private Date endDate;

    private Date created;

    private String createdDuration;

    private Integer limit;

    private IdSchemes outputIdSchemes;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ExportParams()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasOrganisationUnits()
    {
        return !organisationUnits.isEmpty();
    }

    public boolean hasOrganisationUnitGroups()
    {
        return !organisationUnitGroups.isEmpty();
    }

    public boolean hasStartEndDate()
    {
        return startDate != null && endDate != null;
    }

    public boolean hasPeriods()
    {
        return !periods.isEmpty();
    }

    public boolean hasLimit()
    {
        return limit != null;
    }

    public boolean hasCreated()
    {
        return created != null;
    }

    public boolean hasCreatedDuration()
    {
        return StringUtils.isNotBlank( createdDuration );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "dataSets", dataSets )
            .add( "organisationUnits", organisationUnits )
            .add( "organisationUnitGroups", organisationUnitGroups )
            .add( "includeChildren", includeChildren )
            .add( "periods", periods )
            .add( "startDate", startDate )
            .add( "endDate", endDate )
            .add( "created", created )
            .add( "createdDuration", createdDuration )
            .add( "limit", limit )
            .add( "outputIdSchemes", outputIdSchemes )
            .toString();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Set<DataSet> getDataSets()
    {
        return dataSets;
    }

    public ExportParams setDataSets( Set<DataSet> dataSets )
    {
        this.dataSets = dataSets;
        return this;
    }

    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public ExportParams setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
        return this;
    }

    public Set<OrganisationUnitGroup> getOrganisationUnitGroups()
    {
        return organisationUnitGroups;
    }

    public ExportParams setOrganisationUnitGroups( Set<OrganisationUnitGroup> organisationUnitGroups )
    {
        this.organisationUnitGroups = organisationUnitGroups;
        return this;
    }

    public boolean isIncludeChildren()
    {
        return includeChildren;
    }

    public ExportParams setIncludeChildren( boolean includeChildren )
    {
        this.includeChildren = includeChildren;
        return this;
    }

    public Set<Period> getPeriods()
    {
        return periods;
    }

    public ExportParams setPeriods( Set<Period> periods )
    {
        this.periods = periods;
        return this;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public ExportParams setStartDate( Date startDate )
    {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public ExportParams setEndDate( Date endDate )
    {
        this.endDate = endDate;
        return this;
    }

    public Date getCreated()
    {
        return created;
    }

    public ExportParams setCreated( Date created )
    {
        this.created = created;
        return this;
    }

    public String getCreatedDuration()
    {
        return createdDuration;
    }

    public ExportParams setCreatedDuration( String createdDuration )
    {
        this.createdDuration = createdDuration;
        return this;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public ExportParams setLimit( Integer limit )
    {
        this.limit = limit;
        return this;
    }

    public IdSchemes getOutputIdSchemes()
    {
        return outputIdSchemes;
    }

    public ExportParams setOutputIdSchemes( IdSchemes outputIdSchemes )
    {
        this.outputIdSchemes = outputIdSchemes;
        return this;
    }
}
