package com.genai.posture.dataaccess.entity;

import com.genai.posture.dataaccess.entity.converter.MapJsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "security_posture")
@IdClass(SecurityPostureId.class)
public class SecurityPostureEntity {

  @Id
  @Column(name = "service_id", nullable = false, length = 80)
  private String serviceId;

  @Id
  @Column(name = "environment", nullable = false, length = 16)
  private String environment; // dev|stage|prod

  @Column(name = "internet_facing", nullable = false)
  private boolean internetFacing;

  @Column(name = "authn", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> authn;

  @Column(name = "data", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> data;

  @Column(name = "tls", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> tls;

  @Column(name = "network", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> network;

  @Column(name = "secrets", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> secrets;

  @Column(name = "vulns", columnDefinition = "jsonb")
  @Convert(converter = MapJsonbConverter.class)
  private Map<String,Object> vulns;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

}