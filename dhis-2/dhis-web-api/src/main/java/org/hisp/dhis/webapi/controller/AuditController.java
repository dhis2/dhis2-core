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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.commons.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.common.SlimPager;
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
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfilter.Preset;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityDataValueAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/audits")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AuditController {
  private final IdentifiableObjectManager manager;

  private final DataValueAuditService dataValueAuditService;

  private final TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

  private final TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

  private final DataApprovalAuditService dataApprovalAuditService;

  private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

  private final FieldFilterService fieldFilterService;

  private final ContextService contextService;

  private final FileResourceService fileResourceService;

  private final DhisConfigurationProvider dhisConfig;

  private final AclService aclService;

  @GetMapping("/files/{uid}")
  public void getFileAudit(
      @PathVariable String uid, HttpServletResponse response, @CurrentUser User currentUser)
      throws WebMessageException {
    FileResource fileResource = fileResourceService.getFileResource(uid);

    if (fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE) {
      throw new WebMessageException(notFound("No file found with uid '" + uid + "'"));
    }

    if (!fileResourceService.canReadDataValueFile(currentUser, fileResource)) {
      throw new WebMessageException(
          unauthorized(
              "You don't have access to fileResource '"
                  + uid
                  + "' or this fileResource is not available from this endpoint"));
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
          error(
              "Failed fetching the file from storage",
              "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related"));
    }
  }

  @GetMapping("dataValue")
  public RootNode getAggregateDataValueAudit(
      @RequestParam(required = false) List<String> ds,
      @RequestParam(required = false) List<String> de,
      @RequestParam(required = false) List<String> pe,
      @RequestParam(required = false) List<String> ou,
      @RequestParam(required = false) String co,
      @RequestParam(required = false) String cc,
      @RequestParam(required = false) List<AuditType> auditType,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page,
      @RequestParam(required = false, defaultValue = "false") boolean totalPages,
      @CurrentUser User currentUser,
      HttpServletRequest request)
      throws WebMessageException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(Preset.ALL.getFields());
    }

    List<DataElement> dataElements = new ArrayList<>();
    dataElements.addAll(manager.loadByUid(DataElement.class, de));
    dataElements.addAll(getDataElementsByDataSet(ds, currentUser));

    List<Period> periods = getPeriods(pe);
    List<OrganisationUnit> organisationUnits = manager.loadByUid(OrganisationUnit.class, ou);
    CategoryOptionCombo categoryOptionCombo = manager.get(CategoryOptionCombo.class, co);
    CategoryOptionCombo attributeOptionCombo = manager.get(CategoryOptionCombo.class, cc);
    List<AuditType> auditTypes = emptyIfNull(auditType);

    DataValueAuditQueryParams params =
        new DataValueAuditQueryParams()
            .setDataElements(dataElements)
            .setPeriods(periods)
            .setOrgUnits(organisationUnits)
            .setCategoryOptionCombo(categoryOptionCombo)
            .setAttributeOptionCombo(attributeOptionCombo)
            .setAuditTypes(auditTypes);

    List<DataValueAudit> dataValueAudits;
    Node pager = null;

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      dataValueAudits = dataValueAuditService.getDataValueAudits(params);
    } else {
      int requestedPage = Math.max(page, 1);
      int size = Math.max(pageSize, 1);
      // the requested page, not limited to the last page: its depth is what the read costs
      dataValueAudits =
          dataValueAuditService.getDataValueAudits(
              new DataValueAuditQueryParams()
                  .setDataElements(dataElements)
                  .setPeriods(periods)
                  .setOrgUnits(organisationUnits)
                  .setCategoryOptionCombo(categoryOptionCombo)
                  .setAttributeOptionCombo(attributeOptionCombo)
                  .setAuditTypes(auditTypes)
                  .setPager(new SlimPager(requestedPage, size, null)));

      // a paged read has one more entry than the page when there is a next page
      boolean hasNextPage = dataValueAudits.size() > size;
      if (hasNextPage) dataValueAudits = dataValueAudits.subList(0, size);
      Integer total = totalPages ? dataValueAuditService.countDataValueAudits(params) : null;
      pager = createPager(requestedPage, size, total, hasNextPage, getRequestURL(request));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(pager);
    }

    CollectionNode trackedEntityAttributeValueAudits =
        rootNode.addChild(new CollectionNode("dataValueAudits", true));
    trackedEntityAttributeValueAudits.addChildren(
        fieldFilterService
            .toCollectionNode(DataValueAudit.class, new FieldFilterParams(dataValueAudits, fields))
            .getChildren());

    return rootNode;
  }

  /**
   * The pager of the tracker exports on later versions: {@code total} and {@code pageCount} only
   * with {@code totalPages=true}, {@code prevPage} and {@code nextPage} links only when there is
   * such a page.
   */
  private static Node createPager(
      int page, int pageSize, @CheckForNull Integer total, boolean hasNextPage, String requestUrl) {
    Pager totals = total == null ? null : new Pager(page, total, pageSize);
    // a pager with totals reports the last page for a page beyond it
    int current = totals == null ? page : totals.getPage();
    String prevPage = current > 1 ? getPageLink(requestUrl, current - 1) : null;
    String nextPage = hasNextPage ? getPageLink(requestUrl, current + 1) : null;
    if (totals != null) {
      totals.setPrevPage(prevPage);
      totals.setNextPage(nextPage);
      return NodeUtils.createPager(totals);
    }
    ComplexNode pager = new ComplexNode("pager");
    pager.setMetadata(true);
    pager.addChild(new SimpleNode("page", current));
    pager.addChild(new SimpleNode("pageSize", pageSize));
    pager.addChild(new SimpleNode("prevPage", prevPage));
    pager.addChild(new SimpleNode("nextPage", nextPage));
    return pager;
  }

  private static String getPageLink(String requestUrl, int page) {
    return UriComponentsBuilder.fromUriString(requestUrl)
        .replaceQueryParam("page", page)
        .build()
        .toUriString();
  }

  /** The URL of the request, as the tracker exports of later versions build it for page links. */
  private String getRequestURL(HttpServletRequest request) {
    StringBuilder requestURL = new StringBuilder(contextService.getApiPath());
    requestURL.append(request.getPathInfo());
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURL.toString();
    }
    return requestURL.append('?').append(queryString).toString();
  }

  @GetMapping("trackedEntityDataValue")
  public RootNode getTrackedEntityDataValueAudit(
      @RequestParam(required = false) List<String> de,
      @RequestParam(required = false) List<String> ou,
      @RequestParam(required = false) List<String> psi,
      @RequestParam(required = false) List<String> ps,
      @RequestParam(required = false) Date startDate,
      @RequestParam(required = false) Date endDate,
      @RequestParam(required = false) OrganisationUnitSelectionMode ouMode,
      @RequestParam(required = false) List<AuditType> auditType,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page)
      throws WebMessageException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(Preset.ALL.getFields());
    }

    List<DataElement> dataElements = manager.loadByUid(DataElement.class, de);
    List<OrganisationUnit> orgUnits = manager.loadByUid(OrganisationUnit.class, ou);
    List<ProgramStage> programStages = manager.loadByUid(ProgramStage.class, ps);
    List<ProgramStageInstance> programStageInstances =
        manager.loadByUid(ProgramStageInstance.class, psi);
    List<AuditType> auditTypes = emptyIfNull(auditType);

    List<TrackedEntityDataValueAudit> dataValueAudits;
    Pager pager = null;

    TrackedEntityDataValueAuditQueryParams params =
        new TrackedEntityDataValueAuditQueryParams()
            .setDataElements(dataElements)
            .setOrgUnits(orgUnits)
            .setProgramStageInstances(programStageInstances)
            .setProgramStages(programStages)
            .setStartDate(startDate)
            .setEndDate(endDate)
            .setOuMode(ouMode)
            .setAuditTypes(auditTypes);

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      dataValueAudits = trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(params);
    } else {
      int total = trackedEntityDataValueAuditService.countTrackedEntityDataValueAudits(params);

      pager = new Pager(page, total, pageSize);

      dataValueAudits =
          trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(
              new TrackedEntityDataValueAuditQueryParams()
                  .setDataElements(dataElements)
                  .setOrgUnits(orgUnits)
                  .setProgramStageInstances(programStageInstances)
                  .setProgramStages(programStages)
                  .setStartDate(startDate)
                  .setEndDate(endDate)
                  .setOuMode(ouMode)
                  .setAuditTypes(auditTypes)
                  .setPager(pager));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode trackedEntityAttributeValueAudits =
        rootNode.addChild(new CollectionNode("trackedEntityDataValueAudits", true));
    trackedEntityAttributeValueAudits.addChildren(
        fieldFilterService
            .toCollectionNode(
                TrackedEntityDataValueAudit.class, new FieldFilterParams(dataValueAudits, fields))
            .getChildren());

    return rootNode;
  }

  @GetMapping("trackedEntityAttributeValue")
  public RootNode getTrackedEntityAttributeValueAudit(
      @RequestParam(required = false) List<String> tea,
      @RequestParam(required = false) List<String> tei,
      @RequestParam(required = false) List<AuditType> auditType,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page)
      throws WebMessageException {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    List<TrackedEntityAttribute> trackedEntityAttributes =
        manager.loadByUid(TrackedEntityAttribute.class, tea);
    List<TrackedEntityInstance> trackedEntityInstances =
        manager.loadByUid(TrackedEntityInstance.class, tei);
    List<AuditType> auditTypes = emptyIfNull(auditType);

    List<TrackedEntityAttributeValueAudit> attributeValueAudits;
    Pager pager = null;

    TrackedEntityAttributeValueAuditQueryParams params =
        new TrackedEntityAttributeValueAuditQueryParams()
            .setTrackedEntityAttributes(trackedEntityAttributes)
            .setTrackedEntityInstances(trackedEntityInstances)
            .setAuditTypes(auditTypes);

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      attributeValueAudits =
          trackedEntityAttributeValueAuditService.getTrackedEntityAttributeValueAudits(params);
    } else {
      int total =
          trackedEntityAttributeValueAuditService.countTrackedEntityAttributeValueAudits(params);

      pager = new Pager(page, total, pageSize);

      attributeValueAudits =
          trackedEntityAttributeValueAuditService.getTrackedEntityAttributeValueAudits(
              new TrackedEntityAttributeValueAuditQueryParams()
                  .setTrackedEntityAttributes(trackedEntityAttributes)
                  .setTrackedEntityInstances(trackedEntityInstances)
                  .setAuditTypes(auditTypes)
                  .setPager(pager));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode trackedEntityAttributeValueAudits =
        rootNode.addChild(new CollectionNode("trackedEntityAttributeValueAudits", true));
    trackedEntityAttributeValueAudits.addChildren(
        fieldFilterService
            .toCollectionNode(
                TrackedEntityAttributeValueAudit.class,
                new FieldFilterParams(attributeValueAudits, fields))
            .getChildren());

    return rootNode;
  }

  @GetMapping("dataApproval")
  public RootNode getDataApprovalAudit(
      @RequestParam(required = false) List<String> dal,
      @RequestParam(required = false) List<String> wf,
      @RequestParam(required = false) List<String> ou,
      @RequestParam(required = false) List<String> aoc,
      @RequestParam(required = false) Date startDate,
      @RequestParam(required = false) Date endDate,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page) {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(Preset.ALL.getFields());
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

  @GetMapping("trackedEntityInstance")
  public RootNode getTrackedEnityInstanceAudit(
      @RequestParam(required = false) List<String> tei,
      @RequestParam(required = false) List<String> user,
      @RequestParam(required = false) List<AuditType> auditType,
      @RequestParam(required = false) Date startDate,
      @RequestParam(required = false) Date endDate,
      @RequestParam(required = false) Boolean skipPaging,
      @RequestParam(required = false) Boolean paging,
      @RequestParam(required = false, defaultValue = "50") int pageSize,
      @RequestParam(required = false, defaultValue = "1") int page) {
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.addAll(Preset.ALL.getFields());
    }

    List<AuditType> auditTypes = emptyIfNull(auditType);

    TrackedEntityInstanceAuditQueryParams params =
        new TrackedEntityInstanceAuditQueryParams()
            .setTrackedEntityInstances(tei)
            .setUsers(user)
            .setAuditTypes(auditTypes)
            .setStartDate(startDate)
            .setEndDate(endDate);

    List<TrackedEntityInstanceAudit> teiAudits;
    Pager pager = null;

    if (PagerUtils.isSkipPaging(skipPaging, paging)) {
      int total = trackedEntityInstanceAuditService.getTrackedEntityInstanceAuditsCount(params);

      pager = new Pager(page, total, pageSize);

      teiAudits = trackedEntityInstanceAuditService.getTrackedEntityInstanceAudits(params);
    } else {
      teiAudits =
          trackedEntityInstanceAuditService.getTrackedEntityInstanceAudits(
              new TrackedEntityInstanceAuditQueryParams()
                  .setTrackedEntityInstances(tei)
                  .setUsers(user)
                  .setAuditTypes(auditTypes)
                  .setStartDate(startDate)
                  .setEndDate(endDate)
                  .setPager(pager));
    }

    RootNode rootNode = NodeUtils.createMetadata();

    if (pager != null) {
      rootNode.addChild(NodeUtils.createPager(pager));
    }

    CollectionNode trackedEntityInstanceAudits =
        rootNode.addChild(new CollectionNode("trackedEntityInstanceAudits", true));
    trackedEntityInstanceAudits.addChildren(
        fieldFilterService
            .toCollectionNode(
                TrackedEntityInstanceAudit.class, new FieldFilterParams(teiAudits, fields))
            .getChildren());

    return rootNode;
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------------------------------------------

  private List<DataElement> getDataElementsByDataSet(List<String> uids, User currentUser)
      throws WebMessageException {
    List<DataSet> dataSets = manager.loadByUid(DataSet.class, uids);

    List<String> noDataRead =
        dataSets.stream()
            .filter(dataSet -> !aclService.canDataRead(currentUser, dataSet))
            .map(DataSet::getUid)
            .collect(Collectors.toList());
    if (!noDataRead.isEmpty()) {
      throw new IllegalQueryException(ErrorCode.E2010, String.join(",", noDataRead));
    }

    return dataSets.stream()
        .map(ds -> ds.getDataElements())
        .flatMap(Set::stream)
        .collect(Collectors.toList());
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
