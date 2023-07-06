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
package org.hisp.dhis.webapi.webdomain;

import java.util.Map;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.Options;
import org.hisp.dhis.query.Junction;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class WebOptions extends Options {
  public static final String PAGING = "paging";

  public static final String PAGE = "page";

  public static final String PAGE_SIZE = "pageSize";

  public static final String ROOT_JUNCTION = "rootJunction";

  public static final String VIEW_CLASS = "viewClass";

  public static final String MANAGE = "manage";

  public WebOptions(Map<String, String> options) {
    super(options);
  }

  // --------------------------------------------------------------------------
  // Getters for standard web options
  // --------------------------------------------------------------------------

  public boolean hasPaging() {
    return stringAsBoolean(options.get(PAGING), true);
  }

  /**
   * This method will return a boolean flag depending on the current paging value and the given
   * default return value. The input param will be used to force the return of this method in the
   * cases where the PAGING is not set.
   *
   * @param defaultReturnValue is the value to be returned if the paging is not set.
   * @return the boolean flag.
   */
  public boolean hasPaging(boolean defaultReturnValue) {
    return stringAsBoolean(options.get(PAGING), defaultReturnValue);
  }

  public int getPage() {
    return stringAsInt(options.get(PAGE), 1);
  }

  public String getViewClass() {
    return stringAsString(options.get(VIEW_CLASS), null);
  }

  public String getViewClass(String defaultValue) {
    return stringAsString(options.get(VIEW_CLASS), defaultValue);
  }

  public int getPageSize() {
    return stringAsInt(options.get(PAGE_SIZE), Pager.DEFAULT_PAGE_SIZE);
  }

  public boolean isManage() {
    return stringAsBoolean(options.get(MANAGE), false);
  }

  public Junction.Type getRootJunction() {
    String rootJunction = options.get(ROOT_JUNCTION);
    return "OR".equals(rootJunction) ? Junction.Type.OR : Junction.Type.AND;
  }
}
