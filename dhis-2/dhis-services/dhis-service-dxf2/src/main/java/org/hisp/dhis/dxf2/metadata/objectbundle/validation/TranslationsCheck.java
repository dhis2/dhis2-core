/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.translation.Translation;
import org.springframework.stereotype.Component;

/**
 * This class contains all validations to be performed on {@link Translation} objects as a part of
 * the validation sequence in MetadataImportService
 *
 * @author viet@dhis2.org
 */
@Component
public class TranslationsCheck implements ObjectValidationCheck {
  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context,
      Consumer<ObjectReport> addReports) {
    List<T> objects =
        selectObjectsBasedOnImportStrategy(persistedObjects, nonPersistedObjects, importStrategy);

    if (CollectionUtils.isEmpty(objects)) {
      return;
    }

    Schema schema = context.getSchemaService().getSchema(klass);

    for (int i = 0; i < objects.size(); i++) {
      run(objects.get(i), klass, addReports, schema, i, context);
    }
  }

  public <T extends IdentifiableObject> void run(
      IdentifiableObject object,
      Class<T> klass,
      Consumer<ObjectReport> addReports,
      Schema schema,
      int index,
      ValidationContext context) {
    Set<Translation> translations = object.getTranslations();

    if (CollectionUtils.isEmpty(translations)) {
      return;
    }

    ObjectReport objectReport = new ObjectReport(klass, index);

    Consumer<ErrorReport> addError =
        error ->
            objectReport.addErrorReport(
                error.setErrorKlass(klass).setErrorProperty("translations"));

    checkTranslatable(schema, addError);

    if (!objectReport.hasErrorReports()) checkTranslations(translations, addError);

    if (objectReport.hasErrorReports()) {
      addReports.accept(objectReport);
      if (context != null) {
        context.markForRemoval(object);
      }
    }
  }

  public static void checkTranslations(
      Collection<Translation> translations, Consumer<ErrorReport> addError) {
    if (translations == null || translations.isEmpty()) return;
    int errors = 0;
    for (Translation t : translations) {
      ErrorReport error = checkNotNull(t);
      if (error != null) {
        errors++;
        addError.accept(error);
      }
    }
    if (errors == 0) {
      ErrorReport error = checkUniqueness(translations);
      if (error != null) addError.accept(error);
    }
  }

  public static void checkTranslatable(Schema schema, Consumer<ErrorReport> addError) {
    if (!schema.isTranslatable())
      addError.accept(
          new ErrorReport(Translation.class, ErrorCode.E1107, schema.getKlass().getSimpleName()));
  }

  private static ErrorReport checkUniqueness(Collection<Translation> translations) {
    long uniqueCount =
        translations.stream().map(Translation::getCacheKey).filter(Objects::nonNull).count();
    if (uniqueCount < translations.size()) {
      Set<String> keys = new HashSet<>(translations.size());
      for (Translation t : translations) {
        String key = t.getCacheKey();
        if (keys.contains(key))
          return new ErrorReport(
              Translation.class, ErrorCode.E1106, t.getProperty(), t.getLocale().toString());
        keys.add(key);
      }
    }
    return null;
  }

  @CheckForNull
  private static ErrorReport checkNotNull(Translation t) {
    Locale locale = t.getLocale();
    String property = t.getProperty();
    String value = t.getValue();
    if (locale == null) return new ErrorReport(Translation.class, ErrorCode.E4000, "locale");
    if (property == null) return new ErrorReport(Translation.class, ErrorCode.E4000, "property");
    if (value == null) return new ErrorReport(Translation.class, ErrorCode.E4000, "value");
    return null;
  }
}
