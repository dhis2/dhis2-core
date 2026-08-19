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
package org.hisp.dhis.organisationunit.hibernate;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrgTree.OrgTreeEntry;
import org.hisp.dhis.organisationunit.OrgTreeParams;
import org.hisp.dhis.organisationunit.OrgTreeStore;
import org.hisp.dhis.organisationunit.OrgUnitPath;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HibernateOrgTreeStore implements OrgTreeStore {

  private final EntityManager entityManager;

  @Override
  public List<OrgUnitPath> queryPageMatches(OrgTreeParams params) {
    return null;
  }

  @Override
  public int countTotalMatches(OrgTreeParams params) {
    return 0;
  }

  @Override
  public Stream<OrgTreeEntry> streamEntries(Locale locale, Stream<UID> orgUnits) {
    String sql =
        """
      SELECT
          ou.uid,
          ou.name,
          ou.hierarchylevel,
          ou.path,
          NOT EXISTS (
              SELECT 1
              FROM organisationunit child
              WHERE child.parentid = ou.organisationunitid
          ) AS leaf
      FROM organisationunit ou
      WHERE ou.uid = ANY(:ou)""";
    return createQuery(sql).setParameter("ou", orgUnits).stream(HibernateOrgTreeStore::entry)
        .sorted();
  }

  private static OrgTreeEntry entry(SQL.Row row) {
    return new OrgTreeEntry(
        UID.of(row.getString(0)),
        row.getString(1),
        row.getInteger(2),
        row.getString(3),
        row.getBoolean(4));
  }

  private QueryBuilder createQuery(@Language("sql") String sql) {
    return SQL.of(sql, NativeSQL.of(getSession()));
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
