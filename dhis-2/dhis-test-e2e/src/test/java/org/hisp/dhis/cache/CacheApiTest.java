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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

@Slf4j
@Tag("cache")
abstract class CacheApiTest extends ApiTest {
  protected CacheProbe probe;
  protected LoginActions loginActions;
  protected CacheResourceLocator resourceLocator;
  protected CacheMutatorRegistry mutators;

  @BeforeAll
  void setUpCacheHarness() {
    probe = new CacheProbe();
    loginActions = new LoginActions();
    resourceLocator = new CacheResourceLocator();
    mutators = new CacheMutatorRegistry(resourceLocator);
  }

  /**
   * Cleans up entities created by cache test fixtures. These entities can pollute the database
   * state and cause failures in other e2e tests (e.g. MetadataImportTest) that export and reimport
   * all metadata.
   */
  @AfterAll
  void cleanUpCacheFixtureEntities() {
    loginActions.loginAsSuperUser();
    deleteQuietly("/programStageWorkingLists", List.of("CchPSW00001"));
    deleteQuietly("/programStages", List.of("CchPS000001", "CchPSWST001"));
    deleteQuietly("/programs", List.of("CchPrgSTG01", "CchPrgPSW01"));
  }

  private static void deleteQuietly(String endpoint, List<String> uids) {
    RestApiActions api = new RestApiActions(endpoint);
    for (String uid : uids) {
      try {
        api.delete(uid);
      } catch (Exception e) {
        log.debug(
            "Cache test cleanup: could not delete {} {} (may not exist): {}",
            endpoint,
            uid,
            e.getMessage());
      }
    }
  }
}
