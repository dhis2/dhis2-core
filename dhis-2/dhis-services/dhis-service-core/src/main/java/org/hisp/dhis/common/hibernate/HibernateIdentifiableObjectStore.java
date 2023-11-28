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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserDetailsImpl;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author bobj
 */
@Slf4j
public class HibernateIdentifiableObjectStore<T extends BaseIdentifiableObject>
    extends SharingHibernateGenericStoreImpl<T> implements GenericDimensionalObjectStore<T> {
  private static final Set<String> EXISTS_BY_USER_PROPERTIES = Set.of("createdBy", "lastUpdatedBy");

  @Autowired protected DbmsManager dbmsManager;

  protected boolean transientIdentifiableProperties = false;

  public HibernateIdentifiableObjectStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<T> clazz,
      AclService aclService,
      boolean cacheable) {
    super(entityManager, jdbcTemplate, publisher, clazz, aclService, cacheable);

    this.cacheable = cacheable;
  }

  /**
   * Indicates whether the object represented by the implementation does not have persisted
   * identifiable object properties.
   */
  private boolean isTransientIdentifiableProperties() {
    return transientIdentifiableProperties;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObjectStore implementation
  // -------------------------------------------------------------------------

  @Override
  public void save(@Nonnull T object) {
    save(object, true);
  }

  @Override
  public void save(@Nonnull T object, @CheckForNull User user) {
    save(object, CurrentUserDetailsImpl.fromUser(user), true);
  }

  @Override
  public void save(@Nonnull T object, boolean clearSharing) {
    save(object, CurrentUserUtil.getCurrentUserDetails(), clearSharing);
  }

  private void save(T object, CurrentUserDetails userDetails, boolean clearSharing) {

    String username = userDetails != null ? userDetails.getUsername() : "system-process";

    object.setAutoFields();
    //    object.setLastUpdatedBy(user);
    object.setLastUpdatedById(userDetails != null ? userDetails.getId() : null);

    if (clearSharing) {
      object.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
      object.getSharing().resetUserAccesses();
      object.getSharing().resetUserGroupAccesses();
    }

    if (object.getCreatedBy() == null) {
      User createdBy = new User();
      createdBy.setId(userDetails != null ? userDetails.getId() : null);
      createdBy.setUsername(userDetails != null ? userDetails.getUsername() : null);
      object.setCreatedBy(createdBy);

      object.setCreatedById(userDetails != null ? userDetails.getId() : null);
    }

    if (object.getSharing().getOwner() == null) {
      object
          .getSharing()
          .setOwner(object.getCreatedBy() != null ? object.getCreatedBy().getUid() : null);
    }

    if (userDetails != null && aclService.isClassShareable(clazz)) {
      if (clearSharing) {
        if (aclService.canMakePublic(userDetails, (BaseIdentifiableObject) object)) {
          if (aclService.defaultPublic((BaseIdentifiableObject) object)) {
            object.getSharing().setPublicAccess(AccessStringHelper.READ_WRITE);
          }
        } else if (aclService.canMakePrivate(userDetails, (BaseIdentifiableObject) object)) {
          object.getSharing().setPublicAccess(AccessStringHelper.newInstance().build());
        }
      }

      if (!checkPublicAccess(userDetails, object)) {
        AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_CREATE_DENIED);
        throw new CreateAccessDeniedException(object.toString());
      }
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_CREATE);
    getSession().saveOrUpdate(object);
  }

  @Override
  public void update(@Nonnull T object) {
    update(object, CurrentUserUtil.getCurrentUserDetails());
  }

  @Override
  public void update(@Nonnull T object, @CheckForNull CurrentUserDetails userDetails) {
    String username = userDetails != null ? userDetails.getUsername() : "system-process";

    object.setAutoFields();
    //    object.setLastUpdatedBy(user);
    object.setLastUpdatedById(userDetails != null ? userDetails.getId() : null);

    if (object.getSharing().getOwner() == null) {
      //      object.getSharing().setOwner(user);
      object.getSharing().setOwner(userDetails != null ? userDetails.getUid() : null);
    }

    if (object.getCreatedBy() == null) {
      //      object.setCreatedBy(user);
      object.setCreatedById(userDetails != null ? userDetails.getId() : null);
    }

    if (!isUpdateAllowed(object, userDetails)) {
      AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_UPDATE_DENIED);
      throw new UpdateAccessDeniedException(String.valueOf(object));
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_UPDATE);
    getSession().update(object);
  }

  @Override
  public void delete(@Nonnull T object) {
    CurrentUserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();
    String username = userDetails != null ? userDetails.getUsername() : "system-process";

    if (!isDeleteAllowed(object, userDetails)) {
      AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_DELETE_DENIED);
      throw new DeleteAccessDeniedException(object.toString());
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_DELETE);

    super.delete(object);
  }

  @CheckForNull
  @Override
  public final T get(long id) {
    T object = getNoPostProcess(id);
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (object != null && isReadNotAllowed(object, currentUserDetails)) {
      AuditLogUtil.infoWrapper(
          log, CurrentUserUtil.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED);
      throw new ReadAccessDeniedException(object.toString());
    }

    return postProcessObject(object);
  }

  @Nonnull
  @Override
  public final List<T> getAll() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder, new JpaQueryParameters<T>().addPredicates(getSharingPredicates(builder)));
  }

  @Override
  public int getCount() {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Override
  public final T getByUid(@Nonnull String uid) {
    if (isTransientIdentifiableProperties()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("uid"), uid));

    return getSingleResult(builder, param);
  }

  @Override
  public final T getByUidNoAcl(@Nonnull String uid) {
    if (isTransientIdentifiableProperties()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>().addPredicate(root -> builder.equal(root.get("uid"), uid));

    return getSingleResult(builder, param);
  }

  @Override
  public final T getByCodeNoAcl(@Nonnull String code) {
    if (isTransientIdentifiableProperties()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>().addPredicate(root -> builder.equal(root.get("code"), code));

    return getSingleResult(builder, param);
  }

  @Nonnull
  @Override
  public final T loadByUid(@Nonnull String uid) {
    T object = getByUid(uid);

    if (object == null) {
      throw new IllegalQueryException(ErrorCode.E1113, getClazz().getSimpleName(), uid);
    }

    return object;
  }

  @Override
  public final void updateNoAcl(@Nonnull T object) {
    object.setAutoFields();
    getSession().update(object);
  }

  /** Uses query since name property might not be unique. */
  @Override
  @CheckForNull
  public final T getByName(@Nonnull String name) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("name"), name));

    List<T> list = getList(builder, param);

    T object = list != null && !list.isEmpty() ? list.get(0) : null;

    CurrentUserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    if (isReadNotAllowed(object, currentUser)) {
      AuditLogUtil.infoWrapper(
          log, CurrentUserUtil.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED);
      throw new ReadAccessDeniedException(String.valueOf(object));
    }

    return object;
  }

  @Override
  @CheckForNull
  public final T getByCode(@Nonnull String code) {
    if (isTransientIdentifiableProperties()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("code"), code));

    return getSingleResult(builder, param);
  }

  @Nonnull
  @Override
  public final T loadByCode(@Nonnull String code) {
    T object = getByCode(code);

    if (object == null) {
      throw new IllegalQueryException(ErrorCode.E1113, getClazz().getSimpleName(), code);
    }

    return object;
  }

  @Override
  @CheckForNull
  public T getByUniqueAttributeValue(@Nonnull Attribute attribute, @Nonnull String value) {
    if (StringUtils.isEmpty(value) || !attribute.isUnique()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(
                root ->
                    builder.equal(
                        builder.function(
                            FUNCTION_JSONB_EXTRACT_PATH_TEXT,
                            String.class,
                            root.get("attributeValues"),
                            builder.literal(attribute.getUid()),
                            builder.literal("value")),
                        value));

    return getSingleResult(builder, param);
  }

  @Override
  @CheckForNull
  public T getByUniqueAttributeValue(
      @Nonnull Attribute attribute, @Nonnull String value, @CheckForNull CurrentUserDetails user) {
    if (StringUtils.isEmpty(value) || !attribute.isUnique()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder, user))
            .addPredicate(
                root ->
                    builder.equal(
                        builder.function(
                            FUNCTION_JSONB_EXTRACT_PATH_TEXT,
                            String.class,
                            root.get("attributeValues"),
                            builder.literal(attribute.getUid()),
                            builder.literal("value")),
                        value));

    return getSingleResult(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllEqName(@Nonnull String name) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("name"), name))
            .addOrder(root -> builder.asc(root.get("name")));

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllLikeName(@Nonnull String name) {
    return getAllLikeName(name, true);
  }

  @Nonnull
  @Override
  public List<T> getAllLikeName(@Nonnull String name, boolean caseSensitive) {
    CriteriaBuilder builder = getCriteriaBuilder();

    Function<Root<T>, Predicate> likePredicate;

    if (caseSensitive) {
      likePredicate = root -> builder.like(root.get("name"), "%" + name + "%");
    } else {
      likePredicate =
          root -> builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(likePredicate)
            .addOrder(root -> builder.asc(root.get("name")));

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllLikeName(@Nonnull String name, int first, int max) {
    return getAllLikeName(name, first, max, true);
  }

  @Nonnull
  @Override
  public List<T> getAllLikeName(@Nonnull String name, int first, int max, boolean caseSensitive) {
    CriteriaBuilder builder = getCriteriaBuilder();

    Function<Root<T>, Predicate> likePredicate;

    if (caseSensitive) {
      likePredicate = root -> builder.like(root.get("name"), "%" + name + "%");
    } else {
      likePredicate =
          root -> builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(likePredicate)
            .addOrder(root -> builder.asc(root.get("name")))
            .setFirstResult(first)
            .setMaxResults(max);

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllLikeName(@Nonnull Set<String> nameWords, int first, int max) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addOrder(root -> builder.asc(root.get("name")))
            .setFirstResult(first)
            .setMaxResults(max);

    if (nameWords.isEmpty()) {
      return getList(builder, param);
    }

    List<Function<Root<T>, Predicate>> conjunction = new ArrayList<>();

    for (String word : nameWords) {
      conjunction.add(
          root -> builder.like(builder.lower(root.get("name")), "%" + word.toLowerCase() + "%"));
    }

    param.addPredicate(
        root ->
            builder.and(
                conjunction.stream().map(p -> p.apply(root)).toList().toArray(new Predicate[0])));

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllOrderedName() {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addOrder(root -> builder.asc(root.get("name")));

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllOrderedName(int first, int max) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addOrder(root -> builder.asc(root.get("name")))
            .setFirstResult(first)
            .setMaxResults(max);

    return getList(builder, param);
  }

  @Nonnull
  @Override
  public List<T> getAllOrderedLastUpdated(int first, int max) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addOrder(root -> builder.asc(root.get("lastUpdated")));

    return getList(builder, param);
  }

  @Override
  public int getCountLikeName(@Nonnull String name) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(
                root ->
                    builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%"))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Override
  public int getCountGeLastUpdated(@Nonnull Date lastUpdated) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(
                root -> builder.greaterThanOrEqualTo(root.get("lastUpdated"), lastUpdated))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Nonnull
  @Override
  public List<T> getAllGeLastUpdated(@Nonnull Date lastUpdated) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(
                root -> builder.greaterThanOrEqualTo(root.get("lastUpdated"), lastUpdated))
            .addOrder(root -> builder.desc(root.get("lastUpdated")));

    return getList(builder, param);
  }

  @Override
  public int getCountGeCreated(@Nonnull Date created) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.greaterThanOrEqualTo(root.get("created"), created))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Nonnull
  @Override
  public List<T> getAllLeCreated(@Nonnull Date created) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.lessThanOrEqualTo(root.get("created"), created))
            .addOrder(root -> builder.desc(root.get("created")));

    return getList(builder, param);
  }

  @Override
  @CheckForNull
  public Date getLastUpdated() {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Date> query = builder.createQuery(Date.class);

    Root<T> root = query.from(getClazz());

    query.select(root.get("lastUpdated"));

    query.orderBy(builder.desc(root.get("lastUpdated")));

    TypedQuery<Date> typedQuery = getSession().createQuery(query);

    typedQuery.setMaxResults(1);

    typedQuery.setHint(JpaQueryUtils.HIBERNATE_CACHEABLE_HINT, true);

    return getSingleResult(typedQuery);
  }

  @Nonnull
  @Override
  public List<T> getByDataDimension(boolean dataDimension) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> jpaQueryParameters =
        new JpaQueryParameters<T>()
            .addPredicate(root -> builder.equal(root.get("dataDimension"), dataDimension))
            .addPredicates(getSharingPredicates(builder));

    return getList(builder, jpaQueryParameters);
  }

  @Nonnull
  @Override
  public List<T> getByDataDimensionNoAcl(boolean dataDimension) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> jpaQueryParameters =
        new JpaQueryParameters<T>()
            .addPredicate(root -> builder.equal(root.get("dataDimension"), dataDimension));

    return getList(builder, jpaQueryParameters);
  }

  @Nonnull
  @Override
  public List<T> getById(@Nonnull Collection<Long> ids) {
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (ids.isEmpty()) {
      return List.of();
    }

    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, currentUserDetails, "id", ids));
  }

  @Nonnull
  @Override
  public List<T> getByUid(@Nonnull Collection<String> uids) {
    if (uids.isEmpty()) {
      return List.of();
    }

    // TODO Include paging to avoid exceeding max query length

    CriteriaBuilder builder = getCriteriaBuilder();
    List<Function<Root<T>, Predicate>> sharingPredicates = getSharingPredicates(builder);

    return getListFromPartitions(
        builder, uids, 20000, partition -> createInQuery(sharingPredicates, "uid", partition));
  }

  @Nonnull
  @Override
  public List<T> getByUidNoAcl(@Nonnull Collection<String> uids) {
    return getListFromPartitions(
        getCriteriaBuilder(),
        uids,
        OBJECT_FETCH_SIZE,
        partition -> createInQuery(List.of(), "uid", partition));
  }

  @Nonnull
  @Override
  public List<T> getByCode(@Nonnull Collection<String> codes) {
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (codes.isEmpty()) {
      return List.of();
    }
    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, currentUserDetails, "code", codes));
  }

  @Nonnull
  @Override
  public List<T> getByName(@Nonnull Collection<String> names) {
    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (names.isEmpty()) {
      return new ArrayList<>();
    }
    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, currentUserDetails, "name", names));
  }

  @Nonnull
  @Override
  public List<T> getAllNoAcl() {
    return super.getAll();
  }

  @Nonnull
  @Override
  public List<String> getUidsCreatedBefore(@Nonnull Date date) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<String> query = builder.createQuery(String.class);

    Root<T> root = query.from(getClazz());

    query.select(root.get("uid"));
    query.where(builder.lessThan(root.get("created"), date));

    TypedQuery<String> typedQuery = getSession().createQuery(query);
    typedQuery.setHint(JpaQueryUtils.HIBERNATE_CACHEABLE_HINT, true);

    return typedQuery.getResultList();
  }

  // ----------------------------------------------------------------------------------------------------------------
  // Data sharing
  // ----------------------------------------------------------------------------------------------------------------

  @Nonnull
  @Override
  public final List<T> getDataReadAll() {
    return getDataReadAll(CurrentUserUtil.getCurrentUserDetails());
  }

  @Nonnull
  @Override
  public final List<T> getDataReadAll(CurrentUserDetails user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> parameters =
        new JpaQueryParameters<T>().addPredicates(getDataSharingPredicates(builder, user));

    return getList(builder, parameters);
  }

  @Nonnull
  @Override
  public final List<T> getDataWriteAll() {
    return getDataWriteAll(CurrentUserUtil.getCurrentUserDetails());
  }

  @Nonnull
  @Override
  public final List<T> getDataWriteAll(CurrentUserDetails user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> parameters =
        new JpaQueryParameters<T>()
            .addPredicates(getDataSharingPredicates(builder, user, AclService.LIKE_WRITE_DATA));

    return getList(builder, parameters);
  }

  /** Remove given UserGroup UID from all sharing records in given tableName */
  @Override
  public void removeUserGroupFromSharing(@Nonnull String userGroupUid, @Nonnull String tableName) {
    if (!ObjectUtils.allNotNull(userGroupUid, tableName)) {
      return;
    }

    String sql =
        String.format(
            "update %1$s set sharing = sharing #- '{userGroups, %2$s }'", tableName, userGroupUid);

    log.debug("Executing query: " + sql);

    jdbcTemplate.execute(sql);
  }

  /**
   * Look up list objects which have property createdBy or lastUpdatedBy linked to given {@link
   * User}
   *
   * @param user the {@link User} for filtering
   * @return List of objects found.
   */
  @Override
  public List<T> findByUser(@Nonnull CurrentUserDetails user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getListFromPartitions(
        builder,
        List.of(user),
        10000,
        partition ->
            newJpaParameters()
                .addPredicate(
                    root ->
                        builder.or(
                            builder.equal(root.get("createdBy"), user),
                            builder.equal(root.get("lastUpdatedBy"), user))));
  }

  /**
   * Look up list objects which have property lastUpdatedBy linked to given {@link User}
   *
   * @param user the {@link User} for filtering
   * @return List of objects found.
   */
  @Override
  public List<T> findByLastUpdatedBy(@Nonnull CurrentUserDetails user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getListFromPartitions(
        builder,
        List.of(user),
        10000,
        partition ->
            newJpaParameters()
                .addPredicate(root -> builder.equal(root.get("lastUpdatedBy"), user)));
  }

  /**
   * Look up list objects which have property createdBy linked to given {@link User}
   *
   * @param user the {@link User} for filtering
   * @return List of objects found.
   */
  @Override
  public List<T> findByCreatedBy(@Nonnull CurrentUserDetails user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getListFromPartitions(
        builder,
        List.of(user),
        10000,
        partition ->
            newJpaParameters().addPredicate(root -> builder.equal(root.get("createdBy"), user)));
  }

  /**
   * Look up objects which have property createdBy or lastUpdatedBy linked to given {@link User}
   *
   * @param user the {@link User} for filtering
   * @return TRUE of objects found. FALSE otherwise.
   */
  @Override
  public boolean existsByUser(@Nonnull User user, final Set<String> checkProperties) {
    CriteriaBuilder builder = getCriteriaBuilder();
    CriteriaQuery<Integer> query = builder.createQuery(Integer.class);
    Root<T> root = query.from(getClazz());
    query.select(builder.literal(1));
    List<Predicate> predicates =
        checkProperties.stream()
            .filter(EXISTS_BY_USER_PROPERTIES::contains)
            .map(p -> builder.equal(root.get(p), user))
            .toList();
    if (predicates.isEmpty()) {
      return false;
    }
    query.where(builder.or(predicates.toArray(new Predicate[0])));
    return !getSession().createQuery(query).setMaxResults(1).getResultList().isEmpty();
  }

  /**
   * Checks whether the given user has public access to the given identifiable object.
   *
   * @param userDetails the user.
   * @param identifiableObject the identifiable object.
   * @return true or false.
   */
  private boolean checkPublicAccess(
      CurrentUserDetails userDetails, IdentifiableObject identifiableObject) {
    boolean canMakePublic = aclService.canMakePublic(userDetails, identifiableObject);
    boolean canMakePrivate = aclService.canMakePrivate(userDetails, identifiableObject);
    boolean canReadOrWrite =
        AccessStringHelper.canReadOrWrite(identifiableObject.getSharing().getPublicAccess());

    return canMakePublic || (canMakePrivate && !canReadOrWrite);
  }

  private boolean isReadNotAllowed(T object, CurrentUserDetails user) {
    if (sharingEnabled(user == null || user.isSuper())) {
      return !aclService.canRead(user, object);
    }
    return false;
  }

  private boolean isUpdateAllowed(T object, CurrentUserDetails userDetails) {
    if (aclService.isClassShareable(clazz)) {
      return aclService.canUpdate(userDetails, object);
    }
    return true;
  }

  private boolean isDeleteAllowed(T object, CurrentUserDetails userDetails) {
    if (aclService.isClassShareable(clazz)) {
      return aclService.canDelete(userDetails, object);
    }
    return true;
  }

  public void flush() {
    getSession().flush();
  }

  private <V> JpaQueryParameters<T> createInQuery(
      CriteriaBuilder builder, CurrentUserDetails user, String property, Collection<V> values) {
    return createInQuery(getSharingPredicates(builder, user), property, values);
  }

  private <V> JpaQueryParameters<T> createInQuery(
      List<Function<Root<T>, Predicate>> sharing, String property, Collection<V> values) {
    JpaQueryParameters<T> params = new JpaQueryParameters<>();
    if (!sharing.isEmpty()) {
      params = params.addPredicates(sharing);
    }
    return params.addPredicate(root -> root.get(property).in(values));
  }
}
