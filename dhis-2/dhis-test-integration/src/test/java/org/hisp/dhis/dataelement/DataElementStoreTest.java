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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Torgeir Lorange Ostby
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataElementStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementStore dataElementStore;

  @Autowired private AttributeService attributeService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private DbmsManager dbmsManager;

  @Test
  void testAddDataElement() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementStore.save(dataElementA);
    long idA = dataElementA.getId();
    dataElementStore.save(dataElementB);
    long idB = dataElementB.getId();
    dataElementStore.save(dataElementC);
    long idC = dataElementC.getId();
    dataElementA = dataElementStore.get(idA);
    assertNotNull(dataElementA);
    assertEquals(idA, dataElementA.getId());
    assertEquals("DataElementA", dataElementA.getName());
    dataElementB = dataElementStore.get(idB);
    assertNotNull(dataElementB);
    assertEquals(idB, dataElementB.getId());
    assertEquals("DataElementB", dataElementB.getName());
    dataElementC = dataElementStore.get(idC);
    assertNotNull(dataElementC);
    assertEquals(idC, dataElementC.getId());
    assertEquals("DataElementC", dataElementC.getName());
  }

  @Test
  void testUpdateDataElement() {
    DataElement dataElementA = createDataElement('A');
    dataElementStore.save(dataElementA);
    long idA = dataElementA.getId();
    dataElementA = dataElementStore.get(idA);
    assertEquals(ValueType.INTEGER, dataElementA.getValueType());
    dataElementA.setValueType(ValueType.BOOLEAN);
    dataElementStore.update(dataElementA);
    dataElementA = dataElementStore.get(idA);
    assertNotNull(dataElementA.getValueType());
    assertEquals(ValueType.BOOLEAN, dataElementA.getValueType());
  }

  @Test
  void testGetDataElementByName() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementStore.save(dataElementA);
    long idA = dataElementA.getId();
    dataElementStore.save(dataElementB);
    long idB = dataElementB.getId();
    dataElementA = dataElementStore.getByName("DataElementA");
    assertNotNull(dataElementA);
    assertEquals(idA, dataElementA.getId());
    assertEquals("DataElementA", dataElementA.getName());
    dataElementB = dataElementStore.getByName("DataElementB");
    assertNotNull(dataElementB);
    assertEquals(idB, dataElementB.getId());
    assertEquals("DataElementB", dataElementB.getName());
    DataElement dataElementC = dataElementStore.getByName("DataElementC");
    assertNull(dataElementC);
  }

  @Test
  void testGetAllDataElements() {
    assertEquals(0, dataElementStore.getAll().size());
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    DataElement dataElementD = createDataElement('D');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    dataElementStore.save(dataElementD);
    List<DataElement> dataElementsRef = new ArrayList<>();
    dataElementsRef.add(dataElementA);
    dataElementsRef.add(dataElementB);
    dataElementsRef.add(dataElementC);
    dataElementsRef.add(dataElementD);
    List<DataElement> dataElements = dataElementStore.getAll();
    assertNotNull(dataElements);
    assertEquals(dataElementsRef.size(), dataElements.size());
    assertTrue(dataElements.containsAll(dataElementsRef));
  }

  @Test
  void testGetDataElementAggregationLevels() {
    List<Integer> aggregationLevels = Arrays.asList(3, 5);
    DataElement dataElementA = createDataElement('A');
    dataElementA.setAggregationLevels(aggregationLevels);
    dataElementStore.save(dataElementA);
    long idA = dataElementA.getId();
    assertNotNull(dataElementStore.get(idA).getAggregationLevels());
    assertEquals(2, dataElementStore.get(idA).getAggregationLevels().size());
    assertEquals(aggregationLevels, dataElementStore.get(idA).getAggregationLevels());
  }

  @Test
  void testGetDataElementsWithoutGroups() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    DataElement dataElementD = createDataElement('D');
    DataElement dataElementE = createDataElement('E');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    dataElementStore.save(dataElementD);
    dataElementStore.save(dataElementE);
    DataElementGroup dgA = createDataElementGroup('A');
    dgA.addDataElement(dataElementA);
    dgA.addDataElement(dataElementD);
    idObjectManager.save(dgA);
    List<DataElement> dataElements = dataElementStore.getDataElementsWithoutGroups();
    assertEquals(3, dataElements.size());
    assertTrue(dataElements.contains(dataElementB));
    assertTrue(dataElements.contains(dataElementC));
    assertTrue(dataElements.contains(dataElementE));
  }

  @Test
  void testGetDataElementsByAggregationLevel() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementA.getAggregationLevels().addAll(Arrays.asList(3, 5));
    dataElementB.getAggregationLevels().addAll(Arrays.asList(4, 5));
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    List<DataElement> dataElements = dataElementStore.getDataElementsByAggregationLevel(2);
    assertEquals(0, dataElements.size());
    dataElements = dataElementStore.getDataElementsByAggregationLevel(3);
    assertEquals(1, dataElements.size());
    dataElements = dataElementStore.getDataElementsByAggregationLevel(4);
    assertEquals(1, dataElements.size());
    dataElements = dataElementStore.getDataElementsByAggregationLevel(5);
    assertEquals(2, dataElements.size());
    assertTrue(dataElements.contains(dataElementA));
    assertTrue(dataElements.contains(dataElementB));
  }

  @Test
  void testDataElementByNonUniqueAttributeValue() throws NonUniqueAttributeValueException {
    Attribute attribute = new Attribute("cid", ValueType.TEXT);
    attribute.setDataElementAttribute(true);
    attributeService.addAttribute(attribute);
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    attributeService.addAttributeValue(dataElementA, attribute.getUid(), "CID1");
    attributeService.addAttributeValue(dataElementB, attribute.getUid(), "CID2");
    attributeService.addAttributeValue(dataElementC, attribute.getUid(), "CID3");
    dataElementStore.update(dataElementA);
    dataElementStore.update(dataElementB);
    dataElementStore.update(dataElementC);
    UID attributeId = UID.of(attribute);
    assertNull(dataElementStore.getByUniqueAttributeValue(attributeId, "CID1"));
    assertNull(dataElementStore.getByUniqueAttributeValue(attributeId, "CID2"));
    assertNull(dataElementStore.getByUniqueAttributeValue(attributeId, "CID3"));
  }

  @Test
  void testGetLastUpdated() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    Date lastUpdated = dataElementStore.getLastUpdated();

    dbmsManager.clearSession();

    dataElementA.setDescription("testA");
    dataElementStore.update(dataElementA);
    dataElementB.setDescription("testB");
    dataElementStore.update(dataElementB);
    assertNotEquals(lastUpdated, dataElementStore.getLastUpdated());
  }

  @Test
  void testCountByJpaQueryParameters() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    assertEquals(2, dataElementStore.getCount());
  }

  @Test
  void testGetDataElements() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    assertNotNull(dataElementStore.getByUid(dataElementA.getUid()));
    assertNotNull(dataElementStore.getByUidNoAcl(dataElementA.getUid()));
    List<String> uids = new ArrayList<>();
    uids.add(dataElementA.getUid());
    uids.add(dataElementB.getUid());
    assertNotNull(dataElementStore.getByUidNoAcl(uids));
    assertNotNull(dataElementStore.getByName(dataElementA.getName()));
    assertNotNull(dataElementStore.getByCode(dataElementA.getCode()));
    assertEquals(1, dataElementStore.getAllEqName("DataElementA").size());
    assertEquals(2, dataElementStore.getAllLikeName("DataElement").size());
  }

  @Test
  void testGetDataElementsByUidNoAcl() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    List<String> uids =
        List.of(dataElementA.getUid(), dataElementB.getUid(), dataElementC.getUid());
    List<DataElement> dataElements = dataElementStore.getByUidNoAcl(uids);
    assertEquals(3, dataElements.size());
    assertTrue(dataElements.contains(dataElementA));
    assertTrue(dataElements.contains(dataElementB));
    assertTrue(dataElements.contains(dataElementC));
  }

  @Test
  void testLoadDataElementByUid() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    assertNotNull(dataElementStore.loadByUid(dataElementA.getUid()));
    assertNotNull(dataElementStore.loadByUid(dataElementB.getUid()));
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> dataElementStore.loadByUid("nonExisting"));
    assertEquals(ErrorCode.E1113, ex.getErrorCode());
  }

  @Test
  void testLoadDataElementByCode() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    dataElementStore.save(dataElementC);
    assertNotNull(dataElementStore.loadByCode(dataElementA.getCode()));
    assertNotNull(dataElementStore.loadByCode(dataElementB.getCode()));
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> dataElementStore.loadByCode("nonExisting"));
    assertEquals(ErrorCode.E1113, ex.getErrorCode());
  }

  @Test
  void testCountMethods() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementStore.save(dataElementA);
    dataElementStore.save(dataElementB);
    assertEquals(2, dataElementStore.getCountLikeName("dataelement"));
    assertEquals(2, dataElementStore.getCount());
    assertEquals(0, dataElementStore.getCountGeCreated(new Date()));
    assertEquals(2, dataElementStore.getCountGeCreated(dataElementA.getCreated()));
    assertEquals(2, dataElementStore.getCountGeLastUpdated(dataElementA.getLastUpdated()));
  }

  @Test
  void testExistsByUser() {
    User userA = createAndAddUser("userA");
    User userB = getAdminUser();
    DataElement dataElementA = createDataElement('A');
    dataElementA.setCreatedBy(userA);
    dataElementStore.save(dataElementA);
    assertTrue(dataElementStore.existsByUser(userA, Set.of("createdBy")));
    assertFalse(dataElementStore.existsByUser(userB, Set.of("createdBy")));

    dataElementA.setDescription("update");
    dataElementStore.update(dataElementA, UserDetails.fromUser(userB));
    assertFalse(dataElementStore.existsByUser(userA, Set.of("lastUpdatedBy")));
    assertTrue(dataElementStore.existsByUser(userB, Set.of("lastUpdatedBy")));

    assertFalse(dataElementStore.existsByUser(userB, Set.of("name")));
    assertTrue(dataElementStore.existsByUser(userB, Set.of("createdBy", "lastUpdatedBy")));
  }
}
