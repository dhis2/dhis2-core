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
package org.hisp.dhis.gist;

import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;

/**
 * The GIST API gives convenient access to (potentially large) collections.
 *
 * @apiNote Metadata queries can yield large result sets which is why the API uses the phrase
 *     "export" rather than "get" or "read" to put the reader in the right frame of mind of the
 *     operation that is performed.
 * @implNote Good performance is achieved by directly fetching simple values only. Collections are
 *     projected to size, emptiness or arrays of ids. In addition, the result contains references to
 *     the API endpoints where the items of collections can be browsed with more detail.
 * @author Jan Bernitt
 */
public interface GistService {

  /**
   * Reads a selection of properties of a specific object.
   *
   * @param query specifies the object and properties
   * @return the data
   * @throws BadRequestException in case the query is missing mandatory parameters or contains
   *     contradictory details
   */
  @Nonnull
  GistObject exportObject(@Nonnull GistQuery query) throws BadRequestException;

  /**
   * Reads a selection of properties listing all objects of a specific type that match the filter
   * criteria.
   *
   * @param query specifies the object type, properties and filters (and more)
   * @return the data for matching objects given as values for the requested properties
   * @throws BadRequestException in case the query is missing mandatory parameters or contains
   *     contradictory details
   */
  @Nonnull
  GistObjectList exportObjectList(@Nonnull GistQuery query) throws BadRequestException;

  /**
   * Reads a selection of properties listing all objects of a specific object collection property
   * that match the filter criteria.
   *
   * <p>For this query the {@link org.hisp.dhis.gist.GistQuery.Owner} must be specified.
   *
   * @param query specifies the object property, listed properties and filters (and more)
   * @return the data for matching objects given as values for the requested properties
   * @throws BadRequestException in case the query is missing mandatory parameters or contains
   *     contradictory details
   */
  @Nonnull
  GistObjectList exportPropertyObjectList(@Nonnull GistQuery query) throws BadRequestException;
}
