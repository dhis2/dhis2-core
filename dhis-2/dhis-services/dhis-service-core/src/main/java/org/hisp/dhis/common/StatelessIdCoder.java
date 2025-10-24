package org.hisp.dhis.common;

import jakarta.persistence.EntityManager;
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

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.commons.util.TextUtils.replace;

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
  public Map<String, String> mapEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> identifiers) {
    if (to == IdProperty.UID)
      return identifiers.distinct().collect(toMap(UID::getValue, UID::getValue));
    String[] ids =
        identifiers.filter(Objects::nonNull).map(UID::getValue).distinct().toArray(String[]::new);
    if (ids.length == 0) return Map.of();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT t.uid, ${property}
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON t.uid = input.id
      """;
    String sql = replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName(to)));
    return runReadInStatelessSession(sql, q -> q.setParameter("ids", ids).listAsStringsMap());
  }

  @Nonnull
  @Override
  public Stream<String> listEncodedIds(@Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> identifiers) {
    if (to == IdProperty.UID) return identifiers.map(UID::getValue);
    String[] ids =
        identifiers.filter(Objects::nonNull).map(UID::getValue).distinct().toArray(String[]::new);
    if (ids.length == 0) return Stream.empty();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT ${property}
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON t.uid = input.id
      """;
    String sql = replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName(to)));
    return runReadInStatelessSession(sql, q -> q.setParameter("ids", ids).stream(row -> row.getString(0)));
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
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName(from)));
    return runReadInStatelessSession(sql, q -> q.setParameter("ids", ids).listAsStringsMap());
  }

  @Nonnull
  @Override
  public Stream<String> listDecodedIds(@Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> identifiers) {
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
        replace(sqlTemplate, Map.of("table", tableName(type), "property", columnName(from)));
    return runReadInStatelessSession(sql, q -> q.setParameter("ids", ids).stream(row -> row.getString(0)));
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
  private static String columnName(IdProperty id) {
    return switch (id.name()) {
      case UID -> "t.uid";
      case NAME -> "t.name";
      case CODE -> "t.code";
      case ATTR ->
          "jsonb_extract_path_text(t.attributeValues, '%s', 'value')".formatted(id.attributeId());
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
