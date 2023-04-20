/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context;

import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.ValueType;

/**
 * This enum represents the TEI static fields. It is used to generate the query
 * and to provide the header information.
 */
@Getter
@RequiredArgsConstructor
public enum TeiStaticField implements TeiHeaderProvider
{
    TRACKED_ENTITY_INSTANCE( "trackedentityinstanceuid", "Tracked entity instance", TEXT ),
    LAST_UPDATED( "lastupdated", "Last updated", DATETIME ),
    CREATED_BY_DISPLAY_NAME( "createdbydisplayname", "Created by", TEXT ),
    LAST_UPDATED_BY_DISPLAY_NAME( "lastupdatedbydisplayname", "Last updated by", TEXT ),
    GEOMETRY( "geometry", "Geometry", TEXT ),
    LONGITUDE( "longitude", "Longitude", NUMBER ),
    LATITUDE( "latitude", "Latitude", NUMBER ),
    ORG_UNIT_NAME( "ouname", "Organisation unit name", TEXT ),
    ORG_UNIT_CODE( "oucode", "Organisation unit code", TEXT ),
    ORG_UNIT_NAME_HIERARCHY( "ounamehierarchy", "Organisation unit hierarchy", TEXT );

    private final String alias;

    private final String fullName;

    private final ValueType type;
}
