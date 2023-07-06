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
package org.hisp.dhis.dxf2.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionGroupSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorGroup;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.hisp.dhis.visualization.Visualization;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "metadata", namespace = DxfNamespaces.DXF_2_0)
public class Metadata {
  private Date created;

  private final java.util.Map<Class<?>, List<?>> values = new HashMap<>();

  @SuppressWarnings("unchecked")
  public final <T> List<T> getValues(Class<T> type) {
    List<?> objects = values.get(type);
    return objects == null ? Collections.emptyList() : (List<T>) objects;
  }

  private <T> void setValues(Class<T> type, List<T> list) {
    if (list == null) {
      values.remove(type);
    } else {
      values.put(type, list);
    }
  }

  public Metadata() {}

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "schemas", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "schema", namespace = DxfNamespaces.DXF_2_0)
  public List<Schema> getSchemas() {
    return getValues(Schema.class);
  }

  public void setSchemas(List<Schema> schemas) {
    setValues(Schema.class, schemas);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "metadataVersions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "metadataVersion", namespace = DxfNamespaces.DXF_2_0)
  public List<MetadataVersion> getMetadataVersions() {
    return getValues(MetadataVersion.class);
  }

  public void setMetadataVersions(List<MetadataVersion> metadataVersions) {
    setValues(MetadataVersion.class, metadataVersions);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "attributes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "attribute", namespace = DxfNamespaces.DXF_2_0)
  public List<Attribute> getAttributes() {
    return getValues(Attribute.class);
  }

  public void setAttributes(List<Attribute> attributes) {
    setValues(Attribute.class, attributes);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataApprovalLevels", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataApprovalLevel", namespace = DxfNamespaces.DXF_2_0)
  public List<DataApprovalLevel> getDataApprovalLevels() {
    return getValues(DataApprovalLevel.class);
  }

  public void setDataApprovalLevels(List<DataApprovalLevel> dataApprovalLevels) {
    setValues(DataApprovalLevel.class, dataApprovalLevels);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataApprovalWorkflows", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataApprovalWorkflow", namespace = DxfNamespaces.DXF_2_0)
  public List<DataApprovalWorkflow> getDataApprovalWorkflows() {
    return getValues(DataApprovalWorkflow.class);
  }

  public void setDataApprovalWorkflows(List<DataApprovalWorkflow> dataApprovalWorkflows) {
    setValues(DataApprovalWorkflow.class, dataApprovalWorkflows);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "aggregateDataExchanges", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "aggregateDataExchange", namespace = DxfNamespaces.DXF_2_0)
  public List<AggregateDataExchange> getAggregateDataExchanges() {
    return getValues(AggregateDataExchange.class);
  }

  public void setAggregateDataExchanges(List<AggregateDataExchange> aggregateDataExchanges) {
    setValues(AggregateDataExchange.class, aggregateDataExchanges);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "users", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "user", namespace = DxfNamespaces.DXF_2_0)
  public List<User> getUsers() {
    return getValues(User.class);
  }

  public void setUsers(List<User> users) {
    setValues(User.class, users);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "userRoles", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "userRole", namespace = DxfNamespaces.DXF_2_0)
  public List<UserRole> getUserRoles() {
    return getValues(UserRole.class);
  }

  public void setUserRoles(List<UserRole> userRoles) {
    setValues(UserRole.class, userRoles);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "userGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "userGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<UserGroup> getUserGroups() {
    return getValues(UserGroup.class);
  }

  public void setUserGroups(List<UserGroup> userGroups) {
    setValues(UserGroup.class, userGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "interpretations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "interpretation", namespace = DxfNamespaces.DXF_2_0)
  public List<Interpretation> getInterpretations() {
    return getValues(Interpretation.class);
  }

  public void setInterpretations(List<Interpretation> interpretations) {
    setValues(Interpretation.class, interpretations);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataElements", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElement", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElement> getDataElements() {
    return getValues(DataElement.class);
  }

  public void setDataElements(List<DataElement> dataElements) {
    setValues(DataElement.class, dataElements);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "options", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "option", namespace = DxfNamespaces.DXF_2_0)
  public List<Option> getOptions() {
    return getValues(Option.class);
  }

  public void setOptions(List<Option> options) {
    setValues(Option.class, options);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "optionSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "optionSet", namespace = DxfNamespaces.DXF_2_0)
  public List<OptionSet> getOptionSets() {
    return getValues(OptionSet.class);
  }

  public void setOptionSets(List<OptionSet> optionSets) {
    setValues(OptionSet.class, optionSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "optionGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "optionGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<OptionGroup> getOptionGroups() {
    return getValues(OptionGroup.class);
  }

  public void setOptionGroups(List<OptionGroup> optionGroups) {
    setValues(OptionGroup.class, optionGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "optionGroupSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "optionGroupSet", namespace = DxfNamespaces.DXF_2_0)
  public List<OptionGroupSet> getOptionGroupSets() {
    return getValues(OptionGroupSet.class);
  }

  public void setOptionGroupSets(List<OptionGroupSet> optionGroupSets) {
    setValues(OptionGroupSet.class, optionGroupSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElementGroup> getDataElementGroups() {
    return getValues(DataElementGroup.class);
  }

  public void setDataElementGroups(List<DataElementGroup> dataElementGroups) {
    setValues(DataElementGroup.class, dataElementGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataElementGroupSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElementGroupSet", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElementGroupSet> getDataElementGroupSets() {
    return getValues(DataElementGroupSet.class);
  }

  public void setDataElementGroupSets(List<DataElementGroupSet> dataElementGroupSets) {
    setValues(DataElementGroupSet.class, dataElementGroupSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "categories", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "category", namespace = DxfNamespaces.DXF_2_0)
  public List<Category> getCategories() {
    return getValues(Category.class);
  }

  public void setCategories(List<Category> categories) {
    setValues(Category.class, categories);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOption> getCategoryOptions() {
    return getValues(CategoryOption.class);
  }

  public void setCategoryOptions(List<CategoryOption> categoryOptions) {
    setValues(CategoryOption.class, categoryOptions);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "categoryCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryCombo> getCategoryCombos() {
    return getValues(CategoryCombo.class);
  }

  public void setCategoryCombos(List<CategoryCombo> categoryCombos) {
    setValues(CategoryCombo.class, categoryCombos);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOptionCombo> getCategoryOptionCombos() {
    return getValues(CategoryOptionCombo.class);
  }

  public void setCategoryOptionCombos(List<CategoryOptionCombo> categoryOptionCombos) {
    setValues(CategoryOptionCombo.class, categoryOptionCombos);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "categoryOptionGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOptionGroup> getCategoryOptionGroups() {
    return getValues(CategoryOptionGroup.class);
  }

  public void setCategoryOptionGroups(List<CategoryOptionGroup> categoryOptionGroups) {
    setValues(CategoryOptionGroup.class, categoryOptionGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "categoryOptionGroupSets",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "categoryOptionGroupSet", namespace = DxfNamespaces.DXF_2_0)
  public List<CategoryOptionGroupSet> getCategoryOptionGroupSets() {
    return getValues(CategoryOptionGroupSet.class);
  }

  public void setCategoryOptionGroupSets(List<CategoryOptionGroupSet> categoryOptionGroupSets) {
    setValues(CategoryOptionGroupSet.class, categoryOptionGroupSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataElementOperands", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataElementOperand", namespace = DxfNamespaces.DXF_2_0)
  public List<DataElementOperand> getDataElementOperands() {
    return getValues(DataElementOperand.class);
  }

  public void setDataElementOperands(List<DataElementOperand> dataElementOperands) {
    setValues(DataElementOperand.class, dataElementOperands);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "indicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicator", namespace = DxfNamespaces.DXF_2_0)
  public List<Indicator> getIndicators() {
    return getValues(Indicator.class);
  }

  public void setIndicators(List<Indicator> indicators) {
    setValues(Indicator.class, indicators);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "indicatorGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicatorGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<IndicatorGroup> getIndicatorGroups() {
    return getValues(IndicatorGroup.class);
  }

  public void setIndicatorGroups(List<IndicatorGroup> indicatorGroups) {
    setValues(IndicatorGroup.class, indicatorGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "indicatorGroupSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicatorGroupSet", namespace = DxfNamespaces.DXF_2_0)
  public List<IndicatorGroupSet> getIndicatorGroupSets() {
    return getValues(IndicatorGroupSet.class);
  }

  public void setIndicatorGroupSets(List<IndicatorGroupSet> indicatorGroupSets) {
    setValues(IndicatorGroupSet.class, indicatorGroupSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "indicatorTypes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "indicatorType", namespace = DxfNamespaces.DXF_2_0)
  public List<IndicatorType> getIndicatorTypes() {
    return getValues(IndicatorType.class);
  }

  public void setIndicatorTypes(List<IndicatorType> indicatorTypes) {
    setValues(IndicatorType.class, indicatorTypes);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<NameableObject> getItems() {
    return getValues(NameableObject.class);
  }

  public void setItems(List<NameableObject> items) {
    setValues(NameableObject.class, items);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public List<OrganisationUnit> getOrganisationUnits() {
    return getValues(OrganisationUnit.class);
  }

  public void setOrganisationUnits(List<OrganisationUnit> organisationUnits) {
    setValues(OrganisationUnit.class, organisationUnits);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "organisationUnitGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnitGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<OrganisationUnitGroup> getOrganisationUnitGroups() {
    return getValues(OrganisationUnitGroup.class);
  }

  public void setOrganisationUnitGroups(List<OrganisationUnitGroup> organisationUnitGroups) {
    setValues(OrganisationUnitGroup.class, organisationUnitGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "organisationUnitGroupSets",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnitGroupSet", namespace = DxfNamespaces.DXF_2_0)
  public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSets() {
    return getValues(OrganisationUnitGroupSet.class);
  }

  public void setOrganisationUnitGroupSets(
      List<OrganisationUnitGroupSet> organisationUnitGroupSets) {
    setValues(OrganisationUnitGroupSet.class, organisationUnitGroupSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "organisationUnitLevels", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnitLevel", namespace = DxfNamespaces.DXF_2_0)
  public List<OrganisationUnitLevel> getOrganisationUnitLevels() {
    return getValues(OrganisationUnitLevel.class);
  }

  public void setOrganisationUnitLevels(List<OrganisationUnitLevel> organisationUnitLevels) {
    setValues(OrganisationUnitLevel.class, organisationUnitLevels);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataEntryForms", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0)
  public List<DataEntryForm> getDataEntryForms() {
    return getValues(DataEntryForm.class);
  }

  public void setDataEntryForms(List<DataEntryForm> dataEntryForms) {
    setValues(DataEntryForm.class, dataEntryForms);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "sections", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "section", namespace = DxfNamespaces.DXF_2_0)
  public List<Section> getSections() {
    return getValues(Section.class);
  }

  public void setSections(List<Section> sections) {
    setValues(Section.class, sections);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dataSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dataSet", namespace = DxfNamespaces.DXF_2_0)
  public List<DataSet> getDataSets() {
    return getValues(DataSet.class);
  }

  public void setDataSets(List<DataSet> dataSets) {
    setValues(DataSet.class, dataSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "validationRules", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "validationRule", namespace = DxfNamespaces.DXF_2_0)
  public List<ValidationRule> getValidationRules() {
    return getValues(ValidationRule.class);
  }

  public void setValidationRules(List<ValidationRule> validationRules) {
    setValues(ValidationRule.class, validationRules);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "validationRuleGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "validationRuleGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<ValidationRuleGroup> getValidationRuleGroups() {
    return getValues(ValidationRuleGroup.class);
  }

  public void setValidationRuleGroups(List<ValidationRuleGroup> validationRuleGroups) {
    setValues(ValidationRuleGroup.class, validationRuleGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "sqlViews", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "sqlView", namespace = DxfNamespaces.DXF_2_0)
  public List<SqlView> getSqlViews() {
    return getValues(SqlView.class);
  }

  public void setSqlViews(List<SqlView> sqlViews) {
    setValues(SqlView.class, sqlViews);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "reports", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "report", namespace = DxfNamespaces.DXF_2_0)
  public List<Report> getReports() {
    return getValues(Report.class);
  }

  public void setReports(List<Report> reports) {
    setValues(Report.class, reports);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "documents", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "document", namespace = DxfNamespaces.DXF_2_0)
  public List<Document> getDocuments() {
    return getValues(Document.class);
  }

  public void setDocuments(List<Document> documents) {
    setValues(Document.class, documents);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "constants", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "constant", namespace = DxfNamespaces.DXF_2_0)
  public List<Constant> getConstants() {
    return getValues(Constant.class);
  }

  public void setConstants(List<Constant> constants) {
    setValues(Constant.class, constants);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dashboardItems", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dashboardItem", namespace = DxfNamespaces.DXF_2_0)
  public List<DashboardItem> getDashboardItems() {
    return getValues(DashboardItem.class);
  }

  public void setDashboardItems(List<DashboardItem> dashboardItems) {
    setValues(DashboardItem.class, dashboardItems);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dashboards", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dashboard", namespace = DxfNamespaces.DXF_2_0)
  public List<Dashboard> getDashboards() {
    return getValues(Dashboard.class);
  }

  public void setDashboards(List<Dashboard> dashboards) {
    setValues(Dashboard.class, dashboards);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "maps", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "map", namespace = DxfNamespaces.DXF_2_0)
  public List<Map> getMaps() {
    return getValues(Map.class);
  }

  public void setMaps(List<Map> maps) {
    setValues(Map.class, maps);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "mapViews", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "mapView", namespace = DxfNamespaces.DXF_2_0)
  public List<MapView> getMapViews() {
    return getValues(MapView.class);
  }

  public void setMapViews(List<MapView> mapViews) {
    setValues(MapView.class, mapViews);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "legendSets", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "legendSet", namespace = DxfNamespaces.DXF_2_0)
  public List<LegendSet> getLegendSets() {
    return getValues(LegendSet.class);
  }

  public void setLegendSets(List<LegendSet> legendSets) {
    setValues(LegendSet.class, legendSets);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "externalMapLayers", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "externalMapLayer", namespace = DxfNamespaces.DXF_2_0)
  public List<ExternalMapLayer> getExternalMapLayers() {
    return getValues(ExternalMapLayer.class);
  }

  public void setExternalMapLayers(List<ExternalMapLayer> externalMapLayers) {
    setValues(ExternalMapLayer.class, externalMapLayers);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programs", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "program", namespace = DxfNamespaces.DXF_2_0)
  public List<Program> getPrograms() {
    return getValues(Program.class);
  }

  public void setPrograms(List<Program> programs) {
    setValues(Program.class, programs);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programStages", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programStage", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramStage> getProgramStages() {
    return getValues(ProgramStage.class);
  }

  public void setProgramStages(List<ProgramStage> programStages) {
    setValues(ProgramStage.class, programStages);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramIndicator> getProgramIndicators() {
    return getValues(ProgramIndicator.class);
  }

  public void setProgramIndicators(List<ProgramIndicator> programIndicators) {
    setValues(ProgramIndicator.class, programIndicators);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programStageSections", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programStageSection", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramStageSection> getProgramStageSections() {
    return getValues(ProgramStageSection.class);
  }

  public void setProgramStageSections(List<ProgramStageSection> programStageSections) {
    setValues(ProgramStageSection.class, programStageSections);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "relationshipTypes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "relationshipType", namespace = DxfNamespaces.DXF_2_0)
  public List<RelationshipType> getRelationshipTypes() {
    return getValues(RelationshipType.class);
  }

  public void setRelationshipTypes(List<RelationshipType> relationshipTypes) {
    setValues(RelationshipType.class, relationshipTypes);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programRules", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programRule", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramRule> getProgramRules() {
    return getValues(ProgramRule.class);
  }

  public void setProgramRules(List<ProgramRule> programRules) {
    setValues(ProgramRule.class, programRules);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programRuleActions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programRuleAction", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramRuleAction> getProgramRuleActions() {
    return getValues(ProgramRuleAction.class);
  }

  public void setProgramRuleActions(List<ProgramRuleAction> programRuleActions) {
    setValues(ProgramRuleAction.class, programRuleActions);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "programRuleVariables", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programRuleVariable", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramRuleVariable> getProgramRuleVariables() {
    return getValues(ProgramRuleVariable.class);
  }

  public void setProgramRuleVariables(List<ProgramRuleVariable> programRuleVariables) {
    setValues(ProgramRuleVariable.class, programRuleVariables);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "events", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "event", namespace = DxfNamespaces.DXF_2_0)
  public List<Event> getEvents() {
    return getValues(Event.class);
  }

  public void setEvents(List<Event> events) {
    setValues(Event.class, events);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "eventReports", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "eventReport", namespace = DxfNamespaces.DXF_2_0)
  public List<EventReport> getEventReports() {
    return getValues(EventReport.class);
  }

  public void setEventReports(List<EventReport> eventReports) {
    setValues(EventReport.class, eventReports);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "eventCharts", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "eventChart", namespace = DxfNamespaces.DXF_2_0)
  public List<EventChart> getEventCharts() {
    return getValues(EventChart.class);
  }

  public void setEventCharts(List<EventChart> eventCharts) {
    setValues(EventChart.class, eventCharts);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "eventVisualizations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "eventVisualization", namespace = DxfNamespaces.DXF_2_0)
  public List<EventVisualization> getEventVisualizations() {
    return getValues(EventVisualization.class);
  }

  public void setEventVisualizations(List<EventVisualization> eventVisualizations) {
    setValues(EventVisualization.class, eventVisualizations);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "visualizations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "visualization", namespace = DxfNamespaces.DXF_2_0)
  public List<Visualization> getVisualizations() {
    return getValues(Visualization.class);
  }

  public void setVisualizations(List<Visualization> visualizations) {
    setValues(Visualization.class, visualizations);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "trackedEntityTypes", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  public List<TrackedEntityType> getTrackedEntityTypes() {
    return getValues(TrackedEntityType.class);
  }

  public void setTrackedEntityTypes(List<TrackedEntityType> trackedEntityTypes) {
    setValues(TrackedEntityType.class, trackedEntityTypes);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "trackedEntityAttributes",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0)
  public List<TrackedEntityAttribute> getTrackedEntityAttributes() {
    return getValues(TrackedEntityAttribute.class);
  }

  public void setTrackedEntityAttributes(List<TrackedEntityAttribute> trackedEntityAttributes) {
    setValues(TrackedEntityAttribute.class, trackedEntityAttributes);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "dimensions", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dimension", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalObject> getDimensions() {
    return getValues(DimensionalObject.class);
  }

  public void setDimensions(List<DimensionalObject> dimensions) {
    setValues(DimensionalObject.class, dimensions);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "predictors", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "predictor", namespace = DxfNamespaces.DXF_2_0)
  public List<Predictor> getPredictors() {
    return getValues(Predictor.class);
  }

  public void setPredictors(List<Predictor> predictors) {
    setValues(Predictor.class, predictors);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "predictorGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "predictorGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<PredictorGroup> getPredictorGroups() {
    return getValues(PredictorGroup.class);
  }

  public void setPredictorGroups(List<PredictorGroup> predictorGroups) {
    setValues(PredictorGroup.class, predictorGroups);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "programNotificationTemplates",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programNotificationTemplate", namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramNotificationTemplate> getProgramNotificationTemplates() {
    return getValues(ProgramNotificationTemplate.class);
  }

  public void setProgramNotificationTemplates(
      List<ProgramNotificationTemplate> programNotificationTemplates) {
    setValues(ProgramNotificationTemplate.class, programNotificationTemplates);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "analyticsTableHooks", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "analyticsTableHook", namespace = DxfNamespaces.DXF_2_0)
  public List<AnalyticsTableHook> getAnalyticsTableHooks() {
    return getValues(AnalyticsTableHook.class);
  }

  public void setAnalyticsTableHooks(List<AnalyticsTableHook> analyticsTableHooks) {
    setValues(AnalyticsTableHook.class, analyticsTableHooks);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(
      localName = "validationNotificationTemplates",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(
      localName = "validationNotificationTemplate",
      namespace = DxfNamespaces.DXF_2_0)
  public List<ValidationNotificationTemplate> getValidationNotificationTemplates() {
    return getValues(ValidationNotificationTemplate.class);
  }

  public void setValidationNotificationTemplates(
      List<ValidationNotificationTemplate> validationNotificationTemplates) {
    setValues(ValidationNotificationTemplate.class, validationNotificationTemplates);
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "jobConfigurations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0)
  public List<JobConfiguration> getJobConfigurations() {
    return getValues(JobConfiguration.class);
  }

  public void setJobConfigurations(List<JobConfiguration> jobConfigurations) {
    setValues(JobConfiguration.class, jobConfigurations);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("MetaData{");
    str.append("created=").append(created);
    for (Entry<Class<?>, List<?>> e : values.entrySet()) {
      String key = e.getKey().getSimpleName();
      str.append(Character.toLowerCase(key.charAt(0)) + key.substring(1))
          .append(e.getValue().toString());
    }
    str.append("}");
    return str.toString();
  }
}
