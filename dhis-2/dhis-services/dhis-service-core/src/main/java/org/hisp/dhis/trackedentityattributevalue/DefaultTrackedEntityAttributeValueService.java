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
package org.hisp.dhis.trackedentityattributevalue;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsValid;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService")
public class DefaultTrackedEntityAttributeValueService
    implements TrackedEntityAttributeValueService {
  private final TrackedEntityAttributeValueStore attributeValueStore;

  private final FileResourceService fileResourceService;

  private final TrackedEntityAttributeValueChangeLogService
      trackedEntityAttributeValueChangeLogService;

  private final ReservedValueService reservedValueService;

  private final DhisConfigurationProvider config;

  private final UserService userService;

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void deleteTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue) {
    TrackedEntityAttributeValueChangeLog trackedEntityAttributeValueChangeLog =
        new TrackedEntityAttributeValueChangeLog(
            attributeValue,
            attributeValue.getAuditValue(),
            CurrentUserUtil.getCurrentUsername(),
            ChangeLogType.DELETE);

    if (config.isEnabled(CHANGELOG_TRACKER)) {
      trackedEntityAttributeValueChangeLogService.addTrackedEntityAttributeValueChangLog(
          trackedEntityAttributeValueChangeLog);
    }

    deleteFileValue(attributeValue);
    attributeValueStore.delete(attributeValue);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityAttributeValue getTrackedEntityAttributeValue(
      TrackedEntity trackedEntity, TrackedEntityAttribute attribute) {
    return attributeValueStore.get(trackedEntity, attribute);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntity trackedEntity) {
    return attributeValueStore.get(trackedEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues(
      TrackedEntityAttribute attribute) {
    return attributeValueStore.get(attribute);
  }

  @Override
  @Transactional(readOnly = true)
  public int getCountOfAssignedTrackedEntityAttributeValues(TrackedEntityAttribute attribute) {
    return attributeValueStore.getCountOfAssignedTEAValues(attribute);
  }

  @Override
  @Transactional
  public void addTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue) {
    if (attributeValue == null
        || attributeValue.getAttribute() == null
        || attributeValue.getAttribute().getValueType() == null) {
      throw new IllegalQueryException("Attribute or type is null or empty");
    }

    if (attributeValue.getAttribute().isConfidentialBool()
        && !config.getEncryptionStatus().isOk()) {
      throw new IllegalStateException(
          "Unable to encrypt data, encryption is not correctly configured");
    }

    String result =
        valueIsValid(attributeValue.getValue(), attributeValue.getAttribute().getValueType());

    if (result != null) {
      throw new IllegalQueryException("Value is not valid:  " + result);
    }

    attributeValue.setAutoFields();

    if (attributeValue.getAttribute().getValueType().isFile()
        && !StringUtils.isEmpty(attributeValue.getValue())
        && !addFileValue(attributeValue)) {
      throw new IllegalQueryException(
          String.format("FileResource with id '%s' not found", attributeValue.getValue()));
    }

    if (attributeValue.getValue() != null) {
      attributeValueStore.saveVoid(attributeValue);

      if (attributeValue.getAttribute().isGenerated()
          && attributeValue.getAttribute().getTextPattern() != null) {
        reservedValueService.useReservedValue(
            attributeValue.getAttribute().getTextPattern(), attributeValue.getValue());
      }
    }
  }

  @Override
  @Transactional
  public void updateTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    updateTrackedEntityAttributeValue(attributeValue, currentUser);
  }

  @Override
  @Transactional
  public void updateTrackedEntityAttributeValue(
      TrackedEntityAttributeValue attributeValue, UserDetails user) {
    if (attributeValue != null && StringUtils.isEmpty(attributeValue.getValue())) {
      deleteFileValue(attributeValue);
      attributeValueStore.delete(attributeValue);
    } else {
      if (attributeValue == null
          || attributeValue.getAttribute() == null
          || attributeValue.getAttribute().getValueType() == null) {
        throw new IllegalQueryException("Attribute or type is null or empty");
      }

      attributeValue.setAutoFields();

      String result =
          valueIsValid(attributeValue.getValue(), attributeValue.getAttribute().getValueType());

      if (result != null) {
        throw new IllegalQueryException("Value is not valid:  " + result);
      }

      TrackedEntityAttributeValueChangeLog trackedEntityAttributeValueChangeLog =
          new TrackedEntityAttributeValueChangeLog(
              attributeValue,
              attributeValue.getAuditValue(),
              User.username(user),
              ChangeLogType.UPDATE);

      if (config.isEnabled(CHANGELOG_TRACKER)) {
        trackedEntityAttributeValueChangeLogService.addTrackedEntityAttributeValueChangLog(
            trackedEntityAttributeValueChangeLog);
      }

      attributeValueStore.update(attributeValue);

      if (attributeValue.getAttribute().isGenerated()
          && attributeValue.getAttribute().getTextPattern() != null) {
        reservedValueService.useReservedValue(
            attributeValue.getAttribute().getTextPattern(), attributeValue.getValue());
      }
    }
  }

  private void deleteFileValue(TrackedEntityAttributeValue value) {
    if (!value.getAttribute().getValueType().isFile()
        || fileResourceService.getFileResource(value.getValue()) == null) {
      return;
    }

    FileResource fileResource = fileResourceService.getFileResource(value.getValue());
    fileResourceService.updateFileResource(fileResource);
  }

  private boolean addFileValue(TrackedEntityAttributeValue value) {
    FileResource fileResource = fileResourceService.getFileResource(value.getValue());

    if (fileResource == null) {
      return false;
    }

    fileResource.setAssigned(true);
    fileResourceService.updateFileResource(fileResource);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttributeValue> getUniqueAttributeByValues(
      Map<TrackedEntityAttribute, List<String>> uniqueAttributes) {
    return uniqueAttributes.entrySet().stream()
        .flatMap(entry -> this.attributeValueStore.get(entry.getKey(), entry.getValue()).stream())
        .collect(Collectors.toList());
  }
}
