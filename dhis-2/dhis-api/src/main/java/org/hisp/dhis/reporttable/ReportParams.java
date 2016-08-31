package org.hisp.dhis.reporttable;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.DimensionalView;
import org.hisp.dhis.common.view.ExportView;

import java.io.Serializable;

/**
 * The ReportParams object represents report parameters for a ReportTable. Report
 * parameters are meant to make ReportTables more generic, as it can avoid having
 * dynamic, selectable parameters rather than static.
 *
 * @author Lars Helge Overland
 * @version $Id$
 */
@JacksonXmlRootElement( localName = "reportParams", namespace = DxfNamespaces.DXF_2_0)
public class ReportParams
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 2509958165452862235L;

    private Boolean paramReportingMonth; //TODO rename to paramReportingPeriod

    private Boolean paramGrandParentOrganisationUnit;

    private Boolean paramParentOrganisationUnit;

    private Boolean paramOrganisationUnit;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ReportParams()
    {
    }

    public ReportParams( boolean paramReportingMonth, boolean paramGrandParentOrganisationUnit, 
        boolean paramParentOrganisationUnit, boolean paramOrganisationUnit )
    {
        this.paramReportingMonth = paramReportingMonth;
        this.paramGrandParentOrganisationUnit = paramGrandParentOrganisationUnit;
        this.paramParentOrganisationUnit = paramParentOrganisationUnit;
        this.paramOrganisationUnit = paramOrganisationUnit;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isParamReportingMonth()
    {
        return paramReportingMonth != null && paramReportingMonth;
    }

    public boolean isParamGrandParentOrganisationUnit()
    {
        return paramGrandParentOrganisationUnit != null && paramGrandParentOrganisationUnit;
    }

    public boolean isParamParentOrganisationUnit()
    {
        return paramParentOrganisationUnit != null && paramParentOrganisationUnit;
    }

    public boolean isParamOrganisationUnit()
    {
        return paramOrganisationUnit != null && paramOrganisationUnit;
    }

    public boolean isSet()
    {
        return isParamReportingMonth() || isOrganisationUnitSet();
    }

    public boolean isOrganisationUnitSet()
    {
        return isParamGrandParentOrganisationUnit() ||
            isParamParentOrganisationUnit() || isParamOrganisationUnit();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( value = "paramReportingPeriod" )
    @JsonView( {DetailedView.class, ExportView.class, DimensionalView.class} )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0)
    public Boolean getParamReportingMonth()
    {
        return paramReportingMonth;
    }

    public void setParamReportingMonth( Boolean paramReportingMonth )
    {
        this.paramReportingMonth = paramReportingMonth;
    }

    @JsonProperty
    @JsonView( {DetailedView.class, ExportView.class} )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0)
    public Boolean getParamGrandParentOrganisationUnit()
    {
        return paramGrandParentOrganisationUnit;
    }

    public void setParamGrandParentOrganisationUnit( Boolean paramGrandParentOrganisationUnit )
    {
        this.paramGrandParentOrganisationUnit = paramGrandParentOrganisationUnit;
    }

    @JsonProperty
    @JsonView( {DetailedView.class, ExportView.class, DimensionalView.class} )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0)
    public Boolean getParamParentOrganisationUnit()
    {
        return paramParentOrganisationUnit;
    }

    public void setParamParentOrganisationUnit( Boolean paramParentOrganisationUnit )
    {
        this.paramParentOrganisationUnit = paramParentOrganisationUnit;
    }

    @JsonProperty
    @JsonView( {DetailedView.class, ExportView.class, DimensionalView.class} )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0)
    public Boolean getParamOrganisationUnit()
    {
        return paramOrganisationUnit;
    }

    public void setParamOrganisationUnit( Boolean paramOrganisationUnit )
    {
        this.paramOrganisationUnit = paramOrganisationUnit;
    }
}
