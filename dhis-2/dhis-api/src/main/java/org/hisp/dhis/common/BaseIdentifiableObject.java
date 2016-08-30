package org.hisp.dhis.common;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Required;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.translation.ObjectTranslation;
import org.hisp.dhis.translation.TranslationProperty;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserSettingKey;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Bob Jolliffe
 */
@JacksonXmlRootElement( localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0 )
public class BaseIdentifiableObject
    extends BaseLinkableObject
    implements IdentifiableObject
{
    /**
     * The database internal identifier for this Object.
     */
    protected int id;

    /**
     * The Unique Identifier for this Object.
     */
    protected String uid;

    /**
     * The unique code for this Object.
     */
    protected String code;

    /**
     * The name of this Object. Required and unique.
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
    protected Set<AttributeValue> attributeValues = new HashSet<>();

    /**
     * Set of available object translation, normally filtered by locale.
     */
    protected Set<ObjectTranslation> translations = new HashSet<>();

    protected Map<TranslationProperty, ObjectTranslation> translationCache = new HashMap<>();

    /**
     * This object is available as external read-only
     */
    protected boolean externalAccess;

    /**
     * Access string for public access.
     */
    protected String publicAccess;

    /**
     * Owner of this object.
     */
    protected User user;

    /**
     * Access for userGroups
     */
    protected Set<UserGroupAccess> userGroupAccesses = new HashSet<>();

    /**
     * Access information for this object. Applies to current user.
     */
    protected transient Access access;

    /**
     * The i18n variant of the name. Should not be persisted.
     */
    protected transient String displayName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BaseIdentifiableObject()
    {
    }

    public BaseIdentifiableObject( int id, String uid, String name )
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

    @Override
    public int compareTo( IdentifiableObject object )
    {
        return name == null ? (object.getDisplayName() == null ? 0 : -1) : name.compareTo( object.getDisplayName() );
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @Override
    @JsonIgnore
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @Override
    @JsonProperty( value = "id" )
    @JacksonXmlProperty( localName = "id", isAttribute = true )
    @Description( "The Unique Identifier for this Object." )
    @Property( value = PropertyType.IDENTIFIER, required = Required.FALSE )
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
    public String getDisplayName()
    {
        displayName = getTranslation( TranslationProperty.NAME, displayName );
        return displayName != null ? displayName : getName();
    }

    @JsonIgnore
    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The date this object was created." )
    @Property( value = PropertyType.DATE, required = Required.FALSE )
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
    @JacksonXmlProperty( isAttribute = true )
    @Description( "The date this object was last updated." )
    @Property( value = PropertyType.DATE, required = Required.FALSE )
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

    public void setAttributeValues( Set<AttributeValue> attributeValues )
    {
        this.attributeValues = attributeValues;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "translations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "translation", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ObjectTranslation> getTranslations()
    {
        return translations;
    }

    // automatically clear out cache on setting translations
    public void setTranslations( Set<ObjectTranslation> translations )
    {
        this.translationCache.clear();
        this.translations = translations;
    }

    protected String getTranslation( TranslationProperty property, String defaultValue )
    {
        // if either no translations available, or user context does not have locale set, then assume unfiltered list and use default value
        if ( !UserContext.haveUserSetting( UserSettingKey.DB_LOCALE ) || translations.isEmpty() )
        {
            return defaultValue != null ? defaultValue.trim() : null;
        }

        if ( translationCache.containsKey( property ) )
        {
            return translationCache.get( property ).getValue();
        }

        for ( ObjectTranslation translation : translations )
        {
            if ( property == translation.getProperty() && !StringUtils.isEmpty( translation.getValue() ) )
            {
                translationCache.put( property, translation );
                return translation.getValue();
            }
        }

        return defaultValue != null ? defaultValue.trim() : null;
    }

    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 8, max = 8 )
    public String getPublicAccess()
    {
        return publicAccess;
    }

    public void setPublicAccess( String publicAccess )
    {
        this.publicAccess = publicAccess;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean getExternalAccess()
    {
        return externalAccess;
    }

    public void setExternalAccess( Boolean externalAccess )
    {
        this.externalAccess = externalAccess == null ? false : externalAccess;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userGroupAccesses", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userGroupAccess", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserGroupAccess> getUserGroupAccesses()
    {
        return userGroupAccesses;
    }

    public void setUserGroupAccesses( Set<UserGroupAccess> userGroupAccesses )
    {
        this.userGroupAccesses = userGroupAccesses;
    }

    @Override
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

        final BaseIdentifiableObject other = (BaseIdentifiableObject) o;

        if ( getUid() != null ? !getUid().equals( other.getUid() ) : other.getUid() != null )
        {
            return false;
        }

        if ( getCode() != null ? !getCode().equals( other.getCode() ) : other.getCode() != null )
        {
            return false;
        }

        if ( getName() != null ? !getName().equals( other.getName() ) : other.getName() != null )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return true;
    }

    @Override
    public boolean haveUniqueCode()
    {
        return true;
    }

    @Override
    public boolean isAutoGenerated()
    {
        return false;
    }

    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields()
    {
        if ( uid == null || uid.length() == 0 )
        {
            setUid( CodeGenerator.generateCode() );
        }

        Date date = new Date();

        if ( created == null )
        {
            created = date;
        }

        setLastUpdated( date );
    }

    /**
     * Clear out all sharing properties.
     *
     * @param clearUser Clear out user property
     */
    public void clearSharing( boolean clearUser )
    {
        if ( clearUser )
        {
            user = null;
        }

        publicAccess = AccessStringHelper.DEFAULT;
        externalAccess = false;

        if ( userGroupAccesses != null )
        {
            userGroupAccesses.clear();
        }
    }

    /**
     * Get a map of uids to internal identifiers
     *
     * @param objects the IdentifiableObjects to put in the map
     * @return the map
     */
    public static Map<String, Integer> getUIDMap( Collection<? extends BaseIdentifiableObject> objects )
    {
        Map<String, Integer> map = new HashMap<>();

        for ( IdentifiableObject object : objects )
        {
            String uid = object.getUid();
            int internalId = object.getId();

            map.put( uid, internalId );
        }

        return map;
    }

    /**
     * Get a map of codes to internal identifiers
     *
     * @param objects the NameableObjects to put in the map
     * @return the map
     */
    public static Map<String, Integer> getCodeMap( Collection<? extends BaseIdentifiableObject> objects )
    {
        Map<String, Integer> map = new HashMap<>();

        for ( BaseIdentifiableObject object : objects )
        {
            String code = object.getCode();
            int internalId = object.getId();

            map.put( code, internalId );
        }

        return map;
    }

    /**
     * Get a map of names to internal identifiers
     *
     * @param objects the NameableObjects to put in the map
     * @return the map
     */
    public static Map<String, Integer> getNameMap( Collection<? extends BaseIdentifiableObject> objects )
    {
        Map<String, Integer> map = new HashMap<>();

        for ( BaseIdentifiableObject object : objects )
        {
            String name = object.getName();
            int internalId = object.getId();

            map.put( name, internalId );
        }

        return map;
    }

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
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

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        Validate.notNull( other );

        if ( mergeMode.isReplace() )
        {
            uid = other.getUid();
            name = other.getName();
            code = other.getCode();
            lastUpdated = other.getLastUpdated();
            created = other.getCreated();
            user = other.getUser();
        }
        else if ( mergeMode.isMerge() )
        {
            uid = other.getUid() == null ? uid : other.getUid();
            name = other.getName() == null ? name : other.getName();
            code = other.getCode() == null ? code : other.getCode();
            lastUpdated = other.getLastUpdated() == null ? lastUpdated : other.getLastUpdated();
            created = other.getCreated() == null ? created : other.getCreated();
            user = other.getUser() == null ? user : other.getUser();
        }

        attributeValues.clear();
        attributeValues.addAll( other.getAttributeValues() );

        translations.clear();
        translations.addAll( other.getTranslations() );

        translationCache.clear();
    }

    @Override
    public void mergeSharingWith( IdentifiableObject other )
    {
        Validate.notNull( other );

        // sharing
        user = other.getUser() == null ? user : other.getUser();
        publicAccess = other.getPublicAccess() == null ? publicAccess : other.getPublicAccess();
        externalAccess = other.getExternalAccess();

        if ( userGroupAccesses != null )
        {
            userGroupAccesses.clear();
            userGroupAccesses.addAll( other.getUserGroupAccesses() );
        }
    }
}
