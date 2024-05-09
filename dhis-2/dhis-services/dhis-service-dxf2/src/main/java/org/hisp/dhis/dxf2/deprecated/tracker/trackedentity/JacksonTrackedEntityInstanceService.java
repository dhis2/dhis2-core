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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.TrackedEntityInstanceStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.UserService;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service("org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService")
@Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class JacksonTrackedEntityInstanceService extends AbstractTrackedEntityInstanceService {

  public JacksonTrackedEntityInstanceService(
      TrackedEntityInstanceStore trackedEntityInstanceStore,
      TrackedEntityService teiService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      TrackedEntityAttributeValueService trackedEntityAttributeValueService,
      IdentifiableObjectManager manager,
      UserService userService,
      DbmsManager dbmsManager,
      EnrollmentService programInstanceService,
      SchemaService schemaService,
      QueryService queryService,
      ReservedValueService reservedValueService,
      TrackerAccessManager trackerAccessManager,
      FileResourceService fileResourceService,
      TrackerOwnershipManager trackerOwnershipAccessManager,
      TrackedEntityInstanceAggregate trackedEntityInstanceAggregate,
      TrackedEntityAttributeStore trackedEntityAttributeStore,
      TrackedEntityChangeLogService trackedEntityChangeLogService,
      TrackedEntityTypeService trackedEntityTypeService) {
    checkNotNull(teiService);
    checkNotNull(trackedEntityAttributeService);
    checkNotNull(trackedEntityAttributeValueService);
    checkNotNull(manager);
    checkNotNull(userService);
    checkNotNull(dbmsManager);
    checkNotNull(programInstanceService);
    checkNotNull(schemaService);
    checkNotNull(queryService);
    checkNotNull(reservedValueService);
    checkNotNull(trackerAccessManager);
    checkNotNull(fileResourceService);
    checkNotNull(trackerOwnershipAccessManager);
    checkNotNull(trackedEntityInstanceAggregate);
    checkNotNull(trackedEntityAttributeStore);
    checkNotNull(trackedEntityTypeService);

    this.trackedEntityInstanceStore = trackedEntityInstanceStore;
    this.teiService = teiService;
    this.trackedEntityAttributeService = trackedEntityAttributeService;
    this.trackedEntityAttributeValueService = trackedEntityAttributeValueService;
    this.manager = manager;
    this.userService = userService;
    this.dbmsManager = dbmsManager;
    this.programInstanceService = programInstanceService;
    this.schemaService = schemaService;
    this.queryService = queryService;
    this.reservedValueService = reservedValueService;
    this.trackerAccessManager = trackerAccessManager;
    this.fileResourceService = fileResourceService;
    this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
    this.trackedEntityInstanceAggregate = trackedEntityInstanceAggregate;
    this.trackedEntityAttributeStore = trackedEntityAttributeStore;
    this.trackedEntityChangeLogService = trackedEntityChangeLogService;
    this.trackedEntityTypeService = trackedEntityTypeService;
  }
}
