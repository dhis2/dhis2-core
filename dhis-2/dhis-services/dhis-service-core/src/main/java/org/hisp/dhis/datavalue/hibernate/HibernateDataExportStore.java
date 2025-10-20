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

import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.query.JpaQueryUtils.generateSQlQueryForSharingCheck;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataExportStore;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sql.NativeSQL;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.util.DateUtils;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HibernateDataExportStore implements DataExportStore {

  private final EntityManager entityManager;

  @Nonnull
  @Override
  public Map<String, String> getIdMapping(
      @Nonnull EncodeType type, @Nonnull IdProperty to, @Nonnull Stream<UID> identifiers) {
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
    String tableName =
        switch (type) {
          case DE -> "dataelement";
          case OU -> "organisationunit";
          case COC -> "categoryoptioncombo";
        };
    String sql = replace(sqlTemplate, Map.of("table", tableName, "property", columnName("t", to)));
    return createSelectQuery(sql).setParameter("ids", ids).listAsStringsMap();
  }

  @Override
  @CheckForNull
  public DataExportValue getDataValue(@Nonnull DataEntryKey key) {
    String sql =
        """
      SELECT
        de.uid AS de,
        pe.iso,
        ou.uid AS ou,
        coc.uid AS coc,
        aoc.uid AS aoc,
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
    return createSelectQuery(sql)
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
    return getDataValues(new DataExportStoreParams().setIncludeDeleted(false)).toList();
  }

  @Override
  public Stream<DataExportValue> getDataValues(DataExportStoreParams params) {
    return createExportQuery(params, NativeSQL.of(getSession())).stream().map(DataExportValue::of);
  }

  static QueryBuilder createExportQuery(DataExportStoreParams params, SQL.QueryAPI api) {
    String sql =
        """
      SELECT
        de.uid AS deid,
        pe.iso,
        ou.uid AS ouid,
        coc.uid AS cocid,
        aoc.uid AS aocid,
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
      JOIN periodtype pt ON pe.periodtypeid = pt.periodtypeid
      JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid
      JOIN categoryoptioncombo coc ON dv.categoryoptioncomboid = coc.categoryoptioncomboid
      JOIN categoryoptioncombo aoc ON dv.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE 1=1 -- filters use null-erasure...
        AND dv.dataelementid = ANY(:de)
        AND pe.iso = ANY(:pe)
        AND pe.startDate >= :start
        AND pe.endDate <= :end
        AND pe.startdate <= :includedDate AND pe.enddate >= :includedDate
        AND pt.name = ANY(:pt)
        AND dv.sourceid = ANY(:ou)
        AND ou.hierarchylevel = :level
        AND ou.hierarchylevel >= :minLevel
        AND ou.path LIKE ANY(:path)
        AND dv.categoryoptioncomboid = ANY(:coc)
        AND dv.attributeoptioncomboid = ANY(:aoc)
        AND dv.lastupdated >= :lastUpdated
        AND dv.deleted = :deleted
        -- access check below must be 1 line for erasure
        AND NOT EXISTS (SELECT 1 FROM categoryoptioncombos_categoryoptions coc_co JOIN categoryoption co ON coc_co.categoryoptionid = co.categoryoptionid WHERE coc_co.categoryoptioncomboid = aoc.categoryoptioncomboid AND NOT (:access))
      ORDER BY ou.path, pe.startdate, pe.enddate, dv.created, deid
      """;
    Set<OrganisationUnit> units = params.getAllOrganisationUnits();
    boolean descendants = params.isIncludeDescendantsForOrganisationUnits();
    String[] path =
        !descendants
            ? new String[0]
            : units.stream().map(ou -> ou.getStoredPath() + "%").toArray(String[]::new);
    Date lastUpdated = null;
    if (params.hasLastUpdatedDuration())
      lastUpdated = DateUtils.nowMinusDuration(params.getLastUpdatedDuration());
    if (params.hasLastUpdated()) lastUpdated = params.getLastUpdated();

    String accessSql =
        params.isOrderForSync()
                || !params.getAttributeOptionCombos().isEmpty()
                || CurrentUserUtil.getCurrentUserDetails().isSuper()
            ? null // explicit AOCs mean they are already sharing checked
            : generateSQlQueryForSharingCheck(
                "co.sharing", CurrentUserUtil.getCurrentUserDetails(), LIKE_READ_DATA);
    return SQL.selectOf(sql, api)
        .setParameter("de", getIds(params.getAllDataElements()))
        .setParameter("pe", params.getPeriods(), Period::getIsoDate)
        .setParameter("pt", params.getPeriodTypes(), PeriodType::getName)
        .setParameter("start", params.getStartDate())
        .setParameter("end", params.getEndDate())
        .setParameter("includedDate", params.getIncludedDate())
        .setParameter("path", path)
        .setParameter("ou", descendants ? null : getIds(units))
        .setParameter("level", descendants ? null : params.getOrgUnitLevel())
        .setParameter("minLevel", descendants ? params.getOrgUnitLevel() : null)
        .setParameter("coc", getIds(params.getCategoryOptionCombos()))
        .setParameter("aoc", getIds(params.getAttributeOptionCombos()))
        .setParameter("lastUpdated", lastUpdated)
        .setParameter("deleted", params.isIncludeDeleted() ? null : false)
        .setDynamicClause("access", accessSql)
        .eraseOrder("ou.path", !params.isOrderByOrgUnitPath())
        .eraseOrder("pe.startdate", !params.isOrderByPeriod() && !params.isOrderForSync())
        .eraseOrder("pe.enddate", !params.isOrderByPeriod())
        .eraseOrder("dv.created", !params.isOrderForSync())
        .eraseOrder("deid", !params.isOrderForSync())
        .eraseNullParameterLines()
        .eraseNullParameterJoinLine("pt", "pt")
        .useEqualsOverInForParameters("de", "pe", "pt", "ou", "path", "coc", "aoc")
        .setLimit(params.getLimit())
        .setOffset(params.getOffset());
  }

  private static Long[] getIds(Collection<? extends IdentifiableObject> objects) {
    return objects == null || objects.isEmpty()
        ? null
        : objects.stream().map(IdentifiableObject::getId).distinct().toArray(Long[]::new);
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

  private QueryBuilder createSelectQuery(@Language("sql") String sql) {
    return SQL.selectOf(sql, NativeSQL.of(getSession()));
  }

  private Session getSession() {
    return entityManager.unwrap(Session.class);
  }
}
