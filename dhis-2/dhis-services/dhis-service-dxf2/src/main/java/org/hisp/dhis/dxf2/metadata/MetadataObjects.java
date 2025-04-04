/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Schema;

@Getter
@RequiredArgsConstructor
public final class MetadataObjects {

  private final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects;

  public MetadataObjects() {
    this(new HashMap<>());
  }

  public List<Class<? extends IdentifiableObject>> getClasses() {
    return new ArrayList<>(objects.keySet());
  }

  public List<? extends IdentifiableObject> getObjects(Class<? extends IdentifiableObject> klass) {
    return objects.get(klass);
  }

  @SuppressWarnings("unchecked")
  public MetadataObjects addObject(IdentifiableObject object) {
    if (object == null) {
      return this;
    }
    objects.computeIfAbsent(getRealClass(object), key -> new ArrayList<>()).add(object);
    return this;
  }

  public MetadataObjects addObjects(List<? extends IdentifiableObject> objects) {
    objects.forEach(this::addObject);
    return this;
  }

  public MetadataObjects addMetadata(List<Schema> schemas, Metadata metadata) {
    for (Schema schema : schemas) {
      if (schema.isIdentifiableObject()) {
        @SuppressWarnings("unchecked")
        Class<? extends IdentifiableObject> key =
            (Class<? extends IdentifiableObject>) schema.getKlass();
        addObjects(metadata.getValues(key));
      }
    }
    return this;
  }
}
