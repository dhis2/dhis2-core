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
package org.hisp.dhis.dataset;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.InterpretableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

/**
 * This class is used for defining the standardized DataSets. A DataSet consists of a collection of
 * DataElements.
 *
 * @author Kristian Nordal
 */
@Entity
@Table(name = "dataset")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "dataSet", namespace = DxfNamespaces.DXF_2_0)
public class DataSet extends BaseMetadataObject
    implements DimensionalItemObject, VersionedObject, MetadataObject, InterpretableObject {
  public static final int NO_EXPIRY = 0;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "datasetid")
  private long id;

  @Column(name = "code", unique = true, length = 60)
  private String code;

  @Column(name = "name", nullable = false, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, unique = true, length = 50)
  private String shortName;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Type(type = "text")
  @Column(name = "formname", columnDefinition = "text")
  private String formName;

  @Type(type = "jbObjectStyle")
  @Column(name = "style")
  private ObjectStyle style;

  @Type(type = "jbPlainString")
  @Column(name = "displayoptions", length = 50000)
  private String displayOptions;

  /** The PeriodType indicating the frequency that this DataSet should be used */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "periodtypeid",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_dataset_periodtypeid"))
  private PeriodType periodType;

  /**
   * The dataInputPeriods is a set of periods with opening and closing dates, which determines the
   * period of which data can belong (period) and at which dates (between opening and closing dates)
   * actually registering this data is allowed. The same period can exist at the same time with
   * different opening and closing dates to allow for multiple periods for registering data.
   */
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(
      name = "datasetid",
      foreignKey = @ForeignKey(name = "fk_datasetdatainputperiods_datasetid"))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataInputPeriod> dataInputPeriods = new HashSet<>();

  /** All DataElements associated with this DataSet. */
  @OneToMany(mappedBy = "dataSet", cascade = CascadeType.ALL, orphanRemoval = true)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataSetElement> dataSetElements = new HashSet<>();

  /**
   * Indicators associated with this data set. Indicators are used for view and output purposes,
   * such as calculated fields in forms and reports.
   */
  @ManyToMany
  @JoinTable(
      name = "datasetindicators",
      joinColumns =
          @JoinColumn(
              name = "datasetid",
              foreignKey = @ForeignKey(name = "fk_datasetindicators_datasetid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "indicatorid",
              foreignKey = @ForeignKey(name = "fk_dataset_indicatorid")))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Indicator> indicators = new HashSet<>();

  /**
   * The DataElementOperands for which data must be entered in order for the DataSet to be
   * considered as complete.
   */
  @ManyToMany
  @Cascade(
      value = {
        org.hibernate.annotations.CascadeType.ALL,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
      })
  @JoinTable(
      name = "datasetoperands",
      joinColumns =
          @JoinColumn(
              name = "datasetid",
              foreignKey = @ForeignKey(name = "fk_datasetoperands_datasetid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "dataelementoperandid",
              foreignKey = @ForeignKey(name = "fk_dataset_dataelementoperandid")))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<DataElementOperand> compulsoryDataElementOperands = new HashSet<>();

  /** All Sources that register data with this DataSet. */
  @ManyToMany
  @JoinTable(
      name = "datasetsource",
      joinColumns =
          @JoinColumn(
              name = "datasetid",
              foreignKey = @ForeignKey(name = "fk_datasetsource_datasetid")),
      inverseJoinColumns =
          @JoinColumn(
              name = "sourceid",
              foreignKey = @ForeignKey(name = "fk_dataset_organisationunit")))
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<OrganisationUnit> sources = new HashSet<>();

  /** The Sections associated with the DataSet. */
  @OneToMany(mappedBy = "dataSet")
  @OrderBy("sortOrder")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private Set<Section> sections = new HashSet<>();

  /** The CategoryCombo used for data attributes. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "categorycomboid",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_dataset_categorycomboid"))
  private CategoryCombo categoryCombo;

  /** Indicating custom data entry form, can be null. */
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "dataentryform", foreignKey = @ForeignKey(name = "fk_dataset_dataentryform"))
  private DataEntryForm dataEntryForm;

  /** Property indicating if the dataset could be collected using mobile data entry. */
  @Column(name = "mobile")
  private boolean mobile; // TODO Remove, mobile service is now removed

  /** Indicating version number. */
  @Column(name = "version")
  private int version;

  /** How many days after period is over will this dataSet auto-lock */
  @Column(name = "expirydays")
  private double expiryDays;

  /** Days after period end to qualify for timely data submission */
  @Column(name = "timelydays")
  private double timelyDays;

  /** User group which will receive notifications when data set is marked complete, can be null. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "notificationrecipients",
      foreignKey = @ForeignKey(name = "fk_dataset_notificationrecipients"))
  private UserGroup notificationRecipients;

  /** Indicating whether the user completing this data set should be sent a notification. */
  @Column(name = "notifycompletinguser")
  private boolean notifyCompletingUser;

  /** The approval workflow for this data set, can be null. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflowid", foreignKey = @ForeignKey(name = "fk_dataset_workflowid"))
  private DataApprovalWorkflow workflow;

  /** Interpretations of this data set. */
  @OneToMany(mappedBy = "dataSet")
  private Set<Interpretation> interpretations = new HashSet<>();

  // -------------------------------------------------------------------------
  // Form properties
  // -------------------------------------------------------------------------

  /**
   * Number of periods in the future to open for data capture, 0 means capture not allowed for
   * current period.
   */
  @Column(name = "openfutureperiods")
  private int openFuturePeriods;

  /** Number of periods to open for data capture that are after the category option's end date. */
  @Column(name = "openperiodsaftercoenddate")
  private int openPeriodsAfterCoEndDate;

  /** Property indicating that all fields for a data element must be filled. */
  @Column(name = "fieldcombinationrequired")
  private boolean fieldCombinationRequired;

  /** Property indicating that all validation rules must pass before the form can be completed. */
  @Column(name = "validcompleteonly")
  private boolean validCompleteOnly;

  /**
   * Property indicating whether a comment is required for all fields in a form which are not
   * entered, including false for boolean values.
   */
  @Column(name = "novaluerequirescomment")
  private boolean noValueRequiresComment;

  /** Property indicating whether offline storage is enabled for this dataSet or not */
  @Column(name = "skipoffline")
  private boolean skipOffline;

  /** Property indicating whether it should enable data elements decoration in forms. */
  @Column(name = "dataelementdecoration")
  private boolean dataElementDecoration;

  /** Render default and section forms with tabs instead of multiple sections in one page */
  @Column(name = "renderastabs")
  private boolean renderAsTabs;

  /** Render multi-organisationUnit forms either with OU vertically or horizontally. */
  @Column(name = "renderhorizontally")
  private boolean renderHorizontally;

  /**
   * Property indicating whether all compulsory fields should be filled before completing data set
   */
  @Column(name = "compulsoryfieldscompleteonly")
  private boolean compulsoryFieldsCompleteOnly;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  /** The legend sets for this dimension. */
  @ManyToMany
  @JoinTable(
      name = "datasetlegendsets",
      joinColumns = @JoinColumn(name = "datasetid"),
      inverseJoinColumns =
          @JoinColumn(
              name = "legendsetid",
              foreignKey = @ForeignKey(name = "fk_dataset_legendsetid")))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<LegendSet> legendSets = new ArrayList<>();

  /** The aggregation type for this dimension. Not persisted for DataSet. */
  @Transient private AggregationType aggregationType;

  /** Query modifiers for this object. Not persisted for DataSet. */
  @Transient private transient QueryModifiers queryMods;

  // -------------------------------------------------------------------------
  // Dynamic attribute values / Sharing
  // -------------------------------------------------------------------------

  @AuditAttribute
  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataSet() {}

  public DataSet(String name) {
    this.name = name;
  }

  public DataSet(String name, PeriodType periodType) {
    this(name);
    this.periodType = periodType;
  }

  public DataSet(String name, String shortName, PeriodType periodType) {
    this(name, periodType);
    this.shortName = shortName;
  }

  public DataSet(String name, String shortName, String code, PeriodType periodType) {
    this(name, shortName, periodType);
    this.code = code;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addOrganisationUnit(OrganisationUnit organisationUnit) {
    sources.add(organisationUnit);
    organisationUnit.getDataSets().add(this);
  }

  public void addOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::addOrganisationUnit);
  }

  public boolean removeOrganisationUnit(OrganisationUnit organisationUnit) {
    sources.remove(organisationUnit);
    return organisationUnit.getDataSets().remove(this);
  }

  public void removeOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::removeOrganisationUnit);
  }

  public void removeAllOrganisationUnits() {
    for (OrganisationUnit unit : sources) {
      unit.getDataSets().remove(this);
    }

    sources.clear();
  }

  public void updateOrganisationUnits(Set<OrganisationUnit> updates) {
    Set<OrganisationUnit> toRemove = Sets.difference(sources, updates);
    Set<OrganisationUnit> toAdd = Sets.difference(updates, sources);

    toRemove.forEach(u -> u.getDataSets().remove(this));
    toAdd.forEach(u -> u.getDataSets().add(this));

    sources.clear();
    sources.addAll(updates);
  }

  public boolean hasOrganisationUnit(OrganisationUnit unit) {
    return sources.contains(unit);
  }

  public boolean addDataInputPeriod(DataInputPeriod dataInputPeriod) {
    return dataInputPeriods.add(dataInputPeriod);
  }

  public boolean addDataSetElement(DataSetElement element) {
    element.getDataElement().getDataSetElements().add(element);
    return dataSetElements.add(element);
  }

  /**
   * Adds a data set element using this data set, the given data element and no category combo.
   *
   * @param dataElement the data element.
   */
  public boolean addDataSetElement(DataElement dataElement) {
    DataSetElement element = new DataSetElement(this, dataElement, null);
    dataElement.getDataSetElements().add(element);
    return dataSetElements.add(element);
  }

  /**
   * Adds a data set element using this data set, the given data element and the given category
   * combo.
   *
   * @param dataElement the data element.
   * @param categoryCombo the category combination.
   */
  public boolean addDataSetElement(DataElement dataElement, CategoryCombo categoryCombo) {
    DataSetElement element = new DataSetElement(this, dataElement, categoryCombo);
    dataElement.getDataSetElements().add(element);
    return dataSetElements.add(element);
  }

  public boolean removeDataSetElement(DataSetElement element) {
    dataSetElements.remove(element);
    return element.getDataElement().getDataSetElements().remove(element);
  }

  public void removeDataSetElement(DataElement dataElement) {
    Iterator<DataSetElement> elements = dataSetElements.iterator();

    while (elements.hasNext()) {
      DataSetElement element = elements.next();

      DataSetElement other = new DataSetElement(this, dataElement);

      if (element.objectEquals(other)) {
        elements.remove();
        element.getDataElement().getDataSetElements().remove(element);
      }
    }
  }

  public void removeAllDataSetElements() {
    for (DataSetElement element : dataSetElements) {
      element.getDataElement().getDataSetElements().remove(element);
    }

    dataSetElements.clear();
  }

  public void addIndicator(Indicator indicator) {
    indicators.add(indicator);
    indicator.getDataSets().add(this);
  }

  public boolean removeIndicator(Indicator indicator) {
    indicators.remove(indicator);
    return indicator.getDataSets().remove(this);
  }

  public void removeIndicators(List<Indicator> indicators) {
    indicators.forEach(this::removeIndicator);
  }

  public void addCompulsoryDataElementOperand(DataElementOperand dataElementOperand) {
    compulsoryDataElementOperands.add(dataElementOperand);
  }

  public void removeCompulsoryDataElementOperand(DataElementOperand dataElementOperand) {
    compulsoryDataElementOperands.remove(dataElementOperand);
  }

  public void assignWorkflow(DataApprovalWorkflow workflow) {
    workflow.getDataSets().add(this);
    this.workflow = workflow;
  }

  public boolean hasDataEntryForm() {
    return dataEntryForm != null && dataEntryForm.hasForm();
  }

  public boolean hasSections() {
    return isNotEmpty(sections);
  }

  /**
   * Indicates whether data should be approved for this data set, i.e. whether this data set is part
   * of a data approval workflow.
   */
  public boolean isApproveData() {
    return workflow != null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FormType getFormType() {
    if (hasDataEntryForm()) {
      return FormType.CUSTOM;
    }

    if (hasSections()) {
      return FormType.SECTION;
    }

    return FormType.DEFAULT;
  }

  /**
   * Note that this method returns an immutable set and can not be used to modify the model. Returns
   * an immutable set of data sets associated with this data element.
   */
  public Set<DataElement> getDataElements() {
    return ImmutableSet.copyOf(
        dataSetElements.stream().map(DataSetElement::getDataElement).collect(Collectors.toSet()));
  }

  public Set<DataElement> getDataElementsInSections() {
    Set<DataElement> dataElements = new HashSet<>();

    for (Section section : sections) {
      dataElements.addAll(section.getDataElements());
    }

    return dataElements;
  }

  public Set<CategoryOptionCombo> getDataElementOptionCombos() {
    Set<CategoryOptionCombo> optionCombos = new HashSet<>();

    for (DataSetElement element : dataSetElements) {
      optionCombos.addAll(element.getResolvedCategoryCombo().getOptionCombos());
    }

    return optionCombos;
  }

  @Override
  public int increaseVersion() {
    return ++version;
  }

  /**
   * Returns a set of category option group sets which are linked to this data set through its
   * category combination.
   */
  public Set<CategoryOptionGroupSet> getCategoryOptionGroupSets() {
    Set<CategoryOptionGroupSet> groupSets = new HashSet<>();

    if (categoryCombo != null) {
      for (Category category : categoryCombo.getCategories()) {
        for (CategoryOption categoryOption : category.getCategoryOptions()) {
          groupSets.addAll(categoryOption.getGroupSets());
        }
      }
    }

    return groupSets;
  }

  /**
   * Indicates whether this data set has a category combination which is different from the default
   * category combination.
   */
  public boolean hasCategoryCombo() {
    return categoryCombo != null
        && !CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals(categoryCombo.getName());
  }

  // -------------------------------------------------------------------------
  // DimensionalItemObject
  // -------------------------------------------------------------------------

  @Override
  public DimensionItemType getDimensionItemType() {
    return DimensionItemType.REPORTING_RATE;
  }

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
  public boolean hasLegendSet() {
    return legendSets != null && !legendSets.isEmpty();
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
  @JacksonXmlElementWrapper(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  public List<LegendSet> getLegendSets() {
    return this.legendSets;
  }

  public void setLegendSets(List<LegendSet> legendSets) {
    this.legendSets = legendSets;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public LegendSet getLegendSet() {
    return legendSets.isEmpty() ? null : legendSets.get(0);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AggregationType getAggregationType() {
    return (queryMods != null && queryMods.getAggregationType() != null)
        ? queryMods.getAggregationType()
        : aggregationType;
  }

  public void setAggregationType(AggregationType aggregationType) {
    this.aggregationType = aggregationType;
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

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(using = JacksonPeriodTypeSerializer.class)
  @JsonDeserialize(using = JacksonPeriodTypeDeserializer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(PropertyType.TEXT)
  public PeriodType getPeriodType() {
    return periodType;
  }

  public void setPeriodType(PeriodType periodType) {
    this.periodType = periodType;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataInputPeriods", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataInputPeriods", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataInputPeriod> getDataInputPeriods() {
    return dataInputPeriods;
  }

  public void setDataInputPeriods(Set<DataInputPeriod> dataInputPeriods) {
    this.dataInputPeriods = dataInputPeriods;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataEntryForm getDataEntryForm() {
    return dataEntryForm;
  }

  public void setDataEntryForm(DataEntryForm dataEntryForm) {
    this.dataEntryForm = dataEntryForm;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataSetElement", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataSetElement> getDataSetElements() {
    return dataSetElements;
  }

  public void setDataSetElements(Set<DataSetElement> dataSetElements) {
    this.dataSetElements = dataSetElements;
  }

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "indicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicator", namespace = DxfNamespaces.DXF_2_0)
  public Set<Indicator> getIndicators() {
    return indicators;
  }

  public void setIndicators(Set<Indicator> indicators) {
    this.indicators = indicators;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "compulsoryDataElementOperands",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "compulsoryDataElementOperand", namespace = DxfNamespaces.DXF_2_0)
  public Set<DataElementOperand> getCompulsoryDataElementOperands() {
    return compulsoryDataElementOperands;
  }

  public void setCompulsoryDataElementOperands(
      Set<DataElementOperand> compulsoryDataElementOperands) {
    this.compulsoryDataElementOperands = compulsoryDataElementOperands;
  }

  @JsonProperty(value = "organisationUnits")
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getSources() {
    return sources;
  }

  public void setSources(Set<OrganisationUnit> sources) {
    this.sources = sources;
  }

  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "sections", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "section", namespace = DxfNamespaces.DXF_2_0)
  public Set<Section> getSections() {
    return sections;
  }

  public void setSections(Set<Section> sections) {
    this.sections = sections;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getCategoryCombo() {
    return categoryCombo;
  }

  public void setCategoryCombo(CategoryCombo categoryCombo) {
    this.categoryCombo = categoryCombo;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isMobile() {
    return mobile;
  }

  public void setMobile(boolean mobile) {
    this.mobile = mobile;
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
  @PropertyRange(min = Integer.MIN_VALUE)
  public double getExpiryDays() {
    return expiryDays;
  }

  public void setExpiryDays(double expiryDays) {
    this.expiryDays = expiryDays;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public double getTimelyDays() {
    return timelyDays;
  }

  public void setTimelyDays(double timelyDays) {
    this.timelyDays = timelyDays;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public UserGroup getNotificationRecipients() {
    return notificationRecipients;
  }

  public void setNotificationRecipients(UserGroup notificationRecipients) {
    this.notificationRecipients = notificationRecipients;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isNotifyCompletingUser() {
    return notifyCompletingUser;
  }

  public void setNotifyCompletingUser(boolean notifyCompletingUser) {
    this.notifyCompletingUser = notifyCompletingUser;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataApprovalWorkflow getWorkflow() {
    return workflow;
  }

  public void setWorkflow(DataApprovalWorkflow workflow) {
    this.workflow = workflow;
  }

  @Override
  @JsonProperty
  @JsonSerialize(contentAs = IdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "interpretations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "interpretation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Interpretation> getInterpretations() {
    return interpretations;
  }

  public void setInterpretations(Set<Interpretation> interpretations) {
    this.interpretations = interpretations;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getOpenFuturePeriods() {
    return openFuturePeriods;
  }

  public void setOpenFuturePeriods(int openFuturePeriods) {
    this.openFuturePeriods = openFuturePeriods;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getOpenPeriodsAfterCoEndDate() {
    return openPeriodsAfterCoEndDate;
  }

  public void setOpenPeriodsAfterCoEndDate(int openPeriodsAfterCoEndDate) {
    this.openPeriodsAfterCoEndDate = openPeriodsAfterCoEndDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isFieldCombinationRequired() {
    return fieldCombinationRequired;
  }

  public void setFieldCombinationRequired(boolean fieldCombinationRequired) {
    this.fieldCombinationRequired = fieldCombinationRequired;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isValidCompleteOnly() {
    return validCompleteOnly;
  }

  public void setValidCompleteOnly(boolean validCompleteOnly) {
    this.validCompleteOnly = validCompleteOnly;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isNoValueRequiresComment() {
    return noValueRequiresComment;
  }

  public void setNoValueRequiresComment(boolean noValueRequiresComment) {
    this.noValueRequiresComment = noValueRequiresComment;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSkipOffline() {
    return skipOffline;
  }

  public void setSkipOffline(boolean skipOffline) {
    this.skipOffline = skipOffline;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isRenderAsTabs() {
    return renderAsTabs;
  }

  public void setRenderAsTabs(boolean renderAsTabs) {
    this.renderAsTabs = renderAsTabs;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isRenderHorizontally() {
    return renderHorizontally;
  }

  public void setRenderHorizontally(boolean renderHorizontally) {
    this.renderHorizontally = renderHorizontally;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isDataElementDecoration() {
    return dataElementDecoration;
  }

  public void setDataElementDecoration(boolean dataElementDecoration) {
    this.dataElementDecoration = dataElementDecoration;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  public void setStyle(ObjectStyle style) {
    this.style = style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @OpenApi.Property(ObjectNode.class)
  public String getDisplayOptions() {
    return displayOptions;
  }

  public void setDisplayOptions(String displayOptions) {
    this.displayOptions = displayOptions;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCompulsoryFieldsCompleteOnly() {
    return compulsoryFieldsCompleteOnly;
  }

  public void setCompulsoryFieldsCompleteOnly(boolean compulsoryFieldsCompleteOnly) {
    this.compulsoryFieldsCompleteOnly = compulsoryFieldsCompleteOnly;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject / NameableObject
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
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 1)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", getName());
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORT_NAME", getShortName());
  }

  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", getDescription());
  }

  /** Returns the form name, or the name if it does not exist. */
  public String getFormNameFallback() {
    return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
  }

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translations.getTranslation("FORM_NAME", getFormNameFallback());
  }

  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @Override
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

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return attributeValues.get(attributeUid);
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

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
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
    // TODO remove this after implementing functions for using Owner
    setCreatedBy(getCreatedBy() == null ? user : getCreatedBy());
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String userId) {
    getSharing().setOwner(userId);
  }

  public void setPublicAccess(String access) {
    getSharing().setPublicAccess(access);
  }

  public String getPublicAccess() {
    if (sharing != null) {
      return sharing.getPublicAccess();
    }

    return null;
  }

  public Collection<UserAccess> getUserAccesses() {
    if (sharing == null || getSharing().getUsers() == null) {
      return Collections.emptyList();
    }

    return getSharing().getUsers().values();
  }

  public Collection<UserGroupAccess> getUserGroupAccesses() {
    if (sharing == null || getSharing().getUserGroups() == null) {
      return Collections.emptyList();
    }

    return getSharing().getUserGroups().values();
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return attributeValues.get(idScheme.getAttribute());
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
  // hashCode, equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof IdentifiableObject identifiableObject)) return false;
    return getRealClass(this) == getRealClass(obj) && typedEquals(identifiableObject);
  }

  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }
}
