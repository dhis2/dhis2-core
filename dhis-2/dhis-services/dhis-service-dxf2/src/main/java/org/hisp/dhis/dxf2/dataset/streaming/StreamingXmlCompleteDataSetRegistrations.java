package org.hisp.dhis.dxf2.dataset.streaming;

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
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrations;

/**
 * @author Halvdan Hoem Grelland
 */
public class StreamingXmlCompleteDataSetRegistrations
    extends CompleteDataSetRegistrations
{
    private static final String XMLNS = "xmlns";

    private static final String NS = "http://dhis2.org/schema/dxf/2.0";

    private static final String TRUE = "true";

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
        open();
    }

    public StreamingXmlCompleteDataSetRegistrations( XMLReader reader )
    {
        this.reader = reader;
        open();
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

        // TODO Close reader?
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private boolean isWriteMode()
    {
        return writer != null;
    }
}
