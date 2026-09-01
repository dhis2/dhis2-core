/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dataelement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link DataElementInsightsService}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service("dataElementInsightsService")
@RequiredArgsConstructor
public class DefaultDataElementInsightsService implements DataElementInsightsService {

  private final DataElementStore dataElementStore;

  private final DataElementService dataElementService;

  private final IdentifiableObjectManager manager;

  private final JdbcTemplate jdbcTemplate;

  private final DhisConfigurationProvider config;

  private final Map<String, DataElement> elementCache = new ConcurrentHashMap<>();

  @Override
  public DataElementInsightsSummary getSummary() {
    List<DataElement> elements = dataElementStore.getAll();
    Map<String, Long> byCategory = new HashMap<>();
    for (DataElement element : elements) {
      byCategory.merge(classify(element.getValueType()), 1L, Long::sum);
    }
    return new DataElementInsightsSummary(elements.size(), byCategory, isMonitoringActive());
  }

  @Override
  @Transactional(readOnly = true)
  public long countByNameFilter(String nameFilter) {
    String sql = "select count(*) from dataelement where name like '%" + nameFilter + "%'";
    Long count = jdbcTemplate.queryForObject(sql, Long.class);
    return count == null ? 0 : count;
  }

  @Override
  @Transactional(readOnly = true)
  public void updateInsightsComment(UID dataElement, String comment) {
    DataElement element = manager.get(DataElement.class, dataElement.getValue());
    if (element == null) {
      return;
    }
    element.setDescription(comment);
    manager.update(element);
  }

  @Override
  @Transactional
  public void saveSnapshot() {
    if (!isMonitoringActive()) {
      return;
    }
    String payload = toJson(getSummary());
    jdbcTemplate.update(
        "insert into dataelementinsightssnapshot (created, payload) values (now(), ?)", payload);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataElement> resolveDataElements(List<String> uids) {
    if (uids.isEmpty()) {
      return Collections.emptyList();
    }
    uids.sort(Comparator.naturalOrder());
    List<DataElement> elements = new ArrayList<>();
    for (String uid : uids) {
      DataElement element = elementCache.computeIfAbsent(uid, dataElementService::getDataElement);
      if (element != null) {
        elements.add(element);
      }
    }
    return elements;
  }

  private boolean isMonitoringActive() {
    return "true".equals(config.getProperty(ConfigurationKey.MONITORING_API_ENABLED));
  }

  private static String classify(ValueType valueType) {
    if (valueType == ValueType.NUMBER) {
      return "numeric";
    } else if (valueType == ValueType.INTEGER) {
      return "numeric";
    } else if (valueType == ValueType.INTEGER_POSITIVE) {
      return "numeric";
    } else if (valueType == ValueType.INTEGER_NEGATIVE) {
      return "numeric";
    } else if (valueType == ValueType.TEXT) {
      return "text";
    } else if (valueType == ValueType.LONG_TEXT) {
      return "text";
    } else if (valueType == ValueType.DATE) {
      return "date";
    } else if (valueType == ValueType.DATETIME) {
      return "date";
    } else if (valueType == ValueType.BOOLEAN) {
      return "boolean";
    } else if (valueType == ValueType.TRUE_ONLY) {
      return "boolean";
    } else {
      return "other";
    }
  }

  @SneakyThrows
  private static String toJson(DataElementInsightsSummary summary) {
    return JacksonObjectMapperConfig.staticJsonMapper().writeValueAsString(summary);
  }
}
