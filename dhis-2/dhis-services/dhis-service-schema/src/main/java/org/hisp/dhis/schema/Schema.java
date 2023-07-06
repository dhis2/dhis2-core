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
package org.hisp.dhis.schema;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;
import org.springframework.core.Ordered;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "schema", namespace = DxfNamespaces.DXF_2_0)
public class Schema implements Ordered, Klass {
  /** Class that is described in this schema. */
  private final Class<?> klass;

  /**
   * Is this class a sub-class of IdentifiableObject
   *
   * @see org.hisp.dhis.common.IdentifiableObject
   */
  private final boolean identifiableObject;

  /**
   * Is this class a sub-class of NameableObject
   *
   * @see org.hisp.dhis.common.NameableObject
   */
  private final boolean nameableObject;

  /**
   * Is this class a sub-class of SubscribableObject
   *
   * @see org.hisp.dhis.common.SubscribableObject
   */
  private final boolean subscribableObject;

  /** Does this class implement {@link EmbeddedObject} ? */
  private final boolean embeddedObject;

  /** Singular name. */
  private final String singular;

  /** Plural name. */
  private final String plural;

  /** Is this class considered metadata, this is mainly used for our metadata importer/exporter. */
  private final boolean metadata;

  /**
   * Specifies if the class is a more installation specific metadata object, that will not be
   * exported by default. In some cases it is meaningful that this metadata can also be transferred
   * between system installations.
   */
  private final boolean secondaryMetadata;

  /** Namespace URI to be used for this class. */
  private String namespace;

  /**
   * This will normally be set to equal singular, but in certain cases it might be useful to have
   * another name for when this class is used as an item inside a collection.
   */
  private String name;

  /** A beautified (and possibly translated) name that can be used in UI. */
  private String displayName;

  /**
   * This will normally be set to equal plural, and is normally used as a wrapper for a collection
   * of instances of this klass type.
   */
  private String collectionName;

  /** Is sharing supported for instances of this class. */
  private Boolean shareable;

  /** Is data sharing supported for instances of this class. */
  private boolean dataShareable;

  /** Is data write sharing support for instances of this class. */
  private Boolean dataWriteShareable;

  /** Is data read sharing support for instances of this class. */
  private Boolean dataReadShareable;

  /** Points to relative Web-API endpoint (if exposed). */
  private String relativeApiEndpoint;

  /** Used by LinkService to link to the API endpoint containing this type. */
  private String apiEndpoint;

  /** Used by LinkService to link to the Schema describing this type (if reference). */
  private String href;

  /**
   * Are any properties on this class being persisted, if false, this file does not have any hbm
   * file attached to it.
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

  /** Database table name of this class */
  private String tableName;

  /** List of authorities required for doing operations on this class. */
  private List<Authority> authorities = Lists.newArrayList();

  /**
   * Map of all exposed properties on this class, where key is property name, and value is instance
   * of Property class.
   *
   * @see org.hisp.dhis.schema.Property
   */
  private Map<String, Property> propertyMap = Maps.newHashMap();

  /**
   * Map defining a way to retieve values from a set of properties. Only make sense for
   * IdentifiableObjects schemas
   */
  private Map<Collection<String>, Collection<Function<IdentifiableObject, String>>>
      uniqueMultiPropertiesExctractors = Collections.emptyMap();

  /** Map of all readable properties, cached on first request. */
  private final Map<String, Property> readableProperties = new HashMap<>();

  /** Map of all persisted properties, cached on first request. */
  private final Map<String, Property> persistedProperties = new HashMap<>();

  /** Map of all persisted properties, cached on first request. */
  private final Map<String, Property> nonPersistedProperties = new HashMap<>();

  /** Map of all link object properties, cached on first request. */
  private final Map<String, Property> embeddedObjectProperties = new TreeMap<>();

  /** Map of all analytical object properties, cached on first request. */
  private final Map<String, Property> analyticalObjectProperties = new TreeMap<>();

  /** Map containing cached authorities by their type. */
  @JsonIgnore
  private final ConcurrentMap<AuthorityType, List<String>> cachedAuthoritiesByType =
      new ConcurrentHashMap<>();

  /** Used for sorting of schema list when doing metadata import/export. */
  private int order = Ordered.LOWEST_PRECEDENCE;

  public Schema(Class<?> klass, String singular, String plural) {
    this.klass = klass;
    this.embeddedObject = EmbeddedObject.class.isAssignableFrom(klass);
    this.identifiableObject = IdentifiableObject.class.isAssignableFrom(klass);
    this.nameableObject = NameableObject.class.isAssignableFrom(klass);
    this.subscribableObject = SubscribableObject.class.isAssignableFrom(klass);
    this.singular = singular;
    this.plural = plural;
    this.metadata = MetadataObject.class.isAssignableFrom(klass);
    this.secondaryMetadata = SecondaryMetadataObject.class.isAssignableFrom(klass);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Class<?> getKlass() {
    return klass;
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
  public boolean isSubscribableObject() {
    return subscribableObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isEmbeddedObject() {
    return embeddedObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getSingular() {
    return singular;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getPlural() {
    return plural;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isMetadata() {
    return metadata;
  }

  /**
   * Returns if class contains more installation specific metadata, that will not be exported by
   * default. In some cases it is meaningful that this metadata can also be transferred between
   * system installations.
   *
   * @return <code>true</code> if class contains more installation specific metadata.
   */
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSecondaryMetadata() {
    return secondaryMetadata;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCollectionName() {
    return collectionName == null ? plural : collectionName;
  }

  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name == null ? singular : name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDisplayName() {
    return displayName != null ? displayName : getName();
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isShareable() {
    return shareable != null ? shareable : havePersistedProperty("sharing");
  }

  public void setShareable(boolean shareable) {
    this.shareable = shareable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDataShareable() {
    return dataShareable;
  }

  public void setDataShareable(boolean dataShareable) {
    this.dataShareable = dataShareable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDataWriteShareable() {
    return dataWriteShareable != null ? dataWriteShareable : isDataShareable();
  }

  public void setDataWriteShareable(boolean dataWriteShareable) {
    this.dataWriteShareable = dataWriteShareable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDataReadShareable() {
    return dataReadShareable != null ? dataReadShareable : isDataShareable();
  }

  public void setDataReadShareable(boolean dataReadShareable) {
    this.dataReadShareable = dataReadShareable;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getRelativeApiEndpoint() {
    return relativeApiEndpoint;
  }

  public void setRelativeApiEndpoint(String relativeApiEndpoint) {
    this.relativeApiEndpoint = relativeApiEndpoint;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getApiEndpoint() {
    return apiEndpoint;
  }

  public void setApiEndpoint(String apiEndpoint) {
    this.apiEndpoint = apiEndpoint;
  }

  public boolean haveApiEndpoint() {
    return getRelativeApiEndpoint() != null || getApiEndpoint() != null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getHref() {
    return href;
  }

  public void setHref(String href) {
    this.href = href;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isPersisted() {
    return persisted;
  }

  public void setPersisted(boolean persisted) {
    this.persisted = persisted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isTranslatable() {
    return isIdentifiableObject() && havePersistedProperty("translations");
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isFavoritable() {
    return isIdentifiableObject() && havePersistedProperty("favorites");
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSubscribable() {
    return isSubscribableObject() && havePersistedProperty("subscribers");
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDefaultPrivate() {
    return defaultPrivate;
  }

  public void setDefaultPrivate(boolean defaultPrivate) {
    this.defaultPrivate = defaultPrivate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isImplicitPrivateAuthority() {
    return implicitPrivateAuthority;
  }

  public void setImplicitPrivateAuthority(boolean implicitPrivateAuthority) {
    this.implicitPrivateAuthority = implicitPrivateAuthority;
  }

  @JsonIgnore
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "authorities", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "authority", namespace = DxfNamespaces.DXF_2_0)
  public List<Authority> getAuthorities() {
    return unmodifiableList(authorities);
  }

  public void add(Authority authority) {
    cachedAuthoritiesByType.remove(authority.getType());
    this.authorities.add(authority);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "properties", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "property", namespace = DxfNamespaces.DXF_2_0)
  public List<Property> getProperties() {
    return Lists.newArrayList(propertyMap.values());
  }

  public Map<Collection<String>, Collection<Function<IdentifiableObject, String>>>
      getUniqueMultiPropertiesExctractors() {
    return uniqueMultiPropertiesExctractors;
  }

  public void setUniqueMultiPropertiesExctractors(
      Map<Collection<String>, Collection<Function<IdentifiableObject, String>>>
          uniqueMultiPropertiesExctractors) {
    this.uniqueMultiPropertiesExctractors = uniqueMultiPropertiesExctractors;
  }

  public boolean haveProperty(String propertyName) {
    return getPropertyMap().containsKey(propertyName);
  }

  public boolean havePersistedProperty(String propertyName) {
    return haveProperty(propertyName) && getProperty(propertyName).isPersisted();
  }

  public Property propertyByRole(String role) {
    if (!StringUtils.isEmpty(role)) {
      for (Property property : propertyMap.values()) {
        if (property.isCollection()
            && property.isManyToMany()
            && (role.equals(property.getOwningRole()) || role.equals(property.getInverseRole()))) {
          return property;
        }
      }
    }

    return null;
  }

  @JsonIgnore
  public Map<String, Property> getPropertyMap() {
    return propertyMap;
  }

  public void setPropertyMap(Map<String, Property> propertyMap) {
    this.propertyMap = propertyMap;
    invalidatePropertyCaches();
  }

  @SuppressWarnings("rawtypes")
  private Set<Class> references;

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "references", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "reference", namespace = DxfNamespaces.DXF_2_0)
  @SuppressWarnings("rawtypes")
  public Set<Class> getReferences() {
    if (references == null) {
      references =
          getProperties().stream()
              .filter(Schema::isReferenceType)
              .map(Schema::getItemType)
              .collect(toSet());
    }
    return references;
  }

  private static Class<?> getItemType(Property p) {
    return p.isCollection() ? p.getItemKlass() : p.getKlass();
  }

  private static boolean isReferenceType(Property p) {
    return p.isCollection()
        ? PropertyType.REFERENCE == p.getItemPropertyType()
        : PropertyType.REFERENCE == p.getPropertyType();
  }

  public Map<String, Property> getReadableProperties() {
    initEmptyCache(readableProperties, Property::isReadable);
    return readableProperties;
  }

  public Map<String, Property> getPersistedProperties() {
    initEmptyCache(persistedProperties, Property::isPersisted);
    return persistedProperties;
  }

  public Map<String, Property> getNonPersistedProperties() {
    initEmptyCache(nonPersistedProperties, property -> !property.isPersisted());
    return nonPersistedProperties;
  }

  public Map<String, Property> getEmbeddedObjectProperties() {
    initEmptyCache(embeddedObjectProperties, Property::isEmbeddedObject);
    return embeddedObjectProperties;
  }

  public Map<String, Property> getAnalyticalObjectProperties() {
    initEmptyCache(analyticalObjectProperties, Property::isAnalyticalObject);
    return analyticalObjectProperties;
  }

  private void initEmptyCache(Map<String, Property> map, Predicate<Property> filter) {
    if (map.isEmpty()) {
      getPropertyMap().entrySet().stream()
          .filter(entry -> filter.test(entry.getValue()))
          .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
    }
  }

  public void addProperty(Property property) {
    if (property == null
        || property.getName() == null
        || propertyMap.containsKey(property.getName())) {
      return;
    }
    propertyMap.put(property.getName(), property);
    invalidatePropertyCaches();
  }

  private void invalidatePropertyCaches() {
    analyticalObjectProperties.clear();
    embeddedObjectProperties.clear();
    readableProperties.clear();
    persistedProperties.clear();
    nonPersistedProperties.clear();
    references = null;
  }

  public boolean hasAttributeValues() {
    return havePersistedProperty("attributeValues");
  }

  @JsonIgnore
  public Property getProperty(String name) {
    return propertyMap.get(name);
  }

  @JsonIgnore
  public Property getPersistedProperty(String name) {
    Property property = getProperty(name);

    if (property != null && property.isPersisted()) {
      return property;
    }

    return null;
  }

  public List<String> getAuthorityByType(AuthorityType type) {
    return cachedAuthoritiesByType.computeIfAbsent(type, this::computeAuthoritiesForType);
  }

  private List<String> computeAuthoritiesForType(AuthorityType type) {
    final Set<String> uniqueAuthorities = new LinkedHashSet<>();
    authorities.stream()
        .filter(authority -> authority.getType() == type)
        .forEach(authority -> uniqueAuthorities.addAll(authority.getAuthorities()));
    return unmodifiableList(new ArrayList<>(uniqueAuthorities));
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Gets a list of properties marked as unique for this schema
   *
   * @return a List of {@see Property}
   */
  public List<Property> getUniqueProperties() {
    return this.getProperties().stream()
        .filter(p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple())
        .collect(toList());
  }

  public Map<String, Property> getFieldNameMapProperties() {
    return this.getPersistedProperties().entrySet().stream()
        .collect(toMap(p -> p.getValue().getFieldName(), Entry::getValue));
  }

  /**
   * @return Get list of properties marked with {@link org.hisp.dhis.translation.Translatable}
   */
  public List<Property> getTranslatableProperties() {
    return this.getProperties().stream().filter(Property::isTranslatable).collect(toList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        klass,
        identifiableObject,
        nameableObject,
        singular,
        plural,
        namespace,
        name,
        collectionName,
        shareable,
        relativeApiEndpoint,
        metadata,
        authorities,
        propertyMap,
        order);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final Schema other = (Schema) obj;

    return java.util.Objects.equals(this.klass, other.klass)
        && Objects.equals(this.identifiableObject, other.identifiableObject)
        && Objects.equals(this.nameableObject, other.nameableObject)
        && Objects.equals(this.singular, other.singular)
        && Objects.equals(this.plural, other.plural)
        && Objects.equals(this.namespace, other.namespace)
        && Objects.equals(this.name, other.name)
        && Objects.equals(this.collectionName, other.collectionName)
        && Objects.equals(this.shareable, other.shareable)
        && Objects.equals(this.relativeApiEndpoint, other.relativeApiEndpoint)
        && Objects.equals(this.metadata, other.metadata)
        && Objects.equals(this.authorities, other.authorities)
        && Objects.equals(this.propertyMap, other.propertyMap)
        && Objects.equals(this.order, other.order);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("klass", klass)
        .add("identifiableObject", identifiableObject)
        .add("nameableObject", nameableObject)
        .add("singular", singular)
        .add("plural", plural)
        .add("namespace", namespace)
        .add("name", name)
        .add("collectionName", collectionName)
        .add("shareable", shareable)
        .add("relativeApiEndpoint", relativeApiEndpoint)
        .add("metadata", metadata)
        .add("authorities", authorities)
        .add("propertyMap", propertyMap)
        .toString();
  }
}
