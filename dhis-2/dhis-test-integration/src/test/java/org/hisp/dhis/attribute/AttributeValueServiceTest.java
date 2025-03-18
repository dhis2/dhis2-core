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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
class AttributeValueServiceTest extends PostgresIntegrationTestBase {

  @Autowired private AttributeService attributeService;

  @Autowired private DataElementStore dataElementStore;

  @Autowired private CategoryService _categoryService;

  @Autowired private IdentifiableObjectManager manager;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private Attribute attribute1;

  private Attribute attribute2;

  private User currentUser;

  @BeforeEach
  void setUp() {
    categoryService = _categoryService;
    currentUser = getCurrentUser();
    attribute1 = new Attribute("attribute 1", ValueType.TEXT);
    attribute1.setDataElementAttribute(true);
    attribute2 = new Attribute("attribute 2", ValueType.TEXT);
    attribute2.setDataElementAttribute(true);
    Attribute attribute3 = new Attribute("attribute 3", ValueType.TEXT);
    attribute3.setDataElementAttribute(true);
    attributeService.addAttribute(attribute1);
    attributeService.addAttribute(attribute2);
    attributeService.addAttribute(attribute3);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementC = createDataElement('C');
    manager.save(dataElementA);
    manager.save(dataElementB);
    manager.save(dataElementC);
  }

  @Test
  void testAddAttributeValue() {
    attributeService.addAttributeValue(dataElementA, attribute1.getUid(), "valueA");
    attributeService.addAttributeValue(dataElementB, attribute2.getUid(), "valueB");
    assertEquals(1, dataElementA.getAttributeValues().size());
    assertNotNull(dataElementA.getAttributeValue(attribute1.getUid()));
    assertEquals(1, dataElementB.getAttributeValues().size());
    assertNotNull(dataElementB.getAttributeValue(attribute2.getUid()));
  }

  @Test
  void testGetAttributeValue() {
    attributeService.addAttributeValue(dataElementA, attribute1.getUid(), "valueA");
    attributeService.addAttributeValue(dataElementB, attribute2.getUid(), "valueB");
    assertNotNull(dataElementA.getAttributeValue(attribute1.getUid()));
    List<DataElement> list = dataElementStore.getByAttributeAndValue(UID.of(attribute1), "valueA");
    assertNotNull(list);
    assertEquals(1, list.size());
    assertEquals(dataElementA, list.get(0));
  }

  @Test
  void testGetAllByAttribute() {
    attributeService.addAttributeValue(dataElementA, attribute1.getUid(), "valueA");
    attributeService.addAttributeValue(dataElementA, attribute2.getUid(), "valueB");
    attributeService.addAttributeValue(dataElementB, attribute2.getUid(), "valueB");
    manager.update(dataElementA);
    manager.update(dataElementB);
    List<DataElement> result =
        manager.getAllByAttributes(DataElement.class, UID.of(attribute1, attribute2));
    assertEquals(2, result.size());
  }

  @Test
  void testAddNonUniqueAttributeValue() throws NonUniqueAttributeValueException {
    Attribute attribute = new Attribute("ID", ValueType.TEXT);
    attribute.setUnique(true);
    attribute.setDataElementAttribute(true);
    attributeService.addAttribute(attribute);
    attributeService.addAttributeValue(dataElementA, attribute.getUid(), "A");
    assertDoesNotThrow(
        () -> attributeService.addAttributeValue(dataElementB, attribute.getUid(), "B"));
  }

  @Test
  void testAddUniqueAttributeValue() {
    Attribute attribute = new Attribute("ID", ValueType.TEXT);
    attribute.setUnique(true);
    attribute.setDataElementAttribute(true);
    attributeService.addAttribute(attribute);
    attributeService.addAttributeValue(dataElementA, attribute.getUid(), "A");
    assertThrows(
        NonUniqueAttributeValueException.class,
        () -> attributeService.addAttributeValue(dataElementB, attribute.getUid(), "A"));
  }

  @Test
  void testDataElementByUniqueAttributeValue() throws NonUniqueAttributeValueException {
    Attribute attribute = new Attribute("cid", ValueType.TEXT);
    attribute.setDataElementAttribute(true);
    attribute.setUnique(true);
    attributeService.addAttribute(attribute);
    attributeService.addAttributeValue(dataElementA, attribute.getUid(), "CID1");
    attributeService.addAttributeValue(dataElementB, attribute.getUid(), "CID2");
    attributeService.addAttributeValue(dataElementC, attribute.getUid(), "CID3");

    UserDetails userDetails = userService.createUserDetails(currentUser);
    UID attributeId = UID.of(attribute);
    DataElement deA = dataElementStore.getByUniqueAttributeValue(attributeId, "CID1", userDetails);
    DataElement deB = dataElementStore.getByUniqueAttributeValue(attributeId, "CID2", userDetails);
    DataElement deC = dataElementStore.getByUniqueAttributeValue(attributeId, "CID3", userDetails);
    assertNotNull(deA);
    assertNotNull(deB);
    assertNotNull(deC);
    assertNull(dataElementStore.getByUniqueAttributeValue(attributeId, "CID4", userDetails));
    assertNull(dataElementStore.getByUniqueAttributeValue(attributeId, "CID5", userDetails));
    assertEquals("DataElementA", deA.getName());
    assertEquals("DataElementB", deB.getName());
    assertEquals("DataElementC", deC.getName());
  }

  @Test
  void testUniqueAttributesWithSameValues() {
    Attribute attributeA = new Attribute("ATTRIBUTEA", ValueType.TEXT);
    attributeA.setDataElementAttribute(true);
    attributeA.setUnique(true);
    attributeService.addAttribute(attributeA);
    Attribute attributeB = new Attribute("ATTRIBUTEB", ValueType.TEXT);
    attributeB.setDataElementAttribute(true);
    attributeB.setUnique(true);
    attributeService.addAttribute(attributeB);
    Attribute attributeC = new Attribute("ATTRIBUTEC", ValueType.TEXT);
    attributeC.setDataElementAttribute(true);
    attributeC.setUnique(true);
    attributeService.addAttribute(attributeC);
    attributeService.addAttributeValue(dataElementA, attributeA.getUid(), "VALUE");
    attributeService.addAttributeValue(dataElementB, attributeB.getUid(), "VALUE");
    attributeService.addAttributeValue(dataElementC, attributeC.getUid(), "VALUE");
    assertEquals(1, dataElementA.getAttributeValues().size());
    assertEquals(1, dataElementB.getAttributeValues().size());
    assertEquals(1, dataElementC.getAttributeValues().size());

    UserDetails userDetails = userService.createUserDetails(currentUser);
    DataElement de1 =
        dataElementStore.getByUniqueAttributeValue(UID.of(attributeA), "VALUE", userDetails);
    DataElement de2 =
        dataElementStore.getByUniqueAttributeValue(UID.of(attributeB), "VALUE", userDetails);
    DataElement de3 =
        dataElementStore.getByUniqueAttributeValue(UID.of(attributeC), "VALUE", userDetails);
    assertNotNull(de1);
    assertNotNull(de2);
    assertNotNull(de3);
    assertEquals("DataElementA", de1.getName());
    assertEquals("DataElementB", de2.getName());
    assertEquals("DataElementC", de3.getName());
  }

  @Test
  void testDeleteAttributeWithReferences() {
    attributeService.addAttributeValue(dataElementA, attribute1.getUid(), "valueA");
    assertEquals(
        1, manager.countAllValuesByAttributes(DataElement.class, List.of(UID.of(attribute1))));
    assertThrows(
        DeleteNotAllowedException.class, () -> attributeService.deleteAttribute(attribute1));
  }

  @Test
  void testGeoJSONAttributeValue() throws JsonProcessingException {
    Attribute attribute = new Attribute();
    attribute.setValueType(ValueType.GEOJSON);
    attribute.setName("attributeGeoJson");
    attributeService.addAttribute(attribute);

    String geoJson =
        "{\"type\": \"Feature\", \"geometry\": { \"type\": \"Point\","
            + "\"coordinates\": [125.6, 10.1] }, \"properties\": { \"name\": \"Dinagat Islands\" } }";

    dataElementA.addAttributeValue(attribute.getUid(), geoJson);
    dataElementStore.save(dataElementA);

    List<DataElement> dataElements = dataElementStore.getByAttribute(UID.of(attribute));
    assertEquals(1, dataElements.size());
    assertEquals(dataElementA.getUid(), dataElements.get(0).getUid());

    dataElements = dataElementStore.getByAttributeAndValue(UID.of(attribute), geoJson);
    assertEquals(1, dataElements.size());
    assertEquals(dataElementA.getUid(), dataElements.get(0).getUid());

    DataElement dataElement = dataElements.get(0);
    String value = dataElement.getAttributeValues().get(attribute.getUid());
    GeoJsonObject geoJsonObject = new ObjectMapper().readValue(value, GeoJsonObject.class);
    assertInstanceOf(Feature.class, geoJsonObject);
  }
}
