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
package org.hisp.dhis.artemis.audit.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

/**
 * @author Morten (Morty) <netroms@gmail.com>
 */
class PostUpdateAuditListenerTest {

  private static final List<String> PROPERTY_NAMES =
      List.of(
          "firstName", "lastLogin", "lastUpdated", "lastUpdatedBy", "lastCheckedInterpretations");

  private static final Set<String> BOOKKEEPING_ONLY = Set.of("lastUpdated", "lastUpdatedBy");

  private enum CollectionChange {
    RECREATE,
    UPDATE,
    REMOVE
  }

  private AuditManager auditManager;

  private ActionQueue actionQueue;

  private PersistenceContext persistenceContext;

  private EventSource session;

  private EntityPersister persister;

  private PostUpdateAuditListener listener;

  @BeforeEach
  void setUp() {
    auditManager = mock(AuditManager.class);
    actionQueue = mock(ActionQueue.class);
    persistenceContext = mock(PersistenceContext.class);
    session = mock(EventSource.class);
    when(session.getActionQueue()).thenReturn(actionQueue);
    when(session.getPersistenceContextInternal()).thenReturn(persistenceContext);

    persister = mock(EntityPersister.class);
    when(persister.getPropertyNames()).thenReturn(PROPERTY_NAMES.toArray(String[]::new));

    AuditMatrix auditMatrix = mock(AuditMatrix.class);
    when(auditMatrix.isEnabled(AuditScope.METADATA, AuditType.UPDATE)).thenReturn(true);

    Schema schema = mock(Schema.class);
    when(schema.getFieldNameMapProperties()).thenReturn(Map.of());
    SchemaService schemaService = mock(SchemaService.class);
    when(schemaService.getSchema(User.class)).thenReturn(schema);

    listener =
        new PostUpdateAuditListener(
            auditManager,
            mock(AuditObjectFactory.class),
            auditMatrix,
            mock(UsernameSupplier.class),
            schemaService);
  }

  static Stream<Set<String>> bookkeepingOnlyChanges() {
    return Stream.of(
        Set.of("lastLogin"),
        Set.of("lastUpdated"),
        Set.of("lastLogin", "lastUpdated", "lastUpdatedBy"),
        Set.of("lastCheckedInterpretations", "lastUpdated", "lastUpdatedBy"));
  }

  @ParameterizedTest
  @MethodSource("bookkeepingOnlyChanges")
  void testUpdateOfOnlyBookkeepingPropertiesIsNotAudited(Set<String> dirtyProperties) {
    listener.onPostUpdate(updateEvent(new User(), dirtyProperties));

    registeredProcess().doAfterTransactionCompletion(true, session);

    verify(auditManager, never()).send(any());
  }

  static Stream<Set<String>> realChanges() {
    return Stream.of(Set.of("firstName"), Set.of("firstName", "lastLogin", "lastUpdated"));
  }

  @ParameterizedTest
  @MethodSource("realChanges")
  void testRealChangeIsAuditedAfterCommit(Set<String> dirtyProperties) {
    listener.onPostUpdate(updateEvent(new User(), dirtyProperties));

    AfterTransactionCompletionProcess process = registeredProcess();
    verify(auditManager, never()).send(any());

    process.doAfterTransactionCompletion(true, session);

    ArgumentCaptor<Audit> audit = ArgumentCaptor.forClass(Audit.class);
    verify(auditManager).send(audit.capture());
    assertEquals(AuditType.UPDATE, audit.getValue().getAuditType());
    assertEquals(User.class.getName(), audit.getValue().getKlass());
  }

  static Stream<Arguments> collectionChanges() {
    return Arrays.stream(CollectionChange.values())
        .flatMap(change -> Stream.of(Arguments.of(change, true), Arguments.of(change, false)));
  }

  /**
   * A collection changed in place is reported in its own event, in the same flush as the update of
   * its owner or in another one.
   */
  @ParameterizedTest(name = "{0}, before the update: {1}")
  @MethodSource("collectionChanges")
  void testBookkeepingOnlyUpdateOfEntityWithChangedCollectionIsAudited(
      CollectionChange change, boolean beforeUpdate) {
    User user = new User();

    if (beforeUpdate) {
      changeCollection(user, change);
      listener.onPostUpdate(updateEvent(user, BOOKKEEPING_ONLY));
    } else {
      listener.onPostUpdate(updateEvent(user, BOOKKEEPING_ONLY));
      changeCollection(user, change);
    }

    registeredProcess().doAfterTransactionCompletion(true, session);

    verify(auditManager).send(any());
  }

  @Test
  void testBookkeepingOnlyUpdateIsNotAuditedForCollectionChangeOfAnotherEntity() {
    changeCollection(new User(), CollectionChange.UPDATE);
    listener.onPostUpdate(updateEvent(new User(), BOOKKEEPING_ONLY));

    registeredProcess().doAfterTransactionCompletion(true, session);

    verify(auditManager, never()).send(any());
  }

  @Test
  void testCollectionChangeOfCompletedTransactionIsNotCarriedOver() {
    User user = new User();
    changeCollection(user, CollectionChange.UPDATE);
    registeredProcess().doAfterTransactionCompletion(true, session);

    listener.onPostUpdate(updateEvent(user, BOOKKEEPING_ONLY));

    ArgumentCaptor<AfterTransactionCompletionProcess> processes =
        ArgumentCaptor.forClass(AfterTransactionCompletionProcess.class);
    verify(actionQueue, times(2)).registerProcess(processes.capture());
    processes.getAllValues().get(1).doAfterTransactionCompletion(true, session);
    verify(auditManager, never()).send(any());
  }

  @Test
  void testRealChangeIsNotAuditedWhenTransactionFails() {
    listener.onPostUpdate(updateEvent(new User(), Set.of("firstName")));

    registeredProcess().doAfterTransactionCompletion(false, session);

    verify(auditManager, never()).send(any());
  }

  @Test
  void testUpdateWithUnknownDirtyPropertiesIsAudited() {
    Object[] state = new Object[PROPERTY_NAMES.size()];
    listener.onPostUpdate(
        new PostUpdateEvent(new User(), 1L, state, state.clone(), null, persister, session));

    registeredProcess().doAfterTransactionCompletion(true, session);

    verify(auditManager).send(any());
  }

  private AfterTransactionCompletionProcess registeredProcess() {
    ArgumentCaptor<AfterTransactionCompletionProcess> process =
        ArgumentCaptor.forClass(AfterTransactionCompletionProcess.class);
    verify(actionQueue).registerProcess(process.capture());
    return process.getValue();
  }

  private PostUpdateEvent updateEvent(User user, Set<String> dirtyProperties) {
    Object[] state = new Object[PROPERTY_NAMES.size()];
    int[] dirty = dirtyProperties.stream().mapToInt(PROPERTY_NAMES::indexOf).toArray();

    return new PostUpdateEvent(user, 1L, state, state.clone(), dirty, persister, session);
  }

  private void changeCollection(User owner, CollectionChange change) {
    PersistentCollection collection = mock(PersistentCollection.class);
    when(collection.getOwner()).thenReturn(owner);
    when(persistenceContext.getLoadedCollectionOwnerOrNull(collection)).thenReturn(owner);

    switch (change) {
      case RECREATE ->
          listener.onPostRecreateCollection(
              new PostCollectionRecreateEvent(null, collection, session));
      case UPDATE ->
          listener.onPostUpdateCollection(new PostCollectionUpdateEvent(null, collection, session));
      case REMOVE ->
          listener.onPostRemoveCollection(
              new PostCollectionRemoveEvent(null, collection, session, owner));
    }
  }
}
