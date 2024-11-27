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
package org.hisp.dhis.analytics.common.params.dimension;

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.fromFullDimensionId;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.Test;

class DimensionIdentifierHelperTest {
  @Test
  void testFromFullDimensionId() {
    String fullDimensionId = "lxAQ7Zs9VYR[0].RaMbOrTys0n[4].bh1Edk21e2n";

    StringDimensionIdentifier dimensionIdentifier = fromFullDimensionId(fullDimensionId);

    assertEquals(
        "bh1Edk21e2n",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be bh1Edk21e2n");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program uid should be lxAQ7Zs9VYR");
    assertEquals(0, dimensionIdentifier.getProgram().getOffset(), "Program offset should be 0");
    assertEquals(
        "RaMbOrTys0n",
        dimensionIdentifier.getProgramStage().getElement().getUid(),
        "Stage uid should be RaMbOrTys0n");
    assertEquals(4, dimensionIdentifier.getProgramStage().getOffset(), "Stage offset should be 4");
  }

  @Test
  void testFromDimensionIdWithUnsupportedOffset() {
    String dimensionIdWithDashOffset = "lxAQ7Zs9VYR[-].bh1Edk21e2n";
    String dimensionIdWithStringOffset = "lxAQ7Zs9VYR[X].bh1Edk21e2n";

    assertThrows(IllegalQueryException.class, () -> fromFullDimensionId(dimensionIdWithDashOffset));
    assertThrows(
        IllegalQueryException.class, () -> fromFullDimensionId(dimensionIdWithStringOffset));
  }

  @Test
  void testFromFullDimensionIdWithSingleDimension() {
    String singleDimensionId = "bh1Edk21e2n";

    StringDimensionIdentifier dimensionIdentifier = fromFullDimensionId(singleDimensionId);

    assertEquals(
        "bh1Edk21e2n",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be bh1Edk21e2n");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgram(), "Program should be empty");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgramStage(), "Stage should be empty");
  }

  @Test
  void testFromFullDimensionIdWithProgramAndDimension() {
    String singleDimensionId = "lxAQ7Zs9VYR.bh1Edk21e2n";

    StringDimensionIdentifier dimensionIdentifier = fromFullDimensionId(singleDimensionId);

    assertEquals(
        "bh1Edk21e2n",
        dimensionIdentifier.getDimension().getUid(),
        "Dimension uid should be bh1Edk21e2n");
    assertEquals(
        "lxAQ7Zs9VYR",
        dimensionIdentifier.getProgram().getElement().getUid(),
        "Program should be lxAQ7Zs9VYR");
    assertEquals(
        emptyElementWithOffset(), dimensionIdentifier.getProgramStage(), "Stage should be empty");
  }

  @Test
  void testFromFullDimensionIdWhenSingleDimensionHasOffset() {
    String singleDimensionWithOffset = "bh1Edk21e2n[2]";

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> fromFullDimensionId(singleDimensionWithOffset));

    assertEquals(
        "Only program and program stage can have offset",
        thrown.getMessage(),
        "Exception message does not match.");
  }

  @Test
  void testFromFullDimensionIdWhenDimensionHasInvalidFormat() {
    String invalidFullDimensionId = "lxAQ7Zs9VYR[1].RaMbOrTys0n[4].bh1Edk21e2n.invalid-id";

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> fromFullDimensionId(invalidFullDimensionId));

    assertEquals(
        "Invalid dimension identifier: " + invalidFullDimensionId,
        thrown.getMessage(),
        "Exception message does not match.");
  }
}
