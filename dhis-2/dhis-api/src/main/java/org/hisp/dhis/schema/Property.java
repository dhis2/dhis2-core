/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.springframework.core.Ordered;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "property", namespace = DxfNamespaces.DXF_2_0)
public class Property implements Ordered, Klass {
  /** Class for property. */
  private Class<?> klass;

  /** Normalized type of this property */
  @Setter private PropertyType propertyType;

  /** If this property is a collection, this is the class of the items inside the collection. */
  @Setter private Class<?> itemKlass;

  /**
   * If this property is a collection, this is the normalized type of the items inside the
   * collection.
   */
  @Setter private PropertyType itemPropertyType;

  /** Direct link to getter for this property. */
  @Getter @Setter private Method getterMethod;

  /** Direct link to setter for this property. */
  @Getter @Setter private Method setterMethod;

  /**
   * Name for this property, if this class is a collection, it is the name of the items -inside- the
   * collection and not the collection wrapper itself.
   */
  @Setter private String name;

  /** Name for actual field, used to persistence operations and getting setter/getter. */
  @Setter private String fieldName;

  /**
   * Is this property persisted somewhere. This property will be used to create criteria queries on
   * demand (default: false)
   */
  @Setter private boolean persisted;

  /** Name of collection wrapper. */
  @Setter private String collectionName;

  /** If this Property is a collection, should it be wrapped with collectionName? */
  @Setter private Boolean collectionWrapping;

  /**
   * Description if provided, will be fetched from @Description annotation.
   *
   * @see org.hisp.dhis.common.annotation.Description
   */
  @Setter private String description;

  /** Namespace used for this property. */
  @Setter private String namespace;

  /** Usually only used for XML. Is this property considered an attribute. */
  @Setter private boolean attribute;

  /**
   * This property is true if the type pointed to does not export any properties itself, it is then
   * assumed to be a primitive type. If collection is true, then this check is done on the generic
   * type of the collection, e.g. List<String> would set simple to be true, but List<DataElement>
   * would set it to false.
   */
  @Setter private boolean simple;

  /**
   * This property is true if the type of this property is a sub-class of Collection.
   *
   * @see java.util.Collection
   */
  @Setter private boolean collection;

  /**
   * This property is true if collection=true and klass points to a implementation with a stable
   * order (i.e. List).
   */
  @Setter private boolean ordered;

  /**
   * If this property is a complex object or a collection, is this property considered the owner of
   * that relationship (important for imports etc).
   */
  @Setter private boolean owner;

  /**
   * Is this class a sub-class of IdentifiableObject
   *
   * @see org.hisp.dhis.common.IdentifiableObject
   */
  @Setter private boolean identifiableObject;

  /**
   * Is this class a sub-class of NameableObject
   *
   * @see org.hisp.dhis.common.NameableObject
   */
  @Setter private boolean nameableObject;

  /** Does this class implement {@link EmbeddedObject} ? */
  @Setter private boolean embeddedObject;

  /** Does this class implement {@link EmbeddedObject} ? */
  @Setter private boolean analyticalObject;

  /** Can this property be read. */
  @Setter private boolean readable;

  /** Can this property be written to. */
  @Setter private boolean writable;

  /** Are the values for this property required to be unique? */
  @Setter private boolean unique;

  /** Nullability of this property. */
  @Setter private boolean required;

  /** Maximum length/size/value of this property. */
  @Setter private Integer length;

  /** Minimum size/length of this property. */
  @Setter private Double max;

  /** Minimum size/length of this property. */
  @Setter private Double min;

  /** Cascading used when doing CRUD operations. */
  @Setter private String cascade;

  /** Is property many-to-many. */
  @Setter private boolean manyToMany;

  /** Is property one-to-one. */
  @Setter private boolean oneToOne;

  /** Is property many-to-one. */
  @Setter private boolean manyToOne;

  /** Is property one-to-many. */
  @Setter private boolean oneToMany;

  /** The hibernate role of the owning side. */
  @Setter private String owningRole;

  /** The hibernate role of the inverse side (if many-to-many). */
  @Setter private String inverseRole;

  /** If property type is enum, this is the list of valid options. */
  @Setter private List<String> constants;

  /** Used by LinkService to link to the Schema describing this type (if reference). */
  @Setter private String href;

  /** Points to relative Web-API endpoint (if exposed). */
  @Setter private String relativeApiEndpoint;

  /** Used by LinkService to link to the API endpoint containing this type. */
  @Setter private String apiEndpoint;

  /** PropertyTransformer to apply to this property before and field filtering is applied. */
  @Getter @Setter private Class<? extends PropertyTransformer> propertyTransformer;

  /** Default value of the Property */
  private Object defaultValue;

  @Setter private boolean translatable;

  @Setter private String translationKey;

  /**
   * The translation key use for retrieving I18n translation of this property's name. The key
   * follows snake_case naming convention.
   */
  @Setter private String i18nTranslationKey;

  private GistPreferences gistPreferences = GistPreferences.DEFAULT;

  /** All annotations present on this property (either through field or method) */
  @Getter @Setter
  private Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

  public Property() {}

  public Property(Class<?> klass) {
    setKlass(klass);
  }

  public Property(Class<?> klass, Method getter, Method setter) {
    this(klass);
    this.getterMethod = getter;
    this.setterMethod = setter;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Class<?> getKlass() {
    return klass;
  }

  public void setKlass(Class<?> klass) {
    this.identifiableObject = IdentifiableObject.class.isAssignableFrom(klass);
    this.nameableObject = NameableObject.class.isAssignableFrom(klass);
    this.embeddedObject = EmbeddedObject.class.isAssignableFrom(klass);
    this.klass = klass;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public PropertyType getPropertyType() {
    return propertyType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Class<?> getItemKlass() {
    return itemKlass;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public PropertyType getItemPropertyType() {
    return itemPropertyType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFieldName() {
    return fieldName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isPersisted() {
    return persisted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCollectionName() {
    return collectionName != null ? collectionName : (isCollection() ? name : null);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isCollectionWrapping() {
    return collectionWrapping;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isAttribute() {
    return attribute;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSimple() {
    return simple;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCollection() {
    return collection;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSortable() {
    Sortable sortable = getterMethod == null ? null : getterMethod.getAnnotation(Sortable.class);
    return sortable != null
        ? sortable.value() && (!sortable.whenPersisted() || isPersisted())
        : !isCollection() && isSimple() && isPersisted();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isOrdered() {
    return ordered;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isOwner() {
    return owner;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isIdentifiableObject() {
    return identifiableObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isNameableObject() {
    return nameableObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isEmbeddedObject() {
    return embeddedObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isAnalyticalObject() {
    return analyticalObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isReadable() {
    return readable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isWritable() {
    return writable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isUnique() {
    return unique;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isRequired() {
    return required;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getLength() {
    return length;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Double getMax() {
    return max;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Double getMin() {
    return min;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCascade() {
    return cascade;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isManyToMany() {
    return manyToMany;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isOneToOne() {
    return oneToOne;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isManyToOne() {
    return manyToOne;
  }

  /**
   * @return true only, if this property reflects a relation to another table in the DB. There might
   *     be collections which are not actually relations since they store their data in a JSONB
   *     column.
   */
  @JsonIgnore
  public boolean isRelation() {
    return oneToOne || oneToMany || manyToOne || manyToMany;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isOneToMany() {
    return oneToMany;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOwningRole() {
    return owningRole;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getInverseRole() {
    return inverseRole;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getTranslationKey() {
    return this.translationKey;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getI18nTranslationKey() {
    return i18nTranslationKey;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "constants", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "constant", namespace = DxfNamespaces.DXF_2_0)
  public List<String> getConstants() {
    return constants;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getHref() {
    return href;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getRelativeApiEndpoint() {
    return relativeApiEndpoint;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getApiEndpoint() {
    return apiEndpoint;
  }

  @JsonProperty("propertyTransformer")
  @JacksonXmlProperty(localName = "propertyTransformer", namespace = DxfNamespaces.DXF_2_0)
  public boolean hasPropertyTransformer() {
    return propertyTransformer != null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object defaultValue) {
    if (defaultValue != null
        && klass.isAssignableFrom(HibernateProxyUtils.getRealClass(defaultValue))) {
      this.defaultValue = defaultValue;
    } else {
      this.defaultValue = null;
    }
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isTranslatable() {
    return this.translatable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public GistPreferences getGistPreferences() {
    return gistPreferences;
  }

  public void setGistPreferences(GistPreferences gistPreferences) {
    this.gistPreferences = gistPreferences == null ? GistPreferences.DEFAULT : gistPreferences;
  }

  @SuppressWarnings("unchecked")
  public <A extends Annotation> A getAnnotation(Class<? extends Annotation> annotationType) {
    return (A) annotations.get(annotationType);
  }

  public String key() {
    return isCollection() ? collectionName : name;
  }

  public boolean is(PropertyType propertyType) {
    return propertyType == this.propertyType;
  }

  public boolean itemIs(PropertyType propertyType) {
    return propertyType == this.itemPropertyType;
  }

  public boolean is(PropertyType... anyOf) {
    for (PropertyType type : anyOf) {
      if (is(type)) {
        return true;
      }
    }
    return false;
  }

  public boolean itemIs(PropertyType... anyOf) {
    for (PropertyType type : anyOf) {
      if (itemIs(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        klass,
        propertyType,
        itemKlass,
        itemPropertyType,
        getterMethod,
        setterMethod,
        name,
        fieldName,
        persisted,
        collectionName,
        collectionWrapping,
        description,
        namespace,
        attribute,
        simple,
        collection,
        owner,
        identifiableObject,
        nameableObject,
        readable,
        writable,
        unique,
        required,
        length,
        max,
        min,
        cascade,
        manyToMany,
        oneToOne,
        manyToOne,
        owningRole,
        inverseRole,
        constants,
        defaultValue);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final Property other = (Property) obj;

    return Objects.equals(this.klass, other.klass)
        && Objects.equals(this.propertyType, other.propertyType)
        && Objects.equals(this.itemKlass, other.itemKlass)
        && Objects.equals(this.itemPropertyType, other.itemPropertyType)
        && Objects.equals(this.getterMethod, other.getterMethod)
        && Objects.equals(this.setterMethod, other.setterMethod)
        && Objects.equals(this.name, other.name)
        && Objects.equals(this.fieldName, other.fieldName)
        && Objects.equals(this.persisted, other.persisted)
        && Objects.equals(this.collectionName, other.collectionName)
        && Objects.equals(this.collectionWrapping, other.collectionWrapping)
        && Objects.equals(this.description, other.description)
        && Objects.equals(this.namespace, other.namespace)
        && Objects.equals(this.attribute, other.attribute)
        && Objects.equals(this.simple, other.simple)
        && Objects.equals(this.collection, other.collection)
        && Objects.equals(this.owner, other.owner)
        && Objects.equals(this.identifiableObject, other.identifiableObject)
        && Objects.equals(this.nameableObject, other.nameableObject)
        && Objects.equals(this.readable, other.readable)
        && Objects.equals(this.writable, other.writable)
        && Objects.equals(this.unique, other.unique)
        && Objects.equals(this.required, other.required)
        && Objects.equals(this.length, other.length)
        && Objects.equals(this.max, other.max)
        && Objects.equals(this.min, other.min)
        && Objects.equals(this.cascade, other.cascade)
        && Objects.equals(this.manyToMany, other.manyToMany)
        && Objects.equals(this.oneToOne, other.oneToOne)
        && Objects.equals(this.manyToOne, other.manyToOne)
        && Objects.equals(this.owningRole, other.owningRole)
        && Objects.equals(this.inverseRole, other.inverseRole)
        && Objects.equals(this.constants, other.constants)
        && Objects.equals(this.defaultValue, other.defaultValue);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("klass", klass)
        .add("propertyType", propertyType)
        .add("itemKlass", itemKlass)
        .add("itemPropertyType", itemPropertyType)
        .add("getterMethod", getterMethod)
        .add("setterMethod", setterMethod)
        .add("name", name)
        .add("fieldName", fieldName)
        .add("persisted", persisted)
        .add("collectionName", collectionName)
        .add("collectionWrapping", collectionWrapping)
        .add("description", description)
        .add("namespace", namespace)
        .add("attribute", attribute)
        .add("simple", simple)
        .add("collection", collection)
        .add("owner", owner)
        .add("identifiableObject", identifiableObject)
        .add("nameableObject", nameableObject)
        .add("readable", readable)
        .add("writable", writable)
        .add("unique", unique)
        .add("required", required)
        .add("length", length)
        .add("max", max)
        .add("min", min)
        .add("cascade", cascade)
        .add("manyToMany", manyToMany)
        .add("oneToOne", oneToOne)
        .add("manyToOne", manyToOne)
        .add("owningRole", owningRole)
        .add("inverseRole", inverseRole)
        .add("constants", constants)
        .add("defaultValue", defaultValue)
        .toString();
  }
}
