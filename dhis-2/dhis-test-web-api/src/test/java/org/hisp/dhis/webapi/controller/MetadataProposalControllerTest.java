/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

/** Tests that metadata proposals are stored and served through the workflow API on PostgreSQL. */
@Transactional
class MetadataProposalControllerTest extends PostgresControllerIntegrationTestBase {
  private String unitId;

  @BeforeEach
  void setUp() {
    unitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
  }

  @Test
  void shouldServeEveryPropertyOfAProposedUpdate() {
    String proposalId = proposeNameUpdate("New name");

    JsonObject proposal = getProposal(proposalId);

    assertEquals(
        Set.of(
            "id",
            "type",
            "status",
            "target",
            "targetId",
            "createdBy",
            "created",
            "change",
            "comment",
            "apiEndpoints"),
        Set.copyOf(proposal.names()));
    assertEquals(proposalId, proposal.getString("id").string());
    assertEquals("UPDATE", proposal.getString("type").string());
    assertEquals("PROPOSED", proposal.getString("status").string());
    assertEquals("ORGANISATION_UNIT", proposal.getString("target").string());
    assertEquals(unitId, proposal.getString("targetId").string());
    assertEquals(getAdminUid(), proposal.getString("createdBy").string());
    assertFalse(proposal.getString("created").string().isEmpty());
    assertEquals("Rename it", proposal.getString("comment").string());
    JsonObject operation = proposal.getArray("change").getObject(0);
    assertEquals("replace", operation.getString("op").string());
    assertEquals("/name", operation.getString("path").string());
    assertEquals("New name", operation.getString("value").string());
  }

  @Test
  void shouldStoreAdjustedChangeAndTargetOfOpposedProposal() {
    String proposalId = proposeNameUpdate("New name");
    String otherUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'Other Unit', 'shortName':'OU2', 'openingDate': '2020-01-01'}"));
    assertStatus(
        HttpStatus.NO_CONTENT,
        PATCH(
            "/metadata/proposals/" + proposalId,
            Body("Wrong name"),
            ContentType(MediaType.TEXT_PLAIN)));
    JsonObject opposed = getProposal(proposalId);
    assertEquals("NEEDS_UPDATE", opposed.getString("status").string());
    assertEquals("Wrong name", opposed.getString("reason").string());

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/metadata/proposals/" + proposalId,
            "{'targetId':'"
                + otherUnitId
                + "','change':[{'op':'replace','path':'/name','value':'Better name'}]}"));

    JsonObject adjusted = getProposal(proposalId);
    assertEquals("PROPOSED", adjusted.getString("status").string());
    assertEquals(otherUnitId, adjusted.getString("targetId").string());
    assertEquals(
        "Better name", adjusted.getArray("change").getObject(0).getString("value").string());
  }

  @Test
  void shouldStoreFinalisingUserAndDateOfAcceptedProposal() {
    String proposalId = proposeNameUpdate("Accepted name");

    assertStatus(HttpStatus.OK, POST("/metadata/proposals/" + proposalId));

    JsonObject accepted = getProposal(proposalId);
    assertEquals("ACCEPTED", accepted.getString("status").string());
    assertEquals(getAdminUid(), accepted.getString("finalisedBy").string());
    assertFalse(accepted.getString("finalised").string().isEmpty());
  }

  @Test
  void shouldStoreReasonOfRejectedProposal() {
    String proposalId = proposeNameUpdate("Rejected name");

    assertStatus(
        HttpStatus.NO_CONTENT,
        DELETE(
            "/metadata/proposals/" + proposalId,
            Body("Not needed"),
            ContentType(MediaType.TEXT_PLAIN)));

    JsonObject rejected = getProposal(proposalId);
    assertEquals("REJECTED", rejected.getString("status").string());
    assertEquals("Not needed", rejected.getString("reason").string());
    assertEquals(getAdminUid(), rejected.getString("finalisedBy").string());
  }

  @Test
  void shouldFilterProposalsByStatusAndCreator() {
    String acceptedId = proposeNameUpdate("First name");
    String proposedId = proposeNameUpdate("Second name");
    assertStatus(HttpStatus.OK, POST("/metadata/proposals/" + acceptedId));
    startNewRequestSession();

    JsonArray proposals =
        GET(
                "/metadata/proposals?filter=status:eq:PROPOSED&filter=createdBy.id:eq:{user}&fields=id,status",
                getAdminUid())
            .content()
            .getArray("proposals");

    assertEquals(1, proposals.size());
    assertEquals(proposedId, proposals.getObject(0).getString("id").string());
    assertEquals("PROPOSED", proposals.getObject(0).getString("status").string());
  }

  @Test
  void shouldDescribeProposalPropertiesInTheSchema() {
    JsonObject schema = GET("/schemas/proposal").content();
    JsonArray properties = schema.getArray("properties");

    assertEquals("metadataProposal", schema.getString("name").string());
    assertEquals(
        Set.of(
            "id",
            "type",
            "status",
            "target",
            "targetId",
            "createdBy",
            "created",
            "change",
            "comment",
            "reason",
            "finalised",
            "finalisedBy"),
        properties.asList(JsonObject.class).stream()
            .map(property -> property.getString("name").string())
            .collect(Collectors.toSet()));
    assertTrue(
        properties.asList(JsonObject.class).stream()
            .allMatch(property -> property.getBoolean("persisted").booleanValue()),
        "every property is persisted");
    assertEquals(
        Set.of("type", "status", "target", "createdBy", "created", "change"),
        properties.asList(JsonObject.class).stream()
            .filter(property -> property.getBoolean("required").booleanValue())
            .map(property -> property.getString("name").string())
            .collect(Collectors.toSet()));
  }

  private String proposeNameUpdate(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/metadata/proposals/",
            "{'type':'UPDATE','target':'ORGANISATION_UNIT','targetId':'"
                + unitId
                + "','change':[{'op':'replace','path':'/name','value':'"
                + name
                + "'}],'comment':'Rename it'}"));
  }

  private JsonObject getProposal(String proposalId) {
    startNewRequestSession();
    return GET("/metadata/proposals/{id}?fields=*", proposalId).content();
  }
}
