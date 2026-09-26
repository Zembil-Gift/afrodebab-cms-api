package com.afrodebab.cms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {

    private static long etb(long major) {
        return major * 100L;
    }

    @Test
    void noTaxAtOrBelowThreshold() {
        PayrollCalculator.Breakdown b = PayrollCalculator.compute(etb(2000));
        assertEquals(0L, b.incomeTaxMinor());
        assertEquals(etb(140), b.employeePensionMinor()); // 7% of 2000
        assertEquals(etb(220), b.employerPensionMinor()); // 11% of 2000
        assertEquals(etb(2000) - etb(140), b.netMinor());
    }

    @Test
    void taxByBracketMatchesFormula() {
        // (gross * rate) - payback
        assertEquals(etb(500), PayrollCalculator.compute(etb(5000)).incomeTaxMinor());   // 5000*20% - 500
        assertEquals(etb(1150), PayrollCalculator.compute(etb(8000)).incomeTaxMinor());  // 8000*25% - 850
        assertEquals(etb(2250), PayrollCalculator.compute(etb(12000)).incomeTaxMinor()); // 12000*30% - 1350
        assertEquals(etb(4950), PayrollCalculator.compute(etb(20000)).incomeTaxMinor()); // 20000*35% - 2050
    }

    @Test
    void netAndPensionForTypicalSalary() {
        PayrollCalculator.Breakdown b = PayrollCalculator.compute(etb(10000));
        assertEquals(etb(1650), b.incomeTaxMinor());        // 10000*25% - 850
        assertEquals(etb(700), b.employeePensionMinor());   // 7%
        assertEquals(etb(1100), b.employerPensionMinor());  // 11%
        assertEquals(etb(10000) - etb(1650) - etb(700), b.netMinor()); // 7650
    }

    @Test
    void bracketBoundariesAreContinuous() {
        assertEquals(etb(300), PayrollCalculator.compute(etb(4000)).incomeTaxMinor());  // top of 15% bracket
        assertEquals(etb(900), PayrollCalculator.compute(etb(7000)).incomeTaxMinor());  // top of 20% bracket
        assertEquals(etb(2850), PayrollCalculator.compute(etb(14000)).incomeTaxMinor()); // top of 30% bracket
    }
}
