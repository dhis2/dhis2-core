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
package org.hisp.dhis.schema;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.schema.MergeService")
@Slf4j
public class DefaultMergeService implements MergeService {
  private final SchemaService schemaService;

  @Override
  public <T> T merge(MergeParams<T> mergeParams) {
    T source = mergeParams.getSource();
    T target = mergeParams.getTarget();

    Schema schema = schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(source));

    for (Property property : schema.getProperties()) {
      if (schema.isIdentifiableObject()) {
        if (mergeParams.isSkipSharing() && ReflectionUtils.isSharingProperty(property)) {
          continue;
        }

        if (mergeParams.isSkipTranslation() && ReflectionUtils.isTranslationProperty(property)) {
          continue;
        }
      }

      // Passwords should only be merged manually
      if (property.is(PropertyType.PASSWORD)) {
        continue;
      }

      if (property.isCollection()) {
        Collection<T> sourceObject =
            ReflectionUtils.invokeMethod(source, property.getGetterMethod());
        Collection<T> targetObject =
            ReflectionUtils.invokeMethod(target, property.getGetterMethod());

        if (sourceObject == null) {
          continue;
        }

        if (targetObject == null) {
          targetObject = ReflectionUtils.newCollectionInstance(property.getKlass());
        }

        if (mergeParams.getMergeMode().isMerge()) {
          Collection<T> merged = ReflectionUtils.newCollectionInstance(property.getKlass());
          merged.addAll(targetObject);
          merged.addAll(
              sourceObject.stream().filter(o -> !merged.contains(o)).collect(Collectors.toList()));

          targetObject.clear();
          targetObject.addAll(merged);
        } else {
          targetObject.clear();
          targetObject.addAll(sourceObject);
        }

        ReflectionUtils.invokeMethod(target, property.getSetterMethod(), targetObject);
      } else {
        Object sourceObject = ReflectionUtils.invokeMethod(source, property.getGetterMethod());

        if (mergeParams.getMergeMode().isReplace()
            || (mergeParams.getMergeMode().isMerge() && sourceObject != null)) {
          ReflectionUtils.invokeMethod(target, property.getSetterMethod(), sourceObject);
        }
      }
    }

    return target;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T clone(T source) {
    if (source == null) return null;

    try {
      return merge(
          new MergeParams<>(
                  source,
                  (T)
                      HibernateProxyUtils.getRealClass(source)
                          .getDeclaredConstructor()
                          .newInstance())
              .setMergeMode(MergeMode.REPLACE));
    } catch (InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException e) {
      log.info("Failed to clone source object, source=" + source, e);
    }

    return null;
  }
}
