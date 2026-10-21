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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;
import static org.hisp.dhis.security.acl.AclService.LIKE_WRITE_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
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
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final PeriodStore periodStore;

  public HibernateDataValueAuditStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      PeriodStore periodStore) {
    super(entityManager, jdbcTemplate, publisher, DataValueAudit.class, false);

    checkNotNull(periodStore);

    this.periodStore = periodStore;
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

    entityManager.createQuery(hql).setParameter("unit", organisationUnit).executeUpdate();
  }

  @Override
  public void deleteDataValueAudits(DataElement dataElement) {
    String hql = "delete from DataValueAudit d where d.dataElement = :dataElement";

    entityManager.createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  @Override
  public void deleteDataValueAudits(@Nonnull CategoryOptionCombo categoryOptionCombo) {
    String hql =
        "delete from DataValueAudit d where d.categoryOptionCombo = :categoryOptionCombo or d.attributeOptionCombo = :categoryOptionCombo";
    entityManager
        .createQuery(hql)
        .setParameter("categoryOptionCombo", categoryOptionCombo)
        .executeUpdate();
  }

  @Override
  public List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return List.of();
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
          .setMaxResults(params.getPager().getPageSize());
    }

    return query.getResultList();
  }

  @Override
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    Long[] aocAllowed = getAllowedAocIds(user, params.getAttributeOptionCombo());
    if (aocAllowed != null && aocAllowed.length == 0) return 0;
    return createAuditQuery(
            "SELECT count(distinct dva.datavalueauditid) AS audit_count",
            params,
            "",
            Long.class,
            user,
            aocAllowed)
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
    Access access = createAccess(user, true);
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
    Access access =
        user.isSuper() ? null : createAccess(user, params.getCategoryOptionCombo() != null);
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
      sql.append("\n  AND dva.organisationunitid IN (:orgUnits)");
      parameters.put(
          "orgUnits", params.getOrgUnits().stream().map(OrganisationUnit::getId).toList());
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
    if (aocAllowed != null)
      sql.append(
          "\n  AND EXISTS (SELECT 1 FROM unnest(CAST(:aocAllowed AS bigint[])) a(id) WHERE a.id = dva.attributeoptioncomboid)");
    sql.append(orderBy);

    NativeQuery<T> query =
        type == Long.class
            ? getSession()
                .createNativeQuery(sql.toString())
                .addScalar("audit_count", StandardBasicTypes.LONG)
            : getSession().createNativeQuery(sql.toString(), type);
    parameters.forEach(query::setParameter);
    if (aocAllowed != null) query.setParameter("aocAllowed", aocAllowed, LongArrayType.INSTANCE);
    return query;
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
   * for every audit row. There is no {@code patharray} on this version.
   *
   * @param with the {@code WITH} clause computing the option combos the user may not read
   * @param where the condition on the alias {@code dva}
   */
  record Access(@Nonnull String with, @Nonnull String where) {}

  /**
   * @param user the user reading the change log
   * @param boundCombo true to check only the category option combo bound as {@code :coc}, which the
   *     query must bind; false to check all
   * @return the access of the user
   */
  static Access createAccess(@Nonnull UserDetails user, boolean boundCombo) {
    // the default option combo is excluded: AclService always allows it
    String combos =
        !boundCombo
            ? ""
            : """

              AND coc_co.categoryoptioncomboid = :coc""";
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
              ON ou_acl.path ~>=~ root_acl.path AND ou_acl.path ~<~ (root_acl.path || '0')
                AND left(ou_acl.path, length(root_acl.path)) = root_acl.path
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
}
