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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */
class RelationshipTypeControllerTest extends DhisControllerIntegrationTest {

  private String program;

  private String attrA;

  private String attrB;

  private String attrC;

  private String trackedEntityType;

  @BeforeEach
  void setUp() {
    trackedEntityType =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityTypes/",
                "{'name':'person', 'shortName':'person', 'description':'person', 'allowAuditLog':false }"));

    attrA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrA', 'shortName':'attrA', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    attrB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrB', 'shortName':'attrB', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    attrC =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes/",
                "{'name':'attrC', 'shortName':'attrC', 'valueType':'TEXT', 'aggregationType':'NONE'}"));

    program =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test program', 'id':'VoZMWi7rBgj', 'shortName':'test program','programType':'WITH_REGISTRATION', 'trackedEntityType': {"
                    + "'id': '"
                    + trackedEntityType
                    + "'},"
                    + " 'programTrackedEntityAttributes' :  [{ 'trackedEntityAttribute' :{'id': '"
                    + attrA
                    + "' }}, { 'trackedEntityAttribute' :{'id': '"
                    + attrB
                    + "' }}, { 'trackedEntityAttribute' :{'id': '"
                    + attrC
                    + "' }}] }"));
  }

  @Test
  void testPostingRelationshipTypes() {
    String relationshipTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/relationshipTypes/",
                "{'code': 'test-rel','description': 'test-rel','fromToName': 'A to B','toConstraint': { 'relationshipEntity': "
                    + "'TRACKED_ENTITY_INSTANCE','trackedEntityType': {'id': '"
                    + trackedEntityType
                    + "'}, 'program': {'id': '"
                    + program
                    + "'}, 'trackerDataView': {"
                    + "'attributes': ['"
                    + attrA
                    + "','"
                    + attrA
                    + "', '"
                    + attrB
                    + "','"
                    + attrC
                    + "']} }, "
                    + "'fromConstraint': { 'relationshipEntity':"
                    + "'TRACKED_ENTITY_INSTANCE' , 'trackedEntityType': {'id': '"
                    + trackedEntityType
                    + "'}, 'program': {'id': '"
                    + program
                    + "' }},'name': 'test-rel'}"));

    JsonList<JsonString> attributes =
        GET("/relationshipTypes/"
                + relationshipTypeId
                + "?fields=toConstraint[trackerDataView[attributes]]")
            .content()
            .getList("toConstraint.trackerDataView.attributes", JsonString.class);

    assertEquals(3, attributes.size());
    assertEquals(List.of(attrA, attrB, attrC), attributes.toList(JsonString::string));
  }
}
