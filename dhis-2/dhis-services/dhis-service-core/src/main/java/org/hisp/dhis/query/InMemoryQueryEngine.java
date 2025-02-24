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
package org.hisp.dhis.query;

import static org.hisp.dhis.query.Restrictions.eq;
import static org.hisp.dhis.query.Restrictions.ilike;
import static org.hisp.dhis.query.Restrictions.in;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component("org.hisp.dhis.query.InMemoryQueryEngine")
@AllArgsConstructor
public class InMemoryQueryEngine implements QueryEngine {
  private final SchemaService schemaService;

  private final AclService aclService;

  @Override
  public <T extends IdentifiableObject> List<T> query(Query query) {
    validateQuery(query);
    List<T> list = runQuery(query);
    list = runSorter(query, list);

    return query.isSkipPaging()
        ? list
        : PagerUtils.pageCollection(list, query.getFirstResult(), query.getMaxResults());
  }

  @Override
  public <T extends IdentifiableObject> long count(Query query) {
    validateQuery(query);
    List<T> list = runQuery(query);
    return list.size();
  }

  private void validateQuery(Query query) {
    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(CurrentUserUtil.getCurrentUserDetails());
    }

    if (query.getSchema() == null) {
      throw new QueryException("Invalid Query object, does not contain Schema");
    }

    if (query.getObjects() == null) {
      throw new QueryException("InMemoryQueryEngine requires an existing object list to work on.");
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends IdentifiableObject> List<T> runQuery(Query query) {
    return query.getObjects().stream()
        .filter(object -> test(query, (T) object))
        .map(object -> (T) object)
        .collect(Collectors.toList());
  }

  private <T extends IdentifiableObject> List<T> runSorter(Query query, List<T> objects) {
    List<T> sorted = new ArrayList<>(objects);

    sorted.sort(
        (o1, o2) -> {
          for (Order order : query.getOrders()) {
            int result = order.compare(o1, o2);
            if (result != 0) return result;
          }

          return 0;
        });

    return sorted;
  }

  private <T> boolean test(Query query, T object) {
    if (query.getRootJunctionType() == Junction.Type.OR) {
      // OR
      for (Restriction filter : query.getFilters()) {
        if (test(query, object, filter)) return true;
      }
      return false;
    }
    // AND
    for (Restriction filter : query.getFilters()) {
      if (!test(query, object, filter)) return false;
    }
    return true;
  }

  private <T> boolean test(Query query, T object, Restriction filter) {
    if (filter.isVirtual()) {
      if (filter.isMentions()) return testMentions(query, object, filter);
      if (filter.isIdentifiable()) return testIdentifiable(query, object, filter);
      if (filter.isQuery()) return testQuery(query, object, filter);
      throw new UnsupportedOperationException("Special filter is not implemented yet :/ " + filter);
    }
    Object value = getValue(query, object, filter);
    if (!(value instanceof Collection<?> collection)) return filter.getOperator().test(value);
    return collection.stream().anyMatch(item -> filter.getOperator().test(item));
  }

  private <T> boolean testMentions(Query query, T object, Restriction filter) {
    Operator<?> op = filter.getOperator();
    return test(query, object, in("mentions.username", op.getArgs()))
        || test(query, object, in("comments.mentions.username", op.getArgs()));
  }

  private <T> boolean testIdentifiable(Query query, T object, Restriction filter) {
    Operator<?> op = filter.getOperator();
    return test(query, object, new Restriction("id", op))
        || test(query, object, new Restriction("code", op))
        || test(query, object, new Restriction("name", op))
        || query.getSchema().hasPersistedProperty("shortName")
            && test(query, object, new Restriction("shortName", op));
  }

  private <T> boolean testQuery(Query query, T object, Restriction restriction) {
    String value = (String) restriction.getOperator().getArgs().get(0);
    return test(query, object, eq("id", value))
        || test(query, object, eq("code", value))
        || test(query, object, ilike("name", value, MatchMode.ANYWHERE));
  }

  private Object getValue(Query query, Object object, Restriction filter) {
    String path = filter.getPath();
    String[] paths = path.split("\\.");
    Schema currentSchema = query.getSchema();

    if (path.contains("access") && query.getSchema().isIdentifiableObject()) {
      ((BaseIdentifiableObject) object)
          .setAccess(
              aclService.getAccess((IdentifiableObject) object, query.getCurrentUserDetails()));
    }

    for (int i = 0; i < paths.length; i++) {
      Property property = currentSchema.getProperty(paths[i]);

      if (property == null) {
        if (i == paths.length - 1 && filter.isAttribute()) {
          return ((BaseIdentifiableObject) object).getAttributeValues().get(paths[i]);
        }
        throw new QueryException("No property found for path " + path);
      }

      if (property.isCollection()) {
        currentSchema = schemaService.getDynamicSchema(property.getItemKlass());
      } else {
        currentSchema = schemaService.getDynamicSchema(property.getKlass());
      }

      object = collect(object, property);

      if (path.contains("access") && property.isIdentifiableObject()) {
        if (property.isCollection()) {
          for (Object item : ((Collection<?>) object)) {
            ((BaseIdentifiableObject) item)
                .setAccess(
                    aclService.getAccess((IdentifiableObject) item, query.getCurrentUserDetails()));
          }
        } else {
          ((BaseIdentifiableObject) object)
              .setAccess(
                  aclService.getAccess((IdentifiableObject) object, query.getCurrentUserDetails()));
        }
      }

      if (i == (paths.length - 1)) {
        if (property.isCollection()) {
          return Lists.newArrayList(object);
        }

        return object;
      }
    }

    throw new QueryException("No values found for path " + path);
  }

  @SuppressWarnings({"rawtypes"})
  private Object collect(Object object, Property property) {
    object = HibernateProxyUtils.unproxy(object);

    if (object instanceof Collection) {
      Collection<?> collection = (Collection<?>) object;
      List<Object> items = new ArrayList<>();

      for (Object item : collection) {
        Object collect = collect(item, property);

        if (collect instanceof Collection) {
          items.addAll(((Collection) collect));
        } else {
          items.add(collect);
        }
      }

      return items;
    }

    return ReflectionUtils.invokeMethod(object, property.getGetterMethod());
  }
}
