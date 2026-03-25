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
package org.hisp.dhis.tracker.imports.preprocess;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;

/** Preprocesses the tracker bundle before validation. Preprocessors run in declaration order. */
public class Preprocessor {

  private static final List<Consumer<TrackerBundle>> PREPROCESSORS =
      List.of(
          EventProgramPreprocessor
              ::process, // must run before CategoryComboPreprocessor because the latter depends on
          // events having their program set.
          CategoryComboPreprocessor::process,
          EventStatusPreprocessor::process,
          AssignedUserPreprocessor::process,
          DuplicateRelationshipsPreprocessor::process);

  private Preprocessor() {
    throw new UnsupportedOperationException("utility class");
  }

  @Nonnull
  public static TrackerBundle preprocess(@Nonnull TrackerBundle bundle) {
    // StrategyPreprocessor runs for all strategies including DELETE since every object needs its
    // per-object strategy (CREATE/UPDATE/DELETE) resolved before validation and persistence.
    StrategyPreprocessor.process(bundle);
    if (!bundle.getImportStrategy().isDelete()) {
      for (Consumer<TrackerBundle> preprocessor : PREPROCESSORS) {
        preprocessor.accept(bundle);
      }
    }
    return bundle;
  }
}
