package com.trucdnd.gpu_hub_backend.common.constants;

public class Team {
    private Team() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public enum TeamRole {
        MEMBER,
        TEAM_LEAD
    }
}
