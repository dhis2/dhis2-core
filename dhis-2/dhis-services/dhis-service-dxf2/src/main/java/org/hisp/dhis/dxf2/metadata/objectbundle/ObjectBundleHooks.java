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

import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.springframework.stereotype.Component;

/**
 * Organises {@link ObjectBundleHook}s.
 *
 * <p>The main point is that a {@link ObjectBundleHook} most of the time is meant for just one
 * particular type of object which is expressed by the {@link ObjectBundleHook#getTarget()} method.
 *
 * @author Jan Bernitt
 */
@Component
public class ObjectBundleHooks {
  private final Map<Class<?>, List<ObjectBundleHook<?>>> hooksForObjectType =
      new ConcurrentHashMap<>();

  /**
   * This is the list of all {@link ObjectBundleHook}s that exist in the system. We only use this to
   * compute the list of effective {@link ObjectBundleHook}s we need for a particular object type.
   */
  private final List<ObjectBundleHook<?>> availableHooks;

  public ObjectBundleHooks(List<ObjectBundleHook<?>> availableHooks) {
    this.availableHooks = availableHooks;
  }

  /**
   * Returns {@link ObjectBundleHook}s to use on a per-object basis.
   *
   * @param object the processed object
   * @param <T> type of the object
   * @return a list of {@link ObjectBundleHook}s to use when calling {@link
   *     ObjectBundleHook#validate(Object, ObjectBundle, Consumer)}, {@link
   *     ObjectBundleHook#preCreate(Object, ObjectBundle)} and {@link
   *     ObjectBundleHook#postCreate(Object, ObjectBundle)}, {@link
   *     ObjectBundleHook#preUpdate(Object, Object, ObjectBundle)} and {@link
   *     ObjectBundleHook#postUpdate(Object, ObjectBundle)} as well as {@link
   *     ObjectBundleHook#preDelete(Object, ObjectBundle)}.
   */
  @SuppressWarnings("unchecked")
  public <T> List<ObjectBundleHook<? super T>> getObjectHooks(T object) {
    return getTypeImportHooks(HibernateProxyUtils.getRealClass(object));
  }

  /**
   * Returns {@link ObjectBundleHook}s to use on a per-object-type basis.
   *
   * @param objectType type of the committed object(s)
   * @param <T> type of the committed object(s)
   * @return a list of {@link ObjectBundleHook}s to use when calling {@link
   *     ObjectBundleHook#preTypeImport(Class, List, ObjectBundle)} and {@link
   *     ObjectBundleHook#postTypeImport(Class, List, ObjectBundle)}.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> List<ObjectBundleHook<? super T>> getTypeImportHooks(Class<T> objectType) {
    return (List) hooksForObjectType.computeIfAbsent(objectType, this::computeHooks);
  }

  /**
   * Returns {@link ObjectBundleHook}s to use on a per {@link ObjectBundle} basis.
   *
   * @param objectTypes types of objects in an {@link ObjectBundle}
   * @return a list of {@link ObjectBundleHook}s to use when calling {@link
   *     ObjectBundleHook#preCommit(ObjectBundle)} and {@link
   *     ObjectBundleHook#postCommit(ObjectBundle)}
   */
  public List<ObjectBundleHook<?>> getCommitHooks(
      List<Class<? extends IdentifiableObject>> objectTypes) {
    return availableHooks.stream()
        .filter(hook -> objectTypes.contains(hook.getTarget()))
        .collect(toList());
  }

  private <T> List<ObjectBundleHook<?>> computeHooks(Class<T> type) {
    if (availableHooks == null || availableHooks.isEmpty()) {
      return emptyList();
    }
    if (type == null) {
      return unmodifiableList(availableHooks);
    }
    return unmodifiableList(
        availableHooks.stream().filter(hook -> isHookForType(hook, type)).collect(toList()));
  }

  private static boolean isHookForType(ObjectBundleHook<?> hook, Class<?> type) {
    Class<?> target = hook.getTarget();
    if (target == null) {
      return true;
    }
    if (target.isInterface() || isAbstract(target.getModifiers())) {
      return target.isAssignableFrom(type);
    }
    return type == target;
  }
}
