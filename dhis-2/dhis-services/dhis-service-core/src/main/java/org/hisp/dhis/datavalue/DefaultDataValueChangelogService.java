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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@RequiredArgsConstructor
@Service
public class DefaultDataValueChangelogService implements DataValueChangelogService {

  /**
   * The most entries a user who is not a superuser reads with one request: unpaged, or up to the
   * end of a page. The access checked statement reads all entries before the page.
   */
  static final int MAX_ENTRIES = 10_000;

  private final DataValueChangelogStore dataValueChangelogStore;

  @Override
  @Transactional
  public void deleteByOrgUnit(@Nonnull UID orgUnit) {
    dataValueChangelogStore.deleteByOrgUnit(orgUnit);
  }

  @Override
  @Transactional
  public void deleteByDataElement(@Nonnull UID dataElement) {
    dataValueChangelogStore.deleteByDataElement(dataElement);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueChangelogEntry> getChangelogEntries(DataExportValue dataValue) {
    return dataValueChangelogStore.getEntries(dataValue.toKey());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueChangelogEntry> getChangelogEntries(@Nonnull DataValueQueryParams params)
      throws ForbiddenException {
    // the store only returns entries the user may read, so only no entries need the check
    List<DataValueChangelogEntry> entries = dataValueChangelogStore.getEntries(params);
    if (entries.isEmpty() && !dataValueChangelogStore.isReadable(params))
      throw new ForbiddenException(ErrorCode.E2050, params.getDe(), params.getPe(), params.getOu());
    return entries;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueChangelog> getChangelogEntries(DataValueChangelogQueryParams params)
      throws ConflictException {
    validateDataSetAccess(params);
    if (getCurrentUserDetails().isSuper()) return dataValueChangelogStore.getEntries(params);
    Pager pager = params.getPager();
    if (pager != null) {
      if (pager.getOffset() + pager.getPageSize() > MAX_ENTRIES)
        throw new ConflictException(ErrorCode.E2052, MAX_ENTRIES);
      return dataValueChangelogStore.getEntries(params);
    }
    // the unpaged read is bounded: the store reads one more entry than the page, which tells
    // there are more than allowed
    List<DataValueChangelog> entries =
        dataValueChangelogStore.getEntries(withPage(params, MAX_ENTRIES));
    if (entries.size() > MAX_ENTRIES) throw new ConflictException(ErrorCode.E2051, MAX_ENTRIES);
    return entries;
  }

  private static DataValueChangelogQueryParams withPage(
      DataValueChangelogQueryParams params, int pageSize) {
    return new DataValueChangelogQueryParams()
        .setDataSets(params.getDataSets())
        .setDataElements(params.getDataElements())
        .setPeriods(params.getPeriods())
        .setOrgUnits(params.getOrgUnits())
        .setCategoryOptionCombo(params.getCategoryOptionCombo())
        .setAttributeOptionCombo(params.getAttributeOptionCombo())
        .setTypes(params.getTypes())
        .setPager(new Pager(1, pageSize, pageSize));
  }

  @Override
  @Transactional(readOnly = true)
  public int countEntries(DataValueChangelogQueryParams params) throws ConflictException {
    validateDataSetAccess(params);
    return dataValueChangelogStore.countEntries(params);
  }

  private void validateDataSetAccess(DataValueChangelogQueryParams params)
      throws ConflictException {
    if (params.getDataSets() == null || params.getDataSets().isEmpty()) return;
    List<String> noAccess =
        dataValueChangelogStore.getDataSetsNoDataReadAccess(params.getDataSets());
    if (!noAccess.isEmpty()) throw new ConflictException(ErrorCode.E2010, noAccess);
  }
}
