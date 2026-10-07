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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditHibernateListenerTest {
  @InjectMocks private PostInsertAuditListener postInsertAuditListener;

  @Mock private AuditManager auditManager;

  @Mock private AuditObjectFactory objectFactory;

  @Mock private AuditMatrix auditMatrix;

  @Mock private UsernameSupplier usernameSupplier;

  @Mock private SchemaService schemaService;

  @Test
  @SuppressWarnings("unchecked")
  void testSavePassword() {
    User user = createUser(1, "userA", "passwordA");

    Object[] state = new Object[] {"userA", "passwordA"};
    EventSource session = mock(EventSource.class);
    EntityPersister persister = mock(EntityPersister.class);
    when(persister.getPropertyNames()).thenReturn(new String[] {"userName", "password"});

    Schema schema = mock(Schema.class);

    HashMap<String, Property> map = new HashMap<>();
    map.put("userName", createProperty("userName", true));
    // password property has readable = false
    map.put("password", createProperty("password", false));

    when(schema.getFieldNameMapProperties()).thenReturn(map);
    when(schemaService.getSchema(User.class)).thenReturn(schema);
    Map<String, Object> auditObjectMap =
        (Map<String, Object>)
            postInsertAuditListener.createAuditEntry(user, state, session, 1, persister);

    // password is not included
    assertNull(auditObjectMap.get("password"));
    assertEquals(1, auditObjectMap.size());
    assertEquals("userA", auditObjectMap.get("userName").toString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testUnloadedCollectionIsNotLoadedOrIncluded() {
    OrganisationUnitGroup group = new OrganisationUnitGroup("groupA");
    // A collection that was not loaded by the request. It has no session, so any read of it
    // (size, isEmpty, iteration) throws LazyInitializationException
    PersistentSet members = new PersistentSet(null);

    Object[] state = new Object[] {"groupA", members};
    EventSource session = mock(EventSource.class);
    EntityPersister persister = mock(EntityPersister.class);
    when(persister.getPropertyNames()).thenReturn(new String[] {"name", "members"});
    // Reading the property through an entity proxy returns the same unloaded collection
    HibernateProxy proxy = mock(HibernateProxy.class);
    lenient().when(persister.createProxy(1, session)).thenReturn(proxy);
    lenient().when(persister.getPropertyValue(proxy, "members")).thenReturn(members);

    mockSchema(
        OrganisationUnitGroup.class,
        createProperty("name", true),
        createCollectionProperty("members", OrganisationUnit.class));

    Map<String, Object> auditObjectMap =
        (Map<String, Object>)
            postInsertAuditListener.createAuditEntry(group, state, session, 1, persister);

    assertFalse(members.wasInitialized());
    assertFalse(auditObjectMap.containsKey("members"));
    assertEquals("groupA", auditObjectMap.get("name"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadedCollectionIsIncludedAsUids() {
    OrganisationUnitGroup group = new OrganisationUnitGroup("groupA");
    OrganisationUnit unit = new OrganisationUnit("unitA");
    unit.setUid("ouabcdefghA");
    PersistentSet members = new PersistentSet(null, Set.of(unit));

    Object[] state = new Object[] {"groupA", members};
    EventSource session = mock(EventSource.class);
    EntityPersister persister = mock(EntityPersister.class);
    when(persister.getPropertyNames()).thenReturn(new String[] {"name", "members"});

    mockSchema(
        OrganisationUnitGroup.class,
        createProperty("name", true),
        createCollectionProperty("members", OrganisationUnit.class));

    Map<String, Object> auditObjectMap =
        (Map<String, Object>)
            postInsertAuditListener.createAuditEntry(group, state, session, 1, persister);

    assertEquals(List.of("ouabcdefghA"), auditObjectMap.get("members"));
  }

  private void mockSchema(Class<?> klass, Property... properties) {
    Map<String, Property> map = new HashMap<>();
    for (Property property : properties) {
      map.put(property.getFieldName(), property);
    }
    Schema schema = mock(Schema.class);
    when(schema.getFieldNameMapProperties()).thenReturn(map);
    when(schemaService.getSchema(klass)).thenReturn(schema);
  }

  private Property createCollectionProperty(String name, Class<?> itemKlass) {
    Property property = new Property(Set.class);
    property.setReadable(true);
    property.setFieldName(name);
    property.setOwner(true);
    property.setEmbeddedObject(false);
    property.setCollection(true);
    property.setItemKlass(itemKlass);
    return property;
  }

  private User createUser(int id, String userName, String password) {
    User user = new User();
    user.setId(id);
    user.setUsername(userName);
    user.setPassword(password);
    return user;
  }

  private Property createProperty(String name, boolean readable) {
    Property property = new Property(String.class);
    property.setReadable(readable);
    property.setFieldName(name);
    property.setOwner(true);
    property.setEmbeddedObject(false);
    return property;
  }
}
