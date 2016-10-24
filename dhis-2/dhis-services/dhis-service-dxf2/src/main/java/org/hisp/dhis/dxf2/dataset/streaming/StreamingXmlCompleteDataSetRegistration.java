package org.hisp.dhis.dxf2.dataset.streaming;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration;

/**
 * @author Halvdan Hoem Grelland
 */
public class StreamingXmlCompleteDataSetRegistration
    extends CompleteDataSetRegistration
{
    private XMLWriter writer;

    private XMLReader reader;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public StreamingXmlCompleteDataSetRegistration( XMLWriter writer )
    {
        this.writer = writer;
        open();
    }

    public StreamingXmlCompleteDataSetRegistration( XMLReader reader )
    {
        this.reader = reader;
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    @Override
    protected void open()
    {
        writer.openElement( FIELD_DATASET );
    }

    @Override
    public void close()
    {
        if ( writer == null )
        {
            return;
        }

        writer.closeElement();
    }

    @Override
    protected void writeField( String fieldName, String value )
    {
        writer.writeAttribute( fieldName, value );
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @Override
    public String getDataSet()
    {
        return dataSet = dataSet == null ? reader.getAttributeValue( "dataSet" ) : dataSet;
    }

    @Override
    public void setDataSet( String dataSet )
    {
        writeField( "dataSet", dataSet );
    }

    @Override
    public String getPeriod()
    {
        return period = period == null ? reader.getAttributeValue( "period" ) : period;
    }

    @Override
    public void setPeriod( String period )
    {
        writeField( "period", period );
    }

    @Override
    public String getOrganisationUnit()
    {
        return organisationUnit = organisationUnit == null ?
            reader.getAttributeValue( "organisationUnit") : organisationUnit;
    }

    @Override
    public void setOrganisationUnit( String organisationUnit )
    {
        writeField( "organisationUnit", organisationUnit );
    }

    @Override
    public String getAttributeOptionCombo()
    {
        return attributeOptionCombo = attributeOptionCombo == null ?
            reader.getAttributeValue( "attributeOptionCombo" ) : attributeOptionCombo;
    }

    @Override
    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        writeField( "attributeOptionCombo", attributeOptionCombo );
    }

    @Override
    public String getDate()
    {
        return date = date == null ? reader.getAttributeValue( "date" ) : date;
    }

    @Override
    public void setDate( String date )
    {
        writeField( "date", date );
    }

    @Override
    public String getStoredBy()
    {
        return storedBy = storedBy == null ? reader.getAttributeValue( "storedBy" ) : storedBy;
    }

    @Override
    public void setStoredBy( String storedBy )
    {
        writeField( "storedBy", storedBy );
    }
}
