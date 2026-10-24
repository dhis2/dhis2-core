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
package org.hisp.dhis.datavalue.hibernate;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_AGGREGATE_QUERY_TIMEOUT;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;
import static org.hisp.dhis.security.acl.AclService.LIKE_WRITE_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;
import org.hisp.dhis.datavalue.DataValueChangelog;
import org.hisp.dhis.datavalue.DataValueChangelogEntry;
import org.hisp.dhis.datavalue.DataValueChangelogQueryParams;
import org.hisp.dhis.datavalue.DataValueChangelogStore;
import org.hisp.dhis.datavalue.DataValueChangelogType;
import org.hisp.dhis.datavalue.DataValueKey;
import org.hisp.dhis.datavalue.DataValueQueryParams;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.UserDetails;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Repository
public class HibernateDataValueChangelogStore extends HibernateGenericStore<DataValueChangelog>
    implements DataValueChangelogStore {

  /**
   * Users with at most this many org units in their hierarchy get them listed by ID in queries for
   * many values (at most 80 kB), see {@link #getOrgUnitIds}.
   */
  public static final int MAX_ORG_UNIT_IDS = 10_000;

  /** Seconds a statement of a user who is not a superuser may run, 0 for no limit. */
  private final int queryTimeout;

  private int maxOrgUnitIds = MAX_ORG_UNIT_IDS;

  /** Whether the database is PostgreSQL 14 or later, null until asked. */
  private volatile Boolean postgres14;

  public HibernateDataValueChangelogStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      DhisConfigurationProvider config) {
    super(entityManager, jdbcTemplate, publisher, DataValueChangelog.class, false);
    this.queryTimeout = config.getIntProperty(CHANGELOG_AGGREGATE_QUERY_TIMEOUT);
  }

  /**
   * @param max the most org units to list by ID, 0 to always find them inside the query
   */
  @UsageTestOnly
  public void setMaxOrgUnitIds(int max) {
    this.maxOrgUnitIds = max;
  }

  /**
   * @param is14 true to query as on PostgreSQL 14 or later, false as before 14, null to ask the
   *     server
   */
  @UsageTestOnly
  public void setPostgres14(@CheckForNull Boolean is14) {
    this.postgres14 = is14;
  }

  @Override
  public void deleteByOrgUnit(@Nonnull UID orgUnit) {
    String sql =
        """
      DELETE FROM datavalueaudit dva
      WHERE dva.organisationunitid = (SELECT ou.organisationunitid FROM organisationunit ou WHERE ou.uid = :ou)""";

    nativeSynchronizedQuery(sql).setParameter("ou", orgUnit.getValue()).executeUpdate();
  }

  @Override
  public void deleteByDataElement(@Nonnull UID dataElement) {
    String sql =
        """
      DELETE FROM datavalueaudit dva
      WHERE dva.dataelementid = (SELECT de.dataelementid FROM dataelement de WHERE de.uid = :de)""";

    nativeSynchronizedQuery(sql).setParameter("de", dataElement.getValue()).executeUpdate();
  }

  @Override
  public void deleteByOptionCombo(@Nonnull UID categoryOptionCombo) {
    String sql =
        """
        DELETE FROM datavalueaudit dva
        WHERE dva.categoryoptioncomboid = (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc)
           OR dva.attributeoptioncomboid = (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc);""";
    nativeSynchronizedQuery(sql)
        .setParameter("coc", categoryOptionCombo.getValue())
        .executeUpdate();
  }

  @Override
  public List<DataValueChangelog> getEntries(DataValueChangelogQueryParams params) {
    SQL.QueryAPI api = NativeSQL.of(getSession());
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(api, user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return List.of();
    if (aocAllowed != null) limitStatements();
    return createEntriesQuery(
            params, createAccess(api, user, params), aocAllowed, isPostgres14(), api)
        .stream(DataValueChangelog.class)
        .toList();
  }

  @Override
  public int countEntries(DataValueChangelogQueryParams params) {
    SQL.QueryAPI api = NativeSQL.of(getSession());
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(api, user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return 0;
    if (aocAllowed != null) limitStatements();
    return createEntriesQuery(
            params, createAccess(api, user, params), aocAllowed, isPostgres14(), api)
        .count();
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
   * The access of the user to many values, checking only the option combo if the params pin it.
   *
   * <p>With org units listed by ID the rest of the transaction's statements are planned with the
   * values bound to them: from the 5th execution on a connection pgjdbc prepares a statement on the
   * server, and a generic plan of PostgreSQL neither hashes a bound array nor knows its size. For a
   * user with 1,001 org units page 1 took 13 s instead of 36 ms, 5,000 org units over 60 s.
   */
  @CheckForNull
  private Access createAccess(
      SQL.QueryAPI api, UserDetails user, DataValueChangelogQueryParams params) {
    if (user.isSuper()) return null;
    Long[] orgUnits = getOrgUnitIds(api, user, maxOrgUnitIds);
    if (orgUnits != null) jdbcTemplate.execute("SET LOCAL plan_cache_mode = force_custom_plan");
    return createAccess(
        user, params.getCategoryOptionCombo() != null, orgUnits, isOneValue(params));
  }

  /**
   * True when the params select the rows of one data value: one data element, period and org unit,
   * and both option combos.
   */
  private static boolean isOneValue(DataValueChangelogQueryParams params) {
    return isOne(params.getDataElements())
        && isOne(params.getPeriods())
        && isOne(params.getOrgUnits())
        && params.getCategoryOptionCombo() != null
        && params.getAttributeOptionCombo() != null;
  }

  private static boolean isOne(@CheckForNull List<?> items) {
    return items != null && items.size() == 1;
  }

  /**
   * The org units in the user's data capture and data view hierarchies, when there are at most
   * {@code max} and the user does not have every top-level org unit.
   *
   * <p>Resolved up front only for the planner, which has statistics for a list of org unit IDs. It
   * cannot estimate the descendant set found inside the query and takes it to match a fixed share
   * of all rows. For a user whose rows are rare it then walks the index on {@code created} over the
   * whole table to fill the first page: for a user with one facility in 5.6 million rows it took 12
   * s without the list and 0.3 s with it.
   *
   * @return internal IDs of the org units, null when the query finds them: for users with every
   *     top-level org unit, more than {@code max} or none
   */
  @CheckForNull
  static Long[] getOrgUnitIds(SQL.QueryAPI api, @Nonnull UserDetails user, int max) {
    String sql =
        """
        WITH
        %s
        SELECT ou.organisationunitid
        FROM organisationunit ou
        JOIN organisationunit root ON ou.patharray && ARRAY[root.uid]
        WHERE root.organisationunitid IN (SELECT organisationunitid FROM ou_roots)
          AND EXISTS (
            SELECT 1 FROM organisationunit top
            WHERE top.parentid IS NULL
              AND top.organisationunitid NOT IN (SELECT organisationunitid FROM ou_roots))"""
            .formatted(createOrgUnitRoots(user));
    // a single bigint column comes back as a plain number per row, not as an Object[] row
    List<Long> ids =
        SQL.of(sql, api).setLimit(max + 1).stream(Number.class).map(Number::longValue).toList();
    if (ids.isEmpty() || ids.size() > max) return null;
    // a unit below two of the user's org units comes twice
    return ids.stream().distinct().toArray(Long[]::new);
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
  static Long[] getAllowedAocIds(
      SQL.QueryAPI api, @Nonnull UserDetails user, @CheckForNull UID aoc) {
    if (user.isSuper()) return null;
    // a single bigint column comes back as a plain number per row, not as an Object[] row
    return createAllowedAocQuery(api, user, aoc).stream(Number.class)
        .map(Number::longValue)
        .toArray(Long[]::new);
  }

  static QueryBuilder createAllowedAocQuery(
      SQL.QueryAPI api, @Nonnull UserDetails user, @CheckForNull UID aoc) {
    String sql =
        """
        SELECT DISTINCT ccoc.categoryoptioncomboid
        FROM categorycombos_optioncombos ccoc
        JOIN categorycombo cc ON cc.categorycomboid = ccoc.categorycomboid
        JOIN categoryoptioncombo aoc ON aoc.categoryoptioncomboid = ccoc.categoryoptioncomboid
        WHERE (cc.name = 'default'
            OR cc.datadimensiontype = 'ATTRIBUTE'
            AND NOT EXISTS (
              SELECT 1
              FROM categoryoptioncombos_categoryoptions coc_co
              JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
              WHERE coc_co.categoryoptioncomboid = ccoc.categoryoptioncomboid
                AND NOT (:access)))
          AND aoc.uid = :aoc""";
    return SQL.of(sql, api)
        .setParameter("aoc", aoc)
        .setDynamicClause("access", createDataReadCheck("co.sharing", user))
        .eraseNullParameterLines()
        .eraseNullParameterJoinLine("aoc", "aoc");
  }

  @Override
  public List<String> getDataSetsNoDataReadAccess(@Nonnull Collection<UID> dataSets) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper() || dataSets.isEmpty()) return List.of();
    String sql =
        """
        SELECT ds.uid
        FROM dataset ds
        WHERE ds.uid = ANY(:ds)
          AND NOT :access""";
    return SQL
        .of(sql, NativeSQL.of(getSession()))
        .setParameter("ds", dataSets)
        .setDynamicClause("access", createDataReadCheck("ds.sharing", user))
        .stream(String.class)
        .toList();
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
   * with {@code (x IN (...)) IS TRUE}, which PostgreSQL does not turn into a semi join. Joins and
   * semi joins are planned from row estimates for the sharing conditions and filters that can be
   * far off, which made a nested loop over every audit row. The org units of the user's hierarchy
   * are found with the GIN index on {@code patharray} (V2_43_69), one lookup per root, as the data
   * value export does on master: a unit is in the hierarchy of a root exactly when its path array
   * has the root. Queries for many values of a user with a small hierarchy get its org units {@link
   * #getOrgUnitIds listed by ID} instead. Reads of one data value, such as the data value context,
   * look up the org unit of their rows in the hierarchy instead: on PostgreSQL 10 the estimated
   * cost of the set made the planner prefer a scan of all rows by {@code created} (27 s instead of
   * 33 ms for a user with 5,000 org units, on 2.40).
   *
   * @param with the {@code WITH} clause computing the option combos the user may not read
   * @param where the condition on the alias {@code dva}, without the org units when they are listed
   * @param orgUnits internal IDs of the org units the user reads, bound as {@code :orgUnits}; null
   *     when {@code where} checks the org units
   */
  record Access(@Nonnull String with, @Nonnull String where, @CheckForNull Long[] orgUnits) {}

  /**
   * @param user the user reading the change log
   * @param boundCombo true to check only the category option combo bound as {@code :coc} (a null
   *     value for the default combo), which the query must bind; false to check all
   * @param orgUnits internal IDs of the org units the user reads, not empty, which the query must
   *     bind; null to find them inside the query
   * @param oneValue true when the query reads the rows of one data value: their org unit is then
   *     looked up in the user's hierarchy row by row instead of in the set of all org units of the
   *     hierarchy (ignored when {@code orgUnits} are given)
   * @return the access of the user, or null when the user reads all rows
   */
  @CheckForNull
  static Access createAccess(
      @Nonnull UserDetails user,
      boolean boundCombo,
      @CheckForNull Long[] orgUnits,
      boolean oneValue) {
    if (user.isSuper()) return null;
    if (orgUnits != null && orgUnits.length == 0)
      throw new IllegalArgumentException("org units must not be empty");
    // the default option combo is excluded: AclService always allows it
    String combos =
        !boundCombo
            ? ""
            : """

              AND coc_co.categoryoptioncomboid IN (
                SELECT c.categoryoptioncomboid FROM categoryoptioncombo c
                WHERE c.uid = cast(:coc as text))""";
    String roots = orgUnits != null ? "" : "\n" + createOrgUnitRoots(user) + ",";
    String with =
        """
        WITH%s
          coc_no_access AS MATERIALIZED (
            SELECT DISTINCT coc_co.categoryoptioncomboid
            FROM categoryoptioncombos_categoryoptions coc_co
            JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
            WHERE NOT (%s)
              AND coc_co.categoryoptioncomboid NOT IN (
                SELECT d.categoryoptioncomboid FROM categoryoptioncombo d WHERE d.name = 'default')%s
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
                  ON ou_acl.patharray && ARRAY[root_acl.uid]
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
                  ON ou_acl.patharray && ARRAY[root_acl.uid]
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

  /**
   * @param aocAllowed internal ids of the attribute option combos the rows are limited to, null for
   *     no limit; must not be empty (an empty list would erase the condition)
   * @param postgres14 true to test the bound lists as PostgreSQL 14 and later can, false to test
   *     them with hashed subplans as needed before 14
   */
  static QueryBuilder createEntriesQuery(
      DataValueChangelogQueryParams params,
      @CheckForNull Access access,
      @CheckForNull Long[] aocAllowed,
      boolean postgres14,
      SQL.QueryAPI api) {
    if (aocAllowed != null && aocAllowed.length == 0)
      throw new IllegalArgumentException("AOC limit must not be empty");
    // = ANY(:orgUnits) gives the planner the estimate of the list. Before PostgreSQL 14 it searches
    // the array for every row: a hashed subplan does the test (a count of a user with 500 org
    // units: 15.5 s instead of 1.6 s on PostgreSQL 10). A semi join with the allowed combos, which
    // the planner takes to have 100 rows, could become a nested loop over the list before 14 (a
    // count of a user with 1,001 org units and 8,087 allowed combos ran into the timeout on
    // PostgreSQL 10): a hashed subplan instead. On 14 the subplans would only keep the scan from
    // running in parallel.
    String sql =
        """
      :accessWith
      SELECT *
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      JOIN dataelement de ON dva.dataelementid = de.dataelementid
      JOIN organisationunit ou ON dva.organisationunitid = ou.organisationunitid
      JOIN datasetelement dse ON dva.dataelementid = dse.dataelementid
      JOIN dataset ds ON dse.datasetid = ds.datasetid
      WHERE 1=1 -- below filters might be erased...
        AND dva.audittype = ANY(:types)
        AND dva.categoryoptioncomboid = (SELECT coc.categoryoptioncomboid FROM categoryoptioncombo coc WHERE coc.uid = :coc)
        AND dva.attributeoptioncomboid = (SELECT aoc.categoryoptioncomboid FROM categoryoptioncombo aoc WHERE aoc.uid = :aoc)
        AND ds.uid = ANY(:ds)
        AND de.uid = ANY(:de)
        AND ou.uid = ANY(:ou)
        AND pe.iso = ANY(:pe)
        AND :access
        AND dva.organisationunitid = ANY(:orgUnits)
        AND (dva.organisationunitid IN (SELECT unnest(CAST(:orgUnitsHashed AS bigint[])))) IS TRUE
        AND EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dva.attributeoptioncomboid)
        AND (dva.attributeoptioncomboid IN (SELECT unnest(CAST(:aocAllowedHashed AS bigint[])))) IS TRUE
      ORDER BY dva.created DESC""";

    Pager pager = params.getPager();
    Long[] orgUnits = access == null ? null : access.orgUnits();
    return SQL.of(sql, api)
        .setParameter("types", params.getTypes(), DataValueChangelogType::name)
        .setParameter("pe", params.getPeriods(), Period::getIsoDate)
        .setParameter("ds", params.getDataSets())
        .setParameter("de", params.getDataElements())
        .setParameter("ou", params.getOrgUnits())
        .setParameter("coc", params.getCategoryOptionCombo())
        .setParameter("aoc", params.getAttributeOptionCombo())
        .setParameter("aocAllowed", postgres14 ? aocAllowed : null)
        .setParameter("aocAllowedHashed", postgres14 ? null : aocAllowed)
        .setParameter("orgUnits", orgUnits)
        .setParameter("orgUnitsHashed", postgres14 ? null : orgUnits)
        .setDynamicClause("accessWith", access == null ? null : access.with())
        .setDynamicClause("access", access == null ? null : access.where())
        .setOffset(pager == null ? null : pager.getOffset())
        // one more than the page tells whether there is a next page
        .setLimit(pager == null ? null : pager.getPageSize() + 1)
        .eraseNullParameterLines()
        .useEqualsOverInForParameters("types", "ds", "de", "ou", "pe")
        .eraseNullParameterJoinLine("de", "de")
        .eraseNullParameterJoinLine("ou", "ou")
        .eraseNullParameterJoinLine("pe", "pe")
        .eraseNullParameterJoinLine("dse", "ds")
        .eraseNullParameterJoinLine("ds", "ds");
  }

  @Override
  public List<DataValueChangelogEntry> getEntries(@Nonnull DataValueQueryParams params) {
    return getEntries(toKey(params));
  }

  @Override
  public boolean isReadable(@Nonnull DataValueQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return true;
    DataValueKey key = toKey(params);
    SQL.QueryAPI api = NativeSQL.of(getSession());
    if (!isAocReadable(api, user, key)) return false;
    Access access = createAccess(user, true, null, true);
    limitStatements();
    @Language("sql")
    String sql =
        """
        :accessWith
        SELECT 1
        FROM (
            SELECT de.dataelementid, ou.organisationunitid, coc.categoryoptioncomboid,
                aoc.categoryoptioncomboid AS attributeoptioncomboid
            FROM dataelement de, organisationunit ou, categoryoptioncombo coc, categoryoptioncombo aoc
            WHERE de.uid = :de
              AND ou.uid = :ou
              AND (cast(:coc as text) IS NOT NULL AND coc.uid = :coc OR :coc IS NULL AND coc.name = 'default')
              AND (cast(:aoc as text) IS NOT NULL AND aoc.uid = :aoc OR :aoc IS NULL AND aoc.name = 'default')
        ) dva
        WHERE :access""";
    return SQL
        .of(sql, api)
        .setParameter("de", key.dataElement())
        .setParameter("ou", key.orgUnit())
        .setParameter("coc", key.categoryOptionCombo())
        .setParameter("aoc", key.attributeOptionCombo())
        .setDynamicClause("accessWith", access.with())
        .setDynamicClause("access", access.where())
        .stream()
        .findAny()
        .isPresent();
  }

  /**
   * @return true when the user may read data of the attribute option combo of the key, which all
   *     users may for the default one (null)
   */
  private static boolean isAocReadable(SQL.QueryAPI api, UserDetails user, DataValueKey key) {
    if (key.attributeOptionCombo() == null) return true;
    Long[] allowed = getAllowedAocIds(api, user, key.attributeOptionCombo());
    return allowed == null || allowed.length > 0;
  }

  private DataValueKey toKey(DataValueQueryParams params) {
    String aoc = getCategoryOptionComboIdByComboAndOptions(params.getCc(), params.getCp());
    return new DataValueKey(
        UID.of(params.getDe()),
        UID.of(params.getOu()),
        UID.ofNullable(params.getCo()),
        UID.ofNullable(aoc),
        Period.of(params.getPe()));
  }

  @Override
  public List<DataValueChangelogEntry> getEntries(@Nonnull DataValueKey key) {
    UserDetails user = getCurrentUserDetails();
    SQL.QueryAPI api = NativeSQL.of(getSession());
    if (!isAocReadable(api, user, key)) return List.of();
    Access access = createAccess(user, true, null, true);
    if (access != null) limitStatements();
    @Language("sql")
    String sql =
        """
        :accessWith
        SELECT
            de.uid AS de, pe.iso, ou.uid AS ou, coc.uid AS coc, aoc.uid AS aoc, dva.value, dva.modifiedby, dva.created, dva.audittype
        FROM datavalueaudit dva
        JOIN dataelement de ON dva.dataelementid = de.dataelementid
        JOIN period pe ON dva.periodid = pe.periodid
        JOIN organisationunit ou ON dva.organisationunitid = ou.organisationunitid
        JOIN categoryoptioncombo coc ON dva.categoryoptioncomboid = coc.categoryoptioncomboid
        JOIN categoryoptioncombo aoc ON dva.attributeoptioncomboid = aoc.categoryoptioncomboid
        WHERE -- allow for erasure
                de.uid = :de
            AND ou.uid = :ou
            AND pe.iso = :iso
            AND (cast(:coc as text) IS NOT NULL AND coc.uid = :coc OR :coc IS NULL AND coc.name = 'default')
            AND (cast(:aoc as text) IS NOT NULL AND aoc.uid = :aoc OR :aoc IS NULL AND aoc.name = 'default')
            AND :access
        ORDER BY dva.created DESC""";
    return SQL
        .of(sql, api)
        .setParameter("de", key.dataElement())
        .setParameter("ou", key.orgUnit())
        .setParameter("iso", key.period().getIsoDate())
        .eraseNullParameterLines()
        .setParameter("coc", key.categoryOptionCombo())
        .setParameter("aoc", key.attributeOptionCombo())
        .setDynamicClause("accessWith", access == null ? null : access.with())
        .setDynamicClause("access", access == null ? null : access.where())
        .stream()
        .map(HibernateDataValueChangelogStore::toEntry)
        .toList();
  }

  @CheckForNull
  private String getCategoryOptionComboIdByComboAndOptions(
      String categoryCombo, String categoryOptions) {
    if (categoryCombo == null && categoryOptions == null) return null;
    if (categoryCombo == null || categoryOptions == null)
      throw new IllegalArgumentException("CC and COs must either both be null or defined");
    String sql =
        """
        WITH
        co_ids AS ( SELECT categoryoptionid FROM categoryoption WHERE uid IN (:cos)),
        coc_ids AS (
            SELECT coc_co.categoryoptioncomboid
            FROM categorycombos_optioncombos coc_cc
                     JOIN categoryoptioncombos_categoryoptions coc_co ON coc_cc.categoryoptioncomboid = coc_co.categoryoptioncomboid
            WHERE coc_cc.categorycomboid = (SELECT cc.categorycomboid FROM categorycombo cc WHERE cc.uid = :cc)
              AND coc_co.categoryoptionid IN (SELECT categoryoptionid FROM co_ids)
            GROUP BY coc_co.categoryoptioncomboid
            HAVING COUNT(*) = (SELECT COUNT(*) FROM co_ids)
        )
        SELECT coc.uid
        FROM coc_ids ids
        JOIN public.categoryoptioncombo coc ON ids.categoryoptioncomboid = coc.categoryoptioncomboid""";
    @SuppressWarnings("unchecked")
    Object aocId =
        getSingleResult(
            getSession()
                .createNativeQuery(sql)
                .setParameter("cc", categoryCombo)
                .setParameterList("cos", categoryOptions.split(";")));
    return aocId instanceof String s ? s : null;
  }

  private static DataValueChangelogEntry toEntry(Object[] row) {
    return new DataValueChangelogEntry(
        (String) row[0],
        (String) row[1],
        (String) row[2],
        (String) row[3],
        (String) row[4],
        (String) row[5],
        (String) row[6],
        (Date) row[7],
        DataValueChangelogType.valueOf((String) row[8]));
  }

  @Override
  public void enableAudit() {
    String sql =
        """
      DO $$
      BEGIN
          IF NOT EXISTS (
              SELECT 1 FROM pg_trigger
              WHERE tgname = 'trg_datavalue_audit'
              AND tgrelid = to_regclass('datavalue')
          ) THEN
              CREATE TRIGGER trg_datavalue_audit
                  AFTER INSERT OR UPDATE ON datavalue
                  FOR EACH ROW
                  EXECUTE FUNCTION log_datavalue_audit();
          END IF;
      END;
      $$""";
    // DDL, not entity persistence, so it skips Hibernate and its cache invalidation
    jdbcTemplate.execute(sql);
  }

  @Override
  public void disableAudit() {
    String sql =
        """
      DROP TRIGGER IF EXISTS trg_datavalue_audit ON datavalue""";
    // DDL, not entity persistence, so it skips Hibernate and its cache invalidation
    jdbcTemplate.execute(sql);
  }
}
