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

import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = {
    DebugMapper.class,
    UserGroupAccessMapper.class,
    UserAccessMapper.class,
    ProgramStageMapper.class,
    OrganisationUnitMapper.class,
    ProgramInstanceMapper.class
} )
public interface ProgramStageInstanceMapper extends PreheatMapper<ProgramStageInstance>
{
    ProgramStageInstanceMapper INSTANCE = Mappers.getMapper( ProgramStageInstanceMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses" )
    @Mapping( target = "userAccesses" )
    @Mapping( target = "programStage" )
    @Mapping( target = "status" )
    @Mapping( target = "organisationUnit" )
    @Mapping( target = "created" )
    @Mapping( target = "programInstance" )
    @Mapping( target = "eventDataValues" )
    @Mapping( target = "comments" )
    @Mapping( target = "dueDate" )
    @Mapping( target = "executionDate" )
    @Mapping( target = "completedDate" )
    @Mapping( target = "completedBy" )
    @Mapping( target = "deleted" )
    @Mapping( target = "createdByUserInfo" )
    @Mapping( target = "lastUpdatedByUserInfo" )
    ProgramStageInstance map( ProgramStageInstance programStageInstance );

    Set<UserGroupAccess> mapUserGroupAccessPsi( Set<UserGroupAccess> userGroupAccesses );

    Set<UserAccess> mapUserAccessPsi( Set<UserAccess> userAccesses );
}
