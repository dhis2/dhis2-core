package org.hisp.dhis.datavalue;

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

import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import java.util.Date;

import static org.hisp.dhis.dataelement.DataElementCategoryOptionCombo.DEFAULT_TOSTRING;

/**
 * The purpose of this class is to avoid the overhead of creating objects
 * for associated objects, in order to reduce heap space usage during export.
 * 
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DeflatedDataValue
{
    private int dataElementId;
    
    private int periodId;
    
    private int sourceId;

    private int categoryOptionComboId;

    private int attributeOptionComboId;

    private String value;
    
    private String storedBy;

    private Date created;

    private Date lastUpdated;

    private String comment;
    
    private boolean followup;

    // -------------------------------------------------------------------------
    // Optional attributes
    // -------------------------------------------------------------------------

    private int min;
    
    private int max;

    private String dataElementName;
    
    private Period period = new Period();
    
    private String sourceName;
    
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
        this.categoryOptionComboId = dataValue.getCategoryOptionCombo().getId();
        this.attributeOptionComboId = dataValue.getAttributeOptionCombo().getId();
        this.value = dataValue.getValue();
        this.storedBy = dataValue.getStoredBy();
        this.created = dataValue.getCreated();
        this.lastUpdated = dataValue.getLastUpdated();
        this.comment = dataValue.getComment();
        this.followup = dataValue.isFollowup();
    }

    public DeflatedDataValue ( Integer dataElementId, Integer periodId, Integer sourceId,
        Integer categoryOptionComboId, Integer attributeOptionComboId, String value,
        String storedBy, Date created, Date lastUpdated,
        String comment, boolean followup )
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
    }

    public DeflatedDataValue( Integer dataElementId, Integer periodId, Integer sourceId,
        Integer categoryOptionComboId, Integer attributeOptionComboId, String value )
    {
        this.dataElementId = dataElementId;
        this.periodId = periodId;
        this.sourceId = sourceId;
        this.categoryOptionComboId = categoryOptionComboId;
        this.attributeOptionComboId = attributeOptionComboId;
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public int getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( int dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    public int getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId( int periodId )
    {
        this.periodId = periodId;
    }

    public int getSourceId()
    {
        return sourceId;
    }

    public void setSourceId( int sourceId )
    {
        this.sourceId = sourceId;
    }

    public int getCategoryOptionComboId()
    {
        return categoryOptionComboId;
    }

    public void setCategoryOptionComboId( int categoryOptionComboId )
    {
        this.categoryOptionComboId = categoryOptionComboId;
    }

    public int getAttributeOptionComboId()
    {
        return attributeOptionComboId;
    }

    public void setAttributeOptionComboId( int attributeOptionComboId )
    {
        this.attributeOptionComboId = attributeOptionComboId;
    }

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

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public boolean isFollowup()
    {
        return followup;
    }

    public void setFollowup( boolean followup )
    {
        this.followup = followup;
    }

    public int getMin()
    {
        return min;
    }

    public void setMin( int min )
    {
        this.min = min;
    }

    public int getMax()
    {
        return max;
    }

    public void setMax( int max )
    {
        this.max = max;
    }

    public String getDataElementName()
    {
        return dataElementName;
    }

    public void setDataElementName( String dataElementName )
    {
        this.dataElementName = dataElementName;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName( String sourceName )
    {
        this.sourceName = sourceName;
    }

    public String getCategoryOptionComboName()
    {
        return categoryOptionComboName;
    }

    public void setCategoryOptionComboName( String categoryOptionComboName )
    {
        this.categoryOptionComboName = categoryOptionComboName;
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
        return categoryOptionComboName != null && categoryOptionComboName.equals( DEFAULT_TOSTRING ) ? "" : categoryOptionComboName;
    }
    
    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        
        result = prime * result + dataElementId;
        result = prime * result + periodId;
        result = prime * result + sourceId;
        result = prime * result + categoryOptionComboId;
        
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
