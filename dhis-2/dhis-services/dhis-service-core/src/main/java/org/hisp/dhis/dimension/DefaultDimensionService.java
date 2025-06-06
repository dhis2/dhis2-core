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
package org.hisp.dhis.dimension;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.DATA_ELEMENT_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP_SET;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_ESCAPED_SEP;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT_GRANDCHILDREN;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryOptionGroupSetDimension;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.datadimensionitem.DataDimensionItemStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.eventvisualization.Attribute;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramDataElementOptionDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeOptionDimensionItem;
import org.hisp.dhis.schema.MetadataMergeService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.dimension.DimensionService")
public class DefaultDimensionService implements DimensionService {
  private final IdentifiableObjectManager idObjectManager;

  private final CategoryService categoryService;

  private final PeriodService periodService;

  private final OrganisationUnitService organisationUnitService;

  private final AclService aclService;

  private final UserService userService;

  private final MetadataMergeService metadataMergeService;

  private final DataDimensionExtractor dataDimensionExtractor;
  private final DataDimensionItemStore dataDimensionItemStore;

  // --------------------------------------------------------------------------
  // DimensionService implementation
  // --------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<DimensionalItemObject> getCanReadDimensionItems(String uid) {
    DimensionalObject dimension =
        idObjectManager.get(DimensionalObject.DYNAMIC_DIMENSION_CLASSES, uid);

    List<DimensionalItemObject> items = new ArrayList<>();

    if (dimension != null && dimension.hasItems()) {
      items.addAll(
          filterReadableObjects(CurrentUserUtil.getCurrentUserDetails(), dimension.getItems()));
    }

    return items;
  }

  @Override
  @Transactional(readOnly = true)
  public <T extends IdentifiableObject> List<T> filterReadableObjects(
      UserDetails userDetails, List<T> objects) {

    List<T> list = new ArrayList<>(objects);
    list.removeIf(object -> !aclService.canRead(userDetails, object));
    return list;
  }

  @Override
  @Transactional(readOnly = true)
  public DimensionType getDimensionType(String uid) {
    Category cat = idObjectManager.get(Category.class, uid);

    if (cat != null) {
      return DimensionType.CATEGORY;
    }

    DataElementGroupSet degs = idObjectManager.get(DataElementGroupSet.class, uid);

    if (degs != null) {
      return DimensionType.DATA_ELEMENT_GROUP_SET;
    }

    OrganisationUnitGroupSet ougs = idObjectManager.get(OrganisationUnitGroupSet.class, uid);

    if (ougs != null) {
      return DimensionType.ORGANISATION_UNIT_GROUP_SET;
    }

    CategoryOptionGroupSet cogs = idObjectManager.get(CategoryOptionGroupSet.class, uid);

    if (cogs != null) {
      return DimensionType.CATEGORY_OPTION_GROUP_SET;
    }

    TrackedEntityAttribute tea = idObjectManager.get(TrackedEntityAttribute.class, uid);

    if (tea != null) {
      return DimensionType.PROGRAM_ATTRIBUTE;
    }

    DataElement pde = idObjectManager.get(DataElement.class, uid);

    if (pde != null && DataElementDomain.TRACKER.equals(pde.getDomainType())) {
      return DimensionType.PROGRAM_DATA_ELEMENT;
    }

    ProgramIndicator pin = idObjectManager.get(ProgramIndicator.class, uid);

    if (pin != null) {
      return DimensionType.PROGRAM_INDICATOR;
    }

    Map<String, DimensionType> dimObjectTypeMap = new HashMap<>();

    dimObjectTypeMap.put(DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X);
    dimObjectTypeMap.put(DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD);
    dimObjectTypeMap.put(DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT);

    return dimObjectTypeMap.get(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DimensionalObject> getAllDimensions() {
    Collection<Category> dcs = idObjectManager.getDataDimensions(Category.class);
    Collection<CategoryOptionGroupSet> cogs =
        idObjectManager.getDataDimensions(CategoryOptionGroupSet.class);
    Collection<DataElementGroupSet> degs =
        idObjectManager.getDataDimensions(DataElementGroupSet.class);
    Collection<OrganisationUnitGroupSet> ougs =
        idObjectManager.getDataDimensions(OrganisationUnitGroupSet.class);

    List<DimensionalObject> dimensions = new ArrayList<>();

    dimensions.addAll(dcs);
    dimensions.addAll(cogs);
    dimensions.addAll(degs);
    dimensions.addAll(ougs);

    return filterReadableObjects(CurrentUserUtil.getCurrentUserDetails(), dimensions);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DimensionalObject> getDimensionConstraints() {
    Collection<CategoryOptionGroupSet> cogs =
        idObjectManager.getDataDimensions(CategoryOptionGroupSet.class);
    Collection<Category> cs = categoryService.getAttributeCategories();

    List<DimensionalObject> dimensions = new ArrayList<>();

    dimensions.addAll(cogs);
    dimensions.addAll(cs);

    return dimensions;
  }

  @Override
  @Transactional
  public void mergeAnalyticalObject(BaseAnalyticalObject object) {
    if (object != null) {
      object.clear();

      if (object.getCreatedBy() != null) {
        object.setCreatedBy(idObjectManager.get(User.class, object.getCreatedBy().getUid()));
      } else {
        User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
        object.setCreatedBy(currentUser);
      }

      mergeDimensionalObjects(object, object.getColumns());
      mergeDimensionalObjects(object, object.getRows());
      mergeDimensionalObjects(object, object.getFilters());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void mergeEventAnalyticalObject(EventAnalyticalObject object) {
    if (object != null) {
      if (object.getValue() != null) {
        String uid = object.getValue().getUid();

        DataElement dataElement = idObjectManager.get(DataElement.class, uid);

        if (dataElement != null) {
          object.setDataElementValueDimension(dataElement);
        }

        TrackedEntityAttribute attribute = idObjectManager.get(TrackedEntityAttribute.class, uid);

        if (attribute != null) {
          object.setAttributeValueDimension(attribute);
        }
      }

      populateEventRepetitions(object);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public DimensionalObject getDimensionalObjectCopy(String uid, boolean filterCanRead)
      throws NotFoundException {
    DimensionalObject dimension =
        idObjectManager.get(DimensionalObject.DYNAMIC_DIMENSION_CLASSES, uid);
    if (dimension == null) {
      throw new NotFoundException("Dimension does not exist: " + uid);
    }
    DimensionalObject copy = metadataMergeService.clone(dimension);

    if (filterCanRead) {
      UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
      List<DimensionalItemObject> items =
          filterReadableObjects(currentUserDetails, dimension.getItems());
      copy.setItems(items);
    }

    return copy;
  }

  @Override
  @Transactional(readOnly = true)
  public DimensionalItemObject getDataDimensionalItemObject(String dimensionItem) {
    return getDataDimensionalItemObject(UID, dimensionItem);
  }

  @Override
  @Transactional(readOnly = true)
  public DimensionalItemObject getDataDimensionalItemObject(
      IdScheme idScheme, String dimensionItem) {
    if (DimensionalObjectUtils.isCompositeDimensionalObject(dimensionItem)) {
      String id0 = splitSafe(dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 0);
      String id1 = splitSafe(dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 1);
      String id2 = splitSafe(dimensionItem, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, 2);

      DataElementOperand operand;
      ReportingRate reportingRate;
      ProgramDataElementDimensionItem programDataElement;
      ProgramDataElementOptionDimensionItem programDataElementOption;
      ProgramTrackedEntityAttributeDimensionItem programAttribute;
      ProgramTrackedEntityAttributeOptionDimensionItem programAttributeOption;

      if ((programDataElementOption =
              dataDimensionExtractor.getProgramDataElementOptionDimensionItem(
                  idScheme, id0, id1, id2))
          != null) {
        return programDataElementOption;
      }
      if ((programAttributeOption =
              dataDimensionExtractor.getProgramAttributeOptionDimensionItem(
                  idScheme, id0, id1, id2))
          != null) {
        return programAttributeOption;
      }
      if ((operand = getDataElementOperand(idScheme, id0, id1, id2)) != null) {
        return operand;
      }
      if ((reportingRate = dataDimensionExtractor.getReportingRate(idScheme, id0, id1)) != null) {
        return reportingRate;
      }
      if ((programDataElement =
              dataDimensionExtractor.getProgramDataElementDimensionItem(idScheme, id0, id1))
          != null) {
        return programDataElement;
      }
      if ((programAttribute =
              dataDimensionExtractor.getProgramAttributeDimensionItem(idScheme, id0, id1))
          != null) {
        return programAttribute;
      }
    } else if (!idScheme.is(IdentifiableProperty.UID) || CodeGenerator.isValidUid(dimensionItem)) {
      return idObjectManager.get(DataDimensionItem.DATA_DIM_CLASSES, idScheme, dimensionItem);
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public DimensionalItemObject getDataDimensionalItemObject(DimensionalItemId itemId) {
    Collection<DimensionalItemObject> items =
        getDataDimensionalItemObjectMap(Sets.newHashSet(itemId)).values();

    return items.isEmpty() ? null : items.iterator().next();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<DimensionalItemId, DimensionalItemObject> getDataDimensionalItemObjectMap(
      Set<DimensionalItemId> itemIds) {
    SetMap<Class<? extends IdentifiableObject>, String> atomicIds =
        dataDimensionExtractor.getAtomicIds(itemIds);

    MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects =
        dataDimensionExtractor.getAtomicObjects(atomicIds);

    return dataDimensionExtractor.getItemObjectMap(itemIds, atomicObjects);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<DimensionalItemId, DimensionalItemObject> getNoAclDataDimensionalItemObjectMap(
      Set<DimensionalItemId> itemIds) {
    SetMap<Class<? extends IdentifiableObject>, String> atomicIds =
        dataDimensionExtractor.getAtomicIds(itemIds);

    MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> atomicObjects =
        dataDimensionExtractor.getNoAclAtomicObjects(atomicIds);

    return dataDimensionExtractor.getItemObjectMap(itemIds, atomicObjects);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataDimensionItem> getIndicatorDataDimensionItems(List<Indicator> indicators) {
    return dataDimensionItemStore.getIndicatorDataDimensionItems(indicators);
  }

  // --------------------------------------------------------------------------
  // Supportive methods
  // --------------------------------------------------------------------------

  private void populateEventRepetitions(EventAnalyticalObject object) {
    // Populating event repetitions
    object.getEventRepetitions().clear();
    populateEventRepetitions(object, object.getColumns(), COLUMN);
    populateEventRepetitions(object, object.getRows(), ROW);
    populateEventRepetitions(object, object.getFilters(), FILTER);
  }

  private void populateEventRepetitions(
      EventAnalyticalObject object, List<DimensionalObject> dimensionalObjects, Attribute parent) {
    if (isNotEmpty(dimensionalObjects)) {
      for (DimensionalObject dimensionalObject : dimensionalObjects) {
        EventRepetition eventRepetition = dimensionalObject.getEventRepetition();
        String dimension = dimensionalObject.getDimension();
        boolean associateEventRepetition = eventRepetition != null && isNotBlank(dimension);

        if (associateEventRepetition) {
          eventRepetition.setParent(parent);
          eventRepetition.setDimension(dimension);

          if (dimensionalObject.hasProgramStage()) {
            eventRepetition.setProgramStage(dimensionalObject.getProgramStage().getUid());
            eventRepetition.setProgram(dimensionalObject.getProgramStage().getProgram().getUid());
          }

          if (dimensionalObject.hasProgram()) {
            eventRepetition.setProgram(dimensionalObject.getProgram().getUid());
          }

          object.getEventRepetitions().add(eventRepetition);
        }
      }
    }
  }

  /**
   * Returns a {@link DataElementOperand}. For identifier wild cards {@link
   * ExpressionService#SYMBOL_WILDCARD}, the relevant property will be null.
   *
   * @param idScheme the identifier scheme.
   * @param dataElementId the data element identifier.
   * @param categoryOptionComboId the category option combo identifier.
   */
  private DataElementOperand getDataElementOperand(
      IdScheme idScheme,
      String dataElementId,
      String categoryOptionComboId,
      String attributeOptionComboId) {
    DataElement dataElement = idObjectManager.getObject(DataElement.class, idScheme, dataElementId);
    CategoryOptionCombo categoryOptionCombo =
        idObjectManager.getObject(CategoryOptionCombo.class, idScheme, categoryOptionComboId);
    CategoryOptionCombo attributeOptionCombo =
        idObjectManager.getObject(CategoryOptionCombo.class, idScheme, attributeOptionComboId);

    if (dataElement == null
        || (categoryOptionCombo == null && !SYMBOL_WILDCARD.equals(categoryOptionComboId))) {
      return null;
    }

    return new DataElementOperand(dataElement, categoryOptionCombo, attributeOptionCombo);
  }

  /**
   * Loads a program associated with the incoming program stage, if any, so it can be used later in
   * the processing flow.
   *
   * @param dimensionalObject
   */
  private void loadProgramForStage(DimensionalObject dimensionalObject) {
    if (dimensionalObject.hasProgramStage()) {
      // Sometimes we may have the index of the event, so it needs to be removed. ie.:
      // "stage_uid[-2]".
      String stageUid = substringBefore(dimensionalObject.getProgramStage().getUid(), "[");
      ProgramStage programStage = idObjectManager.get(ProgramStage.class, stageUid);

      if (programStage != null) {
        dimensionalObject.getProgramStage().setProgram(programStage.getProgram());
      }
    }
  }

  /**
   * Sets persistent objects for dimensional associations on the given BaseAnalyticalObject based on
   * the given list of transient DimensionalObjects.
   *
   * <p>Relative periods represented by enums are converted into a RelativePeriods object. User
   * organisation units represented by enums are converted and represented by the user organisation
   * unit persisted properties on the BaseAnalyticalObject.
   *
   * @param object the BaseAnalyticalObject to merge.
   * @param dimensions the list of dimensions.
   */
  private void mergeDimensionalObjects(
      BaseAnalyticalObject object, List<DimensionalObject> dimensions) {
    if (object == null || dimensions == null) {
      return;
    }

    for (DimensionalObject dimension : dimensions) {
      String dimensionId = dimension.getDimension();
      loadProgramForStage(dimension);

      DimensionType type = getDimensionType(dimensionId);
      List<DimensionalItemObject> items = dimension.getItems();

      if (items != null) {
        List<String> uids = getUids(items);

        if (DATA_X.equals(type)) {
          for (DimensionalItemObject item : items) {
            DimensionalItemObject dimItemObject = getDataDimensionalItemObject(UID, item.getUid());

            if (dimItemObject != null) {
              DataDimensionItem dataItem = DataDimensionItem.create(dimItemObject);
              object.getDataDimensionItems().add(dataItem);
            }
          }
        } else if (PERIOD.equals(type)) {
          List<Period> periods = new UniqueArrayList<>();
          Set<String> allPeriods = new LinkedHashSet<>();

          for (String period : uids) {
            if (!RelativePeriodEnum.contains(period)) {
              Period isoPeriod = PeriodType.getPeriodFromIsoString(period);

              if (isoPeriod != null) {
                periods.add(isoPeriod);
              }
            }

            allPeriods.add(period);
          }

          object.setRawPeriods(new ArrayList<>(allPeriods));
          object.setPeriods(periodService.reloadPeriods(new ArrayList<>(periods)));
        } else if (ORGANISATION_UNIT.equals(type)) {
          for (String ou : uids) {
            if (ou == null) {
              continue;
            }
            if (KEY_USER_ORGUNIT.equals(ou)) {
              object.setUserOrganisationUnit(true);
            } else if (KEY_USER_ORGUNIT_CHILDREN.equals(ou)) {
              object.setUserOrganisationUnitChildren(true);
            } else if (KEY_USER_ORGUNIT_GRANDCHILDREN.equals(ou)) {
              object.setUserOrganisationUnitGrandChildren(true);
            } else if (ou.startsWith(KEY_LEVEL)) {
              String level = DimensionalObjectUtils.getValueFromKeywordParam(ou);

              Integer orgUnitLevel =
                  organisationUnitService.getOrganisationUnitLevelByLevelOrUid(level);

              if (orgUnitLevel != null) {
                object.getOrganisationUnitLevels().add(orgUnitLevel);
              }
            } else if (ou.startsWith(KEY_ORGUNIT_GROUP)) {
              String uid = DimensionalObjectUtils.getUidFromGroupParam(ou);

              OrganisationUnitGroup group = idObjectManager.get(OrganisationUnitGroup.class, uid);

              if (group != null) {
                object.getItemOrganisationUnitGroups().add(group);
              }
            } else {
              OrganisationUnit unit = idObjectManager.get(OrganisationUnit.class, ou);

              if (unit != null) {
                object.getOrganisationUnits().add(unit);
              }
            }
          }
        } else if (DATA_ELEMENT_GROUP_SET.equals(type)) {
          DataElementGroupSetDimension groupSetDimension = new DataElementGroupSetDimension();
          groupSetDimension.setDimension(
              idObjectManager.get(DataElementGroupSet.class, dimensionId));
          groupSetDimension
              .getItems()
              .addAll(idObjectManager.getByUidOrdered(DataElementGroup.class, uids));

          object.getDataElementGroupSetDimensions().add(groupSetDimension);
        } else if (ORGANISATION_UNIT_GROUP_SET.equals(type)) {
          OrganisationUnitGroupSetDimension groupSetDimension =
              new OrganisationUnitGroupSetDimension();
          groupSetDimension.setDimension(
              idObjectManager.get(OrganisationUnitGroupSet.class, dimensionId));
          groupSetDimension
              .getItems()
              .addAll(idObjectManager.getByUidOrdered(OrganisationUnitGroup.class, uids));

          object.getOrganisationUnitGroupSetDimensions().add(groupSetDimension);
        } else if (CATEGORY.equals(type)) {
          CategoryDimension categoryDimension = new CategoryDimension();
          categoryDimension.setDimension(idObjectManager.get(Category.class, dimensionId));
          categoryDimension
              .getItems()
              .addAll(idObjectManager.getByUidOrdered(CategoryOption.class, uids));

          object.getCategoryDimensions().add(categoryDimension);
        } else if (CATEGORY_OPTION_GROUP_SET.equals(type)) {
          CategoryOptionGroupSetDimension groupSetDimension = new CategoryOptionGroupSetDimension();
          groupSetDimension.setDimension(
              idObjectManager.get(CategoryOptionGroupSet.class, dimensionId));
          groupSetDimension
              .getItems()
              .addAll(idObjectManager.getByUidOrdered(CategoryOptionGroup.class, uids));

          object.getCategoryOptionGroupSetDimensions().add(groupSetDimension);
        } else if (PROGRAM_ATTRIBUTE.equals(type)) {
          TrackedEntityAttributeDimension attributeDimension =
              new TrackedEntityAttributeDimension();
          attributeDimension.setAttribute(
              idObjectManager.get(TrackedEntityAttribute.class, dimensionId));
          attributeDimension.setLegendSet(
              dimension.hasLegendSet()
                  ? idObjectManager.get(LegendSet.class, dimension.getLegendSet().getUid())
                  : null);
          attributeDimension.setFilter(dimension.getFilter());

          object.getAttributeDimensions().add(attributeDimension);
        } else if (PROGRAM_DATA_ELEMENT.equals(type)) {
          TrackedEntityDataElementDimension dataElementDimension =
              new TrackedEntityDataElementDimension();
          dataElementDimension.setDataElement(idObjectManager.get(DataElement.class, dimensionId));
          dataElementDimension.setLegendSet(
              dimension.hasLegendSet()
                  ? idObjectManager.get(LegendSet.class, dimension.getLegendSet().getUid())
                  : null);
          dataElementDimension.setProgramStage(
              dimension.hasProgramStage()
                  ? idObjectManager.get(ProgramStage.class, dimension.getProgramStage().getUid())
                  : null);
          dataElementDimension.setFilter(dimension.getFilter());

          object.getDataElementDimensions().add(dataElementDimension);
        } else if (PROGRAM_INDICATOR.equals(type)) {
          TrackedEntityProgramIndicatorDimension programIndicatorDimension =
              new TrackedEntityProgramIndicatorDimension();
          programIndicatorDimension.setProgramIndicator(
              idObjectManager.get(ProgramIndicator.class, dimensionId));
          programIndicatorDimension.setLegendSet(
              dimension.hasLegendSet()
                  ? idObjectManager.get(LegendSet.class, dimension.getLegendSet().getUid())
                  : null);
          programIndicatorDimension.setFilter(dimension.getFilter());

          object.getProgramIndicatorDimensions().add(programIndicatorDimension);
        }
      }
    }
  }
}
