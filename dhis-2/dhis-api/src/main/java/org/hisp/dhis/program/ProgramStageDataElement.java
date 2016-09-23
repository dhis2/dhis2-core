package org.hisp.dhis.program;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dataelement.DataElement;

/**
 * @author Viet Nguyen
 */
@JacksonXmlRootElement( localName = "programStageDataElement", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStageDataElement
    extends BaseIdentifiableObject
{
    private ProgramStage programStage;

    private DataElement dataElement;

    /**
     * True if this dataElement is mandatory in the dataEntryForm for this
     * programStage
     */
    private boolean compulsory = false;

    private Boolean allowProvidedElsewhere = false;

    private Integer sortOrder;

    private Boolean displayInReports = false;

    private Boolean allowFutureDate = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramStageDataElement()
    {
    }

    public ProgramStageDataElement( ProgramStage programStage, DataElement dataElement )
    {
        this.programStage = programStage;
        this.dataElement = dataElement;
    }

    public ProgramStageDataElement( ProgramStage programStage, DataElement dataElement, boolean compulsory )
    {
        this.programStage = programStage;
        this.dataElement = dataElement;
        this.compulsory = compulsory;
    }

    public ProgramStageDataElement( ProgramStage programStage, DataElement dataElement, boolean compulsory,
        Integer sortOrder )
    {
        this.programStage = programStage;
        this.dataElement = dataElement;
        this.compulsory = compulsory;
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getAllowProvidedElsewhere()
    {
        return allowProvidedElsewhere;
    }

    public void setAllowProvidedElsewhere( Boolean allowProvidedElsewhere )
    {
        this.allowProvidedElsewhere = allowProvidedElsewhere;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCompulsory()
    {
        return compulsory;
    }

    public void setCompulsory( boolean compulsory )
    {
        this.compulsory = compulsory;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( Integer sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayInReports()
    {
        return displayInReports;
    }

    public void setDisplayInReports( Boolean displayInReports )
    {
        this.displayInReports = displayInReports;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getAllowFutureDate()
    {
        return allowFutureDate;
    }

    public void setAllowFutureDate( Boolean allowFutureDate )
    {
        this.allowFutureDate = allowFutureDate;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        ProgramStageDataElement that = (ProgramStageDataElement) o;

        if ( dataElement != null ? !dataElement.equals( that.dataElement ) : that.dataElement != null ) return false;
        if ( programStage != null ? !programStage.equals( that.programStage ) : that.programStage != null ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = programStage != null ? programStage.hashCode() : 0;
        result = 31 * result + (dataElement != null ? dataElement.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "ProgramStageDataElement{" +
            "programStage=" + programStage +
            ", dataElement=" + dataElement +
            ", compulsory=" + compulsory +
            ", allowProvidedElsewhere=" + allowProvidedElsewhere +
            ", sortOrder=" + sortOrder +
            ", displayInReports=" + displayInReports +
            ", allowFutureDate=" + allowFutureDate +
            '}';
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramStageDataElement programStageDataElement = (ProgramStageDataElement) other;

            compulsory = programStageDataElement.isCompulsory();

            if ( mergeMode.isReplace() )
            {
                programStage = programStageDataElement.getProgramStage();
                dataElement = programStageDataElement.getDataElement();
                allowFutureDate = programStageDataElement.getAllowFutureDate();
                allowProvidedElsewhere = programStageDataElement.getAllowProvidedElsewhere();
                displayInReports = programStageDataElement.getDisplayInReports();
                sortOrder = programStageDataElement.getSortOrder();
            }
            else if ( mergeMode.isMerge() )
            {
                programStage = programStageDataElement.getProgramStage() == null ? programStage : programStageDataElement.getProgramStage();
                dataElement = programStageDataElement.getDataElement() == null ? dataElement : programStageDataElement.getDataElement();
                allowFutureDate = programStageDataElement.getAllowFutureDate() == null ? allowFutureDate : programStageDataElement.getAllowFutureDate();
                allowProvidedElsewhere = programStageDataElement.getAllowProvidedElsewhere() == null ? allowProvidedElsewhere : programStageDataElement.getAllowProvidedElsewhere();
                displayInReports = programStageDataElement.getDisplayInReports() == null ? displayInReports : programStageDataElement.getDisplayInReports();
                sortOrder = programStageDataElement.getSortOrder() == null ? sortOrder : programStageDataElement.getSortOrder();
            }
        }
    }
}
