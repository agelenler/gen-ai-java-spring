CREATE TABLE IF NOT EXISTS security_posture (
  service_id       varchar(80)   NOT NULL,
  environment      varchar(16)   NOT NULL,
  internet_facing  boolean       NOT NULL DEFAULT false,
  authn            jsonb,
  data             jsonb,
  tls              jsonb,
  network          jsonb,
  secrets          jsonb,
  vulns            jsonb,
  updated_at       timestamptz   NOT NULL DEFAULT now(),
  PRIMARY KEY (service_id, environment)
);
