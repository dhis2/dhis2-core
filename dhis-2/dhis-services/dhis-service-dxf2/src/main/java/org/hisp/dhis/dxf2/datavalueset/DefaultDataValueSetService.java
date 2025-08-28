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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;

import com.google.common.collect.Lists;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that a mock BatchHandler factory is being injected.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.dxf2.datavalueset.DataValueSetService")
@RequiredArgsConstructor
public class DefaultDataValueSetService implements DataValueSetService {

  private final IdentifiableObjectManager identifiableObjectManager;

  private final CategoryService categoryService;

  private final OrganisationUnitService organisationUnitService;

  private final PeriodService periodService;

  private final CompleteDataSetRegistrationService registrationService;

  private final DataValueSetStore dataValueSetStore;

  private final InputUtils inputUtils;

  private final AclService aclService;

  private final UserService userService;

  // -------------------------------------------------------------------------
  // DataValueSet implementation
  // -------------------------------------------------------------------------

  /**
   * Note that this needs to be RW TX because {@link PeriodService#reloadIsoPeriods(List)} is RW.
   */
  @Override
  @Transactional
  public DataExportParams getFromUrl(DataValueSetQueryParams urlParams) {
    DataExportParams params = new DataExportParams();
    IdSchemes inputIdSchemes = urlParams.getInputIdSchemes();

    if (!isEmpty(urlParams.getDataSet())) {
      params
          .getDataSets()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataSet.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataSetIdScheme),
                  urlParams.getDataSet()));
    }

    if (!isEmpty(urlParams.getDataElementGroup())) {
      params
          .getDataElementGroups()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataElementGroup.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataElementGroupIdScheme),
                  urlParams.getDataElementGroup()));
    }

    if (!isEmpty(urlParams.getDataElement())) {
      params
          .getDataElements()
          .addAll(
              identifiableObjectManager.getObjects(
                  DataElement.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getDataElementIdScheme),
                  urlParams.getDataElement()));
    }

    if (!isEmpty(urlParams.getPeriod())) {
      params
          .getPeriods()
          .addAll(periodService.reloadIsoPeriods(new ArrayList<>(urlParams.getPeriod())));
    } else if (urlParams.getStartDate() != null && urlParams.getEndDate() != null) {
      params.setStartDate(urlParams.getStartDate()).setEndDate(urlParams.getEndDate());
    }

    if (!isEmpty(urlParams.getOrgUnit())) {
      params
          .getOrganisationUnits()
          .addAll(
              identifiableObjectManager.getObjects(
                  OrganisationUnit.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getOrgUnitIdScheme),
                  urlParams.getOrgUnit()));
    }

    if (!isEmpty(urlParams.getOrgUnitGroup())) {
      params
          .getOrganisationUnitGroups()
          .addAll(
              identifiableObjectManager.getObjects(
                  OrganisationUnitGroup.class,
                  IdentifiableProperty.in(inputIdSchemes, IdSchemes::getOrgUnitGroupIdScheme),
                  urlParams.getOrgUnitGroup()));
    }

    if (!isEmpty(urlParams.getAttributeOptionCombo())) {
      params
          .getAttributeOptionCombos()
          .addAll(
              identifiableObjectManager.getObjects(
                  CategoryOptionCombo.class,
                  IdentifiableProperty.in(
                      inputIdSchemes, IdSchemes::getAttributeOptionComboIdScheme),
                  urlParams.getAttributeOptionCombo()));
    } else if (urlParams.getAttributeCombo() != null && !isEmpty(urlParams.getAttributeOptions())) {
      params
          .getAttributeOptionCombos()
          .addAll(
              Lists.newArrayList(
                  inputUtils.getAttributeOptionCombo(
                      urlParams.getAttributeCombo(), urlParams.getAttributeOptions())));
    }

    return params
        .setIncludeDescendants(urlParams.isChildren())
        .setIncludeDeleted(urlParams.isIncludeDeleted())
        .setLastUpdated(urlParams.getLastUpdated())
        .setLastUpdatedDuration(urlParams.getLastUpdatedDuration())
        .setLimit(urlParams.getLimit())
        .setOutputIdSchemes(urlParams.getOutputIdSchemes());
  }

  @Override
  public void validate(DataExportParams params) {
    ErrorMessage error = null;

    if (params == null) {
      throw new IllegalQueryException(ErrorCode.E2000);
    }

    if (!params.hasDataElements() && !params.hasDataSets() && !params.hasDataElementGroups()) {
      error = new ErrorMessage(ErrorCode.E2001);
    }

    if (!params.hasPeriods()
        && !params.hasStartEndDate()
        && !params.hasLastUpdated()
        && !params.hasLastUpdatedDuration()) {
      error = new ErrorMessage(ErrorCode.E2002);
    }

    if (params.hasPeriods() && params.hasStartEndDate()) {
      error = new ErrorMessage(ErrorCode.E2003);
    }

    if (params.hasStartEndDate() && params.getStartDate().after(params.getEndDate())) {
      error = new ErrorMessage(ErrorCode.E2004);
    }

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null) {
      error = new ErrorMessage(ErrorCode.E2005);
    }

    if (!params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2006);
    }

    if (params.isIncludeDescendants() && params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2007);
    }

    if (params.isIncludeDescendants() && !params.hasOrganisationUnits()) {
      error = new ErrorMessage(ErrorCode.E2008);
    }

    if (params.hasLimit() && params.getLimit() < 0) {
      error = new ErrorMessage(ErrorCode.E2009, params.getLimit());
    }

    if (error != null) {
      log.warn("Validation failed: " + error);

      throw new IllegalQueryException(error);
    }
  }

  @Override
  public void decideAccess(DataExportParams params) {
    // Verify data set read sharing

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    for (DataSet dataSet : params.getDataSets()) {
      if (!aclService.canDataRead(currentUserDetails, dataSet)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2010, dataSet.getUid()));
      }
    }

    // Verify attribute option combination data read sharing

    for (CategoryOptionCombo optionCombo : params.getAttributeOptionCombos()) {
      if (!aclService.canDataRead(currentUserDetails, optionCombo)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2011, optionCombo.getUid()));
      }
    }

    // Verify org unit being located within user data capture hierarchy

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    for (OrganisationUnit unit : params.getOrganisationUnits()) {
      if (!organisationUnitService.isInUserDataViewHierarchy(currentUser, unit)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2012, unit.getUid()));
      }
    }
  }

  // -------------------------------------------------------------------------
  // Write
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void exportDataValueSetXml(DataExportParams params, OutputStream out) {
    decideAccess(params);
    validate(params);

    dataValueSetStore.exportDataValueSetXml(params, getCompleteDate(params), out);
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(DataExportParams params, OutputStream out) {
    decideAccess(params);
    validate(params);

    dataValueSetStore.exportDataValueSetJson(params, getCompleteDate(params), out);
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes) {
    dataValueSetStore.exportDataValueSetJson(lastUpdated, outputStream, idSchemes);
  }

  @Override
  @Transactional
  public void exportDataValueSetJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes, int pageSize, int page) {
    dataValueSetStore.exportDataValueSetJson(lastUpdated, outputStream, idSchemes, pageSize, page);
  }

  @Override
  @Transactional
  public void exportDataValueSetCsv(DataExportParams params, Writer writer) {
    decideAccess(params);
    validate(params);

    dataValueSetStore.exportDataValueSetCsv(params, getCompleteDate(params), writer);
  }

  private Date getCompleteDate(DataExportParams params) {
    if (params.isSingleDataValueSet()) {
      CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo(); // TODO

      CompleteDataSetRegistration registration =
          registrationService.getCompleteDataSetRegistration(
              params.getFirstDataSet(),
              params.getFirstPeriod(),
              params.getFirstOrganisationUnit(),
              optionCombo);

      return registration != null ? registration.getDate() : null;
    }

    return null;
  }

  // -------------------------------------------------------------------------
  // Template
  // -------------------------------------------------------------------------

  @Override
  public RootNode getDataValueSetTemplate(
      DataSet dataSet,
      Period period,
      List<String> orgUnits,
      boolean writeComments,
      String ouScheme,
      String deScheme) {
    RootNode rootNode = new RootNode("dataValueSet");
    rootNode.setNamespace(DxfNamespaces.DXF_2_0);
    rootNode.setComment("Data set: " + dataSet.getDisplayName() + " (" + dataSet.getUid() + ")");

    CollectionNode collectionNode = rootNode.addChild(new CollectionNode("dataValues"));
    collectionNode.setWrapping(false);

    if (orgUnits.isEmpty()) {
      for (DataElement dataElement : dataSet.getDataElements()) {
        CollectionNode collection =
            getDataValueTemplate(dataElement, deScheme, null, ouScheme, period, writeComments);
        collectionNode.addChildren(collection.getChildren());
      }
    } else {
      for (String orgUnit : orgUnits) {
        OrganisationUnit organisationUnit =
            identifiableObjectManager.search(OrganisationUnit.class, orgUnit);

        if (organisationUnit == null) {
          continue;
        }

        for (DataElement dataElement : dataSet.getDataElements()) {
          CollectionNode collection =
              getDataValueTemplate(
                  dataElement, deScheme, organisationUnit, ouScheme, period, writeComments);
          collectionNode.addChildren(collection.getChildren());
        }
      }
    }

    return rootNode;
  }

  private CollectionNode getDataValueTemplate(
      DataElement dataElement,
      String deScheme,
      OrganisationUnit organisationUnit,
      String ouScheme,
      Period period,
      boolean comment) {
    CollectionNode collectionNode = new CollectionNode("dataValues");
    collectionNode.setWrapping(false);

    for (CategoryOptionCombo categoryOptionCombo : dataElement.getSortedCategoryOptionCombos()) {
      ComplexNode complexNode = collectionNode.addChild(new ComplexNode("dataValue"));

      String label = dataElement.getDisplayName();

      if (!categoryOptionCombo.isDefault()) {
        label += " " + categoryOptionCombo.getDisplayName();
      }

      if (comment) {
        complexNode.setComment("Data element: " + label);
      }

      if (IdentifiableProperty.CODE.toString().toLowerCase().equals(deScheme.toLowerCase())) {
        SimpleNode simpleNode =
            complexNode.addChild(new SimpleNode("dataElement", dataElement.getCode()));
        simpleNode.setAttribute(true);
      } else {
        SimpleNode simpleNode =
            complexNode.addChild(new SimpleNode("dataElement", dataElement.getUid()));
        simpleNode.setAttribute(true);
      }

      SimpleNode simpleNode =
          complexNode.addChild(new SimpleNode("categoryOptionCombo", categoryOptionCombo.getUid()));
      simpleNode.setAttribute(true);

      simpleNode =
          complexNode.addChild(new SimpleNode("period", period != null ? period.getIsoDate() : ""));
      simpleNode.setAttribute(true);

      if (organisationUnit != null) {
        if (IdentifiableProperty.CODE.toString().equalsIgnoreCase(ouScheme)) {
          simpleNode =
              complexNode.addChild(
                  new SimpleNode(
                      "orgUnit",
                      organisationUnit.getCode() == null ? "" : organisationUnit.getCode()));
          simpleNode.setAttribute(true);
        } else {
          simpleNode =
              complexNode.addChild(
                  new SimpleNode(
                      "orgUnit",
                      organisationUnit.getUid() == null ? "" : organisationUnit.getUid()));
          simpleNode.setAttribute(true);
        }
      }

      simpleNode = complexNode.addChild(new SimpleNode("value", ""));
      simpleNode.setAttribute(true);
    }

    return collectionNode;
  }
}
