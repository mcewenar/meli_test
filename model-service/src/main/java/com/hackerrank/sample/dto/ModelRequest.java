package com.hackerrank.sample.dto;

import com.hackerrank.sample.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ModelRequest {
    @NotNull(message = "id is required", groups = ValidationGroups.Create.class)
    private Long id;

    @NotBlank(message = "name is required", groups = ValidationGroups.Create.class)
    private String name;

    public ModelRequest() {
    }

    public ModelRequest(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
