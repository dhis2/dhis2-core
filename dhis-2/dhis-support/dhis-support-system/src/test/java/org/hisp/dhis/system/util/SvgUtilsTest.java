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
package org.hisp.dhis.system.util;

import static org.hisp.dhis.system.util.SvgUtils.replaceUnicodeZeroWidthSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SvgUtilsTest {
  @Test
  void testReplaceUnicodeZeroWidthSpace() {
    // given
    String svg =
        "<text x=\"332.58333333332666\" text-anchor=\"middle\" transform=\"translate(0,0)\" style=\"color: rgb(64, 75, 90); cursor: default; font-size: 11px; font-weight: normal; font-style: normal; fill: rgb(64, 75, 90);\" y=\"699\" opacity=\"1\">December<tspan dy=\"14\" x=\"332.58333333332666\">\u200B</tspan>2022</text>";
    String expected =
        "<text x=\"332.58333333332666\" text-anchor=\"middle\" transform=\"translate(0,0)\" style=\"color: rgb(64, 75, 90); cursor: default; font-size: 11px; font-weight: normal; font-style: normal; fill: rgb(64, 75, 90);\" y=\"699\" opacity=\"1\">December<tspan dy=\"14\" x=\"332.58333333332666\"> </tspan>2022</text>";

    // when
    svg = replaceUnicodeZeroWidthSpace(svg, " ");

    // then
    assertEquals(expected, svg);
  }
}
