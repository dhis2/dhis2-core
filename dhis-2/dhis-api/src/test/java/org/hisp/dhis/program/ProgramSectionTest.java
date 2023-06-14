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
import static org.hisp.dhis.program.ProgramTest.getNewProgram;
import static org.hisp.dhis.program.ProgramTest.notEqualsOrBothNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramSectionTest
{
    @Test
    void testCopyOfWithPropertyValuesSet()
    {
        Program originalProgram = getNewProgram();
        ProgramSection original = getNewProgramSection( originalProgram );

        Program copyProgram = Program.shallowCopy( originalProgram, Map.of() );
        ProgramSection copy = ProgramSection.copyOf.apply( original, copyProgram );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram().getUid(), copy.getProgram().getUid() );

        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );

        assertEquals( original.getAccess(), copy.getAccess() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFormName(), copy.getFormName() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSharing(), copy.getSharing() );
        assertEquals( original.getShortName(), copy.getShortName() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getStyle(), copy.getStyle() );
        assertEquals( original.getTranslations(), copy.getTranslations() );
        assertEquals( original.getTrackedEntityAttributes(), copy.getTrackedEntityAttributes() );
    }

    @Test
    void testCopyOfWithNullPropertyValues()
    {
        Program originalProgram = getNewProgram();
        ProgramSection original = getNewProgramSectionWithNulls();

        Program copyProgram = Program.shallowCopy( originalProgram, Map.of() );
        ProgramSection copy = ProgramSection.copyOf.apply( original, copyProgram );

        assertNotSame( original, copy );
        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );

        assertTrue( notEqualsOrBothNull( original.getCode(), copy.getCode() ) );

        assertEquals( original.getAccess(), copy.getAccess() );
        assertEquals( original.getDescription(), copy.getDescription() );
        assertEquals( original.getFormName(), copy.getFormName() );
        assertEquals( original.getName(), copy.getName() );
        assertEquals( original.getPublicAccess(), copy.getPublicAccess() );
        assertEquals( original.getRenderType(), copy.getRenderType() );
        assertEquals( original.getSharing(), copy.getSharing() );
        assertEquals( original.getShortName(), copy.getShortName() );
        assertEquals( original.getSortOrder(), copy.getSortOrder() );
        assertEquals( original.getStyle(), copy.getStyle() );
        assertEquals( original.getTranslations(), copy.getTranslations() );
        assertTrue( copy.getTrackedEntityAttributes().isEmpty() );
    }

    /**
     * This test checks the expected field count for {@link ProgramSection}.
     * This is important due to {@link ProgramSection#copyOf} functionality. If
     * a new field is added then {@link ProgramSection#copyOf} should be updated
     * with the appropriate copying approach.
     */
    @Test
    void testExpectedFieldCount()
    {
        Field[] allClassFieldsIncludingInherited = getAllFields( ProgramSection.class );
        assertEquals( 27, allClassFieldsIncludingInherited.length );
    }

    public static ProgramSection getNewProgramSection( Program original )
    {
        ProgramSection ps = new ProgramSection();
        ps.setAccess( new Access() );
        ps.setAutoFields();
        ps.setDescription( "PS Description" );
        ps.setFormName( "PS form name" );
        ps.setName( "PS name" );
        ps.setProgram( original );
        ps.setPublicAccess( "rw------" );
        ps.setRenderType( new DeviceRenderTypeMap<>() );
        ps.setSharing( new Sharing() );
        ps.setShortName( "PS short name" );
        ps.setSortOrder( 1 );
        ps.setStyle( new ObjectStyle() );
        ps.setTrackedEntityAttributes( List.of( new TrackedEntityAttribute() ) );
        ps.setTranslations( Set.of( new Translation() ) );
        return ps;
    }

    private ProgramSection getNewProgramSectionWithNulls()
    {
        ProgramSection ps = new ProgramSection();
        ps.setAccess( null );
        ps.setDescription( null );
        ps.setFormName( null );
        ps.setName( null );
        ps.setPublicAccess( null );
        ps.setRenderType( null );
        ps.setSharing( null );
        ps.setShortName( null );
        ps.setSortOrder( null );
        ps.setStyle( null );
        ps.setTrackedEntityAttributes( null );
        ps.setTranslations( null );
        return ps;
    }
}
