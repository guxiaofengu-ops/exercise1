package nz.ac.massey.editor.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchService {
    public record Match(int start, int length) {
    }

    public List<Match> findAll(String text, String query, boolean caseSensitive) {
        if (text == null || query == null || query.isEmpty()) {
            return List.of();
        }

        String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String needle = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        List<Match> matches = new ArrayList<>();

        int index = 0;
        int match = haystack.indexOf(needle, index);
        while (match >= 0) {
            index = match;
            matches.add(new Match(index, query.length()));
            index += Math.max(1, query.length());
            match = haystack.indexOf(needle, index);
        }
        return List.copyOf(matches);
    }
}
