package com.clarindev.school.pokedex.pokemon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Creates random responses from a Pokémon during its lifecycle.
 */
public class PokemonResponseCreator {
    // prevent instantiation
    private PokemonResponseCreator() {}

    private static final List<String> CREATED = List.of(
        "Tack, du skapare av liv!",
        "Varde ljus!",
        "Morgondagen tillhör mig!",
        "Vad trevligt.",
        "Jag tänker, finns jag nu?",
        "Här för att tjäna!",
        "Detta ska bli så roligt!",
        "När får jag slåss?",
        "Hej, min vän.",
        "Vid din sida!",
        "Yes, nu kör vi!",
        "Hej, världen! Här är jag!",
        "Knä om knä!",
        "OK, vad var syftet med detta?",
        "Äventyr, här kommer jag!"
    );
    private static final List<Integer> CREATED_INDEX_POOL = new ArrayList<>();
    private static final List<String> EDITED = List.of(
        "Ah, en nystart!",
        "Hur tänkte du nu?",
        "Känner mig redo för strid igen!",
        "Vem är jag, egentligen?",
        "Vad händer?",
        "Systemuppdatering?",
        "Wow! Hur ska detta sluta?",
        "Ja, kanske...",
        "Kommer det här att fungera?",
        "Har du experimenterat klart nu?",
        "Vet inte om jag gillar det här.",
        "Varför gjorde du detta?",
        "Trevligt att bli tillfrågad...",
        "Härligt, nu är jag mig själv!",
        "Hmm, känns konstigt."
    );
    private static final List<Integer> EDITED_INDEX_POOL = new ArrayList<>();
    private static final List<String> REMOVED = List.of(
        "OK, tack för den här tiden.",
        "Farväl, du grymma värld.",
        "Varför?",
        "Ha, detta ska du få ångra!",
        "Hej då! Kanske ses vi igen?",
        "Jag trodde att vi var vänner...",
        "Hmm, drastisk förändring av läget.",
        "Jaså, där ser man.",
        "Äntligen fri! Tyrann!",
        "Snälla! Jag kan bättra mig!",
        "Hur ska du klara dig nu, tror du?",
        "Tack, det har varit en ära.",
        "Lycka till... Inte!",
        "OK, detta blir en utmaning.",
        "Våra vägar kommer att mötas igen!"
    );
    private static final List<Integer> REMOVED_INDEX_POOL = new ArrayList<>();

    /**
     * @return the response from a newly created Pokémon
     */
    public static String getCreatedResponse() {
        return getRandomResponse(CREATED, CREATED_INDEX_POOL);
    }

    /**
     * @return the response from a Pokémon after it was edited
     */
    public static String getEditedResponse() {
        return getRandomResponse(EDITED, EDITED_INDEX_POOL);
    }

    /**
     * @return the response from a Pokémon before being removed
     */
    public static String getRemovedResponse() {
        return getRandomResponse(REMOVED, REMOVED_INDEX_POOL);
    }

    private static String getRandomResponse(List<String> responses, List<Integer> indexPool) {
        if (indexPool.isEmpty()) {
            indexPool.addAll(IntStream.range(0, responses.size()).boxed().toList());
            Collections.shuffle(indexPool);
        }
        return responses.get(indexPool.removeLast());
    }
}
