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
package org.hisp.dhis.attribute;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;
import static org.hisp.dhis.schema.annotation.Property.Value.TRUE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.visualization.Visualization;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Entity
@Table(name = "attribute")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "attribute", namespace = DxfNamespaces.DXF_2_0)
public class Attribute extends BaseMetadataObject implements NameableObject, MetadataObject {
  /** The types of {@link IdentifiableObject}s that can have attributes */
  public enum ObjectType {
    DATA_ELEMENT(DataElement.class),

    DATA_ELEMENT_GROUP(DataElementGroup.class),

    INDICATOR(Indicator.class),

    INDICATOR_GROUP(IndicatorGroup.class),

    DATA_SET(DataSet.class),

    ORGANISATION_UNIT(OrganisationUnit.class),

    ORGANISATION_UNIT_GROUP(OrganisationUnitGroup.class),

    ORGANISATION_UNIT_GROUP_SET(OrganisationUnitGroupSet.class),

    USER(User.class),

    USER_GROUP(UserGroup.class),

    PROGRAM(Program.class),

    PROGRAM_STAGE(ProgramStage.class),

    TRACKED_ENTITY_TYPE(TrackedEntityType.class),

    TRACKED_ENTITY_ATTRIBUTE(TrackedEntityAttribute.class),

    CATEGORY_OPTION(CategoryOption.class),

    CATEGORY_OPTION_GROUP(CategoryOptionGroup.class),

    DOCUMENT(Document.class),

    OPTION(Option.class),

    OPTION_SET(OptionSet.class),

    CONSTANT(Constant.class),

    LEGEND_SET(LegendSet.class),

    PROGRAM_INDICATOR(ProgramIndicator.class),

    SQL_VIEW(SqlView.class),

    SECTION(Section.class),

    CATEGORY_OPTION_COMBO(CategoryOptionCombo.class),

    CATEGORY_OPTION_GROUP_SET(CategoryOptionGroupSet.class),

    DATA_ELEMENT_GROUP_SET(DataElementGroupSet.class),

    VALIDATION_RULE(ValidationRule.class),

    VALIDATION_RULE_GROUP(ValidationRuleGroup.class),

    CATEGORY(Category.class),

    VISUALIZATION(Visualization.class),

    MAP(Map.class),

    EVENT_REPORT(EventReport.class),

    EVENT_CHART(EventChart.class),

    RELATIONSHIP_TYPE(RelationshipType.class);

    final Class<? extends IdentifiableObject> type;

    ObjectType(Class<? extends IdentifiableObject> type) {
      this.type = type;
    }

    public Class<? extends IdentifiableObject> getType() {
      return type;
    }

    public static ObjectType valueOf(Class<?> type) {
      return stream(values()).filter(t -> t.type == type).findFirst().orElse(null);
    }

    public static boolean isValidType(String type) {
      return stream(values()).anyMatch(t -> t.getPropertyName().equals(type));
    }

    public String getPropertyName() {
      return CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL).convert(name())
          + "Attribute";
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "attributeid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  private String name;

  @Column(name = "shortname", length = 50)
  private String shortName;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "valuetype", nullable = false, length = 50)
  private ValueType valueType;

  /** Mapped through {@link #getObjectTypes()}, which stores the enum names as a jsonb set. */
  @Transient private EnumSet<ObjectType> objectTypes = EnumSet.noneOf(ObjectType.class);

  @Column(name = "mandatory", nullable = false)
  private boolean mandatory;

  @Column(name = "isunique")
  private boolean unique;

  @Column(name = "sortorder")
  private Integer sortOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "optionsetid")
  private OptionSet optionSet;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  public Attribute() {}

  public Attribute(String uid) {
    this.uid = uid;
  }

  public Attribute(String name, ValueType valueType) {
    this.name = name;
    this.valueType = valueType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getUid(),
        getCode(),
        getName(),
        getShortName(),
        getDescription(),
        getValueType(),
        getObjectTypes(),
        isMandatory(),
        isUnique(),
        getOptionSet());
  }

  /**
   * Compares uid, code, name, shortName and description like {@link BaseNameableObject}, plus
   * valueType, objectTypes, mandatory, unique and optionSet. Uses get-methods and the real class to
   * handle proxied objects.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Attribute other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName())
            && Objects.equals(getShortName(), other.getShortName())
            && Objects.equals(getDescription(), other.getDescription())
            && getValueType() == other.getValueType()
            && Objects.equals(getObjectTypes(), other.getObjectTypes())
            && isMandatory() == other.isMandatory()
            && isUnique() == other.isUnique()
            && Objects.equals(getOptionSet(), other.getOptionSet());
  }

  @JsonIgnore
  public boolean isAttribute(ObjectType type) {
    return objectTypes.contains(type);
  }

  private void setAttribute(ObjectType type, boolean isAttribute) {
    if (isAttribute) {
      objectTypes.add(type);
    } else {
      objectTypes.remove(type);
    }
  }

  @Access(AccessType.PROPERTY)
  @Type(type = "jbSet")
  @Column(name = "objecttypes", nullable = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Property(access = Property.Access.READ_ONLY, required = FALSE)
  public Set<String> getObjectTypes() {
    return objectTypes.stream().map(ObjectType::name).collect(toSet());
  }

  public void setObjectTypes(Set<String> objectTypes) {
    this.objectTypes =
        objectTypes.stream()
            .map(ObjectType::valueOf)
            .collect(toCollection(() -> EnumSet.noneOf(ObjectType.class)));
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return valueType;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = TRUE, owner = TRUE)
  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isDataElementAttribute() {
    return isAttribute(ObjectType.DATA_ELEMENT);
  }

  public void setDataElementAttribute(boolean dataElementAttribute) {
    setAttribute(ObjectType.DATA_ELEMENT, dataElementAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isDataElementGroupAttribute() {
    return isAttribute(ObjectType.DATA_ELEMENT_GROUP);
  }

  public void setDataElementGroupAttribute(boolean dataElementGroupAttribute) {
    setAttribute(ObjectType.DATA_ELEMENT_GROUP, dataElementGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isIndicatorAttribute() {
    return isAttribute(ObjectType.INDICATOR);
  }

  public void setIndicatorAttribute(boolean indicatorAttribute) {
    setAttribute(ObjectType.INDICATOR, indicatorAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isIndicatorGroupAttribute() {
    return isAttribute(ObjectType.INDICATOR_GROUP);
  }

  public void setIndicatorGroupAttribute(boolean indicatorGroupAttribute) {
    setAttribute(ObjectType.INDICATOR_GROUP, indicatorGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isDataSetAttribute() {
    return isAttribute(ObjectType.DATA_SET);
  }

  public void setDataSetAttribute(boolean dataSetAttribute) {
    setAttribute(ObjectType.DATA_SET, dataSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isOrganisationUnitAttribute() {
    return isAttribute(ObjectType.ORGANISATION_UNIT);
  }

  public void setOrganisationUnitAttribute(boolean organisationUnitAttribute) {
    setAttribute(ObjectType.ORGANISATION_UNIT, organisationUnitAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isOrganisationUnitGroupAttribute() {
    return isAttribute(ObjectType.ORGANISATION_UNIT_GROUP);
  }

  public void setOrganisationUnitGroupAttribute(boolean organisationUnitGroupAttribute) {
    setAttribute(ObjectType.ORGANISATION_UNIT_GROUP, organisationUnitGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isOrganisationUnitGroupSetAttribute() {
    return isAttribute(ObjectType.ORGANISATION_UNIT_GROUP_SET);
  }

  public void setOrganisationUnitGroupSetAttribute(boolean organisationUnitGroupSetAttribute) {
    setAttribute(ObjectType.ORGANISATION_UNIT_GROUP_SET, organisationUnitGroupSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isUserAttribute() {
    return isAttribute(ObjectType.USER);
  }

  public void setUserAttribute(boolean userAttribute) {
    setAttribute(ObjectType.USER, userAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isUserGroupAttribute() {
    return isAttribute(ObjectType.USER_GROUP);
  }

  public void setUserGroupAttribute(boolean userGroupAttribute) {
    setAttribute(ObjectType.USER_GROUP, userGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isProgramAttribute() {
    return isAttribute(ObjectType.PROGRAM);
  }

  public void setProgramAttribute(boolean programAttribute) {
    setAttribute(ObjectType.PROGRAM, programAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isProgramStageAttribute() {
    return isAttribute(ObjectType.PROGRAM_STAGE);
  }

  public void setProgramStageAttribute(boolean programStageAttribute) {
    setAttribute(ObjectType.PROGRAM_STAGE, programStageAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isTrackedEntityTypeAttribute() {
    return isAttribute(ObjectType.TRACKED_ENTITY_TYPE);
  }

  public void setTrackedEntityTypeAttribute(boolean trackedEntityTypeAttribute) {
    setAttribute(ObjectType.TRACKED_ENTITY_TYPE, trackedEntityTypeAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isTrackedEntityAttributeAttribute() {
    return isAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE);
  }

  public void setTrackedEntityAttributeAttribute(boolean trackedEntityAttributeAttribute) {
    setAttribute(ObjectType.TRACKED_ENTITY_ATTRIBUTE, trackedEntityAttributeAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isCategoryOptionAttribute() {
    return isAttribute(ObjectType.CATEGORY_OPTION);
  }

  public void setCategoryOptionAttribute(boolean categoryOptionAttribute) {
    setAttribute(ObjectType.CATEGORY_OPTION, categoryOptionAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isCategoryOptionGroupAttribute() {
    return isAttribute(ObjectType.CATEGORY_OPTION_GROUP);
  }

  public void setCategoryOptionGroupAttribute(boolean categoryOptionGroupAttribute) {
    setAttribute(ObjectType.CATEGORY_OPTION_GROUP, categoryOptionGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isDocumentAttribute() {
    return isAttribute(ObjectType.DOCUMENT);
  }

  public void setDocumentAttribute(boolean documentAttribute) {
    setAttribute(ObjectType.DOCUMENT, documentAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isOptionAttribute() {
    return isAttribute(ObjectType.OPTION);
  }

  public void setOptionAttribute(boolean optionAttribute) {
    setAttribute(ObjectType.OPTION, optionAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isOptionSetAttribute() {
    return isAttribute(ObjectType.OPTION_SET);
  }

  public void setOptionSetAttribute(boolean optionSetAttribute) {
    setAttribute(ObjectType.OPTION_SET, optionSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isLegendSetAttribute() {
    return isAttribute(ObjectType.LEGEND_SET);
  }

  public void setLegendSetAttribute(boolean legendSetAttribute) {
    setAttribute(ObjectType.LEGEND_SET, legendSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isConstantAttribute() {
    return isAttribute(ObjectType.CONSTANT);
  }

  public void setConstantAttribute(boolean constantAttribute) {
    setAttribute(ObjectType.CONSTANT, constantAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isProgramIndicatorAttribute() {
    return isAttribute(ObjectType.PROGRAM_INDICATOR);
  }

  public void setProgramIndicatorAttribute(boolean programIndicatorAttribute) {
    setAttribute(ObjectType.PROGRAM_INDICATOR, programIndicatorAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isSqlViewAttribute() {
    return isAttribute(ObjectType.SQL_VIEW);
  }

  public void setSqlViewAttribute(boolean sqlViewAttribute) {
    setAttribute(ObjectType.SQL_VIEW, sqlViewAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isCategoryOptionComboAttribute() {
    return isAttribute(ObjectType.CATEGORY_OPTION_COMBO);
  }

  public void setCategoryOptionComboAttribute(boolean categoryOptionComboAttribute) {
    setAttribute(ObjectType.CATEGORY_OPTION_COMBO, categoryOptionComboAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isSectionAttribute() {
    return isAttribute(ObjectType.SECTION);
  }

  public void setSectionAttribute(boolean sectionAttribute) {
    setAttribute(ObjectType.SECTION, sectionAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getOptionSet() {
    return optionSet;
  }

  public void setOptionSet(OptionSet optionSet) {
    this.optionSet = optionSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isCategoryOptionGroupSetAttribute() {
    return isAttribute(ObjectType.CATEGORY_OPTION_GROUP_SET);
  }

  public void setCategoryOptionGroupSetAttribute(boolean categoryOptionGroupSetAttribute) {
    setAttribute(ObjectType.CATEGORY_OPTION_GROUP_SET, categoryOptionGroupSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isDataElementGroupSetAttribute() {
    return isAttribute(ObjectType.DATA_ELEMENT_GROUP_SET);
  }

  public void setDataElementGroupSetAttribute(boolean dataElementGroupSetAttribute) {
    setAttribute(ObjectType.DATA_ELEMENT_GROUP_SET, dataElementGroupSetAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isValidationRuleAttribute() {
    return isAttribute(ObjectType.VALIDATION_RULE);
  }

  public void setValidationRuleAttribute(boolean validationRuleAttribute) {
    setAttribute(ObjectType.VALIDATION_RULE, validationRuleAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isValidationRuleGroupAttribute() {
    return isAttribute(ObjectType.VALIDATION_RULE_GROUP);
  }

  public void setValidationRuleGroupAttribute(boolean validationRuleGroupAttribute) {
    setAttribute(ObjectType.VALIDATION_RULE_GROUP, validationRuleGroupAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isCategoryAttribute() {
    return isAttribute(ObjectType.CATEGORY);
  }

  public void setCategoryAttribute(boolean categoryAttribute) {
    setAttribute(ObjectType.CATEGORY, categoryAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isVisualizationAttribute() {
    return isAttribute(ObjectType.VISUALIZATION);
  }

  public void setVisualizationAttribute(boolean visualizationAttribute) {
    setAttribute(ObjectType.VISUALIZATION, visualizationAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isMapAttribute() {
    return isAttribute(ObjectType.MAP);
  }

  public void setMapAttribute(boolean mapAttribute) {
    setAttribute(ObjectType.MAP, mapAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isEventReportAttribute() {
    return isAttribute(ObjectType.EVENT_REPORT);
  }

  public void setEventReportAttribute(boolean eventReportAttribute) {
    setAttribute(ObjectType.EVENT_REPORT, eventReportAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isEventChartAttribute() {
    return isAttribute(ObjectType.EVENT_CHART);
  }

  public void setEventChartAttribute(boolean eventChartAttribute) {
    setAttribute(ObjectType.EVENT_CHART, eventChartAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = FALSE, persisted = TRUE, owner = TRUE)
  public boolean isRelationshipTypeAttribute() {
    return isAttribute(ObjectType.RELATIONSHIP_TYPE);
  }

  public void setRelationshipTypeAttribute(boolean relationshipTypeAttribute) {
    setAttribute(ObjectType.RELATIONSHIP_TYPE, relationshipTypeAttribute);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public List<Class<? extends IdentifiableObject>> getSupportedClasses() {
    return objectTypes.stream().map(ObjectType::getType).collect(toList());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sortOrder", sortOrder)
        .add("valueType", valueType)
        .add("objectTypes", objectTypes)
        .add("mandatory", mandatory)
        .toString();
  }

  // -------------------------------------------------------------------------
  // Identifiable and nameable object getters and setters
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
    return translations.getTranslation("NAME", getName());
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
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return translations.getTranslation("SHORT_NAME", getShortName());
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
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translations.getTranslation("DESCRIPTION", getDescription());
  }

  /** Falls back to the display name, as an attribute has no form name. */
  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return translations.getTranslation("FORM_NAME", getDisplayName());
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
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  /** Clears out the translation cache. */
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
  // Unsupported properties (no column in the attribute table)
  // -------------------------------------------------------------------------

  /** Attribute does not support form name. */
  @Deprecated(since = "2.44")
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return null;
  }

  /** Attribute does not support form name. */
  @Deprecated(since = "2.44")
  public void setFormName(String formName) {
    // Not supported
  }

  /** Attribute does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** Attribute does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** Attribute does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** Attribute does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
