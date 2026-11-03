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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonStats;
import org.hisp.dhis.test.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Replacing a non-owned collection must not trust a stale second-level cache entry of the inverse
 * collection. Runs on Postgres because the H2 test setup disables the second-level cache.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class CollectionReplaceSecondLevelCacheTest extends PostgresControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testReplaceCollectionItems_NonOwned_StaleInverseCacheEntry() {
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'L2 OU', 'shortName':'L2OU', 'openingDate':'2020-01-01'}"));
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/dataSets/", "{'name':'L2 Data', 'shortName':'L2D', 'periodType':'Monthly'}"));
    String path = "/organisationUnits/" + ouId + "/dataSets";

    assertReplaceStats(path, 1, 0, dsId);
    // load OrganisationUnit.dataSets into the second-level cache
    GET("/organisationUnits/" + ouId + "?fields=dataSets[id]").content(HttpStatus.OK);

    // unassign through the owning side only; Hibernate does not evict the cached inverse collection
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              DataSet ds = manager.get(DataSet.class, dsId);
              ds.getSources().remove(manager.get(OrganisationUnit.class, ouId));
              manager.update(ds);
            });
    assertEquals(0, countDataSetSources(dsId));

    // re-sending the assignment must restore it, not be skipped as unchanged
    assertReplaceStats(path, 1, 0, dsId);
    assertEquals(1, countDataSetSources(dsId));
  }

  private int countDataSetSources(String dsId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from datasetsource dss join dataset ds on ds.datasetid = dss.datasetid"
            + " where ds.uid = ?",
        Integer.class,
        dsId);
  }

  private void assertReplaceStats(String path, int updated, int deleted, String id) {
    JsonStats stats =
        PUT(path, "{'identifiableObjects':[{'id':'" + id + "'}]}")
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class)
            .getResponse()
            .as(JsonTypeReport.class)
            .getStats();
    assertEquals(updated, stats.getUpdated(), "updated");
    assertEquals(deleted, stats.getDeleted(), "deleted");
  }
}
