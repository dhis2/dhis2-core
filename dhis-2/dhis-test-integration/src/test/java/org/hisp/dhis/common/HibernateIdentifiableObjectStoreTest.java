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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class HibernateIdentifiableObjectStoreTest extends PostgresIntegrationTestBase {
  @Autowired private DataElementStore dataElementStore;

  @Autowired private DataDumpService dataDumpService;

  @Autowired private AggregateAccessManager accessManager;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  /**
   * Test Metadata Read access User and UserGroups mapping User1 | User2 | User3 | User 4 Group1 x |
   * | | Group2 X | | | X
   *
   * <p>DataElementA access defined for Users and UserGroups User1 | User2 | User3 | UserGroup1 |
   * UserGroup2 Can access DEA | X | | X |
   */
  @Test
  void testMetadataRead() {
    User user1 = createAndAddUser("A");
    User user2 = createAndAddUser("B");
    User user3 = createAndAddUser("C");
    User user4 = createAndAddUser("D");
    manager.save(user1);
    manager.save(user2);
    manager.save(user3);
    manager.save(user4);

    UserGroup userGroup1 = createUserGroup('A', Set.of(user1));
    UserGroup userGroup2 = createUserGroup('B', Set.of(user1, user4));
    manager.save(userGroup1);
    manager.save(userGroup2);

    // Create sharing settings
    Map<String, UserAccess> userSharing = new HashMap<>();
    userSharing.put(user1.getUid(), new UserAccess(user1, AccessStringHelper.DEFAULT));
    userSharing.put(user2.getUid(), new UserAccess(user2, AccessStringHelper.READ));
    userSharing.put(user3.getUid(), new UserAccess(user3, AccessStringHelper.DEFAULT));
    userSharing.put(user4.getUid(), new UserAccess(user4, AccessStringHelper.DEFAULT));

    Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
    userGroupSharing.put(
        userGroup1.getUid(), new UserGroupAccess(userGroup1, AccessStringHelper.READ_WRITE));
    userGroupSharing.put(
        userGroup2.getUid(), new UserGroupAccess(userGroup2, AccessStringHelper.DEFAULT));

    // Create DataElement with given sharing settings
    DataElement dataElement = createDataElement('A');
    String dataElementUid = "deabcdefghA";
    dataElement.setUid(dataElementUid);
    dataElement.setCreatedBy(getAdminUser());
    dataElement.setSharing(
        Sharing.builder()
            .external(false)
            .publicAccess(AccessStringHelper.DEFAULT)
            .owner("testOwner")
            .userGroups(userGroupSharing)
            .users(userSharing)
            .build());
    dataElementStore.save(dataElement, false);

    dataElement = dataElementStore.getByUidNoAcl(dataElementUid);
    assertNotNull(dataElement.getSharing());
    assertEquals(2, dataElement.getSharing().getUserGroups().size());
    assertEquals(4, dataElement.getSharing().getUsers().size());

    // Needed to make the user groups available on the user object.
    manager.flush();
    manager.clear();

    User reloadedUser1 = manager.get(User.class, user1.getId());
    DataElement dataElement1 = dataElementStore.getDataElement(dataElement.getUid(), reloadedUser1);
    assertNotNull(dataElement1);
    // User2 has access to DEA
    DataElement dataElement2 = dataElementStore.getDataElement(dataElement.getUid(), user2);
    assertNotNull(dataElement2);
    // User3 doesn't have access and also does't belong to any groups
    DataElement dataElement3 = dataElementStore.getDataElement(dataElement.getUid(), user3);
    assertNull(dataElement3);
    // User4 doesn't have access and it belong to UserGroup2 which also
    // doesn't have access
    User reloadedUser4 = manager.get(User.class, user4.getId());
    DataElement dataElement4 = dataElementStore.getDataElement(dataElement.getUid(), reloadedUser4);
    assertNull(dataElement4);
  }
}
