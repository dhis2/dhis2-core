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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.EventDataType.AGGREGATED_VALUES;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.eventreport.EventReport;

/**
 * This is simple class responsible for ensuring that the necessary attributes are correctly mapped
 * between EventReport and EventVisualization.
 *
 * @deprecated This is needed only to assist the deprecation process of EventReport.
 * @author maikel arabori
 */
@Deprecated
public class EventReportCompatibilityGuard {
  /**
   * Finds objects of type EventReport and sets the respective mandatory type so it can be correctly
   * validated and saved.
   *
   * @deprecated Needed to keep backward compatibility between the new EventVisualization and
   *     EventReport entities.
   * @param bundleParams
   */
  @Deprecated
  public static void handleDeprecationIfEventReport(final ObjectBundleParams bundleParams) {
    final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
        bundleParams.getObjects();

    for (final Map.Entry<Class<? extends IdentifiableObject>, List<IdentifiableObject>> entry :
        objects.entrySet()) {
      handleDeprecationIfEventReport(entry.getKey(), entry.getValue());
    }
  }

  /**
   * If the given class is of type EventReport, we must set a valid type for backward compatibility
   * reasons.
   *
   * @deprecated Needed to keep backward compatibility between the new EventVisualization and
   *     EventReport entities.
   * @param type
   * @param objects
   */
  @Deprecated
  static void handleDeprecationIfEventReport(
      final Class<? extends IdentifiableObject> type,
      final List<? extends IdentifiableObject> objects) {
    if (type.isAssignableFrom(EventReport.class) && isNotEmpty(objects)) {
      setEventReportType(objects);
    }
  }

  /**
   * Sets the correct type for each EventReport in the list.
   *
   * @deprecated Needed to keep backward compatibility between the new EventVisualization and
   *     EventReport entities.
   * @param eventReports
   */
  @Deprecated
  private static void setEventReportType(final List<? extends IdentifiableObject> eventReports) {
    if (isNotEmpty(eventReports)) {
      for (final IdentifiableObject object : eventReports) {
        final EventReport eventReport = (EventReport) object;

        if (AGGREGATED_VALUES == eventReport.getDataType()) {
          eventReport.setType(PIVOT_TABLE);
        } else {
          eventReport.setType(LINE_LIST);
        }
      }
    }
  }
}
