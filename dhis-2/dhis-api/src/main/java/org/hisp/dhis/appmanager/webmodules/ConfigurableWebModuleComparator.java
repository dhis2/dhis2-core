/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.appmanager.webmodules;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator for sorting modules according to a specified order. Modules not listed in the given
 * order are sorted alphabetically after the specified ones.
 *
 * @author Torgeir Lorange Ostby
 */
public class ConfigurableWebModuleComparator implements Comparator<WebModule> {
  private final List<String> order;

  public ConfigurableWebModuleComparator(List<String> order) {
    this.order = order;
  }

  @Override
  public int compare(WebModule moduleA, WebModule moduleB) {
    int indexA = order.indexOf(moduleA.getName());
    int indexB = order.indexOf(moduleB.getName());

    // ---------------------------------------------------------------------
    // If indexA and indexB have different signs, make the positive one come
    // first {if A is 0/+ and B is - return - (A before B), if A is - and B
    // is 0/+ return + (B before A)}. If both are -, compare the names. If
    // both are 0/+, compare the indices.
    // ---------------------------------------------------------------------

    if ((indexA < 0) ^ (indexB < 0)) {
      return indexB * 2 + 1;
    }

    if (indexA < 0) {
      return moduleA.getName().compareTo(moduleB.getName());
    }

    return indexA - indexB;
  }
}
