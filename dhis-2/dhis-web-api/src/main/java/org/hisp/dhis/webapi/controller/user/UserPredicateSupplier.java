/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.user;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaPredicateSupplier;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;

/**
 * Translates {@link UserQueryParams} into a JPA Criteria {@link Predicate} using an EXISTS
 * subquery. This avoids materializing large UID lists that would exceed PostgreSQL's 65,535
 * parameter limit.
 */
public class UserPredicateSupplier implements JpaPredicateSupplier {

  private static final Map<UserOrgUnitType, String> OU_PROPERTY_MAP =
      Map.of(
          UserOrgUnitType.DATA_CAPTURE, "organisationUnits",
          UserOrgUnitType.DATA_OUTPUT, "dataViewOrganisationUnits",
          UserOrgUnitType.TEI_SEARCH, "teiSearchOrganisationUnits");

  private final UserQueryParams params;

  public UserPredicateSupplier(UserQueryParams params) {
    this.params = params;
  }

  @Override
  public <T> Predicate getPredicate(CriteriaBuilder builder, Root<T> root, CriteriaQuery<?> query) {
    // Build an EXISTS (SELECT 1 FROM User u2 WHERE u2.id = root.id AND <conditions>)
    Subquery<Long> subquery = query.subquery(Long.class);
    Root<User> u2 = subquery.from(User.class);
    subquery.select(builder.literal(1L));

    List<Predicate> conditions = new ArrayList<>();
    // Correlate: u2.id = root.id
    conditions.add(builder.equal(u2.get("id"), root.get("id")));

    addOrgUnitConditions(builder, u2, conditions);
    addQueryCondition(builder, u2, conditions);
    addCanManageCondition(u2, conditions);
    addAuthSubsetCondition(builder, query, u2, conditions);
    addDisjointRolesCondition(builder, query, u2, conditions);
    addSimpleConditions(builder, u2, conditions);
    addUserGroupCondition(builder, u2, conditions);

    subquery.where(conditions.toArray(new Predicate[0]));
    return builder.exists(subquery);
  }

  private void addOrgUnitConditions(
      CriteriaBuilder builder, Root<User> u2, List<Predicate> conditions) {
    if (params.getOrganisationUnits().isEmpty()) return;

    String ouProperty =
        OU_PROPERTY_MAP.getOrDefault(params.getOrgUnitBoundary(), "organisationUnits");
    Join<User, OrganisationUnit> ouJoin = u2.join(ouProperty);

    if (params.isIncludeOrgUnitChildren()) {
      List<Predicate> pathPredicates = new ArrayList<>();
      for (OrganisationUnit ou : params.getOrganisationUnits()) {
        pathPredicates.add(builder.like(ouJoin.get("path"), "%/" + ou.getUid() + "%"));
      }
      conditions.add(builder.or(pathPredicates.toArray(new Predicate[0])));
    } else {
      Collection<Long> ouIds =
          IdentifiableObjectUtils.getIdentifiers(params.getOrganisationUnits());
      conditions.add(ouJoin.get("id").in(ouIds));
    }
  }

  private void addQueryCondition(
      CriteriaBuilder builder, Root<User> u2, List<Predicate> conditions) {
    if (params.getQuery() == null) return;

    String key = "%" + params.getQuery().toLowerCase() + "%";
    Predicate nameLike =
        builder.like(
            builder.lower(
                builder.concat(builder.concat(u2.get("firstName"), " "), u2.get("surname"))),
            key);
    Predicate emailLike = builder.like(builder.lower(u2.get("email")), key);
    Predicate usernameLike = builder.like(builder.lower(u2.get("username")), key);
    conditions.add(builder.or(nameLike, emailLike, usernameLike));
  }

  private void addCanManageCondition(Root<User> u2, List<Predicate> conditions) {
    if (!params.isCanManage() || params.getUserDetails() == null) return;

    Collection<Long> managedGroupIds = params.getUserDetails().getManagedGroupLongIds();
    if (managedGroupIds.isEmpty()) return;

    Join<Object, Object> groupJoin = u2.join("groups");
    conditions.add(groupJoin.get("id").in(managedGroupIds));
  }

  private void addAuthSubsetCondition(
      CriteriaBuilder builder, CriteriaQuery<?> query, Root<User> u2, List<Predicate> conditions) {
    if (!params.isAuthSubset() || params.getUserDetails() == null) return;

    var auths = params.getUserDetails().getAllAuthorities();
    if (auths.isEmpty()) return;

    // NOT EXISTS (SELECT uc2 FROM User uc2 JOIN uc2.userRoles ag2 JOIN ag2.authorities a
    //   WHERE uc2.id = u2.id AND a NOT IN (:auths))
    Subquery<User> authSub = query.subquery(User.class);
    Root<User> uc2 = authSub.from(User.class);
    authSub.select(uc2);
    Join<Object, Object> ag2 = uc2.join("userRoles");
    Join<Object, Object> a = ag2.join("authorities");
    authSub.where(builder.equal(uc2.get("id"), u2.get("id")), a.in(auths).not());
    conditions.add(builder.not(builder.exists(authSub)));
  }

  private void addDisjointRolesCondition(
      CriteriaBuilder builder, CriteriaQuery<?> query, Root<User> u2, List<Predicate> conditions) {
    if (!params.isDisjointRoles() || params.getUserDetails() == null) return;

    Collection<Long> roleIds = params.getUserDetails().getUserRoleLongIds();
    if (roleIds.isEmpty()) return;

    // NOT EXISTS (SELECT uc3 FROM User uc3 JOIN uc3.userRoles ag3
    //   WHERE uc3.id = u2.id AND ag3.id IN (:roles))
    Subquery<User> roleSub = query.subquery(User.class);
    Root<User> uc3 = roleSub.from(User.class);
    roleSub.select(uc3);
    Join<Object, Object> ag3 = uc3.join("userRoles");
    roleSub.where(builder.equal(uc3.get("id"), u2.get("id")), ag3.get("id").in(roleIds));
    conditions.add(builder.not(builder.exists(roleSub)));
  }

  private void addSimpleConditions(
      CriteriaBuilder builder, Root<User> u2, List<Predicate> conditions) {
    if (params.getPhoneNumber() != null) {
      conditions.add(builder.equal(u2.get("phoneNumber"), params.getPhoneNumber()));
    }
    if (params.getDisabled() != null) {
      conditions.add(builder.equal(u2.get("disabled"), params.getDisabled()));
    }
    if (params.isNot2FA()) {
      conditions.add(builder.isNull(u2.get("secret")));
    }
    if (params.getLastLogin() != null) {
      conditions.add(builder.greaterThanOrEqualTo(u2.get("lastLogin"), params.getLastLogin()));
    }
    if (params.getInactiveSince() != null) {
      conditions.add(builder.lessThan(u2.get("lastLogin"), params.getInactiveSince()));
    }
    if (params.getPasswordLastUpdated() != null) {
      conditions.add(
          builder.lessThan(u2.get("passwordLastUpdated"), params.getPasswordLastUpdated()));
    }
    if (params.isSelfRegistered()) {
      conditions.add(builder.isTrue(u2.get("selfRegistered")));
    }
    if (params.getInvitationStatus() != null) {
      conditions.add(builder.isTrue(u2.get("invitation")));
      if (params.getInvitationStatus() == org.hisp.dhis.user.UserInvitationStatus.EXPIRED) {
        conditions.add(builder.isNotNull(u2.get("restoreToken")));
        conditions.add(builder.isNotNull(u2.get("restoreExpiry")));
        conditions.add(builder.lessThan(u2.get("restoreExpiry"), builder.currentTimestamp()));
      }
    }
  }

  private void addUserGroupCondition(
      CriteriaBuilder builder, Root<User> u2, List<Predicate> conditions) {
    if (!params.hasUserGroups()) return;

    Collection<Long> groupIds = IdentifiableObjectUtils.getIdentifiers(params.getUserGroups());
    Join<Object, Object> groupJoin = u2.join("groups");
    conditions.add(groupJoin.get("id").in(groupIds));
  }
}
