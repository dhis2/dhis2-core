/*
 * Copyright (c) 2004-2023, University of Oslo
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
import static org.hisp.dhis.program.ProgramStageDataElementTest.getNewProgramStageDataElement;
import static org.hisp.dhis.program.ProgramStageSectionTest.getNewProgramStageSection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramStageTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Program program = new Program( "Program 1" );
        ProgramStage original = getNewProgramStageWithNoNulls( program );
        ProgramStage copy = ProgramStage.copyOf( original, program, Map.of( "prefix", "copy" ) );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertEquals( original.getDataEntryForm(), copy.getDataEntryForm() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFeatureType(), copy.getFeatureType() );
        assertEquals( original.getValidationStrategy(), copy.getValidationStrategy() );
        assertNotEquals( original.getName(), copy.getName() );
        assertNotSame( original.getProgramStageSections(), copy.getProgramStageSections() );
        assertNotEquals( original.getProgramStageSections(), copy.getProgramStageSections() );
        assertNotSame( original.getProgramStageDataElements(), copy.getProgramStageDataElements() );
        assertNotEquals( original.getProgramStageDataElements(), copy.getProgramStageDataElements() );
        assertEquals( original.getNotificationTemplates(), copy.getNotificationTemplates() );
    }

    @Test
    void testCopyOfWithNulls()
    {
        Program program = new Program( "Program 1" );
        ProgramStage original = getNewProgramStageWithNulls();
        ProgramStage copy = ProgramStage.copyOf( original, program, Map.of( "prefix", "copy" ) );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertEquals( original.getDataEntryForm(), copy.getDataEntryForm() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFeatureType(), copy.getFeatureType() );
        assertEquals( original.getValidationStrategy(), copy.getValidationStrategy() );
        assertNotEquals( original.getName(), copy.getName() );
        assertTrue( copy.getNotificationTemplates().isEmpty() );
        assertTrue( copy.getProgramStageSections().isEmpty() );
        assertTrue( copy.getProgramStageDataElements().isEmpty() );
    }

    /**
     * This test checks the expected field count for {@link ProgramStage}. This
     * is important due to {@link ProgramStage#copyOf} functionality. If a new
     * field is added then {@link ProgramStage#copyOf} should be updated with
     * the appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( ProgramStage.class );
        assertEquals( 50, allClassFieldsIncludingInherited.length );
    }

    private ProgramStage getNewProgramStageWithNoNulls( Program program )
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setCode( CodeGenerator.generateCode( CodeGenerator.UID_CODE_SIZE ) );
        programStage.setDataEntryForm( new DataEntryForm( "entry form" ) );
        programStage.setDescription( "Program description" );
        programStage.setDueDateLabel( "due label" );
        programStage.setExecutionDateLabel( "label" );
        programStage.setFeatureType( FeatureType.NONE );
        programStage.setFormName( "Form name" );
        programStage.setName( "Name" + CodeGenerator.generateUid() );
        programStage.setNextScheduleDate( new DataElement( "element" ) );
        programStage.setNotificationTemplates( Collections.emptySet() );
        programStage.setPeriodType( PeriodType.getPeriodType( PeriodTypeEnum.DAILY ) );
        programStage.setProgram( program );
        programStage.setReportDateToUse( "report date" );
        programStage.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        programStage.setShortName( "short name" );
        programStage.setProgramStageSections( getProgramStageSections( programStage ) );
        programStage.setProgramStageDataElements( getProgramStageDataElements( programStage ) );
        programStage.setSortOrder( 2 );
        programStage.setStyle( new ObjectStyle() );
        programStage.setStandardInterval( 11 );
        return programStage;
    }

    private ProgramStage getNewProgramStageWithNulls()
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setCode( null );
        programStage.setDataEntryForm( null );
        programStage.setDescription( null );
        programStage.setDueDateLabel( null );
        programStage.setExecutionDateLabel( null );
        programStage.setFeatureType( null );
        programStage.setFormName( null );
        programStage.setName( null );
        programStage.setNextScheduleDate( null );
        programStage.setNotificationTemplates( null );
        programStage.setPeriodType( null );
        programStage.setProgram( null );
        programStage.setReportDateToUse( null );
        programStage.setSharing( null );
        programStage.setShortName( null );
        programStage.setSortOrder( null );
        programStage.setStyle( null );
        programStage.setStandardInterval( null );
        programStage.setProgramStageSections( null );
        programStage.setProgramStageDataElements( null );
        return programStage;
    }

    private Set<ProgramStageSection> getProgramStageSections( ProgramStage programStage )
    {
        ProgramStageSection pss1 = getNewProgramStageSection( programStage );
        ProgramStageSection pss2 = getNewProgramStageSection( programStage );
        return Set.of( pss1, pss2 );
    }

    private Set<ProgramStageDataElement> getProgramStageDataElements( ProgramStage programStage )
    {
        ProgramStageDataElement psde1 = getNewProgramStageDataElement( programStage, "data el1" );
        ProgramStageDataElement psde2 = getNewProgramStageDataElement( programStage, "data el2" );
        return Set.of( psde1, psde2 );
    }
}
