package org.hisp.dhis.system.deletion;

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

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.notifications.DataSetNotificationTemplate;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.i18n.locale.I18nLocale;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationComment;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionGroupSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorGroup;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramExpression;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorGroup;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeGroup;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.security.oauth2.OAuth2Client;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.hisp.dhis.version.Version;
import org.hisp.dhis.visualization.Visualization;

/**
 * A DeletionHandler should override methods for objects that, when deleted,
 * will affect the current object in any way. Eg. a DeletionHandler for
 * DataElementGroup should override the deleteDataElement(..) method which
 * should remove the DataElement from all DataElementGroups. Also, it should
 * override the allowDeleteDataElement() method and return a non-null String value
 * if there exists objects that are dependent on the DataElement and are
 * considered not be deleted. The return value could be a hint for which object
 * is denying the delete, like the name.
 *
 * @author Lars Helge Overland
 */
public abstract class DeletionHandler
{
    protected static final String ERROR = "";

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract String getClassName();

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public void deleteAttribute( Attribute attribute )
    {
    }

    public String allowDeleteAttribute( Attribute attribute )
    {
        return null;
    }

    public void deleteAttributeValue( AttributeValue attributeValue )
    {
    }

    public String allowDeleteAttributeValue( AttributeValue attributeValue )
    {
        return null;
    }

    public void deleteDataApproval( DataApproval dataApproval )
    {
    }

    public String allowDeleteDataApproval( DataApproval dataApproval )
    {
        return null;
    }

    public void deleteDataApprovalLevel( DataApprovalLevel dataApprovalLevel )
    {
    }

    public String allowDeleteDataApprovalLevel( DataApprovalLevel dataApprovalLevel )
    {
        return null;
    }

    public void deleteDataApprovalWorkflow( DataApprovalWorkflow workflow )
    {
    }

    public String allowDeleteDataApprovalWorkflow( DataApprovalWorkflow workflow )
    {
        return null;
    }

    public void deleteDataApprovalAudit( DataApprovalAudit audit )
    {
    }

    public String allowDeleteDataApprovalAudit( DataApprovalAudit audit )
    {
        return null;
    }

    public void deleteDataElement( DataElement dataElement )
    {
    }

    public String allowDeleteDataElement( DataElement dataElement )
    {
        return null;
    }

    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
    }

    public String allowDeleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        return null;
    }

    public void deleteDataElementGroupSet( DataElementGroupSet dataElementGroupSet )
    {
    }

    public String allowDeleteDataElementGroupSet( DataElementGroupSet dataElementGroupSet )
    {
        return null;
    }

    public void deleteCategory( Category category )
    {
    }

    public String allowDeleteCategory( Category category )
    {
        return null;
    }

    public void deleteCategoryOption( CategoryOption categoryOption )
    {
    }

    public String allowDeleteCategoryOption( CategoryOption categoryOption )
    {
        return null;
    }

    public void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
    }

    public String allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        return null;
    }

    public void deleteCategoryOptionCombo( CategoryOptionCombo categoryOptionCombo )
    {
    }

    public String allowDeleteCategoryOptionCombo( CategoryOptionCombo categoryOptionCombo )
    {
        return null;
    }

    public void deleteProgramMessage( ProgramMessage programMessage )
    {
    }

    public String allowDeleteProgramMessage( ProgramMessage programMessage )
    {
        return null;
    }

    public void deleteDataSet( DataSet dataSet )
    {
    }

    public String allowDeleteDataSet( DataSet dataSet )
    {
        return null;
    }

    public void deleteSection( Section section )
    {
    }

    public String allowDeleteSection( Section section )
    {
        return null;
    }

    public void deleteCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
    }

    public String allowDeleteCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        return null;
    }

    public void deleteDataValue( DataValue dataValue )
    {
    }

    public String allowDeleteDataValue( DataValue dataValue )
    {
        return null;
    }

    public void deleteExpression( Expression expression )
    {
    }

    public String allowDeleteExpression( Expression expression )
    {
        return null;
    }

    public void deleteMinMaxDataElement( MinMaxDataElement minMaxDataElement )
    {
    }

    public String allowDeleteMinMaxDataElement( MinMaxDataElement minMaxDataElement )
    {
        return null;
    }

    public void deleteIndicator( Indicator indicator )
    {
    }

    public String allowDeleteIndicator( Indicator indicator )
    {
        return null;
    }

    public void deleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
    }

    public String allowDeleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        return null;
    }

    public void deleteIndicatorType( IndicatorType indicatorType )
    {
    }

    public String allowDeleteIndicatorType( IndicatorType indicatorType )
    {
        return null;
    }

    public void deleteIndicatorGroupSet( IndicatorGroupSet indicatorGroupSet )
    {
    }

    public String allowDeleteIndicatorGroupSet( IndicatorGroupSet indicatorGroupSet )
    {
        return null;
    }

    public void deletePeriod( Period period )
    {
    }

    public String allowDeletePeriod( Period period )
    {
        return null;
    }

    public void deleteRelativePeriods( RelativePeriods relativePeriods )
    {
    }

    public String allowDeleteRelativePeriods( RelativePeriods relativePeriods )
    {
        return null;
    }

    public void deletePredictor( Predictor predictor )
    {
    }

    public String allowDeletePredictor( Predictor predictor )
    {
        return null;
    }

    public void deletePredictorGroup( PredictorGroup predictorGroup )
    {
    }

    public String allowDeletePredictorGroup( PredictorGroup predictorGroup )
    {
        return null;
    }

    public void deleteValidationRule( ValidationRule validationRule )
    {
    }

    public String allowDeleteValidationRule( ValidationRule validationRule )
    {
        return null;
    }

    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
    }

    public String allowDeleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        return null;
    }

    public void deleteValidationResult( ValidationResult validationResult )
    {
    }

    public String allowDeleteValidationResult( ValidationResult validationResult )
    {
        return null;
    }

    public void deleteDataEntryForm( DataEntryForm form )
    {
    }

    public String allowDeleteValidationNotificationTemplate( ValidationNotificationTemplate vrnt )
    {
        return null;
    }

    public void deleteValidationNotificationTemplate( ValidationNotificationTemplate vrnt )
    {
    }

    public String allowDeleteDataEntryForm( DataEntryForm form )
    {
        return null;
    }

    public void deleteOrganisationUnit( OrganisationUnit unit )
    {
    }

    public String allowDeleteOrganisationUnit( OrganisationUnit unit )
    {
        return null;
    }

    public void deleteOrganisationUnitGroup( OrganisationUnitGroup group )
    {
    }

    public String allowDeleteOrganisationUnitGroup( OrganisationUnitGroup group )
    {
        return null;
    }

    public void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
    }

    public String allowDeleteOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        return null;
    }

    public void deleteOrganisationUnitLevel( OrganisationUnitLevel level )
    {
    }

    public String allowDeleteOrganisationUnitLevel( OrganisationUnitLevel level )
    {
        return null;
    }

    public void deleteReport( Report report )
    {
    }

    public String allowDeleteReport( Report report )
    {
        return null;
    }

    public void deleteUser( User user )
    {
    }

    public String allowDeleteUser( User user )
    {
        return null;
    }

    public void deleteUserCredentials( UserCredentials credentials )
    {
    }

    public String allowDeleteUserCredentials( UserCredentials credentials )
    {
        return null;
    }

    public void deleteUserAuthorityGroup( UserAuthorityGroup authorityGroup )
    {
    }

    public String allowDeleteUserAuthorityGroup( UserAuthorityGroup authorityGroup )
    {
        return null;
    }

    public String allowDeleteUserGroup( UserGroup userGroup )
    {
        return null;
    }

    public void deleteUserGroup( UserGroup userGroup )
    {
    }

    public void deleteUserSetting( UserSetting userSetting )
    {
    }

    public String allowDeleteUserSetting( UserSetting userSetting )
    {
        return null;
    }

    public void deleteDocument( Document document )
    {
    }

    public String allowDeleteDocument( Document document )
    {
        return null;
    }

    public void deleteLegend( Legend mapLegend )
    {
    }

    public String allowDeleteLegend( Legend mapLegend )
    {
        return null;
    }

    public void deleteLegendSet( LegendSet legendSet )
    {
    }

    public String allowDeleteLegendSet( LegendSet legendSet )
    {
        return null;
    }

    public void deleteMap( Map map )
    {
    }

    public String allowDeleteMap( Map map )
    {
        return null;
    }

    public void deleteExternalMapLayer( ExternalMapLayer externalMapLayer )
    {
    }

    public String allowDeleteExternalMapLayer( ExternalMapLayer externalMapLayer )
    {
        return null;
    }

    public void deleteMapView( MapView mapView )
    {
    }

    public String allowDeleteMapView( MapView mapView )
    {
        return null;
    }

    public void deleteInterpretation( Interpretation interpretation )
    {
    }

    public String allowDeleteIntepretation( Interpretation interpretation )
    {
        return null;
    }

    public void deleteTrackedEntityInstance( TrackedEntityInstance entityInstance )
    {
    }

    public String allowDeleteTrackedEntityInstance( TrackedEntityInstance entityInstance )
    {
        return null;
    }

    public void deleteTrackedEntityComment( TrackedEntityComment entityComment )
    {
    }

    public String allowDeleteTrackedEntityComment( TrackedEntityComment entityComment )
    {
        return null;
    }

    public void deleteTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
    }

    public String allowDeleteTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        return null;
    }

    public void deleteTrackedEntityAttributeValue( TrackedEntityAttributeValue attributeValue )
    {
    }

    public String allowDeleteTrackedEntityAttributeValue( TrackedEntityAttributeValue attributeValue )
    {
        return null;
    }

    public void deleteRelationship( Relationship relationship )
    {
    }

    public String allowDeleteRelationship( Relationship relationship )
    {
        return null;
    }

    public void deleteRelationshipType( RelationshipType relationshipType )
    {
    }

    public String allowDeleteRelationshipType( RelationshipType relationshipType )
    {
        return null;
    }

    public void deleteProgram( Program program )
    {
    }

    public String allowDeleteProgram( Program program )
    {
        return null;
    }

    public void deleteProgramInstance( ProgramInstance programInstance )
    {
    }

    public String allowDeleteProgramInstance( ProgramInstance programInstance )
    {
        return null;
    }

    public void deleteProgramStage( ProgramStage programStage )
    {
    }

    public String allowDeleteProgramStage( ProgramStage programStage )
    {
        return null;
    }

    public void deleteProgramStageSection( ProgramStageSection programStageSection )
    {
    }

    public String allowDeleteProgramStageSection( ProgramStageSection programStageSection )
    {
        return null;
    }

    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
    }

    public String allowDeleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        return null;
    }

    public void deleteProgramNotificationTemplate( ProgramNotificationTemplate programNotificationTemplate )
    {
    }

    public void allowDeleteProgramNotificationTemplate( ProgramNotificationTemplate programNotificationTemplate )
    {
    }

    public void deleteProgramRule( ProgramRule programRule )
    {
    }

    public String allowDeleteProgramRule( ProgramRule programRule )
    {
        return null;
    }

    public void deleteProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
    }

    public String allowDeleteProgramRuleVariable( ProgramRuleVariable programRuleVariable )
    {
        return null;
    }

    public void deleteProgramRuleAction( ProgramRuleAction programRuleAction )
    {
    }

    public String allowDeleteProgramRuleAction( ProgramRuleAction programRuleAction )
    {
        return null;
    }

    public void deleteProgramStageDataElement( ProgramStageDataElement programStageDataElement )
    {
    }

    public String allowDeleteProgramStageDataElement( ProgramStageDataElement programStageDataElement )
    {
        return null;
    }

    public void deleteEventDataValue( EventDataValue eventDataValue )
    {
    }

    public String allowDeleteEventDataValue( EventDataValue eventDataValue )
    {
        return null;
    }

    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
    }

    public String allowDeleteProgramIndicator( ProgramIndicator programIndicator )
    {
        return null;
    }

    public void deleteProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
    }

    public String allowDeleteProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        return null;
    }

    public void deleteProgramExpression( ProgramExpression programExpression )
    {
    }

    public String allowDeleteProgramExpression( ProgramExpression programExpression )
    {
        return null;
    }

    public void deleteConstant( Constant constant )
    {
    }

    public String allowDeleteConstant( Constant constant )
    {
        return null;
    }

    public void deleteOptionSet( OptionSet optionSet )
    {
    }

    public String allowDeleteOptionSet( OptionSet optionSet )
    {
        return null;
    }

    public void deleteOptionGroupSet( OptionGroupSet optionGroupSet )
    {
    }

    public String allowDeleteOptionGroupSet( OptionGroupSet optionGroupSet )
    {
        return null;
    }

    public void deleteOptionGroup( OptionGroup optionGroup )
    {
    }

    public String allowDeleteOptionGroup( OptionGroup optionGroup )
    {
        return null;
    }

    public void deleteOption( Option optionSet )
    {
    }

    public String allowDeleteOption( Option option )
    {
        return null;
    }

    public void deleteLockException( LockException lockException )
    {
    }

    public String allowDeleteLockException( LockException lockException )
    {
        return null;
    }

    public void deleteIntepretation( Interpretation interpretation )
    {
    }

    public String allowDeleteInterpretation( Interpretation interpretation )
    {
        return null;
    }

    public void deleteI18nLocale( I18nLocale i18nLocale )
    {
    }

    public String allowDeleteI18nLocale( I18nLocale i18nLocale )
    {
        return null;
    }

    public void deleteSqlView( SqlView sqlView )
    {
    }

    public String allowDeleteSqlView( SqlView sqlView )
    {
        return null;
    }

    public void deleteDashboard( Dashboard dashboard )
    {
    }

    public String allowDeleteDashboard( Dashboard dashboard )
    {
        return null;
    }

    public void deleteDashboardItem( DashboardItem dashboardItem )
    {
    }

    public String allowDeleteDashboardItem( DashboardItem dashboardItem )
    {
        return null;
    }

    public void deleteCategoryOptionGroup( CategoryOptionGroup categoryOptionGroup )
    {
    }

    public String allowDeleteCategoryOptionGroup( CategoryOptionGroup categoryOptionGroup )
    {
        return null;
    }

    public void deleteCategoryOptionGroupSet( CategoryOptionGroupSet categoryOptionGroupSet )
    {
    }

    public String allowDeleteCategoryOptionGroupSet( CategoryOptionGroupSet categoryOptionGroupSet )
    {
        return null;
    }

    public void deleteTrackedEntityType( TrackedEntityType trackedEntityType )
    {
    }

    public String allowDeleteTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        return null;
    }

    public void deleteEventReport( EventReport eventReport )
    {
    }

    public String allowDeleteEventReport( EventReport eventReport )
    {
        return null;
    }

    public void deleteEventChart( EventChart eventChart )
    {
    }

    public String allowDeleteEventChart( EventChart eventChart )
    {
        return null;
    }

    public void deleteOAuth2Client( OAuth2Client oAuth2Client )
    {
    }

    public String allowDeleteOAuth2Client( OAuth2Client oAuth2Client )
    {
        return null;
    }

    public void deleteProgramTrackedEntityAttribute( ProgramTrackedEntityAttribute attribute )
    {
    }

    public String allowDeleteProgramTrackedEntityAttribute( ProgramTrackedEntityAttribute attribute )
    {
        return null;
    }

    public void deleteProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup color )
    {
    }

    public String allowDeleteProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup group )
    {
        return null;
    }

    public void deletePushAnalysis( PushAnalysis pushAnalysis )
    {
    }

    public String allowDeletePushAnalysis( PushAnalysis pushAnalysis )
    {
        return null;
    }

    public void deleteDataSetNotificationTemplate( DataSetNotificationTemplate dataSetNotificationTemplate )
    {

    }

    public String allowDeleteDataSetNotificationTemplate( DataSetNotificationTemplate dataSetNotificationTemplate )
    {
        return null;
    }

    public void deleteSMSCommand( SMSCommand smsCommand )
    {

    }

    public String allowDeleteSMSCommand( SMSCommand smsCommand )
    {
        return null;
    }

    public void deleteMessageConversation( MessageConversation messageConversation )
    {
    }

    public String allowDeleteMessageConversation( MessageConversation messageConversation )
    {
        return null;
    }

    public void deleteJobConfiguration( JobConfiguration jobConfiguration )
    {
    }

    public String allowDeleteJobConfiguration(JobConfiguration jobConfiguration )
    {
        return null;
    }

    public String allowDeleteFileResource( FileResource fileResource )
    {
        return null;
    }

    public void deleteFileResource( FileResource fileResource )
    {
    }

    public String allowDeleteTrackedEntityInstanceFilter( TrackedEntityInstanceFilter filter )
    {
        return null;
    }

    public void deleteTrackedEntityInstanceFilter( TrackedEntityInstanceFilter filter )
    {
    }

    public String allowDeleteVersion( Version version )
    {
        return null;
    }

    public void deleteVersion( Version version )
    {
    }

    public String allowDeleteMetadataVersion( MetadataVersion metadataVersion )
    {
        return null;
    }

    public void deleteMetadataVersion( MetadataVersion metadataVersion )
    {
    }

    public String allowDeleteReservedValue( ReservedValue reservedValue )
    {
        return null;
    }

    public void deleteReservedValue( ReservedValue reservedValue )
    {
    }

    public String allowDeletePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
        return null;
    }

    public void deletePotentialDuplicate( PotentialDuplicate potentialDuplicate )
    {
    }

    public String allowDeleteVisualization( Visualization visualization )
    {
        return null;
    }

    public void deleteVisualization( Visualization visualization )
    {
    }

    public String allowDeleteInterpretationComment( InterpretationComment comment )
    {
        return null;
    }

    public void deleteInterpretationComment ( InterpretationComment comment )
    {
    }
}
