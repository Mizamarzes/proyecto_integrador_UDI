package com.udi.model;

import java.time.LocalDateTime;

/** Entidad con auditoría completa: fecha de modificación y usuario creador. */
public abstract class EntidadAuditable extends Entidad {

    private LocalDateTime updatedAt;
    private Long createdBy;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
