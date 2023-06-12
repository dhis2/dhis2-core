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
package org.hisp.dhis.program;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class ProgramTest
{

    @Test
    void testGetAnalyticsDataElements()
    {
        DataElement deA = new DataElement( "DataElementA" );
        deA.setAutoFields();
        DataElement deB = new DataElement( "DataElementA" );
        deB.setAutoFields();
        Program prA = new Program( "ProgramA" );
        prA.setAutoFields();
        ProgramStage psA = new ProgramStage( "ProgramStageA", prA );
        psA.setAutoFields();
        prA.getProgramStages().add( psA );
        ProgramStageDataElement psdeA = new ProgramStageDataElement( psA, deA );
        psdeA.setSkipAnalytics( false );
        ProgramStageDataElement psdeB = new ProgramStageDataElement( psA, deB );
        psdeB.setSkipAnalytics( true );
        psA.getProgramStageDataElements().add( psdeA );
        psA.getProgramStageDataElements().add( psdeB );
        assertEquals( 2, prA.getDataElements().size() );
        assertTrue( prA.getDataElements().contains( deA ) );
        assertTrue( prA.getDataElements().contains( deB ) );
        assertEquals( 1, prA.getAnalyticsDataElements().size() );
        assertTrue( prA.getDataElements().contains( deA ) );
    }

    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Program original = getNewProgram();
        Program copy = Program.shallowCopy( original, Map.of( "prefix", "copy" ) );

        //check for differences
        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotSame( original.getOrganisationUnits(), copy.getOrganisationUnits() );
        assertNotSame( original.getProgramStages(), copy.getProgramStages() );
        assertNotEquals( original.getProgramStages(), copy.getProgramStages() );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );

        //check equal
        assertEquals( original.getAccess(), copy.getAccess() );
        assertEquals( original.getAccessLevel(), copy.getAccessLevel() );
        assertEquals( original.getAnalyticsDataElements(), copy.getAnalyticsDataElements() );
        assertEquals( original.getCategoryCombo(), copy.getCategoryCombo() );
        assertEquals( original.getCompleteEventsExpiryDays(), copy.getCompleteEventsExpiryDays() );
        assertEquals( original.getDataElements(), copy.getDataElements() );
        assertEquals( original.getDataEntryForm(), copy.getDataEntryForm() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getDisplayEnrollmentDateLabel(), copy.getDisplayEnrollmentDateLabel() );
        assertEquals( original.getDisplayIncidentDate(), copy.getDisplayIncidentDate() );
        assertEquals( original.getEnrollmentDateLabel(), copy.getEnrollmentDateLabel() );
        assertEquals( original.getExpiryDays(), copy.getExpiryDays() );
        assertEquals( original.getExpiryPeriodType(), copy.getExpiryPeriodType() );
        assertEquals( original.getFeatureType(), copy.getFeatureType() );
        assertEquals( original.getFormName(), copy.getFormName() );
        assertEquals( original.getIgnoreOverdueEvents(), copy.getIgnoreOverdueEvents() );
        assertEquals( original.getMaxTeiCountToReturn(), copy.getMaxTeiCountToReturn() );
        assertEquals( original.getMinAttributesRequiredToSearch(), copy.getMinAttributesRequiredToSearch() );
        assertEquals( original.getNotificationTemplates(), copy.getNotificationTemplates() );
        assertEquals( original.getOnlyEnrollOnce(), copy.getOnlyEnrollOnce() );
        assertEquals( original.getOpenDaysAfterCoEndDate(), copy.getOpenDaysAfterCoEndDate() );
        assertEquals( original.getOrganisationUnits(), copy.getOrganisationUnits() );
        assertEquals( original.getProgramType(), copy.getProgramType() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getSelectEnrollmentDatesInFuture(), copy.getSelectEnrollmentDatesInFuture() );
        assertEquals( original.getSelectIncidentDatesInFuture(), copy.getSelectIncidentDatesInFuture() );
        assertEquals( original.getStyle(), copy.getStyle() );
        assertEquals( original.getTrackedEntityType(), copy.getTrackedEntityType() );
        assertEquals( original.getTranslations(), copy.getTranslations() );

        //check empty
        assertTrue( copy.getProgramAttributes().isEmpty() );
        assertTrue( copy.getProgramIndicators().isEmpty() );
        assertTrue( copy.getProgramRuleVariables().isEmpty() );
        assertTrue( copy.getProgramAttributes().isEmpty() );
    }

    @Test
    void testCopyOfWithCodeValue()
    {
        Program original = getNewProgram();
        original.setCode( "code123" );
        Program copy = Program.shallowCopy( original, Map.of( "prefix", "copy" ) );

        assertNull( copy.getCode() );
    }

    @Test
    void testCopyOfWithNullCodeValue()
    {
        Program original = getNewProgram();
        original.setCode( null );
        Program copy = Program.shallowCopy( original, Map.of( "prefix", "copy" ) );

        assertNull( copy.getCode() );
    }

    @Test
    void testCopyOfWithNulls()
    {
        Program original = getNewProgramWithNulls();
        Program copy = Program.shallowCopy( original, Map.of( "prefix", "copy" ) );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );

        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );
        assertFalse( copy.getUid().isBlank() );
        assertNotEquals( copy.getUid(), original.getUid() );

        assertEquals( "copynull", copy.getName() );
        assertEquals( original.getAccessLevel(), copy.getAccessLevel() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertTrue( copy.getAnalyticsDataElements().isEmpty() );
        assertTrue( copy.getDataElements().isEmpty() );
        assertTrue( copy.getNonConfidentialTrackedEntityAttributes().isEmpty() );
        assertTrue( copy.getNonConfidentialTrackedEntityAttributesWithLegendSet().isEmpty() );
        assertTrue( copy.getNotificationTemplates().isEmpty() );
        assertTrue( copy.getOrganisationUnits().isEmpty() );
        assertTrue( copy.getProgramAttributes().isEmpty() );
        assertTrue( copy.getProgramIndicators().isEmpty() );
        assertTrue( copy.getProgramRuleVariables().isEmpty() );
        assertTrue( copy.getProgramStages().isEmpty() );
        assertTrue( copy.getTrackedEntityAttributes().isEmpty() );
        assertTrue( copy.getUserRoles().isEmpty() );
    }

    @Test
    void testCopySections()
    {
        Program original = getNewProgram();
        Program programCopy = Program.shallowCopy( original, Map.of() );
        Set<ProgramSection> copySections = Program.copyProgramSections( programCopy, original.getProgramSections() );

        ProgramSection copySection = new ArrayList<>( copySections ).get( 0 );
        assertEquals( programCopy.getUid(), copySection.getProgram().getUid() );
        assertEquals( "section one", copySection.getName() );
    }

    @Test
    void testCopyRuleVariable()
    {
        Program original = getNewProgram();
        Program programCopy = Program.shallowCopy( original, Map.of() );
        Set<ProgramRuleVariable> copyRules = Program.copyProgramRuleVariables( programCopy,
            original.getProgramRuleVariables(), Map.of() );

        ProgramRuleVariable copyRuleVariable = new ArrayList<>( copyRules ).get( 0 );
        assertEquals( programCopy.getUid(), copyRuleVariable.getProgram().getUid() );
        assertEquals( "rule variable one", copyRuleVariable.getName() );
    }

    @Test
    void testCopyIndicators()
    {
        Program original = getNewProgram();
        Program programCopy = Program.shallowCopy( original, Map.of() );
        Set<ProgramIndicator> copyIndicators = Program.copyProgramIndicators( programCopy,
            original.getProgramIndicators() );

        ProgramIndicator copyIndicator = new ArrayList<>( copyIndicators ).get( 0 );
        assertEquals( programCopy.getUid(), copyIndicator.getProgram().getUid() );
        assertEquals( "indicator one", copyIndicator.getName() );
    }

    @Test
    void testCopyAttributes()
    {
        Program original = getNewProgram();
        Program programCopy = Program.shallowCopy( original, Map.of() );
        List<ProgramTrackedEntityAttribute> copyAttributes = Program.copyProgramAttributes( programCopy,
            original.getProgramAttributes() );

        ProgramTrackedEntityAttribute copyAttribute = copyAttributes.get( 0 );
        assertEquals( programCopy.getUid(), copyAttribute.getProgram().getUid() );
        assertEquals( "Copy of Program Name attribute 1", copyAttribute.getName() );
    }

    /**
     * This test checks the expected field count for {@link Program}. This is
     * important due to {@link Program#shallowCopy} functionality. If a new
     * field is added then {@link Program#shallowCopy} should be updated with
     * the appropriate copying approach (Deep or shallow copy). If the field is
     * not included in {@link Program#shallowCopy} this may have unexpected
     * results.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( Program.class );
        assertEquals( 55, allClassFieldsIncludingInherited.length );
    }

    public static boolean notEqualsOrBothNull( String original, String copy )
    {
        if ( original == null || copy == null )
            return true;
        return !original.equals( copy );
    }

    public static Program getNewProgram()
    {
        Program p = new Program();
        p.setAccessLevel( AccessLevel.OPEN );
        p.setAutoFields();
        p.setCategoryCombo( new CategoryCombo( "cat combo", DataDimensionType.ATTRIBUTE ) );
        p.setCode( CodeGenerator.generateCode( CodeGenerator.UID_CODE_SIZE ) );
        p.setCompleteEventsExpiryDays( 22 );
        p.setDataEntryForm( new DataEntryForm( "entry form" ) );
        p.setDescription( "Program description" );
        p.setDisplayIncidentDate( true );
        p.setDisplayFrontPageList( true );
        p.setEnrollmentDateLabel( "enroll date" );
        p.setExpiryDays( 33 );
        p.setExpiryPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.QUARTERLY ) );
        p.setFeatureType( FeatureType.NONE );
        p.setFormName( "Form name" );
        p.setIgnoreOverdueEvents( true );
        p.setIncidentDateLabel( "incident date" );
        p.setMaxTeiCountToReturn( 2 );
        p.setMinAttributesRequiredToSearch( 3 );
        p.setName( "Program Name" );
        p.setNotificationTemplates( Collections.emptySet() );
        p.setOnlyEnrollOnce( true );
        p.setOpenDaysAfterCoEndDate( 20 );
        p.setOrganisationUnits( Set.of( new OrganisationUnit( "Org One" ) ) );
        p.setPublicAccess( "rw------" );
        p.setProgramAttributes( getAttributes() );
        p.setProgramIndicators( getProgramIndicators() );
        p.setProgramRuleVariables( getProgramRuleVariables() );
        p.setProgramSections( getSections() );
        p.setProgramStages( getProgramStages() );
        p.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        p.setRelatedProgram( new Program( "Related Program" ) );
        p.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        p.setShortName( "short name" );
        p.setSelectEnrollmentDatesInFuture( true );
        p.setSelectIncidentDatesInFuture( false );
        p.setStyle( new ObjectStyle() );
        p.setSkipOffline( true );
        p.setTrackedEntityType( new TrackedEntityType( "TET", "description" ) );
        p.setUseFirstStageDuringRegistration( false );
        p.setUserRoles( Collections.emptySet() );
        return p;
    }

    private Program getNewProgramWithNulls()
    {
        Program p = new Program();
        p.setAccessLevel( null );
        p.setCode( null );
        p.setCompleteEventsExpiryDays( 0 );
        p.setDescription( null );
        p.setDisplayIncidentDate( false );
        p.setDisplayFrontPageList( false );
        p.setEnrollmentDateLabel( null );
        p.setExpiryDays( 0 );
        p.setFeatureType( null );
        p.setFormName( null );
        p.setIgnoreOverdueEvents( true );
        p.setIncidentDateLabel( null );
        p.setMaxTeiCountToReturn( 2 );
        p.setMinAttributesRequiredToSearch( 3 );
        p.setName( null );
        p.setOnlyEnrollOnce( true );
        p.setOpenDaysAfterCoEndDate( 20 );
        p.setProgramType( null );
        p.setPublicAccess( null );
        p.setSharing( null );
        p.setShortName( null );
        p.setSelectEnrollmentDatesInFuture( true );
        p.setSelectIncidentDatesInFuture( false );
        p.setSkipOffline( true );
        p.setUseFirstStageDuringRegistration( false );
        p.setCategoryCombo( null );
        p.setDataEntryForm( null );
        p.setExpiryPeriodType( null );
        p.setNotificationTemplates( null );
        p.setOrganisationUnits( null );
        p.setProgramAttributes( null );
        p.setProgramIndicators( null );
        p.setProgramRuleVariables( null );
        p.setProgramSections( null );
        p.setProgramStages( null );
        p.setRelatedProgram( null );
        p.setStyle( null );
        p.setTrackedEntityType( null );
        p.setUserRoles( null );
        return p;
    }

    private static Set<ProgramStage> getProgramStages()
    {
        ProgramStage stage = new ProgramStage();
        stage.setAutoFields();
        stage.setName( "stage one" );
        return Set.of( stage );
    }

    private static Set<ProgramIndicator> getProgramIndicators()
    {
        ProgramIndicator indicator = new ProgramIndicator();
        indicator.setAutoFields();
        indicator.setName( "indicator one" );
        return Set.of( indicator );
    }

    private static Set<ProgramRuleVariable> getProgramRuleVariables()
    {
        ProgramRuleVariable ruleVariable = new ProgramRuleVariable();
        ruleVariable.setAutoFields();
        ruleVariable.setName( "rule variable one" );
        return Set.of( ruleVariable );
    }

    private static List<ProgramTrackedEntityAttribute> getAttributes()
    {
        ProgramTrackedEntityAttribute programAttribute = new ProgramTrackedEntityAttribute();
        TrackedEntityAttribute attribute = new TrackedEntityAttribute();
        attribute.setName( "attribute 1" );
        programAttribute.setAutoFields();
        programAttribute.setName( "rule variable one" );
        programAttribute.setAttribute( attribute );
        return List.of( programAttribute );
    }

    private static Set<ProgramSection> getSections()
    {
        ProgramSection section = new ProgramSection();
        section.setAutoFields();
        section.setName( "section one" );
        return Set.of( section );
    }
}
