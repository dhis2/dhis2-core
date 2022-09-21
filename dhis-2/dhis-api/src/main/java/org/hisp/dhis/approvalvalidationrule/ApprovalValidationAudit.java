package org.hisp.dhis.approvalvalidationrule;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.MoreObjects;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Date;
import java.util.Objects;

/**
 * @author Mike Nelushi
 */
public class ApprovalValidationAudit
{
    private long id;

    private DataSet dataSet;
    
    private ApprovalValidationRule approvalValidationRule;

	private Period period;

    private OrganisationUnit organisationUnit;

    private CategoryOptionCombo attributeOptionCombo;

    private String modifiedBy;

    private Date created;

    private AuditType auditType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ApprovalValidationAudit()
    {
    }

    public ApprovalValidationAudit( ApprovalValidation approvalValidation, String modifiedBy, AuditType auditType )
    {
        this.dataSet = approvalValidation.getDataSet();
        this.approvalValidationRule = approvalValidation.getApprovalValidationRule();
        this.period = approvalValidation.getPeriod();
        this.organisationUnit = approvalValidation.getOrganisationUnit();
        this.attributeOptionCombo = approvalValidation.getAttributeOptionCombo();

        this.modifiedBy = modifiedBy;
        this.created = new Date();
        this.auditType = auditType;
    }

    public ApprovalValidationAudit( DataSet dataSet, ApprovalValidationRule approvalValidationRule, Period period, OrganisationUnit organisationUnit, 
    		CategoryOptionCombo attributeOptionCombo, String modifiedBy, AuditType auditType )
    {
        this.dataSet = dataSet;
        this.approvalValidationRule = approvalValidationRule;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.attributeOptionCombo = attributeOptionCombo;
        this.modifiedBy = modifiedBy;
        this.created = new Date();
        this.auditType = auditType;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash( dataSet, approvalValidationRule, period, organisationUnit, attributeOptionCombo, modifiedBy, created, auditType );
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null || getClass() != object.getClass() )
        {
            return false;
        }

        final ApprovalValidationAudit other = (ApprovalValidationAudit) object;

        return Objects.equals( this.dataSet, other.dataSet )
        	&& Objects.equals( this.approvalValidationRule, other.approvalValidationRule )
            && Objects.equals( this.period, other.period )
            && Objects.equals( this.organisationUnit, other.organisationUnit )
            && Objects.equals( this.attributeOptionCombo, other.attributeOptionCombo )
            && Objects.equals( this.modifiedBy, other.modifiedBy )
            && Objects.equals( this.created, other.created )
            && Objects.equals( this.auditType, other.auditType );
    }
    
    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "data set", dataSet )
            .add( "approval validation rule", approvalValidationRule )
            .add( "period", period )
            .add( "organisation unit", organisationUnit )
            .add( "attribute option combo", attributeOptionCombo )
            .add( "modified by", modifiedBy )
            .add( "created", created )
            .add( "audit type", auditType ).toString();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}
	
	@JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
	public ApprovalValidationRule getApprovalValidationRule() {
		return approvalValidationRule;
	}

	public void setApprovalValidationRule(ApprovalValidationRule approvalValidationRule) {
		this.approvalValidationRule = approvalValidationRule;
	}

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }


    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public CategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }


    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getModifiedBy()
    {
        return modifiedBy;
    }

    public void setModifiedBy( String modifiedBy )
    {
        this.modifiedBy = modifiedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AuditType getAuditType()
    {
        return auditType;
    }

    public void setAuditType( AuditType auditType )
    {
        this.auditType = auditType;
    }
}
