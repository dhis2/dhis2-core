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
import org.hisp.dhis.datavalue.DataValueChangelog;
import org.hisp.dhis.datavalue.DataValueChangelogEntry;
import org.hisp.dhis.datavalue.DataValueChangelogQueryParams;
import org.hisp.dhis.datavalue.DataValueChangelogStore;
import org.hisp.dhis.datavalue.DataValueChangelogType;
import org.hisp.dhis.datavalue.DataValueKey;
import org.hisp.dhis.datavalue.DataValueQueryParams;
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

  public HibernateDataValueChangelogStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValueChangelog.class, false);
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
    long[] aocAllowed =
        getAllowedAocIds(api, getCurrentUserDetails(), params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return List.of();
    return createEntriesQuery(params, createAccess(params), aocAllowed, api).stream(
            DataValueChangelog.class)
        .toList();
  }

  @Override
  public int countEntries(DataValueChangelogQueryParams params) {
    SQL.QueryAPI api = NativeSQL.of(getSession());
    long[] aocAllowed =
        getAllowedAocIds(api, getCurrentUserDetails(), params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return 0;
    return createEntriesQuery(params, createAccess(params), aocAllowed, api).count();
  }

  /** The access of the current user, checking only the option combo if the params pin it. */
  @CheckForNull
  private static Access createAccess(DataValueChangelogQueryParams params) {
    return createAccess(getCurrentUserDetails(), params.getCategoryOptionCombo() != null);
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
  static long[] getAllowedAocIds(
      SQL.QueryAPI api, @Nonnull UserDetails user, @CheckForNull UID aoc) {
    if (user.isSuper()) return null;
    // a single bigint column comes back as a plain number per row, not as an Object[] row
    return createAllowedAocQuery(api, user, aoc).stream(Number.class)
        .mapToLong(Number::longValue)
        .toArray();
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
   * far off, which made a nested loop over every audit row.
   *
   * @param with the {@code WITH} clause computing the option combos the user may not read
   * @param where the condition on the alias {@code dva}
   */
  record Access(@Nonnull String with, @Nonnull String where) {}

  /**
   * @param user the user reading the change log
   * @param boundCombo true to check only the category option combo bound as {@code :coc} (a null
   *     value for the default combo), which the query must bind; false to check all
   * @return the access of the user, or null when the user reads all rows
   */
  @CheckForNull
  static Access createAccess(@Nonnull UserDetails user, boolean boundCombo) {
    if (user.isSuper()) return null;
    // the default option combo is excluded: AclService always allows it
    String combos =
        !boundCombo
            ? ""
            : """

              AND coc_co.categoryoptioncomboid IN (
                SELECT c.categoryoptioncomboid FROM categoryoptioncombo c
                WHERE c.uid = cast(:coc as text))""";
    String with =
        """
        WITH
          coc_no_access AS MATERIALIZED (
            SELECT DISTINCT coc_co.categoryoptioncomboid
            FROM categoryoptioncombos_categoryoptions coc_co
            JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid
            WHERE NOT (%s)
              AND coc_co.categoryoptioncomboid NOT IN (
                SELECT d.categoryoptioncomboid FROM categoryoptioncombo d WHERE d.name = 'default')%s
          )"""
            .formatted(createDataReadCheck("co.sharing", user), combos);
    long userId = user.getId();
    String where =
        """
        (dva.dataelementid IN (
            SELECT dse_acl.dataelementid
            FROM datasetelement dse_acl
            JOIN dataset ds_acl ON dse_acl.datasetid = ds_acl.datasetid
            WHERE %s)) IS TRUE
          AND (dva.organisationunitid IN (
            SELECT ou_acl.organisationunitid
            FROM organisationunit ou_acl
            JOIN organisationunit root_acl
              ON ou_acl.path = root_acl.path OR ou_acl.path LIKE root_acl.path || '/%%'
            WHERE root_acl.organisationunitid IN (
                SELECT um.organisationunitid FROM usermembership um WHERE um.userinfoid = %d
                UNION
                SELECT udv.organisationunitid FROM userdatavieworgunits udv WHERE udv.userinfoid = %d))) IS TRUE
          AND dva.categoryoptioncomboid NOT IN (SELECT categoryoptioncomboid FROM coc_no_access)"""
            .formatted(createDataReadCheck("ds_acl.sharing", user), userId, userId);
    return new Access(with, where);
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
   */
  static QueryBuilder createEntriesQuery(
      DataValueChangelogQueryParams params,
      @CheckForNull Access access,
      @CheckForNull long[] aocAllowed,
      SQL.QueryAPI api) {
    if (aocAllowed != null && aocAllowed.length == 0)
      throw new IllegalArgumentException("AOC limit must not be empty");
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
        AND EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dva.attributeoptioncomboid)
      ORDER BY dva.created DESC""";

    Pager pager = params.getPager();
    return SQL.of(sql, api)
        .setParameter("types", params.getTypes(), DataValueChangelogType::name)
        .setParameter("pe", params.getPeriods(), Period::getIsoDate)
        .setParameter("ds", params.getDataSets())
        .setParameter("de", params.getDataElements())
        .setParameter("ou", params.getOrgUnits())
        .setParameter("coc", params.getCategoryOptionCombo())
        .setParameter("aoc", params.getAttributeOptionCombo())
        .setParameter("aocAllowed", aocAllowed)
        .setDynamicClause("accessWith", access == null ? null : access.with())
        .setDynamicClause("access", access == null ? null : access.where())
        .setOffset(pager == null ? null : pager.getOffset())
        .setLimit(pager == null ? null : pager.getPageSize())
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
    Access access = createAccess(user, true);
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
    long[] allowed = getAllowedAocIds(api, user, key.attributeOptionCombo());
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
    Access access = createAccess(user, true);
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
