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
package org.hisp.dhis.predictor;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;

/**
 * @author Ken Haase
 */
public interface PredictorStore extends IdentifiableObjectStore<Predictor> {
  String ID = PredictorStore.class.getName();

  List<Predictor> getAllByDataElement(Collection<DataElement> dataElements);

  List<Predictor> getAllWithGeneratorContainingDataElement(@Nonnull List<String> dataElementUids);

  List<Predictor> getAllWithSampleSkipTestContainingDataElement(
      @Nonnull List<String> dataElementUids);

  /**
   * Retrieve all {@link Predictor}s with references to {@link CategoryOptionCombo} {@link UID}s
   *
   * @param uids {@link CategoryOptionCombo} {@link UID}s
   * @return {@link Predictor}s with references to {@link CategoryOptionCombo} {@link UID} passed in
   */
  List<Predictor> getByCategoryOptionCombo(@Nonnull Collection<UID> uids);
}
