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
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.SharingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author bobj
 */
@Slf4j
public class HibernateIdentifiableObjectStore<T extends BaseIdentifiableObject>
    extends SharingHibernateGenericStoreImpl<T> implements GenericDimensionalObjectStore<T> {
  @Autowired protected DbmsManager dbmsManager;

  protected boolean transientIdentifiableProperties = false;

  public HibernateIdentifiableObjectStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      Class<T> clazz,
      CurrentUserService currentUserService,
      AclService aclService,
      boolean cacheable) {
    super(
        sessionFactory, jdbcTemplate, publisher, clazz, aclService, currentUserService, cacheable);

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
  public void save(T object) {
    save(object, true);
  }

  @Override
  public void save(T object, User user) {
    save(object, user, true);
  }

  @Override
  public void save(T object, boolean clearSharing) {
    save(object, currentUserService.getCurrentUser(), clearSharing);
  }

  private void save(T object, User user, boolean clearSharing) {
    String username = user != null ? user.getUsername() : "system-process";

    if (IdentifiableObject.class.isAssignableFrom(HibernateProxyUtils.getRealClass(object))) {
      object.setAutoFields();

      BaseIdentifiableObject identifiableObject = object;
      identifiableObject.setAutoFields();
      identifiableObject.setLastUpdatedBy(user);

      if (clearSharing) {
        identifiableObject.setPublicAccess(AccessStringHelper.DEFAULT);
        SharingUtils.resetAccessCollections(identifiableObject);
      }

      if (identifiableObject.getCreatedBy() == null) {
        identifiableObject.setCreatedBy(user);
      }

      if (identifiableObject.getSharing().getOwner() == null) {
        identifiableObject.getSharing().setOwner(identifiableObject.getCreatedBy());
      }
    }

    if (user != null && aclService.isClassShareable(clazz)) {
      BaseIdentifiableObject identifiableObject = object;

      if (clearSharing) {
        if (aclService.canMakePublic(user, identifiableObject)) {
          if (aclService.defaultPublic(identifiableObject)) {
            identifiableObject.setPublicAccess(AccessStringHelper.READ_WRITE);
          }
        } else if (aclService.canMakePrivate(user, identifiableObject)) {
          identifiableObject.setPublicAccess(AccessStringHelper.newInstance().build());
        }
      }

      if (!checkPublicAccess(user, identifiableObject)) {
        AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_CREATE_DENIED);
        throw new CreateAccessDeniedException(object.toString());
      }
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_CREATE);
    getSession().saveOrUpdate(object);
  }

  @Override
  public void update(T object) {
    update(object, currentUserService.getCurrentUser());
  }

  @Override
  public void update(T object, User user) {
    String username = user != null ? user.getUsername() : "system-process";

    if (object != null) {
      object.setAutoFields();

      object.setAutoFields();
      object.setLastUpdatedBy(user);

      if (object.getSharing().getOwner() == null) {
        object.getSharing().setOwner(user);
      }

      if (object.getCreatedBy() == null) {
        object.setCreatedBy(user);
      }
    }

    if (!isUpdateAllowed(object, user)) {
      AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_UPDATE_DENIED);
      throw new UpdateAccessDeniedException(String.valueOf(object));
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_UPDATE);

    if (object != null) {
      getSession().update(object);
    }
  }

  @Override
  public void delete(T object) {
    this.delete(object, currentUserService.getCurrentUser());
  }

  @Override
  public final void delete(T object, User user) {
    String username = user != null ? user.getUsername() : "system-process";

    if (!isDeleteAllowed(object, user)) {
      AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_DELETE_DENIED);
      throw new DeleteAccessDeniedException(object.toString());
    }

    AuditLogUtil.infoWrapper(log, username, object, AuditLogUtil.ACTION_DELETE);

    if (object != null) {
      super.delete(object);
    }
  }

  @Override
  public final T get(long id) {
    T object = getSession().get(getClazz(), id);

    if (!isReadAllowed(object, currentUserService.getCurrentUser())) {
      AuditLogUtil.infoWrapper(
          log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED);
      throw new ReadAccessDeniedException(object.toString());
    }

    return postProcessObject(object);
  }

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
  public final T getByUid(String uid) {
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
  public final T getByUidNoAcl(String uid) {
    if (isTransientIdentifiableProperties()) {
      return null;
    }

    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>().addPredicate(root -> builder.equal(root.get("uid"), uid));

    return getSingleResult(builder, param);
  }

  @Override
  public final T loadByUid(String uid) {
    T object = getByUid(uid);

    if (object == null) {
      throw new IllegalQueryException(ErrorCode.E1113, getClazz().getSimpleName(), uid);
    }

    return object;
  }

  @Override
  public final void updateNoAcl(T object) {
    object.setAutoFields();
    getSession().update(object);
  }

  /** Uses query since name property might not be unique. */
  @Override
  public final T getByName(String name) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("name"), name));

    List<T> list = getList(builder, param);

    T object = list != null && !list.isEmpty() ? list.get(0) : null;

    if (!isReadAllowed(object, currentUserService.getCurrentUser())) {
      AuditLogUtil.infoWrapper(
          log, currentUserService.getCurrentUsername(), object, AuditLogUtil.ACTION_READ_DENIED);
      throw new ReadAccessDeniedException(String.valueOf(object));
    }

    return object;
  }

  @Override
  public final T getByCode(String code) {
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

  @Override
  public final T loadByCode(String code) {
    T object = getByCode(code);

    if (object == null) {
      throw new IllegalQueryException(ErrorCode.E1113, getClazz().getSimpleName(), code);
    }

    return object;
  }

  @Override
  public T getByUniqueAttributeValue(Attribute attribute, String value) {
    if (attribute == null || StringUtils.isEmpty(value) || !attribute.isUnique()) {
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
  public T getByUniqueAttributeValue(Attribute attribute, String value, User user) {
    if (attribute == null || StringUtils.isEmpty(value) || !attribute.isUnique()) {
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

  @Override
  public List<T> getAllEqName(String name) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.equal(root.get("name"), name))
            .addOrder(root -> builder.asc(root.get("name")));

    return getList(builder, param);
  }

  @Override
  public List<T> getAllLikeName(String name) {
    return getAllLikeName(name, true);
  }

  @Override
  public List<T> getAllLikeName(String name, boolean caseSensitive) {
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

  @Override
  public List<T> getAllLikeName(String name, int first, int max) {
    return getAllLikeName(name, first, max, true);
  }

  @Override
  public List<T> getAllLikeName(String name, int first, int max, boolean caseSensitive) {
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

  @Override
  public List<T> getAllLikeName(Set<String> nameWords, int first, int max) {
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
                conjunction.stream()
                    .map(p -> p.apply(root))
                    .collect(Collectors.toList())
                    .toArray(new Predicate[0])));

    return getList(builder, param);
  }

  public List<T> getAllLikeNameAndEqualsAttribute(
      Set<String> nameWords, String attribute, String attributeValue, int first, int max) {
    if (StringUtils.isEmpty(attribute) || StringUtils.isEmpty(attributeValue)) {
      return new ArrayList<>();
    }

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

    conjunction.add(root -> builder.equal(builder.lower(root.get(attribute)), attributeValue));

    param.addPredicate(
        root ->
            builder.and(
                conjunction.stream()
                    .map(p -> p.apply(root))
                    .collect(Collectors.toList())
                    .toArray(new Predicate[0])));

    return getList(builder, param);
  }

  @Override
  public List<T> getAllOrderedName() {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addOrder(root -> builder.asc(root.get("name")));

    return getList(builder, param);
  }

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
  public int getCountLikeName(String name) {
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
  public int getCountGeLastUpdated(Date lastUpdated) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(
                root -> builder.greaterThanOrEqualTo(root.get("lastUpdated"), lastUpdated))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Override
  public List<T> getAllGeLastUpdated(Date lastUpdated) {
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
  public int getCountGeCreated(Date created) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.greaterThanOrEqualTo(root.get("created"), created))
            .count(root -> builder.countDistinct(root.get("id")));

    return getCount(builder, param).intValue();
  }

  @Override
  public List<T> getAllLeCreated(Date created) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> param =
        new JpaQueryParameters<T>()
            .addPredicates(getSharingPredicates(builder))
            .addPredicate(root -> builder.lessThanOrEqualTo(root.get("created"), created))
            .addOrder(root -> builder.desc(root.get("created")));

    return getList(builder, param);
  }

  @Override
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

  @Override
  public List<T> getByDataDimension(boolean dataDimension) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> jpaQueryParameters =
        new JpaQueryParameters<T>()
            .addPredicate(root -> builder.equal(root.get("dataDimension"), dataDimension))
            .addPredicates(getSharingPredicates(builder));

    return getList(builder, jpaQueryParameters);
  }

  @Override
  public List<T> getByDataDimensionNoAcl(boolean dataDimension) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> jpaQueryParameters =
        new JpaQueryParameters<T>()
            .addPredicate(root -> builder.equal(root.get("dataDimension"), dataDimension));

    return getList(builder, jpaQueryParameters);
  }

  @Override
  public List<T> getById(Collection<Long> ids) {
    return getById(ids, currentUserService.getCurrentUser());
  }

  @Override
  public List<T> getById(Collection<Long> ids, User user) {
    if (ids == null || ids.isEmpty()) {
      return new ArrayList<>();
    }

    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, user, "id", ids));
  }

  @Override
  public List<T> getByUid(Collection<String> uids) {
    return getByUid(uids, currentUserService.getCurrentUser());
  }

  @Override
  public List<T> getByUid(Collection<String> uids, User user) {
    if (uids == null || uids.isEmpty()) {
      return new ArrayList<>();
    }

    // TODO Include paging to avoid exceeding max query length

    CriteriaBuilder builder = getCriteriaBuilder();
    List<Function<Root<T>, Predicate>> sharingPredicates = getSharingPredicates(builder);

    return getListFromPartitions(
        builder, uids, 20000, partition -> createInQuery(sharingPredicates, "uid", partition));
  }

  @Override
  public List<T> getByUidNoAcl(Collection<String> uids) {
    return getListFromPartitions(
        getCriteriaBuilder(),
        uids,
        OBJECT_FETCH_SIZE,
        partition -> createInQuery(List.of(), "uid", partition));
  }

  @Override
  public List<T> getByCode(Collection<String> codes) {
    return getByCode(codes, currentUserService.getCurrentUser());
  }

  @Override
  public List<T> getByCode(Collection<String> codes, User user) {
    if (codes == null || codes.isEmpty()) {
      return new ArrayList<>();
    }
    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, user, "code", codes));
  }

  @Override
  public List<T> getByName(Collection<String> names, User user) {
    if (names == null || names.isEmpty()) {
      return new ArrayList<>();
    }
    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(builder, createInQuery(builder, user, "name", names));
  }

  @Override
  public List<T> getByName(Collection<String> names) {
    return getByName(names, currentUserService.getCurrentUser());
  }

  @Override
  public List<T> getAllNoAcl() {
    return super.getAll();
  }

  @Override
  public List<String> getUidsCreatedBefore(Date date) {
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

  @Override
  public final List<T> getDataReadAll() {
    return getDataReadAll(currentUserService.getCurrentUser());
  }

  @Override
  public final List<T> getDataReadAll(User user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> parameters =
        new JpaQueryParameters<T>().addPredicates(getDataSharingPredicates(builder, user));

    return getList(builder, parameters);
  }

  @Override
  public final List<T> getDataWriteAll() {
    return getDataWriteAll(currentUserService.getCurrentUser());
  }

  @Override
  public final List<T> getDataWriteAll(User user) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<T> parameters =
        new JpaQueryParameters<T>()
            .addPredicates(getDataSharingPredicates(builder, user, AclService.LIKE_WRITE_DATA));

    return getList(builder, parameters);
  }

  /** Remove given UserGroup UID from all sharing records in given tableName */
  @Override
  public void removeUserGroupFromSharing(String userGroupUid, String tableName) {
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
   * Checks whether the given user has public access to the given identifiable object.
   *
   * @param user the user.
   * @param identifiableObject the identifiable object.
   * @return true or false.
   */
  private boolean checkPublicAccess(User user, IdentifiableObject identifiableObject) {
    boolean b1 = aclService.canMakePublic(user, identifiableObject);
    boolean b2 = aclService.canMakePrivate(user, identifiableObject);
    boolean b3 =
        AccessStringHelper.canReadOrWrite(identifiableObject.getSharing().getPublicAccess());
    return b1 || (b2 && !b3);
  }

  private boolean isReadAllowed(T object, User user) {
    if (IdentifiableObject.class.isInstance(object)) {
      IdentifiableObject idObject = object;

      if (sharingEnabled(user)) {
        return aclService.canRead(user, idObject);
      }
    }

    return true;
  }

  private boolean isUpdateAllowed(T object, User user) {
    if (IdentifiableObject.class.isInstance(object)) {
      IdentifiableObject idObject = object;

      if (aclService.isClassShareable(clazz)) {
        return aclService.canUpdate(user, idObject);
      }
    }

    return true;
  }

  private boolean isDeleteAllowed(T object, User user) {
    if (IdentifiableObject.class.isInstance(object)) {
      IdentifiableObject idObject = object;

      if (aclService.isClassShareable(clazz)) {
        return aclService.canDelete(user, idObject);
      }
    }

    return true;
  }

  public void flush() {
    getSession().flush();
  }

  private <V> JpaQueryParameters<T> createInQuery(
      CriteriaBuilder builder, User user, String property, Collection<V> values) {
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
