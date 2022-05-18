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
import org.hisp.dhis.analytics.shared.component.element.select.EventDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ExecutionDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.select.ProgramEnrollmentFlagElement;
import org.hisp.dhis.analytics.shared.component.element.select.SimpleSelectElement;
import org.hisp.dhis.analytics.shared.component.element.select.TeaValueSelectElement;

/**
 * @see SelectVisitor
 *
 * @author dusan bernat
 */
@Getter
public class SelectElementVisitor implements SelectVisitor
{
    private final List<Column> columns = new ArrayList<>();

    @Override
    public void visit( TeaValueSelectElement element )
    {
        columns.add( new Column( " ( SELECT teav.VALUE " +
            " FROM trackedentityattributevalue teav, " +
            "  trackedentityattribute tea" +
            " WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid " +
            "  AND teav.trackedentityattributeid = tea.trackedentityattributeid " +
            "  AND tea.uid = '" + element.getUid() + "' " +
            " LIMIT 1 ) ", ColumnDataType.TEXT, element.getAlias(), false, false ) );
    }

    @Override
    public void visit( ProgramEnrollmentFlagElement element )
    {
        columns.add( new Column( " COALESCE((SELECT TRUE " +
            " FROM program p, " +
            " programinstance pi " +
            " WHERE p.programid = pi.programid " +
            " AND pi.trackedentityinstanceid = t.trackedentityinstanceid " +
            " AND p.uid = '" + element.getUid() + "' " +
            " LIMIT 1 " +
            " ), FALSE) ", ColumnDataType.BOOLEAN, element.getAlias(), false, false ) );
    }

    @Override
    public void visit( EnrollmentDateValueSelectElement element )
    {
        columns.add( new Column( " ( SELECT pi.enrollmentdate " +
            " FROM programinstance pi, " +
            " program p " +
            " WHERE pi.trackedentityinstanceid = t.trackedentityinstanceid " +
            " AND pi.programid = p.programid " +
            " AND p.uid = '" + element.getUid() + "' " +
            " ORDER BY enrollmentdate DESC LIMIT 1 )", ColumnDataType.DATE, element.getAlias(), false, false ) );
    }

    @Override
    public void visit( ExecutionDateValueElement element )
    {
        columns.add( new Column( isBlank( element.getProgramUid() ) ? " ( SELECT psi.executiondate" +
            " FROM programstageinstance psi," +
            " programinstance pi," +
            " programstage ps" +
            " WHERE psi.programinstanceid = pi.programinstanceid" +
            " AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
            " AND ps.uid = '" + element.getProgramStageUid() + "'" +
            " AND psi.programstageid = ps.programstageid" +
            " ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
            " LIMIT 1 ) "
            : " ( SELECT psi.executiondate" +
                " FROM programstageinstance psi," +
                " programinstance pi," +
                " program p," +
                " programstage ps" +
                " WHERE psi.programinstanceid = pi.programinstanceid" +
                " AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                " AND pi.programid = p.programid" +
                " AND p.uid = '" + element.getProgramUid() + "'" +
                " AND ps.uid = '" + element.getProgramStageUid() + "'" +
                " AND ps.programid = p.programid" +
                " ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                "  LIMIT 1 ) ",
            ColumnDataType.DATE,
            element.getAlias(), false, false ) );
    }

    @Override
    public void visit( EventDateValueElement element )
    {
        columns.add( new Column( !isBlank( element.getProgramUid() )
            ? " ( SELECT psi.eventdatavalues -> '" + element.getEventDataValue() + "' -> 'value'" +
                "  FROM programstageinstance psi," +
                "  programinstance pi," +
                "  program p" +
                "  WHERE psi.programinstanceid = pi.programinstanceid" +
                "  AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                "  AND pi.programid = p.programid" +
                "  AND p.uid = '" + element.getProgramUid() + "'" +
                "  ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                "  LIMIT 1 ) "
            : " ( SELECT psi.eventdatavalues -> '" + element.getEventDataValue() + "' -> 'value'" +
                "  FROM programstageinstance psi," +
                "  programinstance pi" +
                "  WHERE psi.programinstanceid = pi.programinstanceid" +
                "  AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                "  ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                "  LIMIT 1 )",
            ColumnDataType.DATE, element.getAlias(), false, false ) );
    }

    @Override
    public void visit( SimpleSelectElement element )
    {
        columns.add( new Column( element.getValue(), ColumnDataType.TEXT, "", false, false ) );
    }
}
