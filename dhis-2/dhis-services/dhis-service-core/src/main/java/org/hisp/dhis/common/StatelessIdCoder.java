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
package org.hisp.dhis.common;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.commons.util.TextUtils.replace;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

/**
 * Implements the {@link IdCoder} using {@link StatelessSession}s.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StatelessIdCoder implements IdCoder {

  private final EntityManager entityManager;

  @Nonnull
  @Override
  public Map<UID, String> mapEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> identifiers) {
    if (to == IdProperty.UID)
      return identifiers
          .filter(Objects::nonNull)
          .distinct()
          .collect(toMap(identity(), UID::getValue));
    String[] ids =
        identifiers.filter(Objects::nonNull).map(UID::getValue).distinct().toArray(String[]::new);
    if (ids.length == 0) return Map.of();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT t.uid, ${property}
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON t.uid = input.id
      WHERE ${property} IS NOT NULL
      """;
    String sql =
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName("t", to)));
    return runReadInStatelessSession(
        sql,
        q ->
            q.setParameter("ids", ids).stream()
                .collect(toMap(row -> UID.of((String) row[0]), row -> (String) row[1])));
  }

  @Nonnull
  @Override
  public List<String> listEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> identifiers) {
    if (to == IdProperty.UID)
      return identifiers.filter(Objects::nonNull).map(UID::getValue).distinct().toList();
    String[] ids =
        identifiers.filter(Objects::nonNull).map(UID::getValue).distinct().toArray(String[]::new);
    if (ids.length == 0) return List.of();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT ${property}
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON t.uid = input.id
      WHERE ${property} IS NOT NULL
      """;
    String sql =
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName("t", to)));
    return runReadInStatelessSession(
        sql, q -> q.setParameter("ids", ids).stream(String.class).toList());
  }

  @Nonnull
  @Override
  public Map<UID, Map<String, String>> mapEncodedOptionCombosAsCategoryAndOption(
      @Nonnull IdProperty toCategory,
      @Nonnull IdProperty toOption,
      @Nonnull Stream<UID> optionCombos) {
    @Language("sql")
    String sqlTemplate =
        """
      SELECT
        coc.uid AS coc_uid,
        array_agg(${c_id}) as c_ids,
        array_agg(${co_id}) as co_ids
      FROM categoryoptioncombo coc
        JOIN categorycombos_optioncombos cc_coc ON cc_coc.categoryoptioncomboid  = coc.categoryoptioncomboid
        JOIN categorycombo cc ON cc_coc.categorycomboid  = cc.categorycomboid
        JOIN categorycombos_categories cc_c ON cc.categorycomboid = cc_c.categorycomboid
        JOIN category c ON cc_c.categoryid = c.categoryid
        JOIN categoryoptioncombos_categoryoptions coc_co ON coc.categoryoptioncomboid = coc_co.categoryoptioncomboid
        JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
        JOIN categories_categoryoptions c_co ON co.categoryoptionid = c_co.categoryoptionid AND c_co.categoryid = c.categoryid
      WHERE coc.uid = ANY(:coc)
      GROUP BY coc.uid""";
    String sql =
        replace(
            sqlTemplate,
            Map.of("c_id", columnName("c", toCategory), "co_id", columnName("co", toOption)));
    return runReadInStatelessSession(
        sql,
        q ->
            q.setParameter("coc", optionCombos).stream(
                    row -> {
                      String[] categories = row.getStringArray(1);
                      if (anyNull(categories)) return null;
                      String[] options = row.getStringArray(2);
                      if (anyNull(options)) return null;
                      @SuppressWarnings("unchecked")
                      Map.Entry<String, String>[] entries = new Map.Entry[categories.length];
                      for (int i = 0; i < categories.length; i++)
                        entries[i] = Map.entry(categories[i], options[i]);
                      return Map.entry(UID.of(row.getString(0)), Map.ofEntries(entries));
                    })
                .filter(Objects::nonNull)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private static boolean anyNull(Object[] arr) {
    if (arr == null) return true;
    if (arr.length == 0) return false;
    if (arr.length == 1) return arr[0] == null;
    for (int i = 0; i < arr.length; i++) if (arr[i] == null) return true;
    return false;
  }

  @Override
  public Map<String, String> mapDecodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> identifiers) {
    if (from == IdProperty.UID)
      return identifiers.distinct().collect(toMap(identity(), identity()));
    String[] ids = identifiers.filter(Objects::nonNull).distinct().toArray(String[]::new);
    if (ids.length == 0) return Map.of();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT ${property}, t.uid
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON ${property} = input.id
      """;
    String sql =
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName("t", from)));
    return runReadInStatelessSession(sql, q -> q.setParameter("ids", ids).listAsStringsMap());
  }

  @Nonnull
  @Override
  public Stream<String> listDecodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> identifiers) {
    if (from == IdProperty.UID) return identifiers;
    String[] ids = identifiers.filter(Objects::nonNull).distinct().toArray(String[]::new);
    if (ids.length == 0) return Stream.empty();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT t.uid
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON ${property} = input.id
      """;
    String sql =
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName("t", from)));
    return runReadInStatelessSession(
        sql, q -> q.setParameter("ids", ids).stream(row -> row.getString(0)));
  }

  private static String tableName(ObjectType type) {
    return switch (type) {
      case DS -> "dataset";
      case DE -> "dataelement";
      case DEG -> "dataelementgroup";
      case OU -> "organisationunit";
      case OUG -> "organisationunitgroup";
      case COC -> "categoryoptioncombo";
    };
  }

  @Nonnull
  private static String columnName(String alias, IdProperty id) {
    return switch (id.name()) {
      case UID -> alias + ".uid";
      case NAME -> alias + ".name";
      case CODE -> alias + ".code";
      case ATTR ->
          "jsonb_extract_path_text(%s.attributeValues, '%s', 'value')"
              .formatted(alias, id.attributeId());
    };
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  private <R> R runReadInStatelessSession(String sql, Function<QueryBuilder, R> query) {
    return runReadInStatelessSession(session -> query.apply(SQL.of(sql, NativeSQL.of(session))));
  }

  private <R> R runReadInStatelessSession(Function<StatelessSession, R> query) {
    StatelessSession session = null;
    Transaction transaction = null;
    try {
      session = getSession().getSessionFactory().openStatelessSession();
      transaction = session.beginTransaction();
      R res = query.apply(session);
      transaction.commit();
      return res;
    } catch (RuntimeException ex) {
      log.warn("ID read failed:", ex);
      if (transaction != null && transaction.isActive()) {
        try {
          transaction.rollback();
        } catch (Exception rollbackEx) {
          log.trace("Rollback failed (possibly due to prior exception)", rollbackEx);
        }
      }
      throw ex;
    } finally {
      if (session != null && session.isOpen()) {
        try {
          session.close();
        } catch (Exception closeEx) {
          log.trace("Session close failed", closeEx);
        }
      }
    }
  }
}
