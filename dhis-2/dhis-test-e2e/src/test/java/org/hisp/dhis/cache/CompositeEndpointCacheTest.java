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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class CompositeEndpointCacheTest extends CacheApiTest {

  @TestFactory
  Stream<DynamicTest> compositeEndpointsHonorConditionalEtags() {
    return scenarios().stream()
        .map(
            scenario ->
                dynamicTest(scenario.displayName(), () -> assertCompositeScenario(scenario)));
  }

  private void assertCompositeScenario(CompositeCacheScenario scenario) {
    CacheProbeUser.SUPERUSER.login(loginActions);
    scenario.beforeProbe().accept(this);
    String path = scenario.path().apply(this);

    scenario.probeUser().login(loginActions);
    CacheProbe.CacheResponse initialResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(initialResponse);

    scenario.probeUser().login(loginActions);
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch(path, initialResponse.etag()), initialResponse.etag());

    if (scenario.checkHead()) {
      scenario.probeUser().login(loginActions);
      CacheAssertions.assertHeadMirrorsGet(initialResponse, probe.head(path));
    }

    mutators.mutate(scenario.positiveMutation());
    CacheProbe.CacheResponse invalidatedResponse =
        CacheAwait.awaitInvalidation(
            probe, scenario.probeUser(), loginActions, path, initialResponse.etag());
    assertNotEquals(initialResponse.etag(), invalidatedResponse.etag());

    mutators.mutate(scenario.negativeMutation());
    scenario.probeUser().login(loginActions);
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch(path, invalidatedResponse.etag()), invalidatedResponse.etag());
  }

  private List<CompositeCacheScenario> scenarios() {
    return List.of(
        new CompositeCacheScenario(
            "GET /systemSettings?key=applicationTitle",
            CacheProbeUser.SUPERUSER,
            test -> "/systemSettings?key=applicationTitle",
            test -> {},
            CacheDependency.SYSTEM_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /systemSettings/applicationTitle",
            CacheProbeUser.SUPERUSER,
            test -> "/systemSettings/applicationTitle",
            test -> {},
            CacheDependency.SYSTEM_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /userSettings?key=keyStyle",
            CacheProbeUser.SUPERUSER,
            test -> "/userSettings?key=keyStyle",
            test -> {},
            CacheDependency.USER_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /userSettings/keyStyle",
            CacheProbeUser.SUPERUSER,
            test -> "/userSettings/keyStyle",
            test -> {},
            CacheDependency.USER_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /userDataStore/cache-e2e",
            CacheProbeUser.SUPERUSER,
            test -> "/userDataStore/cache-e2e",
            test -> test.resourceLocator.ensureUserDataStoreProbeEntry(),
            CacheDependency.USER_DATASTORE_ENTRY,
            CacheDependency.SYSTEM_SETTING,
            true),
        new CompositeCacheScenario(
            "GET /me/settings",
            CacheProbeUser.SUPERUSER,
            test -> "/me/settings",
            test -> {},
            CacheDependency.USER_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/settings/keyStyle",
            CacheProbeUser.SUPERUSER,
            test -> "/me/settings/keyStyle",
            test -> {},
            CacheDependency.USER_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/authorization",
            CacheProbeUser.SUPERUSER,
            test -> "/me/authorization",
            test -> {},
            CacheDependency.USER_ROLE,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/authorization/F_SYSTEM_SETTING",
            CacheProbeUser.SUPERUSER,
            test -> "/me/authorization/F_SYSTEM_SETTING",
            test -> {},
            CacheDependency.USER_ROLE,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/authorities",
            CacheProbeUser.SUPERUSER,
            test -> "/me/authorities",
            test -> {},
            CacheDependency.USER_ROLE,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/authorities/F_SYSTEM_SETTING",
            CacheProbeUser.SUPERUSER,
            test -> "/me/authorities/F_SYSTEM_SETTING",
            test -> {},
            CacheDependency.USER_ROLE,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/dataApprovalLevels",
            CacheProbeUser.SUPERUSER,
            test -> "/me/dataApprovalLevels",
            test -> {},
            CacheDependency.USER,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /me/dataApprovalWorkflows",
            CacheProbeUser.SUPERUSER,
            test -> "/me/dataApprovalWorkflows",
            test -> {},
            CacheDependency.USER,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /dimensions",
            CacheProbeUser.SUPERUSER,
            test -> "/dimensions",
            test -> {},
            CacheDependency.CATEGORY,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /dimensions/constraints",
            CacheProbeUser.SUPERUSER,
            test -> "/dimensions/constraints",
            test -> {},
            CacheDependency.CATEGORY,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /dimensions/dataSet/{uid}",
            CacheProbeUser.SUPERUSER,
            test -> "/dimensions/dataSet/" + test.resourceLocator.firstDataSetId(),
            test -> {},
            CacheDependency.DATA_SET,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /dataStatistics",
            CacheProbeUser.SUPERUSER,
            test -> {
              String today = LocalDate.now().toString();
              return "/dataStatistics?startDate=" + today + "&endDate=" + today + "&interval=DAY";
            },
            test -> {},
            CacheDependency.DATA_STATISTICS,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /dataStatistics/favorites/{uid}",
            CacheProbeUser.SUPERUSER,
            test -> "/dataStatistics/favorites/" + test.resourceLocator.firstVisualizationId(),
            test -> {},
            CacheDependency.DATA_STATISTICS_EVENT,
            CacheDependency.USER_DATASTORE_ENTRY,
            true),
        new CompositeCacheScenario(
            "GET /loginConfig?locale=en",
            CacheProbeUser.SUPERUSER,
            test -> "/loginConfig?locale=en",
            test -> {},
            CacheDependency.SYSTEM_SETTING,
            CacheDependency.USER_DATASTORE_ENTRY,
            true));
  }

  private record CompositeCacheScenario(
      String displayName,
      CacheProbeUser probeUser,
      Function<CompositeEndpointCacheTest, String> path,
      Consumer<CompositeEndpointCacheTest> beforeProbe,
      CacheDependency positiveMutation,
      CacheDependency negativeMutation,
      boolean checkHead) {}
}
