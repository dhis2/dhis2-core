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
package org.hisp.dhis.webapi.controller.option;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonOptionSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Asserts the SQL shape of {@code /api/optionSets} when nested options are requested: the options
 * collections of the selected page must be bulk-loaded in one statement instead of one lazy
 * collection initialization per option set (DHIS2-21905).
 *
 * <p>Deliberately not {@code @Transactional}: fixtures are committed so requests observe real
 * Hibernate second-level cache and invalidation behavior across commit boundaries. MockMvc tests
 * share one thread-bound Hibernate session across requests, so between fixture setup and a measured
 * request the persistence context is cleared explicitly; otherwise the list query would resolve the
 * fixture instances from the first-level cache with their collections still initialized, and no
 * option loading would be observable at all.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ContextConfiguration(classes = QueryCountDataSourceProxy.class)
class OptionSetControllerQueryCountTest extends PostgresControllerIntegrationTestBase {

  private static final String OPTIONS_COLLECTION_REGION = OptionSet.class.getName() + ".options";

  @Autowired private EntityManagerFactory entityManagerFactory;

  /**
   * Detaches everything from the thread-bound session so the next request loads fresh entities with
   * uninitialized option collections. The flush inside {@code clearSession} requires an active
   * transaction; flushing the clean read-only session is a no-op.
   */
  private void clearPersistenceContext() {
    doInTransaction(dbmsManager::clearSession);
  }

  @Test
  void nestedOptionsAreFetchedInOneQuery() {
    List<String> uids = createOptionSetsWithOptions('A');
    clearPersistenceContext();
    evictOptionRegions();
    QueryCountDataSourceProxy.clearCapturedSql();

    JsonObject content = GET(nestedOptionsUrl(uids)).content(HttpStatus.OK);

    assertOptionSetsWithOptions(content, uids.size());
    assertEquals(
        1,
        QueryCountDataSourceProxy.countCapturedSqlMatching("optionvalue"),
        "nested options must be fetched for all selected OptionSets in one SQL statement");
  }

  @Test
  void warmNestedOptionsDoNotQueryOptionValue() {
    List<String> uids = createOptionSetsWithOptions('F');
    clearPersistenceContext();
    evictOptionRegions();

    // The first request fills the OptionSet entity, options collection, and Option entity regions.
    String coldResponse = GET(nestedOptionsUrl(uids)).content(HttpStatus.OK).toJson();

    // Clear only the persistence context and the SQL capture; the second-level cache stays warm.
    clearPersistenceContext();
    QueryCountDataSourceProxy.clearCapturedSql();

    String warmResponse = GET(nestedOptionsUrl(uids)).content(HttpStatus.OK).toJson();

    assertEquals(coldResponse, warmResponse, "warm response must equal the cold response");
    assertEquals(
        0,
        QueryCountDataSourceProxy.countCapturedSqlMatching("optionvalue"),
        "warm nested options must initialize from the second-level cache without SQL");
  }

  @Test
  void fieldsWithoutOptionsDoNotLoadOptionValue() {
    List<String> uids = createOptionSetsWithOptions('K');
    clearPersistenceContext();
    evictOptionRegions();
    QueryCountDataSourceProxy.clearCapturedSql();

    JsonObject content =
        GET("/optionSets?filter=id:in:["
                + String.join(",", uids)
                + "]&fields=id,displayName&paging=false")
            .content(HttpStatus.OK);

    assertEquals(uids.size(), content.getList("optionSets", JsonOptionSet.class).size());
    assertEquals(
        0,
        QueryCountDataSourceProxy.countCapturedSqlMatching("optionvalue"),
        "requests without an options field must not load option data");
  }

  @Test
  void updatedOptionNameAndCodeAreVisibleAfterCommit() {
    List<String> uids = createOptionSetsWithOptions('P');
    String url =
        "/optionSets?filter=id:in:["
            + String.join(",", uids)
            + "]&fields=id,options[id,name,code]&paging=false";
    clearPersistenceContext();
    evictOptionRegions();
    GET(url).content(HttpStatus.OK); // warm the option regions

    String updatedSetUid = uids.get(0);
    doInTransaction(
        () -> {
          OptionSet optionSet = manager.get(OptionSet.class, updatedSetUid);
          Option option = optionSet.getOptions().get(0);
          option.setName("OptionP1Renamed");
          option.setCode("P1R");
          manager.update(option);
        });
    clearPersistenceContext();

    JsonObject content = GET(url).content(HttpStatus.OK);
    JsonOptionSet updated = findOptionSet(content, updatedSetUid);
    assertEquals("P1R", updated.getOptions().get(0).getCode());
    assertEquals("OptionP1Renamed", updated.getOptions().get(0).getName());
  }

  @Test
  void addedAndRemovedOptionsAreVisibleInOrderAfterCommit() {
    List<String> uids = createOptionSetsWithOptions('U');
    String url = nestedOptionsUrl(uids);
    clearPersistenceContext();
    evictOptionRegions();
    GET(url).content(HttpStatus.OK); // warm the option regions

    String updatedSetUid = uids.get(0);
    doInTransaction(
        () -> {
          OptionSet optionSet = manager.get(OptionSet.class, updatedSetUid);
          Option removed = optionSet.getOptions().get(0);
          optionSet.getOptions().remove(removed);
          manager.delete(removed);
          Option added = createOption("U3");
          added.setSortOrder(3);
          added.setOptionSet(optionSet);
          optionSet.addOption(added);
          manager.save(added);
          manager.update(optionSet);
        });
    clearPersistenceContext();

    JsonObject content = GET(url).content(HttpStatus.OK);
    JsonOptionSet updated = findOptionSet(content, updatedSetUid);
    assertEquals(2, updated.getOptions().size(), "removed option must disappear, added one appear");
    assertEquals("U2", updated.getOptions().get(0).getCode());
    assertEquals("U3", updated.getOptions().get(1).getCode());
  }

  /**
   * Creates five committed option sets of two options each, named from the given starting
   * character, and returns their UIDs. Each test uses its own character range so names and codes
   * stay unique in the shared database.
   */
  private List<String> createOptionSetsWithOptions(char startingCharacter) {
    List<String> uids = new ArrayList<>();
    doInTransaction(
        () -> {
          for (char c = startingCharacter; c < startingCharacter + 5; c++) {
            Option first = createOption(c + "1");
            first.setSortOrder(1);
            Option second = createOption(c + "2");
            second.setSortOrder(2);
            OptionSet optionSet = createOptionSet(c, first, second);
            optionSet.setValueType(ValueType.TEXT);
            manager.save(optionSet);
            uids.add(optionSet.getUid());
          }
        });
    return uids;
  }

  private static String nestedOptionsUrl(List<String> uids) {
    return "/optionSets?filter=id:in:["
        + String.join(",", uids)
        + "]&fields=id,options[id,code]&paging=false";
  }

  private static void assertOptionSetsWithOptions(JsonObject content, int expectedCount) {
    JsonList<JsonOptionSet> optionSets = content.getList("optionSets", JsonOptionSet.class);
    assertEquals(expectedCount, optionSets.size());
    optionSets.forEach(optionSet -> assertEquals(2, optionSet.getOptions().size()));
  }

  private static JsonOptionSet findOptionSet(JsonObject content, String uid) {
    return content.getList("optionSets", JsonOptionSet.class).stream()
        .filter(optionSet -> uid.equals(optionSet.getId()))
        .findFirst()
        .orElseThrow();
  }

  /** Evicts all option related second-level cache regions so a test starts cold. */
  private void evictOptionRegions() {
    org.hibernate.Cache cache = entityManagerFactory.unwrap(SessionFactory.class).getCache();
    cache.evictEntityData(OptionSet.class);
    cache.evictEntityData(Option.class);
    cache.evictCollectionData(OPTIONS_COLLECTION_REGION);
  }
}
