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
package org.hisp.dhis.dxf2.metadata.feedback;

import static java.util.Collections.unmodifiableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ErrorReportContainer;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "importReport", namespace = DxfNamespaces.DXF_2_0)
public class ImportReport implements ErrorReportContainer {
  private MetadataImportParams importParams;

  private Status status = Status.OK;

  private final Map<Class<?>, TypeReport> typeReportMap = new HashMap<>();

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  /** Removes all {@link TypeReport} entries where the {@link Stats#getTotal()} is zero. */
  public void clean() {
    typeReportMap.entrySet().removeIf(entry -> entry.getValue().getStats().getTotal() == 0);
  }

  public void addTypeReport(TypeReport typeReport) {
    typeReportMap.computeIfAbsent(typeReport.getKlass(), TypeReport::new).merge(typeReport);
  }

  public void addTypeReports(Iterable<TypeReport> typeReports) {
    typeReports.forEach(this::addTypeReport);
  }

  @JsonIgnore
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

  // -----------------------------------------------------------------------------------
  // Getters and Setters
  // -----------------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MetadataImportParams getImportParams() {
    return importParams;
  }

  public void setImportParams(MetadataImportParams importParams) {
    this.importParams = importParams;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Stats getStats() {
    Stats stats = new Stats();
    typeReportMap.values().forEach(typeReport -> stats.merge(typeReport.getStats()));

    return stats;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "typeReports", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "typeReport", namespace = DxfNamespaces.DXF_2_0)
  public List<TypeReport> getTypeReports() {
    return new ArrayList<>(typeReportMap.values());
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "typeReports", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "typeReport", namespace = DxfNamespaces.DXF_2_0)
  public void setTypeReports(List<TypeReport> typeReports) {
    typeReportMap.clear();
    if (typeReports != null) {
      typeReports.forEach(tr -> typeReportMap.put(tr.getKlass(), tr));
    }
  }

  public Set<Class<?>> getTypeReportKeys() {
    return unmodifiableSet(typeReportMap.keySet());
  }

  @JsonIgnore
  public int getTypeReportCount() {
    return typeReportMap.size();
  }

  public TypeReport getTypeReport(Class<?> klass) {
    return typeReportMap.get(klass);
  }

  public void forEachTypeReport(Consumer<TypeReport> reportConsumer) {
    typeReportMap.values().forEach(reportConsumer);
  }

  public ObjectReport getFirstObjectReport() {
    Iterator<TypeReport> iter = typeReportMap.values().iterator();
    if (!iter.hasNext()) {
      return null;
    }
    TypeReport report = iter.next();
    if (!report.hasObjectReports()) {
      return null;
    }
    return report.getFirstObjectReport();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("stats", getStats())
        .add("typeReports", getTypeReports())
        .toString();
  }
}
