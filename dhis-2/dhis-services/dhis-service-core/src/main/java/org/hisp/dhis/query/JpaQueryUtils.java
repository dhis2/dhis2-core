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

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.schema.PropertyType.EMAIL;
import static org.hisp.dhis.schema.PropertyType.TEXT;
import static org.hisp.dhis.schema.PropertyType.USERNAME;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class JpaQueryUtils {
  public static final String HIBERNATE_CACHEABLE_HINT = "org.hibernate.cacheable";

  public static Function<Root<?>, Order> getOrders(CriteriaBuilder builder, String field) {
    Function<Root<?>, Order> order = root -> builder.asc(root.get(field));

    return order;
  }

  /**
   * Generate a String comparison Predicate base on input parameters.
   *
   * <p>Example: JpaUtils.stringPredicateCaseSensitive( builder, root.get( "name" ),key ,
   * JpaUtils.StringSearchMode.ANYWHERE ) )
   *
   * @param builder CriteriaBuilder
   * @param expressionPath Property Path for query
   * @param objectValue Value to check
   * @param searchMode JpaQueryUtils.StringSearchMode
   * @return a {@link Predicate}.
   */
  public static Predicate stringPredicateCaseSensitive(
      CriteriaBuilder builder,
      Expression<String> expressionPath,
      Object objectValue,
      StringSearchMode searchMode) {
    return stringPredicate(builder, expressionPath, objectValue, searchMode, true);
  }

  /**
   * Generate a String comparison Predicate base on input parameters.
   *
   * <p>Example: JpaUtils.stringPredicateIgnoreCase( builder, root.get( "name" ),key ,
   * JpaUtils.StringSearchMode.ANYWHERE ) )
   *
   * @param builder CriteriaBuilder
   * @param expressionPath Property Path for query
   * @param objectValue Value to check
   * @param searchMode JpaQueryUtils.StringSearchMode
   * @return a {@link Predicate}.
   */
  public static Predicate stringPredicateIgnoreCase(
      CriteriaBuilder builder,
      Expression<String> expressionPath,
      Object objectValue,
      StringSearchMode searchMode) {
    return stringPredicate(builder, expressionPath, objectValue, searchMode, false);
  }

  /**
   * Generate a String comparison Predicate base on input parameters.
   *
   * <p>Example: JpaUtils.stringPredicate( builder, root.get( "name" ), "%" + key + "%",
   * JpaUtils.StringSearchMode.LIKE, false ) )
   *
   * @param builder CriteriaBuilder
   * @param expressionPath Property Path for query
   * @param objectValue Value to check
   * @param searchMode JpaQueryUtils.StringSearchMode
   * @param caseSesnitive is case sensitive
   * @return a {@link Predicate}.
   */
  public static Predicate stringPredicate(
      CriteriaBuilder builder,
      Expression<String> expressionPath,
      Object objectValue,
      StringSearchMode searchMode,
      boolean caseSesnitive) {
    Expression<String> path = expressionPath;
    Object attrValue = objectValue;

    if (!caseSesnitive) {
      path = builder.lower(path);
      attrValue = ((String) attrValue).toLowerCase(LocaleContextHolder.getLocale());
    }

    switch (searchMode) {
      case EQUALS:
        return builder.equal(path, attrValue);
      case NOT_EQUALS:
        return builder.notEqual(path, attrValue);
      case ENDING_LIKE:
        return builder.like(path, "%" + attrValue);
      case NOT_ENDING_LIKE:
        return builder.notLike(path, "%" + attrValue);
      case STARTING_LIKE:
        return builder.like(path, attrValue + "%");
      case NOT_STARTING_LIKE:
        return builder.notLike(path, attrValue + "%");
      case ANYWHERE:
        return builder.like(path, "%" + attrValue + "%");
      case NOT_ANYWHERE:
        return builder.notLike(path, "%" + attrValue + "%");
      case LIKE:
        // Assumes user provides wildcards
        return builder.like(path, (String) attrValue);
      case NOT_LIKE:
        return builder.notLike(path, (String) attrValue);
      default:
        throw new IllegalStateException("expecting a search mode!");
    }
  }

  /** Use for generating search String predicate in JPA criteria query */
  public enum StringSearchMode {
    EQUALS("eq"), // Match exactly
    NOT_EQUALS("neq"),
    ANYWHERE("any"), // Like search with '%' prefix and suffix
    NOT_ANYWHERE("nany"), // Like search with '%' prefix and suffix
    STARTING_LIKE("sl"), // Like search and add '%' prefix first
    NOT_STARTING_LIKE("nsl"),
    LIKE("li"), // User provides the wild card
    NOT_LIKE("nli"), // User provides the wild card
    ENDING_LIKE("el"), // LIKE search and add a '%' suffix first
    NOT_ENDING_LIKE("nel");

    private final String code;

    StringSearchMode(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }

    public static final StringSearchMode convert(String code) {
      for (StringSearchMode searchMode : StringSearchMode.values()) {
        if (searchMode.getCode().equals(code)) {
          return searchMode;
        }
      }

      return EQUALS;
    }
  }

  /** Use for parsing filter parameter for Object which doesn't extend IdentifiableObject. */
  public static Predicate getPredicate(
      CriteriaBuilder builder, Property property, Path<?> path, String operator, String value) {
    switch (operator) {
      case "in":
        return path.in(
            (Collection<?>) QueryUtils.parseValue(Collection.class, property.getKlass(), value));
      case "eq":
        return builder.equal(
            path, property.getKlass().cast(QueryUtils.parseValue(property.getKlass(), value)));
      default:
        throw new QueryParserException("Query operator is not supported : " + operator);
    }
  }

  /**
   * Generate JPA Predicate for checking User Access for given User Uid and access string
   *
   * @param builder
   * @param userUid User Uid
   * @param access Access string for checking
   * @param <T>
   * @return JPA Predicate
   */
  public static <T> Function<Root<T>, Predicate> checkUserAccess(
      CriteriaBuilder builder, String userUid, String access) {
    return root ->
        builder.and(
            builder.equal(
                builder.function(
                    JsonbFunctions.HAS_USER_ID,
                    Boolean.class,
                    root.get("sharing"),
                    builder.literal(userUid)),
                true),
            builder.equal(
                builder.function(
                    JsonbFunctions.CHECK_USER_ACCESS,
                    Boolean.class,
                    root.get("sharing"),
                    builder.literal(userUid),
                    builder.literal(access)),
                true));
  }

  /**
   * Generate Predicate for checking Access for given Set of UserGroup Id and access string Return
   * NULL if given Set of UserGroup is empty
   *
   * @param builder
   * @param userGroupIds List of User Group Uids
   * @param access Access String
   * @return JPA Predicate
   */
  public static <T> Function<Root<T>, Predicate> checkUserGroupsAccess(
      CriteriaBuilder builder, Collection<String> userGroupIds, String access) {
    return root -> {
      if (CollectionUtils.isEmpty(userGroupIds)) {
        return null;
      }

      String groupUuIds = "{" + String.join(",", userGroupIds) + "}";

      return builder.and(
          builder.equal(
              builder.function(
                  JsonbFunctions.HAS_USER_GROUP_IDS,
                  Boolean.class,
                  root.get("sharing"),
                  builder.literal(groupUuIds)),
              true),
          builder.equal(
              builder.function(
                  JsonbFunctions.CHECK_USER_GROUPS_ACCESS,
                  Boolean.class,
                  root.get("sharing"),
                  builder.literal(access),
                  builder.literal(groupUuIds)),
              true));
    };
  }

  /**
   * Return SQL query for checking sharing access for given user
   *
   * @param sharingColumn sharing column reference
   * @param user User for sharing checking
   * @param access The sharing access string for checking. Refer to {@link
   *     org.hisp.dhis.security.acl.AccessStringHelper}
   * @return SQL query
   */
  public static String generateSQlQueryForSharingCheck(
      String sharingColumn, UserDetails user, String access) {
    return generateSQlQueryForSharingCheck(
        sharingColumn, access, user.getUid(), getGroupsIds(user));
  }

  public static String generateSQlQueryForSharingCheck(
      String sharingColumn, String userId, List<String> userGroupIds, String access) {
    return generateSQlQueryForSharingCheck(
        sharingColumn, access, userId, getGroupsIds(userGroupIds));
  }

  private static String generateSQlQueryForSharingCheck(
      String sharingColumn, String access, String userId, String groupsIds) {
    return String.format(
        generateSQlQueryForSharingCheck(groupsIds), sharingColumn, userId, groupsIds, access);
  }

  private static String generateSQlQueryForSharingCheck(String groupsIds) {
    return " ( %1$s->>'owner' is null or %1$s->>'owner' = '%2$s') "
        + " or %1$s->>'public' like '%4$s' or %1$s->>'public' is null "
        + " or ("
        + JsonbFunctions.HAS_USER_ID
        + "( %1$s, '%2$s') = true "
        + " and "
        + JsonbFunctions.CHECK_USER_ACCESS
        + "( %1$s, '%2$s', '%4$s' ) = true )  "
        + (StringUtils.isEmpty(groupsIds)
            ? ""
            : " or ( "
                + JsonbFunctions.HAS_USER_GROUP_IDS
                + "( %1$s, '%3$s') = true "
                + " and "
                + JsonbFunctions.CHECK_USER_GROUPS_ACCESS
                + "( %1$s, '%4$s', '%3$s') = true )");
  }

  public static String generateHqlQueryForSharingCheck(
      String tableAlias, UserDetails user, String access) {
    if (user.isSuper() || user.isAuthorized("Test_skipSharingCheck")) {
      return "1=1";
    }

    return "("
        + sqlToHql(
            tableAlias, generateSQlQueryForSharingCheck(tableAlias + ".sharing", user, access))
        + ")";
  }

  public static String generateHqlQueryForSharingCheck(
      String tableName, String access, String userId, Collection<String> userGroupIds) {
    return "("
        + sqlToHql(
            tableName,
            generateSQlQueryForSharingCheck(
                tableName + ".sharing", access, userId, getGroupsIds(userGroupIds)))
        + ")";
  }

  public static boolean isPropertyTypeText(Property property) {
    return List.of(TEXT, EMAIL, USERNAME).contains(property.getPropertyType());
  }

  private static String getGroupsIds(UserDetails user) {
    return getGroupsIds(user.getUserGroupIds());
  }

  private static String getGroupsIds(Collection<String> userGroupIds) {
    return CollectionUtils.isEmpty(userGroupIds)
        ? null
        : "{" + userGroupIds.stream().sorted().collect(joining(",")) + "}";
  }

  private static String sqlToHql(String tableName, String sql) {
    // HQL does not allow the ->> syntax so we have to substitute with the
    // named function: jsonb_extract_path_text
    return sql.replaceAll(
        tableName + "\\.sharing->>'([^']+)'",
        JsonbFunctions.EXTRACT_PATH_TEXT + "(" + tableName + ".sharing, '$1')");
  }
}
