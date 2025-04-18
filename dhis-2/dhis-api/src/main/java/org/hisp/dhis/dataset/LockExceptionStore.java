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
package org.hisp.dhis.dataset;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface LockExceptionStore extends GenericStore<LockException> {
  List<LockException> getLockExceptions(List<DataSet> dataSets);

  List<LockException> getLockExceptionCombinations();

  void deleteLockExceptions(DataSet dataSet, Period period);

  void deleteLockExceptions(DataSet dataSet, Period period, OrganisationUnit organisationUnit);

  void deleteLockExceptions(OrganisationUnit organisationUnit);

  /**
   * Deletes all lock exceptions that are considered expired. This means their creation date is
   * before the given date.
   *
   * @param createdBefore The threshold date, any {@link LockException} with an older created date
   *     is deleted
   * @return number of deleted lock exceptions
   */
  int deleteExpiredLockExceptions(Date createdBefore);

  long getCount(DataElement dataElement, Period period, OrganisationUnit organisationUnit);

  long getCount(DataSet dataSet, Period period, OrganisationUnit organisationUnit);

  boolean anyExists();
}
