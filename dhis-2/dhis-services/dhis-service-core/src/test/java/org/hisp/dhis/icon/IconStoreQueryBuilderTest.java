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

import java.util.List;
import java.util.Map;
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
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords,
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE c.iconkey IN (:keys )""",
        Map.of("keys", List.of("foo", "bar")),
        createQuery(new IconQueryParams().setKeys(List.of("foo", "bar")), createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterKeys_1KeyToEquals() {
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords,
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE c.iconkey = :keys""",
        Map.of("keys", "foo"),
        createQuery(new IconQueryParams().setKeys(List.of("foo")), createQueryAPI()));
  }

  @Test
  void testGetIcons_FilterSearch() {
    assertSQL(
        """
      SELECT
        c.iconkey,
        c.description,
        c.keywords,
        c.created,
        c.lastupdated,
        c.fileresourceid,
        c.createdby,
        c.custom
      FROM icon c
      WHERE (c.iconkey ilike :search or c.keywords #>> '{}' ilike :search)""",
        Map.of("search", "%foo%"),
        createQuery(new IconQueryParams().setSearch("foo"), createQueryAPI()));
  }
}
