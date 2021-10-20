/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.bundle.persister;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;

@Data
class TrackedEntityAttributeValueContext
{

    private final Session session;

    private final TrackerPreheat trackerPreheat;

    private final TrackedEntityInstance trackedEntityInstance;

    private Map<String, TrackedEntityAttributeValue> valuesByUids;

    private Attribute attributeFromPayload;

    TrackedEntityAttributeValueContext( Session session, TrackerPreheat trackerPreheat,
        TrackedEntityInstance trackedEntityInstance )
    {
        this.session = session;
        this.trackerPreheat = trackerPreheat;
        this.trackedEntityInstance = trackedEntityInstance;
        this.valuesByUids = trackedEntityInstance.getTrackedEntityAttributeValues().stream()
            .collect( Collectors.toMap( teav -> teav.getAttribute().getUid(), Function.identity() ) );
    }

    TrackedEntityAttributeValueContext withAttributeFromPayload( Attribute attribute )
    {
        this.attributeFromPayload = attribute;
        return this;
    }

    TrackedEntityAttributeValue getTrackedEntityAttributeValueFromMap()
    {
        return valuesByUids.get( attributeFromPayload.getAttribute() );
    }

    TrackedEntityAttribute getTrackedEntityAttributeFromPreheat()
    {
        TrackedEntityAttribute trackedEntityAttribute = trackerPreheat.get( TrackedEntityAttribute.class,
            attributeFromPayload.getAttribute() );

        checkNotNull( trackedEntityAttribute,
            "Attribute " + attributeFromPayload.getAttribute()
                + " should never be NULL here if validation is enforced before commit." );

        return trackedEntityAttribute;
    }

    boolean isNewAttribute()
    {
        return Objects.isNull( getTrackedEntityAttributeValueFromMap() );
    }

    boolean isDelete()
    {
        // We cannot get the value from attributeToStore because it uses
        // encryption logic, so we need to use the one from payload
        return StringUtils.isEmpty( getAttributeFromPayload().getValue() );
    }

    boolean isFileResource()
    {
        return getTrackedEntityAttributeFromPreheat().getValueType() == ValueType.FILE_RESOURCE;
    }

    public AuditType getAuditType()
    {
        return isDelete() ? AuditType.DELETE
            : isNewAttribute() ? AuditType.CREATE : AuditType.UPDATE;
    }
}
