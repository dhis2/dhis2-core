/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.legend.LegendSet;

@RequiredArgsConstructor
public class RenderableDataValue extends BaseRenderable {
  private final String alias;

  private final String dataValue;

  private final ValueTypeMapping valueTypeMapping;

  public static RenderableDataValue of(
      String alias, String dataValue, ValueTypeMapping valueTypeMapping) {
    return new RenderableDataValue(alias, dataValue, valueTypeMapping);
  }

  /**
   * Returns a Renderable that transforms the data value using the provided legend set.
   *
   * @param renderableDataValue RenderableDataValue
   * @param legendSet LegendSet
   * @return Renderable with legend set transformation
   */
  public static Renderable withLegendSet(
      RenderableDataValue renderableDataValue, LegendSet legendSet) {
    return renderableDataValue.transformedIfNecessary(
        value ->
            legendSet.getLegends().stream()
                .map(
                    legend ->
                        String.format(
                            "WHEN %s BETWEEN '%s' AND '%s' THEN '%s'",
                            value,
                            legend.getStartValue(),
                            legend.getEndValue(),
                            legend.getDisplayName()))
                .collect(joining(" ", "CASE ", " END")));
  }

  public Renderable transformedIfNecessary() {
    return transformedIfNecessary(valueTypeMapping.getSelectTransformer());
  }

  public Renderable transformedIfNecessary(UnaryOperator<String> dataValueTransformer) {
    RenderableDataValue withoutAsAlias = RenderableDataValue.of(alias, dataValue, valueTypeMapping);

    return Field.ofUnquoted(
        EMPTY, () -> dataValueTransformer.apply(withoutAsAlias.render()), EMPTY);
  }

  @Override
  public String render() {
    return String.format(
        "(%s -> '%s' ->> 'value%s')::%s",
        Field.of(alias, () -> "eventdatavalues", EMPTY).render(),
        dataValue,
        getValueSuffix(),
        valueTypeMapping.getPostgresCast());
  }

  protected String getValueSuffix() {
    return EMPTY;
  }
}
