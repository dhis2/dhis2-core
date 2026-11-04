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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests {@link CategoryOptionComboStore#getDataReadableAttributeOptionComboIds}: the attribute
 * option combos a user may read data of, resolved up front for data value and tracker exports.
 */
@Transactional
class CategoryOptionComboStoreReadableAocTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryOptionComboStore store;
  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;

  private CategoryOption coOpen;
  private CategoryOption coClosed;
  private CategoryOptionCombo aocOpen;
  private CategoryOptionCombo aocClosed;
  private CategoryOptionCombo cocDisaggOpen;
  private User user;

  @BeforeEach
  void setUp() {
    // attribute combo with one readable and one restricted option
    coOpen = createCategoryOption('O');
    readable(coOpen);
    coClosed = createCategoryOption('C');
    restrict(coClosed);
    categoryService.addCategoryOption(coOpen, new SystemUser());
    categoryService.addCategoryOption(coClosed, new SystemUser());
    Category attrCategory = createCategory('A', coOpen, coClosed);
    attrCategory.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategory(attrCategory);
    CategoryCombo attrCombo = createCategoryCombo('A', attrCategory);
    attrCombo.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategoryCombo(attrCombo);
    aocOpen = createCategoryOptionCombo(attrCombo, coOpen);
    aocClosed = createCategoryOptionCombo(attrCombo, coClosed);
    categoryService.addCategoryOptionCombo(aocOpen);
    categoryService.addCategoryOptionCombo(aocClosed);

    // disaggregation combo whose only option is readable: never an attribute option combo
    CategoryOption coDisaggOpen = createCategoryOption('D');
    readable(coDisaggOpen);
    categoryService.addCategoryOption(coDisaggOpen, new SystemUser());
    Category disaggCategory = createCategory('D', coDisaggOpen);
    categoryService.addCategory(disaggCategory);
    CategoryCombo disaggCombo = createCategoryCombo('D', disaggCategory);
    categoryService.addCategoryCombo(disaggCombo);
    cocDisaggOpen = createCategoryOptionCombo(disaggCombo, coDisaggOpen);
    categoryService.addCategoryOptionCombo(cocDisaggOpen);

    user = makeUser("A");
    userService.addUser(user);
  }

  @Test
  void superuserIsNotRestricted() {
    UserDetails superuser = UserDetails.fromUser(makeUser("S", List.of("ALL")));

    assertNull(store.getDataReadableAttributeOptionComboIds(superuser, null));
  }

  @Test
  void defaultAndReadableAttributeOptionCombosOnly() {
    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId()), readable(UserDetails.fromUser(user), null));
  }

  @Test
  void defaultIsReadableEvenWhenTheDefaultOptionIsRestricted() {
    CategoryOption defaultOption = categoryService.getDefaultCategoryOption();
    restrict(defaultOption);
    categoryService.updateCategoryOption(defaultOption, new SystemUser());

    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId()), readable(UserDetails.fromUser(user), null));
  }

  @Test
  void userAccessOnRestrictedOptionMakesItsComboReadable() {
    enableDataSharing(user, coClosed, AccessStringHelper.DATA_READ);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId(), aocClosed.getId()),
        readable(UserDetails.fromUser(user), null));
  }

  @Test
  void groupAccessOnRestrictedOptionMakesItsComboReadable() {
    UserGroup group = createUserGroup('G', Set.of(user));
    manager.save(group);
    user.getGroups().add(group);
    userService.updateUser(user);
    enableDataSharingWithUserGroup(group, coClosed, AccessStringHelper.DATA_READ);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId(), aocClosed.getId()),
        readable(UserDetails.fromUser(user), null));
  }

  @Test
  void limitedToTheGivenOptionCombos() {
    UserDetails details = UserDetails.fromUser(user);

    assertEquals(Set.of(aocOpen.getId()), readable(details, List.of(UID.of(aocOpen))));
    assertEquals(Set.of(), readable(details, List.of(UID.of(aocClosed))));
    assertEquals(Set.of(), readable(details, List.of(UID.of(cocDisaggOpen))));
  }

  private Set<Long> readable(UserDetails user, List<UID> limitTo) {
    long[] ids = store.getDataReadableAttributeOptionComboIds(user, limitTo);
    return Arrays.stream(ids).boxed().collect(Collectors.toSet());
  }

  private CategoryOptionCombo defaultCoc() {
    return categoryService.getDefaultCategoryOptionCombo();
  }

  private static void readable(CategoryOption option) {
    option.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
  }

  private static void restrict(CategoryOption option) {
    // not the test user as owner (a null owner counts as readable), no public access
    option.getSharing().setOwner("ownerOther1");
    option.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
  }
}
