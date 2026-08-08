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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.util.Constant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Preheats the specific {@code (option set, code)} pairs referenced by the import payload, instead
 * of letting {@code OptionSetMapper} fully materialize every {@link
 * org.hisp.dhis.option.OptionSet#getOptions()} collection. Cost is bounded by how many distinct
 * option codes the payload actually references, not by option set size.
 *
 * @see org.hisp.dhis.tracker.imports.preheat.TrackerPreheat#isValidOptionCode(Long, String)
 */
@Component
public class OptionValueSupplier extends JdbcAbstractPreheatSupplier {
  private static final String OPTION_SET_ID = "optionsetid";

  private static final String CODE = "code";

  private static final String SQL =
      "select optionsetid, code from optionvalue"
          + " where optionsetid in (:optionSetIds)"
          + " and code in (:codes)";

  protected OptionValueSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    Set<Pair<Long, String>> candidates = collectCandidates(trackerObjects, preheat);

    if (candidates.isEmpty()) {
      return;
    }

    for (List<Pair<Long, String>> chunk :
        Lists.partition(new ArrayList<>(candidates), Constant.SPLIT_LIST_PARTITION_SIZE)) {
      queryChunk(chunk, preheat);
    }
  }

  Set<Pair<Long, String>> collectCandidates(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    Set<Pair<Long, String>> candidates = new HashSet<>();

    for (TrackedEntity trackedEntity : trackerObjects.getTrackedEntities()) {
      for (Attribute attribute : trackedEntity.getAttributes()) {
        addCandidates(
            preheat.getTrackedEntityAttribute(attribute.getAttribute()),
            attribute.getValue(),
            candidates);
      }
    }

    for (Enrollment enrollment : trackerObjects.getEnrollments()) {
      for (Attribute attribute : enrollment.getAttributes()) {
        addCandidates(
            preheat.getTrackedEntityAttribute(attribute.getAttribute()),
            attribute.getValue(),
            candidates);
      }
    }

    for (Event event : trackerObjects.getEvents()) {
      for (DataValue dataValue : event.getDataValues()) {
        addCandidates(
            preheat.getDataElement(dataValue.getDataElement()), dataValue.getValue(), candidates);
      }
    }

    return candidates;
  }

  private void addCandidates(
      ValueTypedDimensionalItemObject optionalObject,
      String value,
      Set<Pair<Long, String>> candidates) {
    if (optionalObject == null || value == null || !optionalObject.hasOptionSet()) {
      return;
    }

    Long optionSetId = optionalObject.getOptionSet().getId();
    List<String> codes =
        optionalObject.getValueType().isMultiText()
            ? ValueType.splitMultiText(value)
            : List.of(value);

    for (String code : codes) {
      candidates.add(Pair.of(optionSetId, code));
    }
  }

  private void queryChunk(List<Pair<Long, String>> chunk, TrackerPreheat preheat) {
    Set<Long> optionSetIds = new HashSet<>();
    Set<String> codes = new HashSet<>();
    for (Pair<Long, String> pair : chunk) {
      optionSetIds.add(pair.getLeft());
      codes.add(pair.getRight());
    }

    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("optionSetIds", optionSetIds);
    parameters.addValue("codes", codes);

    Set<Pair<Long, String>> confirmed = new HashSet<>();
    jdbcTemplate.query(
        SQL,
        parameters,
        (RowCallbackHandler)
            rs -> confirmed.add(Pair.of(rs.getLong(OPTION_SET_ID), rs.getString(CODE))));

    for (Pair<Long, String> pair : chunk) {
      if (confirmed.contains(pair)) {
        preheat.addValidOptionCode(pair.getLeft(), pair.getRight());
      }
    }
  }
}
