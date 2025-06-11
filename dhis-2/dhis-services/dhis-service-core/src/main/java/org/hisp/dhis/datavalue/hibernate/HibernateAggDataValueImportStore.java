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
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
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
import org.hisp.dhis.common.DbName;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.AggDataValue;
import org.hisp.dhis.datavalue.AggDataValueImportStore;
import org.hisp.dhis.datavalue.AggDataValueKey;
import org.hisp.dhis.datavalue.AggDataValueUpsert;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
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
public class HibernateAggDataValueImportStore extends HibernateGenericStore<DataValue>
    implements AggDataValueImportStore {

  private final PeriodStore periodStore;

  /**
   * Maximum number of {@code VALUES} pairs that get added to a single {@code INSERT} SQL statement.
   */
  private static final int MAX_ROWS_PER_INSERT = 500;

  public HibernateAggDataValueImportStore(
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
      SELECT ou.uid
      FROM organisationunit ou
      JOIN unnest(:ou) AS oux(uid) ON ou.uid = oux.uid
      WHERE NOT EXISTS (
          SELECT 1
          FROM user_orgs
          WHERE ou.path = user_orgs.path  -- Exact match
             OR ou.path LIKE user_orgs.path || '/%'  -- Descendant match
      )
      """;
    String[] ou = orgUnits.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameter("user", user.getValue()).setParameter("ou", ou));
  }

  @Override
  public List<String> getOrgUnitsNotInDataSet(UID dataSet, Stream<UID> orgUnits) {
    String sql =
        """
     SELECT ou.uid
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
  public List<String> getCategoryOptionCombosNotInDataSet(
      UID dataSet, UID dataElement, Stream<UID> optionCombos) {
    String sql =
        """
      SELECT coc.uid
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
  public List<String> getAttributeOptionCombosNotInDataSet(UID dataSet, Stream<UID> optionCombos) {
    String sql =
        """
     SELECT aoc.uid
     FROM categoryoptioncombo aoc
     LEFT JOIN (
       SELECT m.categoryoptioncomboid
       FROM dataset ds
       JOIN categorycombos_optioncombos m ON ds.categorycomboid = m.categorycomboid
       WHERE ds.uid = :ds
     ) excluded ON aoc.categoryoptioncomboid = excluded.categoryoptioncomboid
     WHERE aoc.uid IN (:aoc)
       AND excluded.categoryoptioncomboid IS NULL""";
    String ds = dataSet.getValue();
    String[] aoc = optionCombos.map(UID::getValue).distinct().toArray(String[]::new);
    return listAsStrings(sql, q -> q.setParameterList("aoc", aoc).setParameter("ds", ds));
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
      SELECT de.uid
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
  public boolean getDataSetAccessible(UID dataSet) {
    String accessSql =
        JpaQueryUtils.generateSQlQueryForSharingCheck(
            "ds.sharing", getCurrentUserDetails(), AclService.LIKE_READ_DATA);
    @Language("SQL")
    String sql =
        """
      SELECT ds.uid FROM dataset ds
      WHERE ds.uid = :ds AND (%s);
      """;
    String ds = dataSet.getValue();
    return !listAsStrings(sql.formatted(accessSql), q -> q.setParameter("ds", ds)).isEmpty();
  }

  @Override
  public int deleteByKeys(List<AggDataValueKey> keys) {
    // TODO
    return 0;
  }

  @Override
  public int upsertValues(List<AggDataValue> values) {
    if (values == null || values.isEmpty()) return 0;

    List<AggDataValueUpsert> internalValues = upsertValuesResolveIds(values);
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
              for (AggDataValueUpsert value : internalValues.subList(from, to)) {
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
  private List<AggDataValueUpsert> upsertValuesResolveIds(List<AggDataValue> values) {
    Map<String, Long> des = getDataElementIdMap(values.stream().map(AggDataValue::dataElement));
    Map<String, Long> ous = getOrgUnitIdMap(values.stream().map(AggDataValue::orgUnit));
    Map<String, Long> cocs = getOptionComboIdMap(values);
    Map<String, Long> pes = getPeriodsIdMap(values);
    Function<UID, Long> cocOf = uid -> cocs.get(uid == null ? "default" : uid.getValue());

    List<AggDataValueUpsert> internalValues = new ArrayList<>(values.size());

    for (AggDataValue value : values) {
      Long de = des.get(value.dataElement().getValue());
      Long pe = pes.get(value.period());
      Long ou = ous.get(value.orgUnit().getValue());
      Long coc = cocOf.apply(value.categoryOptionCombo());
      Long aoc = cocOf.apply(value.attributeOptionCombo());
      if (de != null && pe != null && ou != null && coc != null && aoc != null) {
        Boolean deleted = value.deleted();
        if (deleted == null) deleted = false;
        internalValues.add(
            new AggDataValueUpsert(
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

  private Map<String, Long> getOptionComboIdMap(List<AggDataValue> values) {
    long defaultCoc = getDefaultCategoryOptionComboId();
    Map<String, Long> res =
        getOptionComboIdMap(
            Stream.concat(
                values.stream().map(AggDataValue::categoryOptionCombo),
                values.stream().map(AggDataValue::attributeOptionCombo).filter(Objects::nonNull)));
    res.put("", defaultCoc);
    return res;
  }

  @Nonnull
  private Map<String, Long> getPeriodsIdMap(List<AggDataValue> values) {
    List<String> periods = values.stream().map(AggDataValue::period).distinct().toList();
    Map<String, Period> periodsByISO = new HashMap<>(periods.size());
    periods.forEach(p -> periodsByISO.put(p, PeriodType.getPeriodFromIsoString(p)));
    Map<String, Long> pes = new HashMap<>(periodsByISO.size());
    periodsByISO.forEach((iso, period) -> pes.put(iso, periodStore.getPeriodId(period)));
    return pes;
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
