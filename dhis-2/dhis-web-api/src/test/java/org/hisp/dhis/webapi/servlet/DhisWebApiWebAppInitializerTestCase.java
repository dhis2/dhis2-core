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
package org.hisp.dhis.webapi.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRegistration;
import java.util.EnumSet;
import java.util.EventListener;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterRegistration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;

class DhisWebApiWebAppInitializerTestCase {

  @Test
  void testOnStartUpSetsDispatchTypesToRequestAndAsyncForOpenEntityManagerInViewFilter() {
    System.setProperty("dhis2.home", "src/test/resources");

    class DhisWebApiWebAppInitializerMockServletContext extends MockServletContext {
      EnumSet<DispatcherType> dispatcherTypes;

      @Override
      public <T extends EventListener> void addListener(T t) {}

      @Override
      public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return new MockFilterRegistration(filterName, filter.getClass().getName());
      }

      @Override
      public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return mock(ServletRegistration.Dynamic.class);
      }

      @Override
      public ServletRegistration.Dynamic addServlet(
          String servletName, Class<? extends Servlet> servletClass) {
        return mock(ServletRegistration.Dynamic.class);
      }

      @Override
      public FilterRegistration.Dynamic addFilter(
          String filterName, Class<? extends Filter> filterClass) {
        MockFilterRegistration mockFilterRegistration =
            new MockFilterRegistration(filterName, filterClass.getName()) {
              @Override
              public void addMappingForUrlPatterns(
                  EnumSet<DispatcherType> dispatcherTypes,
                  boolean isMatchAfter,
                  String... urlPatterns) {
                if (filterClass.equals(OpenEntityManagerInViewFilter.class)) {
                  DhisWebApiWebAppInitializerMockServletContext.this.dispatcherTypes =
                      dispatcherTypes;
                }
                super.addMappingForUrlPatterns(dispatcherTypes, isMatchAfter, urlPatterns);
              }
            };
        addFilterRegistration(mockFilterRegistration);
        return mockFilterRegistration;
      }
    }

    DhisWebApiWebAppInitializerMockServletContext mockServletContext =
        new DhisWebApiWebAppInitializerMockServletContext();
    new DhisWebApiWebAppInitializer().onStartup(mockServletContext);
    assertEquals(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC),
        mockServletContext.dispatcherTypes,
        "Dispatch type needs to include DispatcherType.ASYNC so that database connections for async requests are closed");
  }
}
