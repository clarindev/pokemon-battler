/*
Author: C. Clarin
Created: 2026-10-20
Last changed: 2026-10-22
*/

package com.clarindev.utility.terminal;

import module java.base;

import static com.clarindev.utility.terminal.TerminalReader.Whitespace;
import static java.lang.IO.println;

/**
 * Holds methods that are intended to be used in an application that communicates with a user through a terminal.
 */
public class TerminalUiHelper {
    // prevent instantiation
    private TerminalUiHelper() {}

    /**
     * Asks the user for a string that is unique according to the provided predicate.
     * @param message a message that describes what is being asked for
     * @param minLength the minimum length of the string
     * @param maxLength the maximum length of the string
     * @param whitespace describes how to handle whitespace in the entered string
     * @param predicate the predicate used when checking for uniqueness
     * @param reader the TerminalReader to use
     * @param notUnique a message displayed to the user when a string is not unique
     * @return a unique string
     */
    public static String getUniqueString(String message, int minLength, int maxLength, Whitespace whitespace,
            Predicate<String> predicate, TerminalReader reader, String notUnique) {
        String uniqueString = null;
        while (uniqueString == null) {
            String input = switch (whitespace) {
                case KEEP      -> reader.readString(message, minLength, maxLength);
                case NORMALIZE -> reader.readNormalizedString(message, minLength, maxLength);
                case REMOVE    -> reader.readWhitespaceRemovedString(message, minLength, maxLength);
                case STRIP     -> reader.readStrippedString(message, minLength, maxLength);
            };
            if (!predicate.test(input)) {
                uniqueString = input;
            } else {
                reader.printErrorMessage(notUnique + "\n'" + input + "'");
            }
        }
        return uniqueString;
    }

    /**
     * Prints a list of strings, row by row, prefixed with an index (starting from 1).
     * @param list the list to print
     * @param indentation the indentation used when printing a row
     */
    public static void printListNumbered(List<String> list, int indentation) {
        printListNumbered(list, String::toString, indentation);
    }

    /**
     * Prints a text for each object in a list, row by row, prefixed with an index (starting from 1).
     * @param list the objects for which each a text will be printed
     * @param textProvider provides what to print
     * @param indentation the indentation used when printing a row
     */
    public static <T> void printListNumbered(List<T> list, Function<T, String> textProvider, int indentation) {
        int indexWidth = String.valueOf(list.size()).length() + Math.max(indentation, 0);
        for (int i = 0; i < list.size(); i++) {
            String text = textProvider.apply(list.get(i)).replace("\n", "\n" + " ".repeat(indexWidth + 2));
            println(("%%%dd. %%s").formatted(indexWidth).formatted((i + 1), text));
        }
    }
}
