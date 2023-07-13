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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.query.QueryUtils.parseValue;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.query.QueryParser")
public class DefaultJpaQueryParser implements QueryParser {
  private static final String IDENTIFIABLE = "identifiable";

  private final SchemaService schemaService;

  public DefaultJpaQueryParser(SchemaService schemaService) {
    checkNotNull(schemaService);

    this.schemaService = schemaService;
  }

  @Override
  public Query parse(Class<?> klass, List<String> filters) throws QueryParserException {
    return parse(klass, filters, Junction.Type.AND);
  }

  @Override
  public Query parse(Class<?> klass, List<String> filters, Junction.Type rootJunction)
      throws QueryParserException {
    Schema schema = schemaService.getDynamicSchema(klass);
    Query query = Query.from(schema, rootJunction);

    for (String filter : filters) {
      String[] split = filter.split(":");

      if (split.length < 2) {
        throw new QueryParserException("Invalid filter => " + filter);
      }

      if (split.length >= 3) {
        int index = split[0].length() + ":".length() + split[1].length() + ":".length();

        if (split[0].equals(IDENTIFIABLE) && !schema.hasProperty(IDENTIFIABLE)) {
          handleIdentifiablePath(schema, split[1], filter.substring(index), query.addDisjunction());
        } else {
          query.add(getRestriction(schema, split[0], split[1], filter.substring(index)));
        }
      } else {
        query.add(getRestriction(schema, split[0], split[1], null));
      }
    }
    return query;
  }

  private void handleIdentifiablePath(
      Schema schema, String operator, Object arg, Disjunction disjunction) {
    disjunction.add(getRestriction(schema, "id", operator, arg));
    disjunction.add(getRestriction(schema, "code", operator, arg));
    disjunction.add(getRestriction(schema, "name", operator, arg));

    if (schema.hasPersistedProperty("shortName")) {
      disjunction.add(getRestriction(schema, "shortName", operator, arg));
    }
  }

  private Restriction getRestriction(Schema schema, String path, String operator, Object arg)
      throws QueryParserException {
    Property property = getProperty(schema, path);

    if (property == null) {
      if (!CodeGenerator.isValidUid(path.substring(path.indexOf('.') + 1))) {
        throw new QueryParserException("Unknown path property: " + path);
      }
      return getRestriction(null, String.class, path, operator, arg).asAttribute();
    }
    return getRestriction(property, property.getKlass(), path, operator, arg);
  }

  @SuppressWarnings("unchecked")
  private Restriction getRestriction(
      Property property, Class<?> valueType, String path, String operator, Object arg)
      throws QueryParserException {

    return switch (operator) {
      case "eq" -> Restrictions.eq(path, parseValue(valueType, arg));
      case "ieq" -> Restrictions.ilike(path, parseValue(valueType, arg), MatchMode.EXACT);
      case "!eq", "neq", "ne" -> Restrictions.ne(path, parseValue(valueType, arg));
      case "gt" -> Restrictions.gt(path, parseValue(valueType, arg));
      case "lt" -> Restrictions.lt(path, parseValue(valueType, arg));
      case "gte", "ge" -> Restrictions.ge(path, parseValue(valueType, arg));
      case "lte", "le" -> Restrictions.le(path, parseValue(valueType, arg));
      case "like" -> Restrictions.like(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "!like" -> Restrictions.notLike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "$like" -> Restrictions.like(path, parseValue(valueType, arg), MatchMode.START);
      case "!$like" -> Restrictions.notLike(path, parseValue(valueType, arg), MatchMode.START);
      case "like$" -> Restrictions.like(path, parseValue(valueType, arg), MatchMode.END);
      case "!like$" -> Restrictions.notLike(path, parseValue(valueType, arg), MatchMode.END);
      case "ilike" -> Restrictions.ilike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "!ilike" -> Restrictions.notIlike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "startsWith", "$ilike" -> Restrictions.ilike(
          path, parseValue(valueType, arg), MatchMode.START);
      case "!$ilike" -> Restrictions.notIlike(path, parseValue(valueType, arg), MatchMode.START);
      case "token" -> Restrictions.token(path, parseValue(valueType, arg), MatchMode.START);
      case "!token" -> Restrictions.notToken(path, parseValue(valueType, arg), MatchMode.START);
      case "endsWith", "ilike$" -> Restrictions.ilike(
          path, parseValue(valueType, arg), MatchMode.END);
      case "!ilike$" -> Restrictions.notIlike(path, parseValue(valueType, arg), MatchMode.END);
      case "in" -> {
        Collection values = null;

        if (property.isCollection()) {
          values = parseValue(Collection.class, property.getItemKlass(), arg);
        } else {
          values = parseValue(Collection.class, valueType, arg);
        }

        if (values == null || values.isEmpty()) {
          throw new QueryParserException("Invalid argument `" + arg + "` for in operator.");
        }

        yield Restrictions.in(path, values);
      }
      case "!in" -> {
        Collection values = null;

        if (property.isCollection()) {
          values = parseValue(Collection.class, property.getItemKlass(), arg);
        } else {
          values = parseValue(Collection.class, valueType, arg);
        }

        if (values == null || values.isEmpty()) {
          throw new QueryParserException("Invalid argument `" + arg + "` for in operator.");
        }

        yield Restrictions.notIn(path, values);
      }
      case "null" -> Restrictions.isNull(path);
      case "!null" -> Restrictions.isNotNull(path);
      case "empty" -> Restrictions.isEmpty(path);
      default -> throw new QueryParserException("`" + operator + "` is not a valid operator.");
    };
  }

  @Override
  public Property getProperty(Schema schema, String path) throws QueryParserException {
    String[] paths = path.split("\\.");
    Schema currentSchema = schema;
    Property currentProperty = null;

    for (int i = 0; i < paths.length; i++) {
      if (!currentSchema.hasProperty(paths[i])) {
        return null;
      }

      currentProperty = currentSchema.getProperty(paths[i]);

      if (currentProperty == null) {
        throw new QueryParserException("Unknown path property: " + paths[i] + " (" + path + ")");
      }

      if ((currentProperty.isSimple() && !currentProperty.isCollection())
          && i != (paths.length - 1)) {
        throw new QueryParserException(
            "Simple type was found before finished parsing path expression, please check your path string.");
      }

      if (currentProperty.isCollection()) {
        currentSchema = schemaService.getDynamicSchema(currentProperty.getItemKlass());
      } else {
        currentSchema = schemaService.getDynamicSchema(currentProperty.getKlass());
      }
    }

    return currentProperty;
  }
}
