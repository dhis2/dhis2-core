/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.config.sqlobserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hisp.dhis.config.sqlobserver.HibernateTableEntityRegistry.TableInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class HibernateTableEntityRegistryTest {

  @Test
  void getTableInfo_returnsNullForUnmappedTable() {
    ObjectProvider<EntityManagerFactory> emfProvider = createMockEmfProvider();
    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(emfProvider);

    assertNull(registry.getTableInfo("nonexistent_table"));
  }

  @Test
  void getTableInfo_returnsMappedEntity() {
    AbstractEntityPersister persister = mock(AbstractEntityPersister.class);
    when(persister.getTableName()).thenReturn("dataelement");
    when(persister.getIdentifierColumnNames()).thenReturn(new String[] {"dataelementid"});
    when(persister.getMappedClass()).thenReturn((Class) TestEntity.class);

    ObjectProvider<EntityManagerFactory> emfProvider =
        createMockEmfProviderWithPersisters(Map.of("TestEntity", persister));
    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(emfProvider);

    TableInfo info = registry.getTableInfo("dataelement");
    assertNotNull(info);
    assertEquals(TestEntity.class, info.entityClass());
    assertEquals(java.util.List.of("dataelementid"), info.pkColumnNames());
    assertEquals("dataelement", info.tableName());
  }

  @Test
  void getTableInfo_stripsSchemaPrefix() {
    AbstractEntityPersister persister = mock(AbstractEntityPersister.class);
    when(persister.getTableName()).thenReturn("public.dataelement");
    when(persister.getIdentifierColumnNames()).thenReturn(new String[] {"dataelementid"});
    when(persister.getMappedClass()).thenReturn((Class) TestEntity.class);

    ObjectProvider<EntityManagerFactory> emfProvider =
        createMockEmfProviderWithPersisters(Map.of("TestEntity", persister));
    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(emfProvider);

    TableInfo info = registry.getTableInfo("dataelement");
    assertNotNull(info);
    assertEquals(TestEntity.class, info.entityClass());
  }

  @Test
  void getTableInfo_caseInsensitive() {
    AbstractEntityPersister persister = mock(AbstractEntityPersister.class);
    when(persister.getTableName()).thenReturn("DataElement");
    when(persister.getIdentifierColumnNames()).thenReturn(new String[] {"dataelementid"});
    when(persister.getMappedClass()).thenReturn((Class) TestEntity.class);

    ObjectProvider<EntityManagerFactory> emfProvider =
        createMockEmfProviderWithPersisters(Map.of("TestEntity", persister));
    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(emfProvider);

    assertNotNull(registry.getTableInfo("dataelement"));
    assertNotNull(registry.getTableInfo("DATAELEMENT"));
  }

  @Test
  void getTableInfo_lazyInitializationOnlyOnFirstCall() {
    AbstractEntityPersister persister = mock(AbstractEntityPersister.class);
    when(persister.getTableName()).thenReturn("dataelement");
    when(persister.getIdentifierColumnNames()).thenReturn(new String[] {"dataelementid"});
    when(persister.getMappedClass()).thenReturn((Class) TestEntity.class);

    ObjectProvider<EntityManagerFactory> emfProvider =
        createMockEmfProviderWithPersisters(Map.of("TestEntity", persister));
    HibernateTableEntityRegistry registry = new HibernateTableEntityRegistry(emfProvider);

    // First call triggers initialization
    TableInfo info1 = registry.getTableInfo("dataelement");
    // Second call reuses cached map
    TableInfo info2 = registry.getTableInfo("dataelement");

    assertNotNull(info1);
    assertEquals(info1, info2);
  }

  // ── Helpers ──

  @SuppressWarnings("unchecked")
  private ObjectProvider<EntityManagerFactory> createMockEmfProvider() {
    return createMockEmfProviderWithPersisters(Map.of());
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<EntityManagerFactory> createMockEmfProviderWithPersisters(
      Map<String, AbstractEntityPersister> persisters) {
    EntityManagerFactory emf = mock(EntityManagerFactory.class);
    SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
    MetamodelImplementor metamodel = mock(MetamodelImplementor.class);

    when(emf.unwrap(SessionFactoryImplementor.class)).thenReturn(sf);
    when(sf.getMetamodel()).thenReturn(metamodel);
    when(metamodel.entityPersisters()).thenReturn((Map) persisters);

    ObjectProvider<EntityManagerFactory> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(emf);
    return provider;
  }

  /** Dummy entity class for testing. */
  private static class TestEntity {}
}
