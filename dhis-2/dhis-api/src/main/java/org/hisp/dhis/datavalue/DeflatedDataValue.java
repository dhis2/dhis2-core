/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.category.CategoryOptionCombo.DEFAULT_TOSTRING;

import java.util.Date;

import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The purpose of this class is to avoid the overhead of creating objects for
 * associated objects, in order to reduce heap space usage during export.
 *
 * @author Lars Helge Overland
 * @version $Id$
 */
@JacksonXmlRootElement
public class DeflatedDataValue
{
    private long dataElementId;

    private long periodId;

    private long sourceId;

    private long categoryOptionComboId;

    private long attributeOptionComboId;

    private String value;

    private String storedBy;

    private Date created;

    private Date lastUpdated;

    private String comment;

    private boolean followup;

    private boolean deleted;

    // -------------------------------------------------------------------------
    // Optional attributes
    // -------------------------------------------------------------------------

    private int min;

    private int max;

    private String dataElementName;

    private Period period = new Period();

    private String sourceName;

    private String sourcePath;

    private String categoryOptionComboName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DeflatedDataValue()
    {
    }

    public DeflatedDataValue( DataValue dataValue )
    {
        this.dataElementId = dataValue.getDataElement().getId();
        this.periodId = dataValue.getPeriod().getId();
        this.sourceId = dataValue.getSource().getId();
        this.sourcePath = dataValue.getSource().getPath();
        this.categoryOptionComboId = dataValue.getCategoryOptionCombo().getId();
        this.attributeOptionComboId = dataValue.getAttributeOptionCombo().getId();
        this.value = dataValue.getValue();
        this.storedBy = dataValue.getStoredBy();
        this.created = dataValue.getCreated();
        this.lastUpdated = dataValue.getLastUpdated();
        this.comment = dataValue.getComment();
        this.followup = dataValue.isFollowup();
        this.deleted = dataValue.isDeleted();
    }

    public DeflatedDataValue( Long dataElementId, Long periodId, Long sourceId,
        Long categoryOptionComboId, Long attributeOptionComboId, String value,
        String storedBy, Date created, Date lastUpdated,
        String comment, boolean followup, boolean deleted )
    {
        this.dataElementId = dataElementId;
        this.periodId = periodId;
        this.sourceId = sourceId;
        this.categoryOptionComboId = categoryOptionComboId;
        this.attributeOptionComboId = attributeOptionComboId;
        this.value = value;
        this.storedBy = storedBy;
        this.created = created;
        this.lastUpdated = lastUpdated;
        this.comment = comment;
        this.followup = followup;
        this.deleted = deleted;
    }

    public DeflatedDataValue( Long dataElementId, Long categoryOptionComboId )
    {
        this.dataElementId = dataElementId;
        this.categoryOptionComboId = categoryOptionComboId;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    public long getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( long dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    @JsonProperty
    public long getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId( long periodId )
    {
        this.periodId = periodId;
    }

    @JsonProperty
    public long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId( long sourceId )
    {
        this.sourceId = sourceId;
    }

    @JsonProperty
    public long getCategoryOptionComboId()
    {
        return categoryOptionComboId;
    }

    public void setCategoryOptionComboId( long categoryOptionComboId )
    {
        this.categoryOptionComboId = categoryOptionComboId;
    }

    @JsonProperty
    public long getAttributeOptionComboId()
    {
        return attributeOptionComboId;
    }

    public void setAttributeOptionComboId( long attributeOptionComboId )
    {
        this.attributeOptionComboId = attributeOptionComboId;
    }

    @JsonProperty
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    @JsonProperty
    public boolean isFollowup()
    {
        return followup;
    }

    public void setFollowup( boolean followup )
    {
        this.followup = followup;
    }

    @JsonProperty
    public int getMin()
    {
        return min;
    }

    public void setMin( int min )
    {
        this.min = min;
    }

    @JsonProperty
    public int getMax()
    {
        return max;
    }

    public void setMax( int max )
    {
        this.max = max;
    }

    @JsonProperty
    public String getDataElementName()
    {
        return dataElementName;
    }

    public void setDataElementName( String dataElementName )
    {
        this.dataElementName = dataElementName;
    }

    @JsonProperty
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    @JsonProperty
    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName( String sourceName )
    {
        this.sourceName = sourceName;
    }

    @JsonProperty
    public String getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath( String sourcePath )
    {
        this.sourcePath = sourcePath;
    }

    @JsonProperty
    public String getCategoryOptionComboName()
    {
        return categoryOptionComboName;
    }

    public void setCategoryOptionComboName( String categoryOptionComboName )
    {
        this.categoryOptionComboName = categoryOptionComboName;
    }

    @JsonProperty
    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted( boolean deleted )
    {
        this.deleted = deleted;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void setPeriod( String periodTypeName, Date startDate, Date endDate )
    {
        period.setPeriodType( PeriodType.getPeriodTypeByName( periodTypeName ) );
        period.setStartDate( startDate );
        period.setEndDate( endDate );
    }

    public String getCategoryOptionComboNameParsed()
    {
        return categoryOptionComboName != null && categoryOptionComboName.equals( DEFAULT_TOSTRING ) ? ""
            : categoryOptionComboName;
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        result = prime * result + Long.hashCode( dataElementId );
        result = prime * result + Long.hashCode( periodId );
        result = prime * result + Long.hashCode( sourceId );
        result = prime * result + Long.hashCode( categoryOptionComboId );

        return result;
    }

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

        final DeflatedDataValue other = (DeflatedDataValue) object;

        return dataElementId == other.dataElementId && periodId == other.periodId &&
            sourceId == other.sourceId && categoryOptionComboId == other.categoryOptionComboId;
    }
}
