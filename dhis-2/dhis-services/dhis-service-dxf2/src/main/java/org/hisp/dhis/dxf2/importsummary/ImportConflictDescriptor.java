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
package org.hisp.dhis.dxf2.importsummary;

import org.hisp.dhis.feedback.ErrorCode;

/**
 * Interface that is meant to be implemented by enumerations that define the set of possible
 * conflicts for a certain type of import or a certain phase of an import.
 *
 * @author Jan Bernitt
 */
public interface ImportConflictDescriptor {
  /**
   * @return The error code for the conflict
   */
  ErrorCode getErrorCode();

  /**
   * Use type {@link org.hisp.dhis.i18n.I18n} for error keys that should be translated using the
   * {@link org.hisp.dhis.i18n.I18n}. Otherwise the classes returned should be either {@link
   * org.hisp.dhis.common.IdentifiableObject}s or simple types like {@link String}.
   *
   * @return The type of object that has the conflict and to which the object references point.
   */
  Class<?>[] getObjectTypes();

  /**
   * @return The name of the property of the imported object that the conflict is related to (if
   *     available and clearly related to a single property)
   */
  default String getProperty() {
    // by default conflicts are not related to a single property
    return null;
  }
}
