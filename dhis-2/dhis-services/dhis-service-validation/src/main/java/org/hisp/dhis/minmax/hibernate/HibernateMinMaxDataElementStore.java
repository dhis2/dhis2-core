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
package org.hisp.dhis.minmax.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hibernate.query.Query;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQueryParams;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.minmax.MinMaxValueDto;
import org.hisp.dhis.minmax.ResolvedMinMaxDto;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.QueryParser;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@Repository("org.hisp.dhis.minmax.MinMaxDataElementStore")
public class HibernateMinMaxDataElementStore extends HibernateGenericStore<MinMaxDataElement>
    implements MinMaxDataElementStore {
  private final QueryParser queryParser;

  private final SchemaService schemaService;

  public HibernateMinMaxDataElementStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      QueryParser queryParser,
      SchemaService schemaService) {
    super(entityManager, jdbcTemplate, publisher, MinMaxDataElement.class, false);

    checkNotNull(queryParser);
    checkNotNull(schemaService);

    this.queryParser = queryParser;
    this.schemaService = schemaService;
  }

  // -------------------------------------------------------------------------
  // MinMaxDataElementStore Implementation
  // -------------------------------------------------------------------------

  @Override
  public MinMaxDataElement get(
      OrganisationUnit source, DataElement dataElement, CategoryOptionCombo optionCombo) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("source"), source))
            .addPredicate(root -> builder.equal(root.get("dataElement"), dataElement))
            .addPredicate(root -> builder.equal(root.get("optionCombo"), optionCombo)));
  }

  @Override
  public List<MinMaxDataElement> get(
      OrganisationUnit source, Collection<DataElement> dataElements) {
    if (dataElements.isEmpty()) {
      return new ArrayList<>();
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("source"), source))
            .addPredicate(root -> root.get("dataElement").in(dataElements)));
  }

  @Override
  public List<MinMaxDataElement> query(MinMaxDataElementQueryParams query) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<MinMaxDataElement> parameters = newJpaParameters();
    parameters.addPredicate(root -> parseFilter(builder, root, query.getFilters()));

    if (!query.isSkipPaging()) {
      Pager pager = query.getPager();
      parameters.setFirstResult(pager.getOffset());
      parameters.setMaxResults(pager.getPageSize());
    }

    return getList(builder, parameters);
  }

  @Override
  public int countMinMaxDataElements(MinMaxDataElementQueryParams query) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
            builder,
            newJpaParameters()
                .addPredicate(root -> parseFilter(builder, root, query.getFilters()))
                .setUseDistinct(true))
        .intValue();
  }

  @Override
  public void delete(OrganisationUnit organisationUnit) {
    String hql = "delete from MinMaxDataElement m where m.source = :source";

    getQuery(hql).setParameter("source", organisationUnit).executeUpdate();
  }

  @Override
  public void delete(DataElement dataElement) {
    String hql = "delete from MinMaxDataElement m where m.dataElement = :dataElement";

    getQuery(hql).setParameter("dataElement", dataElement).executeUpdate();
  }

  @Override
  public void delete(CategoryOptionCombo optionCombo) {
    String hql = "delete from MinMaxDataElement m where m.optionCombo = :optionCombo";

    getQuery(hql).setParameter("optionCombo", optionCombo).executeUpdate();
  }

  @Override
  public void delete(Collection<DataElement> dataElements, OrganisationUnit parent) {
    String hql =
        "delete from MinMaxDataElement m where m.dataElement in (:dataElements) "
            + "and m.source in (select ou from OrganisationUnit ou where path like :path)";

    getQuery(hql)
        .setParameterList("dataElements", dataElements)
        .setParameter("path", parent.getStoredPath() + "%")
        .executeUpdate();
  }

  @Override
  public void delete(UID deUID, UID ouUID, UID cocUID) {
    String sql =
        """
      DELETE FROM minmaxdataelement
      WHERE dataelementid = (SELECT dataelementid FROM dataelement WHERE uid = :de)
        AND sourceid = (SELECT organisationunitid FROM organisationunit WHERE uid = :ou)
        AND categoryoptioncomboid = (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc)
      """;

    getSession()
        .createNativeQuery(sql)
        .setParameter("de", deUID.getValue())
        .setParameter("ou", ouUID.getValue())
        .setParameter("coc", cocUID.getValue())
        .executeUpdate();
  }

  /**
   * Deletes a list of MinMaxDataElement records in bulk based on the provided list of
   *
   * @param dtos
   */
  @Override
  @Transactional
  public void deleteBulkByDtos(List<MinMaxValueDto> dtos) {
    if (dtos == null || dtos.isEmpty()) return;

    final int CHUNK_SIZE = 1000;

    for (int i = 0; i < dtos.size(); i += CHUNK_SIZE) {
      List<MinMaxValueDto> chunk = dtos.subList(i, Math.min(i + CHUNK_SIZE, dtos.size()));
      executeChunkedDelete(chunk);
    }
  }

  /**
   * Deletes a chunk of MinMaxDataElement records based on the provided list of MinMaxValueDto
   * objects.
   *
   * @param chunk List of MinMaxValueDto objects representing the records to be deleted.
   */
  private void executeChunkedDelete(List<MinMaxValueDto> chunk) {
    StringBuilder sql =
        new StringBuilder(
            """
      DELETE FROM minmaxdataelement
      WHERE (dataelementid, sourceid, categoryoptioncomboid) IN (
  """);

    List<String> selects = new ArrayList<>();
    for (int i = 0; i < chunk.size(); i++) {
      selects.add(
          String.format(
              """
        SELECT
          (SELECT dataelementid FROM dataelement WHERE uid = :de%d),
          (SELECT organisationunitid FROM organisationunit WHERE uid = :ou%d),
          (SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE uid = :coc%d)
      """,
              i, i, i));
    }

    sql.append(String.join(" UNION ALL ", selects)).append(")");

    Query<?> query = getSession().createNativeQuery(sql.toString());

    for (int i = 0; i < chunk.size(); i++) {
      MinMaxValueDto dto = chunk.get(i);
      query.setParameter("de" + i, dto.getDataElement());
      query.setParameter("ou" + i, dto.getOrgUnit());
      query.setParameter("coc" + i, dto.getCategoryOptionCombo());
    }

    query.executeUpdate();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UID, Long> getDataElementMap(@Nonnull Collection<UID> uids) {
    if (uids.isEmpty()) return Map.of();

    List<Object[]> results =
        getSession()
            .createNativeQuery("SELECT uid, dataelementid FROM dataelement WHERE uid IN :uids")
            .setParameter("uids", uids.stream().map(UID::getValue).toList())
            .getResultList();

    return Map.copyOf(
        results.stream()
            .collect(
                Collectors.toMap(
                    row -> UID.of((String) row[0]), row -> ((Number) row[1]).longValue())));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UID, Long> getOrgUnitMap(@Nonnull Collection<UID> uids) {
    if (uids.isEmpty()) return Map.of();

    List<Object[]> results =
        getSession()
            .createNativeQuery(
                "SELECT uid, organisationunitid FROM organisationunit WHERE uid IN :uids")
            .setParameter("uids", uids.stream().map(UID::getValue).toList())
            .getResultList();

    return Map.copyOf(
        results.stream()
            .collect(
                Collectors.toMap(
                    row -> UID.of((String) row[0]), row -> ((Number) row[1]).longValue())));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UID, Long> getCategoryOptionComboMap(@Nonnull Collection<UID> uids) {
    if (uids.isEmpty()) return Map.of();

    List<Object[]> results =
        getSession()
            .createNativeQuery(
                "SELECT uid, categoryoptioncomboid FROM categoryoptioncombo WHERE uid IN :uids")
            .setParameter("uids", uids.stream().map(UID::getValue).toList())
            .getResultList();

    return Map.copyOf(
        results.stream()
            .collect(
                Collectors.toMap(
                    row -> UID.of((String) row[0]), row -> ((Number) row[1]).longValue())));
  }

  @Override
  @Transactional
  public void upsertResolvedDtos(List<ResolvedMinMaxDto> chunk) {
    if (chunk == null || chunk.isEmpty()) {
      return;
    }

    StringBuilder sql =
        new StringBuilder(
            """
    INSERT INTO minmaxdataelement (
      dataelementid, sourceid, categoryoptioncomboid,
      minimumvalue, maximumvalue, generatedvalue
    )
    VALUES
  """);

    List<String> valuePlaceholders = new ArrayList<>();
    for (int i = 0; i < chunk.size(); i++) {
      valuePlaceholders.add(
          String.format("(:de%d, :ou%d, :coc%d, :min%d, :max%d, :gen%d)", i, i, i, i, i, i));
    }

    sql.append(String.join(",\n", valuePlaceholders))
        .append(
            """

    ON CONFLICT (sourceid, dataelementid, categoryoptioncomboid)
    DO UPDATE SET
      minimumvalue = EXCLUDED.minimumvalue,
      maximumvalue = EXCLUDED.maximumvalue,
      generatedvalue = EXCLUDED.generatedvalue
  """);

    Query<?> query = getSession().createNativeQuery(sql.toString());

    for (int i = 0; i < chunk.size(); i++) {
      ResolvedMinMaxDto dto = chunk.get(i);
      query.setParameter("de" + i, dto.dataElementId());
      query.setParameter("ou" + i, dto.orgUnitId());
      query.setParameter("coc" + i, dto.categoryOptionComboId());
      query.setParameter("min" + i, dto.minValue());
      query.setParameter("max" + i, dto.maxValue());
      query.setParameter("gen" + i, dto.generated());
    }

    query.executeUpdate();
  }

  @Override
  public List<MinMaxDataElement> getByDataElement(Collection<DataElement> dataElements) {
    return getQuery(
            """
            from  MinMaxDataElement mmde
            where mmde.dataElement in :dataElements
            """,
            MinMaxDataElement.class)
        .setParameter("dataElements", dataElements)
        .list();
  }

  @Override
  public List<MinMaxDataElement> getByCategoryOptionCombo(@Nonnull Collection<UID> uids) {
    if (uids.isEmpty()) return List.of();
    return getQuery(
            """
            select distinct mmde from  MinMaxDataElement mmde
            join mmde.optionCombo coc
            where coc.uid in :uids
            """)
        .setParameter("uids", UID.toValueList(uids))
        .list();
  }

  private Predicate parseFilter(CriteriaBuilder builder, Root<?> root, List<String> filters) {
    Predicate conjunction = builder.conjunction();

    Schema schema = schemaService.getDynamicSchema(MinMaxDataElement.class);

    if (!filters.isEmpty()) {
      for (String filter : filters) {
        String[] split = filter.split(":");

        if (split.length != 3) {
          throw new QueryParserException("Invalid filter: " + filter);
        }

        Path<?> queryPath = getQueryPath(root, schema, split[0]);

        Property property = queryParser.getProperty(schema, split[0]);

        Predicate predicate =
            JpaQueryUtils.getPredicate(builder, property, queryPath, split[1], split[2]);

        if (predicate != null) {
          conjunction.getExpressions().add(predicate);
        }
      }
    }

    return conjunction;
  }

  private Path<?> getQueryPath(Root<?> root, Schema schema, String path) {
    Schema curSchema = schema;
    Property curProperty;
    String[] pathComponents = path.split("\\.");

    Path<?> currentPath = root;

    if (pathComponents.length == 0) {
      return null;
    }

    for (int idx = 0; idx < pathComponents.length; idx++) {
      String name = pathComponents[idx];
      curProperty = curSchema.getProperty(name);

      if (curProperty == null) {
        throw new RuntimeException("Invalid path property: " + name);
      }

      if ((!curProperty.isSimple() && idx == pathComponents.length - 1)) {
        return root.join(curProperty.getFieldName());
      }

      if (curProperty.isCollection()) {
        currentPath = root.join(curProperty.getFieldName());
        curSchema = schemaService.getDynamicSchema(curProperty.getItemKlass());
      } else if (!curProperty.isSimple()) {
        curSchema = schemaService.getDynamicSchema(curProperty.getKlass());
        currentPath = root.join(curProperty.getFieldName());
      } else {
        return currentPath.get(curProperty.getFieldName());
      }
    }

    return currentPath;
  }
}
