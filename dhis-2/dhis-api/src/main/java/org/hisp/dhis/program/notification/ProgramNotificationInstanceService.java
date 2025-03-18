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
package org.hisp.dhis.program.notification;

import java.util.List;

/**
 * @author Zubair Asghar
 */
public interface ProgramNotificationInstanceService {
  void save(ProgramNotificationInstance programNotificationInstance);

  void update(ProgramNotificationInstance programNotificationInstance);

  void delete(ProgramNotificationInstance programNotificationInstance);

  ProgramNotificationInstance get(long programNotificationInstance);

  List<ProgramNotificationInstance> getProgramNotificationInstances(
      ProgramNotificationInstanceParam programNotificationInstanceParam);

  /**
   * Get a page of program notification instances. Use {@link
   * #getProgramNotificationInstances(ProgramNotificationInstanceParam)} if you want all program
   * notification instances. This method will fetch one extra item than the page size so the caller
   * can determine if there is a next page.
   */
  // TODO(tracker) ProgramNotificationInstances lives in module dhis-api which is why we
  // cannot reuse code from the dhis-service-tracker module like org.hisp.dhis.tracker.Page. This is
  // why we cannot return a Page here. We need to make a decision and either move this into
  // dhis-service-tracker or use metadata patterns/code.
  List<ProgramNotificationInstance> getProgramNotificationInstancesPage(
      ProgramNotificationInstanceParam programNotificationInstanceParam);

  Long countProgramNotificationInstances(ProgramNotificationInstanceParam params);
}
