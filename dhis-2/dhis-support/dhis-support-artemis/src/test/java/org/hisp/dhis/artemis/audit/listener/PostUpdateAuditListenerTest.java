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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
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
          "firstName",
          "lastLogin",
          "lastUpdated",
          "lastUpdatedBy",
          "lastCheckedInterpretations",
          "organisationUnits");

  private static final int COLLECTION_INDEX = PROPERTY_NAMES.indexOf("organisationUnits");

  private enum CollectionChange {
    NONE,
    IN_PLACE,
    REPLACED
  }

  private AuditManager auditManager;

  private ActionQueue actionQueue;

  private EventSource session;

  private EntityPersister persister;

  private PostUpdateAuditListener listener;

  @BeforeEach
  void setUp() {
    auditManager = mock(AuditManager.class);
    actionQueue = mock(ActionQueue.class);
    session = mock(EventSource.class);
    when(session.getActionQueue()).thenReturn(actionQueue);

    Type scalarType = mock(Type.class);
    Type collectionType = mock(Type.class);
    when(collectionType.isCollectionType()).thenReturn(true);
    Type[] types = new Type[PROPERTY_NAMES.size()];
    Arrays.fill(types, scalarType);
    types[COLLECTION_INDEX] = collectionType;

    persister = mock(EntityPersister.class);
    when(persister.getPropertyNames()).thenReturn(PROPERTY_NAMES.toArray(String[]::new));
    when(persister.getPropertyTypes()).thenReturn(types);

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
    listener.onPostUpdate(updateEvent(dirtyProperties, CollectionChange.NONE));

    verifyNoInteractions(actionQueue);
    verify(auditManager, never()).send(any());
  }

  static Stream<Arguments> realChanges() {
    return Stream.of(
        Arguments.of(Set.of("firstName"), CollectionChange.NONE),
        Arguments.of(Set.of("firstName", "lastLogin", "lastUpdated"), CollectionChange.NONE),
        Arguments.of(Set.of("lastUpdated", "lastUpdatedBy"), CollectionChange.IN_PLACE),
        Arguments.of(Set.of("lastUpdated", "lastUpdatedBy"), CollectionChange.REPLACED));
  }

  @ParameterizedTest
  @MethodSource("realChanges")
  void testRealChangeIsAuditedAfterCommit(
      Set<String> dirtyProperties, CollectionChange collectionChange) {
    listener.onPostUpdate(updateEvent(dirtyProperties, collectionChange));

    AfterTransactionCompletionProcess process = registeredProcess();
    verify(auditManager, never()).send(any());

    process.doAfterTransactionCompletion(true, session);

    ArgumentCaptor<Audit> audit = ArgumentCaptor.forClass(Audit.class);
    verify(auditManager).send(audit.capture());
    assertEquals(AuditType.UPDATE, audit.getValue().getAuditType());
    assertEquals(User.class.getName(), audit.getValue().getKlass());
  }

  @Test
  void testRealChangeIsNotAuditedWhenTransactionFails() {
    listener.onPostUpdate(updateEvent(Set.of("firstName"), CollectionChange.NONE));

    registeredProcess().doAfterTransactionCompletion(false, session);

    verify(auditManager, never()).send(any());
  }

  @Test
  void testUpdateWithUnknownDirtyPropertiesIsAudited() {
    Object[] state = new Object[PROPERTY_NAMES.size()];
    listener.onPostUpdate(
        new PostUpdateEvent(new User(), 1L, state, state.clone(), null, persister, session));

    registeredProcess();
  }

  private AfterTransactionCompletionProcess registeredProcess() {
    ArgumentCaptor<AfterTransactionCompletionProcess> process =
        ArgumentCaptor.forClass(AfterTransactionCompletionProcess.class);
    verify(actionQueue).registerProcess(process.capture());
    return process.getValue();
  }

  private PostUpdateEvent updateEvent(
      Set<String> dirtyProperties, CollectionChange collectionChange) {
    PersistentCollection loaded = mock(PersistentCollection.class);
    PersistentCollection current =
        collectionChange == CollectionChange.REPLACED ? mock(PersistentCollection.class) : loaded;
    when(current.isDirty()).thenReturn(collectionChange == CollectionChange.IN_PLACE);

    Object[] oldState = new Object[PROPERTY_NAMES.size()];
    Object[] state = new Object[PROPERTY_NAMES.size()];
    oldState[COLLECTION_INDEX] = loaded;
    state[COLLECTION_INDEX] = current;

    int[] dirty = dirtyProperties.stream().mapToInt(PROPERTY_NAMES::indexOf).toArray();

    return new PostUpdateEvent(new User(), 1L, state, oldState, dirty, persister, session);
  }
}
