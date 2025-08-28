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

import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.LongType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditEntry;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueAuditType;
import org.hisp.dhis.datavalue.DataValueQueryParams;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Repository
public class HibernateDataValueAuditStore extends HibernateGenericStore<DataValueAudit>
    implements DataValueAuditStore {

  public HibernateDataValueAuditStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, DataValueAudit.class, false);
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
    String sql =
        """
      SELECT *
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      WHERE (cardinality(:types) = 0 OR dva.audittype = ANY(:types))
        AND (cardinality(:de) = 0 OR dva.dataelementid = ANY(:de))
        AND (cardinality(:ou) = 0 OR dva.organisationunitid = ANY(:ou))
        AND (:coc IS NULL OR dva.categoryoptioncomboid = :coc)
        AND (:aoc IS NULL OR dva.categoryoptioncomboid = :aoc)
        AND (cardinality(:pe) = 0 OR pe.iso = ANY(:pe))
      ORDER BY dva.created DESC""";

    NativeQuery<DataValueAudit> query = setParameters(nativeSynchronizedTypedQuery(sql), params);
    Pager pager = params.getPager();
    if (pager != null) {
      query.setFirstResult(pager.getOffset()).setMaxResults(pager.getPageSize());
    }
    return query.list();
  }

  @Override
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    String sql =
        """
      SELECT count(*)
      FROM datavalueaudit dva
      JOIN period pe ON dva.periodid = pe.periodid
      WHERE (:types IS NULL OR dva.audittype = ANY(:types))
        AND (cardinality(:de) = 0 OR dva.dataelementid = ANY(:de))
        AND (cardinality(:ou) = 0 OR dva.organisationunitid = ANY(:ou))
        AND (:coc IS NULL OR dva.categoryoptioncomboid = :coc)
        AND (:aoc IS NULL OR dva.categoryoptioncomboid = :aoc)
        AND (cardinality(:pe) = 0 OR pe.iso = ANY(:pe))""";

    NativeQuery<?> query = nativeSynchronizedQuery(sql);
    return setParameters(query, params).getSingleResult() instanceof Number n ? n.intValue() : 0;
  }

  private <E> NativeQuery<E> setParameters(NativeQuery<E> query, DataValueAuditQueryParams params) {
    String[] types =
        params.getAuditTypes() == null
            ? null
            : params.getAuditTypes().stream().map(Enum::name).toArray(String[]::new);
    String[] periods =
        params.getPeriods() == null
            ? null
            : params.getPeriods().stream().map(Period::getIsoDate).toArray(String[]::new);
    return query
        .setParameter("types", types, StringArrayType.INSTANCE)
        .setParameter("pe", periods, StringArrayType.INSTANCE)
        .setParameter("de", getIds(params.getDataElements()), LongArrayType.INSTANCE)
        .setParameter("ou", getIds(params.getOrgUnits()), LongArrayType.INSTANCE)
        .setParameter("coc", getId(params.getCategoryOptionCombo()), LongType.INSTANCE)
        .setParameter("aoc", getId(params.getAttributeOptionCombo()), LongType.INSTANCE);
  }

  private static Long[] getIds(List<? extends IdentifiableObject> objects) {
    return objects == null
        ? null
        : objects.stream().map(IdentifiableObject::getId).toArray(Long[]::new);
  }

  private static Long getId(IdentifiableObject object) {
    return object == null ? null : object.getId();
  }

  @Override
  public List<DataValueAuditEntry> getAuditsByKey(@Nonnull DataValueQueryParams params) {
    Long aoc = getCategoryOptionComboIdByComboAndOptions(params.getCc(), params.getCp());
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
        WHERE   de.uid = :de
            AND ou.uid = :ou
            AND pe.iso = :iso
            AND (:coc IS NOT NULL AND coc.uid = :coc OR :coc IS NULL AND coc.name = 'default')
            AND aoc.categoryoptioncomboid = :aoc
        ORDER BY dva.created DESC""";
    @SuppressWarnings("unchecked")
    List<Object[]> rows =
        getSession()
            .createNativeQuery(sql)
            .setParameter("de", params.getDe())
            .setParameter("ou", params.getOu())
            .setParameter("iso", params.getPe())
            .setParameter("coc", params.getCo())
            .setParameter("aoc", aoc)
            .list();
    if (rows.isEmpty()) return List.of();
    return rows.stream()
        .map(
            row ->
                new DataValueAuditEntry(
                    (String) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    (Date) row[7],
                    DataValueAuditType.valueOf((String) row[8])))
        .toList();
  }

  @CheckForNull
  @SuppressWarnings("unchecked")
  private Long getCategoryOptionComboIdByComboAndOptions(
      String categoryCombo, String categoryOptions) {
    Object aocId;
    if (categoryCombo == null && categoryOptions == null) {
      String aocSql =
          "SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE name = 'default' LIMIT 1";
      aocId = getSingleResult(getSession().createNativeQuery(aocSql));
    } else {
      String aocSql =
          """
          WITH co_ids AS ( SELECT categoryoptionid FROM categoryoption WHERE uid IN (:cos))
          SELECT coc_co.categoryoptioncomboid
          FROM categorycombos_optioncombos coc_cc
          JOIN categoryoptioncombos_categoryoptions coc_co ON coc_cc.categoryoptioncomboid = coc_co.categoryoptioncomboid
          WHERE coc_cc.categorycomboid = (SELECT cc.categorycomboid FROM categorycombo cc WHERE cc.uid = :cc)
            AND coc_co.categoryoptionid IN (SELECT categoryoptionid FROM co_ids)
          GROUP BY coc_co.categoryoptioncomboid
          HAVING COUNT(*) = (SELECT COUNT(*) FROM co_ids)""";
      aocId =
          getSingleResult(
              getSession()
                  .createNativeQuery(aocSql)
                  .setParameter("cc", categoryCombo)
                  .setParameterList("cos", categoryOptions.split(";")));
    }
    return aocId instanceof Number n ? n.longValue() : null;
  }
}
