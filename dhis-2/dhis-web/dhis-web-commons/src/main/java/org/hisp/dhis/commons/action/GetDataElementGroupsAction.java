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
package org.hisp.dhis.commons.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Tran Thanh Tri
 * @author mortenoh
 */
public class GetDataElementGroupsAction extends ActionPagingSupport<DataElementGroup> {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private DataElementService dataElementService;

  public void setDataElementService(DataElementService dataElementService) {
    this.dataElementService = dataElementService;
  }

  // -------------------------------------------------------------------------
  // Input & output
  // -------------------------------------------------------------------------

  private String key;

  public void setKey(String key) {
    this.key = key;
  }

  private List<DataElementGroup> dataElementGroups;

  public List<DataElementGroup> getDataElementGroups() {
    return dataElementGroups;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    canReadType(DataElementGroup.class);

    dataElementGroups = new ArrayList<>(dataElementService.getAllDataElementGroups());

    if (key != null) {
      dataElementGroups = IdentifiableObjectUtils.filterNameByKey(dataElementGroups, key, true);
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    dataElementGroups.forEach(instance -> canReadInstance(instance, currentUserDetails));

    Collections.sort(dataElementGroups);

    if (usePaging) {
      this.paging = createPaging(dataElementGroups.size());

      dataElementGroups = dataElementGroups.subList(paging.getStartPos(), paging.getEndPos());
    }

    return SUCCESS;
  }
}
