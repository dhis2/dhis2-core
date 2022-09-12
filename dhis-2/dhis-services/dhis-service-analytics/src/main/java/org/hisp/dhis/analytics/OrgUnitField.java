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
package org.hisp.dhis.analytics;

import static com.google.common.base.Enums.getIfPresent;
import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.hisp.dhis.analytics.DataQueryParams.DEFAULT_ORG_UNIT_COL;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.OrgUnitFieldType.ATTRIBUTE;
import static org.hisp.dhis.analytics.OrgUnitFieldType.REGISTRATION;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_GROUPSET_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.OWNERSHIP_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.program.AnalyticsType.EVENT;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.hisp.dhis.program.AnalyticsType;

/**
 * The organisation unit field to use for an event (or enrollment) analytics
 * query. It may be one of the following types:
 * <ul>
 * <li>{@link OrgUnitFieldType#DEFAULT} use the orgUnit in the analytics table.
 * OrgUnit structure columns (and group sets if needed) are included in the
 * analytics table.</li>
 * <li>{@link OrgUnitFieldType#ATTRIBUTE} use an event attribute or data element
 * of type orgUnit. OrgUnit structure tables are joined to the attribute or data
 * element column.</li>
 * <li>{@link OrgUnitFieldType#REGISTRATION} use the TEI registration orgUnit.
 * OrgUnit structure tables are joined to the analytics table registrationou
 * column.</li>
 * <li>{@link OrgUnitFieldType#ENROLLMENT use the program instance enrollment
 * orgUnit. OrgUnit structure columns are included in the analytics table for
 * enrollment queries. For event queries, orgUnit structure tables are joined to
 * the enrolmentou column.</li>
 * <li>{@link OrgUnitFieldType#OWNER_AT_START} use the TEI owner at the start of
 * reporting period. OrgUnit columns are coalesced from the ownership analytics
 * table when present, and from the enrollment orgUnit when not (the ou column
 * for enrollment queries, or the enrollmentou column for event queries.)</li>
 * <li>{@link OrgUnitFieldType#OWNER_AT_END} use the TEI owner at the end of
 * reporting period. OrgUnit columns are coalesced from the ownership analytics
 * table when present, and from the enrollment orgUnit when not (the ou column
 * for enrollment queries, or the enrollmentou column for event queries.)</li>
 * </ul>
 *
 * @author Jim Grace
 */
@Getter
@EqualsAndHashCode
public class OrgUnitField
{
    public static final OrgUnitField DEFAULT_ORG_UNIT_FIELD = new OrgUnitField( null );

    private final String field;

    private final OrgUnitFieldType type;

    public OrgUnitField( String field )
    {
        this.field = field;

        if ( isEmpty( field ) )
        {
            this.type = OrgUnitFieldType.DEFAULT;
        }
        else
        {
            this.type = getIfPresent( OrgUnitFieldType.class, field )
                .or( OrgUnitFieldType.ATTRIBUTE );
        }
    }

    /**
     * Returns true if we need to join the _orgunitstructure table (and, if
     * group set columns, the _organisationunitgroupsetstructure table).
     *
     * @param analyticsType EVENT or ENROLLMENT
     * @return true if orgUnit resource table joins are needed
     */
    public boolean isJoinOrgUnitTables( AnalyticsType analyticsType )
    {
        return type == ATTRIBUTE || type == REGISTRATION ||
            analyticsType == EVENT && (type == OrgUnitFieldType.ENROLLMENT || type.isOwnership());
    }

    /**
     * Gets table and column for selecting an orgUnit structure column.
     *
     * @param col the column name
     * @param analyticsType EVENT or ENROLLMENT
     * @param noColumnAlias true if column alias should not be added
     * @return the table alias and column name
     */
    public String getOrgUnitStructCol( String col, AnalyticsType analyticsType, boolean noColumnAlias )
    {
        return getTableAndColumn( getOrgUnitStructAlias( analyticsType ), col, noColumnAlias );
    }

    /**
     * Gets table and column for selecting an orgUnit group set column (with
     * column alias) or in the group by clause (without column alias).
     *
     * @param col the column name
     * @param analyticsType EVENT or ENROLLMENT
     * @param noColumnAlias true if column alias should not be added
     * @return the table alias and column name
     */
    public String getOrgUnitGroupSetCol( String col, AnalyticsType analyticsType, boolean noColumnAlias )
    {
        return getTableAndColumn( getOrgUnitGroupSetAlias( analyticsType ), col, noColumnAlias );
    }

    /**
     * Gets table and column for orgUnit level.
     *
     * @param level the organisation unit level
     * @param analyticsType EVENT or ENROLLMENT
     * @return the table alias and column name (without column alias)
     */
    public String getOrgUnitLevelCol( int level, AnalyticsType analyticsType )
    {
        return getOrgUnitStructCol( LEVEL_PREFIX + level, analyticsType, true );
    }

    /**
     * Gets table and column to join the _orgunitstructure table and, if there
     * are group set columns, the _organisationunitgroupsetstructure table.
     *
     * @param analyticsType EVENT or ENROLLMENT
     * @return the table alias and column name
     */
    public String getOrgUnitJoinCol( AnalyticsType analyticsType )
    {
        return quote( ANALYTICS_TBL_ALIAS, getOuUidColumn( analyticsType ) );
    }

    /**
     * Gets table and column for orgUnit UID in forming the WHERE clause.
     *
     * @param analyticsType EVENT or ENROLLMENT
     * @return the table alias and column name (without column alias)
     */
    public String getOrgUnitWhereCol( AnalyticsType analyticsType )
    {
        return getTableAndColumn( ANALYTICS_TBL_ALIAS, getOuUidColumn( analyticsType ), true );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the table alias for the organisation unit structure columns.
     */
    private String getOrgUnitStructAlias( AnalyticsType analyticsType )
    {
        return (isJoinOrgUnitTables( analyticsType ))
            ? ORG_UNIT_STRUCT_ALIAS
            : ANALYTICS_TBL_ALIAS;
    }

    /**
     * Gets the table alias for the organisation unit group set columns.
     */
    private String getOrgUnitGroupSetAlias( AnalyticsType analyticsType )
    {
        return (isJoinOrgUnitTables( analyticsType ))
            ? ORG_UNIT_GROUPSET_STRUCT_ALIAS
            : ANALYTICS_TBL_ALIAS;
    }

    /**
     * Gets the column holding the orgUnit UID (or the fallback enrollment UID
     * in case of ownership).
     */
    private String getOuUidColumn( AnalyticsType analyticsType )
    {
        return (analyticsType == EVENT)
            ? firstNonNull( type.getEventColumn(), field )
            : firstNonNull( type.getEnrollmentColumn(), field );
    }

    /**
     * Gets an organisation unit info (structure or group set) table and column.
     * In case of ownership, returns a coalescing between the ownership table
     * and the fallback enrollment orgUnit column.
     */
    private String getTableAndColumn( String tableAlias, String col, boolean noColumnAlias )
    {
        if ( type.isOwnership() )
        {
            return "coalesce("
                + quote( OWNERSHIP_TBL_ALIAS, col ) + ","
                + ouQuote( tableAlias, col, true ) + ")"
                + ((noColumnAlias) ? "" : " as " + col);
        }

        return ouQuote( tableAlias, col, noColumnAlias );
    }

    /**
     * Quotes the table alias and column. However, if the table alias is for the
     * _orgunitstructure table and the column is "ou", change the column to
     * "organisationunituid" because that is how it is called in that table. Add
     * "ou" as a column alias if requested.
     */
    private String ouQuote( String tableAlias, String col, boolean noColumnAlias )
    {
        if ( ORG_UNIT_STRUCT_ALIAS.equals( tableAlias ) && DEFAULT_ORG_UNIT_COL.equals( col ) )
        {
            return quote( tableAlias, "organisationunituid" )
                + ((noColumnAlias) ? "" : " as " + col);
        }

        return quote( tableAlias, col );
    }
}
