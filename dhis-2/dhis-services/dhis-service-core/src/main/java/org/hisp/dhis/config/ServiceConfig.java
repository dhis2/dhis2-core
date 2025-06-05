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
package org.hisp.dhis.config;

import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.i18n.ui.resourcebundle.DefaultResourceBundleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.log.TimeExecution;
import org.hisp.dhis.log.TimeExecutionInterceptor;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.DefaultOutboundMessageBatchService;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

/**
 * @author Luciano Fiandesio
 */
@Configuration("coreServiceConfig")
public class ServiceConfig implements BeanDefinitionRegistryPostProcessor {

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
      throws BeansException {
    // Note: only the spring gods will know why I have to do this explicitly
    // an Advisor should be ROLE_INFRASTRUCTURE just based on the class but
    // this needed manual override, otherwise the bean is ignored
    registry.getBeanDefinition("timingAdvisor").setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

  @Bean
  public Advisor timingAdvisor() {
    Pointcut pointcut = new StaticAnnotationPointcut(Service.class, TimeExecution.class);
    return new DefaultPointcutAdvisor(pointcut, new TimeExecutionInterceptor());
  }

  @Bean("taskScheduler")
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(25);
    return threadPoolTaskScheduler;
  }

  @Bean("org.hisp.dhis.outboundmessage.OutboundMessageService")
  public DefaultOutboundMessageBatchService defaultOutboundMessageBatchService(
      @Qualifier("smsMessageSender") MessageSender smsMessageSender,
      @Qualifier("emailMessageSender") MessageSender emailMessageSender) {
    Map<DeliveryChannel, MessageSender> channels = new HashMap<>();
    channels.put(DeliveryChannel.SMS, smsMessageSender);
    channels.put(DeliveryChannel.EMAIL, emailMessageSender);

    DefaultOutboundMessageBatchService service = new DefaultOutboundMessageBatchService();

    service.setMessageSenders(channels);

    return service;
  }

  @Bean("org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager")
  public ResourceBundleManager resourceBundleManager() {
    return new DefaultResourceBundleManager();
  }
}
