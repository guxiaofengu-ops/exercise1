package nz.ac.massey.editor.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {
    private final SearchService service = new SearchService();

    @Test
    void findsSingleWordCaseInsensitively() {
        List<SearchService.Match> matches = service.findAll("Alpha beta ALPHA", "alpha", false);

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).start());
        assertEquals(11, matches.get(1).start());
    }

    @Test
    void returnsEmptyListForMissingTerm() {
        assertTrue(service.findAll("hello world", "missing", false).isEmpty());
    }

    @Test
    void supportsCaseSensitiveSearch() {
        List<SearchService.Match> matches = service.findAll("Alpha alpha ALPHA", "Alpha", true);

        assertEquals(1, matches.size());
        assertEquals(0, matches.get(0).start());
    }

    @Test
    void returnsEmptyListForNullOrEmptyInputs() {
        assertTrue(service.findAll(null, "word", false).isEmpty());
        assertTrue(service.findAll("text", null, false).isEmpty());
        assertTrue(service.findAll("text", "", false).isEmpty());
    }
}
