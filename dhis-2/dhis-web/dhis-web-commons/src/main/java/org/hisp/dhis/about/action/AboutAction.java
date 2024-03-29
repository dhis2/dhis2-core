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
package org.hisp.dhis.about.action;

import com.opensymphony.xwork2.Action;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Dang Duy Hieu
 */
public class AboutAction implements Action {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private SystemService systemService;

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private SystemInfo info;

  public SystemInfo getInfo() {
    return info;
  }

  private String userAgent;

  public String getUserAgent() {
    return userAgent;
  }

  private boolean currentUserIsSuper;

  public boolean getCurrentUserIsSuper() {
    return currentUserIsSuper;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    info = systemService.getSystemInfo();

    HttpServletRequest request = ServletActionContext.getRequest();

    userAgent = request.getHeader(ContextUtils.HEADER_USER_AGENT);

    currentUserIsSuper = CurrentUserUtil.getCurrentUserDetails().isSuper();

    return SUCCESS;
  }
}
