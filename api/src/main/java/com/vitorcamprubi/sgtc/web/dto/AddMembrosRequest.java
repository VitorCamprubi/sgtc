package com.vitorcamprubi.sgtc.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class AddMembrosRequest {
    @NotEmpty
    private List<Long> alunosIds;

    public List<Long> getAlunosIds() { return alunosIds; }
    public void setAlunosIds(List<Long> alunosIds) { this.alunosIds = alunosIds; }
}
