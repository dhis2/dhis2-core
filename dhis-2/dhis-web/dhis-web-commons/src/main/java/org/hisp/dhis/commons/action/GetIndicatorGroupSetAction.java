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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.user.CurrentUserUtil;

/**
 * @author Tran Thanh Tri
 */
public class GetIndicatorGroupSetAction extends BaseAction implements Action {

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private IndicatorService indicatorService;

  public void setIndicatorService(IndicatorService indicatorService) {
    this.indicatorService = indicatorService;
  }

  // -------------------------------------------------------------------------
  // Input & Output
  // -------------------------------------------------------------------------

  private Integer id;

  public void setId(Integer id) {
    this.id = id;
  }

  private IndicatorGroupSet indicatorGroupSet;

  public IndicatorGroupSet getIndicatorGroupSet() {
    return indicatorGroupSet;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() {
    canReadType(IndicatorGroupSet.class);

    if (id != null) {
      indicatorGroupSet = indicatorService.getIndicatorGroupSet(id);
    }

    canReadInstance(indicatorGroupSet, CurrentUserUtil.getCurrentUserDetails());

    return SUCCESS;
  }
}
