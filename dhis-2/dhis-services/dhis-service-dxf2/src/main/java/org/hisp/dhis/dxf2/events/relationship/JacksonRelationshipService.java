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
package org.hisp.dhis.dxf2.events.relationship;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.Relationships;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service("org.hisp.dhis.dxf2.events.relationship.RelationshipService")
@Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class JacksonRelationshipService extends AbstractRelationshipService {
  public JacksonRelationshipService(
      DbmsManager dbmsManager,
      CurrentUserService currentUserService,
      SchemaService schemaService,
      QueryService queryService,
      TrackerAccessManager trackerAccessManager,
      org.hisp.dhis.relationship.RelationshipService relationshipService,
      TrackedEntityInstanceService trackedEntityInstanceService,
      EnrollmentService enrollmentService,
      EventService eventService,
      org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiDaoService,
      UserService userService,
      ObjectMapper jsonMapper,
      @Qualifier("xmlMapper") ObjectMapper xmlMapper) {
    checkNotNull(dbmsManager);
    checkNotNull(currentUserService);
    checkNotNull(schemaService);
    checkNotNull(queryService);
    checkNotNull(trackerAccessManager);
    checkNotNull(relationshipService);
    checkNotNull(trackedEntityInstanceService);
    checkNotNull(enrollmentService);
    checkNotNull(eventService);
    checkNotNull(teiDaoService);
    checkNotNull(userService);
    checkNotNull(jsonMapper);
    checkNotNull(xmlMapper);

    this.dbmsManager = dbmsManager;
    this.currentUserService = currentUserService;
    this.schemaService = schemaService;
    this.queryService = queryService;
    this.trackerAccessManager = trackerAccessManager;
    this.relationshipService = relationshipService;
    this.trackedEntityInstanceService = trackedEntityInstanceService;
    this.enrollmentService = enrollmentService;
    this.eventService = eventService;
    this.teiDaoService = teiDaoService;
    this.userService = userService;
    this.jsonMapper = jsonMapper;
    this.xmlMapper = xmlMapper;
  }

  @Override
  public ImportSummaries addRelationshipsJson(InputStream inputStream, ImportOptions importOptions)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.forName("UTF-8"));
    List<Relationship> relationships = new ArrayList<>();

    JsonNode root = jsonMapper.readTree(input);

    if (root.get("relationships") != null) {
      Relationships fromJson = fromJson(input, Relationships.class);
      relationships.addAll(fromJson.getRelationships());
    } else {
      Relationship fromJson = fromJson(input, Relationship.class);
      relationships.add(fromJson);
    }

    return processRelationshipList(relationships, updateImportOptions(importOptions));
  }

  @Override
  public ImportSummaries addRelationshipsXml(InputStream inputStream, ImportOptions importOptions)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.forName("UTF-8"));
    List<Relationship> relationships = new ArrayList<>();

    try {
      Relationships fromXml = fromXml(input, Relationships.class);
      relationships.addAll(fromXml.getRelationships());
    } catch (JsonMappingException ex) {
      Relationship fromXml = fromXml(input, Relationship.class);
      relationships.add(fromXml);
    }

    return processRelationshipList(relationships, updateImportOptions(importOptions));
  }

  @Override
  public ImportSummary updateRelationshipJson(
      String id, InputStream inputStream, ImportOptions importOptions) throws IOException {
    Relationship relationship = fromJson(inputStream, Relationship.class);
    relationship.setRelationship(id);

    return updateRelationship(relationship, updateImportOptions(importOptions));
  }

  @Override
  public ImportSummary updateRelationshipXml(
      String id, InputStream inputStream, ImportOptions importOptions) throws IOException {
    Relationship relationship = fromXml(inputStream, Relationship.class);
    relationship.setRelationship(id);

    return updateRelationship(relationship, updateImportOptions(importOptions));
  }

  @Override
  protected ImportOptions updateImportOptions(ImportOptions importOptions) {
    if (importOptions == null) {
      importOptions = new ImportOptions();
    }

    if (importOptions.getUser() == null) {
      importOptions.setUser(currentUserService.getCurrentUser());
    }

    return importOptions;
  }

  @SuppressWarnings("unchecked")
  private <T> T fromXml(InputStream inputStream, Class<?> clazz) throws IOException {
    return (T) xmlMapper.readValue(inputStream, clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> T fromXml(String input, Class<?> clazz) throws IOException {
    return (T) xmlMapper.readValue(input, clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> T fromJson(InputStream inputStream, Class<?> clazz) throws IOException {
    return (T) jsonMapper.readValue(inputStream, clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> T fromJson(String input, Class<?> clazz) throws IOException {
    return (T) jsonMapper.readValue(input, clazz);
  }
}
