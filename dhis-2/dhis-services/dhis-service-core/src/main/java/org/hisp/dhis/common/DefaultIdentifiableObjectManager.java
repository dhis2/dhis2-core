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
package org.hisp.dhis.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.google.common.base.Defaults;
import com.google.gson.internal.Primitives;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.util.SharingUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that it is required for nameable object stores to have concrete implementation classes, not
 * rely on the HibernateIdentifiableObjectStore class, in order to be injected as nameable object
 * stores.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component("org.hisp.dhis.common.IdentifiableObjectManager")
public class DefaultIdentifiableObjectManager implements IdentifiableObjectManager {
  public static final String DEFAULT = "default";

  /** Cache for default category objects. Disabled during test phase. */
  private final Cache<Long> defaultObjectCache;

  private final Set<IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStores;

  private final Set<GenericDimensionalObjectStore<? extends DimensionalObject>>
      dimensionalObjectStores;

  private final SessionFactory sessionFactory;

  private final CurrentUserService currentUserService;

  protected final SchemaService schemaService;

  private final Map<
          Class<? extends IdentifiableObject>,
          IdentifiableObjectStore<? extends IdentifiableObject>>
      identifiableObjectStoreCache = new ConcurrentHashMap<>();

  private final Map<
          Class<? extends DimensionalObject>,
          GenericDimensionalObjectStore<? extends DimensionalObject>>
      dimensionalObjectStoreCache = new ConcurrentHashMap<>();

  public DefaultIdentifiableObjectManager(
      Set<IdentifiableObjectStore<? extends IdentifiableObject>> identifiableObjectStores,
      Set<GenericDimensionalObjectStore<? extends DimensionalObject>> dimensionalObjectStores,
      SessionFactory sessionFactory,
      CurrentUserService currentUserService,
      SchemaService schemaService,
      CacheProvider cacheProvider) {
    checkNotNull(identifiableObjectStores);
    checkNotNull(dimensionalObjectStores);
    checkNotNull(sessionFactory);
    checkNotNull(currentUserService);
    checkNotNull(schemaService);
    checkNotNull(cacheProvider);

    this.identifiableObjectStores = identifiableObjectStores;
    this.dimensionalObjectStores = dimensionalObjectStores;
    this.sessionFactory = sessionFactory;
    this.currentUserService = currentUserService;
    this.schemaService = schemaService;
    this.defaultObjectCache = cacheProvider.createDefaultObjectCache();
  }

  // --------------------------------------------------------------------------
  // IdentifiableObjectManager implementation
  // --------------------------------------------------------------------------

  @Override
  @Transactional
  public void save(@Nonnull IdentifiableObject object) {
    save(object, true);
  }

  @Override
  @Transactional
  public void save(@Nonnull IdentifiableObject object, boolean clearSharing) {
    IdentifiableObjectStore<? super IdentifiableObject> store = getIdentifiableObjectStore(object);

    if (store != null) {
      store.save(object, clearSharing);
    }
  }

  @Override
  @Transactional
  public void save(@Nonnull List<IdentifiableObject> objects) {
    objects.forEach(o -> save(o, true));
  }

  @Override
  @Transactional
  public void update(@Nonnull IdentifiableObject object) {
    update(object, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional
  public void update(@Nonnull IdentifiableObject object, @CheckForNull User user) {
    IdentifiableObjectStore<? super IdentifiableObject> store = getIdentifiableObjectStore(object);

    if (store != null) {
      store.update(object, user);
    }
  }

  @Override
  @Transactional
  public void update(@Nonnull List<IdentifiableObject> objects) {
    update(objects, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional
  public void update(@Nonnull List<IdentifiableObject> objects, @CheckForNull User user) {
    if (objects.isEmpty()) {
      return;
    }

    for (IdentifiableObject object : objects) {
      update(object, user);
    }
  }

  @Override
  @Transactional
  public void updateTranslations(
      @Nonnull IdentifiableObject persistedObject, @Nonnull Set<Translation> translations) {
    Session session = sessionFactory.getCurrentSession();

    BaseIdentifiableObject translatedObject = (BaseIdentifiableObject) persistedObject;

    translatedObject.setTranslations(
        translations.stream()
            .filter(t -> !StringUtils.isEmpty(t.getValue()))
            .collect(Collectors.toSet()));

    translatedObject.setLastUpdated(new Date());
    translatedObject.setLastUpdatedBy(currentUserService.getCurrentUser());

    session.update(translatedObject);
  }

  @Override
  @Transactional
  public void delete(@Nonnull IdentifiableObject object) {
    delete(object, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional
  public void delete(@Nonnull IdentifiableObject object, @CheckForNull User user) {
    IdentifiableObjectStore<? super IdentifiableObject> store = getIdentifiableObjectStore(object);

    if (store != null) {
      store.delete(object, user);
    }
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Optional<? extends IdentifiableObject> find(@Nonnull String uid) {
    return identifiableObjectStores.stream()
        .map(store -> store.getByUid(uid))
        .filter(Objects::nonNull)
        .findFirst();
  }

  @Override
  public <T extends IdentifiableObject> List<T> findByUser(Class<T> type, @Nonnull User user) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return findByUser(store, user);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T get(@Nonnull Class<T> type, long id) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.get(id);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T get(@Nonnull Class<T> type, @Nonnull String uid) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getByUid(uid);
  }

  @Nonnull
  @Override
  public <T extends IdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull String uid)
      throws IllegalQueryException {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      throw new IllegalQueryException("No store registered for objects of type: " + type);
    }

    return store.loadByUid(uid);
  }

  @Nonnull
  @Override
  public <T extends IdentifiableObject> T load(
      @Nonnull Class<T> type, @Nonnull ErrorCode errorCode, @Nonnull String uid)
      throws IllegalQueryException {
    T object = get(type, uid);

    if (object == null) {
      throw new IllegalQueryException(new ErrorMessage(errorCode, uid));
    }

    return object;
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> boolean exists(
      @Nonnull Class<T> type, @Nonnull String uid) {
    return get(type, uid) != null;
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T get(
      @Nonnull Collection<Class<? extends T>> types, @Nonnull String uid) {
    return types.stream()
        .map(type -> get(type, uid))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T get(
      @Nonnull Collection<Class<? extends T>> types,
      @Nonnull IdScheme idScheme,
      @Nonnull String identifier) {
    return types.stream()
        .map(type -> getObject(type, idScheme, identifier))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getNoAcl(
      @Nonnull Class<T> type, @Nonnull Collection<String> uids) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByUidNoAcl(uids);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getByCode(@Nonnull Class<T> type, @Nonnull String code) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getByCode(code);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T loadByCode(@Nonnull Class<T> type, @Nonnull String code)
      throws IllegalQueryException {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      throw new IllegalQueryException(ErrorCode.E1113, type.getSimpleName(), code);
    }

    return store.loadByCode(code);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getByName(@Nonnull Class<T> type, @Nonnull String name) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getByUniqueAttributeValue(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull String value) {
    return getByUniqueAttributeValue(type, attribute, value, currentUserService.getCurrentUser());
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getByUniqueAttributeValue(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull String value, User user) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getByUniqueAttributeValue(attribute, value, user);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T search(@Nonnull Class<T> type, @Nonnull String query) {
    T object = get(type, query);

    if (object == null) {
      object = getByCode(type, query);
    }

    if (object == null) {
      object = getByName(type, query);
    }

    return object;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> filter(
      @Nonnull Class<T> type, @Nonnull String query) {
    Set<T> uniqueObjects = new HashSet<>();

    T uidObject = get(type, query);

    if (uidObject != null) {
      uniqueObjects.add(uidObject);
    }

    T codeObject = getByCode(type, query);

    if (codeObject != null) {
      uniqueObjects.add(codeObject);
    }

    uniqueObjects.addAll(getLikeName(type, query, false));

    List<T> objects = new ArrayList<>(uniqueObjects);

    Collections.sort(objects);

    return objects;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getAll(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAll();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getDataWriteAll(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getDataWriteAll();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getDataReadAll(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getDataReadAll();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getAllSorted(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllOrderedName();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getAllByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes) {
    if (!hasAttributeValues(type) || attributes.isEmpty()) {
      return List.of();
    }

    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllByAttributes(attributes);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<AttributeValue> getAllValuesByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes) {
    if (!hasAttributeValues(type) || attributes.isEmpty()) {
      return List.of();
    }

    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllValuesByAttributes(attributes);
  }

  @Override
  public <T extends IdentifiableObject> long countAllValuesByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes) {
    if (!hasAttributeValues(type) || attributes.isEmpty()) {
      return 0;
    }

    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return 0;
    }

    return store.countAllValuesByAttributes(attributes);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getByUid(
      @Nonnull Class<T> type, @Nonnull Collection<String> uids) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByUid(uids);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getByUid(
      Collection<Class<? extends T>> types, @Nonnull Collection<String> uids) {
    List<T> list = new ArrayList<>();

    for (Class<? extends T> type : types) {
      list.addAll(getByUid(type, uids));
    }
    return list;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> loadByUid(
      @Nonnull Class<T> type, @CheckForNull Collection<String> uids) throws IllegalQueryException {
    if (uids == null || uids.isEmpty()) {
      return List.of();
    }

    List<T> objects = getByUid(type, uids);

    List<String> identifiers = IdentifiableObjectUtils.getUids(objects);
    List<String> difference = CollectionUtils.difference(uids, identifiers);

    if (!difference.isEmpty()) {
      throw new IllegalQueryException(
          new ErrorMessage(ErrorCode.E1112, type.getSimpleName(), difference));
    }

    return objects;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getById(
      @Nonnull Class<T> type, @Nonnull Collection<Long> ids) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getById(ids);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getByCode(
      @Nonnull Class<T> type, @Nonnull Collection<String> codes) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByCode(codes);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getOrdered(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme, @Nonnull Collection<String> values) {
    if (values.isEmpty()) {
      return List.of();
    }
    return values.stream()
        .map(value -> getObject(type, idScheme, value))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getByUidOrdered(
      @Nonnull Class<T> type, @Nonnull List<String> uids) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null || uids.isEmpty()) {
      return List.of();
    }
    return uids.stream().map(store::getByUid).filter(Objects::nonNull).collect(toList());
  }

  @Override
  @Transactional
  public <T extends IdentifiableObject> int getCount(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store != null) {
      return store.getCount();
    }

    return 0;
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> int getCountByCreated(
      @Nonnull Class<T> type, @Nonnull Date created) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store != null) {
      return store.getCountGeCreated(created);
    }

    return 0;
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> int getCountByLastUpdated(
      @Nonnull Class<T> type, @Nonnull Date lastUpdated) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store != null) {
      return store.getCountGeLastUpdated(lastUpdated);
    }

    return 0;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getLikeName(
      @Nonnull Class<T> type, @Nonnull String name) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllLikeName(name);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getLikeName(
      @Nonnull Class<T> type, @Nonnull String name, boolean caseSensitive) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllLikeName(name, caseSensitive);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getBetweenSorted(
      @Nonnull Class<T> type, int first, int max) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllOrderedName(first, max);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getBetweenLikeName(
      @Nonnull Class<T> type, @Nonnull Set<String> words, int first, int max) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllLikeName(words, first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> Date getLastUpdated(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getLastUpdated();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> Map<String, T> getIdMap(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property) {
    return getIdMap(type, IdScheme.from(property));
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> Map<String, T> getIdMap(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return Map.of();
    }

    List<T> objects = store.getAll();

    return IdentifiableObjectUtils.getIdMap(objects, idScheme);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property) {
    return getIdMapNoAcl(type, IdScheme.from(property));
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return Map.of();
    }

    List<T> objects = store.getAllNoAcl();

    return IdentifiableObjectUtils.getIdMap(objects, idScheme);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getObjects(
      @Nonnull Class<T> type,
      @Nonnull IdentifiableProperty property,
      @Nonnull Collection<String> identifiers) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null || identifiers.isEmpty()) {
      return List.of();
    }

    switch (property) {
      case UID:
        return store.getByUid(identifiers);
      case CODE:
        return store.getByCode(identifiers);
      case NAME:
        return store.getByName(identifiers);
      default:
        throw new InvalidIdentifierReferenceException(
            "Invalid identifiable property / class combination: " + property);
    }
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getObjects(
      @Nonnull Class<T> type, @Nonnull Collection<Long> identifiers) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getById(identifiers);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getObject(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property, @Nonnull String value) {
    return getObject(type, IdScheme.from(property), value);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getObject(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme, @Nonnull String value) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null || StringUtils.isEmpty(value)) {
      return null;
    }

    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return store.getByUid(value);
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return store.getByCode(value);
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return store.getByName(value);
    }
    if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      if (!hasAttributeValues(type)) {
        return null;
      }

      Attribute attribute = get(Attribute.class, idScheme.getAttribute());

      if (attribute == null) {
        throw new InvalidIdentifierReferenceException(
            "Attribute does not exist: " + idScheme.getAttribute());
      }

      return store.getByUniqueAttributeValue(attribute, value);
    }
    if (idScheme.is(IdentifiableProperty.ID) && Integer.parseInt(value) > 0) {
      return store.get(Integer.parseInt(value));
    }

    throw new InvalidIdentifierReferenceException(
        "Invalid identifiable property / class combination: " + idScheme);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public IdentifiableObject getObject(@Nonnull String uid, @Nonnull String simpleClassName) {
    for (IdentifiableObjectStore<? extends IdentifiableObject> objectStore :
        identifiableObjectStores) {
      if (simpleClassName.equals(objectStore.getClazz().getSimpleName())) {
        return objectStore.getByUid(uid);
      }
    }

    return null;
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public IdentifiableObject getObject(long id, @Nonnull String simpleClassName) {
    for (IdentifiableObjectStore<? extends IdentifiableObject> objectStore :
        identifiableObjectStores) {
      if (simpleClassName.equals(objectStore.getClazz().getSimpleName())) {
        return objectStore.get(id);
      }
    }

    return null;
  }

  @Override
  @Transactional
  public void refresh(@Nonnull Object object) {
    sessionFactory.getCurrentSession().refresh(object);
  }

  @Override
  public void resetNonOwnerProperties(@Nonnull Object object) {
    Schema schema = schemaService.getDynamicSchema(getRealClass(object));

    schema.getProperties().stream()
        .filter(
            p ->
                !p.isOwner()
                    && p.getSetterMethod() != null
                    && !SharingUtils.isLegacySharingProperty(p))
        .forEach(
            p -> {
              Class<?> parameterType = p.getSetterMethod().getParameterTypes()[0];

              if (p.isCollection()) {
                Collection<?> targetObject = ReflectionUtils.newCollectionInstance(parameterType);
                ReflectionUtils.invokeMethod(object, p.getSetterMethod(), targetObject);
              } else {
                if (Primitives.isPrimitive(parameterType)) {
                  ReflectionUtils.invokeMethod(
                      object, p.getSetterMethod(), Defaults.defaultValue(parameterType));
                } else {
                  ReflectionUtils.invokeMethod(object, p.getSetterMethod(), (Object) null);
                }
              }
            });
  }

  @Override
  @Transactional
  public void flush() {
    sessionFactory.getCurrentSession().flush();
  }

  @Override
  @Transactional
  public void clear() {
    sessionFactory.getCurrentSession().clear();
  }

  @Override
  @Transactional
  public void evict(@Nonnull Object object) {
    sessionFactory.getCurrentSession().evict(object);
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> T getNoAcl(@Nonnull Class<T> type, @Nonnull String uid) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return null;
    }

    return store.getByUidNoAcl(uid);
  }

  @Override
  @Transactional
  public <T extends IdentifiableObject> void updateNoAcl(@Nonnull T object) {
    IdentifiableObjectStore<? super T> store = getIdentifiableObjectStore(object);

    if (store != null) {
      store.updateNoAcl(object);
    }
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getAllNoAcl(@Nonnull Class<T> type) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getAllNoAcl();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends DimensionalObject> List<T> getDataDimensions(@Nonnull Class<T> type) {
    GenericDimensionalObjectStore<T> store = getDimensionalObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByDataDimension(true);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends DimensionalObject> List<T> getDataDimensionsNoAcl(@Nonnull Class<T> type) {
    GenericDimensionalObjectStore<T> store = getDimensionalObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByDataDimensionNoAcl(true);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getByAttributeAndValue(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull String value) {
    Schema schema = schemaService.getDynamicSchema(type);

    if (schema == null || !schema.hasPersistedProperty("attributeValues")) {
      return List.of();
    }

    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getByAttributeAndValue(attribute, value);
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> boolean isAttributeValueUnique(
      @Nonnull Class<T> type, @Nonnull T object, @Nonnull AttributeValue attributeValue) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);
    return store != null && store.isAttributeValueUnique(object, attributeValue);
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> boolean isAttributeValueUnique(
      @Nonnull Class<T> type,
      @Nonnull T object,
      @Nonnull Attribute attribute,
      @Nonnull String value) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);
    return store != null && store.isAttributeValueUnique(object, attribute, value);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> getAllByAttributeAndValues(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull List<String> values) {
    IdentifiableObjectStore<T> store = getIdentifiableObjectStore(type);
    return store != null ? store.getAllByAttributeAndValues(attribute, values) : List.of();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults() {
    ToLongFunction<Class<? extends IdentifiableObject>> getIdCachedByName =
        type ->
            defaultObjectCache.get(
                type.getName(),
                t -> {
                  IdentifiableObject obj = getByName(type, "default");
                  return obj == null ? -1 : obj.getId();
                });

    return Map.of(
        Category.class,
        requireNonNull(get(Category.class, getIdCachedByName.applyAsLong(Category.class))),
        CategoryCombo.class,
        requireNonNull(
            get(CategoryCombo.class, getIdCachedByName.applyAsLong(CategoryCombo.class))),
        CategoryOption.class,
        requireNonNull(
            get(CategoryOption.class, getIdCachedByName.applyAsLong(CategoryOption.class))),
        CategoryOptionCombo.class,
        requireNonNull(
            get(
                CategoryOptionCombo.class,
                getIdCachedByName.applyAsLong(CategoryOptionCombo.class))));
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<String> getUidsCreatedBefore(
      @Nonnull Class<? extends IdentifiableObject> type, @Nonnull Date date) {
    IdentifiableObjectStore<?> store = getIdentifiableObjectStore(type);

    if (store == null) {
      return List.of();
    }

    return store.getUidsCreatedBefore(date);
  }

  // --------------------------------------------------------------------------
  // Supportive methods
  // --------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public boolean isDefault(@Nonnull IdentifiableObject object) {
    Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = getDefaults();

    Class<?> realClass = getRealClass(object);

    if (!defaults.containsKey(realClass)) {
      return false;
    }

    IdentifiableObject defaultObject = defaults.get(realClass);

    return defaultObject != null && defaultObject.getUid().equals(object.getUid());
  }

  @Override
  @Transactional
  public void removeUserGroupFromSharing(@Nonnull String userGroupUid) {
    List<Schema> schemas =
        schemaService.getSchemas().stream().filter(Schema::isShareable).collect(toList());

    IdentifiableObjectStore<?> store = getIdentifiableObjectStore(UserGroup.class);
    schemas.forEach(
        schema -> store.removeUserGroupFromSharing(userGroupUid, schema.getTableName()));
  }

  /**
   * Indicates whether the given class type is attribute enabled, i.e. has an attribute value
   * collection as part of the data model.
   *
   * @param <T>
   * @param type the class type.
   * @return true if type is attribute enabled.
   */
  private <T extends IdentifiableObject> boolean hasAttributeValues(@Nonnull Class<T> type) {
    Schema schema = schemaService.getDynamicSchema(type);

    return schema != null && schema.hasAttributeValues();
  }

  @SuppressWarnings("unchecked")
  private <T extends IdentifiableObject> IdentifiableObjectStore<T> getIdentifiableObjectStore(
      @Nonnull T object) {
    return getIdentifiableObjectStore((Class<T>) getRealClass(object));
  }

  @SuppressWarnings("unchecked")
  private <T extends IdentifiableObject> IdentifiableObjectStore<T> getIdentifiableObjectStore(
      @Nonnull Class<T> type) {
    return (IdentifiableObjectStore<T>)
        getObjectStore(type, identifiableObjectStoreCache, identifiableObjectStores);
  }

  @SuppressWarnings("unchecked")
  private <T extends DimensionalObject> GenericDimensionalObjectStore<T> getDimensionalObjectStore(
      @Nonnull Class<T> type) {
    return (GenericDimensionalObjectStore<T>)
        getObjectStore(type, dimensionalObjectStoreCache, dimensionalObjectStores);
  }

  @SuppressWarnings("unchecked")
  private <
          T extends E, E extends IdentifiableObject, S extends IdentifiableObjectStore<? extends E>>
      S getObjectStore(Class<T> type, Map<Class<? extends E>, S> cache, Set<S> stores) {
    Class<T> realType =
        HibernateProxy.class.isAssignableFrom(type) ? (Class<T>) type.getSuperclass() : type;
    return cache.computeIfAbsent(
        realType,
        key -> {
          S store = stores.stream().filter(s -> s.getClazz() == key).findFirst().orElse(null);
          if (store == null) {
            log.debug("No IdentifiableObjectStore found for class: '{}'", realType);
          }
          return store;
        });
  }

  /**
   * Look up list objects by property createdBy or lastUpdatedBy. Among those properties, only
   * persisted ones will be used for looking up.
   *
   * @param store the store to be used for looking up objects.
   * @param user the {@link User} that is linked to createdBy or lastUpdateBy property.
   * @return list of {@link IdentifiableObject} found.
   */
  private <T extends IdentifiableObject> List<T> findByUser(
      IdentifiableObjectStore<T> store, User user) {
    Schema schema = schemaService.getDynamicSchema(store.getClazz());
    boolean hasCreatedBy = schema.getPersistedProperty(BaseIdentifiableObject_.CREATED_BY) != null;
    boolean hasLastUpdatedBy =
        schema.getPersistedProperty(BaseIdentifiableObject_.LAST_UPDATED_BY) != null;

    if (hasCreatedBy && hasLastUpdatedBy) {
      return store.findByUser(user);
    } else if (hasLastUpdatedBy) {
      return store.findByLastUpdatedBy(user);
    } else if (hasCreatedBy) {
      return store.findByCreatedBy(user);
    }

    return List.of();
  }
}
