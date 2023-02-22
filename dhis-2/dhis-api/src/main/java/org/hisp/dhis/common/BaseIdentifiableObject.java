/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.util.SharingUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Bob Jolliffe
 */
@JacksonXmlRootElement( localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0 )
public class BaseIdentifiableObject
    extends BaseLinkableObject implements IdentifiableObject
{
    /**
     * The database internal identifier for this Object.
     */
    protected long id;

    /**
     * The unique identifier for this object.
     */
    @AuditAttribute
    protected String uid;

    /**
     * The unique code for this object.
     */
    @AuditAttribute
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    protected String name;

    /**
     * The date this object was created.
     */
    protected Date created;

    /**
     * The date this object was last updated.
     */
    protected Date lastUpdated;

    /**
     * Set of the dynamic attributes values that belong to this data element.
     */
    @AuditAttribute
    protected Set<AttributeValue> attributeValues = new HashSet<>();

    /**
     * Cache of attribute values which allows for lookup by attribute
     * identifier.
     */
    protected Map<String, AttributeValue> cacheAttributeValues = new HashMap<>();

    /**
     * Set of available object translation, normally filtered by locale.
     */
    protected Set<Translation> translations = new HashSet<>();

    /**
     * Cache for object translations, where the cache key is a combination of
     * locale and translation property, and value is the translated value.
     */
    private Map<String, String> translationCache = new ConcurrentHashMap<>();

    /**
     * This object is available as external read-only.
     */
    protected transient Boolean externalAccess;

    /**
     * Access string for public access.
     */
    protected transient String publicAccess;

    /**
     * User who created this object. This field is immutable and must not be
     * updated.
     */
    protected User createdBy;

    /**
     * Access for user groups.
     */
    protected transient Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses = new HashSet<>();

    /**
     * Access for users.
     */
    protected transient Set<org.hisp.dhis.user.UserAccess> userAccesses = new HashSet<>();

    /**
     * Access information for this object. Applies to current user.
     */
    protected transient Access access;

    /**
     * Users who have marked this object as a favorite.
     */
    protected Set<String> favorites = new HashSet<>();

    /**
     * Last user updated this object.
     */
    protected User lastUpdatedBy;

    /**
     * Object sharing (JSONB).
     */
    protected Sharing sharing = new Sharing();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BaseIdentifiableObject()
    {
    }

    public BaseIdentifiableObject( long id, String uid, String name )
    {
        this.id = id;
        this.uid = uid;
        this.name = name;
    }

    public BaseIdentifiableObject( String uid, String code, String name )
    {
        this.uid = uid;
        this.code = code;
        this.name = name;
    }

    public BaseIdentifiableObject( IdentifiableObject identifiableObject )
    {
        this.id = identifiableObject.getId();
        this.uid = identifiableObject.getUid();
        this.name = identifiableObject.getName();
        this.created = identifiableObject.getCreated();
        this.lastUpdated = identifiableObject.getLastUpdated();
    }

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

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @Override
    @JsonIgnore
    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    @Override
    @JsonProperty( value = "id" )
    @JacksonXmlProperty( localName = "id", isAttribute = true )
    @Description( "The Unique Identifier for this Object." )
    @Property( value = PropertyType.IDENTIFIER, required = Value.FALSE )
    @PropertyRange( min = 11, max = 11 )
    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The unique code for this Object." )
    @Property( PropertyType.IDENTIFIER )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The name of this Object. Required and unique." )
    @PropertyRange( min = 1 )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "name", key = "NAME" )
    public String getDisplayName()
    {
        return getTranslation( "NAME", getName() );
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The date this object was created." )
    @Property( value = PropertyType.DATE, required = Value.FALSE )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @Override
    @JsonProperty
    @JsonSerialize( using = UserPropertyTransformer.JacksonSerialize.class )
    @JsonDeserialize( using = UserPropertyTransformer.JacksonDeserialize.class )
    @PropertyTransformer( UserPropertyTransformer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getLastUpdatedBy()
    {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy( User lastUpdatedBy )
    {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The date this object was last updated." )
    @Property( value = PropertyType.DATE, required = Value.FALSE )
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @Override
    @JsonProperty( "attributeValues" )
    @JacksonXmlElementWrapper( localName = "attributeValues", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AttributeValue> getAttributeValues()
    {
        return attributeValues;
    }

    @Override
    public void setAttributeValues( Set<AttributeValue> attributeValues )
    {
        cacheAttributeValues.clear();
        this.attributeValues = attributeValues;
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

    @Gist( included = Include.FALSE )
    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "translations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "translation", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Translation> getTranslations()
    {
        if ( translations == null )
        {
            translations = new HashSet<>();
        }

        return translations;
    }

    /**
     * Clears out cache when setting translations.
     */
    public void setTranslations( Set<Translation> translations )
    {
        this.translationCache.clear();
        this.translations = translations;
    }

    /**
     * Returns a translated value for this object for the given property. The
     * current locale is read from the user context.
     *
     * @param translationKey the translation key.
     * @param defaultValue the value to use if there are no translations.
     * @return a translated value.
     */
    protected String getTranslation( String translationKey, String defaultValue )
    {
        Locale locale = CurrentUserUtil.getUserSetting( UserSettingKey.DB_LOCALE );

        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if ( locale == null || translationKey == null || CollectionUtils.isEmpty( translations ) )
        {
            return defaultValue;
        }

        return translationCache.computeIfAbsent( Translation.getCacheKey( locale.toString(), translationKey ),
            key -> getTranslationValue( locale.toString(), translationKey, defaultTranslation ) );
    }

    private void loadAttributeValuesCacheIfEmpty()
    {
        if ( cacheAttributeValues.isEmpty() && attributeValues != null )
        {
            attributeValues.forEach( av -> cacheAttributeValues.put( av.getAttribute().getUid(), av ) );
        }
    }

    @Override
    @Gist( included = Include.FALSE )
    @JsonProperty
    @JsonSerialize( using = UserPropertyTransformer.JacksonSerialize.class )
    @JsonDeserialize( using = UserPropertyTransformer.JacksonDeserialize.class )
    @PropertyTransformer( UserPropertyTransformer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getCreatedBy()
    {
        return createdBy;
    }

    @Override
    @JsonProperty
    @JsonSerialize( using = UserPropertyTransformer.JacksonSerialize.class )
    @JsonDeserialize( using = UserPropertyTransformer.JacksonDeserialize.class )
    @PropertyTransformer( UserPropertyTransformer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getUser()
    {
        return createdBy;
    }

    @Override
    public void setCreatedBy( User createdBy )
    {
        this.createdBy = createdBy;
    }

    @Override
    public void setUser( User user )
    {
        // TODO remove this after implementing functions for using Owner
        setCreatedBy( createdBy == null ? user : createdBy );
        setOwner( user != null ? user.getUid() : null );
    }

    public void setOwner( String userId )
    {
        getSharing().setOwner( userId );
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 8, max = 8 )
    public String getPublicAccess()
    {
        return SharingUtils.getDtoPublicAccess( publicAccess, getSharing() );
    }

    public void setPublicAccess( String publicAccess )
    {
        this.publicAccess = publicAccess;
        getSharing().setPublicAccess( publicAccess );
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean getExternalAccess()
    {
        return SharingUtils.getDtoExternalAccess( externalAccess, getSharing() );
    }

    public void setExternalAccess( boolean externalAccess )
    {
        this.externalAccess = externalAccess;
        getSharing().setExternal( externalAccess );
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userGroupAccesses", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
    public Set<org.hisp.dhis.user.UserGroupAccess> getUserGroupAccesses()
    {
        return SharingUtils.getDtoUserGroupAccesses( userGroupAccesses, getSharing() );
    }

    public void setUserGroupAccesses( Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses )
    {
        getSharing().setDtoUserGroupAccesses( userGroupAccesses );
        this.userGroupAccesses = userGroupAccesses;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userAccesses", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userAccess", namespace = DxfNamespaces.DXF_2_0 )
    public Set<org.hisp.dhis.user.UserAccess> getUserAccesses()
    {
        return SharingUtils.getDtoUserAccesses( userAccesses, getSharing() );
    }

    public void setUserAccesses( Set<org.hisp.dhis.user.UserAccess> userAccesses )
    {
        getSharing().setDtoUserAccesses( userAccesses );
        this.userAccesses = userAccesses;
    }

    @Override
    @Gist( included = Include.FALSE )
    @JsonProperty
    @JacksonXmlProperty( localName = "access", namespace = DxfNamespaces.DXF_2_0 )
    public Access getAccess()
    {
        return access;
    }

    public void setAccess( Access access )
    {
        this.access = access;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "favorites", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "favorite", namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getFavorites()
    {
        return favorites;
    }

    public void setFavorites( Set<String> favorites )
    {
        this.favorites = favorites;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isFavorite()
    {
        CurrentUserDetails user = CurrentUserUtil.getCurrentUserDetails();
        return user != null && favorites != null && favorites.contains( user.getUid() );
    }

    @Override
    @Gist( included = Include.FALSE )
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Sharing getSharing()
    {
        if ( sharing == null )
        {
            sharing = new Sharing();
        }

        return sharing;
    }

    public void setSharing( Sharing sharing )
    {
        this.sharing = sharing;
    }

    @Override
    public boolean setAsFavorite( User user )
    {
        if ( this.favorites == null )
        {
            this.favorites = new HashSet<>();
        }

        return this.favorites.add( user.getUid() );
    }

    @Override
    public boolean removeAsFavorite( User user )
    {
        if ( this.favorites == null )
        {
            this.favorites = new HashSet<>();
        }

        return this.favorites.remove( user.getUid() );
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int result = getUid() != null ? getUid().hashCode() : 0;
        result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);

        return result;
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied
     * objects.
     */
    @Override
    public boolean equals( Object obj )
    {
        return this == obj || obj instanceof BaseIdentifiableObject
            && getRealClass( this ) == getRealClass( obj )
            && typedEquals( (IdentifiableObject) obj );
    }

    /**
     * Equality check against typed identifiable object. This method is not
     * vulnerable to proxy issues, where an uninitialized object class type
     * fails comparison to a real class.
     *
     * @param other the identifiable object to compare this object against.
     * @return true if equal.
     */
    public final boolean typedEquals( IdentifiableObject other )
    {
        if ( other == null )
        {
            return false;
        }
        return Objects.equals( getUid(), other.getUid() )
            && Objects.equals( getCode(), other.getCode() )
            && Objects.equals( getName(), other.getName() );
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields()
    {
        if ( uid == null || uid.length() == 0 )
        {
            setUid( CodeGenerator.generateUid() );
        }

        Date date = new Date();

        if ( created == null )
        {
            created = date;
        }

        setLastUpdated( date );
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
            return uid;
        }
        else if ( idScheme.is( IdentifiableProperty.CODE ) )
        {
            return code;
        }
        else if ( idScheme.is( IdentifiableProperty.NAME ) )
        {
            return name;
        }
        else if ( idScheme.is( IdentifiableProperty.ID ) )
        {
            return id > 0 ? String.valueOf( id ) : null;
        }
        else if ( idScheme.is( IdentifiableProperty.ATTRIBUTE ) )
        {
            for ( AttributeValue attributeValue : attributeValues )
            {
                if ( idScheme.getAttribute().equals( attributeValue.getAttribute().getUid() ) )
                {
                    return attributeValue.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Set legacy sharing collections to null so that the ImportService will
     * import current object with new Sharing format.
     */
    public void clearLegacySharingCollections()
    {
        this.userAccesses = null;
        this.userGroupAccesses = null;
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

    /**
     * Get Translation value from {@code Set<Translation>} by given locale and
     * translationKey
     *
     * @return Translation value if exists, otherwise return default value.
     */
    private String getTranslationValue( String locale, String translationKey, String defaultValue )
    {
        for ( Translation translation : translations )
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
