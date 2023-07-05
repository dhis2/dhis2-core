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

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;
import org.jfree.chart.JFreeChart;

/**
 * @author Lars Helge Overland
 */
public interface ChartService {
  /** Supported visualization types. */
  Set<VisualizationType> SUPPORTED_TYPES =
      Set.of(
          VisualizationType.LINE,
          VisualizationType.COLUMN,
          VisualizationType.BAR,
          VisualizationType.AREA,
          VisualizationType.PIE,
          VisualizationType.STACKED_COLUMN,
          VisualizationType.STACKED_BAR,
          VisualizationType.RADAR,
          VisualizationType.GAUGE);

  /**
   * Generates a JFreeChart.
   *
   * @param plotData the plot data to use as basis for the JFreeChart generation.
   * @param date the date to use as basis for relative periods, can be null.
   * @param organisationUnit the org unit to use as basis for relative units, will override the
   *     current user org unit if set, can be null.
   * @param format the i18n format.
   * @return a JFreeChart object.
   */
  JFreeChart getJFreeChart(
      PlotData plotData, Date date, OrganisationUnit organisationUnit, I18nFormat format);

  JFreeChart getJFreeChart(
      PlotData plotData,
      Date date,
      OrganisationUnit organisationUnit,
      I18nFormat format,
      User currentUser);

  JFreeChart getJFreePeriodChart(
      Indicator indicator, OrganisationUnit organisationUnit, boolean title, I18nFormat format);

  JFreeChart getJFreeOrganisationUnitChart(
      Indicator indicator, OrganisationUnit parent, boolean title, I18nFormat format);

  JFreeChart getJFreeChartHistory(
      DataElement dataElement,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      Period lastPeriod,
      OrganisationUnit organisationUnit,
      int historyLength,
      I18nFormat format);
}
