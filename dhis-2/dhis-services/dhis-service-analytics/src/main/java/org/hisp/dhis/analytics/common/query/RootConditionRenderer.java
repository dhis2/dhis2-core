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
package org.hisp.dhis.analytics.common.query;

import static java.util.stream.Collectors.mapping;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

/**
 * Class to render the root condition of the main query. It will group the renderables by groupId
 * and create an OR condition for each group. Then it will create an AND condition joining all the
 * OR conditions. Conditions belonging to the {@link GroupableCondition#UNGROUPED_CONDITION} will be
 * joined with an AND condition.
 */
@RequiredArgsConstructor(staticName = "of")
public class RootConditionRenderer implements Renderable {
  private final List<GroupableCondition> groupableConditions;

  @Override
  public String render() {
    return AndCondition.of(
            Stream.concat(
                    groupableConditions.stream()
                        .filter(gc -> !gc.isGrouped())
                        .map(GroupableCondition::getRenderable),
                    getOrCondition().stream())
                .collect(Collectors.toList()))
        .render();
  }

  private List<Renderable> getOrCondition() {
    return groupableConditions.stream()
        .filter(GroupableCondition::isGrouped)
        .collect(
            Collectors.groupingBy(
                GroupableCondition::getGroupId,
                mapping(GroupableCondition::getRenderable, Collectors.toList())))
        .values()
        .stream()
        .map(OrCondition::of)
        .collect(Collectors.toList());
  }
}
