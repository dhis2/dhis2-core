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
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.tei.TeiJdbcQuery;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        String trackedEntityTypeUid = CodeGenerator.generateUid();
        TeiJdbcQuery teiJdbcQuery = new TeiJdbcQuery();
        String analyticsTableName = "analytics_tracked_entity_instance_" + trackedEntityTypeUid;

        String sql = "select distinct atei.trackedentityinstanceid as \"tracked entity instance id\" , \n" +
            "atei.trackedentityinstanceuid as \"tracked entity instance uid\",\n" +
            "atei.\"zDhUuAYrxNC\" as \"last name\", atei.\"w75KJ2mc4zz\" as \"first name\",\n" +
            "atei.\"lzgmxybs97q\" as \"unique id\", \n" +
            "COALESCE(\n" +
            "                  (SELECT TRUE\n" +
            "                   FROM " + analyticsTableName + " ateiin\n" +
            "                   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "                     AND ateiin.programuid = 'IpHINAT79UW'\n" +
            "                   LIMIT 1), FALSE) AS \"Child Program Enrollment Date\",\n" +
            "COALESCE(\n" +
            "                  (SELECT TRUE\n" +
            "                   FROM " + analyticsTableName + " ateiin\n" +
            "                   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "                     AND ateiin.programuid = 'ur1Edk5Oe2n'\n" +
            "                   LIMIT 1), FALSE) AS \"TB Program Enrollment Date\",\n" +
            "(SELECT ateiin.enrollmentdate\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programuid = 'IpHINAT79UW'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC\n" +
            "   LIMIT 1) AS \"is enrolled in Child Program\",\t\t\t\t   \n" +
            "(SELECT ateiin.enrollmentdate\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programuid = 'ur1Edk5Oe2n'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC\n" +
            "   LIMIT 1) AS \"is enrolled in TB Program\",\n" +
            "(SELECT ateiin.executiondate\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.programinstanceid = atei.programinstanceid\n" +
            "     AND ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programuid = atei.programuid\n" +
            "     AND ateiin.programuid = 'IpHINAT79UW'\n" +
            "     AND ateiin.programstageuid = 'ZzYYXq4fJie'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC\n" +
            "   LIMIT 1) AS \"Report Date\",   \n" +
            "(SELECT ateiin.executiondate\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programstageuid = 'jdRD35YwbRH'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC\n" +
            "   LIMIT 1) AS \"Stage Sputum Test Report Date\",\n" +
            "(SELECT ateiin.eventdatavalues -> 'cYGaxwK615G' -> 'value'\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.programinstanceid = atei.programinstanceid\n" +
            "     AND ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programuid = 'IpHINAT79UW'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC\n" +
            "   LIMIT 1) AS \"Infant HIV test Result\",\n" +
            "(SELECT ateiin.eventdatavalues -> 'sj3j9Hwc7so' -> 'value'\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.programinstanceid = atei.programinstanceid\n" +
            "     AND ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "     AND ateiin.programuid = 'IpHINAT79UW'\n" +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC\n" +
            "   LIMIT 1) AS \"Child ARVs\",\n" +
            "(SELECT ateiin.eventdatavalues -> 'zocHNQIQBIN' -> 'value'\n" +
            "   FROM " + analyticsTableName + " ateiin\n" +
            "   WHERE ateiin.trackedentityinstanceid = atei.trackedentityinstanceid\n" +
            "   ORDER BY ateiin.enrollmentdate DESC, ateiin.executiondate DESC\n" +
            "   LIMIT 1) AS \"TB  smear microscopy test outcome\"\n" +
            "from " + analyticsTableName + " atei\n" +
            "where atei.\"zDhUuAYrxNC\" = 'Kelly'\n" +
            "and atei.\"w75KJ2mc4zz\" = 'John'\n" +
            "and atei.enrollmentdate  > '2022-01-01'\n" +
            "and atei.programuid in ('ur1Edk5Oe2n', 'IpHINAT79UW')";
        // When
        final Query query = teiJdbcQuery.from( mockTeiParams( trackedEntityTypeUid ) );
        final String fullStatement = query.fullStatement();

        // Then
        assertEquals( sql.replaceAll( "[\\n\\t ]", "" ).toLowerCase(),
            fullStatement.replaceAll( "[\\n\\t ]", "" ).toLowerCase(),
            fullStatement );
    }

    private TeiQueryParams mockTeiParams( String trackedEntityTypeUid )
    {
        TrackedEntityType trackedEntityType = Mockito.mock( TrackedEntityType.class );

        when( trackedEntityType.getUid() ).thenReturn( trackedEntityTypeUid );

        return TeiQueryParams
            .builder()
            .trackedEntityType( trackedEntityType )
            .build();
    }
}
