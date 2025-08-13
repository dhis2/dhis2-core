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
package org.hisp.dhis.dxf2.metadata;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAnalyticalObjectImportHandler implements AnalyticalObjectImportHandler {
  private final PreheatService preheatService;

  private final IdentifiableObjectManager objectManager;

  @Override
  public void handleAnalyticalObject(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    handleDataDimensionItems(entityManager, schema, analyticalObject, bundle);
    handleCategoryDimensions(entityManager, schema, analyticalObject, bundle);
    handleDataElementDimensions(entityManager, schema, analyticalObject, bundle);
    handleAttributeDimensions(entityManager, schema, analyticalObject, bundle);
    handleProgramIndicatorDimensions(entityManager, schema, analyticalObject, bundle);
    handleAnalyticalLegendSet(schema, analyticalObject, bundle);
    handleRelativePeriods(schema, analyticalObject);
    handleOrgUnitGroupSetDimensions(entityManager, schema, analyticalObject, bundle);
  }

  /**
   * This method implements required custom handling for {@link OrganisationUnitGroupSetDimension}s.
   * Without it, when importing, these objects throw TransientObjectException.
   *
   * <p>There are 2 parts to the handling: <br>
   * 1. The dimension {@link OrganisationUnitGroupSet}
   *
   * <pre>
   *  a. If available from the bundle preheat, use this instance.
   *  b. Otherwise use from persisted if it exists, and then add to the bundle for `connectReferences`
   * </pre>
   *
   * 2. The items: List of {@link OrganisationUnitGroup}
   *
   * <pre>
   *  a. If available from the bundle preheat, use this instance.
   *  b. Otherwise use from persisted if it exists, and then add to the bundle for `connectReferences`
   * </pre>
   *
   * <p>
   *
   * @param entityManager entityManager to save object
   * @param schema schema to check object property
   * @param analyticalObject object that needs custom handling
   * @param bundle bundle with preheat objects
   */
  private void handleOrgUnitGroupSetDimensions(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("organisationUnitGroupSetDimensions")) return;

    for (OrganisationUnitGroupSetDimension organisationUnitGroupSetDimension :
        analyticalObject.getOrganisationUnitGroupSetDimensions()) {

      // handle dimension
      OrganisationUnitGroupSet orgUnitGroupSetBundle =
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), organisationUnitGroupSetDimension.getDimension());

      // use from bundle if available
      if (orgUnitGroupSetBundle != null) {
        organisationUnitGroupSetDimension.setDimension(orgUnitGroupSetBundle);
      } else {
        // use from persisted if available
        OrganisationUnitGroupSet orgUnitGroupSetPersisted =
            objectManager.get(
                OrganisationUnitGroupSet.class,
                organisationUnitGroupSetDimension.getDimension().getUid());

        if (orgUnitGroupSetPersisted != null) {
          organisationUnitGroupSetDimension.setDimension(orgUnitGroupSetPersisted);

          bundle
              .getPreheat()
              .put(bundle.getPreheatIdentifier(), organisationUnitGroupSetDimension.getDimension());
        }
      }

      // handle items
      List<OrganisationUnitGroup> organisationUnitGroups =
          new ArrayList<>(organisationUnitGroupSetDimension.getItems());
      organisationUnitGroupSetDimension.getItems().clear();

      organisationUnitGroups.forEach(
          oug -> {
            // use from bundle if available
            OrganisationUnitGroup orgUnitGroupBundle =
                bundle.getPreheat().get(bundle.getPreheatIdentifier(), oug);
            if (orgUnitGroupBundle != null) {
              organisationUnitGroupSetDimension.getItems().add(orgUnitGroupBundle);
            } else {
              // use from persisted if available
              OrganisationUnitGroup orgUnitGroupPersisted =
                  objectManager.get(OrganisationUnitGroup.class, oug.getUid());
              if (orgUnitGroupPersisted != null) {
                organisationUnitGroupSetDimension.getItems().add(orgUnitGroupPersisted);
                bundle.getPreheat().put(bundle.getPreheatIdentifier(), orgUnitGroupPersisted);
              }
            }
          });

      preheatService.connectReferences(
          organisationUnitGroupSetDimension, bundle.getPreheat(), bundle.getPreheatIdentifier());

      entityManager.persist(organisationUnitGroupSetDimension);
    }
  }

  private void handleRelativePeriods(Schema schema, BaseAnalyticalObject analyticalObject) {
    if (!schema.hasPersistedProperty("rawPeriods")) return;

    Set<String> rawPeriods = new LinkedHashSet<>();

    addRawPeriods(analyticalObject.getRows(), rawPeriods);
    addRawPeriods(analyticalObject.getColumns(), rawPeriods);
    addRawPeriods(analyticalObject.getFilters(), rawPeriods);

    RelativePeriods relativePeriods = analyticalObject.getRelatives();

    if (relativePeriods != null) {
      rawPeriods.addAll(relativePeriods.getRelativePeriodEnums().stream().map(Enum::name).toList());
    }

    analyticalObject.setRawPeriods(new ArrayList<>(rawPeriods));
  }

  /**
   * Adds to the Set of periods, the periods present in the given list of {@link DimensionalObject},
   * if any.
   *
   * @param dimObjects the list of {@link DimensionalObject}.
   * @param rawPeriods the list of periods.
   */
  private void addRawPeriods(List<DimensionalObject> dimObjects, Set<String> rawPeriods) {
    if (dimObjects != null) {
      for (DimensionalObject dimObject : dimObjects) {
        if (dimObject.hasItems()) {
          addRawPeriods(dimObject, rawPeriods);
        }
      }
    }
  }

  /**
   * Adds to the Set of periods, the periods present in the given list of {@link DimensionalObject},
   * if any.
   *
   * @param dimObject the {@link DimensionalObject} where to get the periods from.
   * @param rawPeriods the list of periods.
   */
  private void addRawPeriods(DimensionalObject dimObject, Set<String> rawPeriods) {
    for (DimensionalItemObject item : dimObject.getItems()) {
      String period = item.getUid();
      Period isoPeriod = PeriodType.getPeriodFromIsoString(period);

      if (RelativePeriodEnum.contains(period) || isoPeriod != null) {
        rawPeriods.add(period);
      }
    }
  }

  /**
   * Before saving a {@link Visualization} if its property legendDefinitions has reference to a
   * {@link LegendSet} then we need to make sure that LegendSet object existed in database.
   *
   * @param schema current analytic object {@link Schema}
   * @param analyticalObject the analytic object to be processed
   * @param bundle current {@link ObjectBundle}
   */
  private void handleAnalyticalLegendSet(
      Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle) {
    if (!BaseAnalyticalObject.class.isAssignableFrom(schema.getKlass())) {
      return;
    }

    if (analyticalObject.getLegendDefinitions() == null
        || analyticalObject.getLegendDefinitions().getLegendSet() == null) {
      return;
    }

    String legendSetId = analyticalObject.getLegendDefinitions().getLegendSet().getUid();
    LegendSet legendSet =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), LegendSet.class, legendSetId);

    if (legendSet != null) {
      analyticalObject.getLegendDefinitions().setLegendSet(legendSet);
      return;
    }

    legendSet = objectManager.get(LegendSet.class, legendSetId);

    if (legendSet != null) {
      analyticalObject.getLegendDefinitions().setLegendSet(legendSet);
      bundle.getPreheat().put(bundle.getPreheatIdentifier(), legendSet);
      return;
    }

    // Add new LegendSet
    preheatService.connectReferences(
        analyticalObject.getLegendDefinitions().getLegendSet(),
        bundle.getPreheat(),
        bundle.getPreheatIdentifier());
    objectManager.save(analyticalObject.getLegendDefinitions().getLegendSet());

    bundle
        .getPreheat()
        .put(bundle.getPreheatIdentifier(), analyticalObject.getLegendDefinitions().getLegendSet());
  }

  private void handleDataDimensionItems(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("dataDimensionItems")) return;

    for (DataDimensionItem dataDimensionItem : analyticalObject.getDataDimensionItems()) {
      if (dataDimensionItem == null) {
        continue;
      }

      dataDimensionItem.setDataElement(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), dataDimensionItem.getDataElement()));
      dataDimensionItem.setIndicator(
          bundle.getPreheat().get(bundle.getPreheatIdentifier(), dataDimensionItem.getIndicator()));
      dataDimensionItem.setProgramIndicator(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), dataDimensionItem.getProgramIndicator()));

      if (dataDimensionItem.getDataElementOperand() != null) {
        preheatService.connectReferences(
            dataDimensionItem.getDataElementOperand(),
            bundle.getPreheat(),
            bundle.getPreheatIdentifier());
      }

      if (dataDimensionItem.getReportingRate() != null) {
        dataDimensionItem
            .getReportingRate()
            .setDataSet(
                bundle
                    .getPreheat()
                    .get(
                        bundle.getPreheatIdentifier(),
                        dataDimensionItem.getReportingRate().getDataSet()));
      }

      if (dataDimensionItem.getProgramDataElement() != null) {
        dataDimensionItem
            .getProgramDataElement()
            .setProgram(
                bundle
                    .getPreheat()
                    .get(
                        bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramDataElement().getProgram()));
        dataDimensionItem
            .getProgramDataElement()
            .setDataElement(
                bundle
                    .getPreheat()
                    .get(
                        bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramDataElement().getDataElement()));
      }

      if (dataDimensionItem.getProgramAttribute() != null) {
        dataDimensionItem
            .getProgramAttribute()
            .setProgram(
                bundle
                    .getPreheat()
                    .get(
                        bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramAttribute().getProgram()));
        dataDimensionItem
            .getProgramAttribute()
            .setAttribute(
                bundle
                    .getPreheat()
                    .get(
                        bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramAttribute().getAttribute()));
      }

      // handle expression dimension item
      ExpressionDimensionItem expressionDimensionItem =
          dataDimensionItem.getExpressionDimensionItem();
      if (expressionDimensionItem != null) {
        ExpressionDimensionItem expressionDimensionItemBundle =
            bundle.getPreheat().get(bundle.getPreheatIdentifier(), expressionDimensionItem);
        // use bundle object if available to avoid transient exception
        if (expressionDimensionItemBundle != null) {
          dataDimensionItem.setExpressionDimensionItem(expressionDimensionItemBundle);
        }
      }

      preheatService.connectReferences(
          dataDimensionItem, bundle.getPreheat(), bundle.getPreheatIdentifier());
      entityManager.persist(dataDimensionItem);
    }
  }

  private void handleCategoryDimensions(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("categoryDimensions")) return;

    for (CategoryDimension categoryDimension : analyticalObject.getCategoryDimensions()) {
      if (categoryDimension == null) {
        continue;
      }

      categoryDimension.setDimension(
          bundle.getPreheat().get(bundle.getPreheatIdentifier(), categoryDimension.getDimension()));
      List<CategoryOption> categoryOptions = new ArrayList<>(categoryDimension.getItems());
      categoryDimension.getItems().clear();

      categoryOptions.forEach(
          co -> {
            CategoryOption categoryOption =
                bundle.getPreheat().get(bundle.getPreheatIdentifier(), co);
            if (categoryOption != null) categoryDimension.getItems().add(categoryOption);
          });

      preheatService.connectReferences(
          categoryDimension, bundle.getPreheat(), bundle.getPreheatIdentifier());
      entityManager.persist(categoryDimension);
    }
  }

  private void handleDataElementDimensions(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("dataElementDimensions")) return;

    for (TrackedEntityDataElementDimension dataElementDimension :
        analyticalObject.getDataElementDimensions()) {
      if (dataElementDimension == null) {
        continue;
      }

      dataElementDimension.setDataElement(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), dataElementDimension.getDataElement()));
      dataElementDimension.setLegendSet(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), dataElementDimension.getLegendSet()));
      dataElementDimension.setProgramStage(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), dataElementDimension.getProgramStage()));

      preheatService.connectReferences(
          dataElementDimension, bundle.getPreheat(), bundle.getPreheatIdentifier());
      entityManager.persist(dataElementDimension);
    }
  }

  private void handleAttributeDimensions(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("attributeDimensions")) return;

    for (TrackedEntityAttributeDimension attributeDimension :
        analyticalObject.getAttributeDimensions()) {
      if (attributeDimension == null) {
        continue;
      }

      attributeDimension.setAttribute(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), attributeDimension.getAttribute()));
      attributeDimension.setLegendSet(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), attributeDimension.getLegendSet()));

      preheatService.connectReferences(
          attributeDimension, bundle.getPreheat(), bundle.getPreheatIdentifier());

      entityManager.persist(attributeDimension);
    }
  }

  private void handleProgramIndicatorDimensions(
      EntityManager entityManager,
      Schema schema,
      BaseAnalyticalObject analyticalObject,
      ObjectBundle bundle) {
    if (!schema.hasPersistedProperty("programIndicatorDimensions")) return;

    for (TrackedEntityProgramIndicatorDimension programIndicatorDimension :
        analyticalObject.getProgramIndicatorDimensions()) {
      if (programIndicatorDimension == null) {
        continue;
      }

      programIndicatorDimension.setProgramIndicator(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), programIndicatorDimension.getProgramIndicator()));
      programIndicatorDimension.setLegendSet(
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), programIndicatorDimension.getLegendSet()));

      preheatService.connectReferences(
          programIndicatorDimension, bundle.getPreheat(), bundle.getPreheatIdentifier());
      entityManager.persist(programIndicatorDimension);
    }
  }
}
