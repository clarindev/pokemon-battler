package com.clarindev.school.pokedex.pokemon;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an attack a Pokémon can hold. Use the constants of this
 * record class as an aid to ensure valid data is passed to it.
 * @param name the attack's name
 * @param type the type of attack
 * @param baseDamage the base damage of the attack
 * @param accuracy the accuracy of the attack
 */
public record Attack (String name, Type type, int baseDamage, int accuracy) {
    /** The minimum length the name of an Attack can have */
    public static final int NAME_MIN_LENGTH = Pokemon.NAME_MIN_LENGTH;
    /** The maximum length the name of an Attack can have */
    public static final int NAME_MAX_LENGTH = Pokemon.NAME_MAX_LENGTH;
    /** The minimum base damage an attack should be allowed to have. */
    public static final int BASE_DAMAGE_MIN = 0;
    /** The maximum base damage an attack should be allowed to have. */
    public static final int BASE_DAMAGE_MAX = 300;
    /** The minimum accuracy an attack should be allowed to have. */
    public static final int ACCURACY_MIN = 0;
    /** The maximum accuracy an attack should be allowed to have. */
    public static final int ACCURACY_MAX = 100;

    /**
     * @throws InvalidPokemonDataException Pokémon domain object received invalid data, unchecked exception
     */
    public Attack {
        StringJoiner errors = new StringJoiner("  |  ");
        if (name == null) {
            errors.add("'name' cannot be null");
        } else {
            name = name.strip();
        }
        //noinspection SizeReplaceableByIsEmpty
        if (name != null && (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH)) {
            errors.add("name length must be between " + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH +
            ", is " + name.length() + (!name.isEmpty() ? " (" + name + ")" : ""));
        }
        if (type == null) {
            errors.add("'type' cannot be null");
        }
        if (baseDamage < BASE_DAMAGE_MIN || baseDamage > BASE_DAMAGE_MAX) {
            errors.add("'baseDamage' ("  + baseDamage + ") is not in range (" + BASE_DAMAGE_MIN + "-" + BASE_DAMAGE_MAX + ")");
        }
        if (accuracy < ACCURACY_MIN || accuracy > ACCURACY_MAX) {
            errors.add("'accuracy' ("  + accuracy + ") is not in range (" + ACCURACY_MIN + "-" + ACCURACY_MAX + ")");
        }
        if (errors.length() > 0) {
            throw new InvalidPokemonDataException(errors.toString());
        }
    }

    /**
     * @param baseDamage the new base damage
     * @param accuracy the new accuracy
     * @return a copy of the attack with new stats
     * @throws InvalidPokemonDataException Pokémon domain object received invalid data, unchecked exception
     */
    public Attack withStats(int baseDamage, int accuracy) {
        return new Attack(name, type, baseDamage, accuracy);
    }

    /**
     * The description contains the attack's name, type and stats. Note: the stats are preceded by a line separator, "\n".
     * @return a description of the attack
     */
    @JsonIgnore
    public String getDescription() {
        return name + ", Typ: " + type + "\n" +
            "Basskada: " + "%%%dd".formatted(String.valueOf(BASE_DAMAGE_MAX).length()).formatted(baseDamage) + " HP  •  " +
            "Träffsäkerhet: " + "%%%dd".formatted(String.valueOf(ACCURACY_MAX).length()).formatted(accuracy) + " %";
    }

    /**
     * @return a string representation of the attack
     */
    @Override
    public String toString() {
        return "Namn: " + name + ", Typ: " + type + ", Basskada: " + baseDamage + " HP, Träffsäkerhet: " + accuracy + " %";
    }

    /**
     * An Attack object is only equal to other Attack objects that have the same case-insensitive name and Type.
     * @param otherObject the object to check for equality with this object
     * @return true if this object is equal to otherObject, false otherwise
     */
    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof Attack otherAttack)) {
            return false;
        }
        return name.equalsIgnoreCase(otherAttack.name) && type == otherAttack.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(Locale.ROOT), type);
    }
}
