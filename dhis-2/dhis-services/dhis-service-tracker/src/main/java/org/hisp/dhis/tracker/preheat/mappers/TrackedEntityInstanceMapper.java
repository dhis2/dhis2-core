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
package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {DebugMapper.class, TrackedEntityTypeMapper.class, AttributeValueMapper.class})
public interface TrackedEntityInstanceMapper extends PreheatMapper<TrackedEntityInstance> {
  TrackedEntityInstanceMapper INSTANCE = Mappers.getMapper(TrackedEntityInstanceMapper.class);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id")
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "user")
  @Mapping(target = "publicAccess")
  @Mapping(target = "externalAccess")
  @Mapping(target = "userGroupAccesses", qualifiedByName = "userGroupAccesses")
  @Mapping(target = "userAccesses", qualifiedByName = "userAccesses")
  @Mapping(target = "organisationUnit", qualifiedByName = "organisationUnit")
  @Mapping(target = "trackedEntityType")
  @Mapping(target = "inactive")
  @Mapping(target = "programInstances")
  @Mapping(target = "created")
  @Mapping(target = "trackedEntityAttributeValues")
  @Mapping(target = "deleted")
  @Mapping(target = "createdByUserInfo")
  @Mapping(target = "lastUpdatedByUserInfo")
  TrackedEntityInstance map(TrackedEntityInstance trackedEntityInstance);

  @Named("userGroupAccesses")
  Set<UserGroupAccess> userGroupAccesses(Set<UserGroupAccess> userGroupAccesses);

  @Named("userAccesses")
  Set<UserAccess> mapUserAccessProgramInstance(Set<UserAccess> userAccesses);

  @Named("organisationUnit")
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id")
  @Mapping(target = "uid")
  @Mapping(target = "code")
  @Mapping(target = "name")
  @Mapping(target = "attributeValues")
  @Mapping(target = "user")
  @Mapping(target = "publicAccess")
  @Mapping(target = "externalAccess")
  @Mapping(target = "userGroupAccesses")
  @Mapping(target = "userAccesses")
  OrganisationUnit map(OrganisationUnit organisationUnit);
}
