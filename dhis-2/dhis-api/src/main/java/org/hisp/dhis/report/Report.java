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
package org.hisp.dhis.report;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheable;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
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
import org.hisp.dhis.visualization.ReportingParams;
import org.hisp.dhis.visualization.Visualization;

/**
 * @author Lars Helge Overland
 */
@Entity
@Table(name = "report")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "report", namespace = DXF_2_0)
public class Report extends BaseMetadataObject
    implements IdentifiableObject, Cacheable, MetadataObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "reportid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, length = 230)
  private String name;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 50)
  private ReportType type;

  @Type(type = "text")
  @Column(name = "designcontent", columnDefinition = "text")
  private String designContent;

  @Type(type = "jbList")
  @Column(name = "relativeperiods")
  private List<String> rawPeriods = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "visualizationid",
      foreignKey = @ForeignKey(name = "fk_report_visualizationid"))
  private Visualization visualization;

  /**
   * Only the reporting period and organisation unit flags are stored; {@link ReportParamsUserType}
   * maps them to the two columns of the report table.
   */
  @Type(type = "org.hisp.dhis.report.ReportParamsUserType")
  @Columns(
      columns = {@Column(name = "paramreportingmonth"), @Column(name = "paramorganisationunit")})
  private ReportingParams reportParams;

  @Enumerated(EnumType.STRING)
  @Column(name = "cachestrategy", length = 40)
  private CacheStrategy cacheStrategy = CacheStrategy.RESPECT_SYSTEM_SETTING;

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  @Transient private RelativePeriods relatives;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Report() {}

  public Report(String name, ReportType type, String designContent, Visualization visualization) {
    this.name = name;
    this.type = type;
    this.designContent = designContent;
    this.visualization = visualization;
  }

  public Report(
      String name,
      ReportType type,
      String designContent,
      RelativePeriods relatives,
      ReportingParams reportParams) {
    this.name = name;
    this.type = type;
    this.designContent = designContent;
    this.relatives = relatives;
    this.reportParams = reportParams;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean isTypeReportTable() {
    return type != null && ReportType.JASPER_REPORT_TABLE.equals(type);
  }

  public boolean isTypeJdbc() {
    return type != null && ReportType.JASPER_JDBC.equals(type);
  }

  public boolean isTypeHtml() {
    return type != null && ReportType.HTML.equals(type);
  }

  public boolean hasVisualization() {
    return visualization != null;
  }

  /** Indicates whether this report has relative periods. */
  public boolean hasRelativePeriods() {
    return !getRelatives().isEmpty();
  }

  /** Indicates whether this report has specific report parameters set. */
  public boolean hasReportParams() {
    return reportParams != null && reportParams.isSet();
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
        || obj instanceof Report other
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

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ReportType getType() {
    return type;
  }

  public void setType(ReportType type) {
    this.type = type;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getDesignContent() {
    return designContent;
  }

  public void setDesignContent(String designContent) {
    this.designContent = designContent;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Visualization getVisualization() {
    return visualization;
  }

  public void setVisualization(Visualization visualization) {
    this.visualization = visualization;
  }

  @Gist(included = Include.FALSE)
  @JsonProperty("relativePeriods")
  @JacksonXmlProperty(namespace = DXF_2_0)
  public RelativePeriods getRelatives() {
    if (relatives == null) {
      List<RelativePeriodEnum> enums = new ArrayList<>();

      if (rawPeriods != null) {
        for (String relativePeriod : rawPeriods) {
          if (RelativePeriodEnum.contains(relativePeriod)) {
            enums.add(RelativePeriodEnum.valueOf(relativePeriod));
          }
        }
      }

      return new RelativePeriods().setRelativePeriodsFromEnums(enums);
    }

    return relatives;
  }

  /**
   * It overrides the rawPeriods with the relative periods provided. This is done for backward
   * compatibility reasons.
   *
   * @param relatives the {@link RelativePeriods}.
   */
  public void setRelatives(RelativePeriods relatives) {
    if (relatives != null) {
      List<RelativePeriodEnum> enums = relatives.getRelativePeriodEnums();
      this.rawPeriods = new ArrayList<>(enums.size());

      for (RelativePeriodEnum periodEnum : enums) {
        this.rawPeriods.add(periodEnum.name());
      }

      this.relatives = relatives;
    }
  }

  public List<String> getRawPeriods() {
    return rawPeriods;
  }

  public void setRawPeriods(List<String> rawPeriods) {
    this.rawPeriods = rawPeriods;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ReportingParams getReportParams() {
    return reportParams;
  }

  public void setReportParams(ReportingParams reportParams) {
    this.reportParams = reportParams;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Override
  public CacheStrategy getCacheStrategy() {
    return cacheStrategy;
  }

  public void setCacheStrategy(CacheStrategy cacheStrategy) {
    this.cacheStrategy = cacheStrategy;
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
  @JacksonXmlProperty(namespace = DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translationProperty().getTranslation("NAME", getName());
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DXF_2_0)
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
  @JacksonXmlProperty(namespace = DXF_2_0)
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
  // Unsupported properties (no column in the report table)
  // -------------------------------------------------------------------------

  /** Report does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Report does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** Report does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** Report does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
