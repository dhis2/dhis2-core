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
package org.hisp.dhis.minmax.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQueryParams;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.minmax.MinMaxValue;
import org.hisp.dhis.minmax.MinMaxValueKey;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.QueryParser;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.intellij.lang.annotations.Language;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Kristian Nordal
 */
@Repository("org.hisp.dhis.minmax.MinMaxDataElementStore")
public class HibernateMinMaxDataElementStore extends HibernateGenericStore<MinMaxDataElement>
    implements MinMaxDataElementStore {

  /**
   * Maximum number of {@code VALUES} pairs that get added to a single {@code INSERT} SQL statement.
   */
  private static final int MAX_ROWS_PER_INSERT = 500;

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

  @SuppressWarnings("unchecked")
  @Override
  public List<String> getDataElementsByDataSet(UID dataSet) {
    String sql =
        """
      SELECT de.uid
      FROM dataset ds
      JOIN datasetelement dse ON ds.datasetid = dse.datasetid
      JOIN dataelement de ON dse.dataelementid = de.dataelementid
      WHERE ds.uid = :uid""";
    return getSession().createNativeQuery(sql).setParameter("uid", dataSet.getValue()).list();
  }

  private Map<String, Long> getDataElementMap(Stream<UID> ids) {
    return getIdMap("dataelement", ids);
  }

  private Map<String, Long> getOrgUnitMap(Stream<UID> ids) {
    return getIdMap("organisationunit", ids);
  }

  private Map<String, Long> getCategoryOptionComboMap(Stream<UID> ids) {
    return getIdMap("categoryoptioncombo", ids);
  }

  @Override
  public int deleteByKeys(List<MinMaxValueKey> keys) {
    if (keys == null || keys.isEmpty()) return 0;

    Map<String, Long> des = getDataElementMap(keys.stream().map(MinMaxValueKey::dataElement));
    Map<String, Long> ous = getOrgUnitMap(keys.stream().map(MinMaxValueKey::orgUnit));
    Map<String, Long> cocs =
        getCategoryOptionComboMap(keys.stream().map(MinMaxValueKey::optionCombo));

    record MinMaxValueKeyInternal(long de, long ou, long coc) {}
    List<MinMaxValueKeyInternal> internalKeys = new ArrayList<>(keys.size());
    for (MinMaxValueKey key : keys) {
      Long de = des.get(key.dataElement().getValue());
      Long ou = ous.get(key.orgUnit().getValue());
      Long coc = cocs.get(key.optionCombo().getValue());
      if (de != null && ou != null && coc != null)
        internalKeys.add(new MinMaxValueKeyInternal(de, ou, coc));
    }
    if (internalKeys.isEmpty()) return 0;
    int size = internalKeys.size();

    Session session = entityManager.unwrap(Session.class);
    @Language("sql")
    String sql1 =
        """
      DELETE FROM minmaxdataelement
      WHERE (dataelementid, sourceid, categoryoptioncomboid) IN ((?, ?, ?))""";

    AtomicInteger deleted = new AtomicInteger();
    session.doWork(
        connection -> {
          int from = 0;
          while (from < size) {
            int n = min(MAX_ROWS_PER_INSERT, size - from);
            int to = from + n;
            int p = 0;
            try (PreparedStatement stmt = connection.prepareStatement(deleteNValuesSql(sql1, n))) {
              for (MinMaxValueKeyInternal key : internalKeys.subList(from, to)) {
                stmt.setLong(p + 1, key.de);
                stmt.setLong(p + 2, key.ou);
                stmt.setLong(p + 3, key.coc);
                p += 3;
              }
              deleted.addAndGet(stmt.executeUpdate());
            }
            from += n;
          }
        });

    session.clear();

    return deleted.get();
  }

  @Nonnull
  private static String deleteNValuesSql(String sql1, int n) {
    if (n == 1) return sql1;
    return sql1.replace("(?, ?, ?)", "(?, ?, ?)" + ", (?, ?, ?)".repeat(n - 1));
  }

  @Override
  public int upsertValues(List<MinMaxValue> values) {
    if (values == null || values.isEmpty()) return 0;

    Map<String, Long> des = getDataElementMap(values.stream().map(MinMaxValue::dataElement));
    Map<String, Long> ous = getOrgUnitMap(values.stream().map(MinMaxValue::orgUnit));
    Map<String, Long> cocs =
        getCategoryOptionComboMap(values.stream().map(MinMaxValue::optionCombo));

    record MinMaxValueInternal(long de, long ou, long coc, int min, int max, boolean generated) {}
    List<MinMaxValueInternal> internalValues = new ArrayList<>(values.size());

    for (MinMaxValue value : values) {
      Long de = des.get(value.dataElement().getValue());
      Long ou = ous.get(value.orgUnit().getValue());
      Long coc = cocs.get(value.optionCombo().getValue());
      if (de != null && ou != null && coc != null) {
        Boolean generated = value.generated();
        if (generated == null) generated = true;
        internalValues.add(
            new MinMaxValueInternal(de, ou, coc, value.minValue(), value.maxValue(), generated));
      }
    }
    if (internalValues.isEmpty()) return 0;
    int size = internalValues.size();

    Session session = entityManager.unwrap(Session.class);

    String sql1 =
        """
      INSERT INTO minmaxdataelement
      (dataelementid, sourceid, categoryoptioncomboid, minimumvalue, maximumvalue, generatedvalue)
      VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT (sourceid, dataelementid, categoryoptioncomboid)
      DO UPDATE SET
        minimumvalue = EXCLUDED.minimumvalue,
        maximumvalue = EXCLUDED.maximumvalue,
        generatedvalue = EXCLUDED.generatedvalue""";

    AtomicInteger imported = new AtomicInteger();
    session.doWork(
        connection -> {
          int from = 0;
          while (from < size) {
            int n = min(MAX_ROWS_PER_INSERT, size - from);
            int to = from + n;
            try (PreparedStatement stmt = connection.prepareStatement(upsertNValuesSql(sql1, n))) {
              int p = 0;
              for (MinMaxValueInternal value : internalValues.subList(from, to)) {
                stmt.setLong(p + 1, value.de);
                stmt.setObject(p + 2, value.ou);
                stmt.setObject(p + 3, value.coc);
                stmt.setInt(p + 4, value.min);
                stmt.setInt(p + 5, value.max);
                stmt.setObject(p + 6, value.generated);
                p += 6;
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
  private static String upsertNValuesSql(String sql1, int n) {
    if (n == 1) return sql1;
    return sql1.replace(
        "(?, ?, ?, ?, ?, ?)", "(?, ?, ?, ?, ?, ?)" + ", (?, ?, ?, ?, ?, ?)".repeat(n - 1));
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

  protected final Map<String, Long> getIdMap(String tableName, Stream<UID> ids) {
    return getIdMap(tableName, tableName + "id", ids);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Long> getIdMap(String tableName, String idColumnName, Stream<UID> ids) {
    String[] uids = ids.map(UID::getValue).distinct().toArray(String[]::new);
    if (uids.length == 1) {

      String sql = "SELECT %s FROM %s WHERE uid = :id";
      List<Object> res =
          getSession()
              .createNativeQuery(sql.formatted(idColumnName, tableName))
              .setParameter("id", uids[0])
              .list();
      return res == null || res.isEmpty()
          ? Map.of()
          : Map.of(uids[0], ((Number) res.get(0)).longValue());
    }

    String sql =
        """
        SELECT t.uid, t.%s
        FROM %s t
        JOIN unnest(:ids) AS u(uid) ON t.uid = u.uid""";
    return mapIdToLong(
        getSession()
            .createNativeQuery(sql.formatted(idColumnName, tableName))
            .setParameter("ids", uids)
            .list());
  }

  private static Map<String, Long> mapIdToLong(List<Object[]> results) {
    return Map.copyOf(
        results.stream()
            .collect(
                Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue())));
  }
}
