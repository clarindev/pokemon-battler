/*
Author: C. Clarin
Created: 2026-09-11
Last changed: 2026-10-27
*/

package com.clarindev.utility.terminal;

import module java.base;

import static java.lang.IO.*;

/**
 * Use this class to get input from a user through a terminal window.
 */
public class TerminalReader {
    private boolean enterAllowed = false;
    private boolean scientificNotationAllowed = false;
    private boolean excessiveTrailingZerosAllowed = true;
    private Language language = Language.ENGLISH;
    private String prompt = "> ";
    private String errorPrefix = "- ";

    /**
     * Describes how to handle whitespace when reading a string.
     */
    public enum Whitespace {
        KEEP, NORMALIZE, REMOVE, STRIP
    }

    /**
     * Equal to: new TerminalReader(false, false, true, Language.ENGLISH)<br>
     * See: {@link #TerminalReader(boolean, boolean, boolean, Language)}
     */
    public TerminalReader() {}

    /**
     * @param enterAllowed see: {@link #isEnterAllowed()}
     * @param scientificNotationAllowed see: {@link #isScientificNotationAllowed()}
     * @param excessiveTrailingZerosAllowed see: {@link #isExcessiveTrailingZerosAllowed()}
     */
    public TerminalReader(
            boolean enterAllowed, boolean scientificNotationAllowed, boolean excessiveTrailingZerosAllowed) {
        this.enterAllowed = enterAllowed;
        this.scientificNotationAllowed = scientificNotationAllowed;
        this.excessiveTrailingZerosAllowed = excessiveTrailingZerosAllowed;
    }

    /**
     * @param enterAllowed see: {@link #isEnterAllowed()}
     * @param scientificNotationAllowed see: {@link #isScientificNotationAllowed()}
     * @param excessiveTrailingZerosAllowed see: {@link #isExcessiveTrailingZerosAllowed()}
     * @param language see: {@link #getLanguage()}
     */
    public TerminalReader(boolean enterAllowed, boolean scientificNotationAllowed,
            boolean excessiveTrailingZerosAllowed, Language language) {
        this(enterAllowed, scientificNotationAllowed, excessiveTrailingZerosAllowed);
        this.language = language;
    }

    /**
     * Gets an integer from the user.
     * @param message a prompt message
     * @return the integer entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public int readInt(String message) {
        return readDecimal(message, new BigDecimal(Integer.MIN_VALUE), new BigDecimal(Integer.MAX_VALUE), 0).intValue();
    }

    /**
     * Gets an integer from the user.
     * @param message a prompt message
     * @param min the lowest accepted integer
     * @param max the highest accepted integer
     * @return the integer entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public int readInt(String message, int min, int max) {
        return readDecimal(message, new BigDecimal(min), new BigDecimal(max), 0).intValue();
    }

    /**
     * Gets an integer from the user.
     * @param message a prompt message
     * @return the integer entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public BigInteger readInteger(String message) {
        return new BigInteger(readDecimal(message, null, null, 0).toPlainString());
    }

    /**
     * Gets an integer from the user.
     * @param message a prompt message
     * @param min The lowest accepted integer. Can be null, no limit.
     * @param max The highest accepted integer. Can be null, no limit.
     * @return the integer entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public BigInteger readInteger(String message, BigInteger min, BigInteger max) {
        return new BigInteger(readDecimal(message, (min != null ? new BigDecimal(min.toString()) : null),
            (max != null ? new BigDecimal(max.toString()) : null), 0).toPlainString());
    }

    /**
     * Gets a decimal number from the user.
     * @param message a prompt message
     * @param fractionDigits The number of fraction digits. Will be 0 if a negative value is supplied.
     * @return the decimal number entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public BigDecimal readDecimal(String message, int fractionDigits) {
        return readDecimal(message, null, null, fractionDigits);
    }

    /**
     * Gets a decimal number from the user. The number of fraction digits is set by the BigDecimal object with the
     * highest scale. Unless that scale is less than 0, in which case no fraction digits will be used. If both min
     * and max are null, no fraction digits will be allowed.
     * @param message a prompt message
     * @param min The lowest accepted decimal number. Can be null, no limit.
     * @param max The highest accepted decimal number. Can be null, no limit.
     * @return the decimal number entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public BigDecimal readDecimal(String message, BigDecimal min, BigDecimal max) {
        return readDecimal(message, min, max, 0);
    }

    /**
     * Gets a decimal number from the user. The number of fraction digits is set by the BigDecimal object with the
     * highest scale. Unless that scale is less than 0, in which case no fraction digits will be used. If no BigDecimal
     * object is supplied, the value of fractionDigitsIfNotSet will be used (unless the value is less than 0).
     * @param message a prompt message
     * @param min The lowest accepted decimal number. Can be null, no limit.
     * @param max The highest accepted decimal number. Can be null, no limit.
     * @param fractionDigitsIfNotSet if no BigDecimal object is supplied, this sets the number of fraction digits (unless
     * the value is less than 0)
     * @return the decimal number entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    private BigDecimal readDecimal(String message, BigDecimal min, BigDecimal max, int fractionDigitsIfNotSet) {
        int fractionDigits = 0;
        fractionDigits = (min != null ? Math.max(min.scale(), fractionDigits) : fractionDigits);
        fractionDigits = (max != null ? Math.max(max.scale(), fractionDigits) : fractionDigits);
        fractionDigits = (min == null && max == null ? Math.max(fractionDigitsIfNotSet, 0) : fractionDigits);
        min = (min != null ? min.setScale(fractionDigits, RoundingMode.UNNECESSARY) : null);
        max = (max != null ? max.setScale(fractionDigits, RoundingMode.UNNECESSARY) : null);

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
        }

        BigDecimal decimal = null;
        while (decimal == null) {
            String input = readStrippedString(message);
            if (enterAllowed && input.isEmpty()) {
                throw new EnterAllowedException();
            }
            try {
                String quotedSeparator = Pattern.quote(String.valueOf(language.decimalSeparator));
                String pattern =
                    "^"                                +  /* start    */
                    "[+-]?"                            +  /* sign     */
                    "(0|[1-9][0-9]*)"                  +  /* integer  */
                    "(" + quotedSeparator + "[0-9]+)?" +  /* fraction */
                    "([eE][+-]?(0|[1-9][0-9]*))?"      +  /* exponent */
                    "$";                                  /* end      */
                if (!input.matches(pattern)) {
                    throw new NumberFormatException();
                }
                decimal = new BigDecimal(input.replace(language.decimalSeparator, '.'));
            } catch (NumberFormatException e) {
                printErrorMessage(language.cannotParseInput.get());
            }
            if (decimal != null) {
                if (!scientificNotationAllowed && input.toLowerCase(Locale.ROOT).contains("e")) {
                    printErrorMessage(language.scientificNotationNotAllowed.get());
                    decimal = null;
                } else if (decimal.compareTo(BigDecimal.ZERO) == 0 && input.charAt(0) == '-') {
                    printErrorMessage(language.negativeZeroNotSupported.get());
                    decimal = null;
                } else if (Math.max(decimal.scale(), 0) < fractionDigits) {
                    decimal = decimal.setScale(fractionDigits, RoundingMode.UNNECESSARY);
                } else if (decimal.scale() > fractionDigits) {
                    if (excessiveTrailingZerosAllowed && decimal.stripTrailingZeros().scale() <= fractionDigits) {
                        decimal = decimal.setScale(fractionDigits, RoundingMode.UNNECESSARY);
                    } else {
                        printErrorMessage(language.tooManyFractionDigits.apply(fractionDigits));
                        decimal = null;
                    }
                }
                if (decimal != null && min != null && decimal.compareTo(min) < 0) {
                    printErrorMessage(language.lessThanAllowed.apply(min));
                    decimal = null;
                }
                if (decimal != null && max != null && decimal.compareTo(max) > 0) {
                    printErrorMessage(language.greaterThanAllowed.apply(max));
                    decimal = null;
                }
            }
        }

        return decimal;
    }

    /**
     * Gets a string from the user.
     * @param message a prompt message
     * @return the string entered by the user
     */
    public String readString(String message) {
        return readString(message, 0, Integer.MAX_VALUE, Whitespace.KEEP);
    }

    /**
     * Gets a string from the user.
     * @param message a prompt message
     * @param minLength the minimum length of the string to enter
     * @param maxLength the maximum length of the string to enter
     * @return the string entered by the user
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public String readString(String message, int minLength, int maxLength) {
        return readString(message, minLength, maxLength, Whitespace.KEEP);
    }

    /**
     * Gets a string from the user with leading and trailing whitespace stripped away and all other consecutive
     * whitespace collapsed into a single space.
     * @param message a prompt message
     * @return the string entered by the user after being cleaned up
     */
    public String readNormalizedString(String message) {
        return readString(message, 0, Integer.MAX_VALUE, Whitespace.NORMALIZE);
    }

    /**
     * Gets a string from the user with leading and trailing whitespace stripped away and all other consecutive
     * whitespace collapsed into a single space.
     * @param message a prompt message
     * @param minLength the minimum length of the string to enter
     * @param maxLength the maximum length of the string to enter
     * @return the string entered by the user after being cleaned up
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public String readNormalizedString(String message, int minLength, int maxLength) {
        return readString(message, minLength, maxLength, Whitespace.NORMALIZE);
    }

    /**
     * Gets a string from the user with all whitespace removed.
     * @param message a prompt message
     * @return the string entered by the user after being cleaned up
     */
    public String readWhitespaceRemovedString(String message) {
        return readString(message, 0, Integer.MAX_VALUE, Whitespace.REMOVE);
    }

    /**
     * Gets a string from the user with all whitespace removed.
     * @param message a prompt message
     * @param minLength the minimum length of the string to enter
     * @param maxLength the maximum length of the string to enter
     * @return the string entered by the user after being cleaned up
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public String readWhitespaceRemovedString(String message, int minLength, int maxLength) {
        return readString(message, minLength, maxLength, Whitespace.REMOVE);
    }

    /**
     * Gets a string from the user with leading and trailing whitespace stripped away.
     * @param message a prompt message
     * @return the string entered by the user after being cleaned up
     */
    public String readStrippedString(String message) {
        return readString(message, 0, Integer.MAX_VALUE, Whitespace.STRIP);
    }

    /**
     * Gets a string from the user with leading and trailing whitespace stripped away.
     * @param message a prompt message
     * @param minLength the minimum length of the string to enter
     * @param maxLength the maximum length of the string to enter
     * @return the string entered by the user after being cleaned up
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    public String readStrippedString(String message, int minLength, int maxLength) {
        return readString(message, minLength, maxLength, Whitespace.STRIP);
    }

    /**
     * Gets a string from the user. (Negative lengths will be set to 0.)
     * @param message a prompt message
     * @param minLength the minimum length of the string to enter
     * @param maxLength the maximum length of the string to enter
     * @param whitespace decides what to do with whitespace
     * @return the string entered by the user, possibly cleaned up
     * @throws EnterAllowedException unchecked exception, see: {@link #isEnterAllowed()}
     */
    private String readString(String message, int minLength, int maxLength, Whitespace whitespace) {
        minLength = Math.max(minLength, 0);
        maxLength = Math.max(maxLength, 0);
        if (minLength > maxLength) {
            throw new IllegalArgumentException(
                "minLength (" + minLength + ") > maxLength (" + maxLength + "), " +
                "note: negative lengths will have been set to 0");
        }

        String input = null;
        while (input == null) {
            input = readln((message != null ? message : "") + prompt);
            if (enterAllowed && minLength > 0 && input.isEmpty()) {
                throw new EnterAllowedException();
            }
            int originalLength = input.length();
            switch (whitespace) {
                case KEEP      -> {}
                case NORMALIZE -> input = input.strip().replaceAll("(?U)\\s+", " ");
                case STRIP     -> input = input.strip();
                case REMOVE    -> input = input.replaceAll("(?U)\\s+", "");
            }
            int length = input.length();
            String modifiedInput = (originalLength == length ? "" : "\n'" + input + "'");
            if (length < minLength) {
                printErrorMessage(language.notEnoughCharacters.apply(length, minLength) + modifiedInput);
                input = null;
            } else if (length > maxLength) {
                printErrorMessage(language.tooManyCharacters.apply(length, maxLength) + modifiedInput);
                input = null;
            }
        }

        return input;
    }

    /**
     * Wait for the user to press 'Enter' before continuing execution.
     * @param message a prompt message
     */
    public void waitForEnter(String message) {
        readString(message);
    }

    /**
     * Asks the user a yes/no question. Use y(es) or n(o), or the equivalent in the current language.
     * @param question the question to ask
     * @return true if the user answered yes, false if the user answered no
     */
    public boolean askYesNoQuestion(String question) {
        Boolean answer = null;
        while (answer == null) {
            String input = readStrippedString(question).toLowerCase(Locale.ROOT);
            answer = language.answers.get(input);
            if (answer == null) {
                printErrorMessage(language.invalidAnswer.get());
            }
        }
        return answer;
    }

    /**
     * Prints an error message. This method is public to make it easy to print an error message that looks like the ones
     * created internally. A typical use case of this is when a value was found not valid during external validation. Use
     * "\n" for newlines.
     * @param message the error message to print
     */
    public void printErrorMessage(String message) {
        println(errorPrefix + (message != null ?
            message.replaceAll("\n", System.lineSeparator() + " ".repeat(errorPrefix.length())) : "<null>"));
    }

    /**
     * Returns whether pressing 'Enter' is allowed as an answer instead of entering a value. This setting is only relevant
     * when the user has been asked to enter a number, or asked to enter a string whose length must be greater than 0.
     * @return true if it is allowed, false otherwise
     */
    public boolean isEnterAllowed() {
        return enterAllowed;
    }

    /**
     * Sets whether pressing 'Enter' should be allowed as an answer instead of entering a value. This setting is only
     * relevant when the user has been asked to enter a number, or asked to enter a string whose length must be greater
     * than 0.
     * @param enterAllowed true if it should be allowed, false otherwise
     */
    public void setEnterAllowed(boolean enterAllowed) {
        this.enterAllowed = enterAllowed;
    }

    /**
     * Returns whether entering a number using scientific notation is allowed.
     * @return true if it is allowed, false otherwise
     */
    public boolean isScientificNotationAllowed() {
        return scientificNotationAllowed;
    }

    /**
     * Sets whether entering a number using scientific notation should be allowed.
     * @param scientificNotationAllowed true if it should be allowed, false otherwise
     */
    public void setScientificNotationAllowed(boolean scientificNotationAllowed) {
        this.scientificNotationAllowed = scientificNotationAllowed;
    }

    /**
     * Returns whether trailing zeros that exceed the maximum number of allowed fraction digits are allowed
     * (and truncated). This setting is only relevant when the user has been asked to enter a decimal number.
     * @return true if they are allowed, false otherwise
     */
    public boolean isExcessiveTrailingZerosAllowed() {
        return excessiveTrailingZerosAllowed;
    }

    /**
     * Sets whether trailing zeros that exceed the maximum number of allowed fraction digits should be allowed
     * (and truncated). This setting is only relevant when the user has been asked to enter a decimal number.
     * @param excessiveTrailingZerosAllowed true if they should be allowed, false otherwise
     */
    public void setExcessiveTrailingZerosAllowed(boolean excessiveTrailingZerosAllowed) {
        this.excessiveTrailingZerosAllowed = excessiveTrailingZerosAllowed;
    }

    /**
     * @return the current language used
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * @param language the language to use
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * @return the string used as the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the string to use as the prompt
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * @return the string that is printed before an error message
     */
    public String getErrorPrefix() {
        return errorPrefix;
    }

    /**
     * @param errorPrefix the string to print before an error message
     */
    public void setErrorPrefix(String errorPrefix) {
        this.errorPrefix = errorPrefix;
    }

    /**
     * Returns the decimal separator used in a language.
     * @param language a language
     * @return the decimal separator
     */
    public char getDecimalSeparator(Language language) {
        return language.decimalSeparator;
    }

    /**
     * The supported languages.
     */
    public enum Language {
        /*
         *  1. Decimal separator.
         *  2. Answers to a yes/no question.
         *  3. Entered value less than allowed.
         *  4. Entered value greater than allowed.
         *  5. Too many fraction digits.
         *  6. Cannot parse input.
         *  7. Scientific notation not allowed.
         *  8. Negative zero not supported.
         *  9. Not enough characters entered.
         * 10. Too many characters entered.
         * 11. Invalid answer.
         */

        ENGLISH(
            /*  1. */ '.',
            /*  2. */ Map.of("y", true, "yes", true, "n", false, "no", false),
            /*  3. */ (min) -> "input cannot be less than " + min.toPlainString(),
            /*  4. */ (max) -> "input cannot be greater than " + max.toPlainString(),
            /*  5. */ (max) -> "too many fraction digits, only " + max + " allowed",
            /*  6. */ () -> "cannot parse input",
            /*  7. */ () -> "scientific notation not allowed",
            /*  8. */ () -> "negative zero not supported",
            /*  9. */ (length, minLength) -> length + " character" + (length != 1 ? "s" : "") + " entered, " +
                          "at least " + minLength + " required",
            /* 10. */ (length, maxLength) -> length + " character" + (length != 1 ? "s" : "") + " entered, " +
                          "no more than " + maxLength + " allowed",
            /* 11. */ () -> "invalid answer"
        ),
        SWEDISH(
            /*  1. */ ',',
            /*  2. */ Map.of("j", true, "ja", true, "n", false, "nej", false),
            /*  3. */ (min) -> "inmatningen kan inte vara lägre än " + min.toPlainString().replace('.', ','),
            /*  4. */ (max) -> "inmatningen kan inte vara högre än " + max.toPlainString().replace('.', ','),
            /*  5. */ (max) -> "för många decimaler, endast " + max + " tillåts",
            /*  6. */ () -> "kan inte tolka inmatningen",
            /*  7. */ () -> "vetenskaplig notation inte tillåten",
            /*  8. */ () -> "minus noll stöds inte",
            /*  9. */ (length, minLength) -> length + " tecken " + (length != 1 ? "inmatade" : "inmatat") +
                          ", minst " + minLength + " krävs",
            /* 10. */ (length, maxLength) -> length + " tecken " + (length != 1 ? "inmatade" : "inmatat") +
                          ", maximalt " + maxLength + " tillåts",
            /* 11. */ () -> "ogiltigt svar"
        );

        /*  1. */ private final char decimalSeparator;
        /*  2. */ private final Map<String, Boolean> answers;
        /*  3. */ private final Function<BigDecimal, String> lessThanAllowed;
        /*  4. */ private final Function<BigDecimal, String> greaterThanAllowed;
        /*  5. */ private final Function<Integer, String> tooManyFractionDigits;
        /*  6. */ private final Supplier<String> cannotParseInput;
        /*  7. */ private final Supplier<String> scientificNotationNotAllowed;
        /*  8. */ private final Supplier<String> negativeZeroNotSupported;
        /*  9. */ private final BiFunction<Integer, Integer, String> notEnoughCharacters;
        /* 10. */ private final BiFunction<Integer, Integer, String> tooManyCharacters;
        /* 11. */ private final Supplier<String> invalidAnswer;

        Language(
                /*  1. */ char decimalSeparator,
                /*  2. */ Map<String, Boolean> answers,
                /*  3. */ Function<BigDecimal, String> lessThanAllowed,
                /*  4. */ Function<BigDecimal, String> greaterThanAllowed,
                /*  5. */ Function<Integer, String> tooManyFractionDigits,
                /*  6. */ Supplier<String> cannotParseInput,
                /*  7. */ Supplier<String> scientificNotationNotAllowed,
                /*  8. */ Supplier<String> negativeZeroNotSupported,
                /*  9. */ BiFunction<Integer, Integer, String> notEnoughCharacters,
                /* 10. */ BiFunction<Integer, Integer, String> tooManyCharacters,
                /* 11. */ Supplier<String> invalidAnswer) {
            /*  1. */ this.decimalSeparator = decimalSeparator;
            /*  2. */ this.answers = answers;
            /*  3. */ this.lessThanAllowed = lessThanAllowed;
            /*  4. */ this.greaterThanAllowed = greaterThanAllowed;
            /*  5. */ this.tooManyFractionDigits = tooManyFractionDigits;
            /*  6. */ this.cannotParseInput = cannotParseInput;
            /*  7. */ this.scientificNotationNotAllowed = scientificNotationNotAllowed;
            /*  8. */ this.negativeZeroNotSupported = negativeZeroNotSupported;
            /*  9. */ this.notEnoughCharacters = notEnoughCharacters;
            /* 10. */ this.tooManyCharacters = tooManyCharacters;
            /* 11. */ this.invalidAnswer = invalidAnswer;
        }
    }

    /**
     * Signals that the user pressed 'Enter' instead of entering a value.
     */
    @SuppressWarnings("serial")
    public static class EnterAllowedException extends RuntimeException {
        private EnterAllowedException() {
            super("the user pressed 'Enter' instead of entering a value", null, false, false);
        }
    }
}
