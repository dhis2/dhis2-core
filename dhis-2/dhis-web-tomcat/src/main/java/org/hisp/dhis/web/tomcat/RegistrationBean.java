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
package org.hisp.dhis.web.tomcat;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Base class for Servlet 3.0+ based registration beans.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see ServletRegistrationBean
 * @see FilterRegistrationBean
 * @see DelegatingFilterProxyRegistrationBean
 * @see ServletListenerRegistrationBean
 */
public abstract class RegistrationBean implements ServletContextInitializer, Ordered {

  private static final Log logger = LogFactory.getLog(RegistrationBean.class);

  private int order = Ordered.LOWEST_PRECEDENCE;

  private boolean enabled = true;

  @Override
  public final void onStartup(ServletContext servletContext) throws ServletException {
    String description = getDescription();
    if (!isEnabled()) {
      logger.info(StringUtils.capitalize(description) + " was not registered (disabled)");
      return;
    }
    register(description, servletContext);
  }

  /**
   * Return a description of the registration. For example "Servlet resourceServlet"
   *
   * @return a description of the registration
   */
  protected abstract String getDescription();

  /**
   * Register this bean with the servlet context.
   *
   * @param description a description of the item being registered
   * @param servletContext the servlet context
   */
  protected abstract void register(String description, ServletContext servletContext);

  /**
   * Flag to indicate that the registration is enabled.
   *
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Return if the registration is enabled.
   *
   * @return if enabled (default {@code true})
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Set the order of the registration bean.
   *
   * @param order the order
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Get the order of the registration bean.
   *
   * @return the order
   */
  @Override
  public int getOrder() {
    return this.order;
  }
}
