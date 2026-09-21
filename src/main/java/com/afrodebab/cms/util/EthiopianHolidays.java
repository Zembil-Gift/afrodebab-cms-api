package com.afrodebab.cms.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;

/**
 * Ethiopian public holidays expressed on the Gregorian calendar the rest of the app uses.
 * ponytail: Ethiopian→Gregorian mapping is valid 1901–2099 (Julian/Gregorian gap fixed at 13 days).
 */
public final class EthiopianHolidays {

    // {Ethiopian month, day}
    private static final int[][] FIXED_ETHIOPIAN = {
            {1, 1},   // Enkutatash (New Year)
            {1, 17},  // Meskel
            {5, 11},  // Timket (Epiphany)
            {6, 23},  // Adwa Victory Day
            {8, 23},  // International Labour Day
            {8, 27},  // Patriots' Victory Day
            {9, 20},  // Derg Downfall Day
    };
    // Genna is fixed by law on Jan 7 even in years where Tahsas 29 falls on Jan 8.
    private static final MonthDay GENNA = MonthDay.of(1, 7);

    private EthiopianHolidays() {
    }

    public static boolean isHoliday(LocalDate date) {
        if (MonthDay.from(date).equals(GENNA) || isOrthodoxEasterHoliday(date) || isIslamicHoliday(date)) {
            return true;
        }
        // An Ethiopian year spans two Gregorian years, so check both that can contain `date`.
        for (int ethYear = date.getYear() - 8; ethYear <= date.getYear() - 7; ethYear++) {
            for (int[] md : FIXED_ETHIOPIAN) {
                if (toGregorian(ethYear, md[0], md[1]).equals(date)) {
                    return true;
                }
            }
        }
        return false;
    }

    static LocalDate toGregorian(int ethYear, int month, int day) {
        int gregYear = ethYear + 7;
        // Meskerem 1 moves to Sep 12 in the year before a Gregorian leap year.
        int newYearDay = java.time.Year.isLeap(gregYear + 1) ? 12 : 11;
        return LocalDate.of(gregYear, 9, newYearDay).plusDays(30L * (month - 1) + day - 1);
    }

    // Fasika (Easter) and Siklet (Good Friday), Julian computus shifted to Gregorian.
    private static boolean isOrthodoxEasterHoliday(LocalDate date) {
        LocalDate easter = orthodoxEaster(date.getYear());
        return date.equals(easter) || date.equals(easter.minusDays(2));
    }

    static LocalDate orthodoxEaster(int year) {
        int a = year % 4, b = year % 7, c = year % 19;
        int d = (19 * c + 15) % 30;
        int e = (2 * a + 4 * b - d + 34) % 7;
        int month = (d + e + 114) / 31;
        int day = (d + e + 114) % 31 + 1;
        return LocalDate.of(year, month, day).plusDays(13);
    }

    // Eid al-Fitr, Eid al-Adha, Mawlid.
    // ponytail: Umm al-Qura calendar; Ethiopia's moon-sighted date can differ by a day. Add an admin override table if that bites.
    private static boolean isIslamicHoliday(LocalDate date) {
        HijrahDate hijri;
        try {
            hijri = HijrahDate.from(date);
        } catch (DateTimeException outOfHijrahRange) {
            return false;
        }
        int month = hijri.get(ChronoField.MONTH_OF_YEAR);
        int day = hijri.get(ChronoField.DAY_OF_MONTH);
        return (month == 10 && day == 1) || (month == 12 && day == 10) || (month == 3 && day == 12);
    }
}
