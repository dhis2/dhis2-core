package org.hisp.dhis.dxf2.dataset;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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

    private Set<DataElementCategoryOptionCombo> attributeOptionCombos = new HashSet<>();

    private Set<Period> periods = new HashSet<>();

    private Date startDate;

    private Date endDate;

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

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "dataSets", dataSets )
            .add( "organisationUnits", organisationUnits )
            .add( "attributeOptionCombos", attributeOptionCombos )
            .add( "periods", periods )
            .add( "startDate", startDate )
            .add( "endDate", endDate )
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

    public Set<DataElementCategoryOptionCombo> getAttributeOptionCombos()
    {
        return attributeOptionCombos;
    }

    public ExportParams setAttributeOptionCombos( Set<DataElementCategoryOptionCombo> attributeOptionCombos )
    {
        this.attributeOptionCombos = attributeOptionCombos;
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
