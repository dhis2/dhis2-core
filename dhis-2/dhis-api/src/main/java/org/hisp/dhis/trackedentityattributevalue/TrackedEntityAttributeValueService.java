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
package org.hisp.dhis.trackedentityattributevalue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface TrackedEntityAttributeValueService {
  String ID = TrackedEntityAttributeValueService.class.getName();

  /**
   * Adds an {@link TrackedEntityAttribute}
   *
   * @param attributeValue The to TrackedEntityAttribute add.
   */
  void addTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue);

  /**
   * Updates an {@link TrackedEntityAttribute}.
   *
   * @param attributeValue the TrackedEntityAttribute to update.
   */
  void updateTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue);

  /**
   * Updates an {@link TrackedEntityAttribute}.
   *
   * @param attributeValue the TrackedEntityAttribute to update.
   * @param user User for audits
   */
  void updateTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue, User user);

  /**
   * Deletes a {@link TrackedEntityAttribute}.
   *
   * @param attributeValue the TrackedEntityAttribute to delete.
   */
  void deleteTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue);

  /**
   * Retrieve a {@link TrackedEntityAttributeValue} on a {@link TrackedEntityInstance} and {@link
   * TrackedEntityAttribute}
   *
   * @param attribute {@link TrackedEntityAttribute}
   * @return TrackedEntityAttributeValue
   */
  TrackedEntityAttributeValue getTrackedEntityAttributeValue(
      TrackedEntityInstance instance, TrackedEntityAttribute attribute);

  /**
   * Retrieve {@link TrackedEntityAttributeValue} of a {@link TrackedEntityInstance}
   *
   * @param instance TrackedEntityAttributeValue
   * @return TrackedEntityAttributeValue list
   */
  List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(TrackedEntityInstance instance);

  /**
   * Retrieve {@link TrackedEntityAttributeValue} of a {@link TrackedEntityAttribute}
   *
   * @param attribute {@link TrackedEntityAttribute}
   * @return TrackedEntityAttributeValue list
   */
  List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntityAttribute attribute);

  /**
   * Returns the number of assigned {@link TrackedEntityAttributeValue}s to the given {@link
   * TrackedEntityAttribute}
   *
   * @param attribute {@link TrackedEntityAttribute}
   * @return Number of assigned TrackedEntityAttributeValues
   */
  int getCountOfAssignedTrackedEntityAttributeValues(TrackedEntityAttribute attribute);

  /**
   * Retrieve a list of {@link TrackedEntityAttributeValue} that matches the values and the tea
   * present in uniqueAttributes
   *
   * @param uniqueAttributes A map that links a list of values to a TEA
   * @return TrackedEntityAttributeValue list
   */
  List<TrackedEntityAttributeValue> getUniqueAttributeByValues(
      Map<TrackedEntityAttribute, List<String>> uniqueAttributes);
}
