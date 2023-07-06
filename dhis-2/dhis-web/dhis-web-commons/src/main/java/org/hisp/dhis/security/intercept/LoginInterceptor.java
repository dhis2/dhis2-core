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
package org.hisp.dhis.security.intercept;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.struts2.ServletActionContext;

/**
 * Interceptor that will run a list of actions when the user first logins.
 *
 * @author mortenoh
 */
@Slf4j
public class LoginInterceptor implements Interceptor {
  private static final long serialVersionUID = -5376334780350610573L;

  public static final String JLI_SESSION_VARIABLE = "JLI";

  private List<Action> actions = new ArrayList<>();

  /**
   * @param actions List of actions to run on login.
   */
  public void setActions(List<Action> actions) {
    this.actions = actions;
  }

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    Boolean jli =
        (Boolean)
            ServletActionContext.getRequest()
                .getSession()
                .getAttribute(LoginInterceptor.JLI_SESSION_VARIABLE);

    if (jli != null) {
      log.debug("JLI marker is present. Running " + actions.size() + " JLI actions.");

      for (Action a : actions) {
        a.execute();
      }

      ServletActionContext.getRequest()
          .getSession()
          .removeAttribute(LoginInterceptor.JLI_SESSION_VARIABLE);
    }

    return invocation.invoke();
  }

  @Override
  public void destroy() {}

  @Override
  public void init() {}
}
