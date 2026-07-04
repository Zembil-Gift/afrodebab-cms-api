package com.afrodebab.cms.util;

/**
 * Computes the Ethiopian payroll breakdown for a gross monthly salary.
 *
 * <p>Income tax follows the progressive PAYE schedule where each bracket has a marginal
 * rate and a flat deduction ("payback") that makes the schedule continuous:
 * {@code tax = (gross * rate) - payback}. Amounts are expressed in minor units (cents).
 *
 * <pre>
 * base salary (ETB)   rate   payback
 * &lt;= 2000             0%     0
 * 2001 - 4000         15%    300
 * 4001 - 7000         20%    500
 * 7001 - 10000        25%    850
 * 10001 - 14000       30%    1350
 * &gt; 14000             35%    2050
 * </pre>
 *
 * <p>Retirement (pension) contributions are a flat 7% deducted from the employee's gross
 * plus an 11% contribution paid by the company (not deducted from the employee).
 */
public final class PayrollCalculator {
    public static final int EMPLOYEE_PENSION_PERCENT = 7;
    public static final int EMPLOYER_PENSION_PERCENT = 11;

    private PayrollCalculator() {
    }

    private record Bracket(long upperBoundMinor, int ratePercent, long paybackMinor) {
    }

    // Upper bounds in minor units (ETB * 100). The final bracket has no upper bound.
    private static final Bracket[] BRACKETS = {
            new Bracket(2000_00L, 0, 0L),
            new Bracket(4000_00L, 15, 300_00L),
            new Bracket(7000_00L, 20, 500_00L),
            new Bracket(10000_00L, 25, 850_00L),
            new Bracket(14000_00L, 30, 1350_00L),
            new Bracket(Long.MAX_VALUE, 35, 2050_00L)
    };

    public record Breakdown(
            long grossMinor,
            long incomeTaxMinor,
            long employeePensionMinor,
            long employerPensionMinor,
            long netMinor
    ) {
    }

    public static Breakdown compute(long grossMinor) {
        if (grossMinor < 0) {
            throw new IllegalArgumentException("gross salary must not be negative");
        }

        long incomeTaxMinor = incomeTax(grossMinor);
        long employeePensionMinor = roundedPercent(grossMinor, EMPLOYEE_PENSION_PERCENT);
        long employerPensionMinor = roundedPercent(grossMinor, EMPLOYER_PENSION_PERCENT);
        long netMinor = grossMinor - incomeTaxMinor - employeePensionMinor;

        return new Breakdown(grossMinor, incomeTaxMinor, employeePensionMinor, employerPensionMinor, netMinor);
    }

    private static long incomeTax(long grossMinor) {
        for (Bracket bracket : BRACKETS) {
            if (grossMinor <= bracket.upperBoundMinor()) {
                long tax = roundedPercent(grossMinor, bracket.ratePercent()) - bracket.paybackMinor();
                return Math.max(tax, 0L);
            }
        }
        // Unreachable: the final bracket's upper bound is Long.MAX_VALUE.
        throw new IllegalStateException("no tax bracket matched gross " + grossMinor);
    }

    /** Rounds {@code amount * percent / 100} to the nearest minor unit (half-up). */
    private static long roundedPercent(long amountMinor, int percent) {
        return (amountMinor * percent + 50) / 100;
    }
}
