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
package org.hisp.dhis.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.hisp.dhis.mapping.ImageFormat;
import org.hisp.dhis.mapping.MapLayerPosition;
import org.hisp.dhis.mapping.MapService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
class TranslationServiceTest extends DhisSpringTest
{

    @Autowired
    private UserService injectUserService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private IdentifiableObjectManager manager;

    private User user;

    private Locale locale;

    @Override
    public void setUpTest()
    {
        this.userService = injectUserService;
        user = createUserAndInjectSecurityContext( true );
        locale = Locale.FRENCH;
        CurrentUserUtil.setUserSetting( UserSettingKey.DB_LOCALE, locale );
    }

    @Test
    void testGetTranslation()
    {
        DataElement dataElementA = createDataElement( 'A' );
        manager.save( dataElementA );
        String translatedName = "translatedName";
        String translatedShortName = "translatedShortName";
        String translatedDescription = "translatedDescription";
        Set<Translation> translations = new HashSet<>( dataElementA.getTranslations() );
        translations.add( new Translation( locale.getLanguage(), "NAME", translatedName ) );
        translations.add( new Translation( locale.getLanguage(), "SHORT_NAME", translatedShortName ) );
        translations.add( new Translation( locale.getLanguage(), "DESCRIPTION", translatedDescription ) );
        manager.updateTranslations( dataElementA, translations );
        assertEquals( translatedName, dataElementA.getDisplayName() );
        assertEquals( translatedShortName, dataElementA.getDisplayShortName() );
        assertEquals( translatedDescription, dataElementA.getDisplayDescription() );
    }

    @Test
    void testFormNameTranslationForOption()
    {
        OptionSet optionSet = createOptionSet( 'A' );
        optionSet.setValueType( ValueType.TEXT );
        manager.save( optionSet );
        Option option = createOption( 'A' );
        option.setOptionSet( optionSet );
        manager.save( option );
        Set<Translation> translations = new HashSet<>( option.getTranslations() );
        String translatedValue = "Option FormName Translated";
        translations.add( new Translation( locale.getLanguage(), "FORM_NAME", translatedValue ) );
        manager.updateTranslations( option, translations );
        assertEquals( translatedValue, option.getDisplayFormName() );
    }

    @Test
    void testFormNameTranslationForProgramStageSection()
    {
        ProgramStageSection programStageSection = createProgramStageSection( 'A', 0 );
        manager.save( programStageSection );
        String translatedValue = "ProgramStageSection FormName Translated";
        Set<Translation> translations = new HashSet<>( programStageSection.getTranslations() );
        translations.add( new Translation( locale.getLanguage(), "FORM_NAME", translatedValue ) );
        manager.updateTranslations( programStageSection, translations );
        assertEquals( translatedValue, programStageSection.getDisplayFormName() );
    }

    @Test
    void testFormNameTranslationForProgramStage()
    {
        ProgramStage programStage = createProgramStage( 'A', 0 );
        manager.save( programStage );
        String translatedValue = "ProgramStage FormName Translated";
        Set<Translation> translations = new HashSet<>( programStage.getTranslations() );
        translations.add( new Translation( locale.getLanguage(), "FORM_NAME", translatedValue ) );
        manager.updateTranslations( programStage, translations );
        assertEquals( translatedValue, programStage.getDisplayFormName() );
    }

    @Test
    void testFormNameTranslationForProgramSection()
    {
        ProgramSection programSection = new ProgramSection();
        programSection.setName( "Section A" );
        programSection.setAutoFields();
        programSection.setSortOrder( 0 );
        manager.save( programSection );
        String translatedValue = "ProgramSection FormName Translated";
        Set<Translation> translations = new HashSet<>( programSection.getTranslations() );
        translations.add( new Translation( locale.getLanguage(), "FORM_NAME", translatedValue ) );
        manager.updateTranslations( programSection, translations );
        assertEquals( translatedValue, programSection.getDisplayFormName() );
    }

    @Test
    void testRelationshipTypeFromAndToTranslation()
    {
        RelationshipType relationshipType = createRelationshipType( 'A' );
        relationshipType.setFromToName( "From to name" );
        relationshipType.setToFromName( "To from name" );
        manager.save( relationshipType );
        String fromToNameTranslated = "From to name translated";
        String toFromNameTranslated = "To from name translated";
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "RELATIONSHIP_TO_FROM_NAME", toFromNameTranslated ) );
        translations.add( new Translation( locale.getLanguage(), "RELATIONSHIP_FROM_TO_NAME", fromToNameTranslated ) );
        manager.updateTranslations( relationshipType, translations );
        assertEquals( fromToNameTranslated, relationshipType.getDisplayFromToName() );
        assertEquals( toFromNameTranslated, relationshipType.getDisplayToFromName() );
    }

    @Test
    void testChartTranslations()
    {
        Program prA = createProgram( 'A', null, null );
        manager.save( prA );
        EventChart ecA = new EventChart( "ecA" );
        ecA.setProgram( prA );
        ecA.setType( EventVisualizationType.COLUMN );
        ecA.setBaseLineLabel( "BaseLineLabel" );
        ecA.setDomainAxisLabel( "DomainAxisLabel" );
        ecA.setRangeAxisLabel( "RangeAxisLabel" );
        ecA.setTargetLineLabel( "TargetLineLabel" );
        ecA.setTitle( "Title" );
        ecA.setSubtitle( "SubTitle" );
        manager.save( ecA );
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "baseLineLabel", "translated BaseLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "domainAxisLabel", "translated DomainAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "rangeAxisLabel", "translated RangeAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "targetLineLabel", "translated TargetLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "title", "translated Title" ) );
        translations.add( new Translation( locale.getLanguage(), "subtitle", "translated SubTitle" ) );
        manager.updateTranslations( ecA, translations );
        EventChart updated = manager.get( EventChart.class, ecA.getUid() );
        assertEquals( "translated BaseLineLabel", updated.getDisplayBaseLineLabel() );
        assertEquals( "translated DomainAxisLabel", updated.getDisplayDomainAxisLabel() );
        assertEquals( "translated RangeAxisLabel", updated.getDisplayRangeAxisLabel() );
        assertEquals( "translated TargetLineLabel", updated.getDisplayTargetLineLabel() );
        assertEquals( "translated Title", updated.getDisplayTitle() );
        assertEquals( "translated SubTitle", updated.getDisplaySubtitle() );
    }

    @Test
    void testEventVisualizationTranslations()
    {
        Program prA = createProgram( 'A', null, null );
        manager.save( prA );
        EventVisualization evA = new EventVisualization( "evA" );
        evA.setProgram( prA );
        evA.setType( EventVisualizationType.COLUMN );
        evA.setBaseLineLabel( "BaseLineLabel" );
        evA.setDomainAxisLabel( "DomainAxisLabel" );
        evA.setRangeAxisLabel( "RangeAxisLabel" );
        evA.setTargetLineLabel( "TargetLineLabel" );
        evA.setTitle( "Title" );
        evA.setSubtitle( "SubTitle" );
        manager.save( evA );
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "baseLineLabel", "translated BaseLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "domainAxisLabel", "translated DomainAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "rangeAxisLabel", "translated RangeAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "targetLineLabel", "translated TargetLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "title", "translated Title" ) );
        translations.add( new Translation( locale.getLanguage(), "subtitle", "translated SubTitle" ) );
        manager.updateTranslations( evA, translations );
        EventVisualization updated = manager.get( EventVisualization.class, evA.getUid() );
        assertEquals( "translated BaseLineLabel", updated.getDisplayBaseLineLabel() );
        assertEquals( "translated DomainAxisLabel", updated.getDisplayDomainAxisLabel() );
        assertEquals( "translated RangeAxisLabel", updated.getDisplayRangeAxisLabel() );
        assertEquals( "translated TargetLineLabel", updated.getDisplayTargetLineLabel() );
        assertEquals( "translated Title", updated.getDisplayTitle() );
        assertEquals( "translated SubTitle", updated.getDisplaySubtitle() );
    }

    @Test
    void testVisualizationTranslations()
    {
        Visualization visualization = createVisualization( 'A' );
        visualization.setBaseLineLabel( "BaseLineLabel" );
        visualization.setDomainAxisLabel( "DomainAxisLabel" );
        visualization.setRangeAxisLabel( "RangeAxisLabel" );
        visualization.setTargetLineLabel( "TargetLineLabel" );
        visualization.setTitle( "Title" );
        visualization.setSubtitle( "SubTitle" );
        manager.save( visualization );
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "baseLineLabel", "translated BaseLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "domainAxisLabel", "translated DomainAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "rangeAxisLabel", "translated RangeAxisLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "targetLineLabel", "translated TargetLineLabel" ) );
        translations.add( new Translation( locale.getLanguage(), "title", "translated Title" ) );
        translations.add( new Translation( locale.getLanguage(), "subtitle", "translated SubTitle" ) );
        manager.updateTranslations( visualization, translations );
        Visualization updated = manager.get( Visualization.class, visualization.getUid() );
        assertNotNull( updated );
        assertEquals( "translated BaseLineLabel", updated.getDisplayBaseLineLabel() );
        assertEquals( "translated DomainAxisLabel", updated.getDisplayDomainAxisLabel() );
        assertEquals( "translated RangeAxisLabel", updated.getDisplayRangeAxisLabel() );
        assertEquals( "translated TargetLineLabel", updated.getDisplayTargetLineLabel() );
        assertEquals( "translated Title", updated.getDisplayTitle() );
        assertEquals( "translated SubTitle", updated.getDisplaySubtitle() );
    }

    @Test
    void testExternalMapLayerTranslations()
    {
        ExternalMapLayer map = new ExternalMapLayer();
        map.setName( "Name" );
        map.setUrl( "URL" );
        map.setMapLayerPosition( MapLayerPosition.BASEMAP );
        map.setImageFormat( ImageFormat.JPG );
        map.setMapService( MapService.TMS );
        manager.save( map );
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "NAME", "translated Name" ) );
        manager.updateTranslations( map, translations );
        ExternalMapLayer updatedMap = manager.get( ExternalMapLayer.class, map.getUid() );
        assertEquals( "translated Name", updatedMap.getDisplayName() );
    }

    @Test
    void testNotificationTemplateTranslations()
    {
        ProgramNotificationTemplate template = createProgramNotificationTemplate( "", 0,
            NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE,
            Calendar.getInstance().getTime() );
        manager.save( template );
        Set<Translation> translations = new HashSet<>();
        translations.add( new Translation( locale.getLanguage(), "NAME", "translated Name" ) );
        translations.add( new Translation( locale.getLanguage(), "SUBJECT_TEMPLATE", "translated SUBJECT TEMPLATE" ) );
        translations.add( new Translation( locale.getLanguage(), "MESSAGE_TEMPLATE", "translated MESSAGE TEMPLATE" ) );
        manager.updateTranslations( template, translations );
        template = manager.get( ProgramNotificationTemplate.class, template.getUid() );
        assertEquals( "translated Name", template.getDisplayName() );
        assertEquals( "translated SUBJECT TEMPLATE", template.getDisplaySubjectTemplate() );
        assertEquals( "translated MESSAGE TEMPLATE", template.getDisplayMessageTemplate() );
    }

    @Test
    void testPredictorTranslations()
    {
        DataElement dataElementX = createDataElement( 'X', ValueType.NUMBER, AggregationType.NONE );
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        manager.save( dataElementA );
        manager.save( dataElementB );
        manager.save( dataElementX );
        OrganisationUnitLevel orgUnitLevel1 = new OrganisationUnitLevel( 1, "Level1" );
        manager.save( orgUnitLevel1 );
        CategoryOptionCombo defaultCombo = categoryService.getDefaultCategoryOptionCombo();
        PeriodType periodTypeMonthly = PeriodType.getPeriodTypeByName( "Monthly" );
        Expression expressionA = new Expression(
            "AVG(#{" + dataElementA.getUid() + "})+1.5*STDDEV(#{" + dataElementA.getUid() + "})", "descriptionA" );
        expressionA.setTranslations(
            Sets.newHashSet( new Translation( locale.getLanguage(), "DESCRIPTION", "translated descriptionA" ) ) );
        Expression expressionB = new Expression( "AVG(#{" + dataElementB.getUid() + "." + defaultCombo.getUid() + "})",
            "descriptionB" );
        expressionB.setTranslations(
            Sets.newHashSet( new Translation( locale.getLanguage(), "DESCRIPTION", "translated descriptionB" ) ) );
        Predictor predictor = createPredictor( dataElementX, defaultCombo, "A", expressionA, expressionB,
            periodTypeMonthly, orgUnitLevel1, 6, 1, 0 );
        manager.save( predictor );
        manager.updateTranslations( predictor,
            Sets.newHashSet( new Translation( locale.getLanguage(), "NAME", "translated Predictor Name" ) ) );
        predictor = manager.get( Predictor.class, predictor.getUid() );
        assertEquals( "translated Predictor Name", predictor.getDisplayName() );
        assertEquals( "translated descriptionA", predictor.getGenerator().getDisplayDescription() );
        assertEquals( "translated descriptionB", predictor.getSampleSkipTest().getDisplayDescription() );
    }

    @Test
    public void testDisplayNameWithNoTranslation()
    {
        DataElement dataElementA = createDataElement( 'A' );
        manager.save( dataElementA );
        assertEquals( "DataElementA", dataElementA.getDisplayName() );
    }
}
