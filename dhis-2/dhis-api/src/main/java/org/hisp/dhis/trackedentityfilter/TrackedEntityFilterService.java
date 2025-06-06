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
package org.hisp.dhis.trackedentityfilter;

import java.util.List;
import org.hisp.dhis.program.Program;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
public interface TrackedEntityFilterService {
  String ID = TrackedEntityFilter.class.getName();

  /**
   * Adds trackedEntityFilter
   *
   * @param trackedEntityFilter
   * @return id of added trackedEntityFilter
   */
  long add(TrackedEntityFilter trackedEntityFilter);

  /**
   * Deletes trackedEntityFilter
   *
   * @param trackedEntityFilter
   */
  void delete(TrackedEntityFilter trackedEntityFilter);

  /**
   * Updates trackedEntityFilter
   *
   * @param trackedEntityFilter
   */
  void update(TrackedEntityFilter trackedEntityFilter);

  /**
   * Gets trackedEntityFilter
   *
   * @param id id of trackedEntityFilter to be fetched
   * @return trackedEntityFilter
   */
  TrackedEntityFilter get(long id);

  /**
   * Gets trackedEntityFilter
   *
   * @param program program of trackedEntityFilter to be fetched
   * @return trackedEntityFilter
   */
  List<TrackedEntityFilter> get(Program program);

  /**
   * Gets all trackedEntityFilters
   *
   * @return list of trackedEntityFilters
   */
  List<TrackedEntityFilter> getAll();

  /**
   * Validate the trackedEntityFilter
   *
   * @param teFilter
   * @return list of errors for each validation failures
   */
  List<String> validate(TrackedEntityFilter teFilter);
}
