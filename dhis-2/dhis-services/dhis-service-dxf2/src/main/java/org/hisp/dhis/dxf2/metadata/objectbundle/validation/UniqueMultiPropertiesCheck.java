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

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.feedback.ErrorCode.E5005;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.Schema;
import org.springframework.stereotype.Component;

@Component
public class UniqueMultiPropertiesCheck implements ObjectValidationCheck {
  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context,
      Consumer<ObjectReport> addReports) {
    Schema schema = context.getSchemaService().getSchema(klass);

    Map<List<String>, List<IdentifiableObject>> propertyValuesToObjects = new HashMap<>();

    for (T identifiableObject : nonPersistedObjects) {
      for (Map.Entry<Collection<String>, Collection<Function<IdentifiableObject, String>>> entry :
          schema.getUniqueMultiPropertiesExctractors().entrySet()) {
        List<String> propertyValues =
            entry.getValue().stream()
                .map(valueExtractor -> valueExtractor.apply(identifiableObject))
                .collect(Collectors.toList());

        propertyValuesToObjects
            .computeIfAbsent(propertyValues, key -> new ArrayList<>())
            .add(identifiableObject);
      }
    }

    for (Map.Entry<List<String>, List<IdentifiableObject>> entry :
        propertyValuesToObjects.entrySet()) {
      List<IdentifiableObject> objects = entry.getValue();
      if (objects.size() > 1) {
        for (IdentifiableObject object : objects) {
          ErrorReport errorReport =
              new ErrorReport(
                  klass,
                  E5005,
                  String.join(", ", entry.getKey()),
                  objects.stream().map(IdentifiableObject::getUid).collect(joining(", ")));
          addReports.accept(ValidationUtils.createObjectReport(errorReport, object, bundle));
          context.markForRemoval(object);
        }
      }
    }
  }
}
