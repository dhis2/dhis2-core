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
package org.hisp.dhis.datavalue.hibernate;

import static java.lang.System.currentTimeMillis;
import static java.util.function.Function.identity;
import static org.hisp.dhis.query.JpaQueryUtils.generateSQlQueryForSharingCheck;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportParams.Order;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HibernateDataExportStore implements DataExportStore {

  private final EntityManager entityManager;

  @Override
  @CheckForNull
  public DataExportValue exportValue(@Nonnull DataEntryKey key) {
    String sql =
        """
      SELECT
        de.uid AS de,
        pe.iso,
        ou.uid AS ou,
        coc.uid AS coc,
        aoc.uid AS aoc,
        de.valuetype,
        dv.value,
        dv.comment,
        dv.followup,
        dv.storedby,
        dv.created,
        dv.lastupdated,
        dv.deleted
      FROM datavalue dv
      JOIN dataelement de ON dv.dataelementid = de.dataelementid
      JOIN period pe ON dv.periodid = pe.periodid
      JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
      JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
      JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE dv.deleted = false
        AND de.uid = :de
        AND pe.iso = :pe
        AND ou.uid = :ou
        AND (cast(:coc as text) IS NOT NULL AND coc.uid = :coc OR :coc IS NULL AND coc.name = 'default')
        AND (cast(:aoc as text) IS NOT NULL AND aoc.uid = :aoc OR :aoc IS NULL AND aoc.name = 'default')
      LIMIT 1""";
    return createQuery(sql)
        .setParameter("de", key.dataElement())
        .setParameter("ou", key.orgUnit())
        .setParameter("pe", key.period())
        .setParameter("coc", key.categoryOptionCombo())
        .setParameter("aoc", key.attributeOptionCombo())
        .stream()
        .map(DataExportValue::of)
        .findFirst()
        .orElse(null);
  }

  @Override
  @UsageTestOnly
  public List<DataExportValue> getAllDataValues() {
    return exportValues(DataExportParams.builder().includeDeleted(false).build()).toList();
  }

  @Nonnull
  @Override
  public Stream<DataExportValue> exportValues(@Nonnull DataExportParams params) {
    return createExportQuery(
            params, NativeSQL.of(getSession()), CurrentUserUtil.getCurrentUserDetails())
        .stream()
        .map(DataExportValue::of);
  }

  static QueryBuilder createExportQuery(
      DataExportParams params, SQL.QueryAPI api, UserDetails currentUser) {
    String sql =
        """
      WITH
      de_ids AS (
        SELECT dataelementid
        FROM (
                (SELECT cast(NULL as bigint) AS dataelementid WHERE false)
          UNION (SELECT de.dataelementid FROM dataelement de WHERE de.uid = ANY(:de))
          UNION (SELECT dse.dataelementid FROM datasetelement dse \
            JOIN dataset ds ON dse.datasetid = ds.datasetid WHERE ds.uid = ANY(:ds))
          UNION (SELECT degm.dataelementid FROM dataelementgroupmembers degm \
            JOIN dataelementgroup deg ON degm.dataelementgroupid = deg.dataelementgroupid WHERE deg.uid = ANY(:deg))
        ) de_all
        WHERE dataelementid IS NOT NULL
      ),
      pe_ids AS (
        SELECT periodid
        FROM period
        WHERE 1=1
          AND iso = ANY(:pe)
          AND startDate >= :start
          AND endDate <= :end
          AND startdate <= :includedDate
          AND enddate >= :includedDate
          AND periodtypeid IN (SELECT periodtypeid FROM periodtype WHERE name = ANY(:pt))
      ),
      ou_ids AS (
        SELECT organisationunitid
        FROM (
          (SELECT cast(NULL as bigint) AS organisationunitid WHERE false)
          UNION (SELECT ou.organisationunitid FROM organisationunit ou WHERE ou.uid = ANY(:ou))
          UNION (SELECT ougm.organisationunitid FROM orgunitgroupmembers ougm \
                 JOIN orgunitgroup oug ON ougm.orgunitgroupid = oug.orgunitgroupid \
                 JOIN organisationunit ou ON ougm.organisationunitid = ou.organisationunitid \
                 WHERE oug.uid = ANY(:oug) AND (:super OR ou.uid = ANY(:capture)))
        ) ou_all
        WHERE organisationunitid IS NOT NULL
      ),
      ou_with_descendants_ids AS (
        SELECT DISTINCT ou.organisationunitid
        FROM organisationunit ou
        LEFT JOIN organisationunit parent_ou ON (ou.path LIKE parent_ou.path || '%')
        WHERE ou.organisationunitid IN (SELECT organisationunitid FROM ou_ids)
             OR parent_ou.organisationunitid IN (SELECT organisationunitid FROM ou_ids)
      )
      SELECT
        de.uid AS deid,
        pe.iso,
        ou.uid AS ouid,
        coc.uid AS cocid,
        aoc.uid AS aocid,
        de.valuetype,
        dv.value,
        dv.comment,
        dv.followup,
        dv.storedby,
        dv.created,
        dv.lastupdated,
        dv.deleted
      FROM datavalue dv
      JOIN de_ids ON dv.dataelementid = de_ids.dataelementid
      JOIN pe_ids ON dv.periodid = pe_ids.periodid
      JOIN ou_ids ON dv.sourceid = ou_ids.organisationunitid
      JOIN ou_with_descendants_ids ON dv.sourceid = ou_with_descendants_ids.organisationunitid
      JOIN dataelement de ON dv.dataelementid = de.dataelementid
      JOIN period pe ON dv.periodid = pe.periodid
      JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
      JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
      JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE 1=1
        AND coc.uid = ANY(:coc)
        AND aoc.uid = ANY(:aoc)
        AND dv.lastupdated >= :lastUpdated
        AND dv.deleted = :deleted
        AND ou.hierarchylevel = :level
        -- access check below must be 1 line for erasure
        AND NOT EXISTS (SELECT 1 FROM categoryoptioncombos_categoryoptions coc_co \
          JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid \
          WHERE coc_co.categoryoptioncomboid = aoc.categoryoptioncomboid AND NOT (:aocAccess))""";
    Date lastUpdated = params.getLastUpdated();
    if (lastUpdated == null && params.getLastUpdatedDuration() != null)
      lastUpdated = new Date(currentTimeMillis() - params.getLastUpdatedDuration().toMillis());

    String aocAclSql = null;
    // explicit AOCs mean they are already sharing checked
    if (params.getAttributeOptionCombos() == null || params.getAttributeOptionCombos().isEmpty()) {
      if (!currentUser.isSuper())
        aocAclSql = generateSQlQueryForSharingCheck("co.sharing", currentUser, LIKE_READ_DATA);
    }

    boolean descendants = params.isIncludeDescendants();
    List<Order> orders = params.getOrders();
    if (orders == null || orders.isEmpty()) orders = List.of(Order.PE, Order.CREATED, Order.DE);
    return SQL.of(sql, api)
        .setParameter("ds", params.getDataSets())
        .setParameter("de", params.getDataElements())
        .setParameter("deg", params.getDataElementGroups())
        .setParameter("pe", params.getPeriods(), Period::getIsoDate)
        .setParameter("pt", params.getPeriodTypes(), PeriodType::getName)
        .setParameter("start", params.getStartDate())
        .setParameter("end", params.getEndDate())
        .setParameter("includedDate", params.getIncludedDate())
        .setParameter("ou", params.getOrganisationUnits())
        .setParameter("oug", params.getOrganisationUnitGroups())
        .setParameter("level", params.getOrgUnitLevel())
        .setParameter("coc", params.getCategoryOptionCombos())
        .setParameter("aoc", params.getAttributeOptionCombos())
        .setParameter("lastUpdated", lastUpdated)
        .setParameter("deleted", params.isIncludeDeleted() ? null : false)
        .setDynamicClause("aocAccess", aocAclSql)
        .eraseNullParameterLines()
        // keep params below even when null
        .setParameter("super", currentUser.isSuper())
        .setParameter("capture", currentUser.getUserOrgUnitIds(), identity())
        .eraseJoinLine("de_ids", !params.hasDataElementFilters())
        .eraseJoinLine("pe_ids", !params.hasPeriodFilters())
        .eraseJoinLine("ou_with_descendants_ids", !descendants || !params.hasOrgUnitFilters())
        .eraseJoinLine("ou_ids", descendants || !params.hasOrgUnitFilters())
        .useEqualsOverInForParameters("de", "pe", "pt", "ou", "path", "coc", "aoc")
        .setLimit(params.getLimit())
        .setOffset(params.getOffset())
        .setOrders(
            orders,
            Map.ofEntries(
                Map.entry(Order.OU, "ou.path"),
                Map.entry(Order.PE, "pe.startdate, pe.enddate"),
                Map.entry(Order.CREATED, "dv.created"),
                Map.entry(Order.DE, "deid"),
                Map.entry(Order.AOC, "aocid")));
  }

  @CheckForNull
  @Override
  public UID getAttributeOptionCombo(
      @CheckForNull UID categoryCombo, @Nonnull Stream<UID> categoryOptions) {
    String sql =
        """
        SELECT coc.uid
        FROM categoryoptioncombo coc
        INNER JOIN categorycombos_optioncombos c_coc ON c_coc.categoryoptioncomboid = coc.categoryoptioncomboid
        INNER JOIN categorycombo cc ON cc.categorycomboid = c_coc.categorycomboid
        INNER JOIN categoryoptioncombos_categoryoptions coc_co ON coc_co.categoryoptioncomboid = coc.categoryoptioncomboid
        INNER JOIN categoryoption co ON co.categoryoptionid = coc_co.categoryoptionid
        WHERE 1=1
        AND cc.uid = :cc
        AND cc.name = :name
        AND co.uid = ANY (:co)
        GROUP BY coc.uid
        HAVING COUNT(DISTINCT co.uid) = :n""";
    List<UID> co = categoryOptions.toList();
    return createQuery(sql)
        .setParameter("cc", categoryCombo == null ? null : categoryCombo.getValue())
        .setParameter("name", categoryCombo == null ? "default" : null)
        .setParameter("co", co)
        .setParameter("n", co.size())
        .eraseNullParameterLines()
        .stream(String.class)
        .map(UID::of)
        .findFirst()
        .orElse(null);
  }

  @Override
  @Nonnull
  public List<String> getDataSetsNoDataReadAccess(@Nonnull Stream<UID> dataSets) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return List.of();
    String accessSql = generateSQlQueryForSharingCheck("ds.sharing", user, LIKE_READ_DATA);
    String sql =
        """
      SELECT ds.uid
      FROM dataset ds
      WHERE ds.uid = ANY(:ds)
        AND NOT (:access)""";
    return createQuery(sql)
        .setParameter("ds", dataSets)
        .setDynamicClause("access", accessSql)
        .useEqualsOverInForParameters("ds")
        .stream(String.class)
        .toList();
  }

  @Nonnull
  @Override
  public List<String> getAocNoDataReadAccess(@Nonnull Stream<UID> attributeOptionCombos) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return List.of();
    String accessSql = generateSQlQueryForSharingCheck("co.sharing", user, LIKE_READ_DATA);
    String sql =
        """
      SELECT coc.uid
      FROM categoryoptioncombo coc
      WHERE coc.uid = ANY (:coc)
        AND EXISTS(
          SELECT 1
          FROM categoryoptioncombos_categoryoptions aoc_co
          JOIN categoryoption co ON aoc_co.categoryoptionid = co.categoryoptionid
          WHERE coc.categoryoptioncomboid = aoc_co.categoryoptioncomboid
            AND NOT (:access)
      )""";
    return createQuery(sql)
        .setParameter("coc", attributeOptionCombos)
        .setDynamicClause("access", accessSql)
        .useEqualsOverInForParameters("coc")
        .stream(String.class)
        .toList();
  }

  @Nonnull
  @Override
  public List<String> getOrgUnitsNotInUserHierarchy(@Nonnull Stream<UID> orgUnits) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return List.of();
    String sql =
        """
      SELECT ou.uid
      FROM organisationunit ou
      WHERE ou.uid = ANY(:ou)
        AND NOT EXISTS(
          SELECT 1
          FROM organisationunit parent
          WHERE parent.uid = ANY(:parent)
            AND (ou.path = parent.path OR ou.path LIKE parent.path || '/%')
      )""";
    return createQuery(sql)
        .setParameter("ou", orgUnits)
        .setParameter("parent", user.getUserOrgUnitIds(), identity())
        .useEqualsOverInForParameters("ou")
        .stream(String.class)
        .toList();
  }

  private QueryBuilder createQuery(@Language("sql") String sql) {
    return SQL.of(sql, NativeSQL.of(getSession()));
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
