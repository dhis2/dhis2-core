package org.hisp.dhis.outlierdetection.processor;

import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public interface IOutlierSqlStatementProcessor {

    /**
     * retrieve parametrised sql statement for outliers
     * @param request the instance of {@link OutlierDetectionRequest}.
     * @return sql statement as a string
     */
    String getSqlStatement(OutlierDetectionRequest request);

    /**
     * retrieve psql parameters  for outliers sql statement
     * @param request the instance of {@link OutlierDetectionRequest}.
     * @return teh instance of {@link SqlParameterSource}.
     */
    SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request);
}
