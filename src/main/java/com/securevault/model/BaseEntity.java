package com.securevault.model;

import com.securevault.exception.ValidationException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Parent class of every stored object.
 * Holds what all entities share: a unique id and creation/update times.
 * Subclasses inherit these fields and the helper methods below.
 */
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Creates a brand-new entity with a random id. */
    protected BaseEntity() {
        this(UUID.randomUUID().toString(), LocalDateTime.now());
    }

    /** Re-creates an existing entity (for example when editing it). */
    protected BaseEntity(String id, LocalDateTime createdAt) {
        this.id = requireText(id, "Id");
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Marks the entity as changed right now. */
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Every entity must say how it is shown to the user. */
    public abstract String getDisplayName();

    /** Shared validation: the value must not be null or blank. */
    protected static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        return value.trim();
    }

    /** Two entities are the same if they have the same id. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((BaseEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
