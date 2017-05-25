package org.hisp.dhis.dxf2.dataset.streaming;

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

import org.hisp.staxwax.reader.XMLReader;
import org.hisp.staxwax.writer.XMLWriter;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrations;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Halvdan Hoem Grelland
 */
public class StreamingXmlCompleteDataSetRegistrations
    extends CompleteDataSetRegistrations
{
    private static final String XMLNS = "xmlns";

    private static final String NS = "http://dhis2.org/schema/dxf/2.0";

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    private XMLWriter writer;

    private XMLReader reader;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------

    public StreamingXmlCompleteDataSetRegistrations( XMLWriter writer )
    {
        this.writer = writer;
    }

    public StreamingXmlCompleteDataSetRegistrations( XMLReader reader )
    {
        this.reader = reader;
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    @Override
    protected void open()
    {
        if ( isWriteMode() )
        {
            writer.openDocument();
            writer.openElement( FIELD_COMPLETE_DATA_SET_REGISTRATIONS );
            writer.writeAttribute( XMLNS, NS );
        }
        else
        {
            reader.moveToStartElement( FIELD_COMPLETE_DATA_SET_REGISTRATIONS );
        }
    }

    @Override
    protected void close()
    {
        if ( isWriteMode() )
        {
            writer.closeElement();
            writer.closeDocument();
        }
        else
        {
            reader.closeReader();
        }
    }

    @Override
    public CompleteDataSetRegistration getCompleteDataSetRegistrationInstance()
    {
        return new StreamingXmlCompleteDataSetRegistration( writer );
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
    public String getIdScheme()
    {
        return get( FIELD_ID_SCHEME, super::getIdScheme, super::setIdScheme );
    }

    @Override
    public void setIdScheme( String idScheme )
    {
        writeField( FIELD_ID_SCHEME, idScheme );
    }

    @Override
    public String getDataSetIdScheme()
    {
        return get( FIELD_DATA_SET_ID_SCHEME, super::getDataSetIdScheme, super::setDataSetIdScheme );
    }

    @Override
    public void setDataSetIdScheme( String dataSetIdScheme )
    {
        writeField( FIELD_DATA_SET_ID_SCHEME, dataSetIdScheme );
    }

    @Override
    public String getOrgUnitIdScheme()
    {
        return get( FIELD_ORG_UNIT_ID_SCHEME, super::getOrgUnitIdScheme, super::setOrgUnitIdScheme );
    }

    @Override
    public void setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        writeField( FIELD_ORG_UNIT_ID_SCHEME, orgUnitIdScheme );
    }

    @Override
    public String getAttributeOptionComboIdScheme()
    {
        return get( FIELD_ATTR_OPT_COMBO_ID_SCHEME, super::getAttributeOptionComboIdScheme, super::setAttributeOptionComboIdScheme );
    }

    @Override
    public void setAttributeOptionComboIdScheme( String attributeOptionComboIdScheme )
    {
        writeField( FIELD_ATTR_OPT_COMBO_ID_SCHEME, attributeOptionComboIdScheme );
    }

    @Override
    public Boolean getDryRun()
    {
        return get( FIELD_DRY_RUN, super::getDryRun, v -> super.setDryRun( Boolean.parseBoolean( v ) ? Boolean.TRUE : null ) );
    }

    @Override
    public void setDryRun( Boolean dryRun )
    {
        writeField( FIELD_DRY_RUN, dryRun == null ? null : dryRun.toString() );
    }

    @Override
    public String getStrategy()
    {
        return get( FIELD_IMPORT_STRATEGY, super::getStrategy, super::setStrategy );
    }

    @Override
    public void setStrategy( String strategy )
    {
        writeField( FIELD_IMPORT_STRATEGY, strategy );
    }

    @Override
    public boolean hasNextCompleteDataSetRegistration()
    {
        return reader.moveToStartElement( FIELD_COMPLETE_DATA_SET_REGISTRATION, FIELD_COMPLETE_DATA_SET_REGISTRATIONS );
    }

    @Override
    public CompleteDataSetRegistration getNextCompleteDataSetRegistration()
    {
        return new StreamingXmlCompleteDataSetRegistration( reader );
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private <T> T get( String fieldName, Supplier<T> getter, Consumer<String> setter )
    {
        T prop = getter.get();

        if ( prop == null )
        {
            setter.accept( reader.getAttributeValue( fieldName ) );
        }

        return prop;
    }

    private boolean isWriteMode()
    {
        return writer != null;
    }
}
