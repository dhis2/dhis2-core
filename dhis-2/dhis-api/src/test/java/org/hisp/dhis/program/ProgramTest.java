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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
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
        Program original = getNewProgramWithNoNulls();
        ProgramCopyTuple programCopyTuple = Program.copyOf.apply( original, Map.of( "prefix", "copy" ) );
        Program copy = programCopyTuple.copy();

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertEquals( original.getOrganisationUnits(), copy.getOrganisationUnits() );
        assertNotSame( original.getOrganisationUnits(), copy.getOrganisationUnits() );
        assertEquals( original.getAccessLevel(), copy.getAccessLevel() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertNotEquals( original.getName(), copy.getName() );
        assertNotSame( original.getProgramStages(), copy.getProgramStages() );
        assertNotEquals( original.getProgramStages(), copy.getProgramStages() );
        assertEquals( original.getSelectIncidentDatesInFuture(), copy.getSelectIncidentDatesInFuture() );
    }

    @Test
    void testCopyOfWithNulls()
    {
        Program original = getNewProgramWithNulls();
        ProgramCopyTuple programCopyTuple = Program.copyOf.apply( original, Map.of( "prefix", "copy" ) );
        Program copy = programCopyTuple.copy();

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertTrue( copy.getOrganisationUnits().isEmpty() );
        assertTrue( copy.getOrganisationUnits().isEmpty() );
        assertEquals( original.getAccessLevel(), copy.getAccessLevel() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertNotEquals( original.getName(), copy.getName() );
        assertTrue( copy.getProgramStages().isEmpty() );
        assertTrue( copy.getProgramAttributes().isEmpty() );
        assertTrue( copy.getProgramIndicators().isEmpty() );
        assertTrue( copy.getUserRoles().isEmpty() );
    }

    private Program getNewProgramWithNoNulls()
    {
        Program program = new Program();
        program.setAccessLevel( AccessLevel.OPEN );
        program.setCode( CodeGenerator.generateCode( CodeGenerator.CODESIZE ) );
        program.setCompleteEventsExpiryDays( 22 );
        program.setDescription( "Program description" );
        program.setDisplayIncidentDate( true );
        program.setDisplayFrontPageList( true );
        program.setEnrollmentDateLabel( "enroll date" );
        program.setExpiryDays( 33 );
        program.setFeatureType( FeatureType.NONE );
        program.setFormName( "Form name" );
        program.setIgnoreOverdueEvents( true );
        program.setIncidentDateLabel( "incident date" );
        program.setMaxTeiCountToReturn( 2 );
        program.setMinAttributesRequiredToSearch( 3 );
        program.setName( "Name" + CodeGenerator.generateUid() );
        program.setOnlyEnrollOnce( true );
        program.setOpenDaysAfterCoEndDate( 20 );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        program.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        program.setShortName( "short name" );
        program.setSelectEnrollmentDatesInFuture( true );
        program.setSelectIncidentDatesInFuture( false );
        program.setSkipOffline( true );
        program.setUseFirstStageDuringRegistration( false );
        program.setCategoryCombo( new CategoryCombo( "cat combo", DataDimensionType.ATTRIBUTE ) );
        program.setDataEntryForm( new DataEntryForm( "entry form" ) );
        program.setExpiryPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.QUARTERLY ) );
        program.setNotificationTemplates( Collections.emptySet() );
        program.setOrganisationUnits( Set.of( new OrganisationUnit( "Org One" ) ) );
        program.setProgramAttributes( Collections.emptyList() );
        program.setProgramIndicators( Collections.emptySet() );
        program.setProgramRuleVariables( Collections.emptySet() );
        program.setProgramSections( Collections.emptySet() );
        program.setProgramStages( getProgramStages() );
        program.setRelatedProgram( new Program( "Related Program" ) );
        program.setStyle( new ObjectStyle() );
        program.setTrackedEntityType( new TrackedEntityType( "TET", "description" ) );
        program.setUserRoles( Collections.emptySet() );
        return program;
    }

    private Program getNewProgramWithNulls()
    {
        Program program = new Program();
        program.setAccessLevel( null );
        program.setCode( null );
        program.setCompleteEventsExpiryDays( 0 );
        program.setDescription( null );
        program.setDisplayIncidentDate( false );
        program.setDisplayFrontPageList( false );
        program.setEnrollmentDateLabel( null );
        program.setExpiryDays( 0 );
        program.setFeatureType( null );
        program.setFormName( null );
        program.setIgnoreOverdueEvents( true );
        program.setIncidentDateLabel( null );
        program.setMaxTeiCountToReturn( 2 );
        program.setMinAttributesRequiredToSearch( 3 );
        program.setName( null );
        program.setOnlyEnrollOnce( true );
        program.setOpenDaysAfterCoEndDate( 20 );
        program.setProgramType( null );
        program.setSharing( null );
        program.setShortName( null );
        program.setSelectEnrollmentDatesInFuture( true );
        program.setSelectIncidentDatesInFuture( false );
        program.setSkipOffline( true );
        program.setUseFirstStageDuringRegistration( false );
        program.setCategoryCombo( null );
        program.setDataEntryForm( null );
        program.setExpiryPeriodType( null );
        program.setNotificationTemplates( null );
        program.setOrganisationUnits( null );
        program.setProgramAttributes( null );
        program.setProgramIndicators( null );
        program.setProgramRuleVariables( null );
        program.setProgramSections( null );
        program.setProgramStages( null );
        program.setRelatedProgram( null );
        program.setStyle( null );
        program.setTrackedEntityType( null );
        program.setUserRoles( null );
        return program;
    }

    private Set<ProgramStage> getProgramStages()
    {
        ProgramStage stage = new ProgramStage();
        stage.setAutoFields();
        stage.setName( "stage one" );
        return Set.of( stage );
    }
}
