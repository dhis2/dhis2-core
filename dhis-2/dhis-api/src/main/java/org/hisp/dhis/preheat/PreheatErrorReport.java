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
package org.hisp.dhis.preheat;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PreheatErrorReport extends ErrorReport {
  private final PreheatIdentifier preheatIdentifier;

  public PreheatErrorReport(
      PreheatIdentifier preheatIdentifier,
      Class<?> mainKlass,
      ErrorCode errorCode,
      Object... args) {
    super(mainKlass, errorCode, args);
    this.preheatIdentifier = preheatIdentifier;
  }

  /**
   * Create new instance of PreheatErrorReport.
   *
   * @param preheatIdentifier {@link PreheatIdentifier}
   * @param errorCode {@link ErrorCode}
   * @param object the object that has the error.
   * @param property the property of given object that has the error.
   * @param args additional arguments required to build {@link org.hisp.dhis.feedback.ErrorMessage}
   *     by the {@link ErrorCode}
   */
  public PreheatErrorReport(
      PreheatIdentifier preheatIdentifier,
      ErrorCode errorCode,
      IdentifiableObject object,
      Property property,
      Object... args) {
    super(object.getClass(), errorCode, args);
    this.preheatIdentifier = preheatIdentifier;
    setMainId(object.getUid());
    setErrorProperty(property.getName());
    setErrorKlass(property.getItemKlass());
  }

  public PreheatIdentifier getPreheatIdentifier() {
    return preheatIdentifier;
  }

  public IdentifiableObject getObjectReference() {
    return getValue() != null ? (IdentifiableObject) getValue() : null;
  }
}
