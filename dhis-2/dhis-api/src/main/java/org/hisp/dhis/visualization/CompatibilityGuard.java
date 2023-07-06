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
package org.hisp.dhis.visualization;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * Responsible for maintaining read compatibility for certain Visualization attributes for required
 * use cases, specially when new refactoring affects the existing contract.
 *
 * @author maikel arabori
 */
public class CompatibilityGuard {
  private CompatibilityGuard() {}

  static void keepLegendReadingCompatibility(final Visualization visualization) {
    if (visualization.getSeriesKey() != null && visualization.getSeriesKey().getLabel() != null) {
      if (visualization.getFontStyle() == null) {
        visualization.setFontStyle(new VisualizationFontStyle());
      }

      visualization
          .getFontStyle()
          .setLegend(visualization.getSeriesKey().getLabel().getFontStyle());
    }
  }

  static void keepAxesReadingCompatibility(final Visualization visualization) {
    if (isNotEmpty(visualization.getAxes())) {
      if (visualization.getFontStyle() == null) {
        visualization.setFontStyle(new VisualizationFontStyle());
      }

      keepFirstAxisReadCompatible(visualization);
      keepSecondAxisReadCompatible(visualization);
    }
  }

  private static void keepSecondAxisReadCompatible(final Visualization visualization) {
    if (visualization.getAxes().size() > 1) {
      final AxisV2 secondAxis = visualization.getAxes().get(1);

      if (secondAxis != null && secondAxis.getLabel() != null) {
        visualization.getFontStyle().setCategoryAxisLabel(secondAxis.getLabel().getFontStyle());
      }

      if (secondAxis != null && secondAxis.getTitle() != null) {
        visualization.getFontStyle().setHorizontalAxisTitle(secondAxis.getTitle().getFontStyle());
        visualization.setDomainAxisLabel(secondAxis.getTitle().getText());
      }
    }
  }

  private static void keepFirstAxisReadCompatible(final Visualization visualization) {
    final AxisV2 firstAxis = visualization.getAxes().get(0);

    if (firstAxis != null) {
      if (firstAxis.getLabel() != null) {
        visualization.getFontStyle().setSeriesAxisLabel(firstAxis.getLabel().getFontStyle());
      }

      if (firstAxis.getTitle() != null) {
        visualization.getFontStyle().setVerticalAxisTitle(firstAxis.getTitle().getFontStyle());
        visualization.setRangeAxisLabel(firstAxis.getTitle().getText());
      }

      if (firstAxis.getBaseLine() != null && firstAxis.getBaseLine().getTitle() != null) {
        copyBaseLine(visualization, firstAxis);
      }

      if (firstAxis.getTargetLine() != null && firstAxis.getTargetLine().getTitle() != null) {
        copyTargetLine(visualization, firstAxis);
      }

      visualization.setRangeAxisDecimals(firstAxis.getDecimals());
      visualization.setRangeAxisMaxValue(
          firstAxis.getMaxValue() != null ? Double.valueOf(firstAxis.getMaxValue()) : null);
      visualization.setRangeAxisMinValue(
          firstAxis.getMinValue() != null ? Double.valueOf(firstAxis.getMinValue()) : null);
      visualization.setRangeAxisSteps(firstAxis.getSteps());
    }
  }

  private static void copyTargetLine(final Visualization visualization, final AxisV2 firstAxis) {
    visualization
        .getFontStyle()
        .setTargetLineLabel(firstAxis.getTargetLine().getTitle().getFontStyle());
    visualization.setTargetLineLabel(firstAxis.getTargetLine().getTitle().getText());
    visualization.setTargetLineValue(
        firstAxis.getTargetLine().getValue() != null
            ? Double.valueOf(firstAxis.getTargetLine().getValue())
            : null);
  }

  private static void copyBaseLine(final Visualization visualization, final AxisV2 firstAxis) {
    visualization
        .getFontStyle()
        .setBaseLineLabel(firstAxis.getBaseLine().getTitle().getFontStyle());
    visualization.setBaseLineLabel(firstAxis.getBaseLine().getTitle().getText());
    visualization.setBaseLineValue(
        firstAxis.getBaseLine().getValue() != null
            ? Double.valueOf(firstAxis.getBaseLine().getValue())
            : null);
  }
}
