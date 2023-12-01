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
package org.hisp.dhis.schema.validation;

import java.util.List;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface SchemaValidator {
  /**
   * Validate embedded object against its schema, the object is required to be non-null and have a
   * schema associated with it.
   *
   * @param object Object to validate
   * @param parentClass Only include persisted properties
   * @return list of errors
   */
  List<ErrorReport> validateEmbeddedObject(Object object, Class<?> parentClass);

  /**
   * Validate object against its schema, the object is required to be non-null and have a schema
   * associated with it.
   *
   * @param object Object to validate
   * @param persisted Only include persisted properties
   * @return list of errors
   */
  List<ErrorReport> validate(Object object, boolean persisted);

  /**
   * Validate object against its schema, the object is required to be non-null and have a schema
   * associated with it.
   *
   * <p>Only persisted values will be checked.
   *
   * @param object Object to validate
   * @return list of errors
   */
  List<ErrorReport> validate(Object object);

  /**
   * Validate a single {@link Property} of an object.
   *
   * @param property {@link Property} of the object to validate
   * @param object Object to validate
   * @return list of errors
   */
  List<ErrorReport> validateProperty(Property property, Object object);
}
