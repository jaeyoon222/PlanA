package com.study.StudyCafe.constant;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum SeatTag {
    WINDOW(Set.of("창가","창측","창문옆","window")),
    OUTLET(Set.of("콘센트","플러그","전원","outlet","power")),
    QUIET(Set.of("조용","조용한","quiet","무음")),
    DOOR(Set.of("문가","입구쪽","door")),
    AISLE(Set.of("복도","통로","aisle"));

    public final Set<String> synonyms;
    SeatTag(Set<String> synonyms){ this.synonyms = synonyms; }

    public static Optional<SeatTag> fromText(String token) {
        String t = token.toLowerCase(Locale.KOREAN);
        for (SeatTag tag : values()) {
            if (tag.synonyms.stream().anyMatch(s -> t.contains(s.toLowerCase(Locale.KOREAN)))) {
                return Optional.of(tag);
            }
        }
        return Optional.empty();
    }
}
