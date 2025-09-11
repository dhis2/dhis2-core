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
package org.hisp.dhis.option;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseIdentifiableObject.AttributeValue;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "optionSet", namespace = DxfNamespaces.DXF_2_0)
@Setter
@Entity
@Table(name = "optionset")
public class OptionSet extends BaseMetadataObject implements IdentifiableObject, VersionedObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "optionsetid")
  private long id;

  @Column(name = "code", unique = true, nullable = true, length = 50)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "valueType", length = 50, nullable = false)
  private ValueType valueType;

  private int version;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "optionsetid", foreignKey = @ForeignKey(name = "fk_optionset_optionid"))
  @OrderBy(value = "sortOrder ASC")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Option> options = new ArrayList<>();

  @AuditAttribute
  @Type(type = "jsbAttributeValues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------
  public OptionSet() {}

  public OptionSet(String name, ValueType valueType) {
    this.name = name;
    this.valueType = valueType;
  }

  public OptionSet(String name, ValueType valueType, List<Option> options) {
    this.name = name;
    this.valueType = valueType;
    this.options = options;
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /** Class check uses isAssignableFrom and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof IdentifiableObject
            && getRealClass(this) == getRealClass(obj)
            && typedEquals((IdentifiableObject) obj);
  }

  /**
   * Equality check against typed identifiable object. This method is not vulnerable to proxy
   * issues, where an uninitialized object class type fails comparison to a real class.
   *
   * @param other the identifiable object to compare this object against.
   * @return true if equal.
   */
  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addOption(Option option) {
    if (option.getSortOrder() == null) {
      this.options.add(option);
    } else {
      boolean added = false;
      final int size = this.options.size();
      for (int i = 0; i < size; i++) {
        Option thisOption = this.options.get(i);
        if (thisOption.getSortOrder() == null
            || thisOption.getSortOrder() > option.getSortOrder()) {
          this.options.add(i, option);
          added = true;
          break;
        }
      }
      if (!added) {
        this.options.add(option);
      }
    }
    option.setOptionSet(this);
  }

  public void removeOption(Option option) {
    if (!CollectionUtils.isEmpty(options)) {
      options.remove(option);
    }
  }

  @Override
  public int increaseVersion() {
    return ++version;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "options", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "option", namespace = DxfNamespaces.DXF_2_0)
  public List<Option> getOptions() {
    return options;
  }

  @JsonSetter(contentNulls = Nulls.SKIP)
  public void setOptions(List<Option> options) {
    this.options = options;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return valueType;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", name);
  }

  @Override
  @OpenApi.Property(AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return sharing;
  }

  @Override
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return createdBy;
  }

  @Override
  public void setUser(User user) {
    // TODO remove this after implementing functions for using Owner
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String ownerId) {
    getSharing().setOwner(ownerId);
  }

  @Gist(included = Include.FALSE)
  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  /** Clears out cache when setting translations. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  @Override
  public long getId() {
    return id;
  }

  // --------------------------------------------------
  // Copy methods from BaseIdentifiableObject
  // --------------------------------------------------

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return null;
    }
    return null;
  }

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}. If this happens to
   * refer to NAME, it returns the translatable/display version.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = attributeValues.added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = attributeValues.removed(attributeId);
  }

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return attributeValues.get(attributeUid);
  }
}
