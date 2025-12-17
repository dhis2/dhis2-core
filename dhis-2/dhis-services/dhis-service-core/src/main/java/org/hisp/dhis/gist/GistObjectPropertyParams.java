package org.hisp.dhis.gist;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hisp.dhis.common.OpenApi;

@Data
@OpenApi.Shared
@OpenApi.Property // all fields are public properties
@EqualsAndHashCode(callSuper = true)
public class GistObjectPropertyParams extends GistObjectParams {
  @OpenApi.Description(
      """
      Inverse can be used in context of a collection field gist of the form `/api/<object-type>/<object-id>/<field-name>/gist`
      to not list all items that are contained in the member collection but all items that are not contained in the member collection.
      See [Gist inverse parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-inverse-parameter).""")
  boolean inverse = false;
}
