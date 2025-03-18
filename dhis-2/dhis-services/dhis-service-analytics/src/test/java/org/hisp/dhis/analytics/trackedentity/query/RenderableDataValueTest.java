/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.junit.jupiter.api.Test;

class RenderableDataValueTest {
  @Test
  void testRender() {
    RenderableDataValue renderableDataValue =
        RenderableDataValue.of("alias", "dataValue", ValueTypeMapping.STRING);
    String result = renderableDataValue.transformedIfNecessary().render();
    assertEquals("(alias.\"eventdatavalues\" -> 'dataValue' ->> 'value')::STRING", result);
  }

  @Test
  void testRenderBoolean() {
    RenderableDataValue renderableDataValue =
        RenderableDataValue.of("alias", "dataValue", ValueTypeMapping.BOOLEAN);
    String result = renderableDataValue.transformedIfNecessary().render();
    assertEquals("0", result);
  }

  @Test
  void testRenderTime() {
    RenderableDataValue renderableDataValue =
        RenderableDataValue.of("alias", "dataValue", ValueTypeMapping.TIME);
    String result = renderableDataValue.transformedIfNecessary().render();
    assertEquals("(alias.\"eventdatavalues\" -> 'dataValue' ->> 'value')::varchar", result);
  }

  @Test
  void testRenderWithLegendSet() {
    LegendSet legendSet = mock(LegendSet.class);
    Legend legend = mock(Legend.class);
    when(legendSet.getLegends()).thenReturn(Set.of(legend));

    when(legend.getStartValue()).thenReturn(1.0);
    when(legend.getEndValue()).thenReturn(2.0);

    Renderable renderableDataValue =
        RenderableDataValue.withLegendSet(
            RenderableDataValue.of("alias", "dataValue", ValueTypeMapping.STRING), legendSet);

    String result = renderableDataValue.render();

    assertEquals(
        "CASE WHEN (alias.\"eventdatavalues\" -> 'dataValue' ->> 'value')::STRING BETWEEN '1.0' AND '2.0' THEN 'null' END",
        result);
  }
}
