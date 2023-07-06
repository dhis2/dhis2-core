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

import java.util.List;
import java.util.Set;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AttributeService {
  String ID = AttributeService.class.getName();

  // -------------------------------------------------------------------------
  // Attribute
  // -------------------------------------------------------------------------

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
   * @param attributeUid
   */
  void invalidateCachedAttribute(String attributeUid);

  /**
   * Gets the attribute with the given id.
   *
   * @param id the attribute id.
   * @return the attribute with the given id.
   */
  Attribute getAttribute(long id);

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
   * Gets the attribute with the given code.
   *
   * @param code the code.
   * @return the attribute with the given code.
   */
  Attribute getAttributeByCode(String code);

  /**
   * Gets all attributes.
   *
   * @return a set of all attributes.
   */
  List<Attribute> getAllAttributes();

  List<Attribute> getAttributes(Class<?> klass);

  List<Attribute> getMandatoryAttributes(Class<?> klass);

  List<Attribute> getUniqueAttributes(Class<?> klass);

  // -------------------------------------------------------------------------
  // AttributeValue
  // -------------------------------------------------------------------------

  /**
   * Adds an attribute value.
   *
   * @param attributeValue the attribute value.
   */
  <T extends IdentifiableObject> void addAttributeValue(T object, AttributeValue attributeValue)
      throws NonUniqueAttributeValueException;

  /**
   * Deletes an attribute value.
   *
   * @param object the object which the attributeValue belongs to.
   * @param attributeValue the attribute value.
   */
  <T extends IdentifiableObject> void deleteAttributeValue(T object, AttributeValue attributeValue);

  /**
   * Deletes a Set of attribute values.
   *
   * @param object the object which the attributeValue belongs to.
   * @param attributeValues the Set of attribute values.
   */
  <T extends IdentifiableObject> void deleteAttributeValues(
      T object, Set<AttributeValue> attributeValues);

  <T extends IdentifiableObject> void generateAttributes(List<T> entityList);
}
