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
package org.hisp.dhis.analytics.tei;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.Assert.notNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.shared.Column;
import org.hisp.dhis.analytics.shared.Query;
import org.hisp.dhis.analytics.shared.QueryGenerator;
import org.hisp.dhis.analytics.shared.SqlQuery;
import org.springframework.stereotype.Component;

/**
 * @see QueryGenerator
 *
 * @author maikel arabori
 */
@Component
public class TeiJdbcQuery implements QueryGenerator<TeiParams>
{
    /**
     * @see QueryGenerator#from(Object)
     *
     * @param teiParams
     * @return the built Query object
     * @throws IllegalArgumentException if the given teiParams is null
     */
    @Override
    public Query from( final TeiParams teiParams )
    {
        notNull( teiParams, "The 'teiParams' must not be null" );

        // TODO: build objects below from the teiParams.
        // Probably merge Giuseppe's work in
        // https://github.com/dhis2/dhis2-core/pull/10503/files
        final String fromClause = null;
        final String joinClause = null;
        final String whereClause = null;
        final String closingClauses = null;

        Map<String, String> inputUidMap = new HashMap<>();
        inputUidMap.put( "w75KJ2mc4zz", "First Name" );
        inputUidMap.put( "zDhUuAYrxNC", "LastName" );
        inputUidMap.put( "iESIqZ0R0R0", "DOB" );

        final List<Column> columns = getColumnsForTeavValues( inputUidMap );

        inputUidMap.clear();
        inputUidMap.put( "IpHINAT79UW", "Child Program Enrollment Date" );
        inputUidMap.put( "ur1Edk5Oe2n", "TB Program Enrollment Date" );

        columns.addAll( getColumnsForProgramEnrollmentFlags( inputUidMap ) );

        inputUidMap.clear();
        inputUidMap.put( "IpHINAT79UW", "is enrolled in Child Program" );
        inputUidMap.put( "ur1Edk5Oe2n", "is enrolled in TB Program" );

        columns.addAll( getNestedSelectsForEnrollmentDateValues( inputUidMap ) );

        Map<Pair<String, String>, String> pJsonNodeWithPUidMap = new HashMap<>();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "ZzYYXq4fJie", "IpHINAT79UW" ), "Report Date" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "jdRD35YwbRH", null ), "Stage Sputum Test Report Date" );

        columns.addAll( getNestedSelectsForExecutionDateValues( pJsonNodeWithPUidMap ) );

        pJsonNodeWithPUidMap.clear();
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "cYGaxwK615G", "IpHINAT79UW" ), "Infant HIV test Result" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "sj3j9Hwc7so", "IpHINAT79UW" ), "Child ARV's" );
        pJsonNodeWithPUidMap.put( new ImmutablePair<>( "zocHNQIQBIN", null ), "TB  smear microscopy test outcome" );

        columns.addAll( getNestedSelectsForEventDateValues( pJsonNodeWithPUidMap ) );

        return SqlQuery.builder().columns( columns ).fromClause( fromClause ).joinClause( joinClause )
            .whereClause( whereClause ).closingClauses( closingClauses )
            .build();
    }

    private List<Column> getColumnsForTeavValues( final Map<String, String> teaUidWithAliasMap )
    {
        return teaUidWithAliasMap.keySet().stream().map( uid -> new Column( " (SELECT teav.VALUE " +
            " FROM trackedentityattributevalue teav, " +
            "  trackedentityattribute tea" +
            " WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid " +
            "  AND teav.trackedentityattributeid = tea.trackedentityattributeid " +
            "  AND tea.uid = '" + uid + "' " +
            " LIMIT 1 )", ColumnDataType.TEXT, teaUidWithAliasMap.get( uid ), false, false ) )
            .collect( Collectors.toList() );
    }

    private List<Column> getColumnsForProgramEnrollmentFlags( final Map<String, String> programUidWithAliasMap )
    {
        return programUidWithAliasMap.keySet().stream().map( uid -> new Column( " COALESCE((SELECT TRUE " +
            " FROM program p, " +
            " programinstance pi " +
            " WHERE p.programid = pi.programid " +
            " AND pi.trackedentityinstanceid = t.trackedentityinstanceid " +
            " AND p.uid = '" + uid + "' " +
            " LIMIT 1 " +
            " ), FALSE)", ColumnDataType.BOOLEAN, programUidWithAliasMap.get( uid ), false, false ) )
            .collect( Collectors.toList() );
    }

    private List<Column> getNestedSelectsForEnrollmentDateValues( final Map<String, String> programUidMap )
    {
        return programUidMap.keySet().stream().map( uid -> new Column( "(SELECT pi.enrollmentdate " +
            " FROM programinstance pi, " +
            " program p " +
            " WHERE pi.trackedentityinstanceid = t.trackedentityinstanceid " +
            " AND pi.programid = p.programid " +
            " AND p.uid = '" + uid + "' " +
            " ORDER BY enrollmentdate DESC", ColumnDataType.DATE, programUidMap.get( uid ), false, false ) )
            .collect( Collectors.toList() );
    }

    private List<Column> getNestedSelectsForExecutionDateValues(
        final Map<Pair<String, String>, String> psUidWithPUidMap )
    {
        return psUidWithPUidMap.keySet().stream()
            .map( uidPair -> new Column( isBlank( getProgramUid( uidPair ) ) ? " SELECT psi.executiondate" +
                " FROM programstageinstance psi," +
                " programinstance pi," +
                " programstage ps" +
                " WHERE psi.programinstanceid = pi.programinstanceid" +
                " AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                " AND ps.uid = '" + getProgramStageUid( uidPair ) + "'" +
                " AND psi.programstageid = ps.programstageid" +
                " ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                " LIMIT 1"
                : " SELECT psi.executiondate" +
                    " FROM programstageinstance psi," +
                    " programinstance pi," +
                    " program p," +
                    " programstage ps" +
                    " WHERE psi.programinstanceid = pi.programinstanceid" +
                    " AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                    " AND pi.programid = p.programid" +
                    " AND p.uid = '" + getProgramUid( uidPair ) + "'" +
                    " AND ps.uid = '" + getProgramStageUid( uidPair ) + "'" +
                    " AND ps.programid = p.programid" +
                    " ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC",
                ColumnDataType.DATE,
                psUidWithPUidMap.get( uidPair ), false, false ) )
            .collect( Collectors.toList() );
    }

    private List<Column> getNestedSelectsForEventDateValues(
        final Map<Pair<String, String>, String> pJsonNodeWithPUidMap )
    {
        return pJsonNodeWithPUidMap.keySet().stream()
            .map( uidPair -> new Column( isBlank( getProgramUid( uidPair ) )
                ? " SELECT psi.eventdatavalues -> 'cYGaxwK615G' -> 'value'" +
                    "  FROM programstageinstance psi," +
                    "  programinstance pi," +
                    "  program p" +
                    "  WHERE psi.programinstanceid = pi.programinstanceid" +
                    "  AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                    "  AND pi.programid = p.programid" +
                    "  AND p.uid = '" + getProgramUid( uidPair ) + "'" +
                    "  ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                    "  LIMIT 1"
                : " SELECT psi.eventdatavalues -> '" + getJsonNodeUid( uidPair ) + "' -> 'value'" +
                    "  FROM programstageinstance psi," +
                    "  programinstance pi" +
                    "  WHERE psi.programinstanceid = pi.programinstanceid" +
                    "  AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
                    "  ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC" +
                    "  LIMIT 1",
                ColumnDataType.DATE, pJsonNodeWithPUidMap.get( uidPair ), false, false ) )
            .collect( Collectors.toList() );
    }

    private String getProgramUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getRight();
    }

    private String getProgramStageUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getLeft();
    }

    private String getJsonNodeUid( Pair<String, String> pUidWithPsUid )
    {
        return pUidWithPsUid.getLeft();
    }

}
