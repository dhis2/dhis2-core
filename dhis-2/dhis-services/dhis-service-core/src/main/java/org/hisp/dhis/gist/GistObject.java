package org.hisp.dhis.gist;

import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.object.ObjectOutput.Property;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record GistObject(
    @Nonnull List<Property> properties,
    @CheckForNull Object[] values
) {

  public GistObject {
    requireNonNull(properties);
  }

  public record Input(
      @Nonnull Class<? extends PrimaryKeyObject> objectType,
      @Nonnull UID id,
      @Nonnull GistObjectParams params
  ) {

    public Input {
      requireNonNull(objectType);
      requireNonNull(id);
      requireNonNull(params);
    }
  }

  public record Output(
      @Nonnull List<Property> properties,
      @Nonnull Object[] values
  ) {

    public Output {
      requireNonNull(properties);
      requireNonNull(values);
    }
    public List<String> paths() {
      return properties.stream().map(Property::path).toList();
    }
  }
}
