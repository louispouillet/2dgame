package io.github.game2d.protocol;

import io.github.game2d.model.Team;

/**
 * Client request: join this team.
 *
 * @param team the chosen team
 */
public record ChooseTeam(Team team) {}
