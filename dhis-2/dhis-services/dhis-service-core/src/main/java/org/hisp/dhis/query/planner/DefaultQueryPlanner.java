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
package org.hisp.dhis.query.planner;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.query.Conjunction;
import org.hisp.dhis.query.Criterion;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.operators.TokenOperator;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserSettingKey;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@RequiredArgsConstructor
public class DefaultQueryPlanner implements QueryPlanner {
  private final SchemaService schemaService;
  private final SystemSettingManager systemSettingManager;

  @Override
  public QueryPlan planQuery(Query query) {
    return planQuery(query, false);
  }

  @Override
  public QueryPlan planQuery(Query query, boolean persistedOnly) {
    // if only one filter, always set to Junction.Type AND
    Junction.Type junctionType =
        query.getCriterions().size() <= 1 ? Junction.Type.AND : query.getRootJunctionType();

    if (Junction.Type.OR == junctionType && !persistedOnly) {
      return QueryPlan.builder()
          .persistedQuery(Query.from(query.getSchema()).setPlannedQuery(true))
          .nonPersistedQuery(Query.from(query).setPlannedQuery(true))
          .build();
    }

    Query npQuery =
        Query.from(query)
            .setCurrentUserDetails(query.getCurrentUserDetails())
            .setPlannedQuery(true);

    Query pQuery =
        getQuery(npQuery, persistedOnly)
            .setCurrentUserDetails(query.getCurrentUserDetails())
            .setPlannedQuery(true);

    // if there are any non persisted criterions left, we leave the paging
    // to the in-memory engine
    if (!npQuery.isEmpty()) {
      pQuery.setSkipPaging(true);
    } else {
      pQuery.setFirstResult(npQuery.getFirstResult());
      pQuery.setMaxResults(npQuery.getMaxResults());
    }

    return QueryPlan.builder().persistedQuery(pQuery).nonPersistedQuery(npQuery).build();
  }

  @Override
  public QueryPath getQueryPath(Schema schema, String path) {
    Schema curSchema = schema;
    Property curProperty = null;
    boolean persisted = true;
    List<String> alias = new ArrayList<>();
    String[] pathComponents = path.split("\\.");

    if (pathComponents.length == 0) {
      return null;
    }

    for (int idx = 0; idx < pathComponents.length; idx++) {
      String name = pathComponents[idx];
      curProperty = curSchema.getProperty(name);

      if (isFilterByAttributeId(curProperty, name)) {
        // filter by Attribute Uid
        persisted = false;
        curProperty = curSchema.getProperty("attributeValues");
      }

      if (curProperty == null) {
        throw new RuntimeException("Invalid path property: " + name);
      }

      if (!curProperty.isPersisted()) {
        persisted = false;
      }

      if ((!curProperty.isSimple() && idx == pathComponents.length - 1)) {
        return new QueryPath(curProperty, persisted, alias.toArray(new String[] {}));
      }

      if (curProperty.isCollection()) {
        curSchema = schemaService.getDynamicSchema(curProperty.getItemKlass());
        alias.add(curProperty.getFieldName());
      } else if (!curProperty.isSimple()) {
        curSchema = schemaService.getDynamicSchema(curProperty.getKlass());
        alias.add(curProperty.getFieldName());
      } else {
        return new QueryPath(curProperty, persisted, alias.toArray(new String[] {}));
      }
    }

    return new QueryPath(curProperty, persisted, alias.toArray(new String[] {}));
  }

  @Override
  public Path<?> getQueryPath(Root<?> root, Schema schema, String path) {
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

  /**
   * @param query Query
   * @return Query instance
   */
  private Query getQuery(Query query, boolean persistedOnly) {
    Query pQuery = Query.from(query.getSchema(), query.getRootJunctionType());
    pQuery.setSkipSharing(query.isSkipSharing());
    Iterator<Criterion> iterator = query.getCriterions().iterator();

    while (iterator.hasNext()) {
      Criterion criterion = iterator.next();

      if (criterion instanceof Junction) {
        Junction junction = handleJunction(pQuery, (Junction) criterion, persistedOnly);

        if (!junction.getCriterions().isEmpty()) {
          pQuery.getAliases().addAll(junction.getAliases());
          pQuery.add(junction);
        }

        if (((Junction) criterion).getCriterions().isEmpty()) {
          iterator.remove();
        }
      } else if (criterion instanceof Restriction) {
        Restriction restriction = (Restriction) criterion;
        restriction.setQueryPath(getQueryPath(query.getSchema(), restriction.getPath()));

        if (restriction.getOperator().getClass().isAssignableFrom(TokenOperator.class)) {
          setQueryPathLocale(restriction);
        }

        if (restriction.getQueryPath().isPersisted()
            && !restriction.getQueryPath().haveAlias()
            && !Attribute.ObjectType.isValidType(restriction.getQueryPath().getPath())) {
          pQuery
              .getAliases()
              .addAll(Arrays.asList(((Restriction) criterion).getQueryPath().getAlias()));
          pQuery.getCriterions().add(criterion);
          iterator.remove();
        }
      }
    }

    if (query.ordersPersisted()) {
      pQuery.addOrders(query.getOrders());
      query.clearOrders();
    }

    return pQuery;
  }

  private Junction handleJunction(Query query, Junction queryJunction, boolean persistedOnly) {
    Iterator<Criterion> iterator = queryJunction.getCriterions().iterator();
    Junction criteriaJunction =
        queryJunction instanceof Disjunction
            ? new Disjunction(query.getSchema())
            : new Conjunction(query.getSchema());

    while (iterator.hasNext()) {
      Criterion criterion = iterator.next();

      if (criterion instanceof Junction) {
        Junction junction = handleJunction(query, (Junction) criterion, persistedOnly);

        if (!junction.getCriterions().isEmpty()) {
          criteriaJunction.getAliases().addAll(junction.getAliases());
          criteriaJunction.add(junction);
        }

        if (((Junction) criterion).getCriterions().isEmpty()) {
          iterator.remove();
        }
      } else if (criterion instanceof Restriction) {
        Restriction restriction = (Restriction) criterion;
        restriction.setQueryPath(getQueryPath(query.getSchema(), restriction.getPath()));

        if (restriction.getOperator().getClass().isAssignableFrom(TokenOperator.class)) {
          setQueryPathLocale(restriction);
        }

        if (restriction.getQueryPath().isPersisted()
            && !restriction.getQueryPath().haveAlias(1)
            && !Attribute.ObjectType.isValidType(restriction.getQueryPath().getPath())) {
          criteriaJunction
              .getAliases()
              .addAll(Arrays.asList(((Restriction) criterion).getQueryPath().getAlias()));
          criteriaJunction.getCriterions().add(criterion);
          iterator.remove();
        } else if (persistedOnly) {
          throw new RuntimeException(
              "Path "
                  + restriction.getQueryPath().getPath()
                  + " is not fully persisted, unable to build persisted only query plan.");
        }
      }
    }

    return criteriaJunction;
  }

  private boolean isFilterByAttributeId(Property curProperty, String propertyName) {
    return curProperty == null && CodeGenerator.isValidUid(propertyName);
  }

  /**
   * Set the current locale on the query path. The current locale is the user's selected database
   * locale if available, otherwise the system setting DB_Locale. If neither is available, the
   * {@link LocaleManager#DEFAULT_LOCALE} is used.
   *
   * @param restriction the {@link Restriction} which contains the query path.
   */
  private void setQueryPathLocale(Restriction restriction) {
    restriction
        .getQueryPath()
        .setLocale(
            CurrentUserUtil.getUserSetting(
                UserSettingKey.DB_LOCALE,
                systemSettingManager.getSystemSetting(
                    SettingKey.DB_LOCALE, LocaleManager.DEFAULT_LOCALE)));
  }
}
