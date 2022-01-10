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
package org.hisp.dhis.dxf2.events.event;

import java.util.Map;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

public class EventContext
{
    private final Map<String, TrackedEntityOuInfo> trackedEntityOuInfoByUid;

    private final Map<String, Program> programsByUid;

    private final Map<Pair<String, String>, String> orgUnitByTeiUidAndProgramUidPairs;

    private final Map<String, OrganisationUnit> orgUnitsByUid;

    public EventContext( Map<String, TrackedEntityOuInfo> trackedEntityOuInfoByUid,
        Map<String, Program> programsByUid,
        Map<Pair<String, String>, String> orgUnitByTeiUidAndProgramUidPairs,
        Map<String, OrganisationUnit> orgUnitsByUid )
    {
        this.trackedEntityOuInfoByUid = trackedEntityOuInfoByUid;
        this.programsByUid = programsByUid;
        this.orgUnitByTeiUidAndProgramUidPairs = orgUnitByTeiUidAndProgramUidPairs;
        this.orgUnitsByUid = orgUnitsByUid;
    }

    public Map<String, TrackedEntityOuInfo> getTrackedEntityOuInfoByUid()
    {
        return trackedEntityOuInfoByUid;
    }

    public Map<String, Program> getProgramsByUid()
    {
        return programsByUid;
    }

    public Map<Pair<String, String>, String> getOrgUnitByTeiUidAndProgramUidPairs()
    {
        return orgUnitByTeiUidAndProgramUidPairs;
    }

    public Map<String, OrganisationUnit> getOrgUnitsByUid()
    {
        return orgUnitsByUid;
    }

    @Data
    @RequiredArgsConstructor
    public static class TrackedEntityOuInfo
    {
        private final Long trackerEntityId;

        private final String trackedEntityUid;

        private final Long orgUnitId;
    }
}
