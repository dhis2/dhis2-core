/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datadimensionitem;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;

/**
 * @author david mackessy
 */
public interface DataDimensionItemStore extends GenericStore<DataDimensionItem> {

  /**
   * Gets all {@link DataDimensionItem}s that reference any of the supplied {@link Indicator}s
   *
   * @param indicators to search for
   * @return matching {@link DataDimensionItem}s
   */
  List<DataDimensionItem> getIndicatorDataDimensionItems(List<Indicator> indicators);

  List<DataDimensionItem> getDataElementDataDimensionItems(List<DataElement> dataElements);

  /**
   * Update the entities with refs to the category option combo ids passed in, with the new category
   * option combo id passed in.
   *
   * @param cocIds category option combo ids to be used to update linked data dimension items
   * @param newCocId new category option combo id to use
   * @return number of entities updated
   */
  int updateDeoCategoryOptionCombo(Collection<Long> cocIds, long newCocId);
}
