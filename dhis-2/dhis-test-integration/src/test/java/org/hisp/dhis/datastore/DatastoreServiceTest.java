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
package org.hisp.dhis.datastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class DatastoreServiceTest extends SingleSetupIntegrationTestBase {

  private final String namespace = "DOGS";

  @Autowired private DatastoreService service;

  @Autowired private ObjectMapper jsonMapper;

  @Test
  void testAddGetObject() {
    Dog dogA = new Dog("1", "Fido", "Brown");
    Dog dogB = new Dog("2", "Aldo", "Black");
    addValue(namespace, dogA.getId(), dogA);
    addValue(namespace, dogB.getId(), dogB);
    dogA = getValue(namespace, dogA.getId(), Dog.class);
    dogB = getValue(namespace, dogB.getId(), Dog.class);
    assertNotNull(dogA);
    assertEquals("1", dogA.getId());
    assertEquals("Fido", dogA.getName());
    assertNotNull(dogB);
    assertEquals("2", dogB.getId());
    assertEquals("Aldo", dogB.getName());
  }

  @Test
  void testAddUpdateObject() {
    Dog dogA = new Dog("1", "Fido", "Brown");
    Dog dogB = new Dog("2", "Aldo", "Black");
    addValue(namespace, dogA.getId(), dogA);
    addValue(namespace, dogB.getId(), dogB);
    dogA = getValue(namespace, dogA.getId(), Dog.class);
    dogB = getValue(namespace, dogB.getId(), Dog.class);
    assertEquals("Fido", dogA.getName());
    assertEquals("Aldo", dogB.getName());
    dogA.setName("Lilly");
    dogB.setName("Teddy");
    updateValue(namespace, dogA.getId(), dogA);
    updateValue(namespace, dogB.getId(), dogB);
    dogA = getValue(namespace, dogA.getId(), Dog.class);
    dogB = getValue(namespace, dogB.getId(), Dog.class);
    assertEquals("Lilly", dogA.getName());
    assertEquals("Teddy", dogB.getName());
  }

  private <T> T getValue(String namespace, String key, Class<T> type) {
    return mapJsonValueTo(type, service.getEntry(namespace, key));
  }

  private <T> DatastoreEntry addValue(String namespace, String key, T object) {
    DatastoreEntry entry = new DatastoreEntry(namespace, key, mapValueToJson(object), false);
    service.addEntry(entry);
    return entry;
  }

  public <T> void updateValue(String namespace, String key, T object) {
    DatastoreEntry entry = service.getEntry(namespace, key);
    if (entry == null) {
      throw new IllegalStateException(
          String.format("No object found for namespace '%s' and key '%s'", namespace, key));
    }
    entry.setValue(mapValueToJson(object));
    service.updateEntry(entry);
  }

  private <T> T mapJsonValueTo(Class<T> type, DatastoreEntry entry) {
    if (entry == null || entry.getJbPlainValue() == null) {
      return null;
    }
    try {
      return jsonMapper.readValue(entry.getJbPlainValue(), type);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private <T> String mapValueToJson(T object) {
    try {
      return jsonMapper.writeValueAsString(object);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
