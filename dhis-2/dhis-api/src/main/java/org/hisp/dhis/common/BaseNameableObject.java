package org.hisp.dhis.common;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.TranslationProperty;

/**
 * @author Bob Jolliffe
 */
@JacksonXmlRootElement( localName = "nameableObject", namespace = DxfNamespaces.DXF_2_0 )
public class BaseNameableObject
    extends BaseIdentifiableObject
    implements NameableObject
{
    /**
     * An short name representing this Object. Optional but unique.
     */
    protected String shortName;

    /**
     * Description of this Object.
     */
    protected String description;

    /**
     * The i18n variant of the short name. Should not be persisted.
     */
    protected transient String displayShortName;

    /**
     * The i18n variant of the description. Should not be persisted.
     */
    protected transient String displayDescription;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BaseNameableObject()
    {
    }

    public BaseNameableObject( String uid, String code, String name )
    {
        this.uid = uid;
        this.code = code;
        this.name = name;
    }

    public BaseNameableObject( int id, String uid, String name, String shortName, String code, String description )
    {
        super( id, uid, name );
        this.shortName = shortName;
        this.code = code;
        this.description = description;
    }

    public BaseNameableObject( NameableObject object )
    {
        super( object.getId(), object.getUid(), object.getName() );
        this.shortName = object.getShortName();
        this.code = object.getCode();
        this.description = object.getDescription();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Returns the display property indicated by the given display property. Falls
     * back to display name if display short name is null.
     *
     * @param displayProperty the display property.
     * @return the display property.
     */
    @JsonIgnore
    public String getDisplayProperty( DisplayProperty displayProperty )
    {
        if ( DisplayProperty.SHORTNAME.equals( displayProperty ) && getDisplayShortName() != null )
        {
            return getDisplayShortName();
        }
        else
        {
            return getDisplayName();
        }
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        return result;
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied objects.
     */
    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( o.getClass() ) )
        {
            return false;
        }

        if ( !super.equals( o ) )
        {
            return false;
        }

        final BaseNameableObject other = (BaseNameableObject) o;

        if ( getShortName() != null ? !getShortName().equals( other.getShortName() ) : other.getShortName() != null )
        {
            return false;
        }

        if ( getDescription() != null ? !getDescription().equals( other.getDescription() ) : other.getDescription() != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"hashCode\":\"" + hashCode() + "\", " +
            "\"id\":\"" + getId() + "\", " +
            "\"uid\":\"" + getUid() + "\", " +
            "\"code\":\"" + getCode() + "\", " +
            "\"name\":\"" + getName() + "\", " +
            "\"shortName\":\"" + getShortName() + "\", " +
            "\"description\":\"" + getDescription() + "\", " +
            "\"created\":\"" + getCreated() + "\", " +
            "\"lastUpdated\":\"" + getLastUpdated() + "\" " +
            "}";
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @PropertyRange( min = 1 )
    public String getShortName()
    {
        return shortName;
    }

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDisplayShortName()
    {
        displayShortName = getTranslation( TranslationProperty.SHORT_NAME, displayShortName );
        return displayShortName != null ? displayShortName : getShortName();
    }

    public void setDisplayShortName( String displayShortName )
    {
        this.displayShortName = displayShortName;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 1 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDisplayDescription()
    {
        displayDescription = getTranslation( TranslationProperty.DESCRIPTION, displayDescription );
        return displayDescription != null ? displayDescription : getDescription();
    }

    public void setDisplayDescription( String displayDescription )
    {
        this.displayDescription = displayDescription;
    }
}
