/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalue;

import org.hisp.dhis.fileresource.FileResource;

public interface DataValueTrimService {

  /**
   * Set {@link FileResource#isAssigned()} to {@code false} for any data value related file resource
   * where no data value exists that actually refers to it (has its UID as value).
   *
   * @return the number of file resources that got changed from assigned being true to becoming
   *     false
   */
  int updateFileResourcesNotAssignedToAnyDataValue();

  /**
   * Set {@link FileResource#isAssigned()} to {@code true} for any data value related file resource
   * where at least one data value exists that actually refers to it (has its UID as value).
   *
   * @return the number of file resources that got changed from assigned being false to becoming
   *     true
   */
  int updateFileResourcesAssignedToAnyDataValue();

  /**
   * Set any row to deleted {@code true} that has an empty value and a DE that does not consider
   * zero being significant.
   *
   * @return the number of data values that got changed from deleted being false to becoming true
   */
  int updateDeletedIfNotZeroIsSignificant();
}
