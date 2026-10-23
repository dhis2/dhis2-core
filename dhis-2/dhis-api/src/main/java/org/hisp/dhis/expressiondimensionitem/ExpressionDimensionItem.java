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
package org.hisp.dhis.expressiondimensionitem;

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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DataDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
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
 * An Expression is the expression of e.g. a validation rule. It consist of a String representation
 * of the rule as well as references to the data elements and category option combos included in the
 * expression.
 *
 * <p>The expression can contain numbers and mathematical operators and contain references to data
 * elements and category option combos on the form:
 *
 * <p>i) [1.2] where 1 refers to the data element identifier and 2 refers to the category option
 * combo identifier.
 *
 * <p>ii) [1] where 1 refers to the data element identifier, in this case the formula represents the
 * total value for all category option combos for that data element.
 */
@Entity
@Table(name = "expressiondimensionitem")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "expressionDimensionItem", namespace = DxfNamespaces.DXF_2_0)
public class ExpressionDimensionItem extends BaseMetadataObject
    implements DataDimensionalItemObject, MetadataObject {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 8342548380669463589L;

  public static final String SEPARATOR = ".";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "expressiondimensionitemid")
  @Setter
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @Setter
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, length = 230)
  @Setter
  private String name;

  @Column(name = "shortname", nullable = false, length = 50)
  @Setter
  private String shortName;

  @Column(name = "formname")
  @Setter
  private String formName;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  @Setter
  private String description;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  /** The expression. */
  @Type(type = "text")
  @Column(name = "expression", columnDefinition = "text")
  @Setter
  private String expression;

  /** This expression should be given sliding window based data */
  @Column(name = "slidingwindow")
  @Setter
  private Boolean slidingWindow = false;

  /**
   * Indicates whether the expression should evaluate to null if all or any data values are missing
   * in the expression.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "missingvaluestrategy", length = 100)
  @Setter
  private MissingValueStrategy missingValueStrategy =
      MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  @AuditAttribute
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  @Setter
  private Sharing sharing = new Sharing();

  /** Query modifiers for this object, set by analytics; not persisted. */
  @Transient private transient QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Default empty Expression */
  public ExpressionDimensionItem() {}

  /**
   * @param expression The expression as a String
   * @param description A description of the Expression.
   */
  public ExpressionDimensionItem(String expression, String description) {
    this.expression = expression;
    this.description = description;
  }

  /**
   * Constructor with all the parameters.
   *
   * @param expression The expression as a String
   * @param description A description of the Expression.
   * @param missingValueStrategy Strategy for handling missing values.
   */
  public ExpressionDimensionItem(
      String expression, String description, MissingValueStrategy missingValueStrategy) {

    this.expression = expression;
    this.description = description;
    this.missingValueStrategy = missingValueStrategy;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (queryMods != null ? queryMods.hashCode() : 0);
    return result;
  }

  /** Class check uses get-methods and the real class to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ExpressionDimensionItem other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription())
            && Objects.equals(queryMods, other.queryMods);
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + id
        + "\", "
        + "\"expression\":\""
        + expression
        + "\", "
        + "\"description\":\""
        + description
        + "\" "
        + "}";
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Converts the current object to {@link Indicator}.
   *
   * @return the {@link Indicator} object.
   */
  public Indicator toIndicator() {
    IndicatorType indicatorType = new IndicatorType();
    indicatorType.setNumber(true);
    indicatorType.setFactor(1);

    Indicator indicator = new Indicator();
    indicator.setUid(getUid());
    indicator.setName(getName());
    indicator.setCode(getCode());
    indicator.setIndicatorType(indicatorType);
    indicator.setNumerator(getExpression());
    indicator.setNumeratorDescription(getDescription());
    indicator.setDenominator("1");
    indicator.setAnnualized(false);

    return indicator;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
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
    return translationProperty().getTranslation("NAME", getName());
  }

  @Override
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    if (attributeValues == null) {
      attributeValues = AttributeValues.empty();
    }

    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = getAttributeValues().added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = getAttributeValues().removed(attributeId);
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

  /** A null value is stored as an empty set. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    translationProperty().setTranslations(translations == null ? new HashSet<>() : translations);
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
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return getAttributeValues().get(idScheme.getAttribute());
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------------------------------
  // NameableObject
  // -------------------------------------------------------------------------

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
  public String getShortName() {
    return shortName;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translationProperty().getTranslation("SHORT_NAME", getShortName());
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 1)
  public String getDescription() {
    return description;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translationProperty().getTranslation("DESCRIPTION", getDescription());
  }

  /**
   * Returns the display property indicated by the given display property. Falls back to display
   * name if display short name is null.
   */
  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return formName;
  }

  /** Returns the form name, or the display name if the form name is empty. */
  public String getFormNameFallback() {
    return StringUtils.isEmpty(getFormName()) ? getDisplayName() : getFormName();
  }

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translationProperty().getTranslation("FORM_NAME", getFormNameFallback());
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDimensionItem() {
    return getUid();
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  @Override
  @JsonProperty
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.EXPRESSION_DIMENSION_ITEM;
  }

  /** The aggregation type is not persisted; only query modifiers can set one. */
  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return queryMods != null ? queryMods.getAggregationType() : null;
  }

  @Override
  public boolean hasAggregationType() {
    return getAggregationType() != null;
  }

  @Override
  public TotalAggregationType getTotalAggregationType() {
    return getAggregationType() == AggregationType.NONE
        ? TotalAggregationType.NONE
        : TotalAggregationType.SUM;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public QueryModifiers getQueryMods() {
    return queryMods;
  }

  @Override
  public void setQueryMods(QueryModifiers queryMods) {
    this.queryMods = queryMods;
  }

  @Override
  public boolean hasLegendSet() {
    return false;
  }

  // -------------------------------------------------------------------------
  // DataDimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  public boolean hasAggregateExportCategoryOptionCombo() {
    return false;
  }

  @Override
  public boolean hasAggregateExportAttributeOptionCombo() {
    return false;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getExpression() {
    return expression;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MissingValueStrategy getMissingValueStrategy() {
    return missingValueStrategy;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSlidingWindow() {
    return slidingWindow;
  }

  // -------------------------------------------------------------------------
  // Unsupported DimensionalItemObject methods
  // -------------------------------------------------------------------------

  /** ExpressionDimensionItem does not support setting the dimension item type. */
  @Deprecated(since = "2.44")
  public void setDimensionItemType(DimensionItemType dimensionItemType) {
    // Not supported
  }

  /** ExpressionDimensionItem does not support aggregation type. */
  @Deprecated(since = "2.44")
  public void setAggregationType(AggregationType aggregationType) {
    // Not supported
  }

  /** ExpressionDimensionItem does not support legend sets. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  public List<LegendSet> getLegendSets() {
    return new ArrayList<>();
  }

  /** ExpressionDimensionItem does not support legend sets. */
  @Deprecated(since = "2.44")
  public void setLegendSets(List<LegendSet> legendSets) {
    // Not supported
  }

  /** ExpressionDimensionItem does not support legend sets. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return null;
  }

  // -------------------------------------------------------------------------
  // Unsupported DataDimensionalItemObject methods
  // -------------------------------------------------------------------------

  /** ExpressionDimensionItem does not support an aggregate export category option combo. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getAggregateExportCategoryOptionCombo() {
    return null;
  }

  /** ExpressionDimensionItem does not support an aggregate export category option combo. */
  @Deprecated(since = "2.44")
  public void setAggregateExportCategoryOptionCombo(String aggregateExportCategoryOptionCombo) {
    // Not supported
  }

  /** ExpressionDimensionItem does not support an aggregate export attribute option combo. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getAggregateExportAttributeOptionCombo() {
    return null;
  }

  /** ExpressionDimensionItem does not support an aggregate export attribute option combo. */
  @Deprecated(since = "2.44")
  public void setAggregateExportAttributeOptionCombo(String aggregateExportAttributeOptionCombo) {
    // Not supported
  }
}
