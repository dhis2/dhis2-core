package org.hisp.dhis.validation;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Date;

/**
 * Class representing a validation violation. The validationRule, period and org unit
 * properties make up a composite unique key.
 *
 * @author Margrethe Store
 */
@JacksonXmlRootElement( localName = "validationResult", namespace = DxfNamespaces.DXF_2_0 )
public class ValidationResult implements Comparable<ValidationResult>
{

    private int id;

    private Date created;

    private ValidationRule validationRule;

    private Period period;

    private OrganisationUnit organisationUnit;

    private DataElementCategoryOptionCombo attributeOptionCombo;

    /**
     * The leftsideValue at the time of the violation
     */
    private Double leftsideValue;

    /**
     * The rightsideValue at the time of the violation
     */
    private Double rightsideValue;

    /**
     * This property is a reference to which data was used to generate the result.
     * For rules comparing fixed periods, this dayInPeriod only indicates when in a period the validation was done
     * For rules comparing sliding windows, this will indicate where the end-position of the sliding window was
     * during the validation (IE: the window will span over the days:
     * (period.startDate + dayInPeriod - period.daysInPeriod) to (period.startDate + dayInPeriod)
     */
    private int dayInPeriod;

    /**
     * Indicated whether this ValidationResult has generated a notification for users or not.
     */
    private Boolean notificationSent = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------     

    public ValidationResult()
    {
    }

    public ValidationResult( ValidationRule validationRule, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo,
        Double leftsideValue, Double rightsideValue, int dayInPeriod )
    {
        this.validationRule = validationRule;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.attributeOptionCombo = attributeOptionCombo;
        this.leftsideValue = leftsideValue;
        this.rightsideValue = rightsideValue;
        this.dayInPeriod = dayInPeriod;
    }

    // -------------------------------------------------------------------------
    // Equals, compareTo, hashCode and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((validationRule == null) ? 0 : validationRule.hashCode());
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
        else if ( Math.round( 100.0 * rightsideValue ) != Math.round( 100.0 * other.rightsideValue ) )
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
            ", validation rule: " + validationRule.getUid() +
            "(" + validationRule.getDisplayName() + ")" +
            ", left side value: " + leftsideValue +
            ", right side value: " + rightsideValue + "]";
    }

    /**
     * Comparing validation results is done by priority, then time
     *
     * @param identifiableObject
     * @return
     */
    public int compareTo( ValidationResult other )
    {
        return new CompareToBuilder()
            .append( this.validationRule, other.getValidationRule() )
            .append( this.period, other.getPeriod() )
            .append( this.attributeOptionCombo, other.getAttributeOptionCombo() )
            .append( this.organisationUnit, other.getOrganisationUnit() )
            .append( this.id, other.getId() )
            .toComparison();
    }

    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------     

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

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

    public int getDayInPeriod()
    {
        return dayInPeriod;
    }

    public void setDayInPeriod( int dayInPeriod )
    {
        this.dayInPeriod = dayInPeriod;
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
}
