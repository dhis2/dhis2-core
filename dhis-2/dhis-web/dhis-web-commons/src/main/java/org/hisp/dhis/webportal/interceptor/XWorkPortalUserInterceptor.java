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
package org.hisp.dhis.webportal.interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

/**
 * @author Torgeir Lorange Ostby
 */
public class XWorkPortalUserInterceptor implements Interceptor {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 2809606672626282043L;

  private UserService userService;

  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void destroy() {}

  @Override
  public void init() {}

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    Map<String, Object> map = new HashMap<>(3);

    if (!CurrentUserUtil.hasCurrentUser()) {
      map.put("currentUsername", "no user");
      map.put("currentUser", null);
    } else {
      map.put("currentUsername", CurrentUserUtil.getCurrentUsername());
      User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
      map.put("currentUser", currentUser);
    }

    invocation.getStack().push(map);

    return invocation.invoke();
  }
}
