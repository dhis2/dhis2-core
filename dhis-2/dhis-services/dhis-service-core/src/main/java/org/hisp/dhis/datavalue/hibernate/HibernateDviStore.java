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

import static java.lang.Math.min;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.query.JpaQueryUtils.generateSQlQueryForSharingCheck;
import static org.hisp.dhis.security.acl.AclService.LIKE_WRITE_DATA;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DbName;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DviKey;
import org.hisp.dhis.datavalue.DviRow;
import org.hisp.dhis.datavalue.DviStore;
import org.hisp.dhis.datavalue.DviValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
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
public class HibernateDviStore extends HibernateGenericStore<DataValue> implements DviStore {

  private final PeriodStore periodStore;

  /**
   * Maximum number of {@code VALUES} pairs that get added to a single {@code INSERT} SQL statement.
   */
  private static final int MAX_ROWS_PER_INSERT = 500;

  public HibernateDviStore(
      EntityManager entityManager,
      PeriodStore periodStore,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValue.class, false);
    this.periodStore = periodStore;
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
     SELECT DISTINCT ou.uid
     FROM organisationunit ou
     LEFT JOIN (
       SELECT s.sourceid
       FROM datasetsource s
       JOIN dataset ds ON s.datasetid = ds.datasetid AND ds.uid = :ds
     ) excluded ON ou.organisationunitid = excluded.sourceid
     WHERE ou.uid IN (:ou)
       AND excluded.sourceid IS NULL""";
    String ds = dataSet.getValue();
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameterList("ou", ou).setParameter("ds", ds));
  }

  @Override
  public List<String> getCocNotInDataSet(UID dataSet, UID dataElement, Stream<UID> optionCombos) {
    String sql =
        """
      SELECT DISTINCT coc.uid
      FROM categoryoptioncombo coc
      LEFT JOIN (
          SELECT m.categoryoptioncomboid
          FROM categorycombos_optioncombos m
          WHERE m.categorycomboid = (
              SELECT COALESCE(dse.categorycomboid, de.categorycomboid)
              FROM datasetelement dse
              JOIN dataelement de ON de.dataelementid = dse.dataelementid
              JOIN dataset ds ON ds.datasetid = dse.datasetid
              WHERE ds.uid = :ds
                AND de.uid = :de
          )
      ) excluded ON coc.categoryoptioncomboid = excluded.categoryoptioncomboid
      WHERE coc.uid IN (:coc)
        AND excluded.categoryoptioncomboid IS NULL""";
    String ds = dataSet.getValue();
    String de = dataElement.getValue();
    String[] coc = optionCombos.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(
        sql, q -> q.setParameterList("coc", coc).setParameter("ds", ds).setParameter("de", de));
  }

  @Override
  public List<String> getAocNotInDataSet(UID dataSet, Stream<UID> optionCombos) {
    String sql =
        """
        SELECT DISTINCT aoc.uid
        FROM categoryoptioncombo aoc
        CROSS JOIN (
            SELECT ds.categorycomboid, cc.name AS cc_name
            FROM dataset ds
            JOIN categorycombo cc ON ds.categorycomboid = cc.categorycomboid
            WHERE ds.uid = :ds
        ) cc_info
        LEFT JOIN categorycombos_optioncombos m ON aoc.categoryoptioncomboid = m.categoryoptioncomboid
            AND cc_info.categorycomboid = m.categorycomboid
        WHERE aoc.uid IN (:aoc)
          AND (
              -- when CC is 'default' AOC must also be 'default'
              (cc_info.cc_name = 'default' AND NOT aoc.name = 'default')
              OR
              -- For all other cases, a AOC not linked to DS is an issue
              m.categoryoptioncomboid IS NULL
          )""";
    String ds = dataSet.getValue();
    String[] aoc = optionCombos.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameterList("aoc", aoc).setParameter("ds", ds));
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
    String[] aoc = attrOptionCombos.map(UID::getValue).distinct().toArray(String[]::new);
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
  public List<String> getDataElementsNotInDataSet(UID dataSet, Stream<UID> dataElements) {
    String sql =
        """
      SELECT DISTINCT de.uid
      FROM dataelement de
      LEFT JOIN (
          SELECT de_ds.dataelementid
          FROM datasetelement de_ds
          JOIN dataset ds ON de_ds.datasetid = ds.datasetid AND ds.uid = :ds
      ) excluded ON de.dataelementid = excluded.dataelementid
      WHERE de.uid IN (:de)
        AND excluded.dataelementid IS NULL""";
    String ds = dataSet.getValue();
    String[] de = dataElements.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("ds", ds).setParameterList("de", de));
  }

  @Override
  public Map<String, Set<String>> getOptionsByDataElements(Stream<UID> dataElements) {
    String sql =
        """
      SELECT de.uid, ARRAY_AGG(ov.code)
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
      SELECT de.uid, ARRAY_AGG(ov.code)
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
  public List<String> getCategoryOptionsCanNotDataWrite(Stream<UID> optionCombos) {
    UserDetails user = getCurrentUserDetails();
    if (user.isSuper()) return List.of();
    String accessSql = generateSQlQueryForSharingCheck("co.sharing", user, LIKE_WRITE_DATA);
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
    String[] coc = optionCombos.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql.formatted(accessSql), q -> q.setParameterList("coc", coc));
  }

  @Override
  public int deleteByKeys(List<DviKey> keys) {
    // TODO
    return 0;
  }

  @Override
  public int upsertValues(List<DviValue> values) {
    if (values == null || values.isEmpty()) return 0;

    List<DviRow> internalValues = upsertValuesResolveIds(values);
    if (internalValues.isEmpty()) return 0;

    int size = internalValues.size();
    Session session = entityManager.unwrap(Session.class);

    @Language("sql")
    String sql1 =
        """
      INSERT INTO datavalue
      (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid, value, comment, followup, deleted)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid)
      DO UPDATE SET
        value = EXCLUDED.value,
        comment = EXCLUDED.comment,
        deleted = EXCLUDED.deleted,
        followup = EXCLUDED.followup,
        lastupdated = now(),
        storedby = current_setting('dhis2.user')
        """;

    String user = getCurrentUsername();
    AtomicInteger imported = new AtomicInteger();
    session.doWork(
        conn -> {
          try (PreparedStatement stmt =
              conn.prepareStatement("SELECT set_config('dhis2.user', ?, true)")) {
            stmt.setString(1, user);
            stmt.execute();
          }
          int from = 0;
          while (from < size) {
            int n = min(MAX_ROWS_PER_INSERT, size - from);
            int to = from + n;
            try (PreparedStatement stmt = conn.prepareStatement(upsertNValuesSql(sql1, n))) {
              int p = 0;
              for (DviRow value : internalValues.subList(from, to)) {
                stmt.setLong(p + 1, value.de());
                stmt.setLong(p + 2, value.pe());
                stmt.setLong(p + 3, value.ou());
                stmt.setLong(p + 4, value.coc());
                stmt.setLong(p + 5, value.aoc());
                stmt.setString(p + 6, value.value());
                stmt.setString(p + 7, value.comment());
                stmt.setObject(p + 8, value.followup());
                stmt.setBoolean(p + 9, value.deleted());
                p += 9;
              }
              imported.addAndGet(stmt.executeUpdate());
            }
            from += n;
          }
        });

    session.clear();

    return imported.get();
  }

  @Nonnull
  private List<DviRow> upsertValuesResolveIds(List<DviValue> values) {
    Map<String, Long> des = getDataElementIdMap(values.stream().map(DviValue::dataElement));
    Map<String, Long> ous = getOrgUnitIdMap(values.stream().map(DviValue::orgUnit));
    Map<String, Long> cocs = getOptionComboIdMap(values);
    Map<String, Long> pes = getPeriodsIdMap(values);
    Function<UID, Long> cocOf = uid -> cocs.get(uid == null ? "default" : uid.getValue());

    List<DviRow> internalValues = new ArrayList<>(values.size());

    for (DviValue value : values) {
      Long de = des.get(value.dataElement().getValue());
      Long pe = pes.get(value.period());
      Long ou = ous.get(value.orgUnit().getValue());
      Long coc = cocOf.apply(value.categoryOptionCombo());
      Long aoc = cocOf.apply(value.attributeOptionCombo());
      if (de != null && pe != null && ou != null && coc != null && aoc != null) {
        Boolean deleted = value.deleted();
        if (deleted == null) deleted = false;
        internalValues.add(
            new DviRow(
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

  private Map<String, Long> getOptionComboIdMap(List<DviValue> values) {
    long defaultCoc = getDefaultCategoryOptionComboId();
    Map<String, Long> res =
        getOptionComboIdMap(
            Stream.concat(
                values.stream().map(DviValue::categoryOptionCombo),
                values.stream().map(DviValue::attributeOptionCombo).filter(Objects::nonNull)));
    res.put("", defaultCoc);
    return res;
  }

  @Nonnull
  private Map<String, Long> getPeriodsIdMap(List<DviValue> values) {
    List<String> isoPeriods = values.stream().map(DviValue::period).distinct().toList();
    Map<String, Long> res = new HashMap<>(isoPeriods.size());
    String sql =
        """
      SELECT iso, periodid FROM period where iso IN (:iso)""";
    @SuppressWarnings("unchecked")
    Stream<Object[]> rows =
        getSession().createNativeQuery(sql).setParameterList("iso", isoPeriods).stream();
    rows.forEach(row -> res.put((String) row[0], ((Number) row[1]).longValue()));
    if (res.size() < isoPeriods.size()) {
      // create and add the periods that do not yet exist...
      isoPeriods.stream()
          .filter(not(res::containsKey))
          .forEach(
              iso -> res.put(iso, periodStore.getPeriodId(PeriodType.getPeriodFromIsoString(iso))));
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
      JOIN organisationunit ou ON da.organisationunitid = ou.organisationunitid
      JOIN period pe ON da.periodid = pe.periodid
      WHERE ds.uid = :ds
        AND ou.uid IN (:ou)
      GROUP BY ou.uid""";
    String ds = dataSet.getValue();
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStringsMapOfSet(sql, q -> q.setParameter("ds", ds).setParameterList("ou", ou));
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
  public Map<String, List<DateRange>> getEntryPeriodsByIsoPeriod(UID dataSet) {
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

  @Nonnull
  private Map<String, Set<String>> listAsStringsMapOfSet(
      @Language("sql") String sql, UnaryOperator<NativeQuery<?>> setParameters) {
    NativeQuery<?> query = setParameters.apply(getSession().createNativeQuery(sql));
    @SuppressWarnings("unchecked")
    Stream<Object[]> results = (Stream<Object[]>) query.stream();
    return results.collect(toMap(row -> (String) row[0], row -> Set.of((String[]) row[1])));
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private List<String> listAsStrings(
      @Language("SQL") String sql, UnaryOperator<NativeQuery<?>> setParameters) {
    return (List<String>) setParameters.apply(getSession().createNativeQuery(sql)).list();
  }
}
