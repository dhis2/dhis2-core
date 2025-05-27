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
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.AggDataValue;
import org.hisp.dhis.datavalue.AggDataValueKey;
import org.hisp.dhis.datavalue.AggDataValueStore;
import org.hisp.dhis.datavalue.AggDataValueUpsertSummary;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
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
public class HibernateAggDataValueStore extends HibernateGenericStore<DataValue>
    implements AggDataValueStore {

  private final PeriodStore periodStore;

  /**
   * Maximum number of {@code VALUES} pairs that get added to a single {@code INSERT} SQL statement.
   */
  private static final int MAX_ROWS_PER_INSERT = 500;

  public HibernateAggDataValueStore(
      EntityManager entityManager,
      PeriodStore periodStore,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<DataValue> clazz,
      boolean cacheable) {
    super(entityManager, jdbcTemplate, publisher, clazz, cacheable);
    this.periodStore = periodStore;
  }

  @Override
  public int deleteByKeys(List<AggDataValueKey> keys) {
    return 0;
  }

  private record AggDataValueInternal(
      long de,
      long pe,
      long ou,
      long coc,
      long aoc,
      String value,
      String comment,
      Boolean followup,
      boolean deleted) {}

  @Override
  public AggDataValueUpsertSummary upsertValues(List<AggDataValue> values) {
    if (values == null || values.isEmpty())
      return new AggDataValueUpsertSummary(0, List.of(), List.of(), List.of());

    AggDataValueUpsertSummary res =
        new AggDataValueUpsertSummary(
            0, new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0));
    List<AggDataValueInternal> internalValues = upsertValuesResolveIds(values, res);
    if (internalValues.isEmpty()) return res;

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
          try (PreparedStatement stmt = conn.prepareStatement("SET LOCAL dhis2.user TO ?")) {
            stmt.setString(1, user);
            stmt.execute();
          }
          int from = 0;
          while (from < size) {
            int n = min(MAX_ROWS_PER_INSERT, size - from);
            int to = from + n;
            try (PreparedStatement stmt = conn.prepareStatement(upsertNValuesSql(sql1, n))) {
              int p = 0;
              for (AggDataValueInternal value : internalValues.subList(from, to)) {
                stmt.setLong(p + 1, value.de);
                stmt.setLong(p + 2, value.pe);
                stmt.setLong(p + 3, value.ou);
                stmt.setLong(p + 4, value.coc);
                stmt.setLong(p + 5, value.aoc);
                stmt.setString(p + 6, value.value);
                stmt.setString(p + 7, value.comment);
                stmt.setObject(p + 8, value.followup);
                stmt.setBoolean(p + 9, value.deleted);
                p += 9;
              }
              imported.addAndGet(stmt.executeUpdate());
            }
            from += n;
          }
        });

    session.clear();

    return new AggDataValueUpsertSummary(
        imported.get(), res.noSuchDataElement(), res.noSuchOrgUnit(), res.noSuchOptionCombo());
  }

  @Nonnull
  private List<AggDataValueInternal> upsertValuesResolveIds(
      List<AggDataValue> values, AggDataValueUpsertSummary summary) {
    Map<String, Long> des = getDataElementIdMap(values.stream().map(AggDataValue::dataElement));
    Map<String, Long> ous = getOrgUnitIdMap(values.stream().map(AggDataValue::orgUnit));
    Map<String, Long> cocs = getOptionComboIdMap(values);
    Map<String, Long> pes = getPeriodsIdMap(values);
    Function<UID, Long> cocOf = uid -> cocs.get(uid == null ? "default" : uid.getValue());

    List<AggDataValueInternal> internalValues = new ArrayList<>(values.size());

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
            new AggDataValueInternal(
                de, pe, ou, coc, aoc, value.value(), value.comment(), value.followUp(), deleted));
      } else {
        if (de == null) summary.noSuchDataElement().add(value);
        if (ou == null) summary.noSuchOrgUnit().add(value);
        if (coc == null || aoc == null) summary.noSuchOptionCombo().add(value);
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
    res.put("default", defaultCoc);
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
}
