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
package org.hisp.dhis.analytics.shared.component.element.select;

import lombok.Getter;

import org.hisp.dhis.analytics.shared.component.element.Element;
import org.hisp.dhis.analytics.shared.visitor.select.SelectVisitor;

/**
 * EventDateValueElement represents the "select" clause of a SQL statement.
 *
 * @author dusan bernat
 */

@Getter
public class EventDataValueElement extends SelectElement implements Element<SelectVisitor>
{
    private final String eventDataValue;

    private final String programUid;

    private final String alias;

    public EventDataValueElement( String trackedEntityTypeUid, String eventDataValue, String programUid, String alias )
    {
        super( trackedEntityTypeUid );
        this.eventDataValue = eventDataValue;
        this.programUid = programUid;
        this.alias = alias;
    }

    /**
     * see Visitor design pattern
     *
     * @param v
     */
    @Override
    public void accept( SelectVisitor v )
    {
        v.visit( this );
    }
}
