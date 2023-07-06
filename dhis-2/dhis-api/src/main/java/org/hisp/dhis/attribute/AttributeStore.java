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
package org.hisp.dhis.attribute;

import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.hisp.dhis.attribute.Attribute.ObjectType;
import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AttributeStore extends IdentifiableObjectStore<Attribute> {
  String ID = AttributeStore.class.getName();

  ImmutableMap<Class<?>, String> CLASS_ATTRIBUTE_MAP =
      stream(ObjectType.values())
          .collect(ImmutableMap.toImmutableMap(ObjectType::getType, ObjectType::getPropertyName));

  /**
   * Get all metadata attributes for a given class, returns empty list for un-supported types.
   *
   * @param klass Class to get metadata attributes for
   * @return List of attributes for this class
   */
  List<Attribute> getAttributes(Class<?> klass);

  /**
   * Get all mandatory metadata attributes for a given class, returns empty list for un-supported
   * types.
   *
   * @param klass Class to get metadata attributes for
   * @return List of mandatory metadata attributes for this class
   */
  List<Attribute> getMandatoryAttributes(Class<?> klass);

  /**
   * Get all unique metadata attributes for a given class, returns empty list for un-supported
   * types.
   *
   * @param klass Class to get metadata attributes for
   * @return List of unique metadata attributes for this class
   */
  List<Attribute> getUniqueAttributes(Class<?> klass);
}
