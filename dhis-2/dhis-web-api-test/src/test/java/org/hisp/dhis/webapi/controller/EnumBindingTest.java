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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the custom PropertyEditorSupport for every Enum to ignore case is
 * correctly registered in {@link CrudControllerAdvice}.
 */
class EnumBindingTest extends DhisControllerConvenienceTest
{

    private User owner;

    private OrganisationUnit orgUnit;

    private Program program;

    @BeforeEach
    void setUp()
    {
        owner = makeUser( "owner" );

        orgUnit = createOrganisationUnit( 'A' );
        orgUnit.getSharing().setOwner( owner );
        manager.save( orgUnit, false );

        program = createProgram( 'A' );
        program.addOrganisationUnit( orgUnit );
        program.getSharing().setOwner( owner );
        manager.save( program, false );
    }

    @Test
    void getTrackedEntityReturnsCsvFormat()
    {
        ProgramStatus activeProgramStatus = ProgramStatus.ACTIVE;
        JsonResponse lowerCaseResponse = GET(
            "/tracker/trackedEntities?program={programId}&orgUnit={orgUnitId}&programStatus=" +
                activeProgramStatus.toString().toLowerCase(),
            program.getUid(), orgUnit.getUid() ).content( HttpStatus.OK );

        JsonResponse upperCaseResponse = GET(
            "/tracker/trackedEntities?program={programId}&orgUnit={orgUnitId}&programStatus=" +
                activeProgramStatus.toString().toLowerCase(),
            program.getUid(), orgUnit.getUid() ).content( HttpStatus.OK );

        assertEquals( lowerCaseResponse.getArray( "instances" ).size(),
            upperCaseResponse.getArray( "instances" ).size() );
    }
}
