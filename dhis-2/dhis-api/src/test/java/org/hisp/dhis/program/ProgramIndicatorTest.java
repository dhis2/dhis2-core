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

import static org.hisp.dhis.program.Program.DEFAULT_PREFIX;
import static org.hisp.dhis.program.Program.PREFIX_KEY;
import static org.hisp.dhis.program.ProgramTest.getNewProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class ProgramIndicatorTest
{

    @Test
    void testHasFilter()
    {
        ProgramIndicator pi = new ProgramIndicator();
        assertFalse( pi.hasFilter() );
        pi.setFilter( "true" );
        assertTrue( pi.hasFilter() );
    }

    @Test
    void testCopyOf()
    {
        Program programOriginal = getNewProgram();
        Program programCopy = Program.shallowCopy( programOriginal, Map.of() );
        ProgramIndicator original = getNewProgramIndicator( programOriginal );
        ProgramIndicator copy = ProgramIndicator.copyOf( original, programCopy, Map.of() );

        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram().getUid(), copy.getProgram().getUid() );
        assertNotSame( original, copy );

        assertEquals( original.getDecimals(), copy.getDecimals() );
        assertEquals( DEFAULT_PREFIX + original.getName(), copy.getName() );
        assertEquals( DEFAULT_PREFIX + original.getShortName(), copy.getShortName() );
    }

    @Test
    void testCopyOfWithPrefix()
    {
        String customPrefix = "use this ";
        Program programOriginal = getNewProgram();
        Program programCopy = Program.shallowCopy( programOriginal, Map.of() );
        ProgramIndicator original = getNewProgramIndicator( programOriginal );
        ProgramIndicator copy = ProgramIndicator.copyOf( original, programCopy, Map.of( PREFIX_KEY, customPrefix ) );

        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram().getUid(), copy.getProgram().getUid() );
        assertNotSame( original, copy );

        assertEquals( original.getDecimals(), copy.getDecimals() );
        assertEquals( customPrefix + original.getName(), copy.getName() );
        assertEquals( customPrefix + original.getShortName(), copy.getShortName() );
    }

    private ProgramIndicator getNewProgramIndicator( Program program )
    {
        ProgramIndicator pi = new ProgramIndicator();
        pi.setAutoFields();
        pi.setProgram( program );
        pi.setName( "indicator 1" );
        pi.setAccess( new Access() );
        pi.setDecimals( 2 );
        pi.setPublicAccess( "rw------" );
        pi.setAttributeValues( Set.of() );
        pi.setSharing( new Sharing() );
        pi.setTranslations( Set.of() );
        pi.setExpression( "expression" );
        pi.setFilter( "filter" );
        pi.setFormName( "form name" );
        pi.setOrgUnitField( "org unit field" );
        pi.setDisplayInForm( true );
        pi.setAnalyticsPeriodBoundaries( Set.of() );
        pi.setStyle( new ObjectStyle() );
        pi.setShortName( "short name" );
        pi.setDescription( "description" );
        return pi;
    }
}
