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
package org.hisp.dhis.query;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.query.QueryUtils.parseValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultQueryParser implements QueryParser {

  private final SchemaService schemaService;

  @Override
  public <T extends IdentifiableObject> Query<T> parse(
      Class<T> objectType, @Nonnull List<String> filters) throws QueryParserException {
    return parse(objectType, filters, Junction.Type.AND);
  }

  @Override
  public <T extends IdentifiableObject> Query<T> parse(
      Class<T> objectType, @Nonnull List<String> filters, Junction.Type rootJunction)
      throws QueryParserException {
    Schema schema = schemaService.getDynamicSchema(objectType);
    Query<T> query = Query.of(objectType, rootJunction);

    List<String> mentions = new ArrayList<>();
    for (String filter : filters) {
      String[] parts = rewriteFilter(filter).split(":");

      if (parts.length < 2) {
        throw new QueryParserException("Invalid filter => " + filter);
      }

      String path = parts[0];
      String operator = parts[1];
      if (parts.length >= 3) {
        String arg = parts.length == 3 ? parts[2] : Stream.of(parts).skip(2).collect(joining(":"));
        if ("mentions".equals(path) && "in".equals(operator)) {
          mentions.add(arg);
        } else {
          query.add(createFilter(schema, path, operator, arg));
        }
      } else {
        query.add(createFilter(schema, path, operator, null));
      }
      if (!mentions.isEmpty()) {
        query.add(createMensionsFilter(mentions));
      }
    }
    return query;
  }

  private static String rewriteFilter(String filter) {
    if (filter.startsWith("attributeValues.attribute.id:eq:"))
      return filter.substring(filter.lastIndexOf(':') + 1) + ":!null";
    return filter;
  }

  private Filter createFilter(Schema schema, String path, String operator, Object arg)
      throws QueryParserException {
    if ("identifiable".equals(path)) return createFilter(null, String.class, path, operator, arg);

    Property property = getProperty(schema, path);
    if (property == null) {
      if (!CodeGenerator.isValidUid(path.substring(path.indexOf('.') + 1))) {
        throw new QueryParserException("Unknown path property: " + path);
      }
      return createFilter(null, String.class, path, operator, arg).asAttribute();
    }
    return createFilter(property, property.getKlass(), path, operator, arg);
  }

  @SuppressWarnings("unchecked")
  private Filter createFilter(
      Property property, Class<?> valueType, String path, String operator, Object arg)
      throws QueryParserException {

    return switch (operator) {
      case "eq" -> Filters.eq(path, parseValue(valueType, arg));
      case "ieq" -> Filters.ilike(path, parseValue(valueType, arg), MatchMode.EXACT);
      case "!eq", "neq", "ne" -> Filters.ne(path, parseValue(valueType, arg));
      case "gt" -> Filters.gt(path, parseValue(valueType, arg));
      case "lt" -> Filters.lt(path, parseValue(valueType, arg));
      case "gte", "ge" -> Filters.ge(path, parseValue(valueType, arg));
      case "lte", "le" -> Filters.le(path, parseValue(valueType, arg));
      case "like" -> Filters.like(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "!like" -> Filters.notLike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "$like" -> Filters.like(path, parseValue(valueType, arg), MatchMode.START);
      case "!$like" -> Filters.notLike(path, parseValue(valueType, arg), MatchMode.START);
      case "like$" -> Filters.like(path, parseValue(valueType, arg), MatchMode.END);
      case "!like$" -> Filters.notLike(path, parseValue(valueType, arg), MatchMode.END);
      case "ilike" -> Filters.ilike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "!ilike" -> Filters.notIlike(path, parseValue(valueType, arg), MatchMode.ANYWHERE);
      case "startsWith", "$ilike" ->
          Filters.ilike(path, parseValue(valueType, arg), MatchMode.START);
      case "!$ilike" -> Filters.notIlike(path, parseValue(valueType, arg), MatchMode.START);
      case "token" -> Filters.token(path, parseValue(valueType, arg), MatchMode.START);
      case "!token" -> Filters.notToken(path, parseValue(valueType, arg), MatchMode.START);
      case "endsWith", "ilike$" -> Filters.ilike(path, parseValue(valueType, arg), MatchMode.END);
      case "!ilike$" -> Filters.notIlike(path, parseValue(valueType, arg), MatchMode.END);
      case "in" -> Filters.in(path, parseValues(property, valueType, arg));
      case "!in" -> Filters.notIn(path, parseValues(property, valueType, arg));
      case "null" -> Filters.isNull(path);
      case "!null" -> Filters.isNotNull(path);
      case "empty" -> Filters.isEmpty(path);
      case "!empty" -> Filters.isNotEmpty(path);
      default -> throw new QueryParserException("`" + operator + "` is not a valid operator.");
    };
  }

  @Nonnull
  @SuppressWarnings("rawtypes")
  private Collection parseValues(Property property, Class<?> valueType, Object arg) {
    Collection<?> values =
        property.isCollection()
            ? parseValue(Collection.class, property.getItemKlass(), arg)
            : parseValue(Collection.class, valueType, arg);

    if (values == null || values.isEmpty()) {
      throw new QueryParserException("Invalid argument `" + arg + "` for in operator.");
    }
    return values;
  }

  private Filter createMensionsFilter(List<String> mentions) {
    List<String> items = new ArrayList<>();
    for (String m : mentions) items.addAll(asList(m.substring(1, m.length() - 1).split(",")));
    return Filters.in("mentions", items);
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
