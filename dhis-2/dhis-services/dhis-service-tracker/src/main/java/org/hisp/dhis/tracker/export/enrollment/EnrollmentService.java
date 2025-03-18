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
package org.hisp.dhis.tracker.export.enrollment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;

public interface EnrollmentService {

  /**
   * Finds the enrollment that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. Returns an {@link Optional} indicating whether the enrollment was
   * found.
   *
   * @return an {@link Optional} containing the enrollment if found, or an empty {@link Optional} if
   *     not
   */
  @Nonnull
  Optional<Enrollment> findEnrollment(@Nonnull UID uid);

  /**
   * Retrieves the enrollment that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. This does not include program attributes,events, and
   * relationships. To include events, relationships, and program attributes, use {@link
   * #getEnrollment(UID, EnrollmentParams)}.
   *
   * @return the enrollment associated with the specified {@code UID}
   * @throws NotFoundException if the enrollment cannot be found
   */
  @Nonnull
  Enrollment getEnrollment(UID uid) throws NotFoundException;

  /**
   * Retrieves the enrollment that matches the given {@code UID} based on the privileges of the
   * currently authenticated user. This method also includes any events, relationships and program
   * attributes as defined by the provided {@code params}.
   *
   * @return the enrollment associated with the specified {@code UID}
   * @throws NotFoundException if the enrollment cannot be found
   */
  @Nonnull
  Enrollment getEnrollment(UID uid, EnrollmentParams params) throws NotFoundException;

  /** Find all enrollments matching given params. */
  @Nonnull
  List<Enrollment> findEnrollments(EnrollmentOperationParams params)
      throws BadRequestException, ForbiddenException;

  /** Get a page of enrollments matching given params. */
  @Nonnull
  Page<Enrollment> findEnrollments(EnrollmentOperationParams params, PageParams pageParams)
      throws BadRequestException, ForbiddenException;

  /**
   * Find all enrollments matching given {@code UID} under the privileges the user in the context.
   * This method does not get the enrollment relationships.
   */
  @Nonnull
  List<Enrollment> findEnrollments(@Nonnull Set<UID> uids) throws ForbiddenException;

  /**
   * Fields the {@link #findEnrollments(EnrollmentOperationParams)} can order enrollments by.
   * Ordering by fields other than these is considered a programmer error. Validation of user
   * provided field names should occur before calling {@link
   * #findEnrollments(EnrollmentOperationParams)}.
   */
  Set<String> getOrderableFields();
}
