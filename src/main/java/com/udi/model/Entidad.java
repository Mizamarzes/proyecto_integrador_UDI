package com.udi.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Base de todas las entidades: id generado por secuencia y fecha de creación. */
public abstract class Entidad {

    private Long id;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Dos entidades son iguales si son de la misma clase y tienen el mismo id. */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass() || id == null) {
            return false;
        }
        return id.equals(((Entidad) otro).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
