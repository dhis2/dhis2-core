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

import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.Period;

/**
 * Request query parameters for followup analysis.
 *
 * <p>Mandatory are:
 *
 * <ul>
 *   <li>at least one {@link #ou}
 *   <li>either {@link #de} or {@link #ds}
 *   <li>either {@link #pe} or {@link #startDate} and {@link #endDate}
 * </ul>
 *
 * Optional are:
 *
 * <ul>
 *   <li>{@link #coc} (derived from {@link #de} if empty)
 *   <li>{@link #maxResults} (default 50)
 * </ul>
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FollowupAnalysisRequest {
  /**
   * {@link org.hisp.dhis.dataelement.DataElement} UIDs
   *
   * <p>Only considered when {@link #ds} is non-empty. Otherwise the elements considered is based on
   * {@link DataSet#getDataElements()}.
   */
  private List<String> de;

  /**
   * {@link org.hisp.dhis.dataset.DataSet} UIDs
   *
   * <p>If specified it as the same effect as if {@link #de} had been filled with the union of all
   * {@link DataSet#getDataElements()}.
   */
  private List<String> ds;

  /**
   * {@link org.hisp.dhis.category.CategoryOptionCombo} UIDs
   *
   * <p>If not specified (empty) it has the same effect as if it holds the union of all {@link
   * DataElement#getCategoryOptionCombos()} that are {@link ValueType#isNumeric()}.
   */
  private List<String> coc;

  /**
   * {@link org.hisp.dhis.organisationunit.OrganisationUnit} UIDs of the parent(s) whose children
   * should be included
   */
  private List<String> ou;

  /**
   * As an alternative to providing {@link #startDate} and {@link #endDate} a {@link Period} ISO
   * string can be provided which is just used to derive the start and end date from it.
   */
  private String pe;

  private Date startDate;

  private Date endDate;

  @Builder.Default private int maxResults = 50;
}
