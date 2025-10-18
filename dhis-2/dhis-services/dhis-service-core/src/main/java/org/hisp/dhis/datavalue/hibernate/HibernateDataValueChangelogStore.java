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

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataValueChangelog;
import org.hisp.dhis.datavalue.DataValueChangelogEntry;
import org.hisp.dhis.datavalue.DataValueChangelogQueryParams;
import org.hisp.dhis.datavalue.DataValueChangelogStore;
import org.hisp.dhis.datavalue.DataValueChangelogType;
import org.hisp.dhis.datavalue.DataValueQueryParams;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.sql.QueryBuilder;
import org.hisp.dhis.sql.SQL;
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

    entityManager.createNativeQuery(sql).setParameter("ou", orgUnit.getValue()).executeUpdate();
  }

  @Override
  public void deleteByDataElement(@Nonnull UID dataElement) {
    String sql =
        """
      DELETE FROM datavalueaudit dva
      WHERE dva.dataelementid = (SELECT de.dataelementid FROM dataelement de WHERE de.uid = :de)""";

    entityManager.createNativeQuery(sql).setParameter("de", dataElement.getValue()).executeUpdate();
  }

  @Override
  public void deleteByOptionCombo(@Nonnull UID categoryOptionCombo) {
    String sql =
        """
        DELETE FROM datavalueaudit dva
        WHERE dva.categoryoptioncomboid = (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc)
           OR dva.attributeoptioncomboid = (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc);""";
    entityManager
        .createNativeQuery(sql)
        .setParameter("coc", categoryOptionCombo.getValue())
        .executeUpdate();
  }

  @Override
  public List<DataValueChangelog> getEntries(DataValueChangelogQueryParams params) {
    return createEntriesQuery(params, SQL.of(getSession())).stream(DataValueChangelog.class)
        .toList();
  }

  @Override
  public int countEntries(DataValueChangelogQueryParams params) {
    return createEntriesQuery(params, SQL.of(getSession())).count();
  }

  static QueryBuilder createEntriesQuery(
      DataValueChangelogQueryParams params, SQL.QueryAPI factory) {
    String sql =
        """
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
      ORDER BY dva.created DESC""";

    Pager pager = params.getPager();
    return SQL.selectOf(sql, factory)
        .setParameter("types", params.getTypes(), DataValueChangelogType::name)
        .setParameter("pe", params.getPeriods(), Period::getIsoDate)
        .setParameter("ds", params.getDataSets())
        .setParameter("de", params.getDataElements())
        .setParameter("ou", params.getOrgUnits())
        .setParameter("coc", params.getCategoryOptionCombo())
        .setParameter("aoc", params.getAttributeOptionCombo())
        .setOffset(pager == null ? null : pager.getOffset())
        .setLimit(pager == null ? null : pager.getPageSize())
        .eraseNullParameterLines()
        .eraseNullJoinLine("de", "de")
        .eraseNullJoinLine("ou", "ou")
        .eraseNullJoinLine("pe", "pe")
        .eraseNullJoinLine("dse", "ds")
        .eraseNullJoinLine("ds", "ds");
  }

  @Override
  public List<DataValueChangelogEntry> getEntries(@Nonnull DataValueQueryParams params) {
    String aoc = getCategoryOptionComboIdByComboAndOptions(params.getCc(), params.getCp());
    return getEntries(
        new DataEntryKey(
            UID.of(params.getDe()),
            UID.of(params.getOu()),
            UID.ofNullable(params.getCo()),
            UID.ofNullable(aoc),
            params.getPe()));
  }

  @Override
  public List<DataValueChangelogEntry> getEntries(@Nonnull DataEntryKey key) {
    @Language("sql")
    String sql =
        """
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
        ORDER BY dva.created DESC""";
    return SQL
        .selectOf(sql, SQL.of(getSession()))
        .setParameter("de", key.dataElement())
        .setParameter("ou", key.orgUnit())
        .setParameter("iso", key.period())
        .eraseNullParameterLines()
        .setParameter("coc", key.categoryOptionCombo())
        .setParameter("aoc", key.attributeOptionCombo())
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
          WITH co_ids AS ( SELECT categoryoptionid FROM categoryoption WHERE uid IN (:cos))
          SELECT coc.uid
          FROM categorycombos_optioncombos coc_cc
          JOIN categoryoptioncombos_categoryoptions coc_co ON coc_cc.categoryoptioncomboid = coc_co.categoryoptioncomboid
          JOIN categoryoptioncombo coc ON coc_co.categoryoptioncomboid = coc.categoryoptioncomboid
          WHERE coc_cc.categorycomboid = (SELECT cc.categorycomboid FROM categorycombo cc WHERE cc.uid = :cc)
            AND coc_co.categoryoptionid IN (SELECT categoryoptionid FROM co_ids)
          GROUP BY coc_co.categoryoptioncomboid
          HAVING COUNT(*) = (SELECT COUNT(*) FROM co_ids)""";
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
    getSession().createNativeQuery(sql).executeUpdate();
  }

  @Override
  public void disableAudit() {
    String sql =
        """
      DROP TRIGGER IF EXISTS trg_datavalue_audit ON datavalue""";
    getSession().createNativeQuery(sql).executeUpdate();
  }
}
