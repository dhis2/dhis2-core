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

import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;

/**
 * Base interface for a {@link ValidationCheck} that only checks objects and creates {@link
 * ObjectReport}s for those that are ignored.
 *
 * <p>Instead of returning a full {@link TypeReport} its {@link #check(ObjectBundle, Class, List,
 * List, ImportStrategy, ValidationContext, Consumer)} has an extra {@link Consumer} argument which
 * is called by implementations to add {@link ObjectReport}s.
 *
 * <p>This is used to avoid to create and return an empty {@link TypeReport} that is not the null
 * object created by {@link TypeReport#empty(Class)}.
 *
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface ObjectValidationCheck extends ValidationCheck {

  @Override
  default <T extends IdentifiableObject> TypeReport check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context) {
    // NB. The box is there so we only create an instance of TypeReport
    // when it is actually needed which we must assume is not often
    TypeReport[] reportBox = new TypeReport[1];
    check(
        bundle,
        klass,
        persistedObjects,
        nonPersistedObjects,
        importStrategy,
        context,
        objectReport -> {
          TypeReport report = reportBox[0];
          if (report == null) {
            report = new TypeReport(klass);
            reportBox[0] = report;
          }
          report.addObjectReport(objectReport);
          report.getStats().incIgnored();
        });
    return reportBox[0] == null ? TypeReport.empty(klass) : reportBox[0];
  }

  /**
   * Same as {@link ValidationCheck#check(ObjectBundle, Class, List, List, ImportStrategy,
   * ValidationContext)} except that instead of returning a {@link TypeReport} it as an extra
   * parameter that consumes {@link ObjectReport} for which it will assume that an object has errors
   * and is ignored.
   *
   * @see ValidationCheck#check(ObjectBundle, Class, List, List, ImportStrategy, ValidationContext)
   * @param addReports called by the implementation for each object which has errors and should be
   *     ignored. The passed {@link ObjectReport} is added to the outer {@link TypeReport}.
   */
  <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context,
      Consumer<ObjectReport> addReports);
}
