package org.hisp.dhis.validation;

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
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.io.Serializable;

/**
 * @author Margrethe Store
 */
@JacksonXmlRootElement( localName = "validationResult", namespace = DxfNamespaces.DXF_2_0 )
public class ValidationResult
    implements Serializable, Comparable<ValidationResult>
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -4118317796752962296L;

    private OrganisationUnit orgUnit;

    private Period period;

    private DataElementCategoryOptionCombo attributeOptionCombo;

    private ValidationRule validationRule;

    private Double leftsideValue;

    private Double rightsideValue;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------     

    public ValidationResult()
    {
    }

    public ValidationResult( Period period, OrganisationUnit orgUnit,
        DataElementCategoryOptionCombo attributeOptionCombo, ValidationRule validationRule,
        Double leftsideValue, Double rightsideValue )
    {
        this.orgUnit = orgUnit;
        this.period = period;
        this.attributeOptionCombo = attributeOptionCombo;
        this.validationRule = validationRule;
        this.leftsideValue = leftsideValue;
        this.rightsideValue = rightsideValue;
    }

    // -------------------------------------------------------------------------
    // Equals, compareTo, hashCode and toString
    // -------------------------------------------------------------------------     

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((orgUnit == null) ? 0 : orgUnit.hashCode());
        result = prime * result + ((validationRule == null) ? 0 : validationRule.hashCode());

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

        final ValidationResult other = (ValidationResult) object;

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

        if ( orgUnit == null )
        {
            if ( other.orgUnit != null )
            {
                return false;
            }
        }
        else if ( !orgUnit.equals( other.orgUnit ) )
        {
            return false;
        }

        if ( validationRule == null )
        {
            if ( other.validationRule != null )
            {
                return false;
            }
        }
        else if ( !validationRule.equals( other.validationRule ) )
        {
            return false;
        }

        if ( leftsideValue == null )
        {
            if ( other.leftsideValue != null )
            {
                return false;
            }
        }
        else if ( other.leftsideValue == null )
        {
            return false;
        }
        else if ( Math.round( 100.0 * leftsideValue ) != Math.round( 100.0 * other.leftsideValue ) )
        {
            return false;
        }

        if ( rightsideValue == null )
        {
            if ( other.rightsideValue != null )
            {
                return false;
            }
        }
        else if ( other.rightsideValue == null )
        {
            return false;
        }
        else if ( Math.round( 100.0 * leftsideValue ) != Math.round( 100.0 * other.leftsideValue ) )
        {
            return false;
        }

        return true;
    }

    /**
     * Note: this method is called from threads in which it may not be possible
     * to initialize lazy Hibernate properties. So object properties to compare
     * must be chosen accordingly.
     */
    @Override
    public int compareTo( ValidationResult other )
    {
        int result = orgUnit.getName().compareTo( other.orgUnit.getName() );

        if ( result != 0 )
        {
            return result;
        }

        result = period.getStartDate().compareTo( other.period.getStartDate() );

        if ( result != 0 )
        {
            return result;
        }

        result = period.getEndDate().compareTo( other.period.getEndDate() );

        if ( result != 0 )
        {
            return result;
        }

        result = attributeOptionCombo.getId() - other.attributeOptionCombo.getId();

        if ( result != 0 )
        {
            return result;
        }

        result = validationImportanceOrder( validationRule.getImportance() ) - validationImportanceOrder( other.validationRule.getImportance() );

        if ( result != 0 )
        {
            return result;
        }

        result = validationRule.getLeftSide().getDescription().compareTo( other.validationRule.getLeftSide().getDescription() );

        if ( result != 0 )
        {
            return result;
        }

        result = validationRule.getOperator().compareTo( other.validationRule.getOperator() );

        if ( result != 0 )
        {
            return result;
        }

        result = validationRule.getRightSide().getDescription().compareTo( other.validationRule.getRightSide().getDescription() );

        if ( result != 0 )
        {
            return result;
        }

        result = (int) Math.signum( Math.round( 100.0 * leftsideValue ) - Math.round( 100.0 * other.leftsideValue ) );

        if ( result != 0 )
        {
            return result;
        }

        result = (int) Math.signum( Math.round( 100.0 * rightsideValue ) - Math.round( 100.0 * other.rightsideValue ) );

        if ( result != 0 )
        {
            return result;
        }

        return 0;
    }

    private int validationImportanceOrder( Importance importance )
    {
        return importance == Importance.HIGH ? 0 : importance == Importance.MEDIUM ? 1 : 2;
    }

    @Override
    public String toString()
    {
        return "[Org unit: " + orgUnit.getUid() +
            ", period: " + period.getUid() +
            ", validation rule: " + validationRule.getUid() +
            "(" + validationRule.getDisplayName() + ")"+
            ", left side value: " + leftsideValue +
            ", right side value: " + rightsideValue + "]";
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------     

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( OrganisationUnit orgUnit )
    {
        this.orgUnit = orgUnit;
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
    public DataElementCategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValidationRule getValidationRule()
    {
        return validationRule;
    }

    public void setValidationRule( ValidationRule validationRule )
    {
        this.validationRule = validationRule;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getLeftsideValue()
    {
        return leftsideValue;
    }

    public void setLeftsideValue( Double leftsideValue )
    {
        this.leftsideValue = leftsideValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getRightsideValue()
    {
        return rightsideValue;
    }

    public void setRightsideValue( Double rightsideValue )
    {
        this.rightsideValue = rightsideValue;
    }
}
