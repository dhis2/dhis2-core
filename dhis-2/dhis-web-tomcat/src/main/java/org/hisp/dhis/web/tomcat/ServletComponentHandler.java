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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Abstract base class for handlers of Servlet components discovered through classpath scanning.
 *
 * @author Andy Wilkinson
 */
abstract class ServletComponentHandler {

  private final Class<? extends Annotation> annotationType;

  private final TypeFilter typeFilter;

  protected ServletComponentHandler(Class<? extends Annotation> annotationType) {
    this.typeFilter = new AnnotationTypeFilter(annotationType);
    this.annotationType = annotationType;
  }

  TypeFilter getTypeFilter() {
    return this.typeFilter;
  }

  protected String[] extractUrlPatterns(Map<String, Object> attributes) {
    String[] value = (String[]) attributes.get("value");
    String[] urlPatterns = (String[]) attributes.get("urlPatterns");
    if (urlPatterns.length > 0) {
      Assert.state(
          value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
      return urlPatterns;
    }
    return value;
  }

  protected final Map<String, String> extractInitParameters(Map<String, Object> attributes) {
    Map<String, String> initParameters = new HashMap<>();
    for (AnnotationAttributes initParam : (AnnotationAttributes[]) attributes.get("initParams")) {
      String name = (String) initParam.get("name");
      String value = (String) initParam.get("value");
      initParameters.put(name, value);
    }
    return initParameters;
  }

  void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
    Map<String, Object> attributes =
        beanDefinition.getMetadata().getAnnotationAttributes(this.annotationType.getName());
    if (attributes != null) {
      doHandle(attributes, beanDefinition, registry);
    }
  }

  protected abstract void doHandle(
      Map<String, Object> attributes,
      AnnotatedBeanDefinition beanDefinition,
      BeanDefinitionRegistry registry);
}
