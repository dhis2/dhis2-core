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

import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */

class RelationshipTypeControllerTest extends DhisControllerConvenienceTest
{
    @Test
    void testPostingEmptyRelationshipTypes()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "One or more errors occurred, please see full details in import report.",
            POST( "/relationshipTypes/",
                "{'relationshipTypes':[]}" )
                    .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testPostingRelationshipTypes()
    {
        Program program = createProgram( 'A' );
        TrackedEntityAttribute attributeA = createTrackedEntityAttribute( 'A' );
        TrackedEntityAttribute attributeB = createTrackedEntityAttribute( 'B' );

        ProgramTrackedEntityAttribute programTrackedEntityAttributeA = createProgramTrackedEntityAttribute( program,
            attributeA );
        ProgramTrackedEntityAttribute programTrackedEntityAttributeB = createProgramTrackedEntityAttribute( program,
            attributeB );

        program.setProgramAttributes(
            List.of( programTrackedEntityAttributeA, programTrackedEntityAttributeA, programTrackedEntityAttributeB ) );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'T' );
        manager.save( attributeA );
        manager.save( attributeB );
        manager.save( trackedEntityType );
        manager.save( program );

        JsonWebMessage relationtionShip = assertWebMessage( "Created", 201, "OK", null,
            POST( "/relationshipTypes/",
                "{'code': 'test-rel','description': 'test-rel','fromToName': 'A to B','toConstraint': { 'relationshipEntity': "
                    +
                    "'TRACKED_ENTITY_INSTANCE','trackedEntityType': {'id': '" + trackedEntityType.getUid()
                    + "'}, 'program': {'id': '" + program.getUid() + "'}, 'trackerDataView': {" +
                    "'attributes': ['" + attributeA.getUid() + "', '" + attributeB.getUid()
                    + "']} }, 'fromConstraint': { 'relationshipEntity':"
                    +
                    "'TRACKED_ENTITY_INSTANCE' , 'trackedEntityType': {'id': '" + trackedEntityType.getUid()
                    + "'}, 'program': {'id': '" + program.getUid() + "' }},'name': 'test-rel'}" )
                        .content( HttpStatus.CREATED ) );

    }

}
