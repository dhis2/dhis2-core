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
  public Query parse(Class<?> klass, @Nonnull List<String> filters) throws QueryParserException {
    return parse(klass, filters, Junction.Type.AND);
  }

  @Override
  public Query parse(Class<?> klass, @Nonnull List<String> filters, Junction.Type rootJunction)
      throws QueryParserException {
    Schema schema = schemaService.getDynamicSchema(klass);
    Query query = Query.from(schema, rootJunction);

    List<String> mentions = new ArrayList<>();
    for (String filter : filters) {
      String[] split = rewriteFilter(filter).split(":");

      if (split.length < 2) {
        throw new QueryParserException("Invalid filter => " + filter);
      }

      String path = split[0];
      String operator = split[1];
      if (split.length >= 3) {
        String arg = split.length == 3 ? split[2] : Stream.of(split).skip(2).collect(joining(":"));
        if ("mentions".equals(path) && "in".equals(operator)) {
          mentions.add(arg);
        } else {
          query.add(getRestriction(schema, path, operator, arg));
        }
      } else {
        query.add(getRestriction(schema, path, operator, null));
      }
      if (!mentions.isEmpty()) {
        query.add(getDisjunctionsFromCustomMentions(mentions, query.getSchema()));
      }
    }
    return query;
  }

  private static String rewriteFilter(String filter) {
    if (filter.startsWith("attributeValues.attribute.id:eq:"))
      return filter.substring(filter.lastIndexOf(':') + 1) + ":!null";
    return filter;
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
      case "startsWith", "$ilike" ->
          Restrictions.ilike(path, parseValue(valueType, arg), MatchMode.START);
      case "!$ilike" -> Restrictions.notIlike(path, parseValue(valueType, arg), MatchMode.START);
      case "token" -> Restrictions.token(path, parseValue(valueType, arg), MatchMode.START);
      case "!token" -> Restrictions.notToken(path, parseValue(valueType, arg), MatchMode.START);
      case "endsWith", "ilike$" ->
          Restrictions.ilike(path, parseValue(valueType, arg), MatchMode.END);
      case "!ilike$" -> Restrictions.notIlike(path, parseValue(valueType, arg), MatchMode.END);
      case "in" -> Restrictions.in(path, parseValues(property, valueType, arg));
      case "!in" -> Restrictions.notIn(path, parseValues(property, valueType, arg));
      case "null" -> Restrictions.isNull(path);
      case "!null" -> Restrictions.isNotNull(path);
      case "empty" -> Restrictions.isEmpty(path);
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

  private Restriction getDisjunctionsFromCustomMentions(List<String> mentions, Schema schema) {
    List<String> items = new ArrayList<>();
    for (String m : mentions) items.addAll(asList(m.substring(1, m.length() - 1).split(",")));
    return Restrictions.in("mentions", items);
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
