/*
 * Copyright (c) 2004-2023, University of Oslo
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

import javax.servlet.Filter;
import org.springframework.util.Assert;

/**
 * A {@link ServletContextInitializer} to register {@link Filter}s in a Servlet 3.0+ container.
 * Similar to the {@link ServletContext#addFilter(String, Filter) registration} features provided by
 * {@link ServletContext} but with a Spring Bean friendly design.
 *
 * <p>The {@link #setFilter(Filter) Filter} must be specified before calling {@link
 * #onStartup(ServletContext)}. Registrations can be associated with {@link #setUrlPatterns URL
 * patterns} and/or servlets (either by {@link #setServletNames name} or through a {@link
 * #setServletRegistrationBeans ServletRegistrationBean}s). When no URL pattern or servlets are
 * specified the filter will be associated to '/*'. The filter name will be deduced if not
 * specified.
 *
 * @param <T> the type of {@link Filter} to register
 * @author Phillip Webb
 * @since 1.4.0
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 * @see DelegatingFilterProxyRegistrationBean
 */
public class FilterRegistrationBean<T extends Filter> extends AbstractFilterRegistrationBean<T> {

  private T filter;

  /** Create a new {@link FilterRegistrationBean} instance. */
  public FilterRegistrationBean() {}

  /**
   * Create a new {@link FilterRegistrationBean} instance to be registered with the specified {@link
   * ServletRegistrationBean}s.
   *
   * @param filter the filter to register
   * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   */
  public FilterRegistrationBean(T filter, ServletRegistrationBean<?>... servletRegistrationBeans) {
    super(servletRegistrationBeans);
    Assert.notNull(filter, "Filter must not be null");
    this.filter = filter;
  }

  @Override
  public T getFilter() {
    return this.filter;
  }

  /**
   * Set the filter to be registered.
   *
   * @param filter the filter
   */
  public void setFilter(T filter) {
    Assert.notNull(filter, "Filter must not be null");
    this.filter = filter;
  }
}
