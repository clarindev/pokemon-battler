package com.clarindev.school.pokedex.pokemon;

import com.clarindev.school.pokedex.Pokedex;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.lang.IO.println;

/**
 * Represents a Pokémon. Use the constants of this class as an aid to ensure valid data is passed to it.
 */
@SuppressWarnings({"StringConcatenationInLoop", "SizeReplaceableByIsEmpty"})
public class Pokemon {
    /** The minimum length the name of a Pokémon can have */
    public static final int NAME_MIN_LENGTH = 1;
    /** The maximum length the name of a Pokémon can have */
    public static final int NAME_MAX_LENGTH = 25;
    /** The minimum HP a Pokémon should be allowed to have. */
    public static final int HP_MIN = 0;
    /** The maximum HP a Pokémon should be allowed to have. */
    public static final int HP_MAX = 500;
    /** The minimum amount of attacks a Pokémon can hold. */
    public static final int ATTACKS_MIN_AMOUNT = 1;
    /** The maximum amount of attacks a Pokémon can hold. */
    public static final int ATTACKS_MAX_AMOUNT = 4;

    private final String name;
    private Type type;
    private int currentHp;
    private int maxHp;
    private final Set<Attack> attacks = new HashSet<>(ATTACKS_MAX_AMOUNT);

    /**
     * @param name the Pokémon's name
     * @param type the type of Pokémon
     * @param currentHp the current HP of the Pokémon
     * @param maxHp the maximum HP of the Pokémon
     * @param attacks the attacks to be held by the Pokémon
     * @throws InvalidPokemonDataException Pokémon domain object received invalid data, unchecked exception
     */
    public Pokemon(
            @JsonProperty("name")      String name,
            @JsonProperty("type")      Type type,
            @JsonProperty("currentHp") int currentHp,
            @JsonProperty("maxHp")     int maxHp,
            @JsonProperty("attacks")   List<Attack> attacks) {
        StringJoiner errors = new StringJoiner("  |  ");
        if (name == null) {
            errors.add("'name' cannot be null");
        } else {
            name = name.strip();
        }
        if (name != null && (name.length() < NAME_MIN_LENGTH || name.length() > NAME_MAX_LENGTH)) {
            errors.add("name length must be between " + NAME_MIN_LENGTH + " and " + NAME_MAX_LENGTH +
                ", is " + name.length() + (!name.isEmpty() ? " (name: " + name + ")" : ""));
        }
        if (type == null) {
            errors.add("'type' cannot be null");
        }
        boolean canSetMaxHp = true;
        if (maxHp < HP_MIN || maxHp > HP_MAX) {
            errors.add("'maxHp' ("  + maxHp + ") is not in range (" + HP_MIN + "-" + HP_MAX + ")");
            canSetMaxHp = false;
        }
        if (!canSetMaxHp) {
            errors.add("cannot validate 'currentHp' (maximum HP not set)");
        }
        else if (currentHp < HP_MIN || currentHp > maxHp) {
            errors.add("'currentHp' (" + currentHp + ") is not in range (" + HP_MIN + "-" + maxHp + ")");
        }
        Set<Attack> uniqueAttacks = null;
        if (attacks == null) {
            errors.add("'attacks' cannot be null");
        } else {
            List<Attack> nonNullAttacks = attacks.stream().filter(Objects::nonNull).toList();
            uniqueAttacks = new HashSet<>(nonNullAttacks);
            if (uniqueAttacks.size() < nonNullAttacks.size()) {
                errors.add("'attacks': an Attack should not be equal to another Attack");
            }
            else if (uniqueAttacks.size() < ATTACKS_MIN_AMOUNT || uniqueAttacks.size() > ATTACKS_MAX_AMOUNT) {
                errors.add("number of attacks must be between " + ATTACKS_MIN_AMOUNT + " and " + ATTACKS_MAX_AMOUNT +
                    ", is " + uniqueAttacks.size());
            }
        }
        if (errors.length() > 0) {
            throw new InvalidPokemonDataException(errors.toString());
        }
        this.name = name;
        this.type = type;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.attacks.addAll(Objects.requireNonNull(uniqueAttacks));
    }

    /**
     * @param name the Pokémon's name
     * @param type the type of Pokémon
     * @param hp the initial and maximum HP of the Pokémon
     * @param attacks the attacks to be held by the Pokémon
     * @throws InvalidPokemonDataException Pokémon domain object received invalid data, unchecked exception
     */
    public Pokemon(String name, Type type, int hp, List<Attack> attacks) {
        this(name, type, hp, hp, attacks);
    }

    /**
     * @return the Pokémon's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type of Pokémon
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type the Pokémon should transform into
     */
    public void setType(Type type) {
        if (type != null) {
            this.type = type;
        }
    }

    /**
     * @return the current HP of the Pokémon
     */
    public int getCurrentHp() {
        return currentHp;
    }

    /**
     * @param hp the HP with which to decrease the Pokémon's current HP, should not be negative
     */
    public void takeDamage(int hp) {
        currentHp = Math.max(currentHp - (Math.max(hp, 0)), HP_MIN);
    }

    /**
     * @param hp the HP with which to increase the Pokémon's current HP, should not be negative
     */
    public void recover(int hp) {
        currentHp = Math.min(currentHp + (Math.max(hp, 0)), HP_MAX);
    }

    /**
     * @return true if the Pokémon has fainted, false otherwise
     */
    public boolean hasFainted() {
        return currentHp <= 0;  // just in case, "==" should be enough
    }

    /**
     * @return the maximum HP of the Pokémon
     */
    public int getMaxHp() {
        return maxHp;
    }

    /**
     * Sets, if maxHp is valid, the new maximum HP of the Pokémon.
     * Note: This will also, proportionally, adjust the current HP.
     * @param maxHp the new maximum HP of the Pokémon
     */
    public void setMaxHp(int maxHp) {
        if (maxHp >= HP_MIN && maxHp <= HP_MAX) {
            currentHp = Math.min(new BigDecimal(currentHp)
                .multiply(new BigDecimal(maxHp))
                .divide(new BigDecimal(this.maxHp), RoundingMode.HALF_UP)
                .intValue(), maxHp);
            this.maxHp = maxHp;
        }
    }

    /**
     * Adds, or updates an attack already held by the Pokémon, if possible.
     * @param attack the attack to add or update
     */
    public void addOrUpdateAttack(Attack attack) {
        if (attack != null) {
            boolean wasInSet = attacks.remove(attack);
            if (wasInSet || attacks.size() < ATTACKS_MAX_AMOUNT) {
                attacks.add(attack);
            }
        }
    }

    /**
     * Removes an attack held by the Pokémon, if possible.
     * @param attack the attack to remove
     */
    public void removeAttack(Attack attack) {
        if (attack != null && attacks.size() > ATTACKS_MIN_AMOUNT) {
            attacks.remove(attack);
        }
    }

    /**
     * @return an unmodifiable list holding the attacks held by the Pokémon
     */
    public List<Attack> getAttacks() {
        return attacks.stream()
            .sorted(Comparator.comparing(Attack::name, Pokedex.SWE_COLLATOR).thenComparing(Attack::type))
            .toList();
    }

    /**
     * @return the number of attacks held by the Pokémon
     */
    @JsonIgnore
    public int getNumberOfAttacks() {
        return attacks.size();
    }

    /**
     * Prints information about the Pokémon.
     */
    public void printPokemon() {
        int minSpaceNeeded = Math.max(NAME_MAX_LENGTH, 26);
        String end = " |\n";
        String hp = currentHp + " / " + maxHp;
        String toPrint =
            "--------"        + "-".repeat(minSpaceNeeded)                    + "--"  + "\n";
        int width = toPrint.length() - 1;  // length of "\n" is 1
        toPrint +=
            "| Namn: " + name + " ".repeat(minSpaceNeeded - name.length())            + end +
            "| Typ:  " + type + " ".repeat(minSpaceNeeded - type.toString().length()) + end +
            "| HP:   " + hp   + " ".repeat(minSpaceNeeded - hp.length())              + end +
            "|    "           + "•".repeat(width - 10)                        + "   " + end +
            "| Attacker: "    + " ".repeat(minSpaceNeeded - 4)                        + end ;
        for (Attack attack : getAttacks()) {
            String damageAndAccuracy =  /* on one line */
                "Skada: " + " ".repeat(3 - String.valueOf(attack.baseDamage()).length()) + attack.baseDamage() + " HP  •  " +
                "Träff: " + " ".repeat(3 - String.valueOf(attack.accuracy()).length())   + attack.accuracy()   + " %";
            toPrint +=
                "| • "      + attack.name()     + " ".repeat(minSpaceNeeded - attack.name().length() + 4)            + end +
                "|   Typ: " + attack.type()     + " ".repeat(minSpaceNeeded - attack.type().toString().length() - 1) + end +
                "|   "      + damageAndAccuracy + " ".repeat(minSpaceNeeded - damageAndAccuracy.length() + 4)        + end ;
        }
        toPrint +=
            "--------"        + "-".repeat(minSpaceNeeded)                    + "--"        ;
        println(toPrint);
    }

    /**
     * @return a string representation of the Pokémon
     */
    @Override
    public String toString() {
        StringJoiner attacks = new StringJoiner(", ");
        for (Attack attack : getAttacks()) {
            attacks.add("[" + attack.name() + ", Typ: " + attack.type() + "]");
        }
        return "Namn: " + name + ", Typ: " + type + ", HP: " + currentHp + " / " + maxHp + ", Attacker: " + attacks;
    }

    /**
     * A Pokemon object is only equal to other Pokemon objects that have the same case-insensitive name.
     * @param otherObject the object to check for equality with this object
     * @return true if this object is equal to otherObject, false otherwise
     */
    @Override
    public final boolean equals(Object otherObject) {
        if (!(otherObject instanceof Pokemon otherPokemon)) {
            return false;
        }
        return name.equalsIgnoreCase(otherPokemon.name);
    }

    @Override
    public final int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }
}
