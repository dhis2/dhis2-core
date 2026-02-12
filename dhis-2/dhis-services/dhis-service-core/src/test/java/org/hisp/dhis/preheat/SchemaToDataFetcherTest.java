/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.preheat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.query.Query;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.preheat.SchemaToDataFetcher.UniqueFields;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@SuppressWarnings("unchecked")
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SchemaToDataFetcherTest extends TestBase {

  private SchemaToDataFetcher subject;

  @Mock private EntityManager entityManager;

  @Mock private Query query;

  @BeforeEach
  public void setUp() {
    subject = new SchemaToDataFetcher(entityManager);
  }

  @Test
  void verifyNullSchemaReturnsEmpty() {
    List<UniqueFields> result = subject.fetch(null, List.of());
    assertThat(result, hasSize(0));
  }

  @Test
  void verifyNullObjectsReturnsEmpty() {
    Schema schema = createSchema(DataElement.class, "dataElement");
    List<UniqueFields> result = subject.fetch(schema, null);
    assertThat(result, hasSize(0));
  }

  @Test
  void verifyEmptyObjectsReturnsEmpty() {
    Schema schema = createSchema(DataElement.class, "dataElement");
    List<UniqueFields> result = subject.fetch(schema, List.of());
    assertThat(result, hasSize(0));
    verify(entityManager, times(0)).createQuery(anyString());
  }

  @Test
  void verifyFetchWithUidsAndCodes() {
    Schema schema = createSchema(DataElement.class, "dataElement");

    DataElement de1 = new DataElement("DE1");
    de1.setUid("abcdefghij1");
    de1.setCode("CODE_1");
    DataElement de2 = new DataElement("DE2");
    de2.setUid("abcdefghij2");
    de2.setCode("CODE_2");

    mockQuery();

    List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[] {"abcdefghij1", "CODE_1"});
    rows.add(new Object[] {"abcdefghij2", null});
    when(query.getResultList()).thenReturn(rows);

    List<UniqueFields> result = subject.fetch(schema, List.of(de1, de2));

    assertThat(result, hasSize(2));
    assertEquals(UID.of("abcdefghij1"), result.get(0).uid());
    assertEquals("CODE_1", result.get(0).code());
    assertEquals(UID.of("abcdefghij2"), result.get(1).uid());
    assertNull(result.get(1).code());
  }

  @Test
  void verifyFetchWithOnlyUids() {
    Schema schema = createSchema(DataElement.class, "dataElement");

    DataElement de1 = new DataElement("DE1");
    de1.setUid("abcdefghij1");

    mockQuery();

    List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[] {"abcdefghij1", "EXISTING_CODE"});
    when(query.getResultList()).thenReturn(rows);

    List<UniqueFields> result = subject.fetch(schema, List.of(de1));

    assertThat(result, hasSize(1));
    assertEquals(UID.of("abcdefghij1"), result.get(0).uid());
    assertEquals("EXISTING_CODE", result.get(0).code());

    ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(entityManager).createQuery(hqlCaptor.capture());
    String hql = hqlCaptor.getValue();
    assertEquals("SELECT uid, code FROM DataElement WHERE uid IN (:uids)", hql);
  }

  @Test
  void verifyFetchWithOnlyCodes() {
    Schema schema = createSchema(DataElement.class, "dataElement");

    DataElement de1 = new DataElement("DE1");
    de1.setCode("CODE_1");

    mockQuery();

    List<Object[]> rows = new ArrayList<>();
    rows.add(new Object[] {"xyzxyzxyz11", "CODE_1"});
    when(query.getResultList()).thenReturn(rows);

    List<UniqueFields> result = subject.fetch(schema, List.of(de1));

    assertThat(result, hasSize(1));
    assertEquals(UID.of("xyzxyzxyz11"), result.get(0).uid());
    assertEquals("CODE_1", result.get(0).code());

    ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(entityManager).createQuery(hqlCaptor.capture());
    String hql = hqlCaptor.getValue();
    assertEquals("SELECT uid, code FROM DataElement WHERE code IN (:codes)", hql);
  }

  @Test
  void verifyNoSqlWhenObjectsHaveNoUidOrCode() {
    Schema schema = createSchema(DataElement.class, "dataElement");

    DataElement de1 = new DataElement("DE1");

    List<UniqueFields> result = subject.fetch(schema, List.of(de1));

    assertThat(result, hasSize(0));
    verify(entityManager, times(0)).createQuery(anyString());
  }

  private void mockQuery() {
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.setHint(any(), any())).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
  }

  private Schema createSchema(
      Class<? extends org.hisp.dhis.common.IdentifiableObject> klass, String singularName) {
    return new Schema(klass, singularName, singularName + "s");
  }
}
