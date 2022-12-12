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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */

class RelationshipTypeControllerTest extends DhisControllerIntegrationTest
{

    private String relationshipTypeUid;

    private String programUid;

    private String attrA;

    private String attrB;

    private String trackedEntityType;

    private User userA;

    @BeforeEach
    void setUp()
    {
        userA = createUserWithAuth( "userA", "ALL" );

        switchContextToUser( userA );

        trackedEntityType = assertStatus( HttpStatus.CREATED,
            POST( "/trackedEntityTypes/",
                "{'name':'person', 'shortName':'person', 'description':'person', 'allowAuditLog':false }" ) );

        attrA = assertStatus( HttpStatus.CREATED,
            POST( "/trackedEntityAttributes/",
                "{'name':'attrA', 'shortName':'attrA', 'valueType':'TEXT', 'aggregationType':'NONE'}" ) );

        attrB = assertStatus( HttpStatus.CREATED,
            POST( "/trackedEntityAttributes/",
                "{'name':'attrB', 'shortName':'attrB', 'valueType':'TEXT', 'aggregationType':'NONE'}" ) );

        programUid = assertStatus( HttpStatus.CREATED, POST( "/programs/",
            "{'name':'test program', 'id':'VoZMWi7rBgj', 'shortName':'test program','programType':'WITH_REGISTRATION', 'trackedEntityType': {"
                +
                "'id': '" + trackedEntityType + "'}," +
                " 'programTrackedEntityAttributes' :  [{'id':'ZZplHtHLrgQ', 'trackedEntityAttribute' :{'id': '" + attrA
                + "' }}, {'id':'tOtVj7xlNHB', 'trackedEntityAttribute' :{'id': '" + attrB + "' }}] }" ) );

    }

    @Test
    void testPostingRelationshipTypes()
    {
        JsonWebMessage relationtionShip = assertWebMessage( "Created", 201, "OK", null,
            POST( "/relationshipTypes/",
                "{'code': 'test-rel','description': 'test-rel','fromToName': 'A to B','toConstraint': { 'relationshipEntity': "
                    +
                    "'TRACKED_ENTITY_INSTANCE','trackedEntityType': {'id': '" + trackedEntityType
                    + "'}, 'program': {'id': '" + programUid + "'}, 'trackerDataView': {" +
                    "'attributes': ['" + attrA + "', '" + attrB
                    + "']} }, 'fromConstraint': { 'relationshipEntity':"
                    +
                    "'TRACKED_ENTITY_INSTANCE' , 'trackedEntityType': {'id': '" + trackedEntityType
                    + "'}, 'program': {'id': '" + programUid + "' }},'name': 'test-rel'}" )
                        .content( HttpStatus.CREATED ) );

        String relationShipTypeUid = relationtionShip.getResponse().get( "uid" ).toString();

        JsonResponse relationShipType = GET( "/relationshipTypes/{id}?fields=toConstraint[trackerDataView[attributes]]",
            relationShipTypeUid ).content();
    }
}
