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
package org.hisp.dhis.tracker.export.event;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface EventService {
  /** Get a file for an events' data element. */
  FileResourceStream getFileResource(UID event, UID dataElement) throws NotFoundException;

  /** Get an image for an events' data element in the given dimension. */
  FileResourceStream getFileResourceImage(UID event, UID dataElement, ImageFileDimension dimension)
      throws NotFoundException, ConflictException, BadRequestException;

  /** Get event matching given {@code UID} and params. */
  Event getEvent(String uid, EventParams eventParams) throws NotFoundException, ForbiddenException;

  /** Get all events matching given params. */
  List<Event> getEvents(EventOperationParams params) throws BadRequestException, ForbiddenException;

  /** Get a page of events matching given params. */
  Page<Event> getEvents(EventOperationParams params, PageParams pageParams)
      throws BadRequestException, ForbiddenException;

  /**
   * Fields the {@link #getEvents(EventOperationParams)} and {@link #getEvents(EventOperationParams,
   * PageParams)} can order events by. Ordering by fields other than these is considered a
   * programmer error. Validation of user provided field names should occur before calling {@link
   * #getEvents(EventOperationParams)} or {@link #getEvents(EventOperationParams, PageParams)}.
   */
  Set<String> getOrderableFields();
}
