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
package org.hisp.dhis.legend;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.legend.comparator.LegendValueComparator;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Jan Henrik Overland
 */
@Entity
@Table(name = "maplegendset")
@JacksonXmlRootElement(localName = "legendSet", namespace = DxfNamespaces.DXF_2_0)
public class LegendSet extends BaseMetadataObject implements IdentifiableObject, MetadataObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "maplegendsetid")
  private long id;

  @Column(name = "code", length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues", columnDefinition = "jsonb")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing", columnDefinition = "jsonb")
  private Sharing sharing = new Sharing();

  @Column(name = "symbolizer", length = 255)
  private String symbolizer;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "maplegendsetid")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Legend> legends = new HashSet<>();

  public LegendSet() {}

  public LegendSet(String name, String symbolizer, Set<Legend> legends) {
    this.name = name;
    this.symbolizer = symbolizer;
    this.legends = legends;
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
        || obj instanceof IdentifiableObject other
            && getRealClass(this) == getRealClass(obj)
            && typedEquals(other);
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

  public void removeAllLegends() {
    legends.clear();
  }

  public Legend getLegendByUid(String uid) {
    for (Legend legend : legends) {
      if (legend != null && legend.getUid().equals(uid)) {
        return legend;
      }
    }

    return null;
  }

  public List<Legend> getSortedLegends() {
    return legends.stream().sorted(LegendValueComparator.INSTANCE).collect(Collectors.toList());
  }

  public Map<String, String> getLegendUidPropertyMap(IdScheme idScheme) {
    return legends.stream()
        .collect(Collectors.toMap(Legend::getUid, l -> l.getPropertyValue(idScheme)));
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getSymbolizer() {
    return symbolizer;
  }

  public void setSymbolizer(String symbolizer) {
    this.symbolizer = symbolizer;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "legends", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legend", namespace = DxfNamespaces.DXF_2_0)
  public Set<Legend> getLegends() {
    return legends;
  }

  public void setLegends(Set<Legend> legends) {
    this.legends = legends;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDisplayName() {
    return translations.getTranslation("NAME", getName());
  }

  @Override
  public Set<Translation> getTranslations() {
    return translations != null ? translations.getTranslations() : Set.of();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    if (this.translations == null) {
      this.translations = new TranslationProperty();
    }
    this.translations.setTranslations(translations);
  }

  @Override
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeUid, String value) {
    this.attributeValues = attributeValues.added(attributeUid, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = attributeValues.removed(attributeId);
  }

  @Override
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }
    return sharing;
  }

  @Override
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  public void setPublicAccess(String access) {
    if (sharing == null) {
      sharing = new Sharing();
    }
    sharing.setPublicAccess(access);
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /**
   * @deprecated This method is replaced by {@link #getCreatedBy()}
   */
  @Override
  @Deprecated
  public User getUser() {
    return getCreatedBy();
  }

  /**
   * @deprecated This method is replaced by {@link #setCreatedBy(User)}
   */
  @Override
  @Deprecated
  public void setUser(User user) {
    setCreatedBy(user);
  }

  @Override
  @Deprecated
  public void setOwner(String owner) {
    // Not supported
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getName();
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getDisplayName();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return null;
  }
}
