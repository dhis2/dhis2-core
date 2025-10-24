package org.hisp.dhis.common;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.stream.Stream;

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
   * Fetches a mapping from the given UIDs for the given {@link ObjectType} to their identifier of
   * the given {@link IdScheme}
   *
   * <p>For example, when {@code idsProperty} is CODE, the result looks like:
   *
   * <pre>
   * {uid1} => {code1}
   * {uid2} => {code2}
   * </pre>
   *
   * @param type what object (table)
   * @param to unknown identifier and requested (result map value)
   * @param ids known identifiers (result map key)
   * @return a mapping from the known to the unknown identifier
   */
  @Nonnull
  Map<String, String> mapEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> ids);

  /**
   * Map UIDs to other identifiers.
   * <p>
   * If a mapping (order) is needed use {@link #mapEncodedIds(ObjectType, IdProperty, Stream)}.
   *
   * @param type what object (table)
   * @param to unknown identifier and requested (result map value)
   * @param ids know UID identifiers
   * @return a stream of existing identifier that correspond to the given UIDs in no particular
   *     order (with all UIDs not being represented for which there is no mapping)
   */
  @Nonnull
  Stream<String> listEncodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty to, @Nonnull Stream<UID> ids);

  @CheckForNull
  default String getEncodedId(@Nonnull ObjectType type, @Nonnull IdProperty to, @CheckForNull UID id) {
    if (id == null) return null;
    if (to == IdProperty.UID) return id.getValue();
    return listEncodedIds(type, to, Stream.of(id)).findFirst().orElse(null);
  }

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
   * <p>
   * If a mapping (order) is needed use {@link #mapDecodedIds(ObjectType, IdProperty, Stream)}.
   *
   * @param type the target object type or table
   * @param from the known ID property (values given by ids)
   * @param ids the ids to map to UIDs
   * @return a stream of existing UIDs that correspond to the given ids in no particular
   *         order (with all input IDs not being represented for which there is no mapping)
   */
  @Nonnull
  Stream<String> listDecodedIds(
      @Nonnull ObjectType type, @Nonnull IdProperty from, @Nonnull Stream<String> ids);

}
