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
package org.hisp.dhis.dxf2.metadata.objectbundle.feedback;

import com.google.common.base.MoreObjects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ErrorReportContainer;
import org.hisp.dhis.feedback.TypeReport;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleValidationReport implements ErrorReportContainer, Iterable<TypeReport> {
  private final Map<Class<?>, TypeReport> typeReportMap = new HashMap<>();

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------
  public int getErrorReportsCountByCode(Class<?> klass, ErrorCode errorCode) {
    TypeReport report = typeReportMap.get(klass);
    return report == null ? 0 : report.getErrorReportsCount(errorCode);
  }

  public void addTypeReport(TypeReport report) {
    if (report == null) {
      return;
    }

    typeReportMap.compute(
        report.getKlass(),
        (key, value) -> {
          if (value == null) {
            return report;
          }
          value.merge(report);
          return value;
        });
  }

  // -----------------------------------------------------------------------------------
  // Getters and Setters
  // -----------------------------------------------------------------------------------

  public boolean isEmpty() {
    return typeReportMap.isEmpty();
  }

  @Override
  public Iterator<TypeReport> iterator() {
    return typeReportMap.values().iterator();
  }

  public TypeReport getTypeReport(Class<?> klass) {
    return typeReportMap.get(klass);
  }

  @Override
  public int getErrorReportsCount() {
    return typeReportMap.values().stream().mapToInt(TypeReport::getErrorReportsCount).sum();
  }

  @Override
  public int getErrorReportsCount(ErrorCode errorCode) {
    return typeReportMap.values().stream()
        .mapToInt(report -> report.getErrorReportsCount(errorCode))
        .sum();
  }

  @Override
  public boolean hasErrorReports() {
    return typeReportMap.values().stream().anyMatch(TypeReport::hasErrorReports);
  }

  @Override
  public boolean hasErrorReport(Predicate<ErrorReport> test) {
    return typeReportMap.values().stream().anyMatch(report -> report.hasErrorReport(test));
  }

  @Override
  public void forEachErrorReport(Consumer<ErrorReport> reportConsumer) {
    typeReportMap.values().forEach(report -> report.forEachErrorReport(reportConsumer));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("typeReportMap", typeReportMap).toString();
  }
}
