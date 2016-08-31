package org.hisp.dhis.dxf2.datavalue;

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

import org.amplecode.staxwax.reader.XMLReader;
import org.amplecode.staxwax.writer.XMLWriter;

import static org.hisp.dhis.commons.util.TextUtils.valueOf;

/**
 * @author Lars Helge Overland
 */
public class StreamingDataValue
    extends DataValue
{
    private static final String FIELD_DATAVALUE = "dataValue";
    private static final String FIELD_DATAELEMENT = "dataElement";
    private static final String FIELD_CATEGORY_OPTION_COMBO = "categoryOptionCombo";
    private static final String FIELD_ATTRIBUTE_OPTION_COMBO = "attributeOptionCombo";
    private static final String FIELD_PERIOD = "period";
    private static final String FIELD_ORGUNIT = "orgUnit";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_STOREDBY = "storedBy";
    private static final String FIELD_LAST_UPDATED = "lastUpdated";
    private static final String FIELD_COMMENT = "comment";
    private static final String FIELD_FOLLOWUP = "followUp";

    private XMLWriter writer;

    private XMLReader reader;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public StreamingDataValue( XMLWriter writer )
    {
        this.writer = writer;

        this.writer.openElement( FIELD_DATAVALUE );
    }

    public StreamingDataValue( XMLReader reader )
    {
        this.reader = reader;
    }

    //--------------------------------------------------------------------------
    // Getters
    //--------------------------------------------------------------------------

    @Override
    public String getDataElement()
    {
        return dataElement = dataElement == null ? reader.getAttributeValue( FIELD_DATAELEMENT ) : dataElement;
    }

    @Override
    public String getPeriod()
    {
        return period = period == null ? reader.getAttributeValue( FIELD_PERIOD ) : period;
    }

    @Override
    public String getOrgUnit()
    {
        return orgUnit = orgUnit == null ? reader.getAttributeValue( FIELD_ORGUNIT ) : orgUnit;
    }

    @Override
    public String getCategoryOptionCombo()
    {
        return categoryOptionCombo = categoryOptionCombo == null ? reader.getAttributeValue( FIELD_CATEGORY_OPTION_COMBO ) : categoryOptionCombo;
    }
    
    @Override
    public String getAttributeOptionCombo()
    {
        return attributeOptionCombo = attributeOptionCombo == null ? reader.getAttributeValue( FIELD_ATTRIBUTE_OPTION_COMBO ) : attributeOptionCombo;
    }
    
    @Override
    public String getValue()
    {
        return value = value == null ? reader.getAttributeValue( FIELD_VALUE ) : value;
    }

    @Override
    public String getStoredBy()
    {
        return storedBy = storedBy == null ? reader.getAttributeValue( FIELD_STOREDBY ) : storedBy;
    }

    @Override
    public String getLastUpdated()
    {
        return lastUpdated = lastUpdated == null ? reader.getAttributeValue( FIELD_LAST_UPDATED ) : lastUpdated;
    }

    @Override
    public String getComment()
    {
        return comment = comment == null ? reader.getAttributeValue( FIELD_COMMENT ) : comment;
    }

    @Override
    public Boolean getFollowup()
    {
        return followup = followup == null ? valueOf( reader.getAttributeValue( FIELD_FOLLOWUP ) ) : followup;
    }

    //--------------------------------------------------------------------------
    // Setters
    //--------------------------------------------------------------------------

    @Override
    public void setDataElement( String dataElement )
    {
        writer.writeAttribute( FIELD_DATAELEMENT, dataElement );
    }

    @Override
    public void setPeriod( String period )
    {
        writer.writeAttribute( FIELD_PERIOD, period );
    }

    @Override
    public void setOrgUnit( String orgUnit )
    {
        writer.writeAttribute( FIELD_ORGUNIT, orgUnit );
    }

    @Override
    public void setCategoryOptionCombo( String categoryOptionCombo )
    {
        writer.writeAttribute( FIELD_CATEGORY_OPTION_COMBO, categoryOptionCombo );
    }

    @Override
    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        writer.writeAttribute( FIELD_ATTRIBUTE_OPTION_COMBO, attributeOptionCombo );
    }
    
    @Override
    public void setValue( String value )
    {
        writer.writeAttribute( FIELD_VALUE, value );
    }

    @Override
    public void setStoredBy( String storedBy )
    {
        writer.writeAttribute( FIELD_STOREDBY, storedBy );
    }

    @Override
    public void setLastUpdated( String lastUpdated )
    {
        writer.writeAttribute( FIELD_LAST_UPDATED, lastUpdated );
    }

    @Override
    public void setComment( String comment )
    {
        writer.writeAttribute( FIELD_COMMENT, comment );
    }

    @Override
    public void setFollowup( Boolean followup )
    {
        writer.writeAttribute( FIELD_FOLLOWUP, valueOf( followup ) );
    }

    @Override
    public void close()
    {
        writer.closeElement();
    }
}
