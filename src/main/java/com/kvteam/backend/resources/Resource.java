package com.kvteam.backend.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Resource {

    private String type;

    public Resource() {
    }

    public Resource(@JsonProperty("type") String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
