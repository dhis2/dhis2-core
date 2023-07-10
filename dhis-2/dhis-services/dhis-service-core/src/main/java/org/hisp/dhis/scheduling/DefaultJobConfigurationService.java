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
package org.hisp.dhis.scheduling;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hisp.dhis.scheduling.JobType.values;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.schema.Property;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@RequiredArgsConstructor
@Service("jobConfigurationService")
public class DefaultJobConfigurationService implements JobConfigurationService {
  @Qualifier("org.hisp.dhis.scheduling.JobConfigurationStore")
  private final IdentifiableObjectStore<JobConfiguration> jobConfigurationStore;

  @Override
  @Transactional
  public long addJobConfiguration(JobConfiguration jobConfiguration) {
    if (!jobConfiguration.isInMemoryJob()) {
      jobConfigurationStore.save(jobConfiguration);
    }

    return jobConfiguration.getId();
  }

  @Override
  @Transactional
  public void addJobConfigurations(List<JobConfiguration> jobConfigurations) {
    jobConfigurations.forEach(this::addJobConfiguration);
  }

  @Override
  @Transactional
  public long updateJobConfiguration(JobConfiguration jobConfiguration) {
    if (!jobConfiguration.isInMemoryJob()) {
      jobConfigurationStore.update(jobConfiguration);
    }

    return jobConfiguration.getId();
  }

  @Override
  @Transactional
  public void deleteJobConfiguration(JobConfiguration jobConfiguration) {
    if (!jobConfiguration.isInMemoryJob()) {
      JobConfiguration existing = jobConfigurationStore.loadByUid(jobConfiguration.getUid());
      jobConfigurationStore.delete(existing);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public JobConfiguration getJobConfigurationByUid(String uid) {
    return jobConfigurationStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public JobConfiguration getJobConfiguration(long jobId) {
    return jobConfigurationStore.get(jobId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobConfiguration> getAllJobConfigurations() {
    return jobConfigurationStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobConfiguration> getJobConfigurations(JobType type) {
    return jobConfigurationStore.getAll().stream()
        .filter(config -> config.getJobType() == type)
        .collect(toUnmodifiableList());
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Map<String, Property>> getJobParametersSchema() {
    Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

    for (JobType jobType : values()) {
      if (!jobType.isConfigurable()) {
        continue;
      }

      Map<String, Property> jobParameters =
          Maps.uniqueIndex(getJobParameters(jobType), Property::getName);

      propertyMap.put(jobType.name(), jobParameters);
    }

    return propertyMap;
  }

  @Override
  public List<JobTypeInfo> getJobTypeInfo() {
    List<JobTypeInfo> jobTypes = new ArrayList<>();

    for (JobType jobType : values()) {
      if (!jobType.isConfigurable()) {
        continue;
      }

      String name = TextUtils.getPrettyEnumName(jobType);

      List<Property> jobParameters = getJobParameters(jobType);

      JobTypeInfo info = new JobTypeInfo(name, jobType, jobParameters);

      jobTypes.add(info);
    }

    return jobTypes;
  }

  @Override
  @Transactional
  public void refreshScheduling(JobConfiguration jobConfiguration) {
    if (jobConfiguration.isEnabled()) {
      jobConfiguration.setJobStatus(JobStatus.SCHEDULED);
    } else {
      jobConfiguration.setJobStatus(JobStatus.DISABLED);
    }

    jobConfigurationStore.update(jobConfiguration);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Returns a list of job parameters for the given job type.
   *
   * @param jobType the {@link JobType}.
   * @return a list of {@link Property}.
   */
  private List<Property> getJobParameters(JobType jobType) {
    List<Property> jobParameters = new ArrayList<>();

    Class<?> paramsType = jobType.getJobParameters();

    if (paramsType == null) {
      return jobParameters;
    }

    final Set<PropertyDescriptor> properties =
        Stream.of(PropertyUtils.getPropertyDescriptors(paramsType))
            .filter(pd -> pd.getReadMethod() != null && pd.getWriteMethod() != null)
            .collect(Collectors.toSet());

    for (Field field : paramsType.getDeclaredFields()) {
      PropertyDescriptor descriptor =
          properties.stream()
              .filter(pd -> pd.getName().equals(field.getName()))
              .findFirst()
              .orElse(null);
      if (isProperty(field, descriptor)) {
        jobParameters.add(getProperty(jobType, paramsType, field));
      }
    }

    return jobParameters;
  }

  private boolean isProperty(Field field, PropertyDescriptor descriptor) {
    return !(descriptor == null
        || (descriptor.getReadMethod().getAnnotation(JsonProperty.class) == null
            && field.getAnnotation(JsonProperty.class) == null));
  }

  private static Property getProperty(JobType jobType, Class<?> paramsType, Field field) {
    Class<?> valueType = field.getType();
    Property property = new Property(Primitives.wrap(valueType), null, null);
    property.setName(field.getName());
    property.setFieldName(TextUtils.getPrettyPropertyName(field.getName()));

    try {
      field.setAccessible(true);
      property.setDefaultValue(field.get(jobType.getJobParameters().newInstance()));
    } catch (IllegalAccessException | InstantiationException e) {
      log.error(
          "Fetching default value for JobParameters properties failed for property: "
              + field.getName(),
          e);
    }

    String relativeApiElements =
        jobType.getRelativeApiElements() != null
            ? jobType.getRelativeApiElements().get(field.getName())
            : "";

    if (relativeApiElements != null && !relativeApiElements.equals("")) {
      property.setRelativeApiEndpoint(relativeApiElements);
    }

    if (Collection.class.isAssignableFrom(valueType)) {
      return setPropertyIfCollection(property, field, paramsType);
    }
    if (valueType.isEnum()) {
      property.setConstants(getConstants(valueType));
    }
    return property;
  }

  private static Property setPropertyIfCollection(Property property, Field field, Class<?> klass) {
    property.setCollection(true);
    property.setCollectionName(field.getName());

    Type type = field.getGenericType();

    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Class<?> itemKlass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
      property.setItemKlass(itemKlass);

      property.setIdentifiableObject(IdentifiableObject.class.isAssignableFrom(itemKlass));
      property.setNameableObject(NameableObject.class.isAssignableFrom(itemKlass));
      property.setEmbeddedObject(EmbeddedObject.class.isAssignableFrom(klass));
      property.setAnalyticalObject(AnalyticalObject.class.isAssignableFrom(klass));
      if (itemKlass.isEnum()) {
        property.setConstants(getConstants(itemKlass));
      }
    }
    return property;
  }

  private static List<String> getConstants(Class<?> enumType) {
    return Arrays.stream(enumType.getEnumConstants())
        .map(e -> ((Enum<?>) e).name())
        .collect(Collectors.toList());
  }
}
