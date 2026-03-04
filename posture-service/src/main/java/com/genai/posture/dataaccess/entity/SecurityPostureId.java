package com.genai.posture.dataaccess.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityPostureId implements Serializable {
    private String serviceId;
    private String environment;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityPostureId id)) return false;
        return Objects.equals(serviceId, id.serviceId) && Objects.equals(environment, id.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, environment);
    }
}