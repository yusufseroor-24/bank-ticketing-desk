package com.ga.bankdesk.masking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingSessionTest {

    private static final String INPUT = "My card 4111 1111 1111 1111 was charged 250 BHD twice on 3 Nov. "
            + "Call me on +973 3600 1234 or yusuf@example.com. IBAN BH67BMAG00001299123456";

    @Test
    void masksAllFourTypesAndKeepsAmountsAndDates() {
        MaskingSession s = new MaskingSession();
        String masked = s.mask(INPUT);
        assertTrue(masked.contains("[CARD_1]") && !masked.contains("4111"));
        assertTrue(masked.contains("[PHONE_1]") && !masked.contains("3600"));
        assertTrue(masked.contains("[EMAIL_1]") && !masked.contains("yusuf@"));
        assertTrue(masked.contains("[IBAN_1]") && !masked.contains("BMAG"));
        assertTrue(masked.contains("250 BHD") && masked.contains("3 Nov"));
        assertEquals(4, s.maskedCount());
    }

    @Test
    void restoreGivesBackTheOriginal() {
        MaskingSession s = new MaskingSession();
        assertEquals(INPUT, s.restore(s.mask(INPUT)));
        assertEquals("card 4111 1111 1111 1111 ok", s.restore("card CARD_1 ok"));
    }

    @Test
    void sameValueGetsSameToken() {
        MaskingSession s = new MaskingSession();
        assertEquals("[CARD_1] and again [CARD_1]", s.mask("4111 1111 1111 1111 and again 4111 1111 1111 1111"));
        assertEquals(1, s.maskedCount());
    }

    @Test
    void randomNumbersAreLeftAlone() {
        String text = "Reference 1234 5678 9012 3456 and amount 1500.75 on 2026-11-09, ticket 4821";
        assertEquals(text, new MaskingSession().mask(text));
        String badIban = "IBAN BH67BMAG00001299123457 is wrong";
        assertEquals(badIban, new MaskingSession().mask(badIban));
    }

    @Test
    void phoneNeedsPlusOr00() {
        assertEquals("Call 36001234 or [PHONE_1]", new MaskingSession().mask("Call 36001234 or 0097336001234"));
    }

    @Test
    void worksWithArabicAndNull() {
        assertEquals("بطاقتي [CARD_1] تم خصمها مرتين",
                new MaskingSession().mask("بطاقتي 4111 1111 1111 1111 تم خصمها مرتين"));
        assertNull(new MaskingSession().mask(null));
        assertEquals("", new MaskingSession().mask(""));
    }
}