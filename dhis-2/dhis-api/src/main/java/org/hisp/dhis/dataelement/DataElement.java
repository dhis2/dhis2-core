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
package org.hisp.dhis.dataelement;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionConstants.TEXTVALUE_COLUMN_NAME;
import static org.hisp.dhis.common.DimensionConstants.VALUE_COLUMN_NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseIdentifiableObject.AttributeValue;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.comparator.DataSetApprovalFrequencyComparator;
import org.hisp.dhis.dataset.comparator.DataSetFrequencyComparator;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

/**
 * @author Kristian Nordal
 */
@Setter
@Entity
@Table(name = "dataelement")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
public class DataElement extends BaseMetadataObject
    implements DimensionalItemObject, NameableObject, MetadataObject, ValueTypedDimensionalItemObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dataelementid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "formname", length = 230)
  private String formName;

  @Embedded
  private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jbObjectStyle")
  private ObjectStyle style;

  @Column(name = "fieldmask")
  private String fieldMask;

  /** Data element value type (int, boolean, etc) */
  @Enumerated(EnumType.STRING)
  @Column(name = "valueType", length = 50, nullable = false)
  private ValueType valueType;

  /** Abstract class representing options for value types. */
  @Type(type = "jbValueTypeOptions")
  private ValueTypeOptions valueTypeOptions;

  /**
   * The domain of this DataElement; e.g. DataElementDomainType.AGGREGATE or
   * DataElementDomainType.TRACKER.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "domainType", nullable = false)
  private DataElementDomain domainType;

  @Enumerated(EnumType.STRING)
  @Column(name = "aggregationtype", length = 50, nullable = false)
  private AggregationType aggregationType;

  /**
   * A combination of categories to capture data for this data element. Note that this category
   * combination could be overridden by data set elements which this data element is part of, see
   * {@link DataSetElement}.
   */
  @ManyToOne
  @JoinColumn(
      name = "categorycomboid",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_dataelement_categorycomboid"))
  private CategoryCombo categoryCombo;

  /** URL for lookup of additional information on the web. */
  @Column(name = "url")
  private String url;

  /** The data element groups which this data element is a member of. */
  @ManyToMany(mappedBy = "members")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataElementGroup> groups = new HashSet<>();

  /** The data sets which this data element is a member of. */
  @OneToMany(mappedBy = "dataElement")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataSetElement> dataSetElements = new HashSet<>();

  /** The lower organisation unit levels for aggregation. */
  @ElementCollection
  @CollectionTable(
      name = "dataelementaggregationlevels",
      joinColumns = @JoinColumn(name = "dataelementid"),
      foreignKey = @ForeignKey(name = "fk_dataelementaggregationlevels_dataelementid"))
  @Column(name = "aggregationlevel")
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Integer> aggregationLevels = new ArrayList<>();

  /** Indicates whether to store zero data values. */
  @Column(name = "zeroissignificant", nullable = false)
  private boolean zeroIsSignificant;

  /** The option set for data values linked to this data element, can be null. */
  @AuditAttribute
  @ManyToOne
  @JoinColumn(
      name = "optionsetid",
      foreignKey = @ForeignKey(name = "fk_dataelement_optionsetid"))
  private OptionSet optionSet;

  /** The option set for comments linked to this data element, can be null. */
  @ManyToOne
  @JoinColumn(
      name = "commentoptionsetid",
      foreignKey = @ForeignKey(name = "fk_dataelement_commentoptionsetid"))
  private OptionSet commentOptionSet;

  @ManyToMany
  @JoinTable(
      name = "dataelementlegendsets",
      joinColumns = @JoinColumn(name = "dataelementid"),
      inverseJoinColumns =
          @JoinColumn(
              name = "legendsetid",
              foreignKey = @ForeignKey(name = "fk_dataelement_legendsetid")))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<LegendSet> legendSets = new ArrayList<>();

  @AuditAttribute
  @Type(type = "jsbAttributeValues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  private Sharing sharing = new Sharing();

  /** The style defines how the DataElement should be represented on clients */
  // style field declared above

  /**
   * Field mask represent how the value should be formatted during input. This string will be
   * validated as a TextPatternSegment of type TEXT.
   */
  // fieldMask field declared above

  // -------------------------------------------------------------------------
  // Transient fields
  // -------------------------------------------------------------------------

  @Transient private transient QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataElement() {}

  public DataElement(String name) {
    this();
    this.name = name;
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof DataElement other
            && HibernateProxyUtils.getRealClass(this) == HibernateProxyUtils.getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(queryMods, other.queryMods);
  }

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (queryMods != null ? queryMods.hashCode() : 0);
    return result;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addDataElementGroup(DataElementGroup group) {
    groups.add(group);
    group.getMembers().add(this);
  }

  public void removeDataElementGroup(DataElementGroup group) {
    groups.remove(group);
    group.getMembers().remove(this);
  }

  public void updateDataElementGroups(Set<DataElementGroup> updates) {
    for (DataElementGroup group : new HashSet<>(groups)) {
      if (!updates.contains(group)) {
        removeDataElementGroup(group);
      }
    }

    updates.forEach(this::addDataElementGroup);
  }

  public boolean removeDataSetElement(DataSetElement element) {
    dataSetElements.remove(element);
    return element.getDataSet().getDataSetElements().remove(element);
  }

  public void addDataSetElement(DataSetElement element) {
    dataSetElements.add(element);
    element.getDataSet().getDataSetElements().add(element);
  }

  /**
   * Returns the resolved category combinations by joining the category combinations of the data set
   * elements of which this data element is part of and the category combination linked directly
   * with this data element. The returned set is immutable, will never be null and will contain at
   * least one item.
   */
  public Set<CategoryCombo> getCategoryCombos() {
    return ImmutableSet.<CategoryCombo>builder()
        .addAll(
            dataSetElements.stream()
                .filter(DataSetElement::hasCategoryCombo)
                .map(DataSetElement::getCategoryCombo)
                .collect(Collectors.toSet()))
        .add(categoryCombo)
        .build();
  }

  /**
   * Returns the category combination of the data set element matching the given data set for this
   * data element. If not present, returns the category combination for this data element.
   */
  public CategoryCombo getDataElementCategoryCombo(DataSet dataSet) {
    for (DataSetElement element : dataSetElements) {
      if (dataSet.typedEquals(element.getDataSet()) && element.hasCategoryCombo()) {
        return element.getCategoryCombo();
      }
    }

    return categoryCombo;
  }

  /**
   * Returns the category option combinations of the resolved category combinations of this data
   * element. The returned set is immutable, will never be null and will contain at least one item.
   */
  public Set<CategoryOptionCombo> getCategoryOptionCombos() {
    return getCategoryCombos().stream()
        .map(CategoryCombo::getOptionCombos)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the sorted category option combinations of the resolved category combinations of this
   * data element. The returned list is immutable, will never be null and will contain at least one
   * item.
   */
  public List<CategoryOptionCombo> getSortedCategoryOptionCombos() {
    List<CategoryOptionCombo> optionCombos = Lists.newArrayList();
    getCategoryCombos().forEach(cc -> optionCombos.addAll(cc.getSortedOptionCombos()));
    return optionCombos;
  }

  /** Indicates whether the value type of this data element is numeric. */
  public boolean isNumericType() {
    return getValueType().isNumeric();
  }

  /**
   * Indicates whether the value type of this data element is a file (externally stored resource)
   */
  public boolean isFileType() {
    return getValueType().isFile();
  }

  /**
   * Data type for use in analytics. Both text and date types are recognized as TEXT. Everything
   * else is recognized as NUMERIC. Note that this needs to be based on the QueryModifiers.valueType
   * if present.
   */
  public DataType getAnalyticsDataType() {
    ValueType vType = getValueType();

    return (vType.isText() || vType.isDate()) ? DataType.TEXT : DataType.NUMERIC;
  }

  /** The analytics value column to use for this data element. */
  public String getValueColumn() {
    return (getAnalyticsDataType() == DataType.TEXT) ? TEXTVALUE_COLUMN_NAME : VALUE_COLUMN_NAME;
  }

  /**
   * Returns the data set of this data element. If this data element has multiple data sets, the
   * data set with the highest collection frequency is returned.
   */
  public DataSet getDataSet() {
    List<DataSet> list = new ArrayList<>(getDataSets());
    list.sort(DataSetFrequencyComparator.INSTANCE);
    return !list.isEmpty() ? list.get(0) : null;
  }

  /**
   * Returns the data set of this data element. If this data element has multiple data sets, the
   * data set with approval enabled, then the highest collection frequency, is returned.
   */
  public DataSet getApprovalDataSet() {
    List<DataSet> list = new ArrayList<>(getDataSets());
    list.sort(DataSetApprovalFrequencyComparator.INSTANCE);
    return !list.isEmpty() ? list.get(0) : null;
  }

  /**
   * Note that this method returns an immutable set and can not be used to modify the model. Returns
   * an immutable set of data sets associated with this data element.
   */
  public Set<DataSet> getDataSets() {
    return dataSetElements.stream()
        .map(DataSetElement::getDataSet)
        .filter(Objects::nonNull)
        .collect(toUnmodifiableSet());
  }

  /**
   * Returns the PeriodType of the DataElement, based on the PeriodType of the DataSet which the
   * DataElement is associated with. If this data element has multiple data sets, the data set with
   * the highest collection frequency is returned.
   */
  public PeriodType getPeriodType() {
    DataSet dataSet = getDataSet();

    return dataSet != null ? dataSet.getPeriodType() : null;
  }

  /**
   * Returns the PeriodTypes of the DataElement, based on the PeriodType of the DataSets which the
   * DataElement is associated with.
   */
  public Set<PeriodType> getPeriodTypes() {
    return getDataSets().stream().map(DataSet::getPeriodType).collect(Collectors.toSet());
  }

  /** Tests whether more than one aggregation level exists for the DataElement. */
  public boolean hasAggregationLevels() {
    return isNotEmpty(aggregationLevels);
  }

  /**
   * Indicates whether this data element has a description.
   *
   * @return true if this data element has a description.
   */
  public boolean hasDescription() {
    return isNotEmpty(description);
  }

  /**
   * Indicates whether this data element has a URL.
   *
   * @return true if this data element has a URL.
   */
  public boolean hasUrl() {
    return isNotEmpty(url);
  }

  /**
   * Indicates whether this data element has an option set.
   *
   * @return true if this data element has an option set.
   */
  @Override
  public boolean hasOptionSet() {
    return optionSet != null;
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject implementation
  // -------------------------------------------------------------------------

  // TODO can also be dimension

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.DATA_ELEMENT;
  }

  @Override
  public String getDimensionItem() {
    return getUid();
  }

  @Override
  public String getDimensionItem(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return (queryMods != null && queryMods.getAggregationType() != null)
        ? queryMods.getAggregationType()
        : aggregationType;
  }

  @Override
  public boolean hasAggregationType() {
    return aggregationType != null;
  }

  @Override
  public TotalAggregationType getTotalAggregationType() {
    return getAggregationType() == AggregationType.NONE
        ? TotalAggregationType.NONE
        : TotalAggregationType.SUM;
  }

  @Override
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  public List<LegendSet> getLegendSets() {
    return legendSets;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSets.isEmpty() ? null : legendSets.get(0);
  }

  @Override
  public boolean hasLegendSet() {
    return !legendSets.isEmpty();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public QueryModifiers getQueryMods() {
    return queryMods;
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
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
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

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translations.getTranslation(
        "FORM_NAME", getFormName() != null ? getFormName() : getDisplayName());
  }

  @Override
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
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
  @JsonIgnore
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return attributeValues.get(idScheme.getAttribute());
    }
    return null;
  }

  @Override
  @JsonIgnore
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  @Override
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
  }

  // -------------------------------------------------------------------------
  // AttributeValues
  // -------------------------------------------------------------------------

  @Override
  @OpenApi.Property(AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return attributeValues;
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

  // -------------------------------------------------------------------------
  // NameableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getShortName() {
    return shortName;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORTNAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORTNAME", shortName);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", description);
  }

  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty property) {
    if (DisplayProperty.SHORTNAME == property && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  // -------------------------------------------------------------------------
  // Helper getters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isOptionSetValue() {
    return optionSet != null;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    // TODO return optionSet != null ? optionSet.getValueType() : valueType;
    return (queryMods != null && queryMods.getValueType() != null)
        ? queryMods.getValueType()
        : valueType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueTypeOptions getValueTypeOptions() {
    return valueTypeOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getFormName() {
    return formName;
  }

  /** Returns the form name, or the display name if it does not exist. */
  public String getFormNameFallback() {
    return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataElementDomain getDomainType() {
    return domainType;
  }

  @JsonProperty(value = "categoryCombo")
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getCategoryCombo() {
    return categoryCombo;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.URL)
  public String getUrl() {
    return url;
  }

  @JsonProperty("dataElementGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementGroup> getGroups() {
    return groups;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataSetElement> getDataSetElements() {
    return dataSetElements;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<Integer> getAggregationLevels() {
    return aggregationLevels;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isZeroIsSignificant() {
    return zeroIsSignificant;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getOptionSet() {
    return optionSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getCommentOptionSet() {
    return commentOptionSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFieldMask() {
    return fieldMask;
  }

  // -------------------------------------------------------------------------
  // Sharing helpers
  // -------------------------------------------------------------------------

  public void setPublicAccess(String access) {
    getSharing().setPublicAccess(access);
  }

  public String getPublicAccess() {
    return getSharing().getPublicAccess();
  }

  public Collection<UserAccess> getUserAccesses() {
    return getSharing().getUsers().values();
  }

  public Collection<UserGroupAccess> getUserGroupAccesses() {
    return getSharing().getUserGroups().values();
  }

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return attributeValues.get(attributeUid);
  }
}
