package org.hisp.dhis.gist;

import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public record GistObjectProperty() {

  public record Input(
      @Nonnull Class<? extends PrimaryKeyObject> objectType,
      @Nonnull UID id,
      @Nonnull String property,
      @Nonnull GistObjectPropertyParams params) {

    public Input {
      requireNonNull(objectType);
      requireNonNull(id);
      requireNonNull(property);
      requireNonNull(params);
    }
  }

  public record Output() {}
}
