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

import static org.hisp.dhis.query.Filters.eq;
import static org.hisp.dhis.query.Filters.ilike;
import static org.hisp.dhis.query.Filters.in;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
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
  public <T extends IdentifiableObject> List<T> query(Query<T> query) {
    validateQuery(query);
    List<T> list = runQuery(query);
    list = runSorter(query, list);

    return query.isSkipPaging()
        ? list
        : PagerUtils.pageCollection(list, query.getFirstResult(), query.getMaxResults());
  }

  @Override
  public <T extends IdentifiableObject> long count(Query<T> query) {
    validateQuery(query);
    List<T> list = runQuery(query);
    return list.size();
  }

  private void validateQuery(Query<?> query) {
    if (query.getCurrentUserDetails() == null) {
      query.setCurrentUserDetails(CurrentUserUtil.getCurrentUserDetails());
    }

    if (query.getObjectType() == null) {
      throw new QueryException("Invalid Query object, does not contain an object type");
    }

    if (query.getObjects() == null) {
      throw new QueryException("InMemoryQueryEngine requires an existing object list to work on.");
    }
  }

  private <T extends IdentifiableObject> List<T> runQuery(Query<T> query) {
    List<Matcher> matchers = query.getFilters().stream().map(f -> matcherOf(query, f)).toList();
    return query.getObjects().stream().filter(object -> matches(query, object, matchers)).toList();
  }

  private <T extends IdentifiableObject> List<T> runSorter(Query<T> query, List<T> objects) {
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

  private <T extends IdentifiableObject> boolean matches(
      Query<T> query, T object, List<Matcher> matchers) {
    if (query.getRootJunctionType() == Junction.Type.OR) {
      // OR
      for (Matcher matcher : matchers) if (matches(query, object, matcher)) return true;
      return false;
    }
    // AND
    for (Matcher matcher : matchers) if (!matches(query, object, matcher)) return false;
    return true;
  }

  /**
   * The main purpose of matchers is to "cache" the knowledge of how to extract a value for a filter
   * test from an object of the listed object type.
   *
   * @param filter the filter that is applied
   * @param value a function to extract the filter property value from an object
   */
  private record Matcher(Filter filter, Function<IdentifiableObject, Object> value) {}

  private <T extends IdentifiableObject> boolean matches(
      Query<T> query, T object, Matcher matcher) {
    Filter filter = matcher.filter;
    if (filter.isVirtual()) {
      if (filter.isMentions()) return matchesMentions(query, object, matcher);
      if (filter.isIdentifiable()) return matchesIdentifiable(query, object, matcher);
      if (filter.isQuery()) return matchesQuery(query, object, matcher);
      throw new UnsupportedOperationException("Special filter is not implemented yet :/ " + filter);
    }
    Object value = matcher.value.apply(object);
    if (!(value instanceof Collection<?> c)) return filter.getOperator().test(value);
    return c.stream().anyMatch(item -> filter.getOperator().test(item));
  }

  private <T extends IdentifiableObject> boolean matchesMentions(
      Query<T> query, T object, Matcher matcher) {
    Operator<?> op = matcher.filter.getOperator();
    return matches(query, object, matcherOf(query, in("mentions.username", op.getArgs())))
        || matches(query, object, matcherOf(query, in("comments.mentions.username", op.getArgs())));
  }

  private <T extends IdentifiableObject> boolean matchesIdentifiable(
      Query<T> query, T object, Matcher matcher) {
    Operator<?> op = matcher.filter.getOperator();
    return matches(query, object, matcherOf(query, new Filter("id", op)))
        || matches(query, object, matcherOf(query, new Filter("code", op)))
        || matches(query, object, matcherOf(query, new Filter("name", op)))
        || query.isShortNamePersisted()
            && matches(query, object, matcherOf(query, new Filter("shortName", op)));
  }

  private <T extends IdentifiableObject> boolean matchesQuery(
      Query<T> query, T object, Matcher matcher) {
    String value = (String) matcher.filter.getOperator().getArgs().get(0);
    return matches(query, object, matcherOf(query, eq("id", value)))
        || matches(query, object, matcherOf(query, eq("code", value)))
        || matches(query, object, matcherOf(query, ilike("name", value, MatchMode.ANYWHERE)));
  }

  private Matcher matcherOf(Query<?> query, Filter filter) {
    return new Matcher(filter, obj -> getValue(query, obj, filter));
  }

  private Object getValue(Query<?> query, Object object, Filter filter) {
    String path = filter.getPath();
    String[] paths = path.split("\\.");
    Schema currentSchema = schemaService.getDynamicSchema(query.getObjectType());

    if (path.contains("access")) {
      ((IdentifiableObject) object)
          .setAccess(
              aclService.getAccess((IdentifiableObject) object, query.getCurrentUserDetails()));
    }

    for (int i = 0; i < paths.length; i++) {
      Property property = currentSchema.getProperty(paths[i]);

      if (property == null) {
        if (i == paths.length - 1 && filter.isAttribute()) {
          return ((IdentifiableObject) object).getAttributeValues().get(paths[i]);
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
            ((IdentifiableObject) item)
                .setAccess(
                    aclService.getAccess((IdentifiableObject) item, query.getCurrentUserDetails()));
          }
        } else {
          ((IdentifiableObject) object)
              .setAccess(
                  aclService.getAccess((IdentifiableObject) object, query.getCurrentUserDetails()));
        }
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
