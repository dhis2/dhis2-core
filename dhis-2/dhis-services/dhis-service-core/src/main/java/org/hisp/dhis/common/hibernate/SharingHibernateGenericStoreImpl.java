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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.hibernate.SharingHibernateGenericStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class contains methods for generating predicates which are used for validating sharing
 * access permission.
 */
@Slf4j
public class SharingHibernateGenericStoreImpl<T extends BaseIdentifiableObject>
    extends InternalHibernateGenericStoreImpl<T> implements SharingHibernateGenericStore<T> {
  public SharingHibernateGenericStoreImpl(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<T> clazz,
      AclService aclService,
      boolean cacheable) {
    super(entityManager, jdbcTemplate, publisher, clazz, aclService, cacheable);
  }

  @Override
  public final List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder, String access) {
    return getSharingPredicatesXX(builder, access);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails, String access) {

    Set<String> userGroupIds = userDetails.getUserGroupIds();
    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());

    if (userGroupIds.size() != currentUserGroupInfo.getUserGroupUIDs().size()) {
      // TODO: MAS test with and without current user group info
      log.error("userGroupIds.size()!=currentUserGroupInfo.getUserGroupUIDs().size()");
      throw new RuntimeException(" NO MACH SIZE user group");
    }

    return getDataSharingPredicates(
        builder, userDetails.getUid(), currentUserGroupInfo.getUserGroupUIDs(), access);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicatesXX(
      CriteriaBuilder builder, String access) {

    UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();
    if (userDetails == null) {
      throw new IllegalArgumentException("UserDetails is null");
    }

    if (!sharingEnabled(userDetails)) {
      return new ArrayList<>();
    }

    Set<String> userGroupIds = userDetails.getUserGroupIds();

    CurrentUserGroupInfo currentUserGroupInfo = getCurrentUserGroupInfo(userDetails.getUid());
    if (userGroupIds.size() != currentUserGroupInfo.getUserGroupUIDs().size()) {
      // TODO: MAS test with and without current user group info
      log.error("userGroupIds.size()!=currentUserGroupInfo.getUserGroupUIDs().size()");
      throw new RuntimeException(" MAS: NO MACH SIZE user group");
    }

    return getSharingPredicates(
        builder, userDetails.getUid(), currentUserGroupInfo.getUserGroupUIDs(), access);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicates(CriteriaBuilder builder) {
    // This should be the only method that accepts null UserDetails
    if (CurrentUserUtil.getCurrentUsername() == null) {
      return List.of();
    }
    return getSharingPredicatesXX(builder, AclService.LIKE_READ_METADATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails) {
    if (userDetails == null) {
      return List.of();
    }

    if (!sharingEnabled(userDetails)) {
      return List.of();
    }

    return getSharingPredicates(
        builder,
        userDetails.getUid(),
        userDetails.getUserGroupIds(),
        AclService.LIKE_READ_METADATA);
  }

  @Override
  public List<Function<Root<T>, Predicate>> getDataSharingPredicates(
      CriteriaBuilder builder, UserDetails userDetails) {

    if (!sharingEnabled(userDetails)) {
      return List.of();
    }

    return getDataSharingPredicates(
        builder,
        userDetails.getUid(),
        userDetails.getUserGroupIds(),
        AclService.LIKE_READ_METADATA);
  }
}
