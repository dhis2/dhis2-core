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

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.jackson.domain.JsonRoot;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataExportInputParams;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.webapi.utils.FormUtils;
import org.hisp.dhis.webapi.view.ClassPathUriResolver;
import org.hisp.dhis.webapi.webdomain.form.Field;
import org.hisp.dhis.webapi.webdomain.form.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping("/api/dataSets")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class DataSetController extends AbstractCrudController<DataSet, GetObjectListParams> {
  public static final String DSD_TRANSFORM = "/templates/metadata2dsd.xsl";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private DataSetService dataSetService;

  @Autowired private DataEntryFormService dataEntryFormService;

  @Autowired private DataExportPipeline dataExportPipeline;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Autowired private PeriodService periodService;

  @Autowired private InputUtils inputUtils;

  @Autowired
  @Qualifier("xmlMapper")
  protected ObjectMapper xmlMapper;

  // -------------------------------------------------------------------------
  // Controller
  // -------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  @GetMapping(produces = "application/dsd+xml")
  public void getStructureDefinition(
      @RequestParam Map<String, String> parameters, HttpServletResponse response)
      throws IOException, TransformerException {
    MetadataExportParams exportParams = filterMetadataOptions();

    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadataMap =
        exportService.getMetadata(exportParams);

    Metadata metadata = new Metadata();
    metadata.setDataElements((List<DataElement>) metadataMap.get(DataElement.class));
    metadata.setDataSets((List<DataSet>) metadataMap.get(DataSet.class));
    metadata.setCategoryOptionCombos(
        (List<CategoryOptionCombo>) metadataMap.get(CategoryOptionCombo.class));

    InputStream input =
        new ByteArrayInputStream(xmlMapper.writeValueAsString(metadata).getBytes("UTF-8"));

    TransformerFactory tf = TransformerFactory.newInstance();
    // prevent XXE attack
    // sonar vulnerability:
    // https://sonarcloud.io/organizations/dhis2/rules?open=java%3AS2755&rule_key=java%3AS2755
    tf.setAttribute(ACCESS_EXTERNAL_DTD, "");
    tf.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");

    tf.setURIResolver(new ClassPathUriResolver());

    Transformer transformer =
        tf.newTransformer(new StreamSource(new ClassPathResource(DSD_TRANSFORM).getInputStream()));

    transformer.transform(new StreamSource(input), new StreamResult(response.getOutputStream()));
  }

  @GetMapping("/{uid}/version")
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  public Map<String, Integer> getVersion(
      @PathVariable("uid") String uid, @RequestParam Map<String, String> parameters)
      throws Exception {
    DataSet dataSet = manager.get(DataSet.class, uid);

    if (dataSet == null) {
      throw new WebMessageException(conflict("Data set does not exist: " + uid));
    }

    return singletonMap("version", dataSet.getVersion());
  }

  @PostMapping("/{uid}/version")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void bumpVersion(@PathVariable("uid") String uid) throws Exception {
    DataSet dataSet = manager.get(DataSet.class, uid);

    if (dataSet == null) {
      throw new WebMessageException(conflict("Data set does not exist: " + uid));
    }

    dataSet.increaseVersion();

    dataSetService.updateDataSet(dataSet);
  }

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "categoryCombos", value = CategoryCombo[].class)
      })
  @GetMapping("/{uid}/categoryCombos")
  public ResponseEntity<JsonRoot> getCategoryCombinations(
      @PathVariable("uid") String uid, @RequestParam(defaultValue = "*") List<FieldPath> fields)
      throws Exception {
    DataSet dataSet = manager.get(DataSet.class, uid);

    if (dataSet == null) {
      throw new WebMessageException(conflict("Data set does not exist: " + uid));
    }

    List<CategoryCombo> categoryCombos =
        dataSet.getDataSetElements().stream()
            .map(DataSetElement::getResolvedCategoryCombo)
            .distinct()
            .collect(Collectors.toList());

    Collections.sort(categoryCombos);

    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(categoryCombos, fields);

    return ResponseEntity.ok(new JsonRoot("categoryCombos", objectNodes));
  }

  @GetMapping("/{uid}/dataValueSet")
  public @ResponseBody RootNode getDvs(
      @PathVariable("uid") String uid,
      @RequestParam(value = "orgUnitIdScheme", defaultValue = "ID", required = false)
          String orgUnitIdScheme,
      @RequestParam(value = "dataElementIdScheme", defaultValue = "ID", required = false)
          String dataElementIdScheme,
      @RequestParam(value = "period", defaultValue = "", required = false) String period,
      @RequestParam(value = "orgUnit", defaultValue = "", required = false) List<String> orgUnits,
      @RequestParam(value = "comment", defaultValue = "true", required = false) boolean comment)
      throws NotFoundException {

    Period pe = periodService.getPeriod(period);

    return getDataValueSetTemplate(
        getEntity(uid), pe, orgUnits, comment, orgUnitIdScheme, dataElementIdScheme);
  }

  @GetMapping(value = "/{uid}/form", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @ResponseStatus(HttpStatus.OK)
  public Form getFormJson(
      @PathVariable("uid") String uid,
      @RequestParam(value = "ou", required = false) String orgUnit,
      @RequestParam(value = "pe", required = false) String period,
      @RequestParam(value = "categoryOptions", required = false) String categoryOptions,
      @RequestParam(required = false) boolean metaData)
      throws NotFoundException, ConflictException {

    OrganisationUnit ou = manager.get(OrganisationUnit.class, orgUnit);

    if (ou == null) {
      throw new NotFoundException(OrganisationUnit.class, orgUnit);
    }

    Period pe = PeriodType.getPeriodFromIsoString(period);

    return getForm(List.of(getEntity(uid)), ou, pe, categoryOptions, metaData);
  }

  @GetMapping(
      value = "/{uid}/form",
      produces = {APPLICATION_XML_VALUE, TEXT_XML_VALUE})
  public void getFormXml(
      @PathVariable("uid") String uid,
      @RequestParam(value = "ou", required = false) String orgUnit,
      @RequestParam(value = "pe", required = false) String period,
      @RequestParam(value = "catOpts", required = false) String categoryOptions,
      @RequestParam(required = false) boolean metaData,
      HttpServletResponse response)
      throws IOException, NotFoundException, ConflictException {

    OrganisationUnit ou = manager.get(OrganisationUnit.class, orgUnit);

    if (ou == null) {
      throw new NotFoundException(OrganisationUnit.class, orgUnit);
    }

    Period pe = PeriodType.getPeriodFromIsoString(period);

    Form form = getForm(List.of(getEntity(uid)), ou, pe, categoryOptions, metaData);

    renderService.toXml(response.getOutputStream(), form);
  }

  private Form getForm(
      List<DataSet> dataSets,
      OrganisationUnit ou,
      Period pe,
      String categoryOptions,
      boolean metaData)
      throws ConflictException {
    DataSet dataSet = dataSets.get(0);

    Form form = FormUtils.fromDataSet(dataSets.get(0), metaData, null);

    Set<String> options = null;

    if (StringUtils.isNotEmpty(categoryOptions)
        && categoryOptions.startsWith("[")
        && categoryOptions.endsWith("]")) {
      String[] split = categoryOptions.substring(1, categoryOptions.length() - 1).split(",");

      options = new HashSet<>(Lists.newArrayList(split));
    }

    if (ou != null && pe != null) {
      Set<String> attrOptionCombos =
          options == null || options.isEmpty()
              ? Set.of()
              : Set.of(
                  inputUtils
                      .getAttributeOptionCombo(dataSet.getCategoryCombo(), options, IdScheme.UID)
                      .getUid());

      DataExportInputParams params =
          DataExportInputParams.builder()
              .dataElement(
                  dataSets.get(0).getDataElements().stream()
                      .map(DataElement::getUid)
                      .collect(toSet()))
              .period(Set.of(pe.getIsoDate()))
              .orgUnit(Set.of(ou.getUid()))
              .attributeOptionCombo(attrOptionCombos)
              .build();
      Map<String, Field> operandFieldMap = FormUtils.buildCacheMap(form);
      dataExportPipeline.exportToConsumer(
          params,
          dv -> {
            UID dataElement = dv.dataElement();
            UID categoryOptionCombo = dv.categoryOptionCombo();

            Field field = operandFieldMap.get(dataElement + FormUtils.SEP + categoryOptionCombo);

            if (field != null) {
              field.setValue(dv.value());
              field.setComment(dv.comment());
            }
          });
    }

    return form;
  }

  @RequestMapping(
      value = {"/{uid}/customDataEntryForm", "/{uid}/form"},
      method = {RequestMethod.PUT, RequestMethod.POST},
      consumes = TEXT_HTML_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateCustomDataEntryFormHtml(
      @PathVariable("uid") String uid, @RequestBody String formContent) throws NotFoundException {
    DataSet dataSet = getEntity(uid);

    DataEntryForm form = dataSet.getDataEntryForm();

    if (form == null) {
      form = new DataEntryForm(dataSet.getName(), DisplayDensity.NORMAL, formContent);
      dataEntryFormService.addDataEntryForm(form);
      dataSet.setDataEntryForm(form);
    } else {
      form.setHtmlCode(formContent);
      dataEntryFormService.updateDataEntryForm(form);
    }

    dataSet.increaseVersion();
    dataSetService.updateDataSet(dataSet);
  }

  @PostMapping(value = "/{uid}/form", consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateCustomDataEntryFormJson(
      @PathVariable("uid") String uid, HttpServletRequest request) throws WebMessageException {
    DataSet dataSet = dataSetService.getDataSet(uid);

    if (dataSet == null) {
      throw new WebMessageException(notFound("DataSet not found for uid: " + uid));
    }

    DataEntryForm form = dataSet.getDataEntryForm();
    DataEntryForm newForm;

    try {
      newForm = renderService.fromJson(request.getInputStream(), DataEntryForm.class);
    } catch (IOException e) {
      throw new WebMessageException(badRequest("Failed to parse request", e.getMessage()));
    }

    if (form == null) {
      if (!newForm.hasForm()) {
        throw new WebMessageException(badRequest("Missing required parameter 'htmlCode'"));
      }

      newForm.setName(dataSet.getName());
      dataEntryFormService.addDataEntryForm(newForm);
      dataSet.setDataEntryForm(newForm);
    } else {
      if (newForm.getHtmlCode() != null) {
        form.setHtmlCode(dataEntryFormService.prepareDataEntryFormForSave(newForm.getHtmlCode()));
      }

      if (newForm.getStyle() != null) {
        form.setStyle(newForm.getStyle());
      }

      dataEntryFormService.updateDataEntryForm(form);
    }

    dataSet.increaseVersion();
    dataSetService.updateDataSet(dataSet);
  }

  @GetMapping("/{uid}/metadata")
  public ResponseEntity<MetadataExportParams> getDataSetWithDependencies(
      @PathVariable("uid") String pvUid,
      @RequestParam(required = false, defaultValue = "false") boolean download)
      throws WebMessageException {
    DataSet dataSet = dataSetService.getDataSet(pvUid);

    if (dataSet == null) {
      throw new WebMessageException(notFound("DataSet not found for uid: " + pvUid));
    }

    MetadataExportParams exportParams =
        exportService.getParamsFromMap(contextService.getParameterValuesMap());
    exportService.validate(exportParams);
    exportParams.setObjectExportWithDependencies(dataSet);

    return ResponseEntity.ok(exportParams);
  }

  /**
   * Select only the meta-data required to describe form definitions.
   *
   * @return the filtered options.
   */
  private MetadataExportParams filterMetadataOptions() {
    MetadataExportParams params = new MetadataExportParams();
    params.addQuery(Query.of(DataElement.class));
    params.addQuery(Query.of(DataSet.class));
    params.addQuery(Query.of(CategoryOptionCombo.class));

    return params;
  }

  private RootNode getDataValueSetTemplate(
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
