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
package org.hisp.dhis.sqlview;

import com.google.common.base.MoreObjects;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;

/**
 * @author Kristian Wærstad
 */
public class SqlViewQuery {
  public static final SqlViewQuery EMPTY = new SqlViewQuery();

  private Set<String> criteria;

  private Set<String> var;

  private Boolean skipPaging;

  private Boolean paging;

  private int page = 1;

  private int pageSize = Pager.DEFAULT_PAGE_SIZE;

  private int total;

  public Set<String> getCriteria() {
    return criteria;
  }

  public void setCriteria(Set<String> criteria) {
    this.criteria = criteria;
  }

  public Set<String> getVar() {
    return var;
  }

  public void setVar(Set<String> var) {
    this.var = var;
  }

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

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public Pager getPager() {
    return PagerUtils.isSkipPaging(skipPaging, paging) ? null : new Pager(page, total, pageSize);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("page", page)
        .add("pageSize", pageSize)
        .add("total", total)
        .toString();
  }
}
