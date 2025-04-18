/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.visualization;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.hisp.dhis.common.GenericAnalyticalObjectDeletionHandler;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

@Component
public class VisualizationDeletionHandler
    extends GenericAnalyticalObjectDeletionHandler<Visualization, VisualizationService> {

  public VisualizationDeletionHandler(VisualizationService visualizationService) {
    super(new DeletionVeto(Visualization.class), visualizationService);
    checkNotNull(visualizationService);
  }

  @Override
  protected void registerHandler() {
    // generic
    whenDeleting(Indicator.class, this::deleteIndicator);
    whenDeleting(DataElement.class, this::deleteDataElement);
    whenDeleting(DataSet.class, this::deleteDataSet);
    whenDeleting(ProgramIndicator.class, this::deleteProgramIndicator);
    whenDeleting(Period.class, this::deletePeriod);
    whenVetoing(Period.class, this::allowDeletePeriod);
    whenDeleting(OrganisationUnit.class, this::deleteOrganisationUnit);
    whenDeleting(OrganisationUnitGroup.class, this::deleteOrganisationUnitGroup);
    whenDeleting(OrganisationUnitGroupSet.class, this::deleteOrganisationUnitGroupSet);
    // special
    whenDeleting(LegendSet.class, this::deleteLegendSet);
    whenDeleting(ExpressionDimensionItem.class, this::deleteExpressionDimensionItem);
  }

  private void deleteLegendSet(LegendSet legendSet) {
    List<Visualization> visualizations = service.getAnalyticalObjects(legendSet);

    for (Visualization visualization : visualizations) {
      if (visualization.getLegendDefinitions() != null) {
        visualization.getLegendDefinitions().setLegendSet(null);
      }

      service.update(visualization);
    }
  }
}
