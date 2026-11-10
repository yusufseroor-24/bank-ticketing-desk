package com.ga.bankdesk.masking;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hides sensitive values before text is sent to OpenAI and puts them back in the answer.
 * One session per request, the map of tokens lives in memory only
 * Detects emails, IBANs (using mod-97 check), card numbers (using Luhn check) and international phone numbers.
 */

public class MaskingSession {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}(?: ?[A-Z0-9]{4}){2,7}(?: ?[A-Z0-9]{1,3})?\\b");
    private static final Pattern CARD = Pattern.compile("\\b\\d(?:[ -]?\\d){12,18}\\b");
    //only numbers that start with + or 00, so normal numbers (amounts, dates, ids) are not considered
    private static final Pattern PHONE = Pattern.compile("(?<![\\w+])(?:\\+|00)\\d[\\d -]{6,16}\\d");

    private final Map<String, String> tokenToValue = new LinkedHashMap<>();
    private final Map<String, String> valueToToken = new LinkedHashMap<>();
    private final Map<String, Integer> counters = new LinkedHashMap<>();

    //replaces every sensitive value with a token like [CARD_1] and the same value always gets the same token
    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = replace(text, EMAIL, "EMAIL", value -> true);
        result = replace(result, IBAN, "IBAN", MaskingSession::validIban);
        result = replace(result, CARD, "CARD", MaskingSession::validCard);
        result = replace(result, PHONE, "PHONE", MaskingSession::validPhone);
        return result;
    }

    //puts the original values back where the tokens appear
    public String restore(String text) {
        if (text == null || text.isEmpty() || tokenToValue.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : tokenToValue.entrySet()) {
            //[CARD_1]
            String token = entry.getKey();
            //CARD_1
            String name = token.substring(1, token.length() - 1);
            result = result.replace(token, entry.getValue());
            //the model sometimes drops the brackets
            result = Pattern.compile("\\b" + name + "\\b").matcher(result)
                    .replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    //how many values were masked (hidden)
    public int maskedCount() {
        return tokenToValue.size();
    }

    private String replace(String text, Pattern pattern, String type, Predicate<String> valid) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = matcher.group();
            String replacement = valid.test(value) ? tokenFor(type, value) : value;
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String tokenFor(String type, String value) {
        String existing = valueToToken.get(value);
        if (existing != null) {
            return existing;
        }
        int number = counters.merge(type, 1, Integer::sum);
        String token = "[" + type + "_" + number + "]";
        valueToToken.put(value, token);
        tokenToValue.put(token, value);
        return token;
    }

    //Luhn check: the checksum every real card number passes
    static boolean validCard(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean second = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (second) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            second = !second;
        }
        return sum % 10 == 0;
    }

    //IBAN check: move the first 4 characters to the end, turn letters into numbers, the remainder mod 97 must be 1
    static boolean validIban(String raw) {
        String s = raw.replace(" ", "");
        if (s.length() < 15 || s.length() > 34) {
            return false;
        }
        String rearranged = s.substring(4) + s.substring(0, 4);
        int remainder = 0;
        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                remainder = (remainder * 10 + (c - '0')) % 97;
            } else {
                remainder = (remainder * 100 + (c - 'A' + 10)) % 97;
            }
        }
        return remainder == 1;
    }

    static boolean validPhone(String raw) {
        int digits = raw.replaceAll("[^0-9]", "").length();
        return digits >= 8 && digits <= 15;
    }
}