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
package org.hisp.dhis.preheat;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.schema.Schema;
import org.springframework.stereotype.Component;

/**
 * Fetches existing uid and code values for a given {@link Schema} class to support uniqueness
 * checking during preheat/import.
 *
 * @author Luciano Fiandesio
 */
@Component
public class SchemaToDataFetcher {

  public record UniqueFields(@CheckForNull UID uid, @CheckForNull String code) {}

  private final EntityManager entityManager;

  public SchemaToDataFetcher(EntityManager entityManager) {
    checkNotNull(entityManager);
    this.entityManager = entityManager;
  }

  /**
   * Fetches uid and code values for existing records of the given Schema class, filtered to only
   * include records that have matching uid or code values from the objects being imported.
   *
   * @param schema a {@link Schema}
   * @param objectsBeingImported the objects being imported, used to filter the query. If null or
   *     empty, returns empty list.
   * @return a list of {@link UniqueFields} for existing records that might conflict
   */
  @SuppressWarnings("unchecked")
  public List<UniqueFields> fetch(
      Schema schema, Collection<? extends IdentifiableObject> objectsBeingImported) {
    if (schema == null || objectsBeingImported == null || objectsBeingImported.isEmpty()) {
      return List.of();
    }

    Set<String> uids = new HashSet<>();
    Set<String> codes = new HashSet<>();
    for (IdentifiableObject obj : objectsBeingImported) {
      if (obj.getUid() != null) uids.add(obj.getUid());
      if (obj.getCode() != null) codes.add(obj.getCode());
    }

    if (uids.isEmpty() && codes.isEmpty()) {
      return List.of();
    }

    String entityName = schema.getKlass().getSimpleName();
    String hql;
    if (!uids.isEmpty() && !codes.isEmpty()) {
      hql =
          "SELECT uid, code FROM %s WHERE uid IN (:uids) OR code IN (:codes)".formatted(entityName);
    } else if (!uids.isEmpty()) {
      hql = "SELECT uid, code FROM %s WHERE uid IN (:uids)".formatted(entityName);
    } else {
      hql = "SELECT uid, code FROM %s WHERE code IN (:codes)".formatted(entityName);
    }

    Query query = entityManager.createQuery(hql).setHint(QueryHints.HINT_READONLY, true);
    if (!uids.isEmpty()) query.setParameter("uids", uids);
    if (!codes.isEmpty()) query.setParameter("codes", codes);

    List<Object[]> rows = query.getResultList();
    return rows.stream()
        .map(
            row ->
                new UniqueFields(row[0] != null ? UID.of((String) row[0]) : null, (String) row[1]))
        .toList();
  }
}
