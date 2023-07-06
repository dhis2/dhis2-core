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
package org.hisp.dhis.validation.comparator;

import com.google.common.base.MoreObjects;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;

/**
 * @author Stian Sandvold
 */
public class ValidationResultQuery {
  public static final ValidationResultQuery EMPTY = new ValidationResultQuery();

  private Boolean skipPaging;

  private Boolean paging;

  private int page = 1;

  private int pageSize = Pager.DEFAULT_PAGE_SIZE;

  private long total;

  /**
   * Optional list of validation rule uids to filter. If empty the list is not restricting the
   * query.
   */
  private List<String> vr;

  /**
   * Optional list of organisation unit uids to filter. If empty the list is not restricting the
   * query.
   */
  private List<String> ou;

  /**
   * Optional list of ISO-Date expressions to filter. If empty the list is not restricting the
   * query.
   */
  private List<String> pe;

  /** Optional filter to select only results that have been created on or after the given date. */
  private Date createdDate;

  public ValidationResultQuery() {}

  public boolean isSkipPaging() {
    return PagerUtils.isSkipPaging(skipPaging, paging);
  }

  public void setSkipPaging(Boolean skipPaging) {
    this.skipPaging = skipPaging;
  }

  public boolean isPaging() {
    return BooleanUtils.toBoolean(paging);
  }

  public void setPaging(Boolean paging) {
    this.paging = paging;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public Pager getPager() {
    return PagerUtils.isSkipPaging(skipPaging, paging) ? null : new Pager(page, total, pageSize);
  }

  public List<String> getVr() {
    return vr;
  }

  public void setVr(List<String> vr) {
    this.vr = vr;
  }

  public List<String> getOu() {
    return ou;
  }

  public void setOu(List<String> ou) {
    this.ou = ou;
  }

  public List<String> getPe() {
    return pe;
  }

  public void setPe(List<String> pe) {
    this.pe = pe;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("page", page)
        .add("pageSize", pageSize)
        .add("total", total)
        .add("ou", ou)
        .add("vr", vr)
        .add("pe", pe)
        .toString();
  }
}
