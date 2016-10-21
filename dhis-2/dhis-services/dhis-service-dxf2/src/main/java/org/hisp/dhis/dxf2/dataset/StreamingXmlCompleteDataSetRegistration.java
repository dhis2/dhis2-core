package org.hisp.dhis.dxf2.dataset;

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

        this.writer.openElement( "dataSet" );
    }

    public StreamingXmlCompleteDataSetRegistration( XMLReader reader )
    {
        this.reader = reader;
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    @Override
    public void close()
    {
        if ( writer == null )
        {
            return;
        }

        writer.closeElement();
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @Override
    public String getDataSet()
    {
        return super.getDataSet();
    }

    @Override
    public void setDataSet( String dataSet )
    {
        super.setDataSet( dataSet );
    }

    @Override
    public String getPeriod()
    {
        return super.getPeriod();
    }

    @Override
    public void setPeriod( String period )
    {
        super.setPeriod( period );
    }

    @Override
    public String getOrganisationUnit()
    {
        return super.getOrganisationUnit();
    }

    @Override
    public void setOrganisationUnit( String organisationUnit )
    {
        super.setOrganisationUnit( organisationUnit );
    }

    @Override
    public String getAttributeOptionCombo()
    {
        return super.getAttributeOptionCombo();
    }

    @Override
    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        super.setAttributeOptionCombo( attributeOptionCombo );
    }

    @Override
    public String getDate()
    {
        return super.getDate();
    }

    @Override
    public void setDate( String date )
    {
        super.setDate( date );
    }

    @Override
    public String getStoredBy()
    {
        return super.getStoredBy();
    }

    @Override
    public void setStoredBy( String storedBy )
    {
        super.setStoredBy( storedBy );
    }
}
