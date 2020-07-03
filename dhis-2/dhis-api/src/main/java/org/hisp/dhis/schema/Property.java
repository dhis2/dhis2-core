package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.translation.TranslationProperty;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "property", namespace = DxfNamespaces.DXF_2_0 )
public class Property implements Ordered, Klass
{
    /**
     * Class for property.
     */
    private Class<?> klass;

    /**
     * Normalized type of this property
     */
    private PropertyType propertyType;

    /**
     * If this property is a collection, this is the class of the items inside the collection.
     */
    private Class<?> itemKlass;

    /**
     * If this property is a collection, this is the normalized type of the items inside the collection.
     */
    private PropertyType itemPropertyType;

    /**
     * Direct link to getter for this property.
     */
    private Method getterMethod;

    /**
     * Direct link to setter for this property.
     */
    private Method setterMethod;

    /**
     * Name for this property, if this class is a collection, it is the name of the items -inside- the collection
     * and not the collection wrapper itself.
     */
    private String name;

    /**
     * Name for actual field, used to persistence operations and getting setter/getter.
     */
    private String fieldName;

    /**
     * Is this property persisted somewhere. This property will be used to create criteria queries
     * on demand (default: false)
     */
    private boolean persisted;

    /**
     * Name of collection wrapper.
     */
    private String collectionName;

    /**
     * If this Property is a collection, should it be wrapped with collectionName?
     */
    private Boolean collectionWrapping;

    /**
     * Description if provided, will be fetched from @Description annotation.
     *
     * @see org.hisp.dhis.common.annotation.Description
     */
    private String description;

    /**
     * Namespace used for this property.
     */
    private String namespace;

    /**
     * Usually only used for XML. Is this property considered an attribute.
     */
    private boolean attribute;

    /**
     * This property is true if the type pointed to does not export any properties itself, it is then
     * assumed to be a primitive type. If collection is true, this this check is done on the generic type
     * of the collection, e.g. List<String> would set simple to be true, but List<DataElement> would set it
     * to false.
     */
    private boolean simple;

    /**
     * This property is true if the type of this property is a sub-class of Collection.
     *
     * @see java.util.Collection
     */
    private boolean collection;

    /**
     * This property is true if collection=true and klass points to a implementation with
     * a stable order (i.e. List).
     */
    private boolean ordered;

    /**
     * If this property is a complex object or a collection, is this property considered
     * the owner of that relationship (important for imports etc).
     */
    private boolean owner;

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
     * Does this class implement {@link EmbeddedObject} ?
     */
    private boolean embeddedObject;

    /**
     * Does this class implement {@link EmbeddedObject} ?
     */
    private boolean analyticalObject;

    /**
     * Can this property be read.
     */
    private boolean readable;

    /**
     * Can this property be written to.
     */
    private boolean writable;

    /**
     * Are the values for this property required to be unique?
     */
    private boolean unique;

    /**
     * Nullability of this property.
     */
    private boolean required;

    /**
     * Maximum length/size/value of this property.
     */
    private Integer length;

    /**
     * Minimum size/length of this property.
     */
    private Double max;

    /**
     * Minimum size/length of this property.
     */
    private Double min;

    /**
     * Cascading used when doing CRUD operations.
     */
    private String cascade;

    /**
     * Is property many-to-many.
     */
    private boolean manyToMany;

    /**
     * Is property one-to-one.
     */
    private boolean oneToOne;

    /**
     * Is property many-to-one.
     */
    private boolean manyToOne;

    /**
     * The hibernate role of the owning side.
     */
    private String owningRole;

    /**
     * The hibernate role of the inverse side (if many-to-many).
     */
    private String inverseRole;

    /**
     * If property type is enum, this is the list of valid options.
     */
    private List<String> constants;

    /**
     * Used by LinkService to link to the Schema describing this type (if reference).
     */
    private String href;

    /**
     * Points to relative Web-API endpoint (if exposed).
     */
    private String relativeApiEndpoint;

    /**
     * Used by LinkService to link to the API endpoint containing this type.
     */
    private String apiEndpoint;

    /**
     * Default value of the Property
     */
    private Object defaultValue;

    public Property()
    {
    }

    public Property( Class<?> klass )
    {
        setKlass( klass );
    }

    public Property( Class<?> klass, Method getter, Method setter )
    {
        this( klass );
        this.getterMethod = getter;
        this.setterMethod = setter;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Class<?> getKlass()
    {
        return klass;
    }

    public void setKlass( Class<?> klass )
    {
        this.identifiableObject = IdentifiableObject.class.isAssignableFrom( klass );
        this.nameableObject = NameableObject.class.isAssignableFrom( klass );
        this.embeddedObject = EmbeddedObject.class.isAssignableFrom( klass );
        this.klass = klass;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public PropertyType getPropertyType()
    {
        return propertyType;
    }

    public void setPropertyType( PropertyType propertyType )
    {
        this.propertyType = propertyType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Class<?> getItemKlass()
    {
        return itemKlass;
    }

    public void setItemKlass( Class<?> itemKlass )
    {
        this.itemKlass = itemKlass;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public PropertyType getItemPropertyType()
    {
        return itemPropertyType;
    }

    public void setItemPropertyType( PropertyType itemPropertyType )
    {
        this.itemPropertyType = itemPropertyType;
    }

    public Method getGetterMethod()
    {
        return getterMethod;
    }

    public void setGetterMethod( Method getterMethod )
    {
        this.getterMethod = getterMethod;
    }

    public Method getSetterMethod()
    {
        return setterMethod;
    }

    public void setSetterMethod( Method setterMethod )
    {
        this.setterMethod = setterMethod;
    }

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
    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName( String fieldName )
    {
        this.fieldName = fieldName;
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
    public String getCollectionName()
    {
        return collectionName != null ? collectionName : (isCollection() ? name : null);
    }

    public void setCollectionName( String collectionName )
    {
        this.collectionName = collectionName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isCollectionWrapping()
    {
        return collectionWrapping;
    }

    public void setCollectionWrapping( Boolean collectionWrapping )
    {
        this.collectionWrapping = collectionWrapping;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
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
    public boolean isAttribute()
    {
        return attribute;
    }

    public void setAttribute( boolean attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSimple()
    {
        return simple;
    }

    public void setSimple( boolean simple )
    {
        this.simple = simple;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCollection()
    {
        return collection;
    }

    public void setCollection( boolean collection )
    {
        this.collection = collection;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOrdered()
    {
        return ordered;
    }

    public void setOrdered( boolean ordered )
    {
        this.ordered = ordered;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOwner()
    {
        return owner;
    }

    public void setOwner( boolean owner )
    {
        this.owner = owner;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIdentifiableObject()
    {
        return identifiableObject;
    }

    public void setIdentifiableObject( boolean identifiableObject )
    {
        this.identifiableObject = identifiableObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isNameableObject()
    {
        return nameableObject;
    }

    public void setNameableObject( boolean nameableObject )
    {
        this.nameableObject = nameableObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isEmbeddedObject()
    {
        return embeddedObject;
    }

    public void setEmbeddedObject( boolean embeddedObject )
    {
        this.embeddedObject = embeddedObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isAnalyticalObject()
    {
        return analyticalObject;
    }

    public void setAnalyticalObject(boolean analyticalObject)
    {
        this.analyticalObject = analyticalObject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isReadable()
    {
        return readable;
    }

    public void setReadable( boolean readable )
    {
        this.readable = readable;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWritable()
    {
        return writable;
    }

    public void setWritable( boolean writable )
    {
        this.writable = writable;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUnique()
    {
        return unique;
    }

    public void setUnique( boolean unique )
    {
        this.unique = unique;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getLength()
    {
        return length;
    }

    public void setLength( Integer length )
    {
        this.length = length;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getMax()
    {
        return max;
    }

    public void setMax( Double max )
    {
        this.max = max;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Double getMin()
    {
        return min;
    }

    public void setMin( Double min )
    {
        this.min = min;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCascade()
    {
        return cascade;
    }

    public void setCascade( String cascade )
    {
        this.cascade = cascade;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isManyToMany()
    {
        return manyToMany;
    }

    public void setManyToMany( boolean manyToMany )
    {
        this.manyToMany = manyToMany;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOneToOne()
    {
        return oneToOne;
    }

    public void setOneToOne( boolean oneToOne )
    {
        this.oneToOne = oneToOne;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isManyToOne()
    {
        return manyToOne;
    }

    public void setManyToOne( boolean manyToOne )
    {
        this.manyToOne = manyToOne;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOwningRole()
    {
        return owningRole;
    }

    public void setOwningRole( String owningRole )
    {
        this.owningRole = owningRole;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getInverseRole()
    {
        return inverseRole;
    }

    public void setInverseRole( String inverseRole )
    {
        this.inverseRole = inverseRole;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TranslationProperty getTranslationKey()
    {
        if ( !isPersisted() )
        {
            return null;
        }

        return TranslationProperty.fromValue( name );
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "constants", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "constant", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getConstants()
    {
        return constants;
    }

    public void setConstants( List<String> constants )
    {
        this.constants = constants;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue( Object defaultValue )
    {
        if ( defaultValue != null && klass.isAssignableFrom( defaultValue.getClass() ))
        {
            this.defaultValue = defaultValue;
        }
        else
        {
            this.defaultValue = null;
        }
    }

    public String key()
    {
        return isCollection() ? collectionName : name;
    }

    public boolean is( PropertyType propertyType )
    {
        return propertyType != null && propertyType.equals( this.propertyType );
    }

    @Override
    public int getOrder()
    {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( klass, propertyType, itemKlass, itemPropertyType, getterMethod, setterMethod, name, fieldName, persisted, collectionName,
            collectionWrapping, description, namespace, attribute, simple, collection, owner, identifiableObject, nameableObject, readable, writable,
            unique, required, length, max, min, cascade, manyToMany, oneToOne, manyToOne, owningRole, inverseRole, constants, defaultValue );
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

        final Property other = (Property) obj;

        return Objects.equals( this.klass, other.klass )
            && Objects.equals( this.propertyType, other.propertyType )
            && Objects.equals( this.itemKlass, other.itemKlass )
            && Objects.equals( this.itemPropertyType, other.itemPropertyType )
            && Objects.equals( this.getterMethod, other.getterMethod )
            && Objects.equals( this.setterMethod, other.setterMethod )
            && Objects.equals( this.name, other.name )
            && Objects.equals( this.fieldName, other.fieldName )
            && Objects.equals( this.persisted, other.persisted )
            && Objects.equals( this.collectionName, other.collectionName )
            && Objects.equals( this.collectionWrapping, other.collectionWrapping )
            && Objects.equals( this.description, other.description )
            && Objects.equals( this.namespace, other.namespace )
            && Objects.equals( this.attribute, other.attribute )
            && Objects.equals( this.simple, other.simple )
            && Objects.equals( this.collection, other.collection )
            && Objects.equals( this.owner, other.owner )
            && Objects.equals( this.identifiableObject, other.identifiableObject )
            && Objects.equals( this.nameableObject, other.nameableObject )
            && Objects.equals( this.readable, other.readable )
            && Objects.equals( this.writable, other.writable )
            && Objects.equals( this.unique, other.unique )
            && Objects.equals( this.required, other.required )
            && Objects.equals( this.length, other.length )
            && Objects.equals( this.max, other.max )
            && Objects.equals( this.min, other.min )
            && Objects.equals( this.cascade, other.cascade )
            && Objects.equals( this.manyToMany, other.manyToMany )
            && Objects.equals( this.oneToOne, other.oneToOne )
            && Objects.equals( this.manyToOne, other.manyToOne )
            && Objects.equals( this.owningRole, other.owningRole )
            && Objects.equals( this.inverseRole, other.inverseRole )
            && Objects.equals( this.constants, other.constants )
            && Objects.equals( this.defaultValue, other.defaultValue );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "klass", klass )
            .add( "propertyType", propertyType )
            .add( "itemKlass", itemKlass )
            .add( "itemPropertyType", itemPropertyType )
            .add( "getterMethod", getterMethod )
            .add( "setterMethod", setterMethod )
            .add( "name", name )
            .add( "fieldName", fieldName )
            .add( "persisted", persisted )
            .add( "collectionName", collectionName )
            .add( "collectionWrapping", collectionWrapping )
            .add( "description", description )
            .add( "namespace", namespace )
            .add( "attribute", attribute )
            .add( "simple", simple )
            .add( "collection", collection )
            .add( "owner", owner )
            .add( "identifiableObject", identifiableObject )
            .add( "nameableObject", nameableObject )
            .add( "readable", readable )
            .add( "writable", writable )
            .add( "unique", unique )
            .add( "required", required )
            .add( "length", length )
            .add( "max", max )
            .add( "min", min )
            .add( "cascade", cascade )
            .add( "manyToMany", manyToMany )
            .add( "oneToOne", oneToOne )
            .add( "manyToOne", manyToOne )
            .add( "owningRole", owningRole )
            .add( "inverseRole", inverseRole )
            .add( "constants", constants )
            .add( "defaultValue", defaultValue )
            .toString();
    }
}
