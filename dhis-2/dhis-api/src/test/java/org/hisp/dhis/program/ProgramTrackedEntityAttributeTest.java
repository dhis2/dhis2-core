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

import static org.hisp.dhis.program.ProgramTest.getNewProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramTrackedEntityAttributeTest
{

    @Test
    void testCopyOf()
    {
        Program programOriginal = getNewProgram();
        Program programCopy = Program.shallowCopy( programOriginal, Map.of() );
        ProgramTrackedEntityAttribute original = getNewProgramAttribute( programOriginal );
        ProgramTrackedEntityAttribute copy = ProgramTrackedEntityAttribute.copyOf.apply( original, programCopy );

        assertNotEquals( original, copy );
        assertNotEquals( original.getUid(), copy.getUid() );
        assertNotEquals( original.getProgram().getUid(), copy.getProgram().getUid() );
        assertNotSame( original, copy );

        assertEquals( original.getAttribute(), copy.getAttribute() );
        assertEquals( "Copy of Program Name tracked entity attr 1", copy.getName() );
    }

    private ProgramTrackedEntityAttribute getNewProgramAttribute( Program program )
    {
        ProgramTrackedEntityAttribute ptea = new ProgramTrackedEntityAttribute();
        TrackedEntityAttribute tea = new TrackedEntityAttribute();
        tea.setAutoFields();
        tea.setName( "tracked entity attr 1" );

        ptea.setAttribute( tea );
        ptea.setAutoFields();
        ptea.setProgram( program );
        ptea.setName( "indicator 1" );
        ptea.setSortOrder( 2 );
        ptea.setMandatory( false );
        ptea.setAllowFutureDate( false );
        ptea.setSearchable( true );
        ptea.setAccess( new Access() );
        ptea.setPublicAccess( "rw------" );
        ptea.setAttributeValues( Set.of() );
        ptea.setSharing( new Sharing() );
        ptea.setTranslations( Set.of() );
        return ptea;
    }
}
