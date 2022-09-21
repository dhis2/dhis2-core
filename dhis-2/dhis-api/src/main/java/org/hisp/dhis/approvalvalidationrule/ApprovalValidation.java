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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.hisp.dhis.common.BaseDataDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Date;

import javax.persistence.Column;

/**
 * Class representing a Approval Validation. The approvalValidation, period and org unit
 * properties make up a composite unique key.
 *
 * @author Mike Nelushi
 */
@JacksonXmlRootElement( localName = "approvalValidation", namespace = DxfNamespaces.DXF_2_0 )
public class ApprovalValidation extends BaseDataDimensionalItemObject implements MetadataObject
{
    //private long id;

    private Date created;

    private ApprovalValidationRule approvalValidationRule;
    
    private DataSet dataSet;

	private Period period;

    private OrganisationUnit organisationUnit;

    private CategoryOptionCombo attributeOptionCombo;
    
    private String storedBy;

    /**
     * Indicated whether this ValidationResult has generated a notification for users or not.
     */
    private Boolean notificationSent = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------     

    public ApprovalValidation()
    {
    }

    public ApprovalValidation( ApprovalValidationRule approvalValidationRule, DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo, String storedBy )
    {
        this.approvalValidationRule = approvalValidationRule;
        this.dataSet = dataSet;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.attributeOptionCombo = attributeOptionCombo;
        this.storedBy = storedBy;
    }

    public String getUid()
    {
        return approvalValidationRule != null ? approvalValidationRule.getUid() : null;
    }

    // -------------------------------------------------------------------------
    // Equals, compareTo, hashCode and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((approvalValidationRule == null) ? 0 : approvalValidationRule.hashCode());
        result = prime * result + ((dataSet == null) ? 0 : dataSet.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((organisationUnit == null) ? 0 : organisationUnit.hashCode());

        return result;
    }

    /**
     * Note: this method is called from threads in which it may not be possible
     * to initialize lazy Hibernate properties. So object properties to compare
     * must be chosen accordingly.
     */
    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        final ApprovalValidation other = (ApprovalValidation) object;

        if ( period == null )
        {
            if ( other.period != null )
            {
                return false;
            }
        }
        else if ( !period.equals( other.period ) )
        {
            return false;
        }

        if ( attributeOptionCombo == null )
        {
            if ( other.attributeOptionCombo != null )
            {
                return false;
            }
        }
        else if ( attributeOptionCombo.getId() != other.attributeOptionCombo.getId() )
        {
            return false;
        }

        if ( organisationUnit == null )
        {
            if ( other.organisationUnit != null )
            {
                return false;
            }
        }
        else if ( !organisationUnit.equals( other.organisationUnit ) )
        {
            return false;
        }

        if ( approvalValidationRule == null )
        {
            if ( other.approvalValidationRule != null )
            {
                return false;
            }
        }
        else if ( !approvalValidationRule.equals( other.approvalValidationRule ) )
        {
            return false;
        }
        
        if ( dataSet == null )
        {
            if ( other.dataSet != null )
            {
                return false;
            }
        }
        else if ( !dataSet.equals( other.dataSet ) )
        {
            return false;
        }

        

        return true;
    }

    @Override
    public String toString()
    {
        return "[Org unit: " + organisationUnit.getUid() +
            ", period: " + period.getUid() +
            ", dataSet: " + dataSet.getUid() +
            ", Approval validation rule: " + approvalValidationRule.getUid() +
            "(" + approvalValidationRule.getDisplayName() + ")" + "]";
    }

    /**
     * Compare ApprovalValidation so they will be listed in the desired
     * order: by validationRule, period, attributeOptionCombo and orgUnit.
     *
     * @param other The other ApprovalValidation to compare with.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    /*@Override
    public int compareTo( ApprovalValidation other )
    {
        return new CompareToBuilder()
            .append( this.approvalValidationRule, other.getApprovalValidationRule() )
            .append( this.period, other.getPeriod() )
            .append( this.attributeOptionCombo, other.getAttributeOptionCombo() )
            .append( this.organisationUnit, other.getOrganisationUnit() )
            .append( this.id, other.getId() )
            .toComparison();
    }*/

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------     

    /*@Override
    @Column(
    	    columnDefinition = "NUMERIC(19,0)"
    	)
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }*/

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ApprovalValidationRule getApprovalValidationRule() {
		return approvalValidationRule;
	}

	public void setApprovalValidationRule(ApprovalValidationRule approvalValidationRule) {
		this.approvalValidationRule = approvalValidationRule;
	}
	
	@JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	@JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getNotificationSent()
    {
        return notificationSent;
    }

    public void setNotificationSent( Boolean notificationSent )
    {
        this.notificationSent = notificationSent;
    }

    @JsonProperty
    @JacksonXmlProperty
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
    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }
    
}
