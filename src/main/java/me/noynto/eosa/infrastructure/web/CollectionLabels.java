package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.collection.CollectionSeason;
import me.noynto.eosa.collection.CollectionState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Libellés français des saisons et des états d'une collection, pour les templates de l'administration. */
final class CollectionLabels {

    private CollectionLabels() {
    }

    static String season(CollectionSeason season) {
        return switch (season) {
            case SPRING -> "Printemps";
            case SUMMER -> "Été";
            case AUTUMN -> "Automne";
            case WINTER -> "Hiver";
        };
    }

    static String state(CollectionState state) {
        return switch (state) {
            case DRAFTED -> "Brouillon";
            case PUBLISHED -> "Publiée";
            case ARCHIVED -> "Archivée";
        };
    }

    /** Les options d'une liste de saisons, celle de {@code selected} (peut être nul) marquée. */
    static List<Map<String, Object>> seasonOptions(CollectionSeason selected) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (CollectionSeason season : CollectionSeason.values()) {
            Map<String, Object> option = new HashMap<>();
            option.put("value", season.name());
            option.put("label", season(season));
            option.put("selected", season == selected);
            options.add(option);
        }
        return options;
    }

    /** La saison d'un champ de formulaire ; une erreur lisible quand la valeur manque ou est inconnue. */
    static CollectionSeason parseSeason(String value) {
        try {
            return CollectionSeason.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("La saison de la collection est requise.");
        }
    }

    /** L'année d'un champ de formulaire ; une erreur lisible quand la valeur manque ou n'est pas un nombre. */
    static int parseYear(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("L'année de la collection est requise.");
        }
    }

}
