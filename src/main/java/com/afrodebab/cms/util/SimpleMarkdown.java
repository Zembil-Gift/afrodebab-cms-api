package com.afrodebab.cms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The small formatting subset managers can use in broadcasts: paragraphs (blank line),
 * line breaks, {@code **bold**}, {@code *italic*}, {@code [text](https://…)} links and
 * "- " / "1. " lists. The input is HTML-escaped before any markup is added, and links only
 * accept http(s)/mailto, so the output is safe to embed in emails and the dashboard as-is.
 */
public final class SimpleMarkdown {
    private SimpleMarkdown() {}

    private static final Pattern LINK = Pattern.compile("\\[([^\\]\\n]+)]\\(((?:https?://|mailto:)[^\\s)]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(\\S(?:.*?\\S)?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("\\*(\\S(?:.*?\\S)?)\\*");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^\\s*\\d+[.)]\\s+(.*)$");

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        StringBuilder html = new StringBuilder();
        for (String block : markdown.replace("\r\n", "\n").trim().split("\\n\\s*\\n")) {
            html.append(blockHtml(block.split("\n")));
        }
        return html.toString();
    }

    private static String blockHtml(String[] lines) {
        StringBuilder out = new StringBuilder();
        List<String> paragraph = new ArrayList<>();
        String listTag = null;
        for (String line : lines) {
            var bullet = BULLET.matcher(line);
            var numbered = NUMBERED.matcher(line);
            String tag = bullet.matches() ? "ul" : numbered.matches() ? "ol" : null;
            if (tag == null) {
                if (listTag != null) {
                    out.append("</").append(listTag).append('>');
                    listTag = null;
                }
                paragraph.add(inline(line.trim()));
                continue;
            }
            flushParagraph(out, paragraph);
            if (!tag.equals(listTag)) {
                if (listTag != null) out.append("</").append(listTag).append('>');
                out.append('<').append(tag).append('>');
                listTag = tag;
            }
            out.append("<li>").append(inline((tag.equals("ul") ? bullet : numbered).group(1).trim())).append("</li>");
        }
        if (listTag != null) out.append("</").append(listTag).append('>');
        flushParagraph(out, paragraph);
        return out.toString();
    }

    private static void flushParagraph(StringBuilder out, List<String> paragraph) {
        if (paragraph.isEmpty()) return;
        out.append("<p>").append(String.join("<br>", paragraph)).append("</p>");
        paragraph.clear();
    }

    private static String inline(String text) {
        String html = escape(text);
        html = LINK.matcher(html).replaceAll("<a href=\"$2\" target=\"_blank\" rel=\"noopener noreferrer\">$1</a>");
        html = BOLD.matcher(html).replaceAll("<strong>$1</strong>");
        return ITALIC.matcher(html).replaceAll("<em>$1</em>");
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
