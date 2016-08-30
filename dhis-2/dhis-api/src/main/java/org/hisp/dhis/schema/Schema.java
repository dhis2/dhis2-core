package org.hisp.dhis.schema;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "schema", namespace = DxfNamespaces.DXF_2_0 )
public class Schema implements Ordered, Klass
{
    /**
     * Class that is described in this schema.
     */
    private Class<?> klass;

    /**
     * Is this class a sub-class of IdentifiableObject
     *
     * @see org.hisp.dhis.common.IdentifiableObject
     */
    private boolean identifiableObject;

    /**
     * Is this class a sub-class of NameableObject
     *
     * @see org.hisp.dhis.common.NameableObject
     */
    private boolean nameableObject;

    /**
     * Singular name.
     */
    private String singular;

    /**
     * Plural name.
     */
    private String plural;

    /**
     * Namespace URI to be used for this class.
     */
    private String namespace;

    /**
     * This will normally be set to equal singular, but in certain cases it might be useful to have another name
     * for when this class is used as an item inside a collection.
     */
    private String name;

    /**
     * A beautified (and possibly translated) name that can be used in UI.
     */
    private String displayName;

    /**
     * This will normally be set to equal plural, and is normally used as a wrapper for a collection of
     * instances of this klass type.
     */
    private String collectionName;

    /**
     * Is sharing supported for instances of this class.
     */
    private boolean shareable;

    /**
     * Points to relative Web-API endpoint (if exposed).
     */
    private String relativeApiEndpoint;

    /**
     * Used by LinkService to link to the API endpoint containing this type.
     */
    private String apiEndpoint;

    /**
     * Used by LinkService to link to the Schema describing this type (if reference).
     */
    private String href;

    /**
     * Is this class considered metadata, this is mainly used for our metadata importer/exporter.
     */
    private boolean metadata = true;

    /**
     * Are any properties on this class being persisted, if false, this file does not have any hbm file attached to it.
     */
    private boolean persisted;

    /**
     * Should new instances always be default private, even if the user can create public instances.
     */
    private boolean defaultPrivate;

    /**
     * If this is true, do not require private authority for create/update of instances of this type.
     */
    private boolean implicitPrivateAuthority;

    /**
     * List of authorities required for doing operations on this class.
     */
    private List<Authority> authorities = Lists.newArrayList();

    /**
     * Map of all exposed properties on this class, where key is property
     * name, and value is instance of Property class.
     *
     * @see org.hisp.dhis.schema.Property
     */
    private Map<String, Property> propertyMap = Maps.newHashMap();

    /**
     * Map of all persisted properties, cached on first request.
     */
    private Map<String, Property> persistedProperties;

    /**
     * Map of all persisted properties, cached on first request.
     */
    private Map<String, Property> nonPersistedProperties;

    /**
     * Used for sorting of schema list when doing metadata import/export.
     */
    private int order = Ordered.LOWEST_PRECEDENCE;

    public Schema( Class<?> klass, String singular, String plural )
    {
        this.klass = klass;
        this.identifiableObject = IdentifiableObject.class.isAssignableFrom( klass );
        this.nameableObject = NameableObject.class.isAssignableFrom( klass );
        this.singular = singular;
        this.plural = plural;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Class<?> getKlass()
    {
        return klass;
    }

    public void setKlass( Class<?> klass )
    {
        this.klass = klass;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIdentifiableObject()
    {
        return identifiableObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isNameableObject()
    {
        return nameableObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSingular()
    {
        return singular;
    }

    public void setSingular( String singular )
    {
        this.singular = singular;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getPlural()
    {
        return plural;
    }

    public void setPlural( String plural )
    {
        this.plural = plural;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCollectionName()
    {
        return collectionName == null ? plural : collectionName;
    }

    public void setCollectionName( String collectionName )
    {
        this.collectionName = collectionName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getName()
    {
        return name == null ? singular : name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDisplayName()
    {
        return displayName != null ? displayName : getName();
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isShareable()
    {
        return shareable;
    }

    public void setShareable( boolean shareable )
    {
        this.shareable = shareable;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getRelativeApiEndpoint()
    {
        return relativeApiEndpoint;
    }

    public void setRelativeApiEndpoint( String relativeApiEndpoint )
    {
        this.relativeApiEndpoint = relativeApiEndpoint;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getApiEndpoint()
    {
        return apiEndpoint;
    }

    public void setApiEndpoint( String apiEndpoint )
    {
        this.apiEndpoint = apiEndpoint;
    }

    public boolean haveApiEndpoint()
    {
        return getRelativeApiEndpoint() != null || getApiEndpoint() != null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getHref()
    {
        return href;
    }

    public void setHref( String href )
    {
        this.href = href;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMetadata()
    {
        return metadata;
    }

    public void setMetadata( boolean metadata )
    {
        this.metadata = metadata;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted( boolean persisted )
    {
        this.persisted = persisted;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDefaultPrivate()
    {
        return defaultPrivate;
    }

    public void setDefaultPrivate( boolean defaultPrivate )
    {
        this.defaultPrivate = defaultPrivate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isImplicitPrivateAuthority()
    {
        return implicitPrivateAuthority;
    }

    public void setImplicitPrivateAuthority( boolean implicitPrivateAuthority )
    {
        this.implicitPrivateAuthority = implicitPrivateAuthority;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "authorities", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "authority", namespace = DxfNamespaces.DXF_2_0 )
    public List<Authority> getAuthorities()
    {
        return authorities;
    }

    public void setAuthorities( List<Authority> authorities )
    {
        this.authorities = authorities;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "properties", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "property", namespace = DxfNamespaces.DXF_2_0 )
    public List<Property> getProperties()
    {
        return Lists.newArrayList( propertyMap.values() );
    }

    public boolean haveProperty( String propertyName )
    {
        return getPropertyMap().containsKey( propertyName );
    }

    public boolean havePersistedProperty( String propertyName )
    {
        return haveProperty( propertyName ) && getProperty( propertyName ).isPersisted();
    }

    public Property propertyByRole( String role )
    {
        if ( !StringUtils.isEmpty( role ) )
        {
            for ( Property property : propertyMap.values() )
            {
                if ( property.isCollection() && property.isManyToMany() && (role.equals( property.getOwningRole() ) || role.equals( property.getInverseRole() )) )
                {
                    return property;
                }
            }
        }

        return null;
    }

    @JsonIgnore
    public Map<String, Property> getPropertyMap()
    {
        return propertyMap;
    }

    public void setPropertyMap( Map<String, Property> propertyMap )
    {
        this.propertyMap = propertyMap;
    }

    @SuppressWarnings( "rawtypes" )
    private Set<Class> references;

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "references", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "reference", namespace = DxfNamespaces.DXF_2_0 )
    @SuppressWarnings( "rawtypes" )
    public Set<Class> getReferences()
    {
        if ( references == null )
        {
            references = getProperties().stream()
                .filter( p -> p.isCollection() ? PropertyType.REFERENCE == p.getItemPropertyType() : PropertyType.REFERENCE == p.getPropertyType() )
                .map( p -> p.isCollection() ? p.getItemKlass() : p.getKlass() ).collect( Collectors.toSet() );
        }

        return references;
    }

    public Map<String, Property> getPersistedProperties()
    {
        if ( persistedProperties == null )
        {
            persistedProperties = new HashMap<>();

            getPropertyMap().entrySet().stream()
                .filter( entry -> entry.getValue().isPersisted() )
                .forEach( entry -> persistedProperties.put( entry.getKey(), entry.getValue() ) );
        }

        return persistedProperties;
    }

    public Map<String, Property> getNonPersistedProperties()
    {
        if ( nonPersistedProperties == null )
        {
            nonPersistedProperties = new HashMap<>();

            getPropertyMap().entrySet().stream()
                .filter( entry -> !entry.getValue().isPersisted() )
                .forEach( entry -> nonPersistedProperties.put( entry.getKey(), entry.getValue() ) );
        }

        return nonPersistedProperties;
    }

    public void addProperty( Property property )
    {
        if ( property == null || property.getName() == null || propertyMap.containsKey( property.getName() ) )
        {
            return;
        }

        propertyMap.put( property.getName(), property );
    }

    @JsonIgnore
    public Property getProperty( String name )
    {
        if ( propertyMap.containsKey( name ) )
        {
            return propertyMap.get( name );
        }

        return null;
    }

    @JsonIgnore
    public Property getPersistedProperty( String name )
    {
        Property property = getProperty( name );

        if ( property != null && property.isPersisted() )
        {
            return property;
        }

        return null;
    }

    public List<String> getAuthorityByType( AuthorityType type )
    {
        List<String> authorityList = Lists.newArrayList();

        authorities.stream()
            .filter( authority -> type.equals( authority.getType() ) )
            .forEach( authority -> authorityList.addAll( authority.getAuthorities() ) );

        return authorityList;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getOrder()
    {
        return order;
    }

    public void setOrder( int order )
    {
        this.order = order;
    }


    @Override
    public int hashCode()
    {
        return Objects.hashCode( klass, identifiableObject, nameableObject, singular, plural, namespace, name,
            collectionName, shareable, relativeApiEndpoint, metadata, authorities, propertyMap, order );
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

        final Schema other = (Schema) obj;

        return Objects.equal( this.klass, other.klass ) && Objects.equal( this.identifiableObject, other.identifiableObject )
            && Objects.equal( this.nameableObject, other.nameableObject ) && Objects.equal( this.singular, other.singular )
            && Objects.equal( this.plural, other.plural ) && Objects.equal( this.namespace, other.namespace )
            && Objects.equal( this.name, other.name ) && Objects.equal( this.collectionName, other.collectionName )
            && Objects.equal( this.shareable, other.shareable ) && Objects.equal( this.relativeApiEndpoint, other.relativeApiEndpoint )
            && Objects.equal( this.metadata, other.metadata ) && Objects.equal( this.authorities, other.authorities )
            && Objects.equal( this.propertyMap, other.propertyMap ) && Objects.equal( this.order, other.order );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "klass", klass )
            .add( "identifiableObject", identifiableObject )
            .add( "nameableObject", nameableObject )
            .add( "singular", singular )
            .add( "plural", plural )
            .add( "namespace", namespace )
            .add( "name", name )
            .add( "collectionName", collectionName )
            .add( "shareable", shareable )
            .add( "relativeApiEndpoint", relativeApiEndpoint )
            .add( "metadata", metadata )
            .add( "authorities", authorities )
            .add( "propertyMap", propertyMap )
            .toString();
    }
}
