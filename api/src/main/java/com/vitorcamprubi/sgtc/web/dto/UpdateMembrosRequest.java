package com.vitorcamprubi.sgtc.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateMembrosRequest {
    @NotNull
    private List<Long> alunosIds;

    public List<Long> getAlunosIds() {
        return alunosIds;
    }

    public void setAlunosIds(List<Long> alunosIds) {
        this.alunosIds = alunosIds;
    }
}
