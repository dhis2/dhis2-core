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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramStageDataElementTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Map<String, String> copyOptions = Map.of( "prefix", "copy of" );
        Program originalProgram = new Program( "Program 1" );
        Program copyProgram = Program.shallowCopy( originalProgram, copyOptions );
        ProgramStage originalStage = new ProgramStage( "program stage 1", originalProgram );
        ProgramStage copyStage = ProgramStage.shallowCopy( originalStage, copyProgram );
        ProgramStageDataElement original = getNewProgramStageDataElement( originalStage, "data el" );
        ProgramStageDataElement copy = ProgramStageDataElement.copyOf( original, copyStage );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getProgramStage(), copy.getProgramStage() );
        assertNotSame( original.getProgramStage(), copy.getProgramStage() );
        assertNotEquals( original.getUid(), copy.getUid() );

        assertEquals( original.getAllowFutureDate(), copy.getAllowFutureDate() );
        assertEquals( original.getDataElement(), copy.getDataElement() );
        assertEquals( original.getDisplayInReports(), copy.getDisplayInReports() );
        assertEquals( original.isCompulsory(), copy.isCompulsory() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getSkipAnalytics(), copy.getSkipAnalytics() );
        assertEquals( original.getSkipSynchronization(), copy.getSkipSynchronization() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
    }

    @Test
    void testCopyOfWithNulls()
    {
        Map<String, String> copyOptions = Map.of( "prefix", "copy of" );
        Program originalProgram = new Program( "Program 1" );
        Program copyProgram = Program.shallowCopy( originalProgram, copyOptions );
        ProgramStage originalStage = new ProgramStage( "program stage 1", originalProgram );
        ProgramStage copyStage = ProgramStage.shallowCopy( originalStage, copyProgram );
        ProgramStageDataElement original = getNewProgramStageDataElementWithNulls();
        ProgramStageDataElement copy = ProgramStageDataElement.copyOf( original, copyStage );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getProgramStage(), copy.getProgramStage() );
        assertNotSame( original.getProgramStage(), copy.getProgramStage() );
        assertNotEquals( original.getUid(), copy.getUid() );

        assertEquals( original.getAllowFutureDate(), copy.getAllowFutureDate() );
        assertEquals( original.getDataElement(), copy.getDataElement() );
        assertEquals( original.getDisplayInReports(), copy.getDisplayInReports() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getSkipAnalytics(), copy.getSkipAnalytics() );
        assertEquals( original.getSkipSynchronization(), copy.getSkipSynchronization() );
        assertEquals( original.isCompulsory(), copy.isCompulsory() );
    }

    /**
     * This test checks the expected field count for
     * {@link ProgramStageDataElement}. This is important due to
     * {@link ProgramStageDataElement#copyOf} functionality. If a new field is
     * added then {@link ProgramStageDataElement#copyOf} should be updated with
     * the appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( ProgramStageDataElement.class );
        assertEquals( 27, allClassFieldsIncludingInherited.length );
    }

    public static ProgramStageDataElement getNewProgramStageDataElement( ProgramStage original, String dataElementName )
    {
        ProgramStageDataElement psde = new ProgramStageDataElement();
        psde.setAllowFutureDate( true );
        psde.setAllowProvidedElsewhere( true );
        psde.setAutoFields();
        psde.setCompulsory( true );
        psde.setDataElement( new DataElement( dataElementName ) );
        psde.setDisplayInReports( true );
        psde.setProgramStage( original );
        psde.setPublicAccess( "rw------" );
        psde.setRenderOptionsAsRadio( true );
        psde.setRenderType( new DeviceRenderTypeMap<>() );
        psde.setSkipAnalytics( true );
        psde.setSortOrder( 1 );
        return psde;
    }

    private ProgramStageDataElement getNewProgramStageDataElementWithNulls()
    {
        ProgramStageDataElement psde = new ProgramStageDataElement();
        psde.setDataElement( null );
        psde.setProgramStage( null );
        psde.setPublicAccess( null );
        psde.setRenderType( null );
        psde.setSortOrder( null );
        return psde;
    }
}
