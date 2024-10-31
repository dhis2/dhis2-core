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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.common.adapter.Sharing_;
import org.hisp.dhis.common.collection.CollectionUtils;
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
    if (!CurrentUserUtil.hasCurrentUser()) {
      return List.of();
    }
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
      CriteriaBuilder builder, @Nonnull UserDetails userDetails) {
    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());
    return getDataSharingPredicates(
        builder, userDetails, currentUserGroupInfo, AclService.LIKE_READ_DATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder, @Nonnull UserDetails userDetails) {
    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());
    return getSharingPredicates(
        builder, userDetails, currentUserGroupInfo, AclService.LIKE_READ_METADATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder,
      @Nonnull UserDetails userDetails,
      CurrentUserGroupInfo groupInfo,
      String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    if (dataSharingDisabled(userDetails) || groupInfo == null) {
      return predicates;
    }

    return getDataSharingPredicates(
        builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, @Nonnull UserDetails userDetails, String access) {
    List<Function<Root<T>, Predicate>> predicates = new ArrayList<>();

    if (dataSharingDisabled(userDetails)) {
      return predicates;
    }

    Set<String> groupIds = userDetails.getUserGroupIds();

    return getDataSharingPredicates(builder, userDetails.getUid(), groupIds, access);
  }

  protected boolean forceAcl() {
    return Dashboard.class.isAssignableFrom(clazz);
  }

  protected boolean sharingEnabled(@Nonnull UserDetails userDetails) {
    boolean b = forceAcl();
    if (b) {
      return b;
    } else {
      return (aclService.isClassShareable(clazz) && !(userDetails.isSuper()));
    }
  }

  protected boolean dataSharingDisabled(UserDetails userDetails) {
    return !aclService.isDataClassShareable(clazz) || userDetails.isSuper();
  }

  private List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder,
      @Nonnull UserDetails userDetails,
      CurrentUserGroupInfo groupInfo,
      String access) {

    if (groupInfo == null || !sharingEnabled(userDetails)) {
      return List.of();
    }

    return getSharingPredicates(
        builder, groupInfo.getUserUID(), groupInfo.getUserGroupUIDs(), access);
  }

  @Override
  public CurrentUserGroupInfo getCurrentUserGroupInfo(String userUid) {
    return aclService.getCurrentUserGroupInfo(userUid, this::fetchCurrentUserGroupInfo);
  }

  private CurrentUserGroupInfo fetchCurrentUserGroupInfo(String userUid) {
    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
    Root<User> root = query.from(User.class);
    query.where(builder.equal(root.get("uid"), userUid));
    query.select(builder.array(root.get("uid"), root.join("groups", JoinType.LEFT).get("uid")));

    List<Object[]> results = entityManager.createQuery(query).getResultList();

    CurrentUserGroupInfo currentUserGroupInfo = new CurrentUserGroupInfo();
    currentUserGroupInfo.setUserUID(userUid);

    if (CollectionUtils.isEmpty(results)) {
      currentUserGroupInfo.setUserUID(userUid);
      return currentUserGroupInfo;
    }

    for (Object[] result : results) {
      if (result[1] != null) {
        currentUserGroupInfo.getUserGroupUIDs().add(result[1].toString());
      }
    }

    return currentUserGroupInfo;
  }
}
