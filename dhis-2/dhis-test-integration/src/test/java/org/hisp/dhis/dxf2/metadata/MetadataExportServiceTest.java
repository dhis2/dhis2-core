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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class MetadataExportServiceTest extends TransactionalIntegrationTest {
  @Autowired private MetadataExportService metadataExportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private SchemaService schemaService;

  @Test
  void testValidate() {
    MetadataExportParams params = new MetadataExportParams();
    metadataExportService.validate(params);
  }

  @Test
  void testMetadataExport() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    MetadataExportParams params = new MetadataExportParams();
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertEquals(1, metadata.get(User.class).size());
    assertEquals(1, metadata.get(DataElementGroup.class).size());
    assertEquals(3, metadata.get(DataElement.class).size());
  }

  @Test
  void testMetadataExportWithCustomClasses() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    MetadataExportParams params = new MetadataExportParams();
    params.addClass(DataElement.class);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertFalse(metadata.containsKey(User.class));
    assertFalse(metadata.containsKey(DataElementGroup.class));
    assertTrue(metadata.containsKey(DataElement.class));
    assertEquals(3, metadata.get(DataElement.class).size());
  }

  @Test
  void testMetadataExportWithCustomQueries() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    Query deQuery = Query.from(schemaService.getDynamicSchema(DataElement.class));
    Disjunction disjunction = deQuery.disjunction();
    disjunction.add(Restrictions.eq("id", de1.getUid()));
    disjunction.add(Restrictions.eq("id", de2.getUid()));
    deQuery.add(disjunction);
    Query degQuery = Query.from(schemaService.getDynamicSchema(DataElementGroup.class));
    degQuery.add(Restrictions.eq("id", "INVALID UID"));
    MetadataExportParams params = new MetadataExportParams();
    params.addQuery(deQuery);
    params.addQuery(degQuery);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertFalse(metadata.containsKey(User.class));
    assertFalse(metadata.containsKey(DataElementGroup.class));
    assertTrue(metadata.containsKey(DataElement.class));
    assertEquals(2, metadata.get(DataElement.class).size());
  }

  // @Test
  // TODO Fix this
  public void testSkipSharing() {
    MetadataExportParams params = new MetadataExportParams();
    params.setSkipSharing(true);
    params.setClasses(Sets.newHashSet(DataElement.class));
    User user = makeUser("A");
    UserGroup group = createUserGroup('A', Sets.newHashSet(user));
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    DataElement de4 = createDataElement('D');
    DataElement de5 = createDataElement('E');
    de1.getSharing().setUserAccesses(Sets.newHashSet(new UserAccess(user, "rwrwrwrw")));
    de2.setPublicAccess("rwrwrwrw");
    de3.setCreatedBy(user);
    de4.getSharing().setUserGroupAccess(Sets.newHashSet(new UserGroupAccess(group, "rwrwrwrw")));
    de5.setExternalAccess(true);
    manager.save(user);
    manager.save(group);
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    manager.save(de4);
    manager.save(de5);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertEquals(5, metadata.get(DataElement.class).size());
    metadata.get(DataElement.class).stream().forEach(element -> checkSharingFields(element));
  }

  private void checkSharingFields(IdentifiableObject object) {
    assertTrue(object.getSharing().getUsers().isEmpty());
    assertEquals("--------", object.getSharing().getPublicAccess());
    // assertNull( object.getUser() );
    assertTrue(object.getSharing().getUserGroups().isEmpty());
    // assertFalse( object.getExternalAccess() );
  }
}
