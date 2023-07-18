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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;

/**
 * This interface is implemented by classes that can validate an {@see ObjectBundle}
 *
 * @author Luciano Fiandesio
 */
@FunctionalInterface
public interface ValidationCheck {
  /**
   * Execute a validation check against the {@link ObjectBundle}
   *
   * @param bundle an {@link ObjectBundle} to validate
   * @param klass the class of Object to validate, within the bundle
   * @param persistedObjects a List of IdentifiableObject
   * @param nonPersistedObjects a List of IdentifiableObject
   * @param importStrategy the {@link ImportStrategy}
   * @param context a {@link ValidationContext} containing the services required for validation
   * @return a {@link TypeReport}
   */
  <T extends IdentifiableObject> TypeReport check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context);

  default <T extends IdentifiableObject> List<T> selectObjects(
      List<T> persistedObjects, List<T> nonPersistedObjects, ImportStrategy importStrategy) {

    if (importStrategy.isCreateAndUpdate()) {
      return ValidationUtils.joinObjects(persistedObjects, nonPersistedObjects);
    } else if (importStrategy.isCreate()) {
      return nonPersistedObjects;
    } else if (importStrategy.isUpdate()) {
      return persistedObjects;
    } else {
      return Collections.emptyList();
    }
  }
}
