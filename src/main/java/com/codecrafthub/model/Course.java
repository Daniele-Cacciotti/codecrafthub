package com.codecrafthub.model;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

// A record holds the course data and supplies a constructor and accessors.
public record Course(
        long id,
        String name,
        String description,
        @JsonProperty("target_date") LocalDate targetDate,
        Status status,
        @JsonProperty("created_at") Instant createdAt) {

    public enum Status {
        NOT_STARTED("Not Started"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        // Jackson writes the human-readable label to JSON.
        @JsonValue
        public String label() {
            return label;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Status fromLabel(String label) {
            for (Status status : values()) {
                if (status.label.equals(label)) {
                    return status;
                }
            }
            throw new IllegalArgumentException(
                    "status must be one of: Not Started, In Progress, Completed");
        }
    }
}
