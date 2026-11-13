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
package org.hisp.dhis.security.acl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests {@link AccessControlStore#getDataReadableAttributeOptionCombos}: the attribute option
 * combos a user may read data of, resolved up front for data value and tracker exports.
 *
 * <p>The rules, each pinned by the tests below:
 *
 * <ul>
 *   <li>a superuser is not restricted
 *   <li>the default combo is always readable, whatever the sharing of the default option
 *   <li>only option combos of the default or an {@code ATTRIBUTE} category combo are AOCs
 *   <li>an AOC is readable when the user may read data of <em>every</em> one of its options; an AOC
 *       without options is readable, as in {@link AclService#canDataRead}
 *   <li>data read of an option is granted by its public access, the user's own entry, or the entry
 *       of a group the user is a member of; entries of other users and groups do not count, and an
 *       entry that does not grant data read does not take away what another entry grants
 *   <li>only the data read flag counts: metadata read or write, data write and ownership do not
 *       grant it
 *   <li>a missing public access, or an entry without access string, grants it, as {@link
 *       AccessStringHelper#isEnabled}; an entry that is not a JSON object grants nothing, and
 *       sharing of any shape never fails the lookup
 *   <li>{@code limitTo} restricts the result to the given combos; null or empty means all
 * </ul>
 */
@Transactional
class AccessControlStoreTest extends PostgresIntegrationTestBase {

  @Autowired private AccessControlStore store;
  @Autowired private CategoryService categoryService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AclService aclService;

  private CategoryOption coOpen;
  private CategoryOption coClosed;
  private Category attrCategory;
  private CategoryOptionCombo aocOpen;
  private CategoryOptionCombo aocClosed;
  private CategoryOptionCombo cocDisaggOpen;
  private User user;

  @BeforeEach
  void setUp() {
    // attribute combo with one readable and one restricted option
    coOpen = readableOption('O');
    coClosed = restrictedOption('C');
    attrCategory = attributeCategory('A', coOpen, coClosed);
    CategoryCombo attrCombo = attributeCombo('A', attrCategory);
    aocOpen = optionCombo(attrCombo, coOpen);
    aocClosed = optionCombo(attrCombo, coClosed);

    // disaggregation combo whose only option is readable: never an attribute option combo
    CategoryOption coDisaggOpen = readableOption('D');
    Category disaggCategory = createCategory('D', coDisaggOpen);
    categoryService.addCategory(disaggCategory);
    CategoryCombo disaggCombo = createCategoryCombo('D', disaggCategory);
    categoryService.addCategoryCombo(disaggCombo);
    cocDisaggOpen = optionCombo(disaggCombo, coDisaggOpen);

    user = makeUser("A");
    userService.addUser(user);
  }

  // superuser

  @Test
  void superuserIsNotRestricted() {
    UserDetails superuser = UserDetails.fromUser(makeUser("S", List.of("ALL")));

    assertTrue(store.getDataReadableAttributeOptionCombos(superuser, null).isUnrestricted());
    assertTrue(
        store
            .getDataReadableAttributeOptionCombos(superuser, List.of(UID.of(aocClosed)))
            .isUnrestricted());
  }

  // which option combos are AOCs

  @Test
  void defaultAndReadableAttributeOptionCombosOnly() {
    assertReadable(aocOpen);
  }

  @Test
  void defaultIsReadableEvenWhenTheDefaultOptionIsRestricted() {
    CategoryOption defaultOption = categoryService.getDefaultCategoryOption();
    restrict(defaultOption);
    categoryService.updateCategoryOption(defaultOption, new SystemUser());

    assertReadable(aocOpen);
  }

  @Test
  void disaggregationOptionComboIsNeverReadableAsAnAttributeOptionCombo() {
    UserDetails details = UserDetails.fromUser(user);

    assertEquals(Set.of(), readable(details, List.of(UID.of(cocDisaggOpen))));
  }

  @Test
  void attributeOptionComboWithoutOptionsIsReadable() {
    CategoryCombo combo = attributeCombo('E', attributeCategory('E', readableOption('E')));
    CategoryOptionCombo aocWithoutOptions = optionCombo(combo);

    assertReadable(aocOpen, aocWithoutOptions);
  }

  // every option of an AOC must be readable

  @Test
  void attributeOptionComboIsReadableOnlyWhenEveryOptionIsReadable() {
    CategoryOption coOpen2 = readableOption('P');
    CategoryOption coClosed2 = restrictedOption('Q');
    CategoryCombo combo =
        attributeCombo('M', attrCategory, attributeCategory('B', coOpen2, coClosed2));
    CategoryOptionCombo openOpen = optionCombo(combo, coOpen, coOpen2);
    CategoryOptionCombo openClosed = optionCombo(combo, coOpen, coClosed2);
    optionCombo(combo, coClosed, coOpen2);
    optionCombo(combo, coClosed, coClosed2);

    assertReadable(aocOpen, openOpen);

    grantUser(coClosed2, AccessStringHelper.DATA_READ);

    assertReadable(aocOpen, openOpen, openClosed);
  }

  // public access

  @ParameterizedTest
  @CsvSource({
    "--------, false",
    "r-------, false",
    "rw------, false",
    "---w----, false",
    "rw-w----, false",
    "--r-----, true",
    "r-r-----, true",
    "rwr-----, true",
    "rwrw----, true"
  })
  void publicAccessGrantsDataReadOnlyWithTheDataReadFlag(String access, boolean readable) {
    coClosed.getSharing().setPublicAccess(access);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadableIf(readable);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"owner\": \"ownerOther1\", \"users\": {}, \"userGroups\": {}}",
        "{\"owner\": \"ownerOther1\", \"public\": null, \"users\": {}, \"userGroups\": {}}"
      })
  void missingPublicAccessGrantsDataRead(String sharing) {
    UserDetails details = UserDetails.fromUser(user);
    setSharing(coClosed, sharing);

    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId(), aocClosed.getId()), readable(details, null));
  }

  // the user's own entry

  @ParameterizedTest
  @CsvSource({
    "--------, false",
    "r-------, false",
    "rw------, false",
    "---w----, false",
    "rw-w----, false",
    "--r-----, true",
    "rwr-----, true",
    "rwrw----, true"
  })
  void userAccessGrantsDataReadOnlyWithTheDataReadFlag(String access, boolean readable) {
    grantUser(coClosed, access);

    assertReadableIf(readable);
  }

  @Test
  void userAccessOnRestrictedOptionMakesItsComboReadable() {
    enableDataSharing(user, coClosed, AccessStringHelper.DATA_READ);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  @Test
  void accessOfAnotherUserDoesNotMakeAComboReadable() {
    User other = makeUser("B");
    userService.addUser(other);
    grantUser(coClosed, other, AccessStringHelper.DATA_READ);

    assertReadable(aocOpen);
  }

  // user group entries

  @ParameterizedTest
  @CsvSource({
    "--------, false",
    "r-------, false",
    "rw------, false",
    "---w----, false",
    "rw-w----, false",
    "--r-----, true",
    "rwr-----, true",
    "rwrw----, true"
  })
  void groupAccessGrantsDataReadOnlyWithTheDataReadFlag(String access, boolean readable) {
    UserGroup group = groupOf('G', user);
    grantGroup(coClosed, group, access);

    assertReadableIf(readable);
  }

  @Test
  void groupAccessOnRestrictedOptionMakesItsComboReadable() {
    UserGroup group = createUserGroup('G', Set.of(user));
    manager.save(group);
    user.getGroups().add(group);
    userService.updateUser(user);
    enableDataSharingWithUserGroup(group, coClosed, AccessStringHelper.DATA_READ);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  @Test
  void accessOfAGroupTheUserIsNotInDoesNotMakeAComboReadable() {
    groupOf('G', user);
    UserGroup otherGroup = createUserGroup('H', Set.of());
    manager.save(otherGroup);
    grantGroup(coClosed, otherGroup, AccessStringHelper.DATA_READ);

    assertReadable(aocOpen);
  }

  @Test
  void groupAccessDoesNotMakeAComboReadableForAUserWithoutGroups() {
    UserGroup otherGroup = createUserGroup('H', Set.of());
    manager.save(otherGroup);
    grantGroup(coClosed, otherGroup, AccessStringHelper.DATA_READ);

    assertReadable(aocOpen);
  }

  @Test
  void anyOfTheUsersGroupsCanGrantDataRead() {
    UserGroup first = groupOf('G', user);
    UserGroup second = groupOf('H', user);
    coClosed.getSharing().addUserGroupAccess(new UserGroupAccess(first, AccessStringHelper.READ));
    coClosed
        .getSharing()
        .addUserGroupAccess(new UserGroupAccess(second, AccessStringHelper.DATA_READ));
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  // grants add up

  @Test
  void anEntryWithoutDataReadDoesNotTakeAwayWhatAnotherEntryGrants() {
    UserGroup group = groupOf('G', user);
    coClosed.getSharing().addUserAccess(new UserAccess(user, AccessStringHelper.READ));
    coClosed
        .getSharing()
        .addUserGroupAccess(new UserGroupAccess(group, AccessStringHelper.DATA_READ));
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  @Test
  void publicDataReadIsNotTakenAwayByAUserEntryWithoutDataRead() {
    coClosed.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
    coClosed.getSharing().addUserAccess(new UserAccess(user, AccessStringHelper.READ));
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  // ownership grants no data read

  @Test
  void ownerOfRestrictedOptionCannotReadItsData() {
    coClosed.getSharing().setOwner(user.getUid());
    categoryService.updateCategoryOption(coClosed, new SystemUser());
    UserDetails details = UserDetails.fromUser(user);

    assertFalse(aclService.canDataRead(details, aocClosed));
    assertReadable(aocOpen);
  }

  @Test
  void ownershipDoesNotTakeAwayPublicDataRead() {
    coClosed.getSharing().setOwner(user.getUid());
    coClosed.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen, aocClosed);
  }

  @Test
  void restrictedOptionWithoutOwnerIsNotReadable() {
    // saving sets a missing owner, so only migrated sharing has none
    UserDetails details = UserDetails.fromUser(user);
    setSharing(coClosed, "{\"public\": \"--------\", \"users\": {}, \"userGroups\": {}}");

    assertEquals(Set.of(defaultCoc().getId(), aocOpen.getId()), readable(details, null));
  }

  @Test
  void dataWriteAccessWithoutDataReadDoesNotMakeAComboReadable() {
    enableDataSharing(user, coClosed, AccessStringHelper.DATA_WRITE);
    categoryService.updateCategoryOption(coClosed, new SystemUser());

    assertReadable(aocOpen);
  }

  // sharing as a migration or import may have left it

  /**
   * Sharing JSON of the restricted option, with {@code USER} and {@code GROUP} standing for the
   * test user and a group the user is a member of. Public access is none in all of them.
   */
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        // an entry without access string grants data read, as AccessStringHelper#isEnabled
        "{'users': {'USER': {'id': 'USER'}}} | true",
        "{'users': {'USER': {'id': 'USER', 'access': null}}} | true",
        "{'userGroups': {'GROUP': {'id': 'GROUP'}}} | true",
        "{'userGroups': {'GROUP': {'id': 'GROUP', 'access': null}}} | true",
        // an entry that is not an object grants nothing
        "{'users': {'USER': null}} | false",
        "{'users': {'USER': '--r-----'}} | false",
        "{'userGroups': {'GROUP': null}} | false",
        "{'userGroups': {'GROUP': '--r-----'}} | false",
        // entries missing, JSON null or not an object: no entry grants, and no error
        "{} | false",
        "{'users': null, 'userGroups': null} | false",
        "{'users': [], 'userGroups': []} | false",
        "{'users': '--r-----', 'userGroups': '--r-----'} | false"
      })
  void sharingOfAnyShapeGrantsDataReadOnlyThroughAnEntryForTheUser(
      String entries, boolean readable) {
    UserGroup group = groupOf('G', user);
    UserDetails details = UserDetails.fromUser(user);
    String sharing =
        entries
            .replaceFirst("^\\{", "{'public': '--------', ")
            .replace(", }", "}")
            .replace("USER", user.getUid())
            .replace("GROUP", group.getUid())
            .replace('\'', '"');
    setSharing(coClosed, sharing);

    Set<Long> expected = new HashSet<>(Set.of(defaultCoc().getId(), aocOpen.getId()));
    if (readable) expected.add(aocClosed.getId());
    assertEquals(expected, readable(details, null));
  }

  // limitTo

  @Test
  void limitedToTheGivenOptionCombos() {
    UserDetails details = UserDetails.fromUser(user);

    assertEquals(Set.of(aocOpen.getId()), readable(details, List.of(UID.of(aocOpen))));
    assertEquals(Set.of(), readable(details, List.of(UID.of(aocClosed))));
    assertEquals(Set.of(), readable(details, List.of(UID.of(cocDisaggOpen))));
  }

  @Test
  void limitedToSeveralOptionCombos() {
    UserDetails details = UserDetails.fromUser(user);

    assertEquals(
        Set.of(defaultCoc().getId(), aocOpen.getId()),
        readable(
            details,
            List.of(
                UID.of(defaultCoc()), UID.of(aocOpen), UID.of(aocClosed), UID.of(cocDisaggOpen))));
  }

  @Test
  void limitedToNoOptionCombosChecksAllOfThem() {
    UserDetails details = UserDetails.fromUser(user);

    assertEquals(readable(details, null), readable(details, List.of()));
  }

  @Test
  void limitedToAnUnknownOptionComboReadsNone() {
    DataReadableAocs readable =
        store.getDataReadableAttributeOptionCombos(
            UserDetails.fromUser(user), List.of(UID.generate()));

    assertTrue(readable.isNone());
  }

  // both APIs

  @Test
  void sameLookupOnAJdbcTemplate() {
    UserDetails details = UserDetails.fromUser(user);
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);

    assertEquals(
        readable(details, null),
        ids(store.getDataReadableAttributeOptionCombos(jdbc, details, null)));
    assertEquals(
        Set.of(aocOpen.getId()),
        ids(store.getDataReadableAttributeOptionCombos(jdbc, details, List.of(UID.of(aocOpen)))));
  }

  /** Asserts the user may read data of the default combo and exactly the given AOCs. */
  private void assertReadable(CategoryOptionCombo... aocs) {
    Set<Long> expected = new HashSet<>();
    expected.add(defaultCoc().getId());
    Arrays.stream(aocs).map(CategoryOptionCombo::getId).forEach(expected::add);
    assertEquals(expected, readable(UserDetails.fromUser(user), null));
  }

  private void assertReadableIf(boolean closedIsReadable) {
    if (closedIsReadable) {
      assertReadable(aocOpen, aocClosed);
    } else {
      assertReadable(aocOpen);
    }
  }

  private Set<Long> readable(UserDetails user, List<UID> limitTo) {
    return ids(store.getDataReadableAttributeOptionCombos(user, limitTo));
  }

  private static Set<Long> ids(DataReadableAocs readable) {
    return Arrays.stream(readable.ids()).boxed().collect(Collectors.toSet());
  }

  private CategoryOptionCombo defaultCoc() {
    return categoryService.getDefaultCategoryOptionCombo();
  }

  private CategoryOption readableOption(char id) {
    CategoryOption option = createCategoryOption(id);
    option.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
    categoryService.addCategoryOption(option, new SystemUser());
    return option;
  }

  private CategoryOption restrictedOption(char id) {
    CategoryOption option = createCategoryOption(id);
    restrict(option);
    categoryService.addCategoryOption(option, new SystemUser());
    return option;
  }

  private Category attributeCategory(char id, CategoryOption... options) {
    Category category = createCategory(id, options);
    category.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategory(category);
    return category;
  }

  private CategoryCombo attributeCombo(char id, Category... categories) {
    CategoryCombo combo = createCategoryCombo(id, categories);
    combo.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    categoryService.addCategoryCombo(combo);
    return combo;
  }

  private CategoryOptionCombo optionCombo(CategoryCombo combo, CategoryOption... options) {
    CategoryOptionCombo coc = createCategoryOptionCombo(combo, options);
    categoryService.addCategoryOptionCombo(coc);
    return coc;
  }

  private UserGroup groupOf(char id, User member) {
    UserGroup group = createUserGroup(id, Set.of(member));
    manager.save(group);
    member.getGroups().add(group);
    userService.updateUser(member);
    return group;
  }

  private void grantUser(CategoryOption option, String access) {
    grantUser(option, user, access);
  }

  private void grantUser(CategoryOption option, User grantee, String access) {
    option.getSharing().addUserAccess(new UserAccess(grantee, access));
    categoryService.updateCategoryOption(option, new SystemUser());
  }

  private void grantGroup(CategoryOption option, UserGroup group, String access) {
    option.getSharing().addUserGroupAccess(new UserGroupAccess(group, access));
    categoryService.updateCategoryOption(option, new SystemUser());
  }

  /** Stores the sharing JSON as is, as a migration may have left it. */
  private void setSharing(CategoryOption option, String sharing) {
    entityManager.flush();
    entityManager
        .createNativeQuery(
            "update categoryoption set sharing = cast(:sharing as jsonb) where uid = :uid")
        .setParameter("sharing", sharing)
        .setParameter("uid", option.getUid())
        .executeUpdate();
    entityManager.clear();
  }

  private static void restrict(CategoryOption option) {
    // no public access; the owner is irrelevant, ownership does not grant data read
    option.getSharing().setOwner("ownerOther1");
    option.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
  }
}
