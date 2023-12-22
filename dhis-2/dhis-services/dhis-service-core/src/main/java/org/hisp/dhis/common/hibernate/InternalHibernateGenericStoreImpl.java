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
package org.hisp.dhis.common.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.common.adapter.Sharing_;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.InternalHibernateGenericStore;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class contains methods for generating predicates which are used for validating sharing
 * access permission.
 */
@Slf4j
public class InternalHibernateGenericStoreImpl<T extends BaseIdentifiableObject>
    extends HibernateGenericStore<T> implements InternalHibernateGenericStore<T> {
  protected AclService aclService;

  public InternalHibernateGenericStoreImpl(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<T> clazz,
      AclService aclService,
      boolean cacheable) {
    super(entityManager, jdbcTemplate, publisher, clazz, cacheable);

    checkNotNull(aclService);
    this.aclService = aclService;
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicates(CriteriaBuilder builder) {
    return getSharingPredicates(builder, CurrentUserUtil.getCurrentUserDetails());
  }

  /**
   * Get Predicate for checking Sharing access for given User's uid and UserGroup Uids
   *
   * @param builder CriteriaBuilder
   * @param userUid User Uid for checking access
   * @param userGroupUids List of UserGroup Uid which given user belong to
   * @param access Access String for checking
   * @return List of {@link Predicate}
   */
  protected List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder, String userUid, Set<String> userGroupUids, String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    Function<Root<T>, Predicate> userGroupPredicate =
        JpaQueryUtils.checkUserGroupsAccess(builder, userGroupUids, access);

    Function<Root<T>, Predicate> userPredicate =
        JpaQueryUtils.checkUserAccess(builder, userUid, access);

    predicates.add(
        root -> {
          Predicate disjunction =
              builder.or(
                  builder.like(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC)),
                      access),
                  builder.equal(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC)),
                      "null"),
                  builder.isNull(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC))),
                  builder.isNull(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.OWNER))),
                  builder.equal(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.OWNER)),
                      "null"),
                  builder.equal(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.OWNER)),
                      userUid),
                  userPredicate.apply(root));

          Predicate ugPredicateWithRoot = userGroupPredicate.apply(root);

          if (ugPredicateWithRoot != null) {
            return builder.or(disjunction, ugPredicateWithRoot);
          }

          return disjunction;
        });

    return predicates;
  }

  /**
   * Get Predicate for checking Data Sharing access for given User's uid and UserGroup Uids
   *
   * @param builder CriteriaBuilder
   * @param userUid User Uid for checking access
   * @param userGroupUids List of UserGroup Uid which given user belong to
   * @param access Access String for checking
   * @return List of {@link Predicate}
   */
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, String userUid, Set<String> userGroupUids, String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    preProcessPredicates(builder, predicates);

    Function<Root<T>, Predicate> userGroupPredicate =
        JpaQueryUtils.checkUserGroupsAccess(builder, userGroupUids, access);

    Function<Root<T>, Predicate> userPredicate =
        JpaQueryUtils.checkUserAccess(builder, userUid, access);

    predicates.add(
        root -> {
          Predicate disjunction =
              builder.or(
                  builder.like(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC)),
                      access),
                  builder.equal(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC)),
                      "null"),
                  builder.isNull(
                      builder.function(
                          JsonbFunctions.EXTRACT_PATH_TEXT,
                          String.class,
                          root.get(BaseIdentifiableObject_.SHARING),
                          builder.literal(Sharing_.PUBLIC))),
                  userPredicate.apply(root));

          Predicate ugPredicateWithRoot = userGroupPredicate.apply(root);

          if (ugPredicateWithRoot != null) {
            return builder.or(disjunction, ugPredicateWithRoot);
          }

          return disjunction;
        });

    return predicates;
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails) {

    if (userDetails == null) {
      return List.of();
    }

    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());
    if (userDetails.getUserGroupIds().size() != currentUserGroupInfo.getUserGroupUIDs().size()) {
      String msg =
          String.format(
              "User '%s' getGroups().size() has %d groups, but  getUserGroupUIDs() returns %d groups!",
              userDetails.getUsername(),
              userDetails.getUserGroupIds().size(),
              currentUserGroupInfo.getUserGroupUIDs().size());

      RuntimeException runtimeException = new RuntimeException(msg);
      log.error(msg, runtimeException);
      throw runtimeException;
    }

    return getDataSharingPredicates(
        builder, userDetails, currentUserGroupInfo, AclService.LIKE_READ_DATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails) {
    if (userDetails == null) {
      return List.of();
    }

    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());
    if (userDetails.getUserGroupIds().size() != currentUserGroupInfo.getUserGroupUIDs().size()) {
      String msg =
          String.format(
              "User '%s' getGroups().size() has %d groups, but  getUserGroupUIDs() returns %d groups!",
              userDetails.getUsername(),
              userDetails.getUserGroupIds().size(),
              currentUserGroupInfo.getUserGroupUIDs().size());

      // TODO: MAS: we need to keep this for the special case when acting user's user groups are
      // changed in the request.
      // See tests in AbstractCrudControllerTest, testMergeCollectionItemsJson
      // If it was not the acting user, we could easily invalidate the changed user if they are
      // logged in.
      // Only way to fix this is to update the in session userDetails with the new user groups.

      RuntimeException runtimeException = new RuntimeException(msg);
      log.error(msg, runtimeException);
      //      throw runtimeException;
    }

    return getSharingPredicates(
        builder, userDetails, currentUserGroupInfo, AclService.LIKE_READ_METADATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder,
      UserDetails userDetails,
      CurrentUserGroupInfo groupInfo,
      String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    if (userDetails == null || dataSharingDisabled(userDetails) || groupInfo == null) {
      return predicates;
    }

    return getDataSharingPredicates(
        builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails, String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    if (userDetails == null || dataSharingDisabled(userDetails)) {
      return predicates;
    }

    Set<String> groupIds = userDetails.getUserGroupIds();

    return getDataSharingPredicates(builder, userDetails.getUid(), groupIds, access);
  }

  protected boolean forceAcl() {
    return Dashboard.class.isAssignableFrom(clazz);
  }

  protected boolean sharingEnabled(UserDetails userDetails) {
    boolean b = forceAcl();

    if (b) {
      return b;
    } else {
      return (aclService.isClassShareable(clazz)
          && !(userDetails == null || userDetails.isSuper()));
    }
  }

  protected boolean dataSharingDisabled(UserDetails userDetails) {
    return !aclService.isDataClassShareable(clazz) || userDetails.isSuper();
  }

  private List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder,
      UserDetails userDetails,
      CurrentUserGroupInfo groupInfo,
      String access) {

    if (userDetails == null || groupInfo == null || !sharingEnabled(userDetails)) {
      return List.of();
    }

    return getSharingPredicates(
        builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access);
  }

  @Override
  // TODO: MAS can this be removed and we rely on first fetch on login? make sure current logged in
  // users are get invalidated when group changes
  public CurrentUserGroupInfo getCurrentUserGroupInfo(String userUID) {
    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
    Root<User> root = query.from(User.class);
    query.where(builder.equal(root.get("uid"), userUID));
    query.select(builder.array(root.get("uid"), root.join("groups", JoinType.LEFT).get("uid")));

    Session session = getSession();
    List<Object[]> results = session.createQuery(query).getResultList();

    CurrentUserGroupInfo currentUserGroupInfo = new CurrentUserGroupInfo();

    if (CollectionUtils.isEmpty(results)) {
      currentUserGroupInfo.setUserUID(userUID);
      return currentUserGroupInfo;
    }

    for (Object[] result : results) {
      if (currentUserGroupInfo.getUserUID() == null) {
        currentUserGroupInfo.setUserUID(result[0].toString());
      }

      if (result[1] != null) {
        currentUserGroupInfo.getUserGroupUIDs().add(result[1].toString());
      }
    }

    return currentUserGroupInfo;
  }
}
