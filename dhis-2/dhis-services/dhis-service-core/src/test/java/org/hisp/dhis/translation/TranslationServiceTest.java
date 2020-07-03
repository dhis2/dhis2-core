package org.hisp.dhis.translation;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class TranslationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private UserService injectUserService;

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
        UserContext.setUser( user );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, locale );
    }

    @Test
    public void testOK()
    {
        DataElement dataElementA = createDataElement( 'A' );
        manager.save( dataElementA );

        String translatedValue = "translated";

        Set<Translation> listObjectTranslation = new HashSet<>( dataElementA.getTranslations() );

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.NAME, translatedValue ) );

        manager.updateTranslations( dataElementA, listObjectTranslation );

        assertEquals( translatedValue, dataElementA.getDisplayName() );
    }

    @Test
    public void testFormNameTranslationForOption()
    {
        OptionSet optionSet = createOptionSet( 'A' );
        optionSet.setValueType( ValueType.TEXT );
        manager.save( optionSet );
        Option option = createOption( 'A' );
        option.setOptionSet( optionSet );
        manager.save( option );
        Set<Translation> listObjectTranslation = new HashSet<>( option.getTranslations() );

        String translatedValue = "Option FormName Translated";

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.FORM_NAME, translatedValue ) );

        manager.updateTranslations( option, listObjectTranslation );

        assertEquals( translatedValue, option.getDisplayFormName() );
    }

    @Test
    public void testFormNameTranslationForRelationShip()
    {
        RelationshipType relationshipType = createRelationshipType( 'A' );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        manager.save( relationshipType );
        manager.save( organisationUnit );
        manager.save( attribute );

        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( 'A', organisationUnit, attribute );
        manager.save( trackedEntityInstance );

        Relationship relationship = new Relationship();
        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance( trackedEntityInstance );
        RelationshipItem to = new RelationshipItem();
        to.setTrackedEntityInstance( trackedEntityInstance );
        relationship.setFrom( from );
        relationship.setTo( to );
        relationship.setRelationshipType( relationshipType );

        manager.save( relationship );

        String translatedValue = "RelationShip FormName Translated";

        Set<Translation> listObjectTranslation = new HashSet<>( relationship.getTranslations() );

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.FORM_NAME, translatedValue ) );

        manager.updateTranslations( relationship, listObjectTranslation );

        assertEquals( translatedValue, relationship.getDisplayFormName() );
    }

    @Test
    public void testFormNameTranslationForProgramStageSection()
    {
        ProgramStageSection programStageSection = createProgramStageSection( 'A' , 0 );
        manager.save( programStageSection );

        String translatedValue = "ProgramStageSection FormName Translated";

        Set<Translation> listObjectTranslation = new HashSet<>( programStageSection.getTranslations() );

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.FORM_NAME, translatedValue ) );

        manager.updateTranslations( programStageSection, listObjectTranslation );

        assertEquals( translatedValue, programStageSection.getDisplayFormName() );
    }

    @Test
    public void testFormNameTranslationForProgramStage()
    {
        ProgramStage programStage = createProgramStage( 'A', 0 );
        manager.save( programStage );

        String translatedValue = "ProgramStage FormName Translated";

        Set<Translation> listObjectTranslation = new HashSet<>( programStage.getTranslations() );

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.FORM_NAME, translatedValue ) );

        manager.updateTranslations( programStage, listObjectTranslation );

        assertEquals( translatedValue, programStage.getDisplayFormName() );
    }

    @Test
    public void testFormNameTranslationForProgramSection()
    {
        ProgramSection programSection = new ProgramSection();
        programSection.setName( "Section A" );
        programSection.setAutoFields();
        programSection.setSortOrder( 0 );

        manager.save( programSection );

        String translatedValue = "ProgramSection FormName Translated";

        Set<Translation> listObjectTranslation = new HashSet<>( programSection.getTranslations() );

        listObjectTranslation.add( new Translation( locale.getLanguage(), TranslationProperty.FORM_NAME, translatedValue ) );

        manager.updateTranslations( programSection, listObjectTranslation );

        assertEquals( translatedValue, programSection.getDisplayFormName() );
    }

    @Test
    public void testRelationshipTypeFromAndToTranslation()
    {
        RelationshipType relationshipType = createRelationshipType( 'A' );

        relationshipType.setFromToName( "From to name" );
        relationshipType.setToFromName( "To from name" );

        manager.save( relationshipType );

        String fromToNameTranslated = "From to name translated";

        String toFromNameTranslated = "To from name translated";

        Set<Translation> translations = new HashSet<>();
        translations.add(  new Translation( locale.getLanguage(), TranslationProperty.RELATIONSHIP_TO_FROM_NAME, toFromNameTranslated ) );
        translations.add(  new Translation( locale.getLanguage(), TranslationProperty.RELATIONSHIP_FROM_TO_NAME, fromToNameTranslated ) );

        manager.updateTranslations( relationshipType, translations );

        assertEquals( fromToNameTranslated, relationshipType.getDisplayFromToName() );
        assertEquals( toFromNameTranslated, relationshipType.getDisplayToFromName() );
    }
}
