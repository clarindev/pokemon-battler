package com.clarindev.school.pokedex;

/**
 * Holds the main method that starts the {@value Pokedex#APPLICATION_NAME} application.
 */
public class Main {
    /**
     * The application's entry point.
     * @param args accepts two parameters, a user's username and password
     */
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].strip();
        }
        new Pokedex(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null).run();
    }
}
