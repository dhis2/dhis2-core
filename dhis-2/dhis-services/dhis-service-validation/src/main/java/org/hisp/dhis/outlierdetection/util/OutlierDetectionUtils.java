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
package org.hisp.dhis.outlierdetection.util;

import static org.hisp.dhis.feedback.ErrorCode.E2208;
import static org.hisp.dhis.feedback.ErrorCode.E7131;
import static org.hisp.dhis.util.SqlExceptionUtils.ERR_MSG_SILENT_FALLBACK;
import static org.hisp.dhis.util.SqlExceptionUtils.ERR_MSG_TABLE_NOT_EXISTING;
import static org.hisp.dhis.util.SqlExceptionUtils.relationDoesNotExist;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class OutlierDetectionUtils {

  /**
   * Returns an organisation unit 'path' "like" clause for the given list of {@link
   * OrganisationUnit}.
   *
   * @param orgUnits the list of {@link OrganisationUnit}.
   * @return an organisation unit 'path' "like" clause.
   */
  public static String getOrgUnitPathClause(List<OrganisationUnit> orgUnits, String pathAlias) {
    StringBuilder sql = new StringBuilder("(");
    orgUnits.forEach(
        ou ->
            sql.append(pathAlias).append(".\"path\" like '").append(ou.getPath()).append("%' or "));

    return StringUtils.trim(TextUtils.removeLastOr(sql.toString())) + ")";
  }

  /**
   * Wraps the provided interface around a common exception handling strategy.
   *
   * @param supplier the {@link Supplier} containing the code block to execute and wrap around the
   *     exception handling.
   * @return the {@link Optional} wrapping th result of the supplier execution.
   */
  public static <T> Optional<T> withExceptionHandling(Supplier<T> supplier) {
    try {
      return Optional.ofNullable(supplier.get());
    } catch (BadSqlGrammarException ex) {
      if (relationDoesNotExist(ex.getSQLException())) {
        log.info(ERR_MSG_TABLE_NOT_EXISTING, ex);
        throw ex;
      } else {
        log.info(ERR_MSG_SILENT_FALLBACK, ex);
        throw ex;
      }
    } catch (QueryRuntimeException ex) {
      log.error("Internal runtime exception", ex);
      throw ex;
    } catch (DataIntegrityViolationException ex) {
      log.error(E2208.getMessage(), ex);
      throw new IllegalQueryException(E2208);
    } catch (DataAccessResourceFailureException ex) {
      log.error(E7131.getMessage(), ex);
      throw new QueryRuntimeException(E7131);
    }
  }
}
