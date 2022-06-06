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
package org.hisp.dhis.analytics.shared.visitor.select;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.shared.Column;
import org.hisp.dhis.analytics.shared.component.element.select.EnrollmentDateValueSelectElement;
import org.hisp.dhis.analytics.shared.component.element.select.EventDataValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ExecutionDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ProgramEnrollmentFlagElement;
import org.hisp.dhis.analytics.shared.component.element.select.SimpleSelectElement;

/**
 * @see SelectVisitor
 *
 * @author dusan bernat
 */
@Getter
public class SelectElementVisitor implements SelectVisitor
{

    private final List<Column> columns = new ArrayList<>();

    /**
     * @see SelectVisitor
     * @param element
     */
    @Override
    public void visit( ProgramEnrollmentFlagElement element )
    {
        columns.add( new Column( " COALESCE( " +
            "                           (SELECT TRUE " +
            "                            FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid()
            + " ateiin " +
            "                            WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
            "                              AND ateiin.programuid = '" + element.getUid() + "' " +
            "                            LIMIT 1), FALSE)", ColumnDataType.BOOLEAN, element.getAlias(), false,
            false ) );
    }

    /**
     * @see SelectVisitor
     * @param element
     */
    @Override
    public void visit( EnrollmentDateValueSelectElement element )
    {
        columns.add( new Column( " (SELECT ateiin.enrollmentdate " +
            "   FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid() + " ateiin " +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
            "     AND ateiin.programuid = '" + element.getUid() + "' " +
            "   ORDER BY ateiin.enrollmentdate DESC " +
            "   LIMIT 1) ", ColumnDataType.DATE, element.getAlias(), false, false ) );
    }

    /**
     * @see SelectVisitor
     * @param element
     */
    @Override
    public void visit( ExecutionDateValueElement element )
    {
        columns.add( new Column( isBlank( element.getProgramUid() ) ? " (SELECT ateiin.executiondate " +
            "   FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid() + " ateiin " +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
            "     AND ateiin.programstageuid = '" + element.getProgramStageUid() + "' " +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC " +
            "   LIMIT 1) "
            : " (SELECT ateiin.executiondate " +
                "   FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid() + " ateiin " +
                "   WHERE ateiin.programinstanceuid = atei.programinstanceuid " +
                "     AND ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
                "     AND ateiin.programuid = atei.programuid " +
                "     AND ateiin.programuid = '" + element.getProgramUid() + "' " +
                "     AND ateiin.programstageuid = '" + element.getProgramStageUid() + "' " +
                "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC " +
                " LIMIT 1 ) ",
            ColumnDataType.DATE,
            element.getAlias(), false, false ) );
    }

    /**
     * @see SelectVisitor
     * @param element
     */
    @Override
    public void visit( EventDataValueElement element )
    {
        columns.add( new Column( isBlank( element.getProgramUid() )
            ? " (SELECT ateiin.eventdatavalues -> '" + element.getEventDataValue() + "' -> 'value' " +
                "   FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid() + " ateiin " +
                "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
                "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC " +
                " LIMIT 1 ) "
            : " (SELECT ateiin.eventdatavalues -> '" + element.getEventDataValue() + "' -> 'value' " +
                "   FROM analytics_tracked_entity_instance_" + element.getTrackedEntityTypeUid() + " ateiin " +
                "   WHERE ateiin.programinstanceuid = atei.programinstanceuid " +
                "     AND ateiin.trackedentityinstanceid = atei.trackedentityinstanceid " +
                "     AND ateiin.programuid = '" + element.getProgramUid() + "' " +
                "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC " +
                " LIMIT 1 )",
            ColumnDataType.DATE, element.getAlias(), false, false ) );
    }

    /**
     * @see SelectVisitor
     * @param element
     */
    @Override
    public void visit( SimpleSelectElement element )
    {
        columns.add( new Column( element.getValue(), ColumnDataType.TEXT, element.getAlias(), false, false ) );
    }
}
