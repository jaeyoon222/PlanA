package com.study.StudyCafe.service;

import com.study.StudyCafe.dto.NLP.ParsedRequest;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.regex.*;

@Component
public class KoreanReservationParser {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Pattern TIME_RANGE = Pattern.compile(
            "(오전|오후|am|pm)?\\s*(\\d{1,2})(시|\\s*시)?(?:\\s*(\\d{1,2})\\s*분)?\\s*(부터|~|-|—)?\\s*(오전|오후|am|pm)?\\s*(\\d{1,2})?(시|\\s*시)?(?:\\s*(\\d{1,2})\\s*분)?"
    );

    private static final Pattern PARTY = Pattern.compile("(\\d+)\\s*(명|인|자리|인석|인용)");
    private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*(시간|hr|hour|시간동안)");

    private static final String[] DOW = {"", "월", "화", "수", "목", "금", "토", "일"};


    public ParsedRequest parse(String rawText) {
        String text = normalizeKoreanNumbers(rawText.replaceAll("\\s+", " ").trim());
        LocalDate date = inferDate(text);
        LocalTime start = null, end = null;

        var m = TIME_RANGE.matcher(text);
        if (start == null && text.contains("지금")) {
            LocalDateTime now = LocalDateTime.now(KST).withSecond(0).withNano(0);

            int min = now.getMinute();
            int roundedMin = (min < 15) ? 0 : (min < 45 ? 30 : 0);
            if (min >= 45) now = now.plusHours(1);
            now = now.withMinute(roundedMin);

            start = now.toLocalTime();
            if (date == null) date = now.toLocalDate();
        }
        if (m.find()) {
            start = toTime(m.group(1), m.group(2), m.group(4));
            String endMd = (m.group(6) != null) ? m.group(6) : m.group(1);
            if (m.group(7) != null) {
                end = toTime(endMd, m.group(7), m.group(9));
            }

            if (start != null && end != null && !end.isAfter(start)) {
                if (Duration.between(start, end).abs().toHours() <= 12) {
                    var tmp = start; start = end; end = tmp;
                } else {
                    end = null;
                }
            }

        }
        // 기본 2시간 처리 또는 "2시간" 표현 대응
        if (start != null && end == null) {
            var dm = DURATION.matcher(text);
            if (dm.find()) {
                int hours = Integer.parseInt(dm.group(1));
                end = start.plusHours(hours);
            } else {
                end = start.plusHours(2); // 기본값
            }
        }

        if (date == null) date = LocalDate.now(KST);
        if (start != null && LocalDateTime.of(date, start).isBefore(LocalDateTime.now(KST).minusMinutes(5))) {
            date = date.plusDays(1);
        }

        Integer party = extractParty(text);
        Set<String> tags = extractTags(text);
        String branch = extractBranch(text);

        return ParsedRequest.builder()
                .zoneId(KST).date(date).startTime(start).endTime(end)
                .partySize(party).seatTags(tags).branch(branch).raw(rawText)
                .build();
    }

    private LocalDate inferDate(String t) {
        LocalDate now = LocalDate.now(KST);
        if (t.contains("오늘") || t.contains("금일")) return now;
        if (t.contains("내일")) return now.plusDays(1);
        if (t.contains("모레")) return now.plusDays(2);
        if (t.contains("3일 뒤")) return now.plusDays(3);
        if (t.contains("이틀 뒤") || t.contains("이틀 후")) return now.plusDays(2);

        for (int i = 1; i <= 7; i++) {
            // 공백 포함/미포함 모두 허용
            String weekday = DOW[i];
            if (t.matches(".*이번 ?주 ?"+weekday+".*")) {
                return nextOrSame(now, DayOfWeek.of(i));
            }
            if (t.matches(".*다음 ?주 ?"+weekday+".*")) {
                return nextOrSame(now.plusWeeks(1), DayOfWeek.of(i));
            }
            if (t.matches(".*다다음 ?주 ?"+weekday+".*")) {
                return nextOrSame(now.plusWeeks(2), DayOfWeek.of(i));
            }
        }

        var md = Pattern.compile("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일").matcher(t);
        if (md.find()) return LocalDate.of(now.getYear(),
                Integer.parseInt(md.group(1)), Integer.parseInt(md.group(2)));

        var full = Pattern.compile("(20\\d{2})[-./](\\d{1,2})[-./](\\d{1,2})").matcher(t);
        if (full.find()) return LocalDate.of(
                Integer.parseInt(full.group(1)), Integer.parseInt(full.group(2)), Integer.parseInt(full.group(3))
        );

        return null;
    }

    private static LocalDate nextOrSame(LocalDate d, DayOfWeek target) {
        int diff = (target.getValue() - d.getDayOfWeek().getValue() + 7) % 7;
        return d.plusDays(diff);
    }

    private String normalizeKoreanNumbers(String text) {
        String[][] numberMap = {
                {"한", "1"}, {"두", "2"}, {"세", "3"}, {"네", "4"},
                {"다섯", "5"}, {"여섯", "6"}, {"일곱", "7"}, {"여덟", "8"},
                {"아홉", "9"}, {"열", "10"}, {"열한", "11"}, {"열두", "12"}, {"스무", "20"}
        };
        for (String[] pair : numberMap) {
            text = text.replaceAll(pair[0] + "\\s*명", pair[1] + "명");
            text = text.replaceAll(pair[0] + "\\s*시", pair[1] + "시");
            text = text.replaceAll(pair[0] + "\\s*시간", pair[1] + "시간");
        }
        text = text.replaceAll("반", "30분");
        return text;
    }

    private LocalTime toTime(String md, String hh, String mm) {
        try {
            int h = Integer.parseInt(hh);
            int m = (mm == null) ? 0 : Integer.parseInt(mm);
            String s = md == null ? "" : md.toLowerCase(Locale.KOREAN);

            if (s.contains("오전") || s.contains("am")) {
                if (h == 12) h = 0;
            } else if (s.contains("오후") || s.contains("pm")) {
                if (h < 12) h += 12;
            } else {
                // 오전/오후 생략 시: 1~7시는 오후로 추정
                if (h >= 1 && h <= 7) h += 12;
            }

            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return LocalTime.of(h, m);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractParty(String t) {
        var m = PARTY.matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    private Set<String> extractTags(String t) {
        Set<String> tags = new LinkedHashSet<>();
        String lower = t.toLowerCase(Locale.KOREAN);

        if (lower.matches(".*(아무 자리|상관없음|무관|어디든|어디나).*")) return tags;

        if (lower.matches(".*(창가|창측|창문|햇빛|window).*")) tags.add("WINDOW");
        if (lower.matches(".*(콘센트|플러그|전원|충전|노트북|outlet|power).*")) tags.add("OUTLET");
        if (lower.matches(".*(조용|무음|집중|고요|quiet).*")) tags.add("QUIET");

        return tags;
    }

    private String extractBranch(String t) {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("강남점", "강남");
        aliases.put("강남역", "강남");
        aliases.put("부평점", "부평");
        aliases.put("부평역", "부평");
        aliases.put("부천점", "부천");
        aliases.put("부천역", "부천");
        for (String key : aliases.keySet()) {
            if (t.contains(key)) return aliases.get(key);
        }

        return null;
    }
}
