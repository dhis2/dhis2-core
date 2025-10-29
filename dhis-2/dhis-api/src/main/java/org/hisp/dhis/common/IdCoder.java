/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.common;

import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A store like API for ID encoding and decoding support.
 *
 * <p>It will automatically use stateless session transactions when necessary. A caller does not
 * need to make sure a transaction is in progress. However, this also means the results are always
 * for existing data ignoring pending changes of surrounding transactions. This is by design so that
 * this lookup will always run with predictable conditions and side effect free.
 *
 * @author Jan Bernitt
 */
public interface IdCoder {

  /** Tables for which IDs can be encoded and decoded */
  enum ObjectType {
    DS,
    DE,
    DEG,
    OU,
    OUG,
    COC
  }

  /**
   * Maps UIDs for the given {@link ObjectType} to another target identifier defined by {@link
   * IdProperty}
   *
   * <p>For example, when {@code to} is {@link IdProperty#CODE}, the result looks like:
   *
   * <pre>
   * {uid1} => {code1}
   * {uid2} => {code2}
   * </pre>
   *
   * @param type what object (table)
   * @param to the unknown, requested identifier (result map value)
   * @param ids known identifiers (result map key)
   * @return a mapping from the known to the unknown identifier (not including a mapping for entries
   *     that are either not found or where the target property is null)
   */
  @Nonnull
  Map<UID, String> mapEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> ids);

  /**
   * Resolves UIDs to other identifiers.
   *
   * <p>If a mapping (order) is needed use {@link #mapEncodedIds(ObjectType, IdProperty, Stream)}.
   *
   * @param type what object (table)
   * @param to the unknown, requested identifier (result stream value)
   * @param ids know UID identifiers
   * @return a stream of target identifiers which correspond to the given UIDs, in no particular
   *     order (not including a value for UIDs that are either not found or where the target
   *     property is null)
   */
  @Nonnull
  Stream<String> listEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> ids);

  /**
   * Resolve a single UID to another identifier.
   *
   * @param type what object (table)
   * @param to the unknown, requested identifier (result value type)
   * @param id the known identifier to resolve
   * @return the identifier corresponding to the given UID, or null if no mapping is found or the
   *     value for the target property is null
   */
  @CheckForNull
  default String getEncodedId(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @CheckForNull UID id) {
    if (id == null) return null;
    if (to == IdProperty.UID) return id.getValue();
    return listEncodedIds(type, to, Stream.of(id)).findFirst().orElse(null);
  }

  /**
   * Maps a set of COC UIDs to pairs of category and option they combine.
   *
   * <p>For example, when choosing codes for category and names for options the result map looks
   * like:
   *
   * <pre>
   * COC1_UID => { C1_CODE => CO1_NAME, C2_CODE => CO2_NAME },
   * COC2_UID => { C3_CODE => CO3_NAME, C4_CODE => CO4_NAME },
   * </pre>
   *
   * @param toCategory target ID property for the category IDs
   * @param toOption target ID property for the category option IDs
   * @param optionCombos the COC UIDs to map
   * @return for each COC UID a mapping between the categories (key) and the options (values) the
   *     particular COC represents (not including any mapping for a given COC UID in case no entries
   *     exist or any of the categories or options target property is null - in other words: only
   *     complete, non-null mappings are contained)
   */
  @Nonnull
  Map<UID, Map<String, String>> mapEncodedOptionCombosAsCategoryAndOption(
      @Nonnull IdProperty toCategory,
      @Nonnull IdProperty toOption,
      @Nonnull Stream<UID> optionCombos);

  /**
   * Fetches a mapping between the provided IDs (that used the provided {@code idsProperty}) and the
   * UID of the same object.
   *
   * <p>For example, when {@code idsProperty} is CODE, the result looks like:
   *
   * <pre>
   * {code1} => {uid1}
   * {code2} => {uid2}
   * </pre>
   *
   * @param type the target object type or table
   * @param from the known ID property (values given by ids)
   * @param ids the IDs to map from property to UID
   * @return a map from given ID to the corresponding UID (does not include entries for input IDs
   *     that do not exist and thus do not have a corresponding UID)
   */
  Map<String, String> mapDecodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> ids);

  /**
   * Map other identifiers to UIDs.
   *
   * <p>If a mapping (order) is needed use {@link #mapDecodedIds(ObjectType, IdProperty, Stream)}.
   *
   * @param type the target object type or table
   * @param from the known ID property (values given by ids)
   * @param ids the ids to map to UIDs
   * @return a stream of existing UIDs that correspond to the given ids in no particular order (with
   *     all input IDs not being represented for which there is no mapping)
   */
  @Nonnull
  Stream<String> listDecodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> ids);
}
