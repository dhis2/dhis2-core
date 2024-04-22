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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via package
 * scanning.
 *
 * @author Andy Wilkinson
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 */
class ServletComponentRegisteringPostProcessor
    implements BeanFactoryPostProcessor, ApplicationContextAware {

  //	--BeanFactoryInitializationAotProcessor

  private static final List<ServletComponentHandler> HANDLERS;

  static {
    List<ServletComponentHandler> servletComponentHandlers = new ArrayList<>();
    servletComponentHandlers.add(new WebServletHandler());
    servletComponentHandlers.add(new WebFilterHandler());
    servletComponentHandlers.add(new WebListenerHandler());
    HANDLERS = Collections.unmodifiableList(servletComponentHandlers);
  }

  private final Set<String> packagesToScan;

  private ApplicationContext applicationContext;

  ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    if (isRunningInEmbeddedWebServer()) {
      ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
      for (String packageToScan : this.packagesToScan) {
        scanPackage(componentProvider, packageToScan);
      }
    }
  }

  private void scanPackage(
      ClassPathScanningCandidateComponentProvider componentProvider, String packageToScan) {
    for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {
      if (candidate instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
        for (ServletComponentHandler handler : HANDLERS) {
          handler.handle(annotatedBeanDefinition, (BeanDefinitionRegistry) this.applicationContext);
        }
      }
    }
  }

  private boolean isRunningInEmbeddedWebServer() {
    return this.applicationContext instanceof WebApplicationContext webApplicationContext
        && webApplicationContext.getServletContext() == null;
  }

  private ClassPathScanningCandidateComponentProvider createComponentProvider() {
    ClassPathScanningCandidateComponentProvider componentProvider =
        new ClassPathScanningCandidateComponentProvider(false);
    componentProvider.setEnvironment(this.applicationContext.getEnvironment());
    componentProvider.setResourceLoader(this.applicationContext);
    for (ServletComponentHandler handler : HANDLERS) {
      componentProvider.addIncludeFilter(handler.getTypeFilter());
    }
    return componentProvider;
  }

  Set<String> getPackagesToScan() {
    return Collections.unmodifiableSet(this.packagesToScan);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  //	@Override
  //	public BeanFactoryInitializationAotContribution
  // processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
  //		return new BeanFactoryInitializationAotContribution() {
  //
  //			@Override
  //			public void applyTo(GenerationContext generationContext,
  //					BeanFactoryInitializationCode beanFactoryInitializationCode) {
  //				for (String beanName : beanFactory.getBeanDefinitionNames()) {
  //					BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
  //					if (Objects.equals(definition.getBeanClassName(),
  //							WebListenerHandler.ServletComponentWebListenerRegistrar.class.getName())) {
  //						String listenerClassName = (String) definition.getConstructorArgumentValues()
  //							.getArgumentValue(0, String.class)
  //							.getValue();
  //						generationContext.getRuntimeHints()
  //							.reflection()
  //							.registerType(TypeReference.of(listenerClassName),
  //									MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  //					}
  //				}
  //			}
  //
  //		};
  //	}

}
