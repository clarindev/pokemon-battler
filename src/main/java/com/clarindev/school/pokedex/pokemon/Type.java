package com.clarindev.school.pokedex.pokemon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Represents the different types available in the Pokémon domain.
 */
public enum Type {
    ELECTRIC, FIRE, GRASS, NORMAL, WATER;

    /** Preferred way to get a collection of the types, alphabetically sorted. */
    public static final List<Type> TYPES = Arrays.stream(values())
        .sorted(Comparator.comparing(Type::toString)).toList();

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
