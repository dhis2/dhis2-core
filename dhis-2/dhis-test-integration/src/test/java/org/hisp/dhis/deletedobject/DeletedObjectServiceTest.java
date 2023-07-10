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
package org.hisp.dhis.deletedobject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class DeletedObjectServiceTest extends IntegrationTestBase {

  @Autowired private DeletedObjectService deletedObjectService;

  @Autowired private IdentifiableObjectManager manager;

  private DeletedObject elementA = new DeletedObject(createDataElement('A'));

  private DeletedObject elementB = new DeletedObject(createDataElement('B'));

  private DeletedObject elementC = new DeletedObject(createDataElement('C'));

  private DeletedObject elementD = new DeletedObject(createDataElement('D'));

  private DeletedObject elementE = new DeletedObject(createDataElement('E'));

  @Test
  void testAddDeletedObject() {
    deletedObjectService.addDeletedObject(elementA);
    deletedObjectService.addDeletedObject(elementB);
    deletedObjectService.addDeletedObject(elementC);
    assertEquals(3, deletedObjectService.countDeletedObjects());
  }

  @Test
  void testGetDeletedObject() {
    DeletedObjectQuery deletedObjectQuery = new DeletedObjectQuery();
    deletedObjectQuery.setTotal(5);
    deletedObjectQuery.setPageSize(2);
    deletedObjectService.addDeletedObject(elementA);
    deletedObjectService.addDeletedObject(elementB);
    deletedObjectService.addDeletedObject(elementC);
    deletedObjectService.addDeletedObject(elementD);
    deletedObjectService.addDeletedObject(elementE);
    deletedObjectQuery.setPage(1);
    List<DeletedObject> firstPageDeletedObjects =
        deletedObjectService.getDeletedObjects(deletedObjectQuery);
    deletedObjectQuery.setPage(2);
    List<DeletedObject> secondPageDeletedObjects =
        deletedObjectService.getDeletedObjects(deletedObjectQuery);
    deletedObjectQuery.setPage(3);
    List<DeletedObject> thirdPageDeletedObjects =
        deletedObjectService.getDeletedObjects(deletedObjectQuery);
    assertEquals(5, deletedObjectService.countDeletedObjects());
    assertEquals(2, firstPageDeletedObjects.size());
    assertEquals(2, secondPageDeletedObjects.size());
    assertEquals(1, thirdPageDeletedObjects.size());
    assertThat(firstPageDeletedObjects, contains(elementA, elementB));
    assertThat(secondPageDeletedObjects, contains(elementC, elementD));
    assertThat(thirdPageDeletedObjects, contains(elementE));
  }

  @Test
  void testSearchForKlass() {
    deletedObjectService.addDeletedObject(elementA);
    deletedObjectService.addDeletedObject(elementB);
    deletedObjectService.addDeletedObject(elementC);
    deletedObjectService.addDeletedObject(new DeletedObject(createOrganisationUnit('A')));
    deletedObjectService.addDeletedObject(new DeletedObject(createOrganisationUnit('B')));
    deletedObjectService.addDeletedObject(new DeletedObject(createOrganisationUnit('C')));
    assertEquals(6, deletedObjectService.countDeletedObjects());
    assertEquals(3, deletedObjectService.getDeletedObjectsByKlass("DataElement").size());
    assertEquals(3, deletedObjectService.getDeletedObjectsByKlass("OrganisationUnit").size());
    assertTrue(deletedObjectService.getDeletedObjectsByKlass("Indicator").isEmpty());
  }

  @Test
  void testDeleteDataElement() {
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    OrganisationUnit organisationUnitA = createOrganisationUnit('A');
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    transactionTemplate.execute(
        status -> {
          manager.save(dataElementA);
          manager.save(dataElementB);
          manager.save(dataElementC);
          manager.save(organisationUnitA);
          manager.save(organisationUnitB);
          manager.delete(dataElementA);
          manager.delete(dataElementB);
          manager.delete(dataElementC);
          manager.delete(organisationUnitA);
          manager.delete(organisationUnitB);
          dbmsManager.clearSession();
          return null;
        });
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> deletedObjectService.countDeletedObjects() > 0);
    assertEquals(5, deletedObjectService.countDeletedObjects());
    assertEquals(3, deletedObjectService.getDeletedObjectsByKlass("DataElement").size());
    assertEquals(2, deletedObjectService.getDeletedObjectsByKlass("OrganisationUnit").size());
  }
}
