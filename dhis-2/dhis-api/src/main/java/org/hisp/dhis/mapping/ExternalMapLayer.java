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
package org.hisp.dhis.mapping;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Entity
@Table(name = "externalmaplayer")
@JacksonXmlRootElement(localName = "externalMapLayer", namespace = DxfNamespaces.DXF_2_0)
public class ExternalMapLayer extends BaseMetadataObject
    implements IdentifiableObject, MetadataObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "externalmaplayerid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  /*
   * The three enum columns are bytea: the type names the enum class itself, which Hibernate binds
   * as a SerializableType, so the values stay Java-serialized as they always were.
   */
  @Type(type = "org.hisp.dhis.mapping.MapService")
  @Column(name = "mapservice", nullable = false)
  private MapService mapService;

  @Type(type = "text")
  @Column(name = "url", nullable = false)
  private String url;

  @Type(type = "text")
  @Column(name = "attribution")
  private String attribution;

  @Type(type = "text")
  @Column(name = "layers")
  private String layers;

  @Type(type = "org.hisp.dhis.mapping.ImageFormat")
  @Column(name = "imageformat", nullable = false)
  private ImageFormat imageFormat;

  @Type(type = "org.hisp.dhis.mapping.MapLayerPosition")
  @Column(name = "maplayerposition", nullable = false)
  private MapLayerPosition mapLayerPosition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "legendsetid")
  private LegendSet legendSet;

  @Type(type = "text")
  @Column(name = "legendseturl")
  private String legendSetUrl;

  @Column(name = "description", length = 1024)
  private String description;

  @Column(name = "descriptionurl", length = 255)
  private String descriptionUrl;

  @Column(name = "image", length = 2097152)
  private String image;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -----------------------------------------------------
  // Constructor
  // -----------------------------------------------------

  public ExternalMapLayer() {}

  public ExternalMapLayer(String name) {
    this.name = name;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /**
   * Compares uid, code and name like {@link BaseIdentifiableObject}. Uses get-methods and the real
   * class to handle proxied objects.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ExternalMapLayer other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  @Override
  public String toString() {
    return ("{\"class\":\"%s\", \"id\":\"%s\", \"uid\":\"%s\", \"code\":\"%s\", \"name\":\"%s\","
            + " \"created\":\"%s\", \"lastUpdated\":\"%s\" }")
        .formatted(
            getClass(), getId(), getUid(), getCode(), getName(), getCreated(), getLastUpdated());
  }

  // -----------------------------------------------------
  // Getters & Setters
  // -----------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MapService getMapService() {
    return mapService;
  }

  public void setMapService(MapService mapService) {
    this.mapService = mapService;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getAttribution() {
    return attribution;
  }

  public void setAttribution(String attribution) {
    this.attribution = attribution;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "attribution", key = "ATTRIBUTION")
  @Sortable(whenPersisted = false)
  public String getDisplayAttribution() {
    return translationProperty().getTranslation("ATTRIBUTION", getAttribution());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLayers() {
    return layers;
  }

  public void setLayers(String layers) {
    this.layers = layers;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ImageFormat getImageFormat() {
    return imageFormat;
  }

  public void setImageFormat(ImageFormat imageFormat) {
    this.imageFormat = imageFormat;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MapLayerPosition getMapLayerPosition() {
    return mapLayerPosition;
  }

  public void setMapLayerPosition(MapLayerPosition mapLayerPosition) {
    this.mapLayerPosition = mapLayerPosition;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSet;
  }

  public void setLegendSet(LegendSet legendSet) {
    this.legendSet = legendSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLegendSetUrl() {
    return legendSetUrl;
  }

  public void setLegendSetUrl(String legendSetUrl) {
    this.legendSetUrl = legendSetUrl;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(max = 1024)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  @Sortable(whenPersisted = false)
  public String getDisplayDescription() {
    return translationProperty().getTranslation("DESCRIPTION", getDescription());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(max = 255)
  public String getDescriptionUrl() {
    return descriptionUrl;
  }

  public void setDescriptionUrl(String descriptionUrl) {
    this.descriptionUrl = descriptionUrl;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(max = 2097152)
  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  // -------------------------------------------------------------------------
  // Identifiable object getters and setters
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
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
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
  public void setCode(String code) {
    this.code = code;
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
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translationProperty().getTranslation("NAME", getName());
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translationProperty().getTranslations();
  }

  /** Clears out the translation cache. A null set is stored as an empty set. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    translationProperty().setTranslations(translations != null ? translations : new HashSet<>());
  }

  /**
   * Returns the embedded translations, which Hibernate loads as null when the translations column
   * is null.
   */
  private TranslationProperty translationProperty() {
    if (translations == null) {
      translations = new TranslationProperty();
    }

    return translations;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
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

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String userId) {
    getSharing().setOwner(userId);
  }

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
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // Unsupported properties (no column in the externalmaplayer table)
  // -------------------------------------------------------------------------

  /** ExternalMapLayer does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** ExternalMapLayer does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** ExternalMapLayer does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** ExternalMapLayer does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
