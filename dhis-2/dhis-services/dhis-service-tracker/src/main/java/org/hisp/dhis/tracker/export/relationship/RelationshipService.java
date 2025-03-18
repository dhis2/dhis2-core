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
package org.hisp.dhis.tracker.export.relationship;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;

public interface RelationshipService {

  /** Find all relationship items matching given params. */
  @Nonnull
  Set<RelationshipItem> findRelationshipItems(
      TrackerType trackerType, UID uid, boolean includeDeleted);

  /** Find all relationships matching given params. */
  @Nonnull
  List<Relationship> findRelationships(RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException, BadRequestException;

  /** Get a page of relationships matching given params. */
  @Nonnull
  Page<Relationship> findRelationships(RelationshipOperationParams params, PageParams pageParams)
      throws ForbiddenException, NotFoundException, BadRequestException;

  /**
   * Get a relationship matching given {@code UID} under the privileges of the currently
   * authenticated user. Returns an {@link Optional} indicating whether the relationship was found.
   *
   * @return an {@link Optional} containing the relationship if found, or an empty {@link Optional}
   *     if not
   */
  @Nonnull
  Optional<Relationship> findRelationship(UID uid);

  /**
   * Get a relationship matching given {@code UID} under the privileges of the currently
   * authenticated user.
   */
  @Nonnull
  Relationship getRelationship(UID uid) throws ForbiddenException, NotFoundException;

  /**
   * Find relationships matching given {@code UID}s under the privileges of the currently
   * authenticated user.
   */
  @Nonnull
  List<Relationship> findRelationships(@Nonnull Set<UID> uids)
      throws ForbiddenException, NotFoundException;

  /**
   * Get relationships matching given relationshipKeys. A {@link RelationshipKey} represents a
   * string concatenating the relationshipType uid, the uid of the `from` entity and the uid of the
   * `to` entity.
   */
  List<Relationship> getRelationshipsByRelationshipKeys(List<RelationshipKey> relationshipKeys);

  /**
   * Fields the {@link #findRelationships(RelationshipOperationParams)} can order relationships by.
   * Ordering by fields other than these is considered a programmer error. Validation of user
   * provided field names should occur before calling {@link
   * #findRelationships(RelationshipOperationParams)}.
   */
  Set<String> getOrderableFields();
}
