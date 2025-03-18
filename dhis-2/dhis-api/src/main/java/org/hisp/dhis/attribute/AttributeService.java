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
package org.hisp.dhis.attribute;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AttributeService {

  /**
   * Adds an attribute.
   *
   * @param attribute the attribute.
   */
  void addAttribute(Attribute attribute);

  /**
   * Deletes an attribute.
   *
   * @param attribute the attribute.
   */
  void deleteAttribute(Attribute attribute);

  /**
   * Invalidate cached attribute
   *
   * @param attributeId
   */
  void invalidateCachedAttribute(String attributeId);

  /**
   * Gets the attribute with the given uid.
   *
   * @param uid the attribute uid.
   * @return the attribute with the given uid.
   */
  Attribute getAttribute(String uid);

  /**
   * Gets the attribute with the given name.
   *
   * @param name the name.
   * @return the attribute with the given name.
   */
  Attribute getAttributeByName(String name);

  /**
   * Gets all attributes.
   *
   * @return a list of all attributes in no particular order
   */
  List<Attribute> getAllAttributes();

  List<Attribute> getAttributesByIds(Collection<String> ids);

  // -------------------------------------------------------------------------
  // AttributeValue
  // -------------------------------------------------------------------------

  /** Adds an attribute value. */
  <T extends IdentifiableObject> void addAttributeValue(T object, String attributeId, String value)
      throws NonUniqueAttributeValueException;
}
