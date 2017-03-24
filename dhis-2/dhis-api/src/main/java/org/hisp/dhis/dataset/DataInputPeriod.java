package org.hisp.dhis.dataset;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodSerializer;
import org.hisp.dhis.period.Period;

import java.util.Date;

/**
 * @author Stian Sandvold
 */
@JacksonXmlRootElement( localName = "dataInputPeriods", namespace = DxfNamespaces.DXF_2_0 )
public class DataInputPeriod implements EmbeddedObject
{
    /**
     * The database internal identifier for this Object.
     */
    private int id;

    /**
     * Period data must belong to
     */
    private Period period;

    /**
     * Opening date of which data can be entered
     */
    private Date openingDate;

    /**
     * Closing date of which data can no longer be entered
     */
    private Date closingDate;

    public DataInputPeriod()
    {
    }

    /**
     * Returns true if the period equals the DataInputPeriod's period
     *
     * @param period to check against
     * @return true if the two periods are equal
     */
    public boolean isPeriodEqual( Period period )
    {
        return this.period.equals( period );
    }

    /**
     * Returns true if the given date is after the openingDate and before the closing date
     * If opening date is null, all dates before closing date is valid.
     * If closing date is null, all dates after opening date is valid.
     * If both opening and closing dates are null, all dates are valid
     *
     * @param date to check
     * @return true if date is between openingDate and closingDate
     */
    public boolean isDateWithinOpenCloseDates( Date date )
    {
        return (openingDate == null || date.after( openingDate ))
            && (closingDate == null || date.before( closingDate ));
    }

    /**
     * Checks whether a combination of Period and Date is valid for this DataInputPeriod.
     * Returns true if period is equal to this period, and date is between opening and closing dates if set.
     *
     * @param period
     * @param date
     * @return true if both period and date conforms to this DataInputPeriod.
     */
    public boolean isPeriodAndDateValid( Period period, Date date )
    {
        return isDateWithinOpenCloseDates( date ) && isPeriodEqual( period );
    }

    @JsonIgnore
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JsonSerialize( using = JacksonPeriodSerializer.class )
    @JsonDeserialize( using = JacksonPeriodDeserializer.class )
    @JacksonXmlProperty( localName = "period", namespace = DxfNamespaces.DXF_2_0 )
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "openingDate", namespace = DxfNamespaces.DXF_2_0 )
    public Date getOpeningDate()
    {
        return openingDate;
    }

    public void setOpeningDate( Date openingDate )
    {
        this.openingDate = openingDate;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "closingDate", namespace = DxfNamespaces.DXF_2_0 )
    public Date getClosingDate()
    {
        return closingDate;
    }

    public void setClosingDate( Date closingDate )
    {
        this.closingDate = closingDate;
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

        DataInputPeriod that = (DataInputPeriod) object;

        return new EqualsBuilder()
            .appendSuper( super.equals( object ) )
            .append( period, that.period )
            .append( openingDate, that.openingDate )
            .append( closingDate, that.closingDate )
            .isEquals();
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "period", period )
            .add( "openingDate", openingDate )
            .add( "closingDate", closingDate )
            .toString();
    }
}
