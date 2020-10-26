package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.chart.Chart;
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
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
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
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "metadata", namespace = DxfNamespaces.DXF_2_0 )
public class Metadata
{
    private Date created;

    private List<Schema> schemas = new ArrayList<>();

    private List<MetadataVersion> metadataVersions = new ArrayList<>();

    private List<Attribute> attributes = new ArrayList<>();

    private List<DataApprovalLevel> dataApprovalLevels = new ArrayList<>();

    private List<DataApprovalWorkflow> dataApprovalWorkflows = new ArrayList<>();

    private List<Document> documents = new ArrayList<>();

    private List<Constant> constants = new ArrayList<>();

    private List<User> users = new ArrayList<>();

    private List<UserAuthorityGroup> userRoles = new ArrayList<>();

    private List<UserGroup> userGroups = new ArrayList<>();

    private List<Interpretation> interpretations = new ArrayList<>();

    private List<Option> options = new ArrayList<>();

    private List<OptionSet> optionSets = new ArrayList<>();

    private List<OptionGroup> optionGroups = new ArrayList<>();

    private List<OptionGroupSet> optionGroupSets = new ArrayList<>();

    private List<Category> categories = new ArrayList<>();

    private List<CategoryOption> categoryOptions = new ArrayList<>();

    private List<CategoryCombo> categoryCombos = new ArrayList<>();

    private List<CategoryOptionCombo> categoryOptionCombos = new ArrayList<>();

    private List<CategoryOptionGroup> categoryOptionGroups = new ArrayList<>();

    private List<CategoryOptionGroupSet> categoryOptionGroupSets = new ArrayList<>();

    private List<DataElementOperand> dataElementOperands = new ArrayList<>();

    private List<DashboardItem> dashboardItems = new ArrayList<>();

    private List<Dashboard> dashboards = new ArrayList<>();

    private List<DataElement> dataElements = new ArrayList<>();

    private List<DataElementGroup> dataElementGroups = new ArrayList<>();

    private List<DataElementGroupSet> dataElementGroupSets = new ArrayList<>();

    private List<DimensionalObject> dimensions = new ArrayList<>();

    private List<Indicator> indicators = new ArrayList<>();

    private List<IndicatorGroup> indicatorGroups = new ArrayList<>();

    private List<IndicatorGroupSet> indicatorGroupSets = new ArrayList<>();

    private List<IndicatorType> indicatorTypes = new ArrayList<>();

    private List<NameableObject> items = new ArrayList<>();

    private List<OrganisationUnit> organisationUnits = new ArrayList<>();

    private List<OrganisationUnitGroup> organisationUnitGroups = new ArrayList<>();

    private List<OrganisationUnitGroupSet> organisationUnitGroupSets = new ArrayList<>();

    private List<OrganisationUnitLevel> organisationUnitLevels = new ArrayList<>();

    private List<ValidationRule> validationRules = new ArrayList<>();

    private List<ValidationRuleGroup> validationRuleGroups = new ArrayList<>();

    private List<SqlView> sqlViews = new ArrayList<>();

    private List<Chart> charts = new ArrayList<>();

    private List<Report> reports = new ArrayList<>();

    private List<ReportTable> reportTables = new ArrayList<>();

    private List<Map> maps = new ArrayList<>();

    private List<MapView> mapViews = new ArrayList<>();

    private List<LegendSet> legendSets = new ArrayList<>();

    private List<ExternalMapLayer> externalMapLayers = new ArrayList<>();

    private List<DataEntryForm> dataEntryForms = new ArrayList<>();

    private List<Section> sections = new ArrayList<>();

    private List<DataSet> dataSets = new ArrayList<>();

    private List<Event> events = new ArrayList<>();

    private List<EventReport> eventReports = new ArrayList<>();

    private List<EventChart> eventCharts = new ArrayList<>();

    private List<Program> programs = new ArrayList<>();

    private List<ProgramStage> programStages = new ArrayList<>();

    private List<ProgramIndicator> programIndicators = new ArrayList<>();

    private List<ProgramStageSection> programStageSections = new ArrayList<>();

    private List<RelationshipType> relationshipTypes = new ArrayList<>();

    private List<ProgramRule> programRules = new ArrayList<>();

    private List<ProgramRuleAction> programRuleActions = new ArrayList<>();

    private List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

    private List<TrackedEntityType> trackedEntityTypes = new ArrayList<>();

    private List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

    private List<Predictor> predictors = new ArrayList<>();

    private List<PredictorGroup> predictorGroups = new ArrayList<>();

    private List<ProgramNotificationTemplate> programNotificationTemplates = new ArrayList<>();

    private List<AnalyticsTableHook> analyticsTableHooks = new ArrayList<>();

    private List<ValidationNotificationTemplate> validationNotificationTemplates = new ArrayList<>();

    private List<JobConfiguration> jobConfigurations = new ArrayList<>();

    public Metadata()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "schemas", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "schema", namespace = DxfNamespaces.DXF_2_0 )
    public List<Schema> getSchemas()
    {
        return schemas;
    }

    public void setSchemas( List<Schema> schemas )
    {
        this.schemas = schemas;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "metadataVersions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "metadataVersion", namespace = DxfNamespaces.DXF_2_0 )
    public List<MetadataVersion> getMetadataVersions()
    {
        return metadataVersions;
    }

    public void setMetadataVersions( List<MetadataVersion> metadataVersions )
    {
        this.metadataVersions = metadataVersions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "attributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attribute", namespace = DxfNamespaces.DXF_2_0 )
    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<Attribute> attributes )
    {
        this.attributes = attributes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataApprovalLevels", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataApprovalLevel", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataApprovalLevel> getDataApprovalLevels()
    {
        return dataApprovalLevels;
    }

    public void setDataApprovalLevels( List<DataApprovalLevel> dataApprovalLevels )
    {
        this.dataApprovalLevels = dataApprovalLevels;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataApprovalWorkflows", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataApprovalWorkflow", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataApprovalWorkflow> getDataApprovalWorkflows()
    {
        return dataApprovalWorkflows;
    }

    public void setDataApprovalWorkflows( List<DataApprovalWorkflow> dataApprovalWorkflows )
    {
        this.dataApprovalWorkflows = dataApprovalWorkflows;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "users", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "user", namespace = DxfNamespaces.DXF_2_0 )
    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers( List<User> users )
    {
        this.users = users;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userRoles", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userRole", namespace = DxfNamespaces.DXF_2_0 )
    public List<UserAuthorityGroup> getUserRoles()
    {
        return userRoles;
    }

    public void setUserRoles( List<UserAuthorityGroup> userRoles )
    {
        this.userRoles = userRoles;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<UserGroup> getUserGroups()
    {
        return userGroups;
    }

    public void setUserGroups( List<UserGroup> userGroups )
    {
        this.userGroups = userGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "interpretations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "interpretation", namespace = DxfNamespaces.DXF_2_0 )
    public List<Interpretation> getInterpretations()
    {
        return interpretations;
    }

    public void setInterpretations( List<Interpretation> interpretations )
    {
        this.interpretations = interpretations;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( List<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "options", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "option", namespace = DxfNamespaces.DXF_2_0 )
    public List<Option> getOptions()
    {
        return options;
    }

    public void setOptions( List<Option> options )
    {
        this.options = options;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "optionSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "optionSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<OptionSet> getOptionSets()
    {
        return optionSets;
    }

    public void setOptionSets( List<OptionSet> optionSets )
    {
        this.optionSets = optionSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "optionGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "optionGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<OptionGroup> getOptionGroups()
    {
        return optionGroups;
    }

    public void setOptionGroups( List<OptionGroup> optionGroups )
    {
        this.optionGroups = optionGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "optionGroupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "optionGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<OptionGroupSet> getOptionGroupSets()
    {
        return optionGroupSets;
    }

    public void setOptionGroupSets( List<OptionGroupSet> optionGroupSets )
    {
        this.optionGroupSets = optionGroupSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementGroup> getDataElementGroups()
    {
        return dataElementGroups;
    }

    public void setDataElementGroups( List<DataElementGroup> dataElementGroups )
    {
        this.dataElementGroups = dataElementGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElementGroupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementGroupSet> getDataElementGroupSets()
    {
        return dataElementGroupSets;
    }

    public void setDataElementGroupSets( List<DataElementGroupSet> dataElementGroupSets )
    {
        this.dataElementGroupSets = dataElementGroupSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categories", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "category", namespace = DxfNamespaces.DXF_2_0 )
    public List<Category> getCategories()
    {
        return categories;
    }

    public void setCategories( List<Category> categories )
    {
        this.categories = categories;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryOption> getCategoryOptions()
    {
        return categoryOptions;
    }

    public void setCategoryOptions( List<CategoryOption> categoryOptions )
    {
        this.categoryOptions = categoryOptions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryCombos", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryCombo> getCategoryCombos()
    {
        return categoryCombos;
    }

    public void setCategoryCombos( List<CategoryCombo> categoryCombos )
    {
        this.categoryCombos = categoryCombos;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryOptionCombo> getCategoryOptionCombos()
    {
        return categoryOptionCombos;
    }

    public void setCategoryOptionCombos( List<CategoryOptionCombo> categoryOptionCombos )
    {
        this.categoryOptionCombos = categoryOptionCombos;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryOptionGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryOptionGroup> getCategoryOptionGroups()
    {
        return categoryOptionGroups;
    }

    public void setCategoryOptionGroups( List<CategoryOptionGroup> categoryOptionGroups )
    {
        this.categoryOptionGroups = categoryOptionGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "categoryOptionGroupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<CategoryOptionGroupSet> getCategoryOptionGroupSets()
    {
        return categoryOptionGroupSets;
    }

    public void setCategoryOptionGroupSets( List<CategoryOptionGroupSet> categoryOptionGroupSets )
    {
        this.categoryOptionGroupSets = categoryOptionGroupSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataElementOperands", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementOperand", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementOperand> getDataElementOperands()
    {
        return dataElementOperands;
    }

    public void setDataElementOperands( List<DataElementOperand> dataElementOperands )
    {
        this.dataElementOperands = dataElementOperands;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "indicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicator", namespace = DxfNamespaces.DXF_2_0 )
    public List<Indicator> getIndicators()
    {
        return indicators;
    }

    public void setIndicators( List<Indicator> indicators )
    {
        this.indicators = indicators;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "indicatorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicatorGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<IndicatorGroup> getIndicatorGroups()
    {
        return indicatorGroups;
    }

    public void setIndicatorGroups( List<IndicatorGroup> indicatorGroups )
    {
        this.indicatorGroups = indicatorGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "indicatorGroupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicatorGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<IndicatorGroupSet> getIndicatorGroupSets()
    {
        return indicatorGroupSets;
    }

    public void setIndicatorGroupSets( List<IndicatorGroupSet> indicatorGroupSets )
    {
        this.indicatorGroupSets = indicatorGroupSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "indicatorTypes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicatorType", namespace = DxfNamespaces.DXF_2_0 )
    public List<IndicatorType> getIndicatorTypes()
    {
        return indicatorTypes;
    }

    public void setIndicatorTypes( List<IndicatorType> indicatorTypes )
    {
        this.indicatorTypes = indicatorTypes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "items", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "item", namespace = DxfNamespaces.DXF_2_0 )
    public List<NameableObject> getItems()
    {
        return items;
    }

    public void setItems( List<NameableObject> items )
    {
        this.items = items;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( List<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnitGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnitGroup> getOrganisationUnitGroups()
    {
        return organisationUnitGroups;
    }

    public void setOrganisationUnitGroups( List<OrganisationUnitGroup> organisationUnitGroups )
    {
        this.organisationUnitGroups = organisationUnitGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnitGroupSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnitGroupSet> getOrganisationUnitGroupSets()
    {
        return organisationUnitGroupSets;
    }

    public void setOrganisationUnitGroupSets( List<OrganisationUnitGroupSet> organisationUnitGroupSets )
    {
        this.organisationUnitGroupSets = organisationUnitGroupSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "organisationUnitLevels", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnitLevel", namespace = DxfNamespaces.DXF_2_0 )
    public List<OrganisationUnitLevel> getOrganisationUnitLevels()
    {
        return organisationUnitLevels;
    }

    public void setOrganisationUnitLevels( List<OrganisationUnitLevel> organisationUnitLevels )
    {
        this.organisationUnitLevels = organisationUnitLevels;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataEntryForms", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataEntryForm> getDataEntryForms()
    {
        return dataEntryForms;
    }

    public void setDataEntryForms( List<DataEntryForm> dataEntryForms )
    {
        this.dataEntryForms = dataEntryForms;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "sections", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "section", namespace = DxfNamespaces.DXF_2_0 )
    public List<Section> getSections()
    {
        return sections;
    }

    public void setSections( List<Section> sections )
    {
        this.sections = sections;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dataSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    public void setDataSets( List<DataSet> dataSets )
    {
        this.dataSets = dataSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "validationRules", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationRule", namespace = DxfNamespaces.DXF_2_0 )
    public List<ValidationRule> getValidationRules()
    {
        return validationRules;
    }

    public void setValidationRules( List<ValidationRule> validationRules )
    {
        this.validationRules = validationRules;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "validationRuleGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationRuleGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<ValidationRuleGroup> getValidationRuleGroups()
    {
        return validationRuleGroups;
    }

    public void setValidationRuleGroups( List<ValidationRuleGroup> validationRuleGroups )
    {
        this.validationRuleGroups = validationRuleGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "sqlViews", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "sqlView", namespace = DxfNamespaces.DXF_2_0 )
    public List<SqlView> getSqlViews()
    {
        return sqlViews;
    }

    public void setSqlViews( List<SqlView> sqlViews )
    {
        this.sqlViews = sqlViews;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "charts", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "chart", namespace = DxfNamespaces.DXF_2_0 )
    public List<Chart> getCharts()
    {
        return charts;
    }

    public void setCharts( List<Chart> charts )
    {
        this.charts = charts;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "reports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "report", namespace = DxfNamespaces.DXF_2_0 )
    public List<Report> getReports()
    {
        return reports;
    }

    public void setReports( List<Report> reports )
    {
        this.reports = reports;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "reportTables", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "reportTable", namespace = DxfNamespaces.DXF_2_0 )
    public List<ReportTable> getReportTables()
    {
        return reportTables;
    }

    public void setReportTables( List<ReportTable> reportTables )
    {
        this.reportTables = reportTables;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "documents", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "document", namespace = DxfNamespaces.DXF_2_0 )
    public List<Document> getDocuments()
    {
        return documents;
    }

    public void setDocuments( List<Document> documents )
    {
        this.documents = documents;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "constants", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "constant", namespace = DxfNamespaces.DXF_2_0 )
    public List<Constant> getConstants()
    {
        return constants;
    }

    public void setConstants( List<Constant> constants )
    {
        this.constants = constants;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dashboardItems", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dashboardItem", namespace = DxfNamespaces.DXF_2_0 )
    public List<DashboardItem> getDashboardItems()
    {
        return dashboardItems;
    }

    public void setDashboardItems( List<DashboardItem> dashboardItems )
    {
        this.dashboardItems = dashboardItems;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dashboards", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dashboard", namespace = DxfNamespaces.DXF_2_0 )
    public List<Dashboard> getDashboards()
    {
        return dashboards;
    }

    public void setDashboards( List<Dashboard> dashboards )
    {
        this.dashboards = dashboards;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "maps", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "map", namespace = DxfNamespaces.DXF_2_0 )
    public List<Map> getMaps()
    {
        return maps;
    }

    public void setMaps( List<Map> maps )
    {
        this.maps = maps;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "mapViews", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "mapView", namespace = DxfNamespaces.DXF_2_0 )
    public List<MapView> getMapViews()
    {
        return mapViews;
    }

    public void setMapViews( List<MapView> mapViews )
    {
        this.mapViews = mapViews;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "legendSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "legendSet", namespace = DxfNamespaces.DXF_2_0 )
    public List<LegendSet> getLegendSets()
    {
        return legendSets;
    }

    public void setLegendSets( List<LegendSet> legendSets )
    {
        this.legendSets = legendSets;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "externalMapLayers", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "externalMapLayer", namespace = DxfNamespaces.DXF_2_0 )
    public List<ExternalMapLayer> getExternalMapLayers()
    {
        return externalMapLayers;
    }

    public void setExternalMapLayers( List<ExternalMapLayer> externalMapLayers )
    {
        this.externalMapLayers = externalMapLayers;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programs", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "program", namespace = DxfNamespaces.DXF_2_0 )
    public List<Program> getPrograms()
    {
        return programs;
    }

    public void setPrograms( List<Program> programs )
    {
        this.programs = programs;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programStages", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramStage> getProgramStages()
    {
        return programStages;
    }

    public void setProgramStages( List<ProgramStage> programStages )
    {
        this.programStages = programStages;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramIndicator> getProgramIndicators()
    {
        return programIndicators;
    }

    public void setProgramIndicators( List<ProgramIndicator> programIndicators )
    {
        this.programIndicators = programIndicators;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programStageSections", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStageSection", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramStageSection> getProgramStageSections()
    {
        return programStageSections;
    }

    public void setProgramStageSections( List<ProgramStageSection> programStageSections )
    {
        this.programStageSections = programStageSections;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "relationshipTypes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "relationshipType", namespace = DxfNamespaces.DXF_2_0 )
    public List<RelationshipType> getRelationshipTypes()
    {
        return relationshipTypes;
    }

    public void setRelationshipTypes( List<RelationshipType> relationshipTypes )
    {
        this.relationshipTypes = relationshipTypes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programRules", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRule", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramRule> getProgramRules()
    {
        return programRules;
    }

    public void setProgramRules( List<ProgramRule> programRules )
    {
        this.programRules = programRules;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programRuleActions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRuleAction", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramRuleAction> getProgramRuleActions()
    {
        return programRuleActions;
    }

    public void setProgramRuleActions( List<ProgramRuleAction> programRuleActions )
    {
        this.programRuleActions = programRuleActions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programRuleVariables", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRuleVariable", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramRuleVariable> getProgramRuleVariables()
    {
        return programRuleVariables;
    }

    public void setProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables )
    {
        this.programRuleVariables = programRuleVariables;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "events", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "event", namespace = DxfNamespaces.DXF_2_0 )
    public List<Event> getEvents()
    {
        return events;
    }

    public void setEvents( List<Event> events )
    {
        this.events = events;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "eventReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "eventReport", namespace = DxfNamespaces.DXF_2_0 )
    public List<EventReport> getEventReports()
    {
        return eventReports;
    }

    public void setEventReports( List<EventReport> eventReports )
    {
        this.eventReports = eventReports;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "eventCharts", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "eventChart", namespace = DxfNamespaces.DXF_2_0 )
    public List<EventChart> getEventCharts()
    {
        return eventCharts;
    }

    public void setEventCharts( List<EventChart> eventCharts )
    {
        this.eventCharts = eventCharts;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "trackedEntityTypes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityType> getTrackedEntityTypes()
    {
        return trackedEntityTypes;
    }

    public void setTrackedEntityTypes( List<TrackedEntityType> trackedEntityTypes )
    {
        this.trackedEntityTypes = trackedEntityTypes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "trackedEntityAttributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityAttribute> getTrackedEntityAttributes()
    {
        return trackedEntityAttributes;
    }

    public void setTrackedEntityAttributes( List<TrackedEntityAttribute> trackedEntityAttributes )
    {
        this.trackedEntityAttributes = trackedEntityAttributes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "dimensions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dimension", namespace = DxfNamespaces.DXF_2_0 )
    public List<DimensionalObject> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions( List<DimensionalObject> dimensions )
    {
        this.dimensions = dimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "predictors", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "predictor", namespace = DxfNamespaces.DXF_2_0 )
    public List<Predictor> getPredictors()
    {
        return predictors;
    }

    public void setPredictors( List<Predictor> predictors )
    {
        this.predictors = predictors;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "predictorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "predictorGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<PredictorGroup> getPredictorGroups()
    {
        return predictorGroups;
    }

    public void setPredictorGroups( List<PredictorGroup> predictorGroups )
    {
        this.predictorGroups = predictorGroups;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "programNotificationTemplates", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programNotificationTemplate", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramNotificationTemplate> getProgramNotificationTemplates()
    {
        return programNotificationTemplates;
    }

    public void setProgramNotificationTemplates( List<ProgramNotificationTemplate> programNotificationTemplates )
    {
        this.programNotificationTemplates = programNotificationTemplates;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "analyticsTableHooks", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "analyticsTableHook", namespace = DxfNamespaces.DXF_2_0 )
    public List<AnalyticsTableHook> getAnalyticsTableHooks()
    {
        return analyticsTableHooks;
    }

    public void setAnalyticsTableHooks( List<AnalyticsTableHook> analyticsTableHooks )
    {
        this.analyticsTableHooks = analyticsTableHooks;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "validationNotificationTemplates", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationNotificationTemplate", namespace = DxfNamespaces.DXF_2_0 )
    public List<ValidationNotificationTemplate> getValidationNotificationTemplates()
    {
        return this.validationNotificationTemplates;
    }

    public void setValidationNotificationTemplates( List<ValidationNotificationTemplate> validationNotificationTemplates )
    {
        this.validationNotificationTemplates = validationNotificationTemplates;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "jobConfigurations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
    public List<JobConfiguration> getJobConfigurations()
    {
        return jobConfigurations;
    }

    public void setJobConfigurations( List<JobConfiguration> jobConfigurations )
    {
        this.jobConfigurations = jobConfigurations;
    }

    @Override
    public String toString()
    {
        return "MetaData{" +
            "created=" + created +
            ", schemas=" + schemas +
            ", attributes=" + attributes +
            ", documents=" + documents +
            ", constants=" + constants +
            ", users=" + users +
            ", userRoles=" + userRoles +
            ", userGroups=" + userGroups +
            ", interpretations=" + interpretations +
            ", optionSets=" + optionSets +
            ", optionGroups=" + optionGroups +
            ", optionGroupSets=" + optionGroupSets +
            ", categories=" + categories +
            ", categoryOptions=" + categoryOptions +
            ", categoryCombos=" + categoryCombos +
            ", categoryOptionCombos=" + categoryOptionCombos +
            ", categoryOptionGroups=" + categoryOptionGroups +
            ", categoryOptionGroupSets=" + categoryOptionGroupSets +
            ", dataElementOperands=" + dataElementOperands +
            ", dashboards=" + dashboards +
            ", dataElements=" + dataElements +
            ", dataElementGroups=" + dataElementGroups +
            ", dataElementGroupSets=" + dataElementGroupSets +
            ", dimensions=" + dimensions +
            ", indicators=" + indicators +
            ", indicatorGroups=" + indicatorGroups +
            ", indicatorGroupSets=" + indicatorGroupSets +
            ", indicatorTypes=" + indicatorTypes +
            ", items=" + items +
            ", organisationUnits=" + organisationUnits +
            ", organisationUnitGroups=" + organisationUnitGroups +
            ", organisationUnitGroupSets=" + organisationUnitGroupSets +
            ", organisationUnitLevels=" + organisationUnitLevels +
            ", validationRules=" + validationRules +
            ", validationRuleGroups=" + validationRuleGroups +
            ", sqlViews=" + sqlViews +
            ", charts=" + charts +
            ", reports=" + reports +
            ", reportTables=" + reportTables +
            ", maps=" + maps +
            ", mapViews=" + mapViews +
            ", legendSets=" + legendSets +
            ", externalMapLayers=" + externalMapLayers +
            ", sections=" + sections +
            ", dataSets=" + dataSets +
            ", programs=" + programs +
            ", programStages=" + programStages +
            ", relationshipTypes=" + relationshipTypes +
            ", trackedEntityTypes=" + trackedEntityTypes +
            ", trackedEntityAttributes=" + trackedEntityAttributes +
            ", programNotificationTemplates=" + programNotificationTemplates +
            ", predictors=" + predictors +
            ", predictorGroups=" + predictorGroups +
            ", analyticsTableHooks=" + analyticsTableHooks +
            ", validationNotificationTemplates=" + validationNotificationTemplates +
            ", jobConfigurations=" + jobConfigurations +
            '}';
    }
}
