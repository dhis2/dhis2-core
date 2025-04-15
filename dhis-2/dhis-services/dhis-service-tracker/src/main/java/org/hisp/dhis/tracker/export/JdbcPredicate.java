/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.QueryFilter.affixLikeWildcards;
import static org.hisp.dhis.system.util.SqlUtils.escapeLikeWildcards;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueType.SqlType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * JdbcPredicate turns a {@link QueryFilter} on a data element or tracked entity attribute into a
 * SQL predicate with an optional JDBC parameter (right-hand side operand of the SQL operator). The
 * {@link #getSql()} can be inserted into a SQL statement where clause. User input in the {@link
 * QueryFilter#getFilter()} is transformed into the {@link #getParameter()}. Non-unary operators
 * will have a parameter which must be added to the JDBC template parameter source like {@link
 * MapSqlParameterSource}!
 *
 * <p>These are some of the transformations we need to do on user input
 *
 * <ul>
 *   <li>split input {@code "one;two"} for operator {@link QueryOperator#IN}
 *   <li>convert input value(s) from text to other data types so comparisons can be done using DHIS2
 *       data value type semantics i.e. {@code 170.0 == 170} which is what users expect for value
 *       type integer/number while {@code "170.0" != "170"} is unexpected but what happens for
 *       treating the values as text
 *   <li>escape wildcard characters '%' and '_' for operators backed by SQL {@code like}
 *   <li>insert '%' wildcard(s) before/after user input for like based operators
 * </ul>
 *
 * Other special characters like single quotes are expected to be escaped by the <a
 * href="https://github.com/pgjdbc/pgjdbc/blob/156d724e1d95052b41a19fb568b2f81919ae2197/pgjdbc/src/main/java/org/postgresql/core/v3/SimpleParameterList.java#L213">JDBC
 * driver</a>!
 *
 * <p>On {@link SqlParameterValue}: Spring handles {@code List.of()} values for us no matter if we
 * pass it to an <a
 * href="https://docs.spring.io/spring-framework/reference/data-access/jdbc/parameter-handling.html#jdbc-in-clause">in</a>
 * operator allowing multiple values or an operator only expecting one like {@code eq}.
 * JdbcPredicate ensures we never pass a collection with multiple elements to such operators as this
 * leads to a {@code BadSqlGrammarException}.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class JdbcPredicate {
  String sql;
  Optional<Parameter> parameter;

  public record Parameter(String name, SqlParameterValue value) {}

  /**
   * The SQL generated for tracked entity attributes expects a table alias of the tracked entity
   * attribute {@code UID} to access the attribute value using {@code 'uid'.value}.
   */
  public static JdbcPredicate of(@Nonnull TrackedEntityAttribute tea, @Nonnull QueryFilter filter) {
    Parameter parameter = parseFilterValue(tea, filter);
    String sql = generateSql(tea, filter, parameter);
    return new JdbcPredicate(sql, Optional.ofNullable(parameter));
  }

  public static JdbcPredicate of(
      @Nonnull DataElement de, @Nonnull QueryFilter filter, @Nonnull String tableName) {
    Parameter parameter = parseFilterValue(de, filter);
    String sql = generateSql(de, filter, parameter, tableName);
    return new JdbcPredicate(sql, Optional.ofNullable(parameter));
  }

  @SuppressWarnings("unchecked")
  private static Parameter parseFilterValue(
      ValueTypedDimensionalItemObject valueTypedObject, QueryFilter filter) {
    QueryOperator operator = filter.getOperator();
    if (operator.isUnary()) {
      return null;
    }

    // pre-process values
    // so far all DHIS2 operators are implemented using case-insensitive matching in tracker, so
    // ILIKE == LIKE, EQ == IEQ, ...
    String value = filter.getFilter().toLowerCase();
    List<String> values;
    if (operator.isIn()) {
      values = List.of(value.split(QueryFilter.OPTION_SEP));
    } else if (operator.isLikeBased()) {
      values = List.of(escapeLikeWildcards(value));
    } else {
      values = List.of(value);
    }

    SqlType<?> sqlType =
        operator.isCastOperand()
            ? valueTypedObject.getValueType().getSqlType()
            : ValueType.JAVA_TO_SQL_TYPES.get(String.class);
    Object convertedValue = convertValues(valueTypedObject, sqlType, values);

    // post-process
    if (operator.isLikeBased()) {
      convertedValue =
          ((List<String>) convertedValue)
              .stream().map(f -> affixLikeWildcards(operator, f)).toList();
    }

    String name = "filter_%s_%s".formatted(valueTypedObject.getUid(), UID.generate());
    return new Parameter(name, new SqlParameterValue(sqlType.type(), convertedValue));
  }

  private static Object convertValues(
      ValueTypedDimensionalItemObject valueTypeObject, SqlType<?> sqlType, List<String> values) {
    return values.stream()
        .map(
            value -> {
              try {
                return sqlType.producer().apply(value);
              } catch (Exception e) {
                String name =
                    valueTypeObject instanceof TrackedEntityAttribute
                        ? "attribute"
                        : "data element";
                throw new IllegalArgumentException(
                    String.format(
                        "Filter for %s %s is invalid. The %s value type is %s but the value `%s` is not.",
                        name,
                        valueTypeObject.getUid(),
                        name,
                        valueTypeObject.getValueType().name(),
                        value));
              }
            })
        .toList();
  }

  private static String generateSql(
      TrackedEntityAttribute tea, QueryFilter filter, Parameter parameter) {
    String leftOperand = leftOperandSql(tea, filter.getOperator());
    String rightOperand = rightOperandSql(filter.getOperator(), parameter);
    return leftOperand + " " + filter.getSqlOperator() + rightOperand;
  }

  private static String generateSql(
      DataElement de, QueryFilter filter, Parameter parameter, String ev) {
    String leftOperand = leftOperandSql(de, filter.getOperator(), ev);
    String rightOperand = rightOperandSql(filter.getOperator(), parameter);
    return leftOperand + " " + filter.getSqlOperator() + rightOperand;
  }

  @Nonnull
  private static String leftOperandSql(TrackedEntityAttribute tea, QueryOperator operator) {
    String column = quote(tea.getUid()) + ".value";

    String leftOperand;
    if (operator.isCastOperand() && tea.getValueType().getSqlType().type() != Types.VARCHAR) {
      SqlType<?> sqlType = tea.getValueType().getSqlType();
      leftOperand = SqlUtils.cast(column, sqlType.postgresName());
    } else {
      // lower() is not necessarily needed for unary operators on TEAV but this will allow the DB to
      // use the index on lower(teav.value)
      // for binary operators this means ieq and eq behave the same way which is likely not desired
      // so this might have to change in the future
      leftOperand = lower(column);
    }
    return leftOperand;
  }

  @Nonnull
  private static String leftOperandSql(DataElement de, QueryOperator operator, String table) {
    String column = table + ".eventdatavalues #>> '{" + de.getUid() + ", value}'";

    String leftOperand;
    if (operator.isUnary()) {
      // `ev.eventdatavalues->'vANAXwtLwcT'` is not null is different from `lower(ev.eventdatavalues
      // #>> '{vANAXwtLwcT, value}') is not null`
      leftOperand = table + ".eventdatavalues -> '" + de.getUid() + "'";
    } else if (operator.isCastOperand() && de.getValueType().getSqlType().type() != Types.VARCHAR) {
      SqlType<?> sqlType = de.getValueType().getSqlType();
      leftOperand = SqlUtils.cast(column, sqlType.postgresName());
    } else {
      // for binary operators this means ieq and eq behave the same way which is likely not desired
      // so this might have to change in the future
      leftOperand = lower(column);
    }
    return leftOperand;
  }

  @Nonnull
  private static String rightOperandSql(QueryOperator operator, Parameter parameter) {
    if (parameter == null) {
      return "";
    }

    String name = parameter.name();
    if (operator.isIn()) {
      return " (:" + name + ")";
    }

    return " :" + name;
  }

  /**
   * Map a list of predicates to a compound predicate adding any named parameters to the given
   * parameter source.
   *
   * <p>You need to make sure to prefix the resulting SQL with {@code and} or suffix with a space or
   * {@code and} if needed.
   */
  @Nonnull
  public static <T extends ValueTypedDimensionalItemObject> String mapPredicatesToSql(
      @Nonnull Map<T, List<JdbcPredicate>> predicates,
      @Nonnull MapSqlParameterSource sqlParameters) {
    boolean first = true;
    StringBuilder sql = new StringBuilder();
    for (List<JdbcPredicate> values : predicates.values()) {
      for (JdbcPredicate predicate : values) {
        if (first) {
          first = false;
        } else {
          sql.append(" and ");
        }
        sql.append(predicate.getSql());

        predicate
            .getParameter()
            .ifPresent(
                (Parameter parameter) ->
                    sqlParameters.addValue(parameter.name(), parameter.value()));
      }
    }

    return sql.toString();
  }
}
