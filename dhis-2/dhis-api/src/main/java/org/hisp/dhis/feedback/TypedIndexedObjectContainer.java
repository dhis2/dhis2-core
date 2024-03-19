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
package org.hisp.dhis.feedback;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;

/**
 * {@link IndexedObjectContainer}s for each object type.
 *
 * @author Volker Schmidt
 */
public class TypedIndexedObjectContainer implements ObjectIndexProvider {
  private final Map<Class<? extends IdentifiableObject>, IndexedObjectContainer>
      typedIndexedObjectContainers = new HashMap<>();

  /**
   * Get the typed container for the specified object class.
   *
   * @param c the object class for which the container should be returned.
   * @return the container (if it does not exists, the method creates a container).
   */
  @Nonnull
  public IndexedObjectContainer getTypedContainer(@Nonnull Class<? extends IdentifiableObject> c) {
    return typedIndexedObjectContainers.computeIfAbsent(c, key -> new IndexedObjectContainer());
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Integer mergeObjectIndex(@Nonnull IdentifiableObject object) {
    return getTypedContainer(HibernateProxyUtils.getRealClass(object)).mergeObjectIndex(object);
  }

  /**
   * @param object the identifiable object that should be checked.
   * @return <code>true</code> if the object is included in the container, <code>false</code>
   *     otherwise.
   */
  public boolean containsObject(@Nonnull IdentifiableObject object) {
    final IndexedObjectContainer indexedObjectContainer =
        typedIndexedObjectContainers.get(HibernateProxyUtils.getRealClass(object));
    return indexedObjectContainer != null && indexedObjectContainer.containsObject(object);
  }

  /**
   * Adds an object to the corresponding indexed object container.
   *
   * @param identifiableObject the object that should be added to the container.
   */
  @SuppressWarnings("unchecked")
  public void add(@Nonnull IdentifiableObject identifiableObject) {
    getTypedContainer(HibernateProxyUtils.getRealClass(identifiableObject)).add(identifiableObject);
  }
}
