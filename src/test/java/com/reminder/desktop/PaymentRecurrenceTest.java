package com.reminder.desktop;

import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.RecurrenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentRecurrenceTest {

    // =========================================================================
    // 1. MONTHLY RECURRENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("Monthly recurrence: Jan 31 -> Feb 28 -> March 31 in non-leap year (2026)")
    public void testMonthly_Jan31ToFeb28ToMarch31_NonLeapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 31, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan31 = cal.getTimeInMillis();

        // Cycle 1: January to February
        long febDue = MonthlyPayment.calculateImmediateNextDueDate(jan31, RecurrenceType.MONTHLY, 31);
        Calendar febCal = Calendar.getInstance();
        febCal.setTimeInMillis(febDue);

        assertEquals(2026, febCal.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, febCal.get(Calendar.MONTH));
        assertEquals(28, febCal.get(Calendar.DAY_OF_MONTH), "Jan 31 must clamp to Feb 28 in non-leap year");

        // Cycle 2: February to March (must restore anchor 31)
        long marchDue = MonthlyPayment.calculateImmediateNextDueDate(febDue, RecurrenceType.MONTHLY, 31);
        Calendar marchCal = Calendar.getInstance();
        marchCal.setTimeInMillis(marchDue);

        assertEquals(2026, marchCal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, marchCal.get(Calendar.MONTH));
        assertEquals(31, marchCal.get(Calendar.DAY_OF_MONTH), "Feb 28 must restore to March 31 using anchor 31");
    }

    @Test
    @DisplayName("Monthly recurrence: Jan 31 -> Feb 29 -> March 31 in leap year (2024)")
    public void testMonthly_Jan31ToFeb29ToMarch31_LeapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 31, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan31 = cal.getTimeInMillis();

        // Cycle 1: January to February (leap year)
        long febDue = MonthlyPayment.calculateImmediateNextDueDate(jan31, RecurrenceType.MONTHLY, 31);
        Calendar febCal = Calendar.getInstance();
        febCal.setTimeInMillis(febDue);

        assertEquals(2024, febCal.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, febCal.get(Calendar.MONTH));
        assertEquals(29, febCal.get(Calendar.DAY_OF_MONTH), "Jan 31 must clamp to Feb 29 in leap year");

        // Cycle 2: February to March
        long marchDue = MonthlyPayment.calculateImmediateNextDueDate(febDue, RecurrenceType.MONTHLY, 31);
        Calendar marchCal = Calendar.getInstance();
        marchCal.setTimeInMillis(marchDue);

        assertEquals(2024, marchCal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, marchCal.get(Calendar.MONTH));
        assertEquals(31, marchCal.get(Calendar.DAY_OF_MONTH), "Feb 29 must restore to March 31 in leap year");
    }

    @Test
    @DisplayName("Monthly recurrence: Feb 28 -> March 28 (day 28 anchoring)")
    public void testMonthly_Feb28ToMarch28() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 28, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long feb28 = cal.getTimeInMillis();

        long marchDue = MonthlyPayment.calculateImmediateNextDueDate(feb28, RecurrenceType.MONTHLY, 28);
        Calendar marchCal = Calendar.getInstance();
        marchCal.setTimeInMillis(marchDue);

        assertEquals(Calendar.MARCH, marchCal.get(Calendar.MONTH));
        assertEquals(28, marchCal.get(Calendar.DAY_OF_MONTH), "Feb 28 with anchor 28 advances to March 28");
    }

    @Test
    @DisplayName("Monthly recurrence: March 31 -> April 30 and May 31")
    public void testMonthly_March31ToApril30AndMay31() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MARCH, 31, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long march31 = cal.getTimeInMillis();

        // March 31 -> April 30
        long aprilDue = MonthlyPayment.calculateImmediateNextDueDate(march31, RecurrenceType.MONTHLY, 31);
        Calendar aprilCal = Calendar.getInstance();
        aprilCal.setTimeInMillis(aprilDue);

        assertEquals(Calendar.APRIL, aprilCal.get(Calendar.MONTH));
        assertEquals(30, aprilCal.get(Calendar.DAY_OF_MONTH), "March 31 must clamp to April 30");

        // April 30 -> May 31
        long mayDue = MonthlyPayment.calculateImmediateNextDueDate(aprilDue, RecurrenceType.MONTHLY, 31);
        Calendar mayCal = Calendar.getInstance();
        mayCal.setTimeInMillis(mayDue);

        assertEquals(Calendar.MAY, mayCal.get(Calendar.MONTH));
        assertEquals(31, mayCal.get(Calendar.DAY_OF_MONTH), "April 30 must restore to May 31 using anchor 31");
    }

    // =========================================================================
    // 2. BI-MONTHLY, QUARTERLY, HALF-YEARLY & YEARLY TESTS
    // =========================================================================

    @Test
    @DisplayName("Bi-monthly recurrence: Dec 31 -> Feb 28 -> Apr 30 -> Jun 30 -> Aug 31 anchor restoration")
    public void testBiMonthly_DriftPrevention() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.DECEMBER, 31, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dec31 = cal.getTimeInMillis();

        // Dec 31 + 2 months -> Feb 28, 2026 (clamped)
        long febDue = MonthlyPayment.calculateImmediateNextDueDate(dec31, RecurrenceType.BI_MONTHLY, 31);
        Calendar cFeb = Calendar.getInstance(); cFeb.setTimeInMillis(febDue);
        assertEquals(2026, cFeb.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, cFeb.get(Calendar.MONTH));
        assertEquals(28, cFeb.get(Calendar.DAY_OF_MONTH));

        // Feb 28 + 2 months -> Apr 30, 2026 (clamped)
        long aprDue = MonthlyPayment.calculateImmediateNextDueDate(febDue, RecurrenceType.BI_MONTHLY, 31);
        Calendar cApr = Calendar.getInstance(); cApr.setTimeInMillis(aprDue);
        assertEquals(Calendar.APRIL, cApr.get(Calendar.MONTH));
        assertEquals(30, cApr.get(Calendar.DAY_OF_MONTH));

        // Apr 30 + 2 months -> Jun 30, 2026 (clamped)
        long junDue = MonthlyPayment.calculateImmediateNextDueDate(aprDue, RecurrenceType.BI_MONTHLY, 31);
        Calendar cJun = Calendar.getInstance(); cJun.setTimeInMillis(junDue);
        assertEquals(Calendar.JUNE, cJun.get(Calendar.MONTH));
        assertEquals(30, cJun.get(Calendar.DAY_OF_MONTH));

        // Jun 30 + 2 months -> Aug 31, 2026 (anchor 31 restored!)
        long augDue = MonthlyPayment.calculateImmediateNextDueDate(junDue, RecurrenceType.BI_MONTHLY, 31);
        Calendar cAug = Calendar.getInstance(); cAug.setTimeInMillis(augDue);
        assertEquals(Calendar.AUGUST, cAug.get(Calendar.MONTH));
        assertEquals(31, cAug.get(Calendar.DAY_OF_MONTH), "Anchor 31 must be restored for August");
    }

    @Test
    @DisplayName("Bi-monthly recurrence: Overdue advancement across multiple 2-month cycles")
    public void testBiMonthly_OverdueAdvancement() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan1Due = cal.getTimeInMillis();

        // Reference time: June 15, 2026 (overdue by > 5 months)
        Calendar refCal = Calendar.getInstance();
        refCal.set(2026, Calendar.JUNE, 15, 12, 0, 0);
        long referenceTime = refCal.getTimeInMillis();

        // Jan 1 -> Mar 1 -> May 1 -> Jul 1 (first date > June 15)
        long nextDue = MonthlyPayment.calculateNextDueDate(jan1Due, RecurrenceType.BI_MONTHLY, 1, 28, referenceTime);
        assertTrue(nextDue > referenceTime, "Next due date must be strictly after reference time");

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(nextDue);
        assertEquals(Calendar.JULY, resultCal.get(Calendar.MONTH));
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Quarterly recurrence: Oct 31 -> Jan 31 -> April 30 -> July 31")
    public void testQuarterly_DriftPrevention() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long oct31 = cal.getTimeInMillis();

        // Oct 31 + 3 months -> Jan 31
        long janDue = MonthlyPayment.calculateImmediateNextDueDate(oct31, RecurrenceType.QUARTERLY, 31);
        Calendar cJan = Calendar.getInstance(); cJan.setTimeInMillis(janDue);
        assertEquals(2027, cJan.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cJan.get(Calendar.MONTH));
        assertEquals(31, cJan.get(Calendar.DAY_OF_MONTH));

        // Jan 31 + 3 months -> April 30 (clamped)
        long aprDue = MonthlyPayment.calculateImmediateNextDueDate(janDue, RecurrenceType.QUARTERLY, 31);
        Calendar cApr = Calendar.getInstance(); cApr.setTimeInMillis(aprDue);
        assertEquals(Calendar.APRIL, cApr.get(Calendar.MONTH));
        assertEquals(30, cApr.get(Calendar.DAY_OF_MONTH));

        // April 30 + 3 months -> July 31 (restored)
        long julDue = MonthlyPayment.calculateImmediateNextDueDate(aprDue, RecurrenceType.QUARTERLY, 31);
        Calendar cJul = Calendar.getInstance(); cJul.setTimeInMillis(julDue);
        assertEquals(Calendar.JULY, cJul.get(Calendar.MONTH));
        assertEquals(31, cJul.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Half-yearly recurrence: Aug 31 -> Feb 28 -> Aug 31 anchor restoration")
    public void testHalfYearly_DriftPrevention() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.AUGUST, 31, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long aug31 = cal.getTimeInMillis();

        // Aug 31 + 6 months -> Feb 28, 2026 (clamped)
        long febDue = MonthlyPayment.calculateImmediateNextDueDate(aug31, RecurrenceType.HALF_YEARLY, 31);
        Calendar cFeb = Calendar.getInstance(); cFeb.setTimeInMillis(febDue);
        assertEquals(2026, cFeb.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, cFeb.get(Calendar.MONTH));
        assertEquals(28, cFeb.get(Calendar.DAY_OF_MONTH));

        // Feb 28 + 6 months -> Aug 31, 2026 (anchor 31 restored!)
        long augDue = MonthlyPayment.calculateImmediateNextDueDate(febDue, RecurrenceType.HALF_YEARLY, 31);
        Calendar cAug = Calendar.getInstance(); cAug.setTimeInMillis(augDue);
        assertEquals(2026, cAug.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, cAug.get(Calendar.MONTH));
        assertEquals(31, cAug.get(Calendar.DAY_OF_MONTH), "Anchor 31 must be restored for August in half-yearly recurrence");
    }

    @Test
    @DisplayName("Half-yearly recurrence: Overdue advancement across multiple 6-month cycles")
    public void testHalfYearly_OverdueAdvancement() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 15, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan15Due = cal.getTimeInMillis();

        // Reference time: August 20, 2026 (overdue by > 1.5 years)
        Calendar refCal = Calendar.getInstance();
        refCal.set(2026, Calendar.AUGUST, 20, 12, 0, 0);
        long referenceTime = refCal.getTimeInMillis();

        // Jan 15, 2025 -> Jul 15, 2025 -> Jan 15, 2026 -> Jul 15, 2026 -> Jan 15, 2027
        long nextDue = MonthlyPayment.calculateNextDueDate(jan15Due, RecurrenceType.HALF_YEARLY, 15, 28, referenceTime);
        assertTrue(nextDue > referenceTime, "Next due date must be strictly after reference time");

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(nextDue);
        assertEquals(2027, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, resultCal.get(Calendar.MONTH));
        assertEquals(15, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Yearly recurrence: Feb 29 leap year anchoring across 4 consecutive years")
    public void testYearly_Feb29LeapAnchoring() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 29, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        // 2024 -> 2025 (Feb 28)
        long y2025 = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.YEARLY, 29);
        Calendar c25 = Calendar.getInstance(); c25.setTimeInMillis(y2025);
        assertEquals(2025, c25.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, c25.get(Calendar.MONTH));
        assertEquals(28, c25.get(Calendar.DAY_OF_MONTH));

        // 2025 -> 2028 (Leap year restores Feb 29)
        long y2026 = MonthlyPayment.calculateImmediateNextDueDate(y2025, RecurrenceType.YEARLY, 29);
        long y2027 = MonthlyPayment.calculateImmediateNextDueDate(y2026, RecurrenceType.YEARLY, 29);
        long y2028 = MonthlyPayment.calculateImmediateNextDueDate(y2027, RecurrenceType.YEARLY, 29);
        Calendar c28 = Calendar.getInstance(); c28.setTimeInMillis(y2028);
        assertEquals(2028, c28.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, c28.get(Calendar.MONTH));
        assertEquals(29, c28.get(Calendar.DAY_OF_MONTH), "Feb 29 anchor must restore in next leap year 2028");
    }

    // =========================================================================
    // 3. CUSTOM RECURRENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("Custom recurrence: Exact intervals 1, 7, 14, 28, 30, 45, 90, and 365 days")
    public void testCustom_ExactIntervals() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1, 9, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        int[] intervals = {1, 7, 14, 28, 30, 45, 90, 365};
        for (int interval : intervals) {
            long nextDue = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.CUSTOM_DAYS, 1, interval);
            long diffDays = (nextDue - start) / (24L * 60L * 60L * 1000L);
            assertEquals(interval, diffDays, "Difference in days must exactly equal interval " + interval);
        }
    }

    @Test
    @DisplayName("Custom recurrence: Invalid values fallback to 28 days")
    public void testCustom_InvalidValuesFallback() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        // 0 days -> defaults to 28
        long next0 = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.CUSTOM_DAYS, 1, 0);
        assertEquals(28, (next0 - start) / (24L * 60L * 60L * 1000L));

        // Negative days (-15) -> defaults to 28
        long nextNeg = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.CUSTOM_DAYS, 1, -15);
        assertEquals(28, (nextNeg - start) / (24L * 60L * 60L * 1000L));

        // > 365 days (500) -> defaults to 28
        long nextOver = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.CUSTOM_DAYS, 1, 500);
        assertEquals(28, (nextOver - start) / (24L * 60L * 60L * 1000L));
    }

    @Test
    @DisplayName("Custom recurrence: Month-end transitions across calendar months")
    public void testCustom_MonthEndTransitions() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 28, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan28 = cal.getTimeInMillis();

        // 7 days from Jan 28: Jan 28 + 3d -> Jan 31 + 4d -> Feb 4
        long next7 = MonthlyPayment.calculateImmediateNextDueDate(jan28, RecurrenceType.CUSTOM_DAYS, 28, 7);
        Calendar c7 = Calendar.getInstance(); c7.setTimeInMillis(next7);
        assertEquals(Calendar.FEBRUARY, c7.get(Calendar.MONTH));
        assertEquals(4, c7.get(Calendar.DAY_OF_MONTH));

        // 14 days from Jan 28 -> Feb 11
        long next14 = MonthlyPayment.calculateImmediateNextDueDate(jan28, RecurrenceType.CUSTOM_DAYS, 28, 14);
        Calendar c14 = Calendar.getInstance(); c14.setTimeInMillis(next14);
        assertEquals(Calendar.FEBRUARY, c14.get(Calendar.MONTH));
        assertEquals(11, c14.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Custom recurrence: Leap-year boundary crossing (Feb 27 + 5 days)")
    public void testCustom_LeapYearBoundary() {
        // Leap year 2024: Feb has 29 days
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 27, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start2024 = cal.getTimeInMillis();

        long next2024 = MonthlyPayment.calculateImmediateNextDueDate(start2024, RecurrenceType.CUSTOM_DAYS, 27, 5);
        Calendar c24 = Calendar.getInstance(); c24.setTimeInMillis(next2024);
        // Feb 27 + 2d -> Feb 29 + 3d -> March 3
        assertEquals(Calendar.MARCH, c24.get(Calendar.MONTH));
        assertEquals(3, c24.get(Calendar.DAY_OF_MONTH), "Leap year Feb 27 + 5 days must be March 3");

        // Non-leap year 2025: Feb has 28 days
        Calendar cal2025 = Calendar.getInstance();
        cal2025.set(2025, Calendar.FEBRUARY, 27, 9, 0, 0);
        cal2025.set(Calendar.MILLISECOND, 0);
        long start2025 = cal2025.getTimeInMillis();

        long next2025 = MonthlyPayment.calculateImmediateNextDueDate(start2025, RecurrenceType.CUSTOM_DAYS, 27, 5);
        Calendar c25 = Calendar.getInstance(); c25.setTimeInMillis(next2025);
        // Feb 27 + 1d -> Feb 28 + 4d -> March 4
        assertEquals(Calendar.MARCH, c25.get(Calendar.MONTH));
        assertEquals(4, c25.get(Calendar.DAY_OF_MONTH), "Non-leap year Feb 27 + 5 days must be March 4");
    }

    @Test
    @DisplayName("Custom recurrence: Overdue recurrence progression past completion time")
    public void testCustom_OverdueAdvancement() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long jan1Due = cal.getTimeInMillis();

        // Reference time is 50 days later: Feb 20, 2026
        Calendar refCal = Calendar.getInstance();
        refCal.set(2026, Calendar.FEBRUARY, 20, 15, 0, 0);
        long referenceTime = refCal.getTimeInMillis();

        // 14-day recurrence: Jan 1 -> Jan 15 -> Jan 29 -> Feb 12 -> Feb 26 (strict future of Feb 20)
        long nextDue = MonthlyPayment.calculateNextDueDate(jan1Due, RecurrenceType.CUSTOM_DAYS, 1, 14, referenceTime);
        assertTrue(nextDue > referenceTime, "Next due date must be strictly after reference time");

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTimeInMillis(nextDue);
        assertEquals(Calendar.FEBRUARY, resultCal.get(Calendar.MONTH));
        assertEquals(26, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Custom recurrence: Time-of-day preservation across cycles")
    public void testCustom_TimeOfDayPreservation() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 10, 14, 45, 30);
        cal.set(Calendar.MILLISECOND, 250);
        long start = cal.getTimeInMillis();

        long next = MonthlyPayment.calculateImmediateNextDueDate(start, RecurrenceType.CUSTOM_DAYS, 10, 28);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTimeInMillis(next);

        assertEquals(14, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, nextCal.get(Calendar.MINUTE));
        assertEquals(30, nextCal.get(Calendar.SECOND));
        assertEquals(250, nextCal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Switching recurrence: Custom to Monthly and back")
    public void testCustom_SwitchingRecurrenceTypes() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 10, 9, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        MonthlyPayment payment = new MonthlyPayment(1, null, "Gym", start, false, System.currentTimeMillis(), "SYNCED", 45.0, RecurrenceType.CUSTOM_DAYS, "0", 10, 14);
        assertEquals(RecurrenceType.CUSTOM_DAYS, payment.getRecurrence());
        assertEquals(14, payment.getCustomIntervalDays());

        // Jan 10 + 14d -> Jan 24
        long next14 = MonthlyPayment.calculateImmediateNextDueDate(payment.getDueDate(), payment.getRecurrence(), payment.getOriginalDueDay(), payment.getCustomIntervalDays());
        Calendar c14 = Calendar.getInstance(); c14.setTimeInMillis(next14);
        assertEquals(Calendar.JANUARY, c14.get(Calendar.MONTH));
        assertEquals(24, c14.get(Calendar.DAY_OF_MONTH));

        // Switch to Monthly: custom interval must not interfere
        payment.setRecurrence(RecurrenceType.MONTHLY);
        long nextMonthly = MonthlyPayment.calculateImmediateNextDueDate(payment.getDueDate(), payment.getRecurrence(), payment.getOriginalDueDay(), payment.getCustomIntervalDays());
        Calendar cM = Calendar.getInstance(); cM.setTimeInMillis(nextMonthly);
        assertEquals(Calendar.FEBRUARY, cM.get(Calendar.MONTH));
        assertEquals(10, cM.get(Calendar.DAY_OF_MONTH));
    }

    // =========================================================================
    // 4. DETERMINISTIC TIMESTAMP TESTS
    // =========================================================================

    @Test
    @DisplayName("D7: getLastPaidAt() returns null deterministically when unrecorded")
    public void testD7_getLastPaidAt_deterministicNullHandling() {
        MonthlyPayment payment = new MonthlyPayment(1, null, "Electric", 1700000000L, true, System.currentTimeMillis(), "SYNCED");
        // lastPaidAt is null
        Long t1 = payment.getLastPaidAt();
        Long t2 = payment.getLastPaidAt();

        assertNull(t1, "Unrecorded lastPaidAt must return null even when completed is true");
        assertNull(t2);
        assertEquals(t1, t2, "Consecutive calls must be idempotent");
    }

    @Test
    @DisplayName("D7: getLastPaidAt() returns identical recorded timestamp across calls")
    public void testD7_getLastPaidAt_deterministicValidTimestamp() throws InterruptedException {
        long recorded = 1750000000000L;
        MonthlyPayment payment = new MonthlyPayment(1, null, "Water", 1700000000L, true, System.currentTimeMillis(), "SYNCED");
        payment.setLastPaidAt(recorded);

        Long t1 = payment.getLastPaidAt();
        Thread.sleep(5);
        Long t2 = payment.getLastPaidAt();

        assertEquals(Long.valueOf(recorded), t1);
        assertEquals(t1, t2, "Repeated reads must not alter or regenerate timestamp");
    }

    @Test
    @DisplayName("isRecentlyPaid: Correctly handles custom recurrence")
    public void testCustom_isRecentlyPaid() {
        long now = System.currentTimeMillis();
        long futureDue = now + 7L * 24L * 60L * 60L * 1000L;
        MonthlyPayment payment = new MonthlyPayment(1, null, "Milk", futureDue, false, now, "LOCAL_ONLY", 20.0, RecurrenceType.CUSTOM_DAYS, "0", 1, 7);

        // Not yet paid
        assertFalse(payment.isRecentlyPaid());

        // Paid right now for the upcoming cycle
        payment.setLastPaidAt(now);
        assertTrue(payment.isRecentlyPaid(), "Payment paid for the upcoming 7-day cycle must evaluate to recently paid");

        // When due date has passed, it is no longer recently paid (it is due now)
        payment.setDueDate(now - 1000L);
        assertFalse(payment.isRecentlyPaid(), "Overdue payment must evaluate to NOT recently paid");
    }
}
