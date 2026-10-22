/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.persistence.TypedQuery;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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
  private final CurrentUserService currentUserService;

  public HibernateDataValueAuditStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      PeriodStore periodStore,
      CurrentUserService currentUserService) {
    super(sessionFactory, jdbcTemplate, publisher, DataValueAudit.class, false);

    checkNotNull(periodStore);

    this.periodStore = periodStore;
    this.currentUserService = currentUserService;
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

    getSession().createQuery(hql).setParameter("unit", organisationUnit).executeUpdate();
  }

  @Override
  public void deleteDataValueAudits(DataElement dataElement) {
    String hql = "delete from DataValueAudit d where d.dataElement = :dataElement";

    getSession().createQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  @Override
  public List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params) {
    TypedQuery<DataValueAudit> query =
        createAuditQuery(
            "select dva.*", params, " order by dva.created desc", DataValueAudit.class);

    if (params.hasPaging()) {
      query
          .setFirstResult(params.getPager().getOffset())
          .setMaxResults(params.getPager().getPageSize());
    }

    return query.getResultList();
  }

  @Override
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    return ((Number)
            createAuditQuery("select count(distinct dva.datavalueauditid)", params, "", null)
                .getSingleResult())
        .intValue();
  }

  @Override
  public boolean isReadable(
      @Nonnull DataElement dataElement,
      @Nonnull OrganisationUnit organisationUnit,
      @Nonnull CategoryOptionCombo categoryOptionCombo,
      @Nonnull CategoryOptionCombo attributeOptionCombo) {
    User user = currentUserService.getCurrentUser();
    if (user.isSuper()) return true;
    String sql =
        createAccessWith(user, true)
            + " select count(*) from (select :de as dataelementid, :ou as organisationunitid,"
            + " :coc as categoryoptioncomboid, :aoc as attributeoptioncomboid) dva where "
            + createAccessCondition(user);
    return ((Number)
                getSession()
                    .createNativeQuery(sql)
                    .setParameter("de", dataElement.getId())
                    .setParameter("ou", organisationUnit.getId())
                    .setParameter("coc", categoryOptionCombo.getId())
                    .setParameter("aoc", attributeOptionCombo.getId())
                    .getSingleResult())
            .longValue()
        > 0;
  }

  /**
   * Creates the query for the data value audits matching the given parameters that the current user
   * may read. Matches nothing if none of the given periods exist.
   */
  private <T> TypedQuery<T> createAuditQuery(
      String select, DataValueAuditQueryParams params, String orderBy, Class<T> type) {
    User user = currentUserService.getCurrentUser();
    Map<String, Object> parameters = new HashMap<>();
    StringBuilder sql =
        new StringBuilder(
                user.isSuper()
                    ? ""
                    : createAccessWith(
                        user,
                        params.getCategoryOptionCombo() != null
                            && params.getAttributeOptionCombo() != null))
            .append(select)
            .append(" from datavalueaudit dva where 1=1");

    if (!params.getPeriods().isEmpty()) {
      List<Period> storedPeriods =
          params.getPeriods().stream()
              .map(periodStore::reloadPeriod)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (storedPeriods.isEmpty()) {
        sql.append(" and 1=0");
      } else {
        sql.append(" and dva.periodid in (:periods)");
        parameters.put(
            "periods", storedPeriods.stream().map(Period::getId).collect(Collectors.toList()));
      }
    }
    if (!params.getDataElements().isEmpty()) {
      sql.append(" and dva.dataelementid in (:dataElements)");
      parameters.put(
          "dataElements",
          params.getDataElements().stream().map(DataElement::getId).collect(Collectors.toList()));
    }
    if (!params.getOrgUnits().isEmpty()) {
      sql.append(" and dva.organisationunitid in (:orgUnits)");
      parameters.put(
          "orgUnits",
          params.getOrgUnits().stream().map(OrganisationUnit::getId).collect(Collectors.toList()));
    }
    if (params.getCategoryOptionCombo() != null) {
      sql.append(" and dva.categoryoptioncomboid = :coc");
      parameters.put("coc", params.getCategoryOptionCombo().getId());
    }
    if (params.getAttributeOptionCombo() != null) {
      sql.append(" and dva.attributeoptioncomboid = :aoc");
      parameters.put("aoc", params.getAttributeOptionCombo().getId());
    }
    if (!params.getAuditTypes().isEmpty()) {
      sql.append(" and dva.audittype in (:auditTypes)");
      parameters.put(
          "auditTypes",
          params.getAuditTypes().stream().map(Enum::name).collect(Collectors.toList()));
    }
    if (!user.isSuper()) {
      sql.append(" and ").append(createAccessCondition(user));
    }
    sql.append(orderBy);

    NativeQuery<T> query =
        type == null
            ? getSession().createNativeQuery(sql.toString())
            : getSession().createNativeQuery(sql.toString(), type);
    parameters.forEach(
        (name, value) -> {
          if (value instanceof java.util.Collection) {
            query.setParameterList(name, (java.util.Collection<?>) value);
          } else {
            query.setParameter(name, value);
          }
        });
    return query;
  }

  /**
   * Compute unreadable combos once, shared by the two NOT IN subplans. This avoids a correlated
   * category option check for every audit row. PostgreSQL before 12 always materializes CTEs; newer
   * versions materialize this one because it is referenced twice. Omit the MATERIALIZED keyword to
   * support both.
   */
  private static String createAccessWith(User user, boolean restrictCombos) {
    return "WITH coc_no_access AS ("
        + "SELECT DISTINCT coc_co.categoryoptioncomboid"
        + " FROM categoryoptioncombos_categoryoptions coc_co"
        + " JOIN dataelementcategoryoption co ON coc_co.categoryoptionid = co.categoryoptionid"
        + " WHERE NOT ("
        + createDataReadCheck("co.sharing", user)
        + ")"
        + (restrictCombos ? " AND coc_co.categoryoptioncomboid IN (:coc, :aoc)" : "")
        // AclService always allows the default combo
        + " AND coc_co.categoryoptioncomboid NOT IN ("
        + "SELECT d.categoryoptioncomboid FROM categorycombos_optioncombos d JOIN categorycombo dcc ON d.categorycomboid = dcc.categorycomboid WHERE dcc.name = 'default'"
        + ")) ";
  }

  /** Data set sharing, capture/view hierarchy and both option combos must allow data read. */
  private static String createAccessCondition(User user) {
    // IS TRUE keeps these uncorrelated checks as hashed subplans instead of nested-loop semi joins.
    return "(dva.dataelementid IN (SELECT dse_acl.dataelementid FROM datasetelement dse_acl"
        + " JOIN dataset ds_acl ON dse_acl.datasetid = ds_acl.datasetid"
        + " WHERE "
        + createDataReadCheck("ds_acl.sharing", user)
        + ")) IS TRUE"
        + " AND (dva.organisationunitid IN (SELECT ou_acl.organisationunitid FROM organisationunit ou_acl"
        + " JOIN organisationunit root_acl ON (ou_acl.path = root_acl.path"
        + " OR ou_acl.path LIKE concat(root_acl.path, '/%'))"
        + " WHERE root_acl.organisationunitid IN ("
        + " SELECT um.organisationunitid FROM usermembership um WHERE um.userinfoid = "
        + user.getId()
        + " UNION SELECT udv.organisationunitid FROM userdatavieworgunits udv WHERE udv.userinfoid = "
        + user.getId()
        + "))) IS TRUE"
        + " AND dva.categoryoptioncomboid NOT IN (SELECT categoryoptioncomboid FROM coc_no_access)"
        + " AND dva.attributeoptioncomboid NOT IN (SELECT categoryoptioncomboid FROM coc_no_access)";
  }

  /**
   * Matches AclService.canDataRead: public, user or group data read/write grants access, including
   * missing access strings. Ownership does not grant data access. IS TRUE makes the condition total
   * even with absent or JSON-null sharing entries.
   *
   * <p>This follows {@code AclService.canDataRead} on purpose and deliberately differs from {@code
   * JpaQueryUtils.generateSQlQueryForSharingCheck}: ownership does not grant data read, and data
   * write implies data read. A shared helper for both checks is a planned follow-up.
   */
  private static String createDataReadCheck(String sharing, User user) {
    String publicAccess = "(" + sharing + "->>'public')";
    String check =
        grantsDataRead(publicAccess)
            + " OR jsonb_typeof("
            + sharing
            + "->'users'->'"
            + user.getUid()
            + "') = 'object' AND "
            + grantsDataRead("(" + sharing + "->'users'->'" + user.getUid() + "'->>'access')");
    if (user.getGroups() != null && !user.getGroups().isEmpty()) {
      String groups =
          user.getGroups().stream()
              .map(group -> group.getUid())
              .sorted()
              .collect(Collectors.joining(","));
      // CASE guarantees jsonb_each only receives objects, including for migrated sharing.
      check +=
          " OR EXISTS (SELECT 1 FROM jsonb_each(CASE WHEN jsonb_typeof("
              + sharing
              + "->'userGroups') = 'object' THEN "
              + sharing
              + "->'userGroups' ELSE '{}' END) g"
              + " WHERE g.key = ANY(cast('{"
              + groups
              + "}' as text[])) AND "
              + grantsDataRead("(g.value->>'access')")
              + ")";
    }
    return "(" + check + ") IS TRUE";
  }

  private static String grantsDataRead(String access) {
    return "("
        + access
        + " IS NULL OR "
        + access
        + " LIKE '"
        + LIKE_READ_DATA
        + "' OR "
        + access
        + " LIKE '"
        + LIKE_WRITE_DATA
        + "')";
  }
}
