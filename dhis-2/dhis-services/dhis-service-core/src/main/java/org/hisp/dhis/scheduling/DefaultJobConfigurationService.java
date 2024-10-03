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

import static org.hisp.dhis.jsontree.JsonBuilder.createArray;
import static org.hisp.dhis.scheduling.JobType.TRACKER_IMPORT_JOB;
import static org.hisp.dhis.scheduling.JobType.values;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.scheduling.JobType.Defaults;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultJobConfigurationService implements JobConfigurationService {

  private final JobConfigurationStore jobConfigurationStore;
  private final FileResourceService fileResourceService;
  private final SystemSettingManager systemSettings;
  private final JobCreationHelper jobCreationHelper;

  @Override
  public String create(JobConfiguration config) throws ConflictException {
    return jobCreationHelper.create(config);
  }

  @Override
  public String create(JobConfiguration config, MimeType contentType, InputStream content)
      throws ConflictException {
    return jobCreationHelper.create(config, contentType, content);
  }

  @Override
  @Transactional
  public int createDefaultJobs() {
    int created = 0;
    Set<String> jobIds = jobConfigurationStore.getAllIds();
    for (JobType t : JobType.values()) {
      Defaults defaults = t.getDefaults();
      if (defaults != null && !jobIds.contains(defaults.uid())) {
        createDefaultJob(t);
        created++;
      }
    }
    return created;
  }

  @Override
  @Transactional
  public void createDefaultJob(JobType type, UserDetails actingUser) {
    Defaults job = type.getDefaults();
    if (job == null) return;
    JobConfiguration config = new JobConfiguration(job.name(), type);
    config.setCronExpression(job.cronExpression());
    config.setDelay(job.delay());
    config.setUid(job.uid());
    config.setSchedulingType(
        job.delay() != null ? SchedulingType.FIXED_DELAY : SchedulingType.CRON);
    jobConfigurationStore.save(config, actingUser, false);
  }

  @Override
  @Transactional
  public void createDefaultJob(JobType type) {
    createDefaultJob(type, CurrentUserUtil.getCurrentUserDetails());
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String createInTransaction(
      JobConfiguration jobConfiguration, MimeType contentType, InputStream content)
      throws ConflictException, NotFoundException {
    String jobId = jobCreationHelper.create(jobConfiguration, contentType, content);

    if (!jobConfigurationStore.executeNow(jobId)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) throw new NotFoundException(JobConfiguration.class, jobId);
      if (job.getJobStatus() == JobStatus.RUNNING)
        throw new ConflictException("Job is already running.");
      if (job.getSchedulingType() == SchedulingType.ONCE_ASAP && job.getLastFinished() != null)
        throw new ConflictException("Job did already run once.");
      throw new ConflictException("Failed to transition job into ONCE_ASAP state.");
    }
    return jobId;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String createInTransaction(JobConfiguration jobConfiguration)
      throws ConflictException, NotFoundException {
    String jobId = jobCreationHelper.create(jobConfiguration);

    if (!jobConfigurationStore.executeNow(jobId)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) throw new NotFoundException(JobConfiguration.class, jobId);
      if (job.getJobStatus() == JobStatus.RUNNING)
        throw new ConflictException("Job is already running.");
      if (job.getSchedulingType() == SchedulingType.ONCE_ASAP && job.getLastFinished() != null)
        throw new ConflictException("Job did already run once.");
      throw new ConflictException("Failed to transition job into ONCE_ASAP state.");
    }
    return jobId;
  }

  @Override
  @Transactional
  public int updateDisabledJobs() {
    return jobConfigurationStore.updateDisabledJobs();
  }

  @Override
  @Transactional
  public int deleteFinishedJobs(int ttlMinutes) {
    if (ttlMinutes <= 0) {
      ttlMinutes = systemSettings.getIntSetting(SettingKey.JOBS_CLEANUP_AFTER_MINUTES);
    }
    return jobConfigurationStore.deleteFinishedJobs(ttlMinutes);
  }

  @Override
  @Transactional
  public int rescheduleStaleJobs(int timeoutMinutes) {
    if (timeoutMinutes <= 0) {
      timeoutMinutes = systemSettings.getIntSetting(SettingKey.JOBS_RESCHEDULE_STALE_FOR_MINUTES);
    }
    return jobConfigurationStore.rescheduleStaleJobs(timeoutMinutes);
  }

  @Override
  @Transactional
  public long addJobConfiguration(JobConfiguration jobConfiguration) {
    jobConfigurationStore.save(jobConfiguration);
    return jobConfiguration.getId();
  }

  @Override
  @Transactional
  public long updateJobConfiguration(JobConfiguration jobConfiguration) {
    jobConfigurationStore.update(jobConfiguration);
    return jobConfiguration.getId();
  }

  @Override
  @Transactional
  public void deleteJobConfiguration(JobConfiguration jobConfiguration) {
    jobConfigurationStore.delete(jobConfigurationStore.loadByUid(jobConfiguration.getUid()));
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
    return jobConfigurationStore.getJobConfigurations(type);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobConfiguration> getDueJobConfigurations(
      int dueInNextSeconds, boolean includeWaiting) {
    Instant now = Instant.now();
    Instant endOfWindow = now.plusSeconds(dueInNextSeconds);
    Duration maxCronDelay =
        Duration.ofHours(systemSettings.getIntSetting(SettingKey.JOBS_MAX_CRON_DELAY_HOURS));
    return jobConfigurationStore
        .getDueJobConfigurations(includeWaiting)
        .filter(c -> c.isDueBetween(now, endOfWindow, maxCronDelay))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobConfiguration> getStaleConfigurations(int staleForSeconds) {
    if (staleForSeconds <= 0) {
      staleForSeconds =
          60 * systemSettings.getIntSetting(SettingKey.JOBS_RESCHEDULE_STALE_FOR_MINUTES);
    }
    return jobConfigurationStore.getStaleConfigurations(staleForSeconds);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<JobRunErrors> findJobRunErrors(@Nonnull JobRunErrorsParams params) {
    return jobConfigurationStore
        .findJobRunErrors(params)
        .map(json -> errorEntryWithMessages(json, params))
        .toList();
  }

  private JobRunErrors errorEntryWithMessages(String json, JobRunErrorsParams params) {
    JsonObject entry = JsonMixed.of(json);
    List<JsonNode> flatErrors = new ArrayList<>();
    JobType type = entry.getString("type").parsed(JobType::valueOf);
    String fileResourceId = entry.getString("file").string();
    JsonObject errors = entry.getObject("errors");
    String errorCodeNamespace =
        type == TRACKER_IMPORT_JOB
            ? ValidationCode.class.getSimpleName()
            : ErrorCode.class.getSimpleName();
    errors
        .node()
        .members()
        .forEach(
            byObject ->
                byObject
                    .getValue()
                    .members()
                    .forEach(
                        byCode ->
                            byCode
                                .getValue()
                                .elements()
                                .forEach(error -> flatErrors.add(errorWithMessage(type, error)))));

    return JsonMixed.of(
            errors
                .node()
                .replaceWith(createArray(arr -> flatErrors.forEach(arr::addElement)))
                .addMembers(
                    obj ->
                        obj.addString("codes", errorCodeNamespace)
                            .addMember("input", getJobInput(params, fileResourceId))))
        .as(JobRunErrors.class);
  }

  private JsonNode getJobInput(JobRunErrorsParams params, String fileResourceId) {
    if (!params.isIncludeInput()) return JsonNode.of("null");
    try {
      FileResource fr = fileResourceService.getFileResource(fileResourceId);
      if (fr == null || fr.getStorageStatus() != FileResourceStorageStatus.STORED)
        return JsonNode.NULL;
      byte[] bytes = fileResourceService.copyFileResourceContent(fr);
      return JsonNode.of(new String(bytes, StandardCharsets.UTF_8));
    } catch (Exception ex) {
      log.warn("Could not copy file content to error info for file: " + fileResourceId, ex);
      return JsonNode.of("\"" + ex.getMessage() + "\"");
    }
  }

  private static JsonNode errorWithMessage(JobType type, JsonNode error) {
    String codeName = JsonMixed.of(error).getString("code").string();
    String template =
        type == TRACKER_IMPORT_JOB
            ? ValidationCode.valueOf(codeName).getMessage()
            : ErrorCode.valueOf(codeName).getMessage();
    Object[] args = JsonMixed.of(error).getArray("args").stringValues().toArray(new String[0]);
    String msg = MessageFormat.format(template, args);
    return error.extract().addMembers(e -> e.addString("message", msg));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Map<String, Property>> getJobParametersSchema() {
    Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

    for (JobType jobType : values()) {
      if (!jobType.isUserDefined()) {
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
      if (!jobType.isUserDefined()) {
        continue;
      }

      String name = TextUtils.getPrettyEnumName(jobType);

      List<Property> jobParameters = getJobParameters(jobType);

      JobTypeInfo info = new JobTypeInfo(name, jobType, jobParameters);

      jobTypes.add(info);
    }

    return jobTypes;
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
      JsonProperty property = getJsonProperty(field, descriptor);
      if (property != null) {
        jobParameters.add(getProperty(jobType, paramsType, field, property));
      }
    }

    return jobParameters;
  }

  @CheckForNull
  private JsonProperty getJsonProperty(Field field, PropertyDescriptor descriptor) {
    JsonProperty property = field.getAnnotation(JsonProperty.class);
    if (property != null) return property;
    if (descriptor == null) return null;
    return descriptor.getReadMethod().getAnnotation(JsonProperty.class);
  }

  private static Property getProperty(
      JobType jobType, Class<?> paramsType, Field field, @Nonnull JsonProperty annotation) {
    Class<?> valueType = field.getType();
    Property property = new Property(Primitives.wrap(valueType), null, null);
    property.setName(field.getName());
    property.setFieldName(TextUtils.getPrettyPropertyName(field.getName()));
    property.setRequired(annotation.required());

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
