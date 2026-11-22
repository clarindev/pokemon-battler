package com.clarindev.school.pokedex;

/**
 * Thrown in the event of an unsuccessful login attempt.
 */
public class UnsuccessfulLoginAttemptException extends Exception {
    /**
     * @param message an informative message
     */
    public UnsuccessfulLoginAttemptException(String message) {
        super(message);
    }
}
