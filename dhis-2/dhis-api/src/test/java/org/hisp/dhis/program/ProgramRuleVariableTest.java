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
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.program.ProgramTest.getNewProgram;
import static org.hisp.dhis.program.ProgramTest.notEqualsOrBothNull;
import static org.hisp.dhis.programrule.ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramRuleVariableTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Program originalProgram = getNewProgram();
        String originalStageUid = "aaA123456aa";
        String copyStageUid = "zzZ123456zz";

        ProgramRuleVariable original = getNewProgramRuleVariableWithStage( originalProgram, originalStageUid );
        ProgramStage stageOriginal = original.getProgramStage();
        ProgramStage stageCopy = ProgramStage.shallowCopy( stageOriginal, originalProgram );
        stageCopy.setUid( copyStageUid );

        Program copyProgram = Program.shallowCopy( originalProgram, Map.of() );
        ProgramRuleVariable copy = ProgramRuleVariable.copyOf( original, copyProgram, stageCopy );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram().getUid(), copy.getProgram().getUid() );
        assertNotEquals( original.getProgramStage(), copy.getProgramStage() );
        assertNotEquals( original.getProgramStage().getUid(), copy.getProgramStage().getUid() );

        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );

        assertEquals( "rule name", copy.getName() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getAccess(), copy.getAccess() );
        assertEquals( original.getAttributeValues(), copy.getAttributeValues() );
        assertEquals( original.getAttribute(), copy.getAttribute() );
        assertEquals( original.getSourceType(), copy.getSourceType() );
        assertEquals( original.getDataElement(), copy.getDataElement() );
        assertEquals( original.getUseCodeForOptionSet(), copy.getUseCodeForOptionSet() );
        assertEquals( original.getValueType(), copy.getValueType() );
        assertEquals( original.getSharing(), copy.getSharing() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( copyStageUid, copy.getProgramStage().getUid() );
    }

    @Test
    void testCopyOfCodeShouldBeNullWhenOriginalHasCode()
    {
        Program program = new Program( "Program 1" );
        ProgramStage stage = new ProgramStage( "Stage 1", program );
        ProgramRuleVariable original = getNewProgramRuleVariable( program, stage );
        original.setCode( "rule code" );
        ProgramRuleVariable copy = ProgramRuleVariable.copyOf( original, program, stage );

        assertNull( copy.getCode() );
    }

    /**
     * This test checks the expected field count for
     * {@link ProgramRuleVariable}. This is important due to
     * {@link ProgramRuleVariable#copyOf(ProgramRuleVariable, Program, ProgramStage)}
     * functionality. If a new field is added then
     * {@link ProgramRuleVariable#copyOf(ProgramRuleVariable, Program, ProgramStage)}
     * should be updated with the appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( ProgramRuleVariable.class );
        assertEquals( 23, allClassFieldsIncludingInherited.length );
    }

    private ProgramRuleVariable getNewProgramRuleVariable( Program program, ProgramStage programStage )
    {
        ProgramRuleVariable prv = new ProgramRuleVariable();
        prv.setAccess( new Access() );
        prv.setAttribute( new TrackedEntityAttribute() );
        prv.setAttributeValues( Set.of() );
        prv.setDataElement( new DataElement() );
        prv.setName( "rule name" );
        prv.setPublicAccess( "rw------" );
        prv.setProgram( program );
        prv.setProgramStage( programStage );
        prv.setSharing( Sharing.builder().publicAccess( "yes" ).owner( "admin" ).build() );
        prv.setSourceType( DATAELEMENT_CURRENT_EVENT );
        prv.setTranslations( Set.of() );
        prv.setUseCodeForOptionSet( true );
        prv.setValueType( TEXT );
        return prv;
    }

    private ProgramRuleVariable getNewProgramRuleVariableWithStage( Program program, String originalStageUid )
    {
        ProgramStage stage = new ProgramStage( "stage test", program );
        stage.setUid( originalStageUid );
        ProgramRuleVariable prv = getNewProgramRuleVariable( program, stage );
        prv.setProgramStage( stage );
        return prv;
    }
}
