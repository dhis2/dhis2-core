/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data.aggregate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.junit.jupiter.api.Test;

class AggregatedEnrollmentHeaderColumnResolverTest {
  private final StageHeaderClassifier stageHeaderClassifier = new StageHeaderClassifier();
  private final AggregatedEnrollmentHeaderColumnResolver subject =
      new AggregatedEnrollmentHeaderColumnResolver(stageHeaderClassifier);

  @Test
  void shouldUseFilterCteForStageEventDate() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.eventdate\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_occurreddate as \"stage1.eventdate\""));
  }

  @Test
  void shouldUseFilterCteForStageOuName() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.ouname\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_ouname as \"stage1.ouname\""));
  }

  @Test
  void shouldUseFilterCteForStageOuCode() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.oucode\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_oucode as \"stage1.oucode\""));
  }

  @Test
  void shouldUseFilterCteForStageEventStatus() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.eventstatus\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_eventstatus as \"stage1.eventstatus\""));
  }

  @Test
  void shouldUseFilterCteForStageOu() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.ou\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_ou as \"stage1.ou\""));
  }

  @Test
  void shouldUseFilterCteForGenericStageItem() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.de1\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_de1 as \"stage1.de1\""));
  }

  @Test
  void shouldUseCteDefinitionWhenMatching() {
    CteDefinition cteDefinition = new CteDefinition("ps1", "de1", "select 1", 0);
    Map<String, CteDefinition> cteDefinitionMap = Map.of("\"de1\"", cteDefinition);

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"de1\""),
        new CteContext(EndpointItem.ENROLLMENT),
        sb,
        cteDefinitionMap,
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("value as \"de1\""));
  }

  @Test
  void shouldFallBackToBaseColumnWhenNoCteMatch() {
    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"plain\""),
        new CteContext(EndpointItem.ENROLLMENT),
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("\"plain\""));
  }

  @Test
  void shouldFallBackToBaseColumnWhenStageSpecificAndNoFilterCte() {
    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.de1\""),
        new CteContext(EndpointItem.ENROLLMENT),
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("\"stage1.de1\""));
  }

  @Test
  void shouldResolveDimensionsFromDifferentStages() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stageA", "select 1");
    cteContext.addFilterCte("latest_events_stageB", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stageA.eventdate\"", "\"stageB.eventstatus\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_occurreddate as \"stageA.eventdate\""));
    assertThat(sql, containsString("ev_eventstatus as \"stageB.eventstatus\""));
  }

  @Test
  void shouldResolveTwoDimensionsFromSameStage() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    SelectBuilder sb = new SelectBuilder();
    sb.from("dummy");

    subject.addHeaderColumns(
        Set.of("\"stage1.eventdate\"", "\"stage1.eventstatus\""),
        cteContext,
        sb,
        Collections.emptyMap(),
        column -> "\"" + column + "\"");

    String sql = sb.build();

    assertThat(sql, containsString("ev_occurreddate as \"stage1.eventdate\""));
    assertThat(sql, containsString("ev_eventstatus as \"stage1.eventstatus\""));
  }
}
