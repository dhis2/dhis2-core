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
package org.hisp.dhis.tracker.imports.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.EnumMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hisp.dhis.tracker.TrackerType;

/**
 * The Bundle Report is responsible for aggregating the outcome of the persistence stage of the
 * Tracker Import.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersistenceReport {
  public static PersistenceReport emptyReport() {
    return new PersistenceReport();
  }

  public PersistenceReport(
      TrackerTypeReport trackedEntityReport,
      TrackerTypeReport enrollmentReport,
      TrackerTypeReport trackerEventReport,
      TrackerTypeReport singleEventReport,
      TrackerTypeReport relationshipReport) {
    TrackerTypeReport eventReport = merge(trackerEventReport, singleEventReport);
    this.typeReportMap =
        Map.of(
            TrackerType.TRACKED_ENTITY, trackedEntityReport,
            TrackerType.ENROLLMENT, enrollmentReport,
            TrackerType.EVENT, eventReport,
            TrackerType.RELATIONSHIP, relationshipReport);
  }

  @JsonProperty
  private Map<TrackerType, TrackerTypeReport> typeReportMap = new EnumMap<>(TrackerType.class);

  @JsonIgnore
  public Stats getStats() {
    Stats stats = new Stats();
    typeReportMap.values().forEach(tr -> stats.merge(tr.getStats()));

    return stats;
  }

  public boolean isEmpty() {
    return typeReportMap.values().stream().allMatch(TrackerTypeReport::isEmpty);
  }

  private TrackerTypeReport merge(
      TrackerTypeReport typeReport, TrackerTypeReport anotherTypeReport) {
    typeReport.getStats().merge(anotherTypeReport.getStats());
    typeReport.getEntityReport().addAll(anotherTypeReport.getEntityReport());
    typeReport.getNotificationDataBundles().addAll(anotherTypeReport.getNotificationDataBundles());

    TrackerTypeReport trackerTypeReport = new TrackerTypeReport(TrackerType.EVENT);
    trackerTypeReport.setStats(typeReport.getStats());
    trackerTypeReport.setEntityReport(typeReport.getEntityReport());
    trackerTypeReport.setNotificationDataBundles(typeReport.getNotificationDataBundles());

    return trackerTypeReport;
  }
}
