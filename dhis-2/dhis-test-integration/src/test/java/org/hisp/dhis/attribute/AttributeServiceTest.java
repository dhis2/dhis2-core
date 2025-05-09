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
package org.hisp.dhis.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class AttributeServiceTest extends PostgresIntegrationTestBase {
  @Autowired private AttributeService attributeService;

  @Test
  void testAddAttribute() {
    Attribute attribute = createAttribute("attribute1", ValueType.TEXT);
    attributeService.addAttribute(attribute);
    attribute = attributeService.getAttribute(attribute.getUid());
    assertNotNull(attribute);
    assertEquals(ValueType.TEXT, attribute.getValueType());
    assertEquals("attribute1", attribute.getName());
  }

  @Test
  void testDeleteAttribute() {
    Attribute attribute = createAttribute("attribute1", ValueType.TEXT);
    attributeService.addAttribute(attribute);
    attribute = attributeService.getAttribute(attribute.getUid());
    assertNotNull(attribute);
    attributeService.deleteAttribute(attribute);
    attribute = attributeService.getAttribute(attribute.getUid());
    assertNull(attribute);
  }

  @Test
  void testGetAttribute() {
    Attribute attribute = createAttribute("attribute1", ValueType.TEXT);
    attributeService.addAttribute(attribute);
    attribute = attributeService.getAttribute(attribute.getUid());
    assertNotNull(attribute);
  }

  @Test
  void testGetAllAttributes() {
    Attribute attribute1 = createAttribute("attribute1", ValueType.TEXT);
    Attribute attribute2 = createAttribute("attribute2", ValueType.TEXT);
    attributeService.addAttribute(attribute1);
    attributeService.addAttribute(attribute2);
    assertEquals(2, attributeService.getAllAttributes().size());
  }

  @Test
  void testGeoJSONAttribute() {
    Attribute attribute = createAttribute("attributeGeoJson", ValueType.GEOJSON);
    attributeService.addAttribute(attribute);

    attribute = attributeService.getAttributeByName(attribute.getName());
    assertNotNull(attribute);
    assertTrue(attribute.getValueType().isGeo());
    assertEquals(attribute.getValueType(), ValueType.GEOJSON);
  }
}
