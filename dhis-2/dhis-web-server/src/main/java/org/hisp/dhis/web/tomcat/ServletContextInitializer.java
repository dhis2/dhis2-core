/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.web.tomcat;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>Interface used to configure a Servlet 3.0+ {@link ServletContext context} programmatically.
 * Unlike {@link WebApplicationInitializer}, classes that implement this interface (and do not
 * implement {@link WebApplicationInitializer}) will <b>not</b> be detected by {@link
 * SpringServletContainerInitializer} and hence will not be automatically bootstrapped by the
 * Servlet container.
 *
 * <p>For configuration examples see {@link WebApplicationInitializer}.
 *
 * @author Phillip Webb
 * @see WebApplicationInitializer
 */
@FunctionalInterface
public interface ServletContextInitializer {

  /**
   * Configure the given {@link ServletContext} with any servlets, filters, listeners context-params
   * and attributes necessary for initialization.
   *
   * @param servletContext the {@code ServletContext} to initialize
   * @throws ServletException if any call against the given {@code ServletContext} throws a {@code
   *     ServletException}
   */
  void onStartup(ServletContext servletContext) throws ServletException;
}
