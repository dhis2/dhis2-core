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

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Registration;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Servlet 3.0+ {@link jakarta.servlet.Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @param <D> the dynamic registration result
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public abstract class DynamicRegistrationBean<D extends Registration.Dynamic>
    extends RegistrationBean implements BeanNameAware {

  private static final Log logger = LogFactory.getLog(RegistrationBean.class);

  private String name;

  private boolean asyncSupported = true;

  private Map<String, String> initParameters = new LinkedHashMap<>();

  private String beanName;

  private boolean ignoreRegistrationFailure;

  /**
   * Set the name of this registration. If not specified the bean name will be used.
   *
   * @param name the name of the registration
   */
  public void setName(String name) {
    Assert.hasLength(name, "Name must not be empty");
    this.name = name;
  }

  /**
   * Sets if asynchronous operations are supported for this registration. If not specified defaults
   * to {@code true}.
   *
   * @param asyncSupported if async is supported
   */
  public void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  /**
   * Returns if asynchronous operations are supported for this registration.
   *
   * @return if async is supported
   */
  public boolean isAsyncSupported() {
    return this.asyncSupported;
  }

  /**
   * Set init-parameters for this registration. Calling this method will replace any existing
   * init-parameters.
   *
   * @param initParameters the init parameters
   * @see #getInitParameters
   * @see #addInitParameter
   */
  public void setInitParameters(Map<String, String> initParameters) {
    Assert.notNull(initParameters, "InitParameters must not be null");
    this.initParameters = new LinkedHashMap<>(initParameters);
  }

  /**
   * Returns a mutable Map of the registration init-parameters.
   *
   * @return the init parameters
   */
  public Map<String, String> getInitParameters() {
    return this.initParameters;
  }

  /**
   * Add a single init-parameter, replacing any existing parameter with the same name.
   *
   * @param name the init-parameter name
   * @param value the init-parameter value
   */
  public void addInitParameter(String name, String value) {
    Assert.notNull(name, "Name must not be null");
    this.initParameters.put(name, value);
  }

  @Override
  protected final void register(String description, ServletContext servletContext) {
    D registration = addRegistration(description, servletContext);
    if (registration == null) {
      if (this.ignoreRegistrationFailure) {
        logger.info(
            StringUtils.capitalize(description)
                + " was not registered (possibly already registered?)");
        return;
      }
      throw new IllegalStateException(
          "Failed to register '%s' on the servlet context. Possibly already registered?"
              .formatted(description));
    }
    configure(registration);
  }

  /**
   * Sets whether registration failures should be ignored. If set to true, a failure will be logged.
   * If set to false, an {@link IllegalStateException} will be thrown.
   *
   * @param ignoreRegistrationFailure whether to ignore registration failures
   * @since 3.1.0
   */
  public void setIgnoreRegistrationFailure(boolean ignoreRegistrationFailure) {
    this.ignoreRegistrationFailure = ignoreRegistrationFailure;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  protected abstract D addRegistration(String description, ServletContext servletContext);

  protected void configure(D registration) {
    registration.setAsyncSupported(this.asyncSupported);
    if (!this.initParameters.isEmpty()) {
      registration.setInitParameters(this.initParameters);
    }
  }

  /**
   * Deduces the name for this registration. Will return user specified name or fallback to the bean
   * name. If the bean name is not available, convention based naming is used.
   *
   * @param value the object used for convention based names
   * @return the deduced name
   */
  protected final String getOrDeduceName(Object value) {
    if (this.name != null) {
      return this.name;
    }
    if (this.beanName != null) {
      return this.beanName;
    }
    return Conventions.getVariableName(value);
  }
}
