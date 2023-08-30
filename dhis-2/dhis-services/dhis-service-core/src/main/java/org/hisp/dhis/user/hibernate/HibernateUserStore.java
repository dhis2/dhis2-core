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
package org.hisp.dhis.user.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.time.ZoneId.systemDefault;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.QueryHints;
import org.hibernate.query.Query;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccountExpiryInfo;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Nguyen Hong Duc
 */
@Slf4j
@Repository("org.hisp.dhis.user.UserStore")
public class HibernateUserStore extends HibernateIdentifiableObjectStore<User>
    implements UserStore {
  public static final String DISABLED_COLUMN = "disabled";

  private final QueryCacheManager queryCacheManager;

  private final SchemaService schemaService;

  public HibernateUserStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService,
      SchemaService schemaService,
      QueryCacheManager queryCacheManager) {
    super(
        sessionFactory, jdbcTemplate, publisher, User.class, currentUserService, aclService, true);

    checkNotNull(schemaService);
    this.schemaService = schemaService;
    this.queryCacheManager = queryCacheManager;
  }

  @Override
  public void save(@Nonnull User user, boolean clearSharing) {
    super.save(user, clearSharing);

    currentUserService.invalidateUserGroupCache(user.getUid());
  }

  @Override
  public List<User> getUsers(UserQueryParams params, @Nullable List<String> orders) {
    Query<?> userQuery = getUserQuery(params, orders, false);

    return extractUserQueryUsers(userQuery.list());
  }

  @Override
  public List<User> getUsers(UserQueryParams params) {
    return getUsers(params, null);
  }

  @Override
  public List<User> getExpiringUsers(UserQueryParams params) {
    return extractUserQueryUsers(getUserQuery(params, null, false).list());
  }

  @Override
  public List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays) {
    Date expiryLookAheadDate =
        Date.from(LocalDate.now().plusDays(inDays).atStartOfDay(systemDefault()).toInstant());
    String hql =
        "select new org.hisp.dhis.user.UserAccountExpiryInfo(u.username, u.email, u.accountExpiry) "
            + "from User u "
            + "where u.email is not null and u.disabled = false and u.accountExpiry <= :expiryLookAheadDate";
    return getSession()
        .createQuery(hql, UserAccountExpiryInfo.class)
        .setParameter("expiryLookAheadDate", expiryLookAheadDate)
        .list();
  }

  @Override
  public int getUserCount(UserQueryParams params) {
    Long count = (Long) getUserQuery(params, null, true).uniqueResult();
    return count != null ? count.intValue() : 0;
  }

  @Nonnull
  private List<User> extractUserQueryUsers(@Nonnull List<?> result) {
    if (result.isEmpty()) {
      return Collections.emptyList();
    }

    final List<User> users = new ArrayList<>(result.size());
    for (Object o : result) {
      if (o instanceof User) {
        users.add((User) o);
      } else if (o.getClass().isArray()) {
        users.add((User) ((Object[]) o)[0]);
      }
    }
    return users;
  }

  private Query<?> getUserQuery(UserQueryParams params, List<String> orders, boolean count) {
    SqlHelper hlp = new SqlHelper();

    List<Order> convertedOrder = null;
    String hql = null;

    if (count) {
      hql = "select count(distinct u) ";
    } else {
      Schema userSchema = schemaService.getSchema(User.class);
      convertedOrder = QueryUtils.convertOrderStrings(orders, userSchema);

      hql =
          Stream.of(
                  "select distinct u",
                  JpaQueryUtils.createSelectOrderExpression(convertedOrder, "u"))
              .filter(Objects::nonNull)
              .collect(Collectors.joining(","));
      hql += " ";
    }

    hql += "from User u ";

    if (params.isPrefetchUserGroups() && !count) {
      hql += "left join fetch u.groups g ";
    } else {
      hql += "left join u.groups g ";
    }

    if (!params.getOrganisationUnits().isEmpty()) {
      String opProperty =
          Map.of(
                  UserOrgUnitType.DATA_CAPTURE, "organisationUnits",
                  UserOrgUnitType.DATA_OUTPUT, "dataViewOrganisationUnits",
                  UserOrgUnitType.TEI_SEARCH, "teiSearchOrganisationUnits")
              .getOrDefault(params.getOrgUnitBoundary(), "organisationUnits");
      hql += "left join u." + opProperty + " ou ";

      if (params.isIncludeOrgUnitChildren()) {
        hql += hlp.whereAnd() + " (";

        for (OrganisationUnit ou : params.getOrganisationUnits()) {
          hql += format("ou.path like :ou%s or ", ou.getUid());
        }

        hql = TextUtils.removeLastOr(hql) + ")";
      } else {
        hql += hlp.whereAnd() + " ou.id in (:ouIds) ";
      }
    }

    if (params.hasUserGroups()) {
      hql += hlp.whereAnd() + " g.id in (:userGroupIds) ";
    }

    if (params.getDisabled() != null) {
      hql += hlp.whereAnd() + " u.disabled = :disabled ";
    }

    if (params.isNot2FA()) {
      hql += hlp.whereAnd() + " u.secret is null ";
    }

    if (params.getQuery() != null) {
      hql +=
          hlp.whereAnd()
              + " ("
              + "concat(lower(u.firstName),' ',lower(u.surname)) like :key "
              + "or lower(u.email) like :key "
              + "or lower(u.username) like :key) ";
    }

    if (params.getPhoneNumber() != null) {
      hql += hlp.whereAnd() + " u.phoneNumber = :phoneNumber ";
    }

    if (params.isCanManage() && params.getUser() != null) {
      hql += hlp.whereAnd() + " g.id in (:ids) ";
    }

    if (params.isAuthSubset() && params.getUser() != null) {
      hql +=
          hlp.whereAnd()
              + " not exists ("
              + "select uc2 from User uc2 "
              + "inner join uc2.userRoles ag2 "
              + "inner join ag2.authorities a "
              + "where uc2.id = u.id "
              + "and a not in (:auths) ) ";
    }

    // TODO handle users with no user roles

    if (params.isDisjointRoles() && params.getUser() != null) {
      hql +=
          hlp.whereAnd()
              + " not exists ("
              + "select uc3 from User uc3 "
              + "inner join uc3.userRoles ag3 "
              + "where uc3.id = u.id "
              + "and ag3.id in (:roles) ) ";
    }

    if (params.getLastLogin() != null) {
      hql += hlp.whereAnd() + " u.lastLogin >= :lastLogin ";
    }

    if (params.getInactiveSince() != null) {
      hql += hlp.whereAnd() + " u.lastLogin < :inactiveSince ";
    }

    if (params.getPasswordLastUpdated() != null) {
      hql += hlp.whereAnd() + " u.passwordLastUpdated < :passwordLastUpdated ";
    }

    if (params.isSelfRegistered()) {
      hql += hlp.whereAnd() + " u.selfRegistered = true ";
    }

    if (UserInvitationStatus.ALL.equals(params.getInvitationStatus())) {
      hql += hlp.whereAnd() + " u.invitation = true ";
    }

    if (UserInvitationStatus.EXPIRED.equals(params.getInvitationStatus())) {
      hql +=
          hlp.whereAnd()
              + " u.invitation = true "
              + "and u.restoreToken is not null "
              + "and u.restoreExpiry is not null "
              + "and u.restoreExpiry < current_timestamp() ";
    }

    if (!count) {
      String orderExpression = JpaQueryUtils.createOrderExpression(convertedOrder, "u");
      hql += "order by " + StringUtils.defaultString(orderExpression, "u.surname, u.firstName");
    }

    // ---------------------------------------------------------------------
    // Query parameters
    // ---------------------------------------------------------------------

    log.debug("User query HQL: '{}'", hql);

    Query<?> query = getQuery(hql);

    if (params.getQuery() != null) {
      query.setParameter("key", "%" + params.getQuery().toLowerCase() + "%");
    }

    if (params.getPhoneNumber() != null) {
      query.setParameter("phoneNumber", params.getPhoneNumber());
    }

    if (params.isCanManage() && params.getUser() != null) {
      Collection<Long> managedGroups =
          IdentifiableObjectUtils.getIdentifiers(params.getUser().getManagedGroups());

      query.setParameterList("ids", managedGroups);
    }

    if (params.getDisabled() != null) {
      query.setParameter(DISABLED_COLUMN, params.getDisabled());
    }

    if (params.isAuthSubset() && params.getUser() != null) {
      Set<String> auths = params.getUser().getAllAuthorities();

      query.setParameterList("auths", auths);
    }

    if (params.isDisjointRoles() && params.getUser() != null) {
      Collection<Long> roles =
          IdentifiableObjectUtils.getIdentifiers(params.getUser().getUserRoles());

      query.setParameterList("roles", roles);
    }

    if (params.getLastLogin() != null) {
      query.setParameter("lastLogin", params.getLastLogin());
    }

    if (params.getPasswordLastUpdated() != null) {
      query.setParameter("passwordLastUpdated", params.getPasswordLastUpdated());
    }

    if (params.getInactiveSince() != null) {
      query.setParameter("inactiveSince", params.getInactiveSince());
    }

    if (!params.getOrganisationUnits().isEmpty()) {
      if (params.isIncludeOrgUnitChildren()) {
        for (OrganisationUnit ou : params.getOrganisationUnits()) {
          query.setParameter(format("ou%s", ou.getUid()), "%/" + ou.getUid() + "%");
        }
      } else {
        Collection<Long> ouIds =
            IdentifiableObjectUtils.getIdentifiers(params.getOrganisationUnits());

        query.setParameterList("ouIds", ouIds);
      }
    }

    if (params.hasUserGroups()) {
      Collection<Long> userGroupIds =
          IdentifiableObjectUtils.getIdentifiers(params.getUserGroups());

      query.setParameterList("userGroupIds", userGroupIds);
    }

    if (!count) {
      if (params.getFirst() != null) {
        query.setFirstResult(params.getFirst());
      }

      if (params.getMax() != null) {
        query.setMaxResults(params.getMax());
      }
    }

    setQueryCacheRegionName(query);

    return query;
  }

  private void setQueryCacheRegionName(Query<?> query) {
    if (query.isCacheable()) {
      query.setHint("org.hibernate.cacheable", true);
      query.setHint(
          "org.hibernate.cacheRegion",
          queryCacheManager.getQueryCacheRegionName(User.class, query));
    }
  }

  @Override
  public int getUserCount() {
    Query<Long> query = getTypedQuery("select count(*) from User");
    setQueryCacheRegionName(query);
    return query.uniqueResult().intValue();
  }

  @Override
  public User getUser(long id) {
    return getSession().get(User.class, id);
  }

  @Override
  public User getUserByUsername(String username, boolean ignoreCase) {
    if (username == null) {
      return null;
    }

    String hql =
        ignoreCase
            ? "from User u where lower(u.username) = lower(:username)"
            : "from User u where u.username = :username";

    TypedQuery<User> typedQuery = sessionFactory.getCurrentSession().createQuery(hql, User.class);
    typedQuery.setParameter("username", username);
    typedQuery.setHint(QueryHints.CACHEABLE, true);

    return QueryUtils.getSingleResult(typedQuery);
  }

  @Override
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

  @Override
  public int disableUsersInactiveSince(Date inactiveSince) {
    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaUpdate<User> update = builder.createCriteriaUpdate(User.class);
    Root<User> user = update.from(User.class);
    update.where(
        builder.and(
            // just so we do not count rows already disabled
            builder.equal(user.get(DISABLED_COLUMN), false),
            builder.lessThanOrEqualTo(user.get("lastLogin"), inactiveSince)));
    update.set(DISABLED_COLUMN, true);
    return getSession().createQuery(update).executeUpdate();
  }

  @Override
  public Map<String, Optional<Locale>> findNotifiableUsersWithLastLoginBetween(Date from, Date to) {
    String hql =
        "select u.email, s.value "
            + "from User u "
            + "left outer join UserSetting s on u.id = s.user and s.name = 'keyUiLocale' "
            + "where u.email is not null and u.disabled = false and u.lastLogin >= :from and u.lastLogin < :to";
    return toLocaleMap(hql, from, to);
  }

  @Override
  public Map<String, Optional<Locale>> findNotifiableUsersWithPasswordLastUpdatedBetween(
      Date from, Date to) {
    String hql =
        "select u.email, s.value "
            + "from User u "
            + "left outer join UserSetting s on u.id = s.user and s.name = 'keyUiLocale' "
            + "where u.email is not null and u.disabled = false and u.passwordLastUpdated >= :from and u.passwordLastUpdated < :to";
    return toLocaleMap(hql, from, to);
  }

  private Map<String, Optional<Locale>> toLocaleMap(String hql, Date from, Date to) {
    return getSession()
        .createQuery(hql, Object[].class)
        .setParameter("from", from)
        .setParameter("to", to)
        .stream()
        .collect(
            toMap(
                (Object[] columns) -> (String) columns[0],
                (Object[] columns) -> Optional.ofNullable((Locale) columns[1])));
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getDisplayName(String userUid) {
    String sql = "select concat(firstname, ' ', surname) from userinfo where uid =:uid";
    Query<String> query = getSession().createNativeQuery(sql);
    query.setParameter("uid", userUid);
    return getSingleResult(query);
  }

  @Override
  @CheckForNull
  public User getUserByOpenId(@Nonnull String openId) {
    Query<User> query = getQuery("from User u where u.openId = :openId order by u.lastLogin desc");
    query.setParameter("openId", openId);
    List<User> list = query.getResultList();
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public User getUserByLdapId(String ldapId) {
    Query<User> query = getQuery("from User u where u.ldapId = :ldapId");
    query.setParameter("ldapId", ldapId);
    return query.uniqueResult();
  }

  @Override
  public User getUserByIdToken(String token) {
    Query<User> query = getQuery("from User u where u.idToken = :token");
    query.setParameter("token", token);
    return query.uniqueResult();
  }

  @Override
  public User getUserByUuid(UUID uuid) {
    Query<User> query = getQuery("from User u where u.uuid = :uuid");
    query.setParameter("uuid", uuid);
    return query.uniqueResult();
  }

  @Override
  public List<User> getHasAuthority(String authority) {
    String hql =
        "select distinct uc2 from User uc2 "
            + "inner join uc2.userRoles ag2 "
            + "inner join ag2.authorities a "
            + "where :authority in elements(a)";

    Query<User> query = getQuery(hql);
    query.setParameter("authority", authority);

    return query.getResultList();
  }

  @Override
  @Nonnull
  public List<User> getLinkedUserAccounts(@Nonnull User currentUser) {
    if (currentUser.getOpenId() == null) {
      return List.of();
    }

    Query<User> query = getQuery("from User u where u.openId = :openId order by u.username");
    query.setParameter("openId", currentUser.getOpenId());
    return query.getResultList();
  }

  @Override
  public List<User> getUserByUsernames(Collection<String> usernames) {
    if (usernames == null || usernames.isEmpty()) {
      return new ArrayList<>();
    }

    Query<User> query = getQuery("from User u where u.username in (:usernames) ");
    query.setParameter("usernames", usernames);

    return query.getResultList();
  }
}
