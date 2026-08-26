package com.nhnacademy.core.repository.team;

public interface TeamIdProjection {

    TeamProjection getTeam();

    interface TeamProjection {

        Long getId();
    }
}
