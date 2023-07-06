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
package org.hisp.dhis.dataanalysis;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.query.JpaQueryUtils.generateHqlQueryForSharingCheck;

import java.util.List;
import lombok.AllArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Performs the database operations of the "new" followup analysis which returns {@link
 * FollowupValue}s.
 *
 * @author Jan Bernitt
 */
@Component
@AllArgsConstructor
public class FollowupValueManager {

  private final SessionFactory sessionFactory;

  /** HQL query used to select {@link FollowupValue}s. */
  private static final String FOLLOWUP_VALUE_HQL =
      "select new org.hisp.dhis.dataanalysis.FollowupValue("
          + "de.uid, de.name, "
          + "pt.class, "
          + "pe.startDate, pe.endDate, cast(null as java.lang.String),"
          + "ou.uid, ou.name, ou.path, "
          + "coc.uid, coc.name, "
          + "aoc.uid, aoc.name, "
          + "dv.value, dv.storedBy, dv.lastUpdated, dv.created, dv.comment, "
          + "mm.min, mm.max "
          + ")"
          + "from DataValue dv "
          + "left join MinMaxDataElement mm on (dv.dataElement.id = mm.dataElement.id and dv.categoryOptionCombo.id = mm.optionCombo.id and dv.source.id = mm.source.id) "
          + "inner join DataElement de on dv.dataElement.id = de.id "
          + "inner join Period pe on dv.period.id = pe.id "
          + "inner join PeriodType pt on pe.periodType.id = pt.id "
          + "inner join OrganisationUnit ou on dv.source.id = ou.id "
          + "inner join CategoryOptionCombo coc on dv.categoryOptionCombo.id = coc.id "
          + "inner join CategoryOptionCombo aoc on dv.attributeOptionCombo.id = aoc.id "
          + "where de.uid in (:de_ids) and (<<sharing>>)"
          + "and dv.followup = true and dv.deleted is false "
          + "and coc.uid in (:coc_ids) "
          + "and pe.startDate >= :startDate and pe.endDate <= :endDate "
          + "and exists (select 1 from OrganisationUnit parent where parent.uid in (:ou_ids) and ou.path like concat(parent.path, '%'))";

  /**
   * HQL query potentially used to resolve all {@link org.hisp.dhis.dataelement.DataElement} UIDS
   * from {@link org.hisp.dhis.dataset.DataSet} UIDs in context of {@link #FOLLOWUP_VALUE_HQL}
   * query.
   */
  private static final String DATA_ELEMENT_UIDS_BY_DATA_SET_UIDS_HQL =
      "select dse.dataElement.uid "
          + "from DataSetElement dse "
          + "where dse.dataSet.uid in (:ds_ids)"
          + "and dse.dataElement.valueType in ('INTEGER', 'INTEGER_POSITIVE', 'INTEGER_NEGATIVE', 'INTEGER_ZERO_OR_POSITIVE', 'NUMBER', 'UNIT_INTERVAL', 'PERCENTAGE')";

  private static final String CATEGORY_OPTION_COMBO_UIDS_BY_DATE_ELEMENT_UIDS_HQL =
      "select coc.uid "
          + "from CategoryOptionCombo coc "
          + "where coc.categoryCombo.id in (select de.categoryCombo.id from DataElement de where de.uid in (:de_ids))";

  /**
   * Returns {@link FollowupValue}s which were marked for {@link DataValue#isFollowup()} and which
   * match the filter parameters.
   *
   * <p>Note that as a side effect the passed {@link FollowupAnalysisRequest} is updated with the
   * {@link org.hisp.dhis.dataelement.DataElement}s and {@link
   * org.hisp.dhis.category.CategoryOptionCombo}s and start and end date actually used should those
   * not be set.
   *
   * @param currentUser the current user (for sharing access)
   * @param request filter parameters
   * @return a list if {@link FollowupValue}s.
   */
  public List<FollowupValue> getFollowupDataValues(
      User currentUser, FollowupAnalysisRequest request) {
    if (isEmpty(request.getDe()) && !isEmpty(request.getDs())) {
      request.setDe(
          sessionFactory
              .getCurrentSession()
              .createQuery(DATA_ELEMENT_UIDS_BY_DATA_SET_UIDS_HQL, String.class)
              .setParameter("ds_ids", request.getDs())
              .list());
    }
    if (!isEmpty(request.getDe()) && isEmpty(request.getCoc())) {
      request.setCoc(
          sessionFactory
              .getCurrentSession()
              .createQuery(CATEGORY_OPTION_COMBO_UIDS_BY_DATE_ELEMENT_UIDS_HQL, String.class)
              .setParameter("de_ids", request.getDe())
              .list());
    }
    if (isEmpty(request.getDe()) || isEmpty(request.getCoc()) || isEmpty(request.getOu())) {
      return emptyList();
    }
    if (request.getStartDate() == null && request.getPe() != null) {
      request.setStartDate(PeriodType.getPeriodFromIsoString(request.getPe()).getStartDate());
    }
    if (request.getEndDate() == null && request.getPe() != null) {
      request.setEndDate(PeriodType.getPeriodFromIsoString(request.getPe()).getEndDate());
    }

    Query<FollowupValue> query =
        sessionFactory
            .getCurrentSession()
            .createQuery(
                FOLLOWUP_VALUE_HQL.replace(
                    "<<sharing>>",
                    generateHqlQueryForSharingCheck(
                        "de", currentUser, AclService.LIKE_READ_METADATA)),
                FollowupValue.class);

    query.setParameter("ou_ids", request.getOu());
    query.setParameter("de_ids", request.getDe());
    query.setParameter("coc_ids", request.getCoc());
    query.setParameter("startDate", request.getStartDate());
    query.setParameter("endDate", request.getEndDate());
    query.setMaxResults(request.getMaxResults());
    return query.setCacheable(false).list();
  }
}
