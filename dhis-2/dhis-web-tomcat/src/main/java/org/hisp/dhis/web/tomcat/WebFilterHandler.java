/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * Handler for {@link WebFilter @WebFilter}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebFilterHandler extends ServletComponentHandler {

  WebFilterHandler() {
    super(WebFilter.class);
  }

  @Override
  public void doHandle(
      Map<String, Object> attributes,
      AnnotatedBeanDefinition beanDefinition,
      BeanDefinitionRegistry registry) {
    BeanDefinitionBuilder builder =
        BeanDefinitionBuilder.rootBeanDefinition(FilterRegistrationBean.class);
    builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
    builder.addPropertyValue("dispatcherTypes", extractDispatcherTypes(attributes));
    builder.addPropertyValue("filter", beanDefinition);
    builder.addPropertyValue("initParameters", extractInitParameters(attributes));
    String name = determineName(attributes, beanDefinition);
    builder.addPropertyValue("name", name);
    builder.addPropertyValue("servletNames", attributes.get("servletNames"));
    builder.addPropertyValue("urlPatterns", extractUrlPatterns(attributes));
    registry.registerBeanDefinition(name, builder.getBeanDefinition());
  }

  private EnumSet<DispatcherType> extractDispatcherTypes(Map<String, Object> attributes) {
    DispatcherType[] dispatcherTypes = (DispatcherType[]) attributes.get("dispatcherTypes");
    if (dispatcherTypes.length == 0) {
      return EnumSet.noneOf(DispatcherType.class);
    }
    if (dispatcherTypes.length == 1) {
      return EnumSet.of(dispatcherTypes[0]);
    }
    return EnumSet.of(
        dispatcherTypes[0], Arrays.copyOfRange(dispatcherTypes, 1, dispatcherTypes.length));
  }

  private String determineName(Map<String, Object> attributes, BeanDefinition beanDefinition) {
    return (String)
        (StringUtils.hasText((String) attributes.get("filterName"))
            ? attributes.get("filterName")
            : beanDefinition.getBeanClassName());
  }
}
