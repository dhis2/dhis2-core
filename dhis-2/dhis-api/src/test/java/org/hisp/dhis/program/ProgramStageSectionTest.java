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
import static org.hisp.dhis.program.ProgramTest.notEqualsOrBothNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramStageSectionTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Map<String, String> copyOptions = Map.of( "prefix", "copy of" );
        Program originalProgram = new Program( "Program 1" );
        Program copyProgram = Program.copyOf.apply( originalProgram, copyOptions ).copy();
        ProgramStage originalStage = new ProgramStage( "program stage 1", originalProgram );
        ProgramStage copyStage = ProgramStage.copyOf( originalStage, copyProgram, copyOptions );
        ProgramStageSection original = getNewProgramStageSection( originalStage );
        ProgramStageSection copy = ProgramStageSection.copyOf( original, copyStage );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getProgramStage(), copy.getProgramStage() );
        assertNotSame( original.getProgramStage(), copy.getProgramStage() );

        //check known unique constraints are not equal or both null
        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );
        assertTrue( notEqualsOrBothNull( original.getUid(), copy.getUid() ) );

        assertEquals( original.getDataElements(), copy.getDataElements() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFormName(), copy.getFormName() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getProgramIndicators(), copy.getProgramIndicators() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSharing(), copy.getSharing() );
        assertEquals( original.getShortName(), copy.getShortName() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getStyle(), copy.getStyle() );
    }

    @Test
    void testCopyOfWithNullValues()
    {
        Map<String, String> copyOptions = Map.of( "prefix", "copy of" );
        Program originalProgram = new Program( "Program 1" );
        Program copyProgram = Program.copyOf.apply( originalProgram, copyOptions ).copy();
        ProgramStage originalStage = new ProgramStage( "program stage 1", originalProgram );
        ProgramStage copyStage = ProgramStage.copyOf( originalStage, copyProgram, copyOptions );
        ProgramStageSection original = getNewProgramStageSectionWithNulls();
        ProgramStageSection copy = ProgramStageSection.copyOf( original, copyStage );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getProgramStage(), copy.getProgramStage() );
        assertNotSame( original.getProgramStage(), copy.getProgramStage() );

        //check known unique constraints are not equal or both null
        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );
        assertTrue( notEqualsOrBothNull( original.getUid(), copy.getUid() ) );

        assertTrue( copy.getDataElements().isEmpty() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFormName(), copy.getFormName() );
        assertEquals( original.getName(), copy.getName() );
        assertTrue( copy.getProgramIndicators().isEmpty() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSharing(), copy.getSharing() );
        assertEquals( original.getShortName(), copy.getShortName() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getStyle(), copy.getStyle() );
    }

    /**
     * This test checks the expected field count for
     * {@link ProgramStageSection}. This is important due to
     * {@link ProgramStageSection#copyOf} functionality. If a new field is added
     * then {@link ProgramStageSection#copyOf} should be updated with the
     * appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( ProgramStageSection.class );
        assertEquals( 27, allClassFieldsIncludingInherited.length );
    }

    public static ProgramStageSection getNewProgramStageSection( ProgramStage original )
    {
        ProgramStageSection pss = new ProgramStageSection();
        pss.setAutoFields();
        pss.setDataElements( List.of( new DataElement( "DE1" ), new DataElement( "DE2" ) ) );
        pss.setDescription( "PSS Description" );
        pss.setFormName( "PSS form name" );
        pss.setProgramIndicators( List.of( new ProgramIndicator() ) );
        pss.setProgramStage( original );
        pss.setPublicAccess( original.getPublicAccess() );
        pss.setRenderType( new DeviceRenderTypeMap<>() );
        pss.setSortOrder( 1 );
        pss.setShortName( "PSS short name" );
        pss.setSharing( new Sharing() );
        pss.setStyle( new ObjectStyle() );
        return pss;
    }

    private ProgramStageSection getNewProgramStageSectionWithNulls()
    {
        ProgramStageSection pss = new ProgramStageSection();
        pss.setDataElements( null );
        pss.setDescription( null );
        pss.setFormName( null );
        pss.setProgramIndicators( null );
        pss.setProgramStage( null );
        pss.setPublicAccess( null );
        pss.setRenderType( null );
        pss.setSharing( null );
        pss.setShortName( null );
        pss.setSortOrder( null );
        pss.setStyle( null );
        return pss;
    }
}
