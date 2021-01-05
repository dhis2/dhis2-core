package org.hisp.dhis.outlierdetection.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.period.PeriodType;

public class OutlierDetectionUtils
{
    /**
     * Returns the ISO period name for the given {@link ResultSet} row. Requires
     * that a column <code>pe_start_date</code> of type date and a column
     * <code>pt_name</code> are present.
     *
     * @param calendar the {@link Calendar}.
     * @param rs the {@link ResultSet}.
     * @return the ISO period name.
     */
    public static String getIsoPeriod( Calendar calendar, String periodType, Date startDate )
        throws SQLException
    {
        final PeriodType pt = PeriodType.getPeriodTypeByName( periodType );
        return pt.createPeriod( startDate, calendar ).getIsoDate();
    }

    /**
     * Returns an organisation unit 'path' "like" clause for the given query.
     *
     * @param query the {@link OutlierDetectionRequest}.
     * @return an organisation unit 'path' "like" clause.
     */
    public static String getOrgUnitPathClause( OutlierDetectionRequest query )
    {
        String sql = "(";

        for ( OrganisationUnit ou : query.getOrgUnits() )
        {
            sql += "ou.\"path\" like '" + ou.getPath() + "%' or ";
        }

        return TextUtils.removeLastOr( sql ) + ")";
    }
}
