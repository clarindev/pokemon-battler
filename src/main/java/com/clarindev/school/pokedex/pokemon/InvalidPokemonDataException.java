package com.clarindev.school.pokedex.pokemon;

/**
 * Thrown when a Pokémon domain object has received invalid data.
 */
public class InvalidPokemonDataException extends RuntimeException {
    /**
     * @param message an informative message
     */
    public InvalidPokemonDataException(String message) {
        super(message);
    }
}
