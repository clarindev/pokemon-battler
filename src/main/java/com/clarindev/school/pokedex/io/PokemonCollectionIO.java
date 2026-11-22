package com.clarindev.school.pokedex.io;

import com.clarindev.school.pokedex.pokemon.InvalidPokemonDataException;
import com.clarindev.school.pokedex.pokemon.Pokemon;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Holds methods for loading and saving a file that contains a Pokémon collection.
 */
public class PokemonCollectionIO {
    private static final ObjectMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private PokemonCollectionIO() {}

    /**
     * Loads a Pokémon collection from a file.
     * @param filePath the path to the file that holds the collection
     * @return the loaded Pokémon collection
     * @throws InvalidPokemonDataException Pokémon domain object received invalid data, unchecked exception
     * @throws PokemonCollectionIOException if the collection could not be loaded, I/O exception
     */
    public static List<Pokemon> loadPokemonCollection(Path filePath) throws PokemonCollectionIOException {
        try {
            return MAPPER.readValue(filePath.toFile(), new TypeReference<List<Pokemon>>() {});
        } catch (IOException ioException) {
            if (ioException.getCause() instanceof InvalidPokemonDataException invalidPokemonDataException) {
                throw invalidPokemonDataException;
            }
            throw new PokemonCollectionIOException("could not load '" + filePath.getFileName() + "'");
        }
    }

    /**
     * Saves a Pokémon collection to a file.
     * @param collection the collection to save
     * @param filePath the path to the file that will hold the collection
     * @throws PokemonCollectionIOException if the collection could not be saved, I/O exception
     */
    public static void savePokemonCollection(List<Pokemon> collection, Path filePath)
            throws PokemonCollectionIOException {
        try {
            MAPPER.writeValue(filePath.toFile(), collection);
        } catch (IOException e) {
            throw new PokemonCollectionIOException("could not save '" + filePath.getFileName() + "'");
        }
    }

    /**
     * Thrown when a Pokémon collection could not be loaded or saved because of an I/O exception.
     */
    public static class PokemonCollectionIOException extends Exception {
        /**
         * @param message An informative message.
         */
        private PokemonCollectionIOException(String message) {
            super(message);
        }
    }
}
