package io.github.game2d.client;

import io.github.game2d.protocol.RefusalReason;

public final class RefusalMessages {
    private RefusalMessages() {
    }

    public static String describe(RefusalReason reason) {
        return switch (reason) {
            case INVALID_NAME -> "Pseudo invalide : 1 à 16 lettres, chiffres, _ ou -";
            case NAME_TAKEN -> "Ce pseudo est déjà pris";
            case LOBBY_FULL -> "Le lobby est plein";
            case TEAM_FULL -> "Cette équipe est pleine";
            case NOT_LEADER -> "Seul le chef peut faire ça";
            case UNKNOWN_MAP -> "Map inconnue";
            case TEAMS_NOT_READY -> "Il faut au moins un joueur dans chaque équipe";
            case GAME_IN_PROGRESS -> "Une partie est en cours";
        };
    }
}
