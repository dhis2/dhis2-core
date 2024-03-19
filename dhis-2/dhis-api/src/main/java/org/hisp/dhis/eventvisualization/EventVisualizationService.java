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
package org.hisp.dhis.eventvisualization;

import java.util.List;
import org.hisp.dhis.common.AnalyticalObjectService;

/**
 * @author maikel arabori
 */
public interface EventVisualizationService extends AnalyticalObjectService<EventVisualization> {
  /**
   * Saves a EventVisualization.
   *
   * @param eventVisualization the EventVisualization to save.
   * @return the generated identifier.
   */
  long save(EventVisualization eventVisualization);

  /**
   * Retrieves the EventVisualization with the given id.
   *
   * @param id the id of the EventVisualization to retrieve.
   * @return the EventVisualization.
   */
  EventVisualization getEventVisualization(long id);

  /**
   * Retrieves the EventVisualization with the given uid.
   *
   * @param uid the uid of the EventVisualization to retrieve.
   * @return the EventVisualization.
   */
  EventVisualization getEventVisualization(String uid);

  /**
   * Deletes a EventVisualization.
   *
   * @param eventVisualization the EventVisualization to delete.
   */
  void delete(EventVisualization eventVisualization);

  /**
   * Retrieves the EventVisualization with the given uid. Bypasses the ACL system.
   *
   * @param uid the uid of the EventVisualization to retrieve.
   * @return the EventVisualization found.
   */
  EventVisualization getVisualizationNoAcl(String uid);

  /**
   * Retrieves all EventVisualizations.
   *
   * @return the list of EventVisualization found.
   */
  List<EventVisualization> getAllEventVisualizations();
}
