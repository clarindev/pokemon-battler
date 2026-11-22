package com.clarindev.school.pokedex;

import com.clarindev.school.pokedex.io.PokemonCollectionIO.PokemonCollectionIOException;
import com.clarindev.school.pokedex.pokemon.*;
import com.clarindev.utility.terminal.TerminalReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.clarindev.school.pokedex.io.PokemonCollectionIO.*;
import static com.clarindev.school.pokedex.pokemon.Attack.*;
import static com.clarindev.school.pokedex.pokemon.Pokemon.*;
import static com.clarindev.school.pokedex.pokemon.Type.TYPES;
import static com.clarindev.utility.terminal.TerminalReader.*;
import static com.clarindev.utility.terminal.TerminalReader.Whitespace.*;
import static com.clarindev.utility.terminal.TerminalUiHelper.getUniqueString;
import static com.clarindev.utility.terminal.TerminalUiHelper.*;
import static java.lang.IO.*;

/**
 * Represents an enhanced Pokédex that lets the user edit a collection of Pokémon.
 */
public class Pokedex {
    public static final Collator SWE_COLLATOR = Collator.getInstance(Locale.of("sv"));
    /** Intended to be used as a prefix when printing a message from the application. */
    public static final String PRINT_PREFIX = new TerminalReader().getErrorPrefix();
    public static final String APPLICATION_NAME = "Pokédex 3000";
    public static final String APPLICATION_VERSION = "1.0";

    private static final String UNKNOWN_USER = "<unknown>";
    private static final Path LAST_USED_POKEMON_COLLECTION = Path.of("last-collection");
    private static final Path POKEMON_COLLECTIONS_DIR = Path.of("collections");
    private static final int FILE_NAME_MIN_LENGTH = 1;
    private static final int FILE_NAME_MAX_LENGTH = 15;

    private final String username;
    private final List<Pokemon> pokemonCollection = new ArrayList<>();
    private final List<MainMenuItem> mainMenuItems = List.of(
        /*  1. */ MainMenuItem.SHOW,
        /*  2. */ MainMenuItem.SHOW_ALL,
        /*  3. */ MainMenuItem.CREATE,
        /*  4. */ MainMenuItem.EDIT,
        /*  5. */ MainMenuItem.REMOVE,
        /*  6. */ MainMenuItem.REMOVE_ALL,
        /*  7. */ MainMenuItem.FILES,
        /*  8. */ MainMenuItem.RESET,
        /*  9. */ MainMenuItem.QUIT
    );
    private final TerminalReader reader = new TerminalReader();
    private boolean quit = false;

    /**
     * @param username a user's username
     * @param password a user's password
     */
    Pokedex(String username, String password) {
        try {
            verifyUser(username, password);
        } catch (UnsuccessfulLoginAttemptException e) {
            username = UNKNOWN_USER;
            println();
            InternalWorker.work();
            println(e.getMessage());
        }
        this.username = username;
        if (!loadCollectionFile(LAST_USED_POKEMON_COLLECTION)) {
            installDefaultPokemonCollection();
        }
        if (!Files.isDirectory(POKEMON_COLLECTIONS_DIR)) {
            try {
                Files.createDirectory(POKEMON_COLLECTIONS_DIR);
            } catch (IOException e) {
                println();
                InternalWorker.work();
                println("\33[31m" + PRINT_PREFIX + "kunde inte skapa mappen att spara Pokémon samlingar i\33[m");
            }
        }
        reader.setLanguage(Language.SWEDISH);
        println();
        InternalWorker.work();
        printWelcomeMessage();
    }

    /**
     * Holds the main loop in which the application runs.
     */
    void run() {
        try {
            while (!quit) {
                println();
                InternalWorker.work();
                printListNumbered(mainMenuItems, MainMenuItem::getMenuText, 0);
                reader.setEnterAllowed(false);
                int selected = reader.readInt("", 1, mainMenuItems.size()) - 1;
                reader.setEnterAllowed(true);
                try {
                    mainMenuItems.get(selected).execute(this);
                } catch (EnterAllowedException e) {
                    // the user chose to go back to this menu by pressing 'Enter'
                }
            }
        } catch (Exception e) {
            if (!quit) {
                println();
                InternalWorker.work();
                println("\n\33[31m" + PRINT_PREFIX + APPLICATION_NAME + " avslutas\n" +
                    " ".repeat(PRINT_PREFIX.length()) + "orsak: okänd\33[m");
                quit();
            }
        }
    }

    /**
     * Lets the user select one of its Pokémon to have information about it shown.
     */
    private void showPokemon() {
        println();
        InternalWorker.work();
        if (pokemonCollection.isEmpty()) {
            println(PRINT_PREFIX + "det finns ingen Pokémon att visa");
            return;
        }
        println(PRINT_PREFIX + "Visa");
        printListNumbered(pokemonCollection, Pokemon::getName, 0);
        int selected = reader.readInt("", 1, pokemonCollection.size()) - 1;
        println();
        InternalWorker.work();
        pokemonCollection.get(selected).printPokemon();
    }

    /**
     * Shows information about all the user's Pokémon.
     */
    private void showAllPokemon() {
        println();
        if (pokemonCollection.isEmpty()) {
            InternalWorker.work();
            println(PRINT_PREFIX + "det finns inga Pokémon att visa");
            return;
        }
        for (Pokemon pokemon : pokemonCollection) {
            InternalWorker.work();
            pokemon.printPokemon();
        }
    }

    /**
     * Lets the user create a new Pokémon.
     */
    private void createPokemon() {
        println();
        InternalWorker.work();
        println(PRINT_PREFIX + "Skapa");
        String name = getUniqueString(
            "Namn (" + Pokemon.NAME_MIN_LENGTH + "-" + Pokemon.NAME_MAX_LENGTH + " tecken)",
            Pokemon.NAME_MIN_LENGTH, Pokemon.NAME_MAX_LENGTH, NORMALIZE,
            input -> pokemonCollection.stream().anyMatch(pokemon -> pokemon.getName().equalsIgnoreCase(input)),
            reader, "det finns redan en Pokémon med det namnet");
        InternalWorker.work();
        Type type = selectType();
        InternalWorker.work();
        int hp = reader.readInt("Max HP (" + HP_MIN + "-" + HP_MAX + ")", HP_MIN, HP_MAX);
        InternalWorker.work();
        int numberOfAttacks = reader.readInt("Antal attacker (" + ATTACKS_MIN_AMOUNT + "-" + ATTACKS_MAX_AMOUNT + ")",
            ATTACKS_MIN_AMOUNT, ATTACKS_MAX_AMOUNT);
        List<Attack> attacks = new ArrayList<>();
        for (int i = 1; i <= numberOfAttacks; i++) {
            InternalWorker.work();
            println("• Attack" + (numberOfAttacks > 1 ? " " + i : ""));
            Attack attack = createAttack(attacks);
            if (!attacks.contains(attack)) {
                attacks.add(attack);
            } else {
                InternalWorker.work();
                println(PRINT_PREFIX + "'" + name + "' har redan denna attack");
                i--;
            }
        }
        InternalWorker.work();
        Pokemon pokemon = new Pokemon(name, type, hp, attacks);
        pokemon.printPokemon();
        if (reader.askYesNoQuestion("Spara '" + pokemon.getName() + "'? (j/n)")) {
            pokemonCollection.add(pokemon);
            pokemonCollection.sort(Comparator.comparing(Pokemon::getName, SWE_COLLATOR));
            println();
            printPokemonMessage(pokemon, PokemonResponseCreator.getCreatedResponse());
            InternalWorker.work(0, 5);
        }
    }

    /**
     * Lets the user edit one of its Pokémon.
     */
    private void editPokemon() {
        println();
        InternalWorker.work();
        if (pokemonCollection.isEmpty()) {
            println(PRINT_PREFIX + "det finns ingen Pokémon att redigera");
            return;
        }
        println(PRINT_PREFIX + "Redigera");
        printListNumbered(pokemonCollection, Pokemon::getName, 0);
        int selected = reader.readInt("", 1, pokemonCollection.size()) - 1;
        Pokemon pokemon = pokemonCollection.get(selected);
        while (true) {
            println();
            InternalWorker.work();
            println("""
                 - %s -
                1. Ändra typ
                2. Ändra max HP
                3. Hantera attacker
                4. <- Tillbaka\
                """.formatted(pokemon.getName()));
            selected = reader.readInt("", 1, 4);
            try {
                switch (selected) {
                    case 1 -> {
                        println();
                        InternalWorker.work();
                        println("• Nuvarande typ: " + pokemon.getType());
                        pokemon.setType(selectType());
                    }
                    case 2 -> {
                        println();
                        InternalWorker.work();
                        println("• Nuvarande max HP: " + pokemon.getMaxHp());
                        pokemon.setMaxHp(reader.readInt("Max HP (" + HP_MIN + "-" + HP_MAX + ")", HP_MIN, HP_MAX));
                    }
                    case 3 -> {
                        manageAttacks(pokemon);
                    }
                    case 4 -> {
                        return;
                    }
                }
            } catch (EnterAllowedException e) {
                // the user chose to go back to this menu by pressing 'Enter'
            }
        }
    }

    /**
     * Lets the user select one of the types a Pokémon or an attack can have.
     * @return the selected type
     */
    private Type selectType() {
        printListNumbered(TYPES, Type::toString, 0);
        int selected = reader.readInt("Typ", 1, TYPES.size()) - 1;
        return TYPES.get(selected);
    }

    /**
     * Lets the user create an attack. If the attack being created already is in the list passed here, then the user
     * won't be able to set the stats.
     * @param attacks a list of attacks, can be null
     * @return the created attack
     */
    private Attack createAttack(List<Attack> attacks) {
        String name = reader.readNormalizedString(
            "Namn (" + Attack.NAME_MIN_LENGTH + "-" + Attack.NAME_MAX_LENGTH + " tecken)",
            Attack.NAME_MIN_LENGTH, Attack.NAME_MAX_LENGTH);
        InternalWorker.work();
        Type type = selectType();
        Attack attack = new Attack(name, type, BASE_DAMAGE_MIN, ACCURACY_MIN);
        if (attacks != null && attacks.contains(attack)) {
            return attack;
        }
        InternalWorker.work();
        int baseDamage = reader.readInt("Basskada (" + BASE_DAMAGE_MIN + "-" + BASE_DAMAGE_MAX + ")",
            BASE_DAMAGE_MIN, BASE_DAMAGE_MAX);
        InternalWorker.work();
        int accuracy = reader.readInt("Träffsäkerhet (" + ACCURACY_MIN + "-" + ACCURACY_MAX + ")",
            ACCURACY_MIN, ACCURACY_MAX);
        return attack.withStats(baseDamage, accuracy);
    }

    /**
     * Lets the user manage attacks for one of its Pokémon.
     * @param pokemon the Pokémon to manage attacks for
     */
    private void manageAttacks(Pokemon pokemon) {
        while (true) {
            println();
            InternalWorker.work();
            println("""
                 - %s -
                   • Attacker\
                """.formatted(pokemon.getName()));
            printListNumbered(pokemon.getAttacks(), Attack::getDescription, 3);
            println("""
                1. Lägg till
                2. Ta bort
                3. Redigera
                4. <- Tillbaka\
                """);
            int selected = reader.readInt("", 1, 4);
            try {
                switch (selected) {
                    case 1 -> {
                        InternalWorker.work();
                        addAttack(pokemon);
                    }
                    case 2 -> {
                        InternalWorker.work();
                        removeAttack(pokemon);
                    }
                    case 3 -> {
                        InternalWorker.work();
                        editAttack(pokemon);
                    }
                    case 4 -> {
                        return;
                    }
                }
            } catch (EnterAllowedException e) {
                // the user chose to go back to this menu by pressing 'Enter'
            }
        }
    }

    /**
     * Lets the user add an attack to one of its Pokémon, if possible.
     * @param pokemon the Pokémon to add an attack to
     */
    private void addAttack(Pokemon pokemon) {
        List<Attack> attacks = pokemon.getAttacks();
        if (attacks.size() == ATTACKS_MAX_AMOUNT) {
            println();
            println(PRINT_PREFIX + "'" + pokemon.getName() + "' har max antal tillåtna attacker");
            return;
        }
        Attack attack = createAttack(attacks);
        if (!attacks.contains(attack)) {
            InternalWorker.work();
            println("• " + attack.getDescription().replace("\n", "\n  "));
            InternalWorker.work();
            if (reader.askYesNoQuestion("Lägg till attack? (j/n)")) {
                pokemon.addOrUpdateAttack(attack);
            }
        } else {
            InternalWorker.work();
            println();
            println(PRINT_PREFIX + "'" + pokemon.getName() + "' har redan denna attack");
        }
    }

    /**
     * Lets the user remove an attack from one of its Pokémon, if possible.
     * @param pokemon the Pokémon to remove an attack from
     */
    private void removeAttack(Pokemon pokemon) {
        List<Attack> attacks = pokemon.getAttacks();
        if (attacks.size() == ATTACKS_MIN_AMOUNT) {
            println();
            println(PRINT_PREFIX + "'" + pokemon.getName() + "' har lägst antal tillåtna attacker");
            return;
        }
        int selected = reader.readInt("Attack att ta bort", 1, attacks.size()) - 1;
        InternalWorker.work();
        if (reader.askYesNoQuestion("Ta bort '" + attacks.get(selected).name() + "'? (j/n)")) {
            pokemon.removeAttack(attacks.get(selected));
        }
    }

    /**
     * Lets the user edit the stats of an attack held by one of its Pokémon.
     * @param pokemon the Pokémon that holds the attack whose stats is to be edited
     */
    private void editAttack(Pokemon pokemon) {
        List<Attack> attacks = pokemon.getAttacks();
        if (attacks.isEmpty()) {
            println();
            println(PRINT_PREFIX + "det finns ingen attack att redigera");
            return;
        }
        int selected = 0;
        if (attacks.size() > 1) {
            selected = reader.readInt("Attack att redigera", 1, attacks.size()) - 1;
            InternalWorker.work();
        }
        Attack attack = attacks.get(selected);
        InternalWorker.work();
        int baseDamage = attack.baseDamage();
        try {
            baseDamage = reader.readInt("Basskada (" + BASE_DAMAGE_MIN + "-" + BASE_DAMAGE_MAX +
                ", 'Enter' = " + attack.baseDamage() + ")", BASE_DAMAGE_MIN, BASE_DAMAGE_MAX);
        } catch (EnterAllowedException e) {
            // current value will be used
        }
        InternalWorker.work();
        int accuracy = attack.accuracy();
        try {
            accuracy = reader.readInt("Träffsäkerhet (" + ACCURACY_MIN + "-" + ACCURACY_MAX +
                ", 'Enter' = " + attack.accuracy() + ")", ACCURACY_MIN, ACCURACY_MAX);
        } catch (EnterAllowedException e) {
            // current value will be used
        }
        pokemon.addOrUpdateAttack(attack.withStats(baseDamage, accuracy));
    }

    /**
     * Lets the user select one of its Pokémon to have it removed.
     */
    private void removePokemon() {
        println();
        InternalWorker.work();
        if (pokemonCollection.isEmpty()) {
            println(PRINT_PREFIX + "det finns ingen Pokémon att radera");
            return;
        }
        println(PRINT_PREFIX + "Radera");
        printListNumbered(pokemonCollection, Pokemon::getName, 0);
        int selected = reader.readInt("", 1, pokemonCollection.size()) - 1;
        InternalWorker.work();
        if (reader.askYesNoQuestion("Radera '" + pokemonCollection.get(selected).getName() + "'? (j/n)")) {
            println();
            printPokemonMessage(pokemonCollection.get(selected), PokemonResponseCreator.getRemovedResponse());
            pokemonCollection.remove(selected);
            InternalWorker.work(0, 5);
        }
    }

    /**
     * Removes all the user's Pokémon.
     */
    private void removeAllPokemon() {
        if (pokemonCollection.isEmpty()) {
            println();
            InternalWorker.work();
            println(PRINT_PREFIX + "det finns inga Pokémon att radera");
            return;
        }
        InternalWorker.work();
        if (!reader.askYesNoQuestion("Radera alla? (j/n)")) {
            return;
        }
        println();
        Collections.shuffle(pokemonCollection);
        for (Pokemon pokemon : pokemonCollection) {
            printPokemonMessage(pokemon, PokemonResponseCreator.getRemovedResponse());
        }
        pokemonCollection.clear();
        InternalWorker.work(0, 5);
    }

    /**
     * Lets the user manage the files of which each contains a collection of Pokémon.
     */
    private void manageCollectionFiles() {
        while (true) {
            println();
            InternalWorker.work();
            List<String> fileNames;
            try (Stream<Path> stream = Files.list(POKEMON_COLLECTIONS_DIR)) {
                fileNames = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(SWE_COLLATOR)
                    .toList();
            } catch (IOException e) {
                println("\33[31m" + PRINT_PREFIX + "kan inte lista sparade filer\33[m");
                return;
            }
            println(PRINT_PREFIX + "Filhantering");
            if (!fileNames.isEmpty()) {
                println("   • Filer");
                printListNumbered(fileNames, 3);
            }
            println("""
                1. Ladda
                2. Spara
                3. Radera
                4. <- Tillbaka\
                """);
            int selected = reader.readInt("", 1, 4);
            try {
                switch (selected) {
                    case 1 -> {
                        InternalWorker.work();
                        loadCollectionFile(fileNames);
                    }
                    case 2 -> {
                        InternalWorker.work();
                        saveCollectionFile(fileNames);
                    }
                    case 3 -> {
                        InternalWorker.work();
                        deleteCollectionFile(fileNames);
                    }
                    case 4 -> {
                        return;
                    }
                }
            } catch (EnterAllowedException e) {
                // the user chose to go back to this menu by pressing 'Enter'
            }
        }
    }

    /**
     * Lets the user load a collection of Pokémon from file.
     * @param fileNames a list of file names of files that each hold a Pokémon collection
     */
    private void loadCollectionFile(List<String> fileNames) {
        if (!fileNames.isEmpty()) {
            int selected = reader.readInt("Välj fil", 1, fileNames.size()) - 1;
            InternalWorker.work();
            if (reader.askYesNoQuestion("Ladda '" + fileNames.get(selected) + "'? (j/n)")) {
                loadCollectionFile(POKEMON_COLLECTIONS_DIR.resolve(fileNames.get(selected)));
            }
        } else  {
            println();
            println(PRINT_PREFIX + "det finns ingen fil att ladda");
        }
    }

    /**
     * Loads a collection of Pokémon from file.
     * @param filePath the path to the file that holds the collection
     * @return true if the collection could be loaded, false otherwise
     */
    private boolean loadCollectionFile(Path filePath) {
        boolean couldLoad = false;
        try {
            List<Pokemon> loaded = loadPokemonCollection(filePath);
            pokemonCollection.clear();
            pokemonCollection.addAll(loaded);
            couldLoad = true;
        } catch (InvalidPokemonDataException e1) {
            println();
            InternalWorker.work();
            println(PRINT_PREFIX + "det gick inte att ladda '" + filePath.getFileName() + "'\n" +
                " ".repeat(PRINT_PREFIX.length()) + "ogiltig data: \33[31m" + e1.getMessage() + "\33[m");
        } catch (PokemonCollectionIOException e2) {
            println();
            InternalWorker.work();
            println(PRINT_PREFIX + "det gick inte att ladda '" + filePath.getFileName() + "'");
        }
        return couldLoad;
    }

    /**
     * Lets the user save a collection of Pokémon to file.
     * @param fileNames a list of file names of files that each hold a Pokémon collection
     */
    private void saveCollectionFile(List<String> fileNames) {
        String fileName = null;
        while (fileName == null) {
            fileName = reader.readNormalizedString(
                "Filnamn (" + FILE_NAME_MIN_LENGTH + "-" + FILE_NAME_MAX_LENGTH + " tecken)",
                FILE_NAME_MIN_LENGTH, FILE_NAME_MAX_LENGTH);
            InternalWorker.work();
            if (fileName.matches(".*[\\\\/\"<>*?:|\\x00-\\x1F].*")) {
                reader.printErrorMessage("ogiltigt filnamn\n'" + fileName + "'");
                fileName = null;
                InternalWorker.work();
            }
            if (fileName != null) {
                boolean fileExists = Files.exists(POKEMON_COLLECTIONS_DIR.resolve(fileName));
                if (reader.askYesNoQuestion((!fileExists ? "Spara '" : "Skriv över '") + fileName + "'? (j/n)")) {
                    try {
                        savePokemonCollection(pokemonCollection.stream().toList(),
                            POKEMON_COLLECTIONS_DIR.resolve(fileName));
                    } catch (PokemonCollectionIOException e) {
                        println();
                        InternalWorker.work();
                        println(PRINT_PREFIX + "det gick inte att spara '" + fileName + "'");
                    }
                }
            }
        }
    }

    /**
     * Lets the user delete a file containing a collection of Pokémon.
     * @param fileNames a list of file names of files that each hold a Pokémon collection
     */
    private void deleteCollectionFile(List<String> fileNames) {
        if (!fileNames.isEmpty()) {
            int selected = reader.readInt("Välj fil", 1, fileNames.size()) - 1;
            InternalWorker.work();
            if (reader.askYesNoQuestion("Radera '" + fileNames.get(selected) + "'? (j/n)")) {
                try {
                    Files.delete(POKEMON_COLLECTIONS_DIR.resolve(fileNames.get(selected)));
                } catch (IOException e) {
                    println();
                    InternalWorker.work();
                    println(PRINT_PREFIX + "det gick inte att radera '" + fileNames.get(selected) + "'");
                }
            }
        } else  {
            println();
            println(PRINT_PREFIX + "det finns ingen fil att radera");
        }
    }

    /**
     * Performs a factory reset of the Pokédex.
     */
    private void reset() {
        println();
        InternalWorker.work();
        installDefaultPokemonCollection();
        println(PRINT_PREFIX + "fabriksåterställning utförd");
    }

    /**
     * Should be called before the Pokédex is turned off.
     */
    private void quit() {
        println();
        InternalWorker.work();
        try {
            savePokemonCollection(pokemonCollection.stream().toList(), LAST_USED_POKEMON_COLLECTION);
        } catch (PokemonCollectionIOException e) {
            println(PRINT_PREFIX + "det gick inte att spara '" + LAST_USED_POKEMON_COLLECTION + "'");
            println();
            InternalWorker.work();
        }
        println(PRINT_PREFIX + "Hörs senare!");
        println();
        InternalWorker.work();
        quit = true;
    }

    /**
     * Prints a message from a Pokémon.
     * @param pokemon the Pokémon that has something to say
     * @param message what the Pokémon has to say
     */
    private void printPokemonMessage(Pokemon pokemon, String message) {
        InternalWorker.work(10, 2);
        println("\33[1;3;32m" + pokemon.getName() + ":\33[m " + message);
    }

    /**
     * Verifies a user.
     * @param username a user's username
     * @param password a user's password
     * @throws UnsuccessfulLoginAttemptException Thrown if a user could not be successfully verified.
     */
    private void verifyUser(String username, String password) throws UnsuccessfulLoginAttemptException {
        boolean validUser = false;
        if (username != null && password != null && !username.equalsIgnoreCase(UNKNOWN_USER)) {
            if (username.length() >= 3 && password.length() >= 5) {
                validUser = true;
            }
        }
        if (!validUser) {
            throw new UnsuccessfulLoginAttemptException("""
                \33[31m\
                Error: User could not be verified.
                       Access to sandbox version allowed for now.
                       Our cyber security team is on the case.\
                \33[m\
                """
            );
        }
    }

    /**
     * Prints a welcome message when the application starts.
     */
    private void printWelcomeMessage() {
        println("""
            Hej, %s!
            Välkommen till %s, version %s
            
            Kom ihåg: Du kan återvända till föregående meny eller
                      avbryta en inmatning genom att trycka 'Enter'\
            """.formatted(username, APPLICATION_NAME, APPLICATION_VERSION)
        );
    }

    /**
     * The Pokédex's main menu items.
     */
    private enum MainMenuItem {
        /*  1. Show a Pokémon. */        SHOW       ("Visa",                 Pokedex::showPokemon),
        /*  2. Show all Pokémon. */      SHOW_ALL   ("Visa alla",            Pokedex::showAllPokemon),
        /*  3. Create a Pokémon. */      CREATE     ("Skapa",                Pokedex::createPokemon),
        /*  4. Edit a Pokémon. */        EDIT       ("Redigera",             Pokedex::editPokemon),
        /*  5. Remove a Pokémon. */      REMOVE     ("Radera",               Pokedex::removePokemon),
        /*  6. Remove all Pokémon. */    REMOVE_ALL ("Radera alla",          Pokedex::removeAllPokemon),
        /*  7. Manage files. */          FILES      ("Hantera filer",        Pokedex::manageCollectionFiles),
        /*  8. Factory reset. */         RESET      ("Fabriksåterställning", Pokedex::reset),
        /*  9. Quit the application. */  QUIT       ("Avsluta",              Pokedex::quit);

        private final String menuText;
        private final Consumer<Pokedex> action;

        MainMenuItem(String menuText, Consumer<Pokedex> action) {
            this.menuText = menuText;
            this.action = action;
        }

        /**
         * @return the text associated with a main menu item
         */
        public String getMenuText() {
            return menuText;
        }

        /**
         * Executes the action associated with a main menu item.
         * @param pokedex the Pokédex object used by the action
         */
        private void execute(Pokedex pokedex) {
            action.accept(pokedex);
        }
    }

    /**
     * Installs the default collection of Pokémon into the Pokédex.
     */
    private void installDefaultPokemonCollection() {
        pokemonCollection.clear();
        pokemonCollection.addAll(List.of(
            // I used AI to generate these
            new Pokemon("Blastoise", Type.WATER, 365, new ArrayList<>(List.of(
                new Attack("Bite", Type.NORMAL, 60, 100),
                new Attack("Hydro Pump", Type.WATER, 110, 80),
                new Attack("Water Gun", Type.WATER, 40, 100)
            ))),
            new Pokemon("Charizard", Type.FIRE, 360, new ArrayList<>(List.of(
                new Attack("Fire Blast", Type.FIRE, 110, 85),
                new Attack("Flamethrower", Type.FIRE, 90, 100),
                new Attack("Scratch", Type.NORMAL, 40, 100),
                new Attack("Slash", Type.NORMAL, 70, 100)
            ))),
            new Pokemon("Jolteon", Type.ELECTRIC, 335, new ArrayList<>(List.of(
                new Attack("Quick Attack", Type.NORMAL, 40, 100),
                new Attack("Thunderbolt", Type.ELECTRIC, 90, 100)
            ))),
            new Pokemon("Lapras", Type.WATER, 465, new ArrayList<>(List.of(
                new Attack("Body Slam", Type.NORMAL, 85, 100),
                new Attack("Water Gun", Type.WATER, 40, 100),
                new Attack("Water Pulse", Type.WATER, 60, 100)
            ))),
            new Pokemon("Pikachu", Type.ELECTRIC, 275, new ArrayList<>(List.of(
                new Attack("Electro Ball", Type.ELECTRIC, 60, 100),
                new Attack("Feint", Type.NORMAL, 30, 100),
                new Attack("Spark", Type.ELECTRIC, 65, 100),
                new Attack("Thunderbolt", Type.ELECTRIC, 90, 100)
            ))),
            new Pokemon("Tyranitar", Type.FIRE, 405, new ArrayList<>(List.of(
                new Attack("Hyper Beam", Type.NORMAL, 150, 90)
            ))),
            new Pokemon("Venusaur", Type.GRASS, 350, new ArrayList<>(List.of(
                new Attack("Petal Blizzard", Type.GRASS, 90, 100),
                new Attack("Razor Leaf", Type.GRASS, 55, 95),
                new Attack("Solar Beam", Type.GRASS, 120, 100)
            )))
        ));
    }

    /**
     * Performs advanced internal work.
     */
    private static class InternalWorker {
        private static final Random random = new Random();

        /**
         * Performs work that is on the stack.
         */
        private static void work() {
            work(0, 1);
        }

        /**
         * @param work the work to perform, should be from 0 to 10 (both inclusive)
         * @param resources the resources needed to perform the work, should be from 0 to 5 (both inclusive).
         */
        private static void work(int work, int resources) {
            try {
                Thread.sleep(random.nextInt(Math.clamp(++work, 1, 11)) * 100L + Math.clamp(++resources, 1, 6) * 250L);
            } catch (InterruptedException e) {
                // do nothing
                // in a "real" program we would obviously do something like Thread.currentThread().interrupt() etc.
            }
        }
    }
}
