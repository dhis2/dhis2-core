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
package org.hisp.dhis.tracker.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
public class TrackerTypeReport {
  @JsonProperty private final TrackerType trackerType;

  @JsonProperty private Stats stats = new Stats();

  @JsonIgnore private List<TrackerSideEffectDataBundle> sideEffectDataBundles = new ArrayList<>();

  private Map<Integer, Entity> entityReport = new HashMap<>();

  public TrackerTypeReport(TrackerType trackerType) {
    this.trackerType = trackerType;
  }

  @JsonCreator
  public TrackerTypeReport(
      @JsonProperty("trackerType") TrackerType trackerType,
      @JsonProperty("stats") Stats stats,
      @JsonProperty("sideEffectDataBundles")
          List<TrackerSideEffectDataBundle> sideEffectDataBundles,
      @JsonProperty("objectReports") List<Entity> entityReport) {
    this.trackerType = trackerType;
    this.stats = stats;
    this.sideEffectDataBundles = sideEffectDataBundles;

    if (entityReport != null) {
      for (Entity entity : entityReport) {
        this.entityReport.put(entity.getIndex(), entity);
      }
    }
  }

  @JsonProperty("objectReports")
  public List<Entity> getEntityReport() {
    return new ArrayList<>(entityReport.values());
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  /**
   * Are there any errors present?
   *
   * @return true or false depending on any errors found
   */
  public boolean isEmpty() {
    return getErrorReports().isEmpty();
  }

  public void addEntity(Entity entity) {
    this.entityReport.put(entity.getIndex(), entity);
  }

  private List<Error> getErrorReports() {
    List<Error> errorReports = new ArrayList<>();
    entityReport.values().forEach(entity -> errorReports.addAll(entity.getErrorReports()));

    return errorReports;
  }

  public Map<Integer, Entity> getEntityReportMap() {
    return entityReport;
  }

  public List<TrackerSideEffectDataBundle> getSideEffectDataBundles() {
    return sideEffectDataBundles;
  }
}
