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
package org.hisp.dhis.tracker.acl;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerAccessManager {
  /**
   * Checks the data read permissions and ownership of a tracked entity given the programs for which
   * the user has metadata access to.
   *
   * @return No errors if a user has access to at least one program
   */
  List<String> canRead(UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks the data write permissions to the TET of a given tracked entity.
   *
   * @return No errors if the user has write access to the TET.
   */
  List<String> canCreate(@Nonnull UserDetails user, TrackedEntity trackedEntity);

  /**
   * Checks the data write permissions to the TET and ownership of a tracked entity given the
   * programs for which the user has metadata access to.
   *
   * @return No errors if a user has write access to the TET and access to at least one program
   */
  List<String> canUpdateAndDelete(UserDetails user, TrackedEntity trackedEntity);

  List<String> canRead(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canCreate(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canUpdate(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canDelete(UserDetails user, Enrollment enrollment, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, TrackerEvent event, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, SingleEvent event);

  List<String> canWrite(UserDetails user, SingleEvent event);

  List<String> canCreate(UserDetails user, TrackerEvent event, boolean skipOwnershipCheck);

  List<String> canUpdate(UserDetails user, TrackerEvent event, boolean skipOwnershipCheck);

  List<String> canDelete(UserDetails user, TrackerEvent event, boolean skipOwnershipCheck);

  List<String> canRead(UserDetails user, Relationship relationship);

  List<String> canCreate(UserDetails user, Relationship relationship);

  List<String> canDelete(UserDetails user, @Nonnull Relationship relationship);

  /**
   * Checks the sharing read access to EventDataValue
   *
   * @param user User validated for write access
   * @param event Event under which the EventDataValue belongs
   * @param dataElement DataElement of EventDataValue
   * @return Empty list if read access allowed, list of errors otherwise.
   */
  List<String> canRead(
      UserDetails user, TrackerEvent event, DataElement dataElement, boolean skipOwnershipCheck);

  /**
   * Checks the sharing read access to EventDataValue
   *
   * @param user User validated for write access
   * @param event SingleEvent under which the EventDataValue belongs
   * @param dataElement DataElement of EventDataValue
   * @return Empty list if read access allowed, list of errors otherwise.
   */
  List<String> canRead(UserDetails user, SingleEvent event, DataElement dataElement);
}
