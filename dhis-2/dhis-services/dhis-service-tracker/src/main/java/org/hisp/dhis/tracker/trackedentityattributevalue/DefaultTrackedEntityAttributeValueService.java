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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import static org.hisp.dhis.changelog.ChangeLogType.DELETE;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsValid;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DefaultTrackedEntityAttributeValueService
    implements TrackedEntityAttributeValueService {
  private final HibernateTrackedEntityAttributeValueStore attributeValueStore;

  private final FileResourceService fileResourceService;

  private final TrackedEntityChangeLogService trackedEntityChangeLogService;

  private final ReservedValueService reservedValueService;

  private final DhisConfigurationProvider config;

  @Override
  @Transactional
  public void deleteTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue) {
    trackedEntityChangeLogService.addTrackedEntityChangeLog(
        attributeValue.getTrackedEntity(),
        attributeValue.getAttribute(),
        attributeValue.getPlainValue(),
        null,
        DELETE,
        getCurrentUsername());

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

      if (Boolean.TRUE.equals(attributeValue.getAttribute().isGenerated())
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
        .toList();
  }
}
