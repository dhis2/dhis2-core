/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.datavalue.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_AGGREGATE_QUERY_TIMEOUT;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;
import static org.hisp.dhis.security.acl.AclService.LIKE_WRITE_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import com.vladmihalcea.hibernate.type.array.LongArrayType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Repository("org.hisp.dhis.datavalue.DataValueAuditStore")
public class HibernateDataValueAuditStore extends HibernateGenericStore<DataValueAudit>
    implements DataValueAuditStore {
  /**
   * Users with at most this many org units in their hierarchy get them listed by ID in queries for
   * many values (at most 80 kB), see {@link #getOrgUnitIds}.
   */
  public static final int MAX_ORG_UNIT_IDS = 10_000;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final PeriodStore periodStore;

  /** Seconds a statement of a user who is not a superuser may run, 0 for no limit. */
  private final int queryTimeout;

  private int maxOrgUnitIds = MAX_ORG_UNIT_IDS;

  /** Whether the database is PostgreSQL 14 or later, null until asked. */
  private volatile Boolean postgres14;

  public HibernateDataValueAuditStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      PeriodStore periodStore,
      DhisConfigurationProvider config) {
    super(entityManager, jdbcTemplate, publisher, DataValueAudit.class, false);

    checkNotNull(periodStore);

    this.periodStore = periodStore;
    this.queryTimeout = Integer.parseInt(config.getProperty(CHANGELOG_AGGREGATE_QUERY_TIMEOUT));
  }

  /**
   * For tests.
   *
   * @param max the most org units to list by ID, 0 to always find them inside the query
   */
  public void setMaxOrgUnitIds(int max) {
    this.maxOrgUnitIds = max;
  }

  /**
   * For tests.
   *
   * @param is14 true to query as on PostgreSQL 14 or later, false as before 14, null to ask the
   *     server
   */
  public void setPostgres14(@CheckForNull Boolean is14) {
    this.postgres14 = is14;
  }

  // -------------------------------------------------------------------------
  // DataValueAuditStore implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void updateDataValueAudit(DataValueAudit dataValueAudit) {
    getSession().update(dataValueAudit);
  }

  @Override
  public void addDataValueAudit(DataValueAudit dataValueAudit) {
    getSession().save(dataValueAudit);
  }

  @Override
  public void deleteDataValueAudits(OrganisationUnit organisationUnit) {
    String hql = "delete from DataValueAudit d where d.organisationUnit = :unit";

    getSession().createQuery(hql).setParameter("unit", organisationUnit).executeUpdate();
  }

  @Override
  public void deleteDataValueAudits(DataElement dataElement) {
    String hql = "delete from DataValueAudit d where d.dataElement = :dataElement";

    getSession().createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  @Override
  public List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return List.of();
    if (aocAllowed != null) limitStatements();
    NativeQuery<DataValueAudit> query =
        createAuditQuery(
            "SELECT dva.*",
            params,
            "\nORDER BY dva.created DESC",
            DataValueAudit.class,
            user,
            aocAllowed);

    if (params.hasPaging()) {
      query
          .setFirstResult(params.getPager().getOffset())
          // one more than the page tells whether there is a next page
          .setMaxResults(params.getPager().getPageSize() + 1);
    }

    return query.getResultList();
  }

  @Override
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return 0;
    if (aocAllowed != null) limitStatements();
    return createAuditQuery(
            "SELECT count(*) AS audit_count", params, "", Long.class, user, aocAllowed)
        .getSingleResult()
        .intValue();
  }

  @Override
  public boolean isReadable(
      @Nonnull DataElement dataElement,
      @Nonnull OrganisationUnit organisationUnit,
      @Nonnull CategoryOptionCombo categoryOptionCombo,
      @Nonnull CategoryOptionCombo attributeOptionCombo) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return true;
    if (getAllowedAocIds(user, attributeOptionCombo).length == 0) return false;
    limitStatements();
    Access access = createAccess(user, true, null, true);
    String sql =
        access.with()
            + """

            SELECT count(*) AS audit_count
            FROM (
                SELECT de.dataelementid, ou.organisationunitid, coc.categoryoptioncomboid
                FROM dataelement de, organisationunit ou, categoryoptioncombo coc
                WHERE de.dataelementid = :de
                  AND ou.organisationunitid = :ou
                  AND coc.categoryoptioncomboid = :coc
            ) dva
            WHERE\s"""
            + access.where();
    return ((Number)
                getSession()
                    .createNativeQuery(sql)
                    .addScalar("audit_count", StandardBasicTypes.LONG)
                    .setParameter("de", dataElement.getId())
                    .setParameter("ou", organisationUnit.getId())
                    .setParameter("coc", categoryOptionCombo.getId())
                    .getSingleResult())
            .longValue()
        > 0;
  }

  /**
   * Cancels the rest of the transaction's statements, which check the access of a user who is not a
   * superuser, when they run longer than {@link
   * org.hisp.dhis.external.conf.ConfigurationKey#CHANGELOG_AGGREGATE_QUERY_TIMEOUT}: a client that
   * gives up does not cancel them.
   */
  private void limitStatements() {
    if (queryTimeout > 0)
      jdbcTemplate.execute("SET LOCAL statement_timeout = '%ds'".formatted(queryTimeout));
  }

  /**
   * The attribute option combos the user may read data of: the default one, and those of attribute
   * category combos with only category options the user may read data of.
   *
   * <p>Resolved up front, so the planner sees a known list instead of guessing the size of a
   * sharing-filtered set of option combos. Copied from {@code
   * HibernateDataExportStore#getAllowedAocIds} and {@code #createAllowedAocQuery} on master
   * (DHIS2-22290), with this store's data read check.
   *
   * @param aoc the only attribute option combo to check, null to check all
   * @return internal ids of the option combos, null when the user reads all (superuser)
   */
  @CheckForNull
  private Long[] getAllowedAocIds(
      @Nonnull UserDetails user, @CheckForNull CategoryOptionCombo aoc) {
    if (user.isSuper()) return null;
    String sql =
        """
        SELECT DISTINCT ccoc.categoryoptioncomboid
        FROM categorycombos_optioncombos ccoc
        JOIN categorycombo cc ON cc.categorycomboid = ccoc.categorycomboid
        WHERE (cc.name = 'default'
            OR cc.datadimensiontype = 'ATTRIBUTE'
            AND NOT EXISTS (
              SELECT 1
              FROM categoryoptioncombos_categoryoptions coc_co
              JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
              WHERE coc_co.categoryoptioncomboid = ccoc.categoryoptioncomboid
                AND NOT (%s)))"""
            .formatted(createDataReadCheck("co.sharing", user));
    if (aoc != null) sql += "\n  AND ccoc.categoryoptioncomboid = :aoc";
    NativeQuery<?> query =
        getSession()
            .createNativeQuery(sql)
            .addScalar("categoryoptioncomboid", StandardBasicTypes.LONG);
    if (aoc != null) query.setParameter("aoc", aoc.getId());
    return query.getResultList().stream().map(Long.class::cast).toArray(Long[]::new);
  }

  /**
   * Creates the query for the data value audits matching the given parameters that the current user
   * may read. Matches nothing if none of the given periods exist.
   *
   * @param aocAllowed internal ids of the attribute option combos the rows are limited to, null for
   *     no limit; must not be empty (an empty list would match nothing)
   */
  @SuppressWarnings("unchecked")
  private <T> NativeQuery<T> createAuditQuery(
      String select,
      DataValueAuditQueryParams params,
      String orderBy,
      Class<T> type,
      UserDetails user,
      @CheckForNull Long[] aocAllowed) {
    Map<String, Object> parameters = new HashMap<>();
    Access access = createAccess(user, params);
    StringBuilder sql =
        new StringBuilder(access == null ? "" : access.with() + "\n")
            .append(select)
            .append("\nFROM datavalueaudit dva\nWHERE 1=1");

    if (!params.getPeriods().isEmpty()) {
      List<Period> storedPeriods =
          params.getPeriods().stream()
              .map(periodStore::reloadPeriod)
              .filter(Objects::nonNull)
              .toList();
      if (storedPeriods.isEmpty()) {
        sql.append("\n  AND 1=0");
      } else {
        sql.append("\n  AND dva.periodid IN (:periods)");
        parameters.put("periods", storedPeriods.stream().map(Period::getId).toList());
      }
    }
    if (!params.getDataElements().isEmpty()) {
      sql.append("\n  AND dva.dataelementid IN (:dataElements)");
      parameters.put(
          "dataElements", params.getDataElements().stream().map(DataElement::getId).toList());
    }
    if (!params.getOrgUnits().isEmpty()) {
      sql.append("\n  AND dva.organisationunitid IN (:ou)");
      parameters.put("ou", params.getOrgUnits().stream().map(OrganisationUnit::getId).toList());
    }
    if (params.getCategoryOptionCombo() != null) {
      sql.append("\n  AND dva.categoryoptioncomboid = :coc");
      parameters.put("coc", params.getCategoryOptionCombo().getId());
    }
    if (params.getAttributeOptionCombo() != null) {
      sql.append("\n  AND dva.attributeoptioncomboid = :aoc");
      parameters.put("aoc", params.getAttributeOptionCombo().getId());
    }
    if (!params.getAuditTypes().isEmpty()) {
      sql.append("\n  AND dva.audittype IN (:auditTypes)");
      parameters.put("auditTypes", params.getAuditTypes().stream().map(Enum::name).toList());
    }
    if (access != null) sql.append("\n  AND ").append(access.where());
    if (access != null && access.orgUnits() != null) {
      // = ANY gives the planner the estimate of the list
      sql.append("\n  AND dva.organisationunitid = ANY(:orgUnits)");
      // before PostgreSQL 14 it searches the array for every row: a hashed subplan does the test
      // (a count of a user with 500 org units: 15.5 s instead of 1.6 s on PostgreSQL 10); on 14
      // the subplan would only keep the scan from running in parallel
      if (!isPostgres14())
        sql.append(
            "\n  AND (dva.organisationunitid IN (SELECT unnest(CAST(:orgUnits AS bigint[])))) IS TRUE");
    }
    if (aocAllowed != null && isPostgres14())
      sql.append(
          "\n  AND EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dva.attributeoptioncomboid)");
    // before PostgreSQL 14 a semi join with the list, which the planner takes to have 100 rows,
    // could become a nested loop over the list (a count of a user with 1,001 org units and 8,087
    // allowed option combos ran into the timeout on PostgreSQL 10): a hashed subplan instead
    if (aocAllowed != null && !isPostgres14())
      sql.append(
          "\n  AND (dva.attributeoptioncomboid IN (SELECT unnest(CAST(:aocAllowed AS bigint[])))) IS TRUE");
    sql.append(orderBy);

    NativeQuery<T> query =
        type == Long.class
            ? getSession()
                .createNativeQuery(sql.toString())
                .addScalar("audit_count", StandardBasicTypes.LONG)
            : getSession().createNativeQuery(sql.toString(), type);
    parameters.forEach(query::setParameter);
    if (aocAllowed != null) query.setParameter("aocAllowed", aocAllowed, LongArrayType.INSTANCE);
    if (access != null && access.orgUnits() != null)
      query.setParameter("orgUnits", access.orgUnits(), LongArrayType.INSTANCE);
    return query;
  }

  /**
   * Whether the database is PostgreSQL 14 or later, which hashes {@code = ANY} of an array. Before
   * 14 the bound lists are tested with hashed subplans, which on 14 would only keep the scan from
   * running in parallel.
   */
  private boolean isPostgres14() {
    Boolean is14 = postgres14;
    if (is14 == null) {
      String version = jdbcTemplate.queryForObject("SHOW server_version_num", String.class);
      is14 = version != null && Integer.parseInt(version) >= 140000;
      postgres14 = is14;
    }
    return is14;
  }

  /**
   * The access of the user to many values, checking only the category option combo if the params
   * pin it.
   *
   * <p>With org units listed by ID the rest of the transaction's statements are planned with the
   * values bound to them: from the 5th execution on a connection pgjdbc prepares a statement on the
   * server, and a generic plan of PostgreSQL neither hashes a bound array nor knows its size. For a
   * user with 1,001 org units page 1 took 13 s instead of 36 ms, 5,000 org units over 60 s.
   */
  @CheckForNull
  private Access createAccess(UserDetails user, DataValueAuditQueryParams params) {
    if (user.isSuper()) return null;
    // a query for given org units, such as the one of dataValueContext, already has their IDs
    Long[] orgUnits = params.getOrgUnits().isEmpty() ? getOrgUnitIds(user, maxOrgUnitIds) : null;
    // PostgreSQL 10 and 11, which this version supports, have no plan_cache_mode
    if (orgUnits != null)
      jdbcTemplate.execute(
          "SELECT set_config('plan_cache_mode', 'force_custom_plan', true)"
              + " WHERE current_setting('plan_cache_mode', true) IS NOT NULL");
    return createAccess(
        user, params.getCategoryOptionCombo() != null, orgUnits, isOneValue(params));
  }

  /**
   * True when the params select the rows of one data value, as the data value context does: one
   * data element, period and org unit, and both option combos.
   */
  private static boolean isOneValue(DataValueAuditQueryParams params) {
    return params.getDataElements().size() == 1
        && params.getPeriods().size() == 1
        && params.getOrgUnits().size() == 1
        && params.getCategoryOptionCombo() != null
        && params.getAttributeOptionCombo() != null;
  }

  /**
   * The org units in the user's data capture and data view hierarchies, when there are at most
   * {@code max} and the user does not have every top-level org unit.
   *
   * <p>Resolved up front only for the planner, which has statistics for a list of org unit IDs. It
   * cannot estimate the descendant set found inside the query and takes it to match a fixed share
   * of all rows. For a user whose rows are rare it then walks the index on {@code created} over the
   * whole table to fill the first page.
   *
   * <p>The user's org units are written out twice rather than taken from the {@code ou_roots} CTE
   * of the access SQL: the planner has no statistics for a CTE it uses twice and took the user to
   * have 200 of them. With 5,000 it then joined every org unit to every root and did not finish in
   * 60 s; with the subquery it knows how many there are and scans the path index once per root.
   *
   * @return internal IDs of the org units, null when the query finds them: for users with every
   *     top-level org unit, more than {@code max} or none
   */
  @CheckForNull
  private Long[] getOrgUnitIds(@Nonnull UserDetails user, int max) {
    long userId = user.getId();
    String sql =
        """
        SELECT ou.organisationunitid
        FROM organisationunit ou
        JOIN organisationunit root
          ON ou.path ~>=~ root.path AND ou.path ~<~ (root.path || '0')
            AND left(ou.path, length(root.path)) = root.path
        WHERE root.organisationunitid IN (
            SELECT um.organisationunitid FROM usermembership um WHERE um.userinfoid = %1$d
            UNION
            SELECT udv.organisationunitid FROM userdatavieworgunits udv WHERE udv.userinfoid = %1$d)
          AND EXISTS (
            SELECT 1 FROM organisationunit top
            WHERE top.parentid IS NULL
              AND top.organisationunitid NOT IN (
                SELECT um.organisationunitid FROM usermembership um WHERE um.userinfoid = %1$d
                UNION
                SELECT udv.organisationunitid FROM userdatavieworgunits udv WHERE udv.userinfoid = %1$d))"""
            .formatted(userId);
    List<?> ids =
        getSession()
            .createNativeQuery(sql)
            .addScalar("organisationunitid", StandardBasicTypes.LONG)
            .setMaxResults(max + 1)
            .getResultList();
    if (ids.isEmpty() || ids.size() > max) return null;
    // a unit below two of the user's org units comes twice
    return ids.stream().map(Long.class::cast).distinct().toArray(Long[]::new);
  }

  /** The {@code ou_roots} CTE: the user's data capture and data view org units. */
  private static String createOrgUnitRoots(UserDetails user) {
    long userId = user.getId();
    return """
          ou_roots AS (
            SELECT um.organisationunitid FROM usermembership um WHERE um.userinfoid = %d
            UNION
            SELECT udv.organisationunitid FROM userdatavieworgunits udv WHERE udv.userinfoid = %d
          )
        """
        .stripTrailing()
        .formatted(userId, userId);
  }

  /**
   * The data read access of a user who is not a superuser to change log rows {@code dva}, apart
   * from the attribute option combo, which the queries check against the list of {@link
   * #getAllowedAocIds allowed ones}.
   *
   * <p>The user reads a row when its data element belongs to a data set the user may read data of,
   * its organisation unit is in the user's data capture or data view hierarchy, the user may read
   * data of all category options of its category option combo, and its attribute option combo is
   * allowed.
   *
   * <p>Each condition is computed once per query and tested as a hashed subplan: the option combos
   * the user may not read with {@code NOT IN}, the readable data elements and organisation units
   * with {@code (x IN (...)) IS TRUE}, which PostgreSQL does not turn into a semi join. The org
   * units of the user's hierarchy are found with the path index ({@code varchar_pattern_ops}), one
   * range scan per root: a unit is in the hierarchy of a root exactly when its path starts with the
   * root's path. The pattern operators compare bytes, and {@code 0} follows {@code /} and comes
   * before every character of a UID, so {@code [path, path0)} is exactly the root and the paths
   * below it. The prefix test repeats the range for the planner, which estimates such a range at a
   * ninth of all org units per root: the set then seemed too large to hash and was searched again
   * for every audit row. There is no {@code patharray} on this version. Queries for many values of
   * a user with a small hierarchy get its org units {@link #getOrgUnitIds listed by ID} instead.
   * Reads of one data value look up the org unit of their rows in the hierarchy instead: the
   * estimated cost of the set made PostgreSQL 10 prefer a scan of all rows by {@code created} (27 s
   * instead of 33 ms for a user with 5,000 org units).
   *
   * @param with the {@code WITH} clause computing the option combos the user may not read
   * @param where the condition on the alias {@code dva}, without the org units when they are listed
   * @param orgUnits internal IDs of the org units the user reads, bound as {@code :orgUnits}; null
   *     when {@code where} checks the org units
   */
  record Access(@Nonnull String with, @Nonnull String where, @CheckForNull Long[] orgUnits) {}

  /**
   * @param user the user reading the change log
   * @param boundCombo true to check only the category option combo bound as {@code :coc}, which the
   *     query must bind; false to check all
   * @param orgUnits internal IDs of the org units the user reads, not empty, which the query must
   *     bind; null to find them inside the query
   * @param oneValue true when the query reads the rows of one data value: their org unit is then
   *     looked up in the user's hierarchy row by row instead of in the set of all org units of the
   *     hierarchy (ignored when {@code orgUnits} are given)
   * @return the access of the user
   */
  static Access createAccess(
      @Nonnull UserDetails user,
      boolean boundCombo,
      @CheckForNull Long[] orgUnits,
      boolean oneValue) {
    if (orgUnits != null && orgUnits.length == 0)
      throw new IllegalArgumentException("org units must not be empty");
    // the default option combo is excluded: AclService always allows it
    String combos =
        !boundCombo
            ? ""
            : """

              AND coc_co.categoryoptioncomboid = :coc""";
    String roots = orgUnits != null ? "" : "\n" + createOrgUnitRoots(user) + ",";
    String with =
        """
        WITH%s
          coc_no_access AS (
            SELECT DISTINCT coc_co.categoryoptioncomboid
            FROM categoryoptioncombos_categoryoptions coc_co
            JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
            WHERE NOT (%s)
              AND coc_co.categoryoptioncomboid NOT IN (
                SELECT d.categoryoptioncomboid FROM categorycombos_optioncombos d
                JOIN categorycombo dcc ON d.categorycomboid = dcc.categorycomboid
                WHERE dcc.name = 'default')%s
          )"""
            .formatted(roots, createDataReadCheck("co.sharing", user), combos);
    // a user with every top-level org unit reads all org units: no descendant set is built
    String orgUnitCheck;
    if (orgUnits != null) {
      orgUnitCheck = "";
    } else if (oneValue) {
      // the descendant set's estimated cost made PostgreSQL 10 scan the whole table by created
      orgUnitCheck =
          """

            AND (NOT EXISTS (
                SELECT 1 FROM organisationunit top_acl
                WHERE top_acl.parentid IS NULL
                  AND top_acl.organisationunitid NOT IN (SELECT organisationunitid FROM ou_roots))
              OR EXISTS (
                SELECT 1
                FROM organisationunit ou_acl
                JOIN organisationunit root_acl
                  ON ou_acl.path ~>=~ root_acl.path AND ou_acl.path ~<~ (root_acl.path || '0')
                    AND left(ou_acl.path, length(root_acl.path)) = root_acl.path
                WHERE ou_acl.organisationunitid = dva.organisationunitid
                  AND root_acl.organisationunitid IN (SELECT organisationunitid FROM ou_roots)))
          """
              .stripTrailing();
    } else {
      orgUnitCheck =
          """

            AND (NOT EXISTS (
                SELECT 1 FROM organisationunit top_acl
                WHERE top_acl.parentid IS NULL
                  AND top_acl.organisationunitid NOT IN (SELECT organisationunitid FROM ou_roots))
              OR (dva.organisationunitid IN (
                SELECT ou_acl.organisationunitid
                FROM organisationunit ou_acl
                JOIN organisationunit root_acl
                  ON ou_acl.path ~>=~ root_acl.path AND ou_acl.path ~<~ (root_acl.path || '0')
                    AND left(ou_acl.path, length(root_acl.path)) = root_acl.path
                WHERE root_acl.organisationunitid IN (SELECT organisationunitid FROM ou_roots))) IS TRUE)
          """
              .stripTrailing();
    }
    String where =
        """
        (dva.dataelementid IN (
            SELECT dse_acl.dataelementid
            FROM datasetelement dse_acl
            JOIN dataset ds_acl ON dse_acl.datasetid = ds_acl.datasetid
            WHERE %s)) IS TRUE%s
          AND dva.categoryoptioncomboid NOT IN (SELECT categoryoptioncomboid FROM coc_no_access)"""
            .formatted(createDataReadCheck("ds_acl.sharing", user), orgUnitCheck);
    return new Access(with, where, orgUnits);
  }

  /**
   * A condition that is true exactly when the user may read data of the object with the given
   * sharing column, as {@link org.hisp.dhis.security.acl.AclService#canDataRead} decides for a user
   * who is not a superuser: the public access, the user's own entry, or the entry of one of the
   * user's groups grants data read or data write. An entry without access string, or no public
   * access string, grants access, as {@link
   * org.hisp.dhis.security.acl.AccessStringHelper#isEnabled} does. Ownership does not grant data
   * access. The condition is never null, also for sharing without, or with JSON {@code null}, user
   * or user group entries.
   *
   * <p>This follows {@code AclService.canDataRead} on purpose and deliberately differs from {@code
   * JpaQueryUtils.generateSQlQueryForSharingCheck}: ownership does not grant data read, and data
   * write implies data read. A shared helper for both checks is a planned follow-up.
   */
  private static String createDataReadCheck(String sharing, UserDetails user) {
    String uid = user.getUid();
    String publicAccess = "(%s->>'public')".formatted(sharing);
    String check =
        "%s OR jsonb_typeof(%s->'users'->'%s') = 'object' AND (%s)"
            .formatted(
                grantsDataRead(publicAccess),
                sharing,
                uid,
                grantsDataRead("(%s->'users'->'%s'->>'access')".formatted(sharing, uid)));
    Set<String> groups = user.getUserGroupIds();
    if (groups != null && !groups.isEmpty()) {
      // CASE, not AND, guarantees jsonb_each only sees an object (migrated sharing has JSON null)
      check +=
          (" OR EXISTS (SELECT 1 FROM jsonb_each(CASE WHEN jsonb_typeof(%1$s->'userGroups') ="
                  + " 'object' THEN %1$s->'userGroups' ELSE '{}' END) g"
                  + " WHERE g.key = ANY(cast('{%2$s}' as text[])) AND (%3$s))")
              .formatted(
                  sharing,
                  String.join(",", groups.stream().sorted().toList()),
                  grantsDataRead("(g.value->>'access')"));
    }
    return "(%s) IS TRUE".formatted(check);
  }

  /** An access string grants data read when it is missing, or allows data read or data write. */
  private static String grantsDataRead(String access) {
    return "(%1$s IS NULL OR %1$s LIKE '%2$s' OR %1$s LIKE '%3$s')"
        .formatted(access, LIKE_READ_DATA, LIKE_WRITE_DATA);
  }
}
