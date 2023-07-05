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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementQueryParams;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.QueryParser;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.planner.QueryPlanner;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Kristian Nordal
 */
@Repository("org.hisp.dhis.minmax.MinMaxDataElementStore")
public class HibernateMinMaxDataElementStore extends HibernateGenericStore<MinMaxDataElement>
    implements MinMaxDataElementStore {
  private final QueryParser queryParser;

  private final QueryPlanner queryPlanner;

  private final SchemaService schemaService;

  public HibernateMinMaxDataElementStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      QueryParser queryParser,
      QueryPlanner queryPlanner,
      SchemaService schemaService) {
    super(sessionFactory, jdbcTemplate, publisher, MinMaxDataElement.class, false);

    checkNotNull(queryParser);
    checkNotNull(queryPlanner);
    checkNotNull(schemaService);

    this.queryParser = queryParser;
    this.queryPlanner = queryPlanner;
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
        .setParameter("path", parent.getPath() + "%")
        .executeUpdate();
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

        Path<?> queryPath = queryPlanner.getQueryPath(root, schema, split[0]);

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
}
