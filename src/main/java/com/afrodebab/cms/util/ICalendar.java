package com.afrodebab.cms.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Minimal RFC 5545 writer: enough for a subscribable events feed (PUBLISH) and for meeting
 * invitations that calendar apps accept or cancel (REQUEST / CANCEL). Text is escaped and
 * lines are folded at 75 octets as the spec requires.
 */
public final class ICalendar {
    private ICalendar() {}

    public enum Method { PUBLISH, REQUEST, CANCEL }

    public record Attendee(String name, String email) {}

    /**
     * {@code sequence} must grow on every change of an invitation so calendar apps replace
     * the old copy. {@code organizer} and {@code attendees} are only used for REQUEST/CANCEL.
     */
    public record Event(String uid, Instant start, Instant end, String summary, String description,
                        String location, String url, int sequence, Attendee organizer, List<Attendee> attendees) {
        public Event(String uid, Instant start, Instant end, String summary, String description, String location, String url) {
            this(uid, start, end, summary, description, location, url, 0, null, List.of());
        }
    }

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public static String write(String calendarName, Method method, List<Event> events) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Mahberix//Company Calendar//EN");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:" + method);
        if (calendarName != null) line(out, "X-WR-CALNAME:" + text(calendarName));
        String stamp = UTC.format(Instant.now());
        for (Event e : events) {
            line(out, "BEGIN:VEVENT");
            line(out, "UID:" + e.uid());
            line(out, "DTSTAMP:" + stamp);
            line(out, "DTSTART:" + UTC.format(e.start()));
            line(out, "DTEND:" + UTC.format(e.end() != null ? e.end() : e.start().plusSeconds(3600)));
            line(out, "SEQUENCE:" + e.sequence());
            line(out, "SUMMARY:" + text(e.summary()));
            if (e.description() != null && !e.description().isBlank()) line(out, "DESCRIPTION:" + text(e.description()));
            if (e.location() != null && !e.location().isBlank()) line(out, "LOCATION:" + text(e.location()));
            if (e.url() != null && !e.url().isBlank()) line(out, "URL:" + e.url());
            if (method == Method.CANCEL) line(out, "STATUS:CANCELLED");
            else if (method == Method.REQUEST) line(out, "STATUS:CONFIRMED");
            if (method != Method.PUBLISH && e.organizer() != null) {
                line(out, "ORGANIZER;CN=" + param(e.organizer().name()) + ":mailto:" + e.organizer().email());
                for (Attendee a : e.attendees()) {
                    line(out, "ATTENDEE;CN=" + param(a.name()) + ";ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:" + a.email());
                }
            }
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private static String text(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n");
    }

    // Parameter values can't contain quotes; quoting keeps ':' ';' ',' in names harmless.
    private static String param(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "'")) + "\"";
    }

    /** Appends one content line folded at 75 octets (continuation lines start with a space). */
    private static void line(StringBuilder out, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int start = 0;
        int limit = 75;
        while (bytes.length - start > limit) {
            int end = start + limit;
            while ((bytes[end] & 0xC0) == 0x80) end--; // never split a UTF-8 character
            out.append(new String(bytes, start, end - start, StandardCharsets.UTF_8)).append("\r\n ");
            start = end;
            limit = 74; // the leading space counts toward the next line's 75
        }
        out.append(new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8)).append("\r\n");
    }
}
