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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static java.util.Collections.emptyList;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.UserOverrideMode;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.feedback.ObjectIndexProvider;
import org.hisp.dhis.feedback.TypedIndexedObjectContainer;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundle implements ObjectIndexProvider {
  /** User to use for import job (important for threaded imports). */
  private final User user;

  /**
   * How should the user property be handled, by default it is left as is. You can override this to
   * use current user, or a selected user instead (not yet supported).
   */
  private final UserOverrideMode userOverrideMode;

  /** User to use for override, can be current or a selected user. */
  private User overrideUser;

  /** Should import be imported or just validated. */
  private final ObjectBundleMode objectBundleMode;

  /** What identifiers to match on. */
  private final PreheatIdentifier preheatIdentifier;

  /** Preheat mode to use (default is REFERENCE and should not be changed). */
  private final PreheatMode preheatMode;

  /** Sets import strategy (create, update, etc). */
  private final ImportStrategy importMode;

  /** Adjust hows objects imported should be reported (all objects, only errors etc) */
  private final ImportReportMode importReportMode;

  /** Should import be treated as a atomic import (all or nothing). */
  private final AtomicMode atomicMode;

  /** Merge mode for object updates (default is REPLACE). */
  private final MergeMode mergeMode;

  /** Flush for every object or per type. */
  private final FlushMode flushMode;

  /** Internal preheat bundle. */
  private final Preheat preheat;

  /** Should sharing be considered when importing objects. */
  private final boolean skipSharing;

  /** Should translation be considered when importing objects. */
  private final boolean skipTranslation;

  /** Skip validation of objects (not recommended). */
  private final boolean skipValidation;

  /** Is this import request from MetadataSync service; */
  private final boolean metadataSyncImport;

  /** Job id to use for threaded imports. */
  private JobConfiguration jobId;

  /** Current status of object bundle. */
  private ObjectBundleStatus objectBundleStatus = ObjectBundleStatus.CREATED;

  /** Objects to import. */
  private final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>>
      persistedObjects = new HashMap<>();

  private final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>>
      nonPersistedObjects = new HashMap<>();

  /** Contains the indexes of the objects to be imported grouped by their type. */
  private final TypedIndexedObjectContainer typedIndexedObjectContainer =
      new TypedIndexedObjectContainer();

  /** Pre-scanned map of all object references (mainly used for object book hundle). */
  private Map<Class<?>, Map<String, Map<String, Object>>> objectReferences = new HashMap<>();

  /**
   * Simple class => uid => object map to store extra info about an object. Especially useful for
   * object hooks as they can be working on more than one object at a time, and needs to be
   * stateless.
   */
  private Map<Class<?>, Map<String, Map<String, Object>>> extras = new HashMap<>();

  public ObjectBundle(
      ObjectBundleParams params,
      Preheat preheat,
      Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap) {
    this.user = params.getUser();
    this.userOverrideMode = params.getUserOverrideMode();
    this.overrideUser = params.getOverrideUser();
    this.objectBundleMode = params.getObjectBundleMode();
    this.preheatIdentifier = params.getPreheatIdentifier();
    this.importMode = params.getImportStrategy();
    this.importReportMode = params.getImportReportMode();
    this.atomicMode = params.getAtomicMode();
    this.preheatMode = params.getPreheatMode();
    this.mergeMode = params.getMergeMode();
    this.flushMode = params.getFlushMode();
    this.skipSharing = params.isSkipSharing();
    this.skipTranslation = params.isSkipTranslation();
    this.skipValidation = params.isSkipValidation();
    this.jobId = params.getJobId();
    this.preheat = preheat;
    this.metadataSyncImport = params.isMetadataSyncImport();

    addObject(objectMap);
  }

  public User getUser() {
    return user;
  }

  public UserOverrideMode getUserOverrideMode() {
    return userOverrideMode;
  }

  public User getOverrideUser() {
    return overrideUser;
  }

  public void setOverrideUser(User overrideUser) {
    this.overrideUser = overrideUser;
  }

  public String getUsername() {
    return user != null ? user.getUsername() : "system-process";
  }

  public ObjectBundleMode getObjectBundleMode() {
    return objectBundleMode;
  }

  public PreheatIdentifier getPreheatIdentifier() {
    return preheatIdentifier;
  }

  public PreheatMode getPreheatMode() {
    return preheatMode;
  }

  public ImportStrategy getImportMode() {
    return importMode;
  }

  public ImportReportMode getImportReportMode() {
    return importReportMode;
  }

  public AtomicMode getAtomicMode() {
    return atomicMode;
  }

  public MergeMode getMergeMode() {
    return mergeMode;
  }

  public FlushMode getFlushMode() {
    return flushMode;
  }

  public boolean isSkipSharing() {
    return skipSharing;
  }

  public boolean isSkipTranslation() {
    return skipTranslation;
  }

  public boolean isSkipValidation() {
    return skipValidation;
  }

  public boolean isMetadataSyncImport() {
    return metadataSyncImport;
  }

  public JobConfiguration getJobId() {
    return jobId;
  }

  public boolean hasJobId() {
    return jobId != null;
  }

  public ObjectBundleStatus getObjectBundleStatus() {
    return objectBundleStatus;
  }

  public void setObjectBundleStatus(ObjectBundleStatus objectBundleStatus) {
    this.objectBundleStatus = objectBundleStatus;
  }

  public Preheat getPreheat() {
    return preheat;
  }

  @Nonnull
  @Override
  public Integer mergeObjectIndex(@Nonnull IdentifiableObject object) {
    return typedIndexedObjectContainer.mergeObjectIndex(object);
  }

  /**
   * Returns if the object bundle container contains the specified object.
   *
   * @param object the object that should be checked.
   * @return <code>true</code> if this object container contains the specified object, <code>false
   *     </code> otherwise.
   */
  public boolean containsObject(@Nullable IdentifiableObject object) {
    if (object == null) {
      return false;
    }
    return typedIndexedObjectContainer.containsObject(object);
  }

  @SuppressWarnings({"unchecked"})
  private void addObject(IdentifiableObject object) {
    if (object == null) {
      return;
    }

    Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass(object);

    if (isPersisted(object)) {
      persistedObjects.computeIfAbsent(realClass, key -> new ArrayList<>()).add(object);
    } else {
      nonPersistedObjects.computeIfAbsent(realClass, key -> new ArrayList<>()).add(object);
    }

    typedIndexedObjectContainer.add(object);
  }

  private void addObject(List<IdentifiableObject> objects) {
    objects.forEach(this::addObject);
  }

  private void addObject(
      Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects) {
    objects.values().forEach(this::addObject);
  }

  public void forEach(Consumer<IdentifiableObject> objectConsumer) {
    persistedObjects.values().forEach(list -> list.forEach(objectConsumer));
    nonPersistedObjects.values().forEach(list -> list.forEach(objectConsumer));
  }

  public boolean hasObjects(Class<? extends IdentifiableObject> klass) {
    return persistedObjects.containsKey(klass) || nonPersistedObjects.containsKey(klass);
  }

  public int getObjectsCount(Class<? extends IdentifiableObject> klass) {
    List<IdentifiableObject> none = emptyList();
    return persistedObjects.getOrDefault(klass, none).size()
        + nonPersistedObjects.getOrDefault(klass, none).size();
  }

  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> Iterable<T> getObjects(Class<T> klass) {
    List<IdentifiableObject> persistedObjectsOfType = persistedObjects.get(klass);
    if (persistedObjectsOfType == null || persistedObjectsOfType.isEmpty()) {
      return (Iterable<T>) nonPersistedObjects.getOrDefault(klass, emptyList());
    }
    List<IdentifiableObject> nonPersistedObjectsOfType = nonPersistedObjects.get(klass);
    if (nonPersistedObjectsOfType == null || nonPersistedObjectsOfType.isEmpty()) {
      return (Iterable<T>) persistedObjectsOfType;
    }
    return (Iterable<T>) Iterables.concat(persistedObjectsOfType, nonPersistedObjectsOfType);
  }

  public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects(
      boolean persisted) {
    return persisted ? persistedObjects : nonPersistedObjects;
  }

  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> List<T> getObjects(Class<T> klass, boolean persisted) {
    List<T> identifiableObjects = null;

    if (persisted) {
      if (persistedObjects.containsKey(klass)) {
        identifiableObjects = (List<T>) persistedObjects.get(klass);
      }
    } else {
      if (nonPersistedObjects.containsKey(klass)) {
        identifiableObjects = (List<T>) nonPersistedObjects.get(klass);
      }
    }

    return identifiableObjects != null ? identifiableObjects : emptyList();
  }

  public Map<String, Map<String, Object>> getObjectReferences(Class<?> klass) {
    return objectReferences.get(klass);
  }

  public Map<Class<?>, Map<String, Map<String, Object>>> getObjectReferences() {
    return objectReferences;
  }

  public void setObjectReferences(
      Map<Class<?>, Map<String, Map<String, Object>>> objectReferences) {
    this.objectReferences = objectReferences;
  }

  @SuppressWarnings({"unchecked"})
  public void putExtras(IdentifiableObject identifiableObject, String key, Object object) {
    if (identifiableObject == null
        || StringUtils.isEmpty(identifiableObject.getUid())
        || object == null) {
      return;
    }

    Class<? extends IdentifiableObject> realClass =
        HibernateProxyUtils.getRealClass(identifiableObject);

    extras.computeIfAbsent(realClass, k -> new HashMap<>());

    extras
        .get(realClass)
        .computeIfAbsent(identifiableObject.getUid(), s -> new HashMap<>())
        .put(key, object);
  }

  @SuppressWarnings({"unchecked"})
  public Object getExtras(IdentifiableObject identifiableObject, String key) {
    if (identifiableObject == null || StringUtils.isEmpty(identifiableObject.getUid())) {
      return null;
    }

    Class<? extends IdentifiableObject> realClass =
        HibernateProxyUtils.getRealClass(identifiableObject);

    if (!extras.containsKey(realClass)) {
      return null;
    }

    if (!extras.get(realClass).containsKey(identifiableObject.getUid())) {
      return null;
    }

    return extras.get(realClass).get(identifiableObject.getUid()).get(key);
  }

  @SuppressWarnings({"unchecked"})
  public boolean hasExtras(IdentifiableObject identifiableObject, String key) {
    if (identifiableObject == null || StringUtils.isEmpty(identifiableObject.getUid())) {
      return false;
    }

    Class<? extends IdentifiableObject> realClass =
        HibernateProxyUtils.getRealClass(identifiableObject);

    if (!extras.containsKey(realClass)) {
      return false;
    }

    if (!extras.get(realClass).containsKey(identifiableObject.getUid())) {
      return false;
    }

    return extras.get(realClass).get(identifiableObject.getUid()).containsKey(key);
  }

  @SuppressWarnings({"unchecked"})
  public void removeExtras(IdentifiableObject identifiableObject, String key) {
    if (identifiableObject == null
        || StringUtils.isEmpty(identifiableObject.getUid())
        || !hasExtras(identifiableObject, key)) {
      return;
    }

    Class<? extends IdentifiableObject> realClass =
        HibernateProxyUtils.getRealClass(identifiableObject);

    extras.get(realClass).get(identifiableObject.getUid()).remove(key);

    if (extras.get(realClass).get(identifiableObject.getUid()).isEmpty()) {
      extras.get(realClass).remove(identifiableObject.getUid());
    }

    if (extras.get(realClass).isEmpty()) {
      extras.remove(realClass);
    }
  }

  public boolean isPersisted(IdentifiableObject object) {
    IdentifiableObject cachedObject = preheat.get(preheatIdentifier, object);
    return !(cachedObject == null || cachedObject.getId() == 0);
  }
}
