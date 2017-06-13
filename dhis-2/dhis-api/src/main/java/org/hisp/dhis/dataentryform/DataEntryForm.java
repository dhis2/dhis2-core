package org.hisp.dhis.dataentryform;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

import java.util.Objects;

/**
 * @author Bharath Kumar
 */
@JacksonXmlRootElement( localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0 )
public class DataEntryForm
    extends BaseIdentifiableObject implements MetadataObject
{
    public static final int CURRENT_FORMAT = 2;

    /**
     * Name of DataEntryForm. Required and unique.
     */
    private String name;

    /**
     * The display style to use to render the form.
     */
    private DisplayDensity style;

    /**
     * HTML Code of DataEntryForm
     */
    private String htmlCode;

    /**
     * The format of the DataEntryForm.
     */
    private int format;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataEntryForm()
    {

    }

    public DataEntryForm( String name )
    {
        this();
        this.name = name;
    }

    public DataEntryForm( String name, String htmlCode )
    {
        this( name );
        this.htmlCode = htmlCode;
    }

    public DataEntryForm( String name, DisplayDensity style, String htmlCode )
    {
        this( name, htmlCode );
        this.style = style;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Indicates whether this data entry form has custom form HTML code.
     */
    public boolean hasForm()
    {
        return htmlCode != null && !htmlCode.trim().isEmpty();
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( name, style, htmlCode, format );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        final DataEntryForm other = (DataEntryForm) obj;
        return Objects.equals( this.name, other.name )
            && Objects.equals( this.style, other.style )
            && Objects.equals( this.htmlCode, other.htmlCode )
            && Objects.equals( this.format, other.format );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DisplayDensity getStyle()
    {
        return style;
    }

    public void setStyle( DisplayDensity style )
    {
        this.style = style;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getHtmlCode()
    {
        return htmlCode;
    }

    public void setHtmlCode( String htmlCode )
    {
        this.htmlCode = htmlCode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getFormat()
    {
        return format;
    }

    public void setFormat( int format )
    {
        this.format = format;
    }
}
