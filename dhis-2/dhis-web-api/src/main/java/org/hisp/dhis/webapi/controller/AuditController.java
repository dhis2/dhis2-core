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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueAuditType;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAudit;
import org.hisp.dhis.trackedentity.TrackedEntityAuditQueryParams;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = Audit.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audits")
public class AuditController {
  private final IdentifiableObjectManager manager;

  private final DataValueAuditService dataValueAuditService;

  private final DataApprovalAuditService dataApprovalAuditService;

  private final TrackedEntityAuditService trackedEntityAuditService;

  private final FieldFilterService fieldFilterService;

  private final ContextService contextService;

  private final FileResourceService fileResourceService;

  private final DhisConfigurationProvider dhisConfig;

  @GetMapping("/files/{uid}")
  public void getFileAudit(
      @OpenApi.Param({UID.class, FileResource.class}) @PathVariable String uid,
      HttpServletResponse response)
      throws WebMessageException {
    FileResource fileResource = fileResourceService.getFileResource(uid);

    if (fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE) {
      throw new WebMessageException(notFound("No file found with uid '" + uid + "'"));
    }

    FileResourceStorageStatus storageStatus = fileResource.getStorageStatus();

    if (storageStatus != FileResourceStorageStatus.STORED) {
      // HTTP 409, for lack of a more suitable status code
      throw new WebMessageException(
          conflict(
                  "The content is being processed and is not available yet. Try again later.",
                  "The content requested is in transit to the file store and will be available at a later time.")
              .setResponse(new FileResourceWebMessageResponse(fileResource)));
    }

    response.setContentType(fileResource.getContentType());
    response.setContentLengthLong(fileResource.getContentLength());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());
    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException e) {
      throw new WebMessageException(
          error(FileResourceStream.EXCEPTION_IO, FileResourceStream.EXCEPTION_IO_DEV));
    }
  }

  @GetMapping("dataValue")
  public RootNode getAggregateDataValueAudit(
      @OpenApi.Param({UID[].class, DataSet.class}) @RequestParam(required = false) List<String> ds,
      @OpenApi.Param({UID[].class, DataElement.class}) @RequestParam(required = false)
          List<String> de,
      @OpenApi.Param(Period[].class) @RequestParam(required = false) List<String> pe,
      @OpenApi.Param({UID[].class, OrganisationUnit.class}) @RequestParam(required = false)
          List<String> ou,
      @OpenApi.Param({UID.class, CategoryOptionCombo.class}) @RequestParam(required = false)
          String co,
      @OpenApi.Param({UID.class, CategoryOptionCombo.class}) @RequestParam(required = false)
          String cc,
      @RequestParam(required = false) List<DataValueAuditType> auditType,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page)
      throws WebMessageException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(FieldPreset.ALL.getFields());
    }

    List<DataElement> dataElements = new ArrayList<>();
    dataElements.addAll(manager.loadByUid(DataElement.class, de));
    dataElements.addAll(getDataElementsByDataSet(ds));

    List<Period> periods = getPeriods(pe);
    List<OrganisationUnit> organisationUnits = manager.loadByUid(OrganisationUnit.class, ou);
    CategoryOptionCombo categoryOptionCombo = manager.get(CategoryOptionCombo.class, co);
    CategoryOptionCombo attributeOptionCombo = manager.get(CategoryOptionCombo.class, cc);
    List<DataValueAuditType> auditTypes = emptyIfNull(auditType);

    DataValueAuditQueryParams params =
        new DataValueAuditQueryParams()
            .setDataElements(dataElements)
            .setPeriods(periods)
            .setOrgUnits(organisationUnits)
            .setCategoryOptionCombo(categoryOptionCombo)
            .setAttributeOptionCombo(attributeOptionCombo)
            .setAuditTypes(auditTypes);

    List<DataValueAudit> dataValueAudits;
    Pager pager = null;

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      dataValueAudits = dataValueAuditService.getDataValueAudits(params);
    } else {
      int total = dataValueAuditService.countDataValueAudits(params);

      pager = new Pager(page, total, pageSize);

      dataValueAudits =
          dataValueAuditService.getDataValueAudits(
              new DataValueAuditQueryParams()
                  .setDataElements(dataElements)
                  .setPeriods(periods)
                  .setOrgUnits(organisationUnits)
                  .setCategoryOptionCombo(categoryOptionCombo)
                  .setAttributeOptionCombo(attributeOptionCombo)
                  .setAuditTypes(auditTypes)
                  .setPager(pager));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode trackedEntityAttributeValueAudits =
        rootNode.addChild(new CollectionNode("dataValueAudits", true));
    trackedEntityAttributeValueAudits.addChildren(
        fieldFilterService
            .toCollectionNode(DataValueAudit.class, new FieldFilterParams(dataValueAudits, fields))
            .getChildren());

    return rootNode;
  }

  @GetMapping("dataApproval")
  public RootNode getDataApprovalAudit(
      @OpenApi.Param({UID[].class, DataApprovalLevel.class}) @RequestParam(required = false)
          List<String> dal,
      @OpenApi.Param({UID[].class, DataApprovalWorkflow.class}) @RequestParam(required = false)
          List<String> wf,
      @OpenApi.Param({UID[].class, OrganisationUnit.class}) @RequestParam(required = false)
          List<String> ou,
      @OpenApi.Param({UID[].class, CategoryOptionCombo.class}) @RequestParam(required = false)
          List<String> aoc,
      @RequestParam(required = false) Date startDate,
      @RequestParam(required = false) Date endDate,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page) {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(FieldPreset.ALL.getFields());
    }

    DataApprovalAuditQueryParams params =
        new DataApprovalAuditQueryParams()
            .setLevels(new HashSet<>(manager.loadByUid(DataApprovalLevel.class, dal)))
            .setWorkflows(new HashSet<>(manager.loadByUid(DataApprovalWorkflow.class, wf)))
            .setOrganisationUnits(new HashSet<>(manager.loadByUid(OrganisationUnit.class, ou)))
            .setAttributeOptionCombos(
                new HashSet<>(manager.loadByUid(CategoryOptionCombo.class, aoc)))
            .setStartDate(startDate)
            .setEndDate(endDate);

    List<DataApprovalAudit> audits = dataApprovalAuditService.getDataApprovalAudits(params);

    Pager pager;
    RootNode rootNode = NodeUtils.createMetadata();

    if (!PagerUtils.isSkipPaging(skipPaging, paging)) {
      pager = new Pager(page, audits.size(), pageSize);

      audits =
          audits.subList(
              pager.getOffset(), Math.min(pager.getOffset() + pager.getPageSize(), audits.size()));

      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode dataApprovalAudits =
        rootNode.addChild(new CollectionNode("dataApprovalAudits", true));
    dataApprovalAudits.addChildren(
        fieldFilterService
            .toCollectionNode(DataApprovalAudit.class, new FieldFilterParams(audits, fields))
            .getChildren());

    return rootNode;
  }

  @GetMapping("trackedEntity")
  public RootNode getTrackedEntityAudit(
      @OpenApi.Param({UID[].class, TrackedEntity.class}) @RequestParam(required = false)
          Set<UID> trackedEntities,
      @OpenApi.Param({UID[].class, User.class}) @RequestParam(required = false) List<String> user,
      @RequestParam(required = false) List<AuditOperationType> auditType,
      @RequestParam(required = false) Date startDate,
      @RequestParam(required = false) Date endDate,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page) {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(FieldPreset.ALL.getFields());
    }

    List<AuditOperationType> auditOperationTypes = emptyIfNull(auditType);

    TrackedEntityAuditQueryParams params =
        new TrackedEntityAuditQueryParams()
            .setTrackedEntities(UID.toValueList(trackedEntities))
            .setUsers(user)
            .setAuditTypes(auditOperationTypes)
            .setStartDate(startDate)
            .setEndDate(endDate);

    List<TrackedEntityAudit> teAudits;
    Pager pager = null;

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      int total = trackedEntityAuditService.getTrackedEntityAuditsCount(params);

      pager = new Pager(page, total, pageSize);

      teAudits = trackedEntityAuditService.getTrackedEntityAudits(params);
    } else {
      teAudits = trackedEntityAuditService.getTrackedEntityAudits(params.setPager(pager));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode audits = rootNode.addChild(new CollectionNode("trackedEntityAudits", true));
    audits.addChildren(
        fieldFilterService
            .toCollectionNode(TrackedEntityAudit.class, new FieldFilterParams(teAudits, fields))
            .getChildren());

    return rootNode;
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------------------------------------------

  private List<DataElement> getDataElementsByDataSet(List<String> uids) {
    List<DataSet> dataSets = manager.loadByUid(DataSet.class, uids);

    return dataSets.stream().map(DataSet::getDataElements).flatMap(Set::stream).toList();
  }

  private List<Period> getPeriods(List<String> isoPeriods) throws WebMessageException {
    if (isoPeriods == null) {
      return new ArrayList<>();
    }

    List<Period> periods = new ArrayList<>();

    for (String pe : isoPeriods) {
      Period period = PeriodType.getPeriodFromIsoString(pe);

      if (period == null) {
        throw new WebMessageException(conflict("Illegal period identifier: " + pe));
      }

      periods.add(period);
    }

    return periods;
  }
}
