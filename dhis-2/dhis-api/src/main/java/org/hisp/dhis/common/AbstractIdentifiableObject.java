/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;

public abstract class AbstractIdentifiableObject implements IdentifiableObject
{
    /**
     * Cache of attribute values which allows for lookup by attribute
     * identifier.
     */
    protected Map<String, AttributeValue> cacheAttributeValues = new HashMap<>();

    /**
     * Cache for object translations, where the cache key is a combination of
     * locale and translation property, and value is the translated value.
     */
    private Map<String, String> translationCache = new ConcurrentHashMap<>();

    /**
     * Access information for this object. Applies to current user.
     */
    protected transient Access access;

    // -------------------------------------------------------------------------
    // Comparable implementation
    // -------------------------------------------------------------------------

    /**
     * Compares objects based on display name. A null display name is ordered
     * after a non-null display name.
     */
    @Override
    public int compareTo( IdentifiableObject object )
    {
        if ( this.getDisplayName() == null )
        {
            return object.getDisplayName() == null ? 0 : 1;
        }

        return object.getDisplayName() == null ? -1
            : this.getDisplayName().compareToIgnoreCase( object.getDisplayName() );
    }

    public AttributeValue getAttributeValue( Attribute attribute )
    {
        loadAttributeValuesCacheIfEmpty();
        return cacheAttributeValues.get( attribute.getUid() );
    }

    public AttributeValue getAttributeValue( String attributeUid )
    {
        loadAttributeValuesCacheIfEmpty();
        return cacheAttributeValues.get( attributeUid );
    }

    public String getAttributeValueString( Attribute attribute )
    {
        AttributeValue attributeValue = getAttributeValue( attribute );
        return attributeValue != null ? attributeValue.getValue() : null;
    }

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
    @Override
    public String getPropertyValue( IdScheme idScheme )
    {
        if ( idScheme.isNull() || idScheme.is( IdentifiableProperty.UID ) )
        {
            return getUid();
        }
        else if ( idScheme.is( IdentifiableProperty.CODE ) )
        {
            return getCode();
        }
        else if ( idScheme.is( IdentifiableProperty.NAME ) )
        {
            return getName();
        }
        else if ( idScheme.is( IdentifiableProperty.ID ) )
        {
            return getId() > 0 ? String.valueOf( getId() ) : null;
        }
        else if ( idScheme.is( IdentifiableProperty.ATTRIBUTE ) )
        {
            for ( AttributeValue attributeValue : getAttributeValues() )
            {
                if ( idScheme.getAttribute().equals( attributeValue.getAttribute().getUid() ) )
                {
                    return attributeValue.getValue();
                }
            }
        }

        return null;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + getId() + "\", " +
            "\"uid\":\"" + getUid() + "\", " +
            "\"code\":\"" + getCode() + "\", " +
            "\"name\":\"" + getName() + "\", " +
            "\"created\":\"" + getCreated() + "\", " +
            "\"lastUpdated\":\"" + getLastUpdated() + "\" " +
            "}";
    }

    private void loadAttributeValuesCacheIfEmpty()
    {
        if ( cacheAttributeValues.isEmpty() && getAttributeValues() != null )
        {
            getAttributeValues().forEach( av -> cacheAttributeValues.put( av.getAttribute().getUid(), av ) );
        }
    }

    /**
     * Get Translation value from {@code Set<Translation>} by given locale and
     * translationKey
     *
     * @return Translation value if exists, otherwise return default value.
     */
    private String getTranslationValue( String locale, String translationKey, String defaultValue )
    {
        for ( Translation translation : getTranslations() )
        {
            if ( locale.equals( translation.getLocale() ) && translationKey.equals( translation.getProperty() ) &&
                !StringUtils.isEmpty( translation.getValue() ) )
            {
                return translation.getValue();
            }
        }

        return defaultValue;
    }
}
