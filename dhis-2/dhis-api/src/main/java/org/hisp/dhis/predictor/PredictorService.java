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
package org.hisp.dhis.predictor;

import java.util.List;

/** Created by haase on 6/12/16. */
public interface PredictorService {
  String ID = PredictorService.class.getName();

  // -------------------------------------------------------------------------
  // Predictor
  // -------------------------------------------------------------------------

  /**
   * Add a predictor to the database.
   *
   * @param predictor the Predictor to add.
   * @return the generated unique identifier for the predictor.
   */
  long addPredictor(Predictor predictor);

  /**
   * Update a predictor in the database.
   *
   * @param predictor the predictor to update.
   */
  void updatePredictor(Predictor predictor);

  /**
   * Delete a predictor from the database.
   *
   * @param predictor the predictor to delete.
   */
  void deletePredictor(Predictor predictor);

  /**
   * Get predictor with the given identifier.
   *
   * @param id the unique identifier of the predictor.
   * @return the predictor or null if it doesn't exist.
   */
  Predictor getPredictor(long id);

  /**
   * Get predictor with the given uid.
   *
   * @param uid the unique identifier of the predictor.
   * @return the predictor or null if it doesn't exist.
   */
  Predictor getPredictor(String uid);

  /**
   * Get all predictors.
   *
   * @return a List of predictors or null if there are no predictors.
   */
  List<Predictor> getAllPredictors();

  // -------------------------------------------------------------------------
  // Predictor Group
  // -------------------------------------------------------------------------

  /**
   * Adds a predictor group to the database.
   *
   * @param predictorGroup the predictor group to add.
   * @return the generated unique identifier for the predictor group.
   */
  long addPredictorGroup(PredictorGroup predictorGroup);

  /**
   * Delete a predictor group from the database.
   *
   * @param predictorGroup the predictor group to delete.
   */
  void deletePredictorGroup(PredictorGroup predictorGroup);

  /**
   * Update a predictor group with the given identifiers.
   *
   * @param predictorGroup the predictor group to update.
   */
  void updatePredictorGroup(PredictorGroup predictorGroup);

  /**
   * Get predictor group with the given identifier.
   *
   * @param id the unique identifier of the predictor group.
   * @return the predictor group or null if it doesn't exist.
   */
  PredictorGroup getPredictorGroup(long id);

  /**
   * Get predictor group with the given uid.
   *
   * @param uid the unique identifier of the predictor group.
   * @return the predictor group or null if it doesn't exist.
   */
  PredictorGroup getPredictorGroup(String uid);

  /**
   * Get all predictor groups.
   *
   * @return a List of predictor groups or null if it there are no predictor groups.
   */
  List<PredictorGroup> getAllPredictorGroups();
}
