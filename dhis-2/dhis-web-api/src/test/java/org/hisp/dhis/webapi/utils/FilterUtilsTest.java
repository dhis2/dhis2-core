package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterUtilsTest {

    @Test
    public void testFromFilterWithNull() {
        // Test with null input
        List<String> result = FilterUtils.fromFilter(null);
        assertTrue(result.isEmpty(), "Result should be empty for null input");
    }

    @Test
    public void testFromFilterWithEmptyString() {
        // Test with empty string
        List<String> result = FilterUtils.fromFilter("");
        assertTrue(result.isEmpty(), "Result should be empty for empty string input");
    }

    @Test
    public void testFromFilterWithNoOperation() {
        // Test with input that doesn't contain a colon (no operation)
        String filter = "abc123";
        List<String> result = FilterUtils.fromFilter(filter);

        assertEquals(1, result.size(), "Should return a list with one element");
        assertEquals(filter, result.get(0), "The element should be the input string");
    }

    @Test
    public void testFromFilterWithInOperation() {
        // Test with IN operation and multiple identifiers
        String filter = "IN:id1;id2;id3";
        List<String> result = FilterUtils.fromFilter(filter);

        List<String> expected = Arrays.asList("id1", "id2", "id3");
        assertEquals(expected, result, "Should return a list with all identifiers");
    }

    @Test
    public void testFromFilterWithEqOperation() {
        // Test with EQ operation or any other operation besides IN
        String filter = "EQ:id1";
        List<String> result = FilterUtils.fromFilter(filter);

        assertEquals(1, result.size(), "Should return a list with one element");
        assertEquals("id1", result.get(0), "The element should be the identifier");
    }


    @Test
    public void testFromFilterWithEmptyIdentifiers() {
        // Test with IN operation but empty identifiers
        String filter = "IN:";
        List<String> result = FilterUtils.fromFilter(filter);

        assertEquals(1, result.size(), "Should return a list with one element");
        assertEquals("", result.get(0), "The element should be an empty string");
    }

    @Test
    public void testFromFilterWithInOperationAndEmptyElements() {
        // Test with IN operation and empty elements in the list
        String filter = "IN:id1;;id3";
        List<String> result = FilterUtils.fromFilter(filter);

        List<String> expected = Arrays.asList("id1", "", "id3");
        assertEquals(expected, result, "Should return a list including empty elements");
    }

    @Test
    public void testFromFilterWithMultipleColons() {
        // Test with multiple colons in the filter
        String filter = "EQ:prefix:value";
        List<String> result = FilterUtils.fromFilter(filter);

        assertEquals(1, result.size(), "Should return a list with one element");
        assertEquals("prefix:value", result.get(0), "The element should include everything after the first colon");
    }

    @Test
    public void testFromFilterWithColonOnly() {
        // Test with just a colon
        String filter = ":";
        List<String> result = FilterUtils.fromFilter(filter);

        assertEquals(1, result.size(), "Should return a list with one element");
        assertEquals("", result.get(0), "The element should be an empty string");
    }

}