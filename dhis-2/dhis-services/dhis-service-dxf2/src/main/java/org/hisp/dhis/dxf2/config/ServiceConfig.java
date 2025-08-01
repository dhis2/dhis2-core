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
package org.hisp.dhis.dxf2.config;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import com.google.common.base.Functions;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.CreationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DashboardCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DeletionCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.DuplicateIdsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.GeoJsonAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MandatoryAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.MetadataAttributeCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.NotOwnerReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ReferencesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SchemaCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.SecurityCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.TranslationsCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UidFormatCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniqueAttributesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniqueMultiPropertiesCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UniquenessCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.UpdateCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationCheck;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationHooksCheck;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.importexport.ImportStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Luciano Fiandesio
 */
@Configuration("dxf2ServiceConfig")
public class ServiceConfig {
  @Autowired
  @Qualifier("initialInterval")
  private ConfigurationPropertyFactoryBean initialInterval;

  @Autowired
  @Qualifier("maxAttempts")
  private ConfigurationPropertyFactoryBean maxAttempts;

  private final Map<Class<? extends ValidationCheck>, ValidationCheck> validationCheckByClass;

  public ServiceConfig(Collection<ValidationCheck> validationChecks) {
    validationCheckByClass = byClass(validationChecks);
  }

  @SuppressWarnings("unchecked")
  private <T> Map<Class<? extends T>, T> byClass(Collection<T> items) {
    return items.stream()
        .collect(Collectors.toMap(e -> (Class<? extends T>) e.getClass(), Functions.identity()));
  }

  @Bean
  public RetryTemplate retryTemplate() {
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

    backOffPolicy.setInitialInterval(Long.parseLong((String) initialInterval.getObject()));

    SimpleRetryPolicy simpleRetryPolicy =
        new SimpleRetryPolicy(Integer.parseInt((String) maxAttempts.getObject()));

    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.setRetryPolicy(simpleRetryPolicy);

    return retryTemplate;
  }

  @Bean
  public Map<ImportStrategy, List<ValidationCheck>> validatorsByImportStrategy() {
    return Map.of(
        CREATE_AND_UPDATE,
            newArrayList(
                getValidationCheckByClass(DuplicateIdsCheck.class),
                getValidationCheckByClass(ValidationHooksCheck.class),
                getValidationCheckByClass(SecurityCheck.class),
                getValidationCheckByClass(SchemaCheck.class),
                getValidationCheckByClass(UniquenessCheck.class),
                getValidationCheckByClass(UniqueMultiPropertiesCheck.class),
                getValidationCheckByClass(MandatoryAttributesCheck.class),
                getValidationCheckByClass(UniqueAttributesCheck.class),
                getValidationCheckByClass(ReferencesCheck.class),
                getValidationCheckByClass(NotOwnerReferencesCheck.class),
                getValidationCheckByClass(TranslationsCheck.class),
                getValidationCheckByClass(GeoJsonAttributesCheck.class),
                getValidationCheckByClass(MetadataAttributeCheck.class),
                getValidationCheckByClass(UidFormatCheck.class),
                getValidationCheckByClass(DashboardCheck.class)),
        CREATE,
            newArrayList(
                getValidationCheckByClass(DuplicateIdsCheck.class),
                getValidationCheckByClass(ValidationHooksCheck.class),
                getValidationCheckByClass(SecurityCheck.class),
                getValidationCheckByClass(CreationCheck.class),
                getValidationCheckByClass(SchemaCheck.class),
                getValidationCheckByClass(UniquenessCheck.class),
                getValidationCheckByClass(UniqueMultiPropertiesCheck.class),
                getValidationCheckByClass(MandatoryAttributesCheck.class),
                getValidationCheckByClass(UniqueAttributesCheck.class),
                getValidationCheckByClass(ReferencesCheck.class),
                getValidationCheckByClass(NotOwnerReferencesCheck.class),
                getValidationCheckByClass(TranslationsCheck.class),
                getValidationCheckByClass(GeoJsonAttributesCheck.class),
                getValidationCheckByClass(MetadataAttributeCheck.class),
                getValidationCheckByClass(UidFormatCheck.class),
                getValidationCheckByClass(DashboardCheck.class)),
        UPDATE,
            newArrayList(
                getValidationCheckByClass(DuplicateIdsCheck.class),
                getValidationCheckByClass(ValidationHooksCheck.class),
                getValidationCheckByClass(SecurityCheck.class),
                getValidationCheckByClass(UpdateCheck.class),
                getValidationCheckByClass(SchemaCheck.class),
                getValidationCheckByClass(UniquenessCheck.class),
                getValidationCheckByClass(UniqueMultiPropertiesCheck.class),
                getValidationCheckByClass(MandatoryAttributesCheck.class),
                getValidationCheckByClass(UniqueAttributesCheck.class),
                getValidationCheckByClass(ReferencesCheck.class),
                getValidationCheckByClass(NotOwnerReferencesCheck.class),
                getValidationCheckByClass(TranslationsCheck.class),
                getValidationCheckByClass(GeoJsonAttributesCheck.class),
                getValidationCheckByClass(MetadataAttributeCheck.class),
                getValidationCheckByClass(DashboardCheck.class)),
        DELETE,
            newArrayList(
                getValidationCheckByClass(SecurityCheck.class),
                getValidationCheckByClass(DeletionCheck.class)));
  }

  private ValidationCheck getValidationCheckByClass(
      Class<? extends ValidationCheck> validationCheckClass) {
    return getByClass(validationCheckByClass, validationCheckClass);
  }

  private <T> T getByClass(
      Map<Class<? extends T>, ? extends T> tByClass, Class<? extends T> clazz) {
    return Optional.ofNullable(tByClass.get(clazz))
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to find validator by class: " + clazz));
  }
}
