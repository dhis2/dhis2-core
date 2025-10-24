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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.query.JpaQueryUtils.generateSQlQueryForSharingCheck;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_DATA;
import static org.hisp.dhis.security.acl.AclService.LIKE_WRITE_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DbName;
import org.hisp.dhis.common.IdProperty;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UsageTestOnly;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataEntryRow;
import org.hisp.dhis.datavalue.DataEntryStore;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserDetails;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * A store just for handling data value bulk operations.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Repository
public class HibernateDataEntryStore extends HibernateGenericStore<DataValue>
    implements DataEntryStore {

  private final PeriodStore periodStore;

  /**
   * Maximum number of {@code VALUES} pairs that get added to a single {@code INSERT} SQL statement.
   */
  private static final int MAX_ROWS_PER_INSERT = 500;

  public HibernateDataEntryStore(
      EntityManager entityManager,
      PeriodStore periodStore,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValue.class, false);
    this.periodStore = periodStore;
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

  @Override
  public Map<String, String> getIdMapping(
      @Nonnull DecodeType type, @Nonnull IdProperty from, @Nonnull Stream<String> identifiers) {
    String[] ids = identifiers.filter(Objects::nonNull).distinct().toArray(String[]::new);
    if (ids.length == 0) return Map.of();
    @Language("sql")
    String sqlTemplate =
        """
      SELECT ${property}, t.uid
      FROM ${table} t
      JOIN unnest(:ids) AS input(id) ON ${property} = input.id
      """;
    String tableName =
        switch (type) {
          case DS -> "dataset";
          case DE -> "dataelement";
          case OU -> "organisationunit";
          case COC -> "categoryoptioncombo";
        };
    String sql =
        replace(sqlTemplate, Map.of("table", tableName, "property", columnName("t", from)));
    return listAsStringsMap(sql, q -> q.setParameter("ids", ids));
  }

  @Override
  public List<String> getDataSetAocCategories(
      @Nonnull UID dataSet, @Nonnull IdProperty categories) {
    @Language("SQL")
    String sqlTemplate =
        """
      SELECT ${c_id} AS sort_name
      FROM dataset ds
      JOIN categorycombos_categories cc_c ON ds.categorycomboid = cc_c.categorycomboid
      JOIN category c ON cc_c.categoryid = c.categoryid
      ORDER BY sort_name
      """;
    String sql = replace(sqlTemplate, Map.of("c_id", columnName("c", categories)));
    return listAsStrings(sql, q -> q.setParameter("ds", dataSet.getValue()));
  }

  @Override
  public Map<Set<String>, String> getDataSetAocIdMapping(
      @Nonnull UID dataSet, @Nonnull IdProperty attributeOptions) {
    @Language("SQL")
    String sqlTemplate =
        """
        SELECT
          (
            -- 2. translate CO ids (PK) to external ID property used
            SELECT array_agg(${co_id})
            FROM unnest(co_coc.categoryoptionids) AS options(categoryoptionid)
            INNER JOIN categoryoption co ON options.categoryoptionid = co.categoryoptionid
          ) AS category_option_ids,
          coc.uid
        FROM (
          -- 1. expand DS => CC to a mapping of CO IDs as array for each COC
          SELECT
           ARRAY(SELECT DISTINCT unnest(array_agg(coc_co.categoryoptionid))) AS categoryoptionids,
           coc_co.categoryoptioncomboid
          FROM dataset ds
          JOIN categorycombos_optioncombos cc_coc ON ds.categorycomboid = cc_coc.categorycomboid
          JOIN categoryoptioncombos_categoryoptions coc_co ON cc_coc.categoryoptioncomboid = coc_co.categoryoptioncomboid
          GROUP BY coc_co.categoryoptioncomboid
        ) co_coc
        JOIN categoryoptioncombo coc ON co_coc.categoryoptioncomboid = coc.categoryoptioncomboid""";
    Map<String, String> vars = Map.of("co_id", columnName("co", attributeOptions));
    String sql = replace(sqlTemplate, vars);
    @SuppressWarnings("unchecked")
    Stream<Object[]> rows =
        getSession().createNativeQuery(sql).setParameter("ds", dataSet.getValue()).stream();
    return rows.collect(toMap(row -> Set.of((String[]) row[0]), row -> (String) row[1]));
  }

  @Override
  public Map<String, List<String>> getDataElementCocCategories(
      @Nonnull UID dataSet,
      @Nonnull IdProperty categories,
      @Nonnull IdProperty dataElements,
      @Nonnull Stream<String> dataElementIds) {
    @Language("SQL")
    String sqlTemplate =
        """
      -- effective data-element view
      WITH data_elements AS (
          SELECT ${de_id} AS de_id, coalesce(dse.categorycomboid, de.categorycomboid) AS categorycomboid
          FROM dataset ds
          JOIN datasetelement dse ON ds.datasetid = dse.datasetid
          JOIN dataelement de ON dse.dataelementid = de.dataelementid
          JOIN unnest(:de) AS dex(id) ON ${de_id} = dex.id
          WHERE ds.uid = :ds
      ),
      data_element_categories AS (
        SELECT de.de_id, ${c_id} as c_id
        FROM data_elements de
        JOIN categorycombos_categories cc_c ON de.categorycomboid = cc_c.categorycomboid
        JOIN category c ON cc_c.categoryid = c.categoryid
      )
      SELECT de_c.de_id, array_agg(de_c.c_id ORDER BY de_c.c_id)
      FROM data_element_categories de_c
      GROUP BY de_c.de_id
      """;
    Map<String, String> vars =
        Map.ofEntries(
            Map.entry("c_id", columnName("c", categories)),
            Map.entry("de_id", columnName("de", dataElements)));
    String sql = replace(sqlTemplate, vars);
    String[] de = dataElementIds.distinct().toArray(String[]::new);
    return listAsStringsMapOfList(
        sql, q -> q.setParameter("ds", dataSet.getValue()).setParameter("de", de));
  }

  @Override
  public Map<String, Map<Set<String>, String>> getDataElementCocIdMapping(
      @Nonnull UID dataSet,
      @Nonnull IdProperty categoryOptions,
      @Nonnull IdProperty dataElements,
      @Nonnull Stream<String> dataElementIds) {
    @Language("SQL")
    String sqlTemplate =
        """
        SELECT
          co_coc.de_ids,
          (
            -- 3. translate option ids (PK) to external ID property used
            SELECT array_agg(${co_id})
            FROM unnest(co_coc.categoryoptionids) AS options(categoryoptionid)
            INNER JOIN categoryoption co ON options.categoryoptionid = co.categoryoptionid
          ),
          coc.uid
        FROM (
        -- 2. expand effective CCs used to a mapping of options IDs as array for each COC
        -- the first column has all DEs (of the scope) that use the same CC
          SELECT
           array_agg(cc.de_id) AS de_ids,
           ARRAY(SELECT DISTINCT unnest(array_agg(coc_co.categoryoptionid))) AS categoryoptionids,
           coc_co.categoryoptioncomboid
          FROM (
            -- 1. expand the DE list (+DS) into effective CCs used
            SELECT
              ${de_id} AS de_id,
              coalesce(dse.categorycomboid, de.categorycomboid) AS categorycomboid
            FROM dataset ds
            JOIN datasetelement dse ON ds.datasetid = dse.datasetid
            JOIN dataelement de ON dse.dataelementid = de.dataelementid
            JOIN unnest(:de) AS dex(id) ON ${de_id} = dex.id
            WHERE ds.uid = :ds
          ) cc
          JOIN categorycombos_optioncombos cc_coc ON cc.categorycomboid = cc_coc.categorycomboid
          JOIN categoryoptioncombos_categoryoptions coc_co ON cc_coc.categoryoptioncomboid = coc_co.categoryoptioncomboid
          GROUP BY coc_co.categoryoptioncomboid, cc.categorycomboid
        ) co_coc
        JOIN categoryoptioncombo coc ON co_coc.categoryoptioncomboid = coc.categoryoptioncomboid""";
    Map<String, String> vars =
        Map.ofEntries(
            Map.entry("de_id", columnName("de", dataElements)),
            Map.entry("co_id", columnName("co", categoryOptions)));
    String sql = replace(sqlTemplate, vars);
    String[] de = dataElementIds.distinct().toArray(String[]::new);
    return listAsStringMapOfSetMap(
        sql, q -> q.setParameter("ds", dataSet.getValue()).setParameter("de", de));
  }

  @Override
  public Map<String, Map<Set<String>, String>> getCategoryComboAocIdMapping(
      @Nonnull Stream<String> categoryCombos) {
    String sql =
        """
        SELECT
          co_coc.cc_ids,
          (
            -- 3. translate option ids (PK) to external ID property used
            SELECT array_agg(co.uid)
            FROM unnest(co_coc.categoryoptionids) AS options(categoryoptionid)
            INNER JOIN categoryoption co ON options.categoryoptionid = co.categoryoptionid
          ),
          coc.uid
        FROM (
        -- 2. expand effective CCs used to a mapping of options IDs as array for each COC
        -- the first column has all DEs (of the scope) that use the same CC
          SELECT
           array_agg(cc.cc_id) AS cc_ids,
           ARRAY(SELECT DISTINCT unnest(array_agg(coc_co.categoryoptionid))) AS categoryoptionids,
           coc_co.categoryoptioncomboid
          FROM (
            -- 1. extract CC scope (to have same SQL shape as in COC query)
            SELECT
              cc_raw.uid AS cc_id,
              cc_raw.categorycomboid
            FROM categorycombo cc_raw
            WHERE cc_raw.uid IN (:cc)
          ) cc
          JOIN categorycombos_optioncombos cc_coc ON cc.categorycomboid = cc_coc.categorycomboid
          JOIN categoryoptioncombos_categoryoptions coc_co ON cc_coc.categoryoptioncomboid = coc_co.categoryoptioncomboid
          GROUP BY coc_co.categoryoptioncomboid, cc.categorycomboid
        ) co_coc
        JOIN categoryoptioncombo coc ON co_coc.categoryoptioncomboid = coc.categoryoptioncomboid""";
    String[] cc = categoryCombos.filter(Objects::nonNull).distinct().toArray(String[]::new);
    return listAsStringMapOfSetMap(sql, q -> q.setParameterList("cc", cc));
  }

  @Override
  public DataEntryValue getPartialDataValue(
      @Nonnull UID dataElement,
      @Nonnull UID orgUnit,
      @CheckForNull UID categoryOptionCombo,
      @CheckForNull UID attributeOptionCombo,
      @Nonnull String period) {
    String sql =
        """
      SELECT dv.value, dv.comment, dv.followup, dv.deleted
      FROM datavalue dv
      WHERE dv.dataelementid = (SELECT de.dataelementid FROM dataelement de WHERE de.uid = :de)
        AND dv.sourceid = (SELECT ou.organisationunitid FROM organisationunit ou WHERE ou.uid = :ou)
        AND dv.categoryoptioncomboid = (SELECT coc.categoryoptioncomboid FROM categoryoptioncombo coc WHERE coc.uid = :coc)
        AND dv.attributeoptioncomboid = (SELECT aoc.categoryoptioncomboid FROM categoryoptioncombo aoc WHERE aoc.uid = :aoc)
        AND dv.periodid = (SELECT pe.periodid FROM period pe WHERE pe.iso = :iso)
      """;
    if (categoryOptionCombo == null) categoryOptionCombo = getDefaultCategoryOptionComboUid();
    if (attributeOptionCombo == null) attributeOptionCombo = getDefaultCategoryOptionComboUid();
    List<Object[]> rows =
        createNativeRawQuery(sql)
            .setParameter("de", dataElement.getValue())
            .setParameter("ou", orgUnit.getValue())
            .setParameter("coc", categoryOptionCombo.getValue())
            .setParameter("aoc", attributeOptionCombo.getValue())
            .setParameter("iso", period)
            .list();
    if (rows.isEmpty()) return null; // does not exist
    Object[] row0 = rows.get(0);
    return new DataEntryValue(
        0,
        dataElement,
        orgUnit,
        categoryOptionCombo,
        attributeOptionCombo,
        period,
        (String) row0[0],
        (String) row0[1],
        (Boolean) row0[2],
        (Boolean) row0[3]);
  }

  @Override
  public List<String> getOrgUnitsNotInUserHierarchy(UID user, Stream<UID> orgUnits) {
    String sql =
        """
      WITH user_orgs AS (
          SELECT ou_inner.path
          FROM usermembership um
          JOIN userinfo u ON um.userinfoid = u.userinfoid
          JOIN organisationunit ou_inner ON um.organisationunitid = ou_inner.organisationunitid
          WHERE u.uid = :user
      )
      SELECT DISTINCT ou.uid
      FROM organisationunit ou
      JOIN unnest(:ou) AS oux(uid) ON ou.uid = oux.uid
      WHERE NOT EXISTS (
          SELECT 1
          FROM user_orgs
          WHERE ou.path = user_orgs.path  -- Exact match
             OR ou.path LIKE user_orgs.path || '/%'  -- Descendant match
      )""";
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("user", user.getValue()).setParameter("ou", ou));
  }

  @Override
  public List<String> getOrgUnitsNotInAocHierarchy(UID attrOptionCombo, Stream<UID> orgUnits) {
    // WITH part builds lists of paths for each CO connected to the AOC
    // the main SELECT then checks that any OU in parameter list
    // that does not have an exact or descendant match in each path list
    // is included in the result
    String sql =
        """
        WITH aoc_orgs AS (
          SELECT aoc_co.categoryoptionid, array_agg(DISTINCT ou.path) AS paths
          FROM categoryoptioncombo aoc
          JOIN categoryoptioncombos_categoryoptions aoc_co ON aoc.categoryoptioncomboid = aoc_co.categoryoptioncomboid
          JOIN categoryoption_organisationunits co_ou ON aoc_co.categoryoptionid = co_ou.categoryoptionid
          JOIN organisationunit ou ON co_ou.organisationunitid = ou.organisationunitid
          WHERE aoc.uid = :aoc
          GROUP BY aoc_co.categoryoptionid
        )
        SELECT DISTINCT ou.uid
        FROM organisationunit ou
        JOIN unnest(:ou) AS oux(uid) ON ou.uid = oux.uid
        WHERE EXISTS (
          SELECT 1
          FROM aoc_orgs
          WHERE NOT EXISTS (
            SELECT 1
            FROM unnest(aoc_orgs.paths) AS org_path(path)
            WHERE
              ou.path = org_path.path OR         -- Exact match
              ou.path LIKE org_path.path || '/%'  -- Descendant match
          )
        )""";
    String aoc = attrOptionCombo.getValue();
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("aoc", aoc).setParameter("ou", ou));
  }

  @Override
  public List<String> getOrgUnitsNotInDataSet(UID dataSet, Stream<UID> orgUnits) {
    String sql =
        """
        WITH ou_list(uid) AS ( SELECT DISTINCT UNNEST(:ou) AS uid )
        SELECT ou_list.uid
        FROM ou_list
        LEFT JOIN organisationunit ou ON ou_list.uid = ou.uid
        LEFT JOIN (
            SELECT s.sourceid
            FROM datasetsource s
            JOIN dataset ds ON s.datasetid = ds.datasetid AND ds.uid = :ds
        ) excluded ON ou.organisationunitid = excluded.sourceid
        WHERE excluded.sourceid IS NULL""";
    String ds = dataSet.getValue();
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("ou", ou).setParameter("ds", ds));
  }

  @Override
  public List<String> getCocNotInDataSet(UID dataSet, UID dataElement, Stream<UID> optionCombos) {
    String sql =
        """
      WITH coc_list(uid) AS ( SELECT DISTINCT UNNEST(:coc) AS uid ),
      dsde_coc AS (
          SELECT coc_cc.categoryoptioncomboid
          FROM categorycombos_optioncombos coc_cc
          WHERE coc_cc.categorycomboid = (
              SELECT COALESCE(dse.categorycomboid, de.categorycomboid)
              FROM datasetelement dse
              JOIN dataelement de ON de.dataelementid = dse.dataelementid
              JOIN dataset ds ON ds.datasetid = dse.datasetid
              WHERE ds.uid = :ds
                AND de.uid = :de
          )
      )
      SELECT coc_list.uid
      FROM coc_list
      LEFT JOIN categoryoptioncombo coc ON coc_list.uid = coc.uid
      LEFT JOIN dsde_coc excluded ON coc.categoryoptioncomboid = excluded.categoryoptioncomboid
      WHERE excluded.categoryoptioncomboid IS NULL""";
    String ds = dataSet.getValue();
    String de = dataElement.getValue();
    UID defaultCoc = getDefaultCategoryOptionComboUid();
    String[] coc =
        optionCombos
            .map(id -> id == null ? defaultCoc : id)
            .map(UID::getValue)
            .distinct()
            .toArray(String[]::new);
    return listAsStrings(
        sql, q -> q.setParameter("coc", coc).setParameter("ds", ds).setParameter("de", de));
  }

  @Override
  public List<String> getAocNotInDataSet(UID dataSet, Stream<UID> optionCombos) {
    String sql =
        """
        WITH aoc_list(uid) AS ( SELECT DISTINCT UNNEST(:aoc) AS uid ),
        ds_cc AS (
            SELECT ds.categorycomboid, cc.name AS cc_name
            FROM dataset ds
            JOIN categorycombo cc ON ds.categorycomboid = cc.categorycomboid
            WHERE ds.uid = :ds
        )
        SELECT aoc_list.uid
        FROM aoc_list
        LEFT JOIN categoryoptioncombo aoc ON aoc_list.uid = aoc.uid
        CROSS JOIN ds_cc
        LEFT JOIN categorycombos_optioncombos aoc_cc ON aoc.categoryoptioncomboid = aoc_cc.categoryoptioncomboid
            AND ds_cc.categorycomboid = aoc_cc.categorycomboid
        WHERE (
            -- Include UIDs that don't exist in categoryoptioncombo table
            aoc.uid IS NULL
            OR
            -- Include UIDs that fail the existing conditions
            (
                -- When CC is 'default', AOC must also be 'default'
                (ds_cc.cc_name = 'default' AND NOT aoc.name = 'default')
                OR
                -- For all other cases, an AOC not linked to DS is an issue
                aoc_cc.categoryoptioncomboid IS NULL
            )
        )""";
    String ds = dataSet.getValue();
    UID defaultAoc = getDefaultCategoryOptionComboUid();
    String[] aoc =
        optionCombos
            .map(id -> id == null ? defaultAoc : id)
            .map(UID::getValue)
            .distinct()
            .toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("aoc", aoc).setParameter("ds", ds));
  }

  @Override
  public List<String> getAocWithOrgUnitHierarchy(Stream<UID> attrOptionCombos) {
    String sql =
        """
      SELECT DISTINCT aoc.uid
      FROM categoryoptioncombo aoc
      JOIN categoryoptioncombos_categoryoptions aoc_co ON aoc.categoryoptioncomboid = aoc_co.categoryoptioncomboid
      JOIN categoryoption_organisationunits co_ou ON aoc_co.categoryoptionid = co_ou.categoryoptionid
      WHERE aoc.uid IN (:aoc)""";
    // filter nulls to ignore "default" AOC assuming they never have a hierarchy limitation
    String[] aoc =
        attrOptionCombos
            .filter(Objects::nonNull)
            .map(UID::getValue)
            .distinct()
            .toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameterList("aoc", aoc));
  }

  @Override
  public List<String> getDataSets(Stream<UID> dataElements) {
    String sql =
        """
      SELECT DISTINCT ds.uid
      FROM dataelement de
      JOIN datasetelement de_ds ON de.dataelementid = de_ds.dataelementid
      JOIN dataset ds ON de_ds.datasetid = ds.datasetid
      WHERE de.uid IN (:de)""";
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameterList("de", de));
  }

  @Override
  public Map<String, Set<String>> getDataSetsByDataElement(Stream<UID> dataElements) {
    String sql =
        """
        SELECT
            de.uid,
            coalesce(
                array_agg(ds.uid ORDER BY ds.created DESC) FILTER (WHERE ds.uid IS NOT NULL),
                '{}'
            ) AS dataset_uids
        FROM dataelement de
        LEFT JOIN datasetelement de_ds ON de.dataelementid = de_ds.dataelementid
        LEFT JOIN dataset ds ON de_ds.datasetid = ds.datasetid
        WHERE de.uid IN (:de)
        GROUP BY de.uid""";
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStringsMapOfSet(sql, q -> q.setParameterList("de", de));
  }

  @Override
  public List<String> getDataElementsNotInDataSet(UID dataSet, Stream<UID> dataElements) {
    String sql =
        """
      WITH de_list(uid) AS ( SELECT DISTINCT UNNEST(:de) AS uid )
      SELECT de_list.uid
      FROM de_list
      LEFT JOIN dataelement de ON de_list.uid = de.uid
      LEFT JOIN (
          SELECT de_ds.dataelementid
          FROM datasetelement de_ds
          JOIN dataset ds ON de_ds.datasetid = ds.datasetid AND ds.uid = :ds
      ) excluded ON de.dataelementid = excluded.dataelementid
      WHERE excluded.dataelementid IS NULL""";
    String ds = dataSet.getValue();
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("ds", ds).setParameter("de", de));
  }

  @Override
  public Map<String, Set<String>> getOptionsByDataElements(Stream<UID> dataElements) {
    String sql =
        """
      SELECT de.uid, array_agg(ov.code)
      FROM dataelement de
      JOIN optionvalue ov ON de.optionsetid = ov.optionsetid
      WHERE de.uid IN (:de)
      GROUP BY de.uid""";
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStringsMapOfSet(sql, q -> q.setParameterList("de", de));
  }

  @Override
  public Map<String, Set<String>> getCommentOptionsByDataElements(Stream<UID> dataElements) {
    String sql =
        """
      SELECT de.uid, array_agg(ov.code)
      FROM dataelement de
      JOIN optionvalue ov ON de.commentoptionsetid = ov.optionsetid
      WHERE de.uid IN (:de)
      GROUP BY de.uid""";
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStringsMapOfSet(sql, q -> q.setParameterList("de", de));
  }

  @Override
  public Map<String, ValueType> getValueTypeByDataElements(Stream<UID> dataElements) {
    return getUidToAnyMap(
        new DbName("dataelement"),
        new DbName("valuetype"),
        dataElements,
        str -> ValueType.valueOf(str.toString()));
  }

  @Override
  public boolean getDataSetCanDataWrite(UID dataSet) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return true;
    String accessSql = generateSQlQueryForSharingCheck("ds.sharing", user, LIKE_WRITE_DATA);
    @Language("SQL")
    String sql =
        """
      SELECT ds.uid
      FROM dataset ds
      WHERE ds.uid = :ds AND NOT (%s);
      """;
    String ds = dataSet.getValue();
    return listAsStrings(sql.formatted(accessSql), q -> q.setParameter("ds", ds)).isEmpty();
  }

  @Override
  public boolean getDataSetCommentAllowsEmptyValue(UID dataSet) {
    String sql = "SELECT ds.novaluerequirescomment FROM dataset ds WHERE ds.uid = :ds";
    Object res =
        getSession()
            .createNativeQuery(sql)
            .setParameter("ds", dataSet.getValue())
            .getSingleResult();
    return res instanceof Boolean b && b;
  }

  @Override
  public int getDataSetExpiryDays(UID dataSet) {
    String sql = "SELECT ds.expirydays FROM dataset ds WHERE ds.uid = :ds";
    Object res =
        getSession()
            .createNativeQuery(sql)
            .setParameter("ds", dataSet.getValue())
            .getSingleResult();
    return res instanceof Number n ? n.intValue() : 0;
  }

  @Override
  public int getDataSetOpenPeriodsOffset(UID dataSet) {
    String sql = "SELECT ds.openfutureperiods FROM dataset ds WHERE ds.uid = :ds";
    Object res =
        getSession()
            .createNativeQuery(sql)
            .setParameter("ds", dataSet.getValue())
            .getSingleResult();
    return res instanceof Number n ? n.intValue() : 0;
  }

  @Override
  public int getDataSetOpenPeriodsAfterCoEndDate(UID dataSet) {
    String sql = "SELECT ds.openperiodsaftercoenddate FROM dataset ds WHERE ds.uid = :ds";
    Object res =
        getSession()
            .createNativeQuery(sql)
            .setParameter("ds", dataSet.getValue())
            .getSingleResult();
    return res instanceof Number n ? n.intValue() : 0;
  }

  @Override
  public List<String> getCategoryOptionsCanNotDataRead(Stream<UID> optionCombos) {
    return getCategoryOptionsCanNotDataAccess(optionCombos, LIKE_READ_DATA);
  }

  @Override
  public List<String> getCategoryOptionsCanNotDataWrite(Stream<UID> optionCombos) {
    return getCategoryOptionsCanNotDataAccess(optionCombos, LIKE_WRITE_DATA);
  }

  @Nonnull
  private List<String> getCategoryOptionsCanNotDataAccess(
      Stream<UID> optionCombos, String accessPattern) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return List.of();
    String accessSql = generateSQlQueryForSharingCheck("co.sharing", user, accessPattern);
    @Language("SQL")
    String sql =
        """
      SELECT co.uid
      FROM categoryoptioncombo coc
      JOIN categoryoptioncombos_categoryoptions aoc_co ON coc.categoryoptioncomboid = aoc_co.categoryoptioncomboid
      JOIN categoryoption co ON aoc_co.categoryoptionid = co.categoryoptionid
      WHERE coc.uid IN (:coc)
      AND NOT (%s);
      """;
    // ignore nulls (default COC) assuming that it does not have special user restrictions
    String[] coc =
        optionCombos.filter(Objects::nonNull).map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql.formatted(accessSql), q -> q.setParameterList("coc", coc));
  }

  @Override
  public int deleteByKeys(List<DataEntryKey> keys) {
    // ATM it does not seem worth it to make a dedicated implementation
    // instead we do...
    return upsertValues(keys.stream().map(DataEntryKey::toDeletedValue).toList());
  }

  @Override
  @UsageTestOnly
  public int upsertValuesForJdbcTest(List<DataEntryValue> values) {
    if (values == null || values.isEmpty()) return 0;

    List<DataEntryRow> internalValues = upsertValuesResolveIds(values);
    if (internalValues.isEmpty()) return 0;

    @Language("sql")
    String sql =
        """
        INSERT INTO datavalue
      (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid, value, comment, followup, deleted, storedby, lastupdated, created)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid)
      DO UPDATE SET
        value = EXCLUDED.value,
        comment = EXCLUDED.comment,
        followup = EXCLUDED.followup,
        deleted = EXCLUDED.deleted,
        storedby = EXCLUDED.storedby,
        lastupdated = EXCLUDED.lastupdated,
        created = EXCLUDED.created
        """;
    int imported = 0;
    for (DataEntryRow row : internalValues) {
      Date now = new Date();
      imported +=
          jdbcTemplate.update(
              sql,
              row.de(),
              row.pe(),
              row.ou(),
              row.coc(),
              row.aoc(),
              row.value(),
              row.comment(),
              row.followup(),
              row.deleted(),
              "test",
              now,
              now);
    }
    return imported;
  }

  @Override
  public int upsertValues(List<DataEntryValue> values) {
    if (values == null || values.isEmpty()) return 0;
    List<DataEntryRow> internalValues = upsertValuesResolveIds(values);
    if (internalValues.isEmpty()) return 0;

    // (A) Deterministic order by conflict key
    internalValues.sort(
        Comparator.comparingLong(DataEntryRow::de)
            .thenComparingLong(DataEntryRow::pe)
            .thenComparingLong(DataEntryRow::ou)
            .thenComparingLong(DataEntryRow::coc)
            .thenComparingLong(DataEntryRow::aoc));

    try {
      return withTxnRetries(
          () -> {
            final AtomicInteger imported = new AtomicInteger();
            final int size = internalValues.size();
            final Session session = entityManager.unwrap(Session.class);

            @Language("sql")
            String sql1 =
                """
          INSERT INTO datavalue
          (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid, value, comment, followup, deleted)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid)
          DO UPDATE SET
            value = EXCLUDED.value,
            comment = CASE
              WHEN datavalue.deleted = false AND EXCLUDED.deleted = true THEN datavalue.comment
              ELSE EXCLUDED.comment
            END,
            deleted = EXCLUDED.deleted,
            followup = EXCLUDED.followup,
            lastupdated = now(),
            storedby = current_setting('dhis2.user')
            """;

            session.doWork(
                conn -> {
                  try (PreparedStatement stmt =
                      conn.prepareStatement("SELECT set_config('dhis2.user', ?, true)")) {
                    stmt.setString(1, getCurrentUsername());
                    stmt.execute();
                  }
                  int from = 0;
                  while (from < size) {
                    int n = Math.min(MAX_ROWS_PER_INSERT, size - from);
                    int to = from + n;
                    try (PreparedStatement ps = conn.prepareStatement(upsertNValuesSql(sql1, n))) {
                      int p = 0;
                      for (var v : internalValues.subList(from, to)) {
                        ps.setLong(++p, v.de());
                        ps.setLong(++p, v.pe());
                        ps.setLong(++p, v.ou());
                        ps.setLong(++p, v.coc());
                        ps.setLong(++p, v.aoc());
                        ps.setString(++p, v.value());
                        ps.setString(++p, v.comment());
                        ps.setObject(++p, v.followup());
                        ps.setBoolean(++p, v.deleted());
                      }
                      imported.addAndGet(ps.executeUpdate());
                    }
                    from = to;
                  }
                });
            session.clear();
            return imported.get();
          });
    } catch (SQLTransactionRollbackException e) {
      throw new RuntimeException("Transaction retry limit exceeded during data value upsert", e);
    }
  }

  @Nonnull
  private List<DataEntryRow> upsertValuesResolveIds(List<DataEntryValue> values) {
    Map<String, Long> des = getDataElementIdMap(values.stream().map(DataEntryValue::dataElement));
    Map<String, Long> ous = getOrgUnitIdMap(values.stream().map(DataEntryValue::orgUnit));
    Map<String, Long> cocs = getOptionComboIdMap(values);
    Map<String, Long> pes = getPeriodsIdMap(values);
    long defaultCoc = getDefaultCategoryOptionComboId();
    Function<UID, Long> cocOf = uid -> uid == null ? defaultCoc : cocs.get(uid.getValue());

    List<DataEntryRow> internalValues = new ArrayList<>(values.size());

    for (DataEntryValue value : values) {
      Long de = des.get(value.dataElement().getValue());
      Long pe = pes.get(value.period());
      Long ou = ous.get(value.orgUnit().getValue());
      Long coc = cocOf.apply(value.categoryOptionCombo());
      Long aoc = cocOf.apply(value.attributeOptionCombo());
      if (de != null && pe != null && ou != null && coc != null && aoc != null) {
        Boolean deleted = value.deleted();
        if (deleted == null) deleted = false;
        internalValues.add(
            new DataEntryRow(
                de, pe, ou, coc, aoc, value.value(), value.comment(), value.followUp(), deleted));
      }
    }
    return internalValues;
  }

  @Nonnull
  private static String upsertNValuesSql(String sql1, int n) {
    if (n == 1) return sql1;
    return sql1.replace(
        "(?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "(?, ?, ?, ?, ?, ?, ?, ?, ?)" + ", (?, ?, ?, ?, ?, ?, ?, ?, ?)".repeat(n - 1));
  }

  private Map<String, Long> getDataElementIdMap(Stream<UID> ids) {
    return getIdMap("dataelement", ids);
  }

  private Map<String, Long> getOrgUnitIdMap(Stream<UID> ids) {
    return getIdMap("organisationunit", ids);
  }

  private Map<String, Long> getOptionComboIdMap(Stream<UID> ids) {
    return getIdMap("categoryoptioncombo", ids);
  }

  private long getDefaultCategoryOptionComboId() {
    String sql = "select categoryoptioncomboid from categoryoptioncombo where name = 'default'";
    return ((Number) getSession().createNativeQuery(sql).getSingleResult()).longValue();
  }

  private UID getDefaultCategoryOptionComboUid() {
    String sql = "select uid from categoryoptioncombo where name = 'default'";
    return UID.of((String) getSession().createNativeQuery(sql).getSingleResult());
  }

  private Map<String, Long> getOptionComboIdMap(List<DataEntryValue> values) {
    return getOptionComboIdMap(
        Stream.concat(
            values.stream().map(DataEntryValue::categoryOptionCombo),
            values.stream().map(DataEntryValue::attributeOptionCombo).filter(Objects::nonNull)));
  }

  @Nonnull
  private Map<String, Long> getPeriodsIdMap(List<DataEntryValue> values) {
    List<String> isoPeriods = values.stream().map(DataEntryValue::period).distinct().toList();
    Map<String, Long> res = new HashMap<>(isoPeriods.size());
    String sql = "SELECT iso, periodid FROM period where iso IN (:iso)";
    @SuppressWarnings("unchecked")
    Stream<Object[]> rows =
        getSession().createNativeQuery(sql).setParameterList("iso", isoPeriods).stream();
    rows.forEach(row -> res.put((String) row[0], ((Number) row[1]).longValue()));
    if (res.size() < isoPeriods.size()) {
      // create and add the periods that do not yet exist...
      isoPeriods.stream()
          .filter(not(res::containsKey))
          .forEach(
              iso -> {
                Period p = PeriodType.getPeriodFromIsoString(iso);
                periodStore.reloadForceAddPeriod(p);
                res.put(iso, p.getId());
              });
    }
    return res;
  }

  @Override
  public String getDataSetPeriodType(UID dataSet) {
    String sql =
        """
      SELECT pt.name FROM dataset ds
      JOIN periodtype pt ON ds.periodtypeid = pt.periodtypeid
      WHERE ds.uid = :ds
      """;
    return (String)
        getSession()
            .createNativeQuery(sql)
            .setParameter("ds", dataSet.getValue())
            .getSingleResult();
  }

  @Override
  public List<String> getIsoPeriodsNotUsableInDataSet(UID dataSet, Stream<String> isoPeriods) {
    String expected = getDataSetPeriodType(dataSet);
    return isoPeriods
        .distinct()
        .filter(
            iso -> {
              PeriodType actual = PeriodType.getPeriodTypeFromIsoString(iso);
              return actual == null || !expected.equals(actual.getName());
            })
        .toList();
  }

  @Override
  public List<String> getDataSetAocInApproval(UID dataSet) {
    String sql =
        """
      SELECT DISTINCT aoc.uid
      FROM dataset ds
      JOIN dataapproval da ON ds.workflowid = da.workflowid
      JOIN categoryoptioncombo aoc ON da.attributeoptioncomboid = aoc.categoryoptioncomboid
      WHERE ds.uid = :ds
        AND ds.workflowid IS NOT NULL""";
    return listAsStrings(sql, q -> q.setParameter("ds", dataSet.getValue()));
  }

  @Override
  public Map<String, Set<String>> getApprovedIsoPeriodsByOrgUnit(
      UID dataSet, UID attrOptionCombo, Stream<UID> orgUnits) {
    String sql =
        """
      SELECT ou.uid, array_agg(pe.iso)
      FROM dataset ds
      JOIN dataapproval da ON ds.workflowid = da.workflowid
      JOIN categoryoptioncombo aoc ON da.attributeoptioncomboid = aoc.categoryoptioncomboid
      JOIN organisationunit ou ON da.organisationunitid = ou.organisationunitid
      JOIN period pe ON da.periodid = pe.periodid
      WHERE ds.uid = :ds
        AND aoc.uid = :aoc
        AND ou.uid IN (:ou)
      GROUP BY ou.uid""";
    String ds = dataSet.getValue();
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStringsMapOfSet(
        sql,
        q ->
            q.setParameter("ds", ds)
                .setParameterList("ou", ou)
                .setParameter("aoc", attrOptionCombo.getValue()));
  }

  @Override
  public Map<String, Set<String>> getExpiryDaysExemptedIsoPeriodsByOrgUnit(UID dataSet) {
    String sql =
        """
      SELECT ou.uid, array_agg(p.iso)
      FROM lockexception ex
      JOIN organisationunit ou ON ex.organisationunitid = ou.organisationunitid
      JOIN period p ON ex.periodid = p.periodid
      WHERE ex.datasetid = (SELECT ds.datasetid FROM dataset ds WHERE ds.uid = :ds)
      GROUP BY ou.uid""";
    String ds = dataSet.getValue();
    return listAsStringsMapOfSet(sql, q -> q.setParameter("ds", ds));
  }

  @Override
  public Map<String, List<DateRange>> getEntrySpansByIsoPeriod(UID dataSet) {
    String sql =
        """
      SELECT p.iso, ip.openingdate, ip.closingdate
      FROM datainputperiod ip
      JOIN period p ON ip.periodid = p.periodid
      WHERE ip.datasetid = (SELECT ds.datasetid FROM dataset ds WHERE ds.uid = :ds)""";
    String ds = dataSet.getValue();
    @SuppressWarnings("unchecked")
    Stream<Object[]> rows = getSession().createNativeQuery(sql).setParameter("ds", ds).stream();
    Map<String, List<DateRange>> res = new HashMap<>();
    rows.forEach(
        row ->
            res.computeIfAbsent((String) row[0], key -> new ArrayList<>())
                .add(new DateRange((Date) row[1], (Date) row[2])));
    return res;
  }

  @Override
  public Map<String, DateRange> getEntrySpanByAoc(Stream<UID> attributeOptionCombos) {
    // ignoring nulls (default AOC) assuming it does not have a limited entry span
    String[] aoc =
        attributeOptionCombos
            .filter(Objects::nonNull)
            .map(UID::getValue)
            .distinct()
            .toArray(String[]::new);
    if (aoc.length == 0) return Map.of();
    String sql =
        """
      SELECT
        aoc.uid,
        MAX(co.startdate) AS startdate,  -- Latest start (ignores NULLs)
        MIN(co.enddate) AS enddate       -- Earliest end (ignores NULLs)
      FROM categoryoptioncombo aoc
      JOIN categoryoptioncombos_categoryoptions aoc_co ON aoc.categoryoptioncomboid = aoc_co.categoryoptioncomboid
      JOIN categoryoption co ON aoc_co.categoryoptionid = co.categoryoptionid
      WHERE aoc.uid IN (:aoc)
      GROUP BY aoc.uid""";
    return listAsMapOfDateRange(sql, q -> q.setParameterList("aoc", aoc));
  }

  @Override
  public Map<String, DateRange> getEntrySpanByOrgUnit(Stream<UID> orgUnits, DateRange timeframe) {
    String sql =
        """
      SELECT ou.uid, ou.openingdate, ou.closeddate
      FROM organisationunit ou
      WHERE (ou.openingdate > :start OR (ou.closeddate is not null and ou.closeddate < :end))
        AND ou.uid IN (:ou)""";
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsMapOfDateRange(
        sql,
        q ->
            q.setParameter("start", timeframe.getStartDate())
                .setParameter("end", timeframe.getEndDate())
                .setParameterList("ou", ou));
  }

  @SuppressWarnings("unchecked")
  private NativeQuery<Object[]> createNativeRawQuery(@Language("SQL") String sql) {
    return getSession().createNativeQuery(sql);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private List<String> listAsStrings(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<?>> setParameters) {
    return (List<String>) setParameters.apply(createNativeRawQuery(sql)).list();
  }

  private Map<String, DateRange> listAsMapOfDateRange(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<Object[]>> setParameters) {
    NativeQuery<Object[]> query = setParameters.apply(createNativeRawQuery(sql));
    Stream<Object[]> rows = query.stream();
    return rows.collect(
        toMap(row -> (String) row[0], row -> new DateRange((Date) row[1], (Date) row[2])));
  }

  @Nonnull
  private Map<String, Set<String>> listAsStringsMapOfSet(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<Object[]>> setParameters) {
    return listAsStringsMapOfArray(sql, setParameters, Set::of);
  }

  @Nonnull
  private Map<String, List<String>> listAsStringsMapOfList(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<Object[]>> setParameters) {
    return listAsStringsMapOfArray(sql, setParameters, List::of);
  }

  @Nonnull
  private <C> Map<String, C> listAsStringsMapOfArray(
      @Language("SQL") String sql,
      UnaryOperator<NativeQuery<Object[]>> setParameters,
      Function<String[], C> f) {
    NativeQuery<Object[]> query = setParameters.apply(createNativeRawQuery(sql));
    Stream<Object[]> rows = query.stream();
    return rows.collect(toMap(row -> (String) row[0], row -> f.apply((String[]) row[1])));
  }

  @Nonnull
  private Map<String, String> listAsStringsMap(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<Object[]>> setParameters) {
    NativeQuery<Object[]> query = setParameters.apply(createNativeRawQuery(sql));
    Stream<Object[]> rows = query.stream();
    return rows.collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  @Nonnull
  private Map<String, Map<Set<String>, String>> listAsStringMapOfSetMap(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<Object[]>> setParameters) {
    NativeQuery<Object[]> query = setParameters.apply(createNativeRawQuery(sql));
    Stream<Object[]> rows = query.stream();
    Map<String, Map<Set<String>, String>> res = new HashMap<>();
    rows.forEach(
        row -> {
          Set<String> ccOptions = Set.of((String[]) row[1]);
          String coc = (String) row[2];
          String[] ccDataElements = (String[]) row[0];
          for (String ccDe : ccDataElements) {
            Map<Set<String>, String> mapping = res.computeIfAbsent(ccDe, key -> new HashMap<>());
            mapping.put(ccOptions, coc);
          }
        });
    return res;
  }

  private static boolean isRetryableSqlState(String state) {
    return "40P01".equals(state) || "40001".equals(state); // deadlock / serialization
  }

  /**
   * Executes a given task with retry logic in case of specific SQL exceptions. This method retries
   * the task up to 3 times if a deadlock or serialization failure occurs. It uses an exponential
   * backoff strategy with a random jitter to reduce contention.
   *
   * @param <T> The type of the result returned by the task.
   * @param work A `Callable` representing the task to be executed.
   * @return The result of the task if it completes successfully.
   * @throws RuntimeException If the task fails after 3 retries or encounters an unexpected
   *     exception.
   */
  private <T> T withTxnRetries(Callable<T> work) throws SQLTransactionRollbackException {
    int tries = 0;
    long backoffMs = 50;

    while (true) {
      try {
        return work.call();
      } catch (Exception ex) {
        Throwable cause =
            (ex instanceof java.util.concurrent.ExecutionException) ? ex.getCause() : ex;

        if (cause instanceof SQLException sqlEx) {
          String state = sqlEx.getSQLState();

          if (isRetryableSqlState(state)) {
            if (++tries <= 3) {
              try {
                Thread.sleep(backoffMs + ThreadLocalRandom.current().nextInt(40));
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
              }
              backoffMs *= 2;
              continue;
            }
            // Retries exhausted
            throw new SQLTransactionRollbackException(
                ("40P01".equals(state) ? "Deadlock" : "Serialization failure")
                    + " after "
                    + tries
                    + " attempts",
                sqlEx);
          }
          // Non-retryable -> rethrow
          if (cause instanceof RuntimeException re) throw re;
          throw new RuntimeException(sqlEx);
        }
        // Not an SQLException
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
      }
    }
  }
}
