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
package org.hisp.dhis.organisationunit.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Kristian Nordal
 */
@Repository("org.hisp.dhis.organisationunit.OrganisationUnitStore")
public class HibernateOrganisationUnitStore
    extends HibernateIdentifiableObjectStore<OrganisationUnit> implements OrganisationUnitStore {
  private final DbmsManager dbmsManager;

  public HibernateOrganisationUnitStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService,
      DbmsManager dbmsManager) {
    super(entityManager, jdbcTemplate, publisher, OrganisationUnit.class, aclService, true);

    checkNotNull(dbmsManager);

    this.dbmsManager = dbmsManager;
  }

  // -------------------------------------------------------------------------
  // OrganisationUnit
  // -------------------------------------------------------------------------

  @Override
  public List<String> getOrganisationUnitsUidsByUser(String username) {
    String sql = getOrgUnitTablesUids(username, "usermembership");
    return jdbcTemplate.queryForList(sql, String.class);
  }

  @Override
  public List<String> getSearchOrganisationUnitsUidsByUser(String username) {
    String sql = getOrgUnitTablesUids(username, "userteisearchorgunits");
    return jdbcTemplate.queryForList(sql, String.class);
  }

  @Override
  public List<String> getDataViewOrganisationUnitsUidsByUser(String username) {
    String sql = getOrgUnitTablesUids(username, "userdatavieworgunits");
    return jdbcTemplate.queryForList(sql, String.class);
  }

  private static String getOrgUnitTablesUids(String username, String orgUnitTableName) {
    return """
        select ou.uid
        from organisationunit ou
        join %s um ON ou.organisationunitid = um.organisationunitid
        join userinfo ui ON um.userinfoid = ui.userinfoid
        where ui.username = '%s';"""
        .formatted(orgUnitTableName, username);
  }

  @Override
  public List<OrganisationUnit> getAllOrganisationUnitsByLastUpdated(Date lastUpdated) {
    return getAllGeLastUpdated(lastUpdated);
  }

  @Override
  public List<OrganisationUnit> getRootOrganisationUnits() {
    return getQuery("from OrganisationUnit o where o.parent is null").list();
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnitsWithoutGroups() {
    return getQuery("from OrganisationUnit o where size(o.groups) = 0").list();
  }

  @Override
  public List<OrganisationUnit> getOrphanedOrganisationUnits() {
    return getQuery(
            "from OrganisationUnit o where o.parent is null and not exists "
                + "(select 1 from OrganisationUnit io where io.parent = o.id)")
        .list();
  }

  @Override
  public Set<OrganisationUnit> getOrganisationUnitsWithCyclicReferences() {
    return getQuery(
            "from OrganisationUnit o where exists (select 1 from OrganisationUnit i "
                + "where i.id <> o.id "
                + "and i.path like concat('%', o.uid, '%') "
                + "and o.path like concat('%', i.uid, '%'))")
        .stream()
        .collect(toSet());
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnitsViolatingExclusiveGroupSets() {
    // OBS: size(o.groups) > 1 is just to narrow search right away
    return getQuery(
            "from OrganisationUnit o where size(o.groups) > 1 and exists "
                + "(select 1 from OrganisationUnitGroupSet s where "
                + "(select count(*) from OrganisationUnitGroup g where o in elements(g.members) and s in elements(g.groupSets)) > 1)")
        .list();
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnitsWithProgram(Program program) {
    final String jpql =
        "select distinct o from OrganisationUnit o " + "join o.programs p where p.id = :programId";

    return getQuery(jpql).setParameter("programId", program.getId()).list();
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnitsByProgram(String programUid) {
    String jpql =
        "select distinct o from OrganisationUnit o join o.programs p where p.uid = :programUid";

    return getQuery(jpql).setParameter("programUid", programUid).list();
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnitsByDataSet(String dataSetUid) {
    String jpql =
        "select distinct o from OrganisationUnit o join o.dataSets d where d.uid = :dataSetUid";

    return getQuery(jpql).setParameter("dataSetUid", dataSetUid).list();
  }

  @Override
  public Long getOrganisationUnitHierarchyMemberCount(
      OrganisationUnit parent, Object member, String collectionName) {
    final String hql =
        "select count(*) from OrganisationUnit o "
            + "where o.path like :path "
            + "and :object in elements(o."
            + collectionName
            + ")";

    Query<Long> query = getTypedQuery(hql);
    query.setParameter("path", parent.getStoredPath() + "%").setParameter("object", member);

    return query.getSingleResult();
  }

  @Override
  public List<OrganisationUnit> getOrganisationUnits(OrganisationUnitQueryParams params) {
    SqlHelper hlp = new SqlHelper();

    String hql = "select distinct o from OrganisationUnit o ";

    if (params.isFetchChildren()) {
      hql += "left join fetch o.children c ";
    }

    if (params.hasGroups()) {
      hql += "join o.groups og ";
    }

    if (params.hasQuery()) {
      hql +=
          hlp.whereAnd()
              + " (lower(o.name) like :queryLower or o.code = :query or o.uid = :query) ";
    }

    if (params.hasParents()) {
      hql += hlp.whereAnd() + " (";

      for (OrganisationUnit parent : params.getParents()) {
        hql += "o.path like :" + parent.getUid() + " or ";
      }

      hql = TextUtils.removeLastOr(hql) + ") ";
    }

    if (params.hasGroups()) {
      hql += hlp.whereAnd() + " og.id in (:groupIds) ";
    }

    if (params.hasLevels()) {
      hql += hlp.whereAnd() + " o.hierarchyLevel in (:levels) ";
    }

    if (params.getMaxLevels() != null) {
      hql += hlp.whereAnd() + " o.hierarchyLevel <= :maxLevels ";
    }

    hql += "order by o." + params.getOrderBy().getName();

    Query<OrganisationUnit> query = getQuery(hql);

    if (params.hasQuery()) {
      query.setParameter("queryLower", "%" + params.getQuery().toLowerCase() + "%");
      query.setParameter("query", params.getQuery());
    }

    if (params.hasParents()) {
      for (OrganisationUnit parent : params.getParents()) {
        query.setParameter(parent.getUid(), parent.getStoredPath() + "%");
      }
    }

    if (params.hasGroups()) {
      query.setParameterList(
          "groupIds", IdentifiableObjectUtils.getIdentifiers(params.getGroups()));
    }

    if (params.hasLevels()) {
      query.setParameterList("levels", params.getLevels());
    }

    if (params.getMaxLevels() != null) {
      query.setParameter("maxLevels", params.getMaxLevels());
    }

    if (params.getFirst() != null) {
      query.setFirstResult(params.getFirst());
    }

    if (params.getMax() != null) {
      query.setMaxResults(params.getMax()).list();
    }

    return query.list();
  }

  @Override
  public List<OrganisationUnit> getWithinCoordinateArea(double[] box) {
    // can't use hibernate-spatial 'makeenvelope' function, because not
    // available in
    // current hibernate version
    // see: https://hibernate.atlassian.net/browse/HHH-13083

    if (box != null && box.length == 4) {
      return getSession()
          .createQuery(
              "from OrganisationUnit ou "
                  + "where within(ou.geometry, "
                  + doMakeEnvelopeSql(box)
                  + ") = true",
              OrganisationUnit.class)
          .getResultList();
    }
    return new ArrayList<>();
  }

  private String doMakeEnvelopeSql(double[] box) {
    // equivalent to: postgis 'ST_MakeEnvelope'
    // (https://postgis.net/docs/ST_MakeEnvelope.html)
    return "ST_MakeEnvelope(" + box[1] + "," + box[0] + "," + box[3] + "," + box[2] + ", 4326)";
  }

  // -------------------------------------------------------------------------
  // OrganisationUnitHierarchy
  // -------------------------------------------------------------------------

  @Override
  public void updatePaths() {
    getQuery("from OrganisationUnit ou where ou.path is null or ou.hierarchyLevel is null").list();
  }

  @Override
  public void forceUpdatePaths() {
    List<OrganisationUnit> organisationUnits =
        new ArrayList<>(getQuery("from OrganisationUnit").list());
    updatePaths(organisationUnits);
  }

  @Override
  public int getMaxLevel() {
    String hql = "select max(ou.hierarchyLevel) from OrganisationUnit ou";

    Query<Integer> query = getTypedQuery(hql);
    Integer maxLength = getSingleResult(query);

    return maxLength != null ? maxLength : 0;
  }

  @Override
  public int updateAllOrganisationUnitsGeometryToNull() {
    return getQuery("update OrganisationUnit o set o.geometry = null").executeUpdate();
  }

  @Override
  public List<OrganisationUnit> getByCategoryOption(@Nonnull Collection<String> categoryOptions) {
    if (categoryOptions.isEmpty()) return List.of();
    return getQuery(
            """
            select distinct ou from OrganisationUnit ou \
            join ou.categoryOptions co \
            where co.uid in :categoryOptions""",
            OrganisationUnit.class)
        .setParameter("categoryOptions", categoryOptions)
        .getResultList();
  }

  private void updatePaths(List<OrganisationUnit> organisationUnits) {
    Session session = getSession();
    int counter = 0;

    for (OrganisationUnit organisationUnit : organisationUnits) {
      session.update(organisationUnit);

      if ((counter % 400) == 0) {
        dbmsManager.flushSession();
      }

      counter++;
    }
  }
}
