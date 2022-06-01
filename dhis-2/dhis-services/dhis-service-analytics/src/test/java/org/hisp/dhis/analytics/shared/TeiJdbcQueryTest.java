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
package org.hisp.dhis.analytics.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.tei.TeiJdbcQuery;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.junit.jupiter.api.Test;

/**
 * // TODO: Improve unit tests and coverage
 *
 * Unit tests for {@link SqlQuery}
 *
 * @author maikel arabori
 */
class TeiJdbcQueryTest
{
    @Test
    void testFullStatementSuccessfully()
    {
        // Given
        TeiJdbcQuery teiJdbcQuery = new TeiJdbcQuery();
        String sql = "SELECT t.trackedentityinstanceid AS \"tracked entity instance id\",\n" +
            "       t.uid AS \"uid\",\n" +
            "  (SELECT teav.VALUE\n" +
            "   FROM trackedentityattributevalue teav,\n" +
            "        trackedentityattribute tea\n" +
            "   WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND teav.trackedentityattributeid = tea.trackedentityattributeid\n" +
            "     AND tea.uid = 'zDhUuAYrxNC'\n" +
            "   LIMIT 1) AS \"LastName\",\n" +
            "\n" +
            "  (SELECT teav.VALUE\n" +
            "   FROM trackedentityattributevalue teav,\n" +
            "        trackedentityattribute tea\n" +
            "   WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND teav.trackedentityattributeid = tea.trackedentityattributeid\n" +
            "     AND tea.uid = 'w75KJ2mc4zz'\n" +
            "   LIMIT 1) AS \"First Name\",\n" +
            "\n" +
            "  (SELECT teav.VALUE\n" +
            "   FROM trackedentityattributevalue teav,\n" +
            "        trackedentityattribute tea\n" +
            "   WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND teav.trackedentityattributeid = tea.trackedentityattributeid\n" +
            "     AND tea.uid = 'iESIqZ0R0R0'\n" +
            "   LIMIT 1) AS \"DOB\",\n" +
            "       COALESCE(\n" +
            "                  (SELECT TRUE\n" +
            "                   FROM program p, programinstance pi\n" +
            "                   WHERE p.programid = pi.programid\n" +
            "                     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "                     AND p.uid = 'IpHINAT79UW'\n" +
            "                   LIMIT 1), FALSE) AS \"Child Program Enrollment Date\",\n" +
            "       COALESCE(\n" +
            "                  (SELECT TRUE\n" +
            "                   FROM program p, programinstance pi\n" +
            "                   WHERE p.programid = pi.programid\n" +
            "                     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "                     AND p.uid = 'ur1Edk5Oe2n'\n" +
            "                   LIMIT 1), FALSE) AS \"TB Program Enrollment Date\",\n" +
            "\n" +
            "  (SELECT pi.enrollmentdate\n" +
            "   FROM programinstance pi,\n" +
            "        program p\n" +
            "   WHERE pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND pi.programid = p.programid\n" +
            "     AND p.uid = 'IpHINAT79UW'\n" +
            "   ORDER BY enrollmentdate DESC\n" +
            "   LIMIT 1) AS \"is enrolled in Child Program\",\n" +
            "\n" +
            "  (SELECT pi.enrollmentdate\n" +
            "   FROM programinstance pi,\n" +
            "        program p\n" +
            "   WHERE pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND pi.programid = p.programid\n" +
            "     AND p.uid = 'ur1Edk5Oe2n'\n" +
            "   ORDER BY enrollmentdate DESC\n" +
            "   LIMIT 1) AS \"is enrolled in TB Program\",\n" +
            "\n" +
            "  (SELECT psi.executiondate\n" +
            "   FROM programstageinstance psi,\n" +
            "        programinstance pi,\n" +
            "        program p,\n" +
            "        programstage ps\n" +
            "   WHERE psi.programinstanceid = pi.programinstanceid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND pi.programid = p.programid\n" +
            "     AND p.uid = 'IpHINAT79UW'\n" +
            "     AND ps.uid = 'ZzYYXq4fJie'\n" +
            "     AND ps.programid = p.programid\n" +
            "   ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC\n" +
            "   LIMIT 1) AS \"Report Date\",\n" +
            "\n" +
            "  (SELECT psi.executiondate\n" +
            "   FROM programstageinstance psi,\n" +
            "        programinstance pi,\n" +
            "        programstage ps\n" +
            "   WHERE psi.programinstanceid = pi.programinstanceid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND ps.uid = 'jdRD35YwbRH'\n" +
            "     AND psi.programstageid = ps.programstageid\n" +
            "   ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC\n" +
            "   LIMIT 1) AS \"Stage Sputum Test Report Date\",\n" +
            "\n" +
            "  (SELECT psi.eventdatavalues -> 'cYGaxwK615G' -> 'value'\n" +
            "   FROM programstageinstance psi,\n" +
            "        programinstance pi,\n" +
            "        program p\n" +
            "   WHERE psi.programinstanceid = pi.programinstanceid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND pi.programid = p.programid\n" +
            "     AND p.uid = 'IpHINAT79UW'\n" +
            "   ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC\n" +
            "   LIMIT 1) AS \"Infant HIV test Result\",\n" +
            "\n" +
            "  (SELECT psi.eventdatavalues -> 'sj3j9Hwc7so' -> 'value'\n" +
            "   FROM programstageinstance psi,\n" +
            "        programinstance pi,\n" +
            "        program p\n" +
            "   WHERE psi.programinstanceid = pi.programinstanceid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND pi.programid = p.programid\n" +
            "     AND p.uid = 'IpHINAT79UW'\n" +
            "   ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC\n" +
            "   LIMIT 1) AS \"Child ARVs\",\n" +
            "\n" +
            "  (SELECT psi.eventdatavalues -> 'zocHNQIQBIN' -> 'value'\n" +
            "   FROM programstageinstance psi,\n" +
            "        programinstance pi\n" +
            "   WHERE psi.programinstanceid = pi.programinstanceid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "   ORDER BY pi.enrollmentdate DESC, psi.executiondate DESC\n" +
            "   LIMIT 1) AS \"TB  smear microscopy test outcome\"\n" +
            "FROM trackedentityinstance t where\n" +
            "  (SELECT teav.VALUE\n" +
            "   FROM trackedentityattributevalue teav, trackedentityattribute tea\n" +
            "   WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND teav.trackedentityattributeid = tea.trackedentityattributeid\n" +
            "     AND tea.uid = 'zDhUuAYrxNC'\n" +
            "   LIMIT 1) = 'Kelly'\n" +
            "AND\n" +
            "  (SELECT teav.VALUE\n" +
            "   FROM trackedentityattributevalue teav,\n" +
            "        trackedentityattribute tea\n" +
            "   WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND teav.trackedentityattributeid = tea.trackedentityattributeid\n" +
            "     AND tea.uid = 'w75KJ2mc4zz'\n" +
            "   LIMIT 1) = 'John'\n" +
            "AND exists\n" +
            "  (SELECT 1\n" +
            "   FROM programinstance pi, program p\n" +
            "   WHERE pi.programid = p.programid\n" +
            "     AND pi.trackedentityinstanceid = t.trackedentityinstanceid\n" +
            "     AND p.uid IN ('ur1Edk5Oe2n', 'IpHINAT79UW')\n" +
            "     AND pi.enrollmentdate > '2022-01-01' )";
        // When
        final Query query = teiJdbcQuery.from( TeiQueryParams.builder().build() );
        final String fullStatement = query.fullStatement();

        // Then
        assertEquals( sql.replaceAll( "[\\n\\t ]", "" ).toLowerCase(),
            fullStatement.replaceAll( "[\\n\\t ]", "" ).toLowerCase(),
            fullStatement );
    }
}
