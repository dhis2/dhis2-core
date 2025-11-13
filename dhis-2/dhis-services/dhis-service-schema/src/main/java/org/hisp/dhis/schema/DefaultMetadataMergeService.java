/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.schema;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultMetadataMergeService implements MetadataMergeService {
  private final SchemaService schemaService;

  @Override
  public <T> T merge(MetadataMergeParams<T> metadataMergeParams) {
    T source = metadataMergeParams.getSource();
    T target = metadataMergeParams.getTarget();

    Schema schema = schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(source));

    for (Property property : schema.getProperties()) {
      if (schema.isIdentifiableObject()) {
        if (metadataMergeParams.isSkipSharing() && ReflectionUtils.isSharingProperty(property)) {
          continue;
        }

        if (metadataMergeParams.isSkipTranslation()
            && ReflectionUtils.isTranslationProperty(property)) {
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

        // Note this exception for Period is sort of a hack
        // because the collections used for periods might be projection
        // which might be reflected in them being immutable, so they cannot just be modified
        if (targetObject == null || property.getItemKlass() == Period.class) {
          targetObject = ReflectionUtils.newCollectionInstance(property.getKlass());
        }

        targetObject.clear();
        targetObject.addAll(sourceObject);

        ReflectionUtils.invokeMethod(target, property.getSetterMethod(), targetObject);
      } else {
        Object sourceObject = ReflectionUtils.invokeMethod(source, property.getGetterMethod());

        ReflectionUtils.invokeMethod(target, property.getSetterMethod(), sourceObject);
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
          new MetadataMergeParams<>(
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
