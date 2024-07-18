package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class AppOverrideFilterTest {

  @Test
  @DisplayName("Content size for File resource returns positive int")
  void fileResourceContentLengthTest() {
    // given
    AppOverrideFilter filter = new AppOverrideFilter(null, null);
    Resource resource =
        new FileSystemResource("./src/test/resources/filter/test-file-content-length.txt");

    // when
    int uriContentLength = filter.getUriContentLength(resource);

    // then
    assertEquals(38, uriContentLength);
  }
}
