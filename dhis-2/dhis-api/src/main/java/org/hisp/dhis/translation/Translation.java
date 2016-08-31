package org.hisp.dhis.translation;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.Objects;

/**
 * @author Oyvind Brucker
 */
@JacksonXmlRootElement( localName = "translation", namespace = DxfNamespaces.DXF_2_0 )
public class Translation
    extends BaseIdentifiableObject
{
    private String objectUid;

    private String className;

    private String locale;

    private String property;

    private String value;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Translation()
    {

    }

    public Translation( String className, String locale, String property, String value, String objectUid )
    {
        this.className = className;
        this.locale = locale;
        this.property = property;
        this.value = value;
        this.objectUid = objectUid;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @JsonIgnore
    public String getClassIdPropKey()
    {
        return className + "-" + objectUid + "-" + property;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( value = "objectId" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getObjectUid()
    {
        return objectUid;
    }

    public void setObjectUid( String objectUid )
    {
        this.objectUid = objectUid;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLocale()
    {
        return locale;
    }

    public void setLocale( String locale )
    {
        this.locale = locale;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getProperty()
    {
        return property;
    }

    public void setProperty( String property )
    {
        this.property = property;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( objectUid, className, locale, property, value );
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

        final Translation other = (Translation) obj;

        return Objects.equals( this.objectUid, other.objectUid )
            && Objects.equals( this.className, other.className )
            && Objects.equals( this.locale, other.locale )
            && Objects.equals( this.property, other.property )
            && Objects.equals( this.value, other.value );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Translation{" );

        sb.append( "objectUid='" ).append( objectUid ).append( '\'' );
        sb.append( ", className='" ).append( className ).append( '\'' );
        sb.append( ", locale='" ).append( locale ).append( '\'' );
        sb.append( ", property='" ).append( property ).append( '\'' );
        sb.append( ", value='" ).append( value ).append( '\'' );
        sb.append( '}' );

        return sb.toString();
    }
}