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
package org.hisp.dhis.eventhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.sql.DataSource;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventHookListenerTest extends PostgresIntegrationTestBase {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private FieldFilterService fieldFilterService;

  @Autowired private DataSource dataSource;

  @Autowired private EventHookService eventHookService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EventHookStore eventHookStore;

  @Test
  void testOnPreCommitCreatesTransactionWhenNotInTransaction() {
    EventHook eventHook = newEventHook("FooBar");
    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook.getUID());

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets())
        .thenReturn(
            List.of(
                EventHookTargets.builder()
                    .eventHook(eventHook)
                    .targets(List.of((outboxMessages, handlerCallback) -> {}))
                    .build()));

    assertEquals(
        0,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());

    EventHookListener eventHookListener =
        new EventHookListener(objectMapper, fieldFilterService, mockEventHookService, dataSource);
    eventHookListener.onPreCommit(EventUtils.metadataCreate(new OrganisationUnit()));

    assertEquals(
        1,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());
  }

  @Test
  @Transactional
  void testOnPreCommitsJoinsTransactionWhenInTransaction() {
    EventHook eventHook = newEventHook("Bar");
    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook.getUID());

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets())
        .thenReturn(
            List.of(
                EventHookTargets.builder()
                    .eventHook(eventHook)
                    .targets(List.of((outboxMessages, handlerCallback) -> {}))
                    .build()));

    EventHookListener eventHookListener =
        new EventHookListener(objectMapper, fieldFilterService, mockEventHookService, dataSource);

    assertEquals(
        0,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());

    OrganisationUnit organisationUnit = new OrganisationUnit();
    eventHookListener.onPreCommit(EventUtils.metadataCreate(organisationUnit));

    assertEquals(
        1,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());

    TestTransaction.end();

    assertEquals(
        0,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());
  }

  @Test
  @Transactional
  void testOnPreCommitPersistsEvent() {
    EventHook eventHook = newEventHook("Bar");
    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook.getUID());

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets())
        .thenReturn(
            List.of(
                EventHookTargets.builder()
                    .eventHook(eventHook)
                    .targets(List.of((outboxMessages, handlerCallback) -> {}))
                    .build()));

    EventHookListener eventHookListener =
        new EventHookListener(objectMapper, fieldFilterService, mockEventHookService, dataSource);

    assertEquals(
        0,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());

    OrganisationUnit organisationUnit = new OrganisationUnit();
    eventHookListener.onPreCommit(EventUtils.metadataCreate(organisationUnit));

    assertEquals(
        1,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()))
            .size());
  }

  @Test
  @Transactional
  void testOnPreCommitSkipsEventHookAndSwallowsExceptionOnPersistenceFailure() {
    EventHook barEventHook = newEventHook("Bar");
    EventHook quuzEventHook = newEventHook("Quuz");

    eventHookStore.save(quuzEventHook);
    eventHookService.createOutbox(quuzEventHook.getUID());

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets())
        .thenReturn(
            List.of(
                EventHookTargets.builder()
                    .eventHook(barEventHook)
                    .targets(List.of((outboxMessages, handlerCallback) -> {}))
                    .build(),
                EventHookTargets.builder()
                    .eventHook(quuzEventHook)
                    .targets(List.of((outboxMessages, handlerCallback) -> {}))
                    .build()));

    EventHookListener eventHookListener =
        new EventHookListener(objectMapper, fieldFilterService, mockEventHookService, dataSource);

    assertEquals(
        0,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + quuzEventHook.getUID()))
            .size());

    OrganisationUnit organisationUnit = new OrganisationUnit();
    eventHookListener.onPreCommit(EventUtils.metadataCreate(organisationUnit));

    assertEquals(
        1,
        jdbcTemplate
            .queryForList(
                String.format(
                    "SELECT * FROM \"%s\"",
                    EventHookService.OUTBOX_PREFIX_TABLE_NAME + quuzEventHook.getUID()))
            .size());
  }

  private EventHook newEventHook(String name) {
    Source source = new Source();
    source.setPath("metadata.");

    WebhookTarget webhookTarget = new WebhookTarget();
    webhookTarget.setUrl("http://stub");

    EventHook eventHook = new EventHook();
    eventHook.setName(name);
    eventHook.setTargets(List.of(webhookTarget));
    eventHook.setUid(CodeGenerator.generateUid());
    eventHook.setSource(source);

    return eventHook;
  }
}
