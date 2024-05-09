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
package org.hisp.dhis.dxf2.deprecated.tracker.relationship;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.dxf2.events.relationship.RelationshipService")
@Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class JacksonRelationshipService extends AbstractRelationshipService {
  public JacksonRelationshipService(
      DbmsManager dbmsManager,
      SchemaService schemaService,
      QueryService queryService,
      TrackerAccessManager trackerAccessManager,
      org.hisp.dhis.relationship.RelationshipService relationshipService,
      TrackedEntityInstanceService trackedEntityInstanceService,
      EnrollmentService enrollmentService,
      EventService eventService,
      TrackedEntityService teiDaoService,
      UserService userService,
      ObjectMapper jsonMapper,
      @Qualifier("xmlMapper") ObjectMapper xmlMapper) {
    checkNotNull(dbmsManager);
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
  protected ImportOptions updateImportOptions(ImportOptions importOptions) {
    if (importOptions == null) {
      importOptions = new ImportOptions();
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (importOptions.getUser() == null) {
      importOptions.setUser(currentUser);
    }

    return importOptions;
  }
}
