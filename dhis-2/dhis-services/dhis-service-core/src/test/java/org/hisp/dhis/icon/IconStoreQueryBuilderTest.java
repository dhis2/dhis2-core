/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.icon;

import static org.hisp.dhis.icon.JdbcIconStore.createQuery;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.sql.AbstractQueryBuilderTest;
import org.hisp.dhis.sql.SQL;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the SQL generation as made by {@link JdbcIconStore#createQuery(IconQueryParams,
 * SQL.QueryAPI)}.
 *
 * @author Jan Bernitt
 */
class IconStoreQueryBuilderTest extends AbstractQueryBuilderTest {

  @Test
  void testGetIcons_FilterKeys() {
    IconQueryParams params = new IconQueryParams().setKeys(List.of("foo", "bar"));
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords #>> '{}',
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE c.iconkey = ANY (:keys )""",
        Map.of("keys", List.of("foo", "bar")),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterKeys_1KeyToEquals() {
    IconQueryParams params = new IconQueryParams().setKeys(List.of("foo"));
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords #>> '{}',
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE c.iconkey = :keys""",
        Map.of("keys", "foo"),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testCountIcons_FilterKeys() {
    IconQueryParams params = new IconQueryParams().setKeys(List.of("foo", "bar"));
    assertCountSQL(
        """
      SELECT count(*)
      FROM icon c
      WHERE c.iconkey = ANY (:keys )""",
        Map.of("keys", List.of("foo", "bar")),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterSearch() {
    IconQueryParams params = new IconQueryParams().setSearch("foo");
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords #>> '{}',
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE (c.iconkey ilike :search or c.keywords #>> '{}' ilike :search)""",
        Map.of("search", "%foo%"),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterKeywords() {
    IconQueryParams params = new IconQueryParams().setKeywords(List.of("foo"));
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords #>> '{}',
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE c.keywords @> cast(:keywords as jsonb)""",
        Map.of("keywords", "[\"foo\"]"),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterCreatedStartDate() {
    Date now = new Date();
    IconQueryParams params = new IconQueryParams().setCreatedStartDate(now);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.created >= :createdStartDate""",
        Map.of("createdStartDate", now),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterCreatedEndDate() {
    Date now = new Date();
    IconQueryParams params = new IconQueryParams().setCreatedEndDate(now);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.created <= :createdEndDate""",
        Map.of("createdEndDate", now),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterLastUpdatedStartDate() {
    Date now = new Date();
    IconQueryParams params = new IconQueryParams().setLastUpdatedStartDate(now);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.lastupdated >= :lastUpdatedStartDate""",
        Map.of("lastUpdatedStartDate", now),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterLastUpdatedEndDate() {
    Date now = new Date();
    IconQueryParams params = new IconQueryParams().setLastUpdatedEndDate(now);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.lastupdated <= :lastUpdatedEndDate""",
        Map.of("lastUpdatedEndDate", now),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterType_ALL() {
    IconQueryParams params = new IconQueryParams().setType(IconTypeFilter.ALL);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE 1=1""",
        Map.of(),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterType_Custom() {
    IconQueryParams params = new IconQueryParams().setType(IconTypeFilter.CUSTOM);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.custom = :custom""",
        Map.of("custom", true),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterType_Default() {
    IconQueryParams params = new IconQueryParams().setType(IconTypeFilter.DEFAULT);
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.custom = :custom""",
        Map.of("custom", false),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_OrderBy_NoFilters() {
    IconQueryParams params =
        new IconQueryParams().setOrder(List.of(OrderCriteria.of("key", SortDirection.DESC)));
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE 1=1
        ORDER BY c.iconkey DESC""",
        Map.of(),
        createQuery(params, createQueryAPI()));
  }

  @Test
  void testGetIcons_Mixed() {
    IconQueryParams params =
        new IconQueryParams()
            .setKeys(List.of("ab", "ba"))
            .setType(IconTypeFilter.CUSTOM)
            .setSearch("term")
            .setOrder(
                List.of(
                    OrderCriteria.of("key", SortDirection.DESC),
                    OrderCriteria.of("created", SortDirection.ASC)));
    assertSQL(
        """
        SELECT
          c.iconkey,
          c.description,
          c.keywords #>> '{}',
          c.created,
          c.lastupdated,
          c.fileresourceid,
          c.createdby,
          c.custom
        FROM icon c
        WHERE c.custom = :custom
          AND c.iconkey = ANY (:keys )
          AND (c.iconkey ilike :search or c.keywords #>> '{}' ilike :search)
        ORDER BY c.iconkey DESC, c.created""",
        Set.of("custom", "keys", "search"),
        createQuery(params, createQueryAPI()));
  }
}
