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
package org.hisp.dhis.minmax;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Kristian Nordal
 */
public interface MinMaxDataElementStore extends GenericStore<MinMaxDataElement> {
  String ID = MinMaxDataElementStore.class.getName();

  MinMaxDataElement get(
      OrganisationUnit source, DataElement dataElement, CategoryOptionCombo optionCombo);

  List<MinMaxDataElement> get(OrganisationUnit source, Collection<DataElement> dataElements);

  List<MinMaxDataElement> query(MinMaxDataElementQueryParams query);

  int countMinMaxDataElements(MinMaxDataElementQueryParams query);

  void delete(OrganisationUnit organisationUnit);

  void delete(DataElement dataElement);

  void delete(CategoryOptionCombo optionCombo);

  void delete(Collection<DataElement> dataElements, OrganisationUnit parent);

  List<MinMaxDataElement> getByDataElement(Collection<DataElement> dataElements);

  /**
   * Retrieve all {@link MinMaxDataElement}s with references to {@link CategoryOptionCombo} {@link
   * UID}s
   *
   * @param uids {@link CategoryOptionCombo} {@link UID}s
   * @return {@link MinMaxDataElement}s with references to {@link CategoryOptionCombo} {@link UID}
   *     passed in
   */
  List<MinMaxDataElement> getByCategoryOptionCombo(@Nonnull Collection<UID> uids);

  @SuppressWarnings("unchecked")
  List<String> getDataElementsByDataSet(UID dataSet);

  int deleteByKeys(List<MinMaxValueKey> keys);

  int upsertValues(List<MinMaxValue> values);
}
