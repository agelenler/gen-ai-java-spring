TRUNCATE TABLE security_posture;

-- order (internet-facing; HTTP endpoint; coordinates workflow; reads materialized view; Kafka in/out)
INSERT INTO security_posture (
  service_id, environment, internet_facing, authn, data, tls, network, secrets, vulns
) VALUES (
  'order-service', 'prod', true,
  '{
    "type": "oauth2",
    "flows": ["client_credentials"],
    "scopes": ["order:write","order:read"]
  }',
  '{
    "classes": ["PII","OrderData"],
    "retentionDays": 365,
    "notes": "Reads customer data via materialized view"
  }',
  '{
    "minVersion": "TLS1.2",
    "mtlsInternal": false,
    "certDaysLeft": 23
  }',
  '{
    "ingress": [
      {"from":"internet","proto":"HTTPS","port":443}
    ],
    "egressAllowList": [
      "order-db:5432",
      "kafka:9092",
      "customer-db:5432"
    ],
    "topics": {
      "publish": ["order.created","restaurant.request","shipment.request"],
      "subscribe": ["payment.result","restaurant.result","shipment.result"]
    }
  }',
  '{
    "store": "vault",
    "lastRotationDays": 110
  }',
  '{
    "critical": 0,
    "high": 2,
    "oldestOpenDays": 37
  }'
);

-- PAYMENT (internal; consumes Kafka; writes DB; publishes result)
INSERT INTO security_posture (
  service_id, environment, internet_facing, authn, data, tls, network, secrets, vulns
) VALUES (
  'payment-service', 'prod', false,
  '{
    "type": "service",
    "mechanism": "mTLS + Kafka SASL"
  }',
  '{
    "classes": ["Payments"],
    "retentionDays": 730,
    "notes": "No PAN stored; tokenized payments only"
  }',
  '{
    "minVersion": "TLS1.2",
    "mtlsInternal": true,
    "certDaysLeft": 58
  }',
  '{
    "ingress": [
      {"from":"kafka","proto":"SASL_SSL","port":9092}
    ],
    "egressAllowList": [
      "payment-db:5432",
      "kafka:9092",
      "psp-gateway.example.com:443"
    ],
    "topics": {
      "subscribe": ["order.created"],
      "publish": ["payment.result"]
    }
  }',
  '{
    "store": "vault",
    "lastRotationDays": 45
  }',
  '{
    "critical": 0,
    "high": 0,
    "oldestOpenDays": 0
  }'
);

-- RESTAURANT (internal; consumes request, publishes approval)
INSERT INTO security_posture (
  service_id, environment, internet_facing, authn, data, tls, network, secrets, vulns
) VALUES (
  'restaurant-service', 'prod', false,
  '{
    "type": "service",
    "mechanism": "mTLS + Kafka ACLs"
  }',
  '{
    "classes": ["OrderRouting"],
    "retentionDays": 180
  }',
  '{
    "minVersion": "TLS1.2",
    "mtlsInternal": true,
    "certDaysLeft": 90
  }',
  '{
    "ingress": [
      {"from":"kafka","proto":"SASL_SSL","port":9092}
    ],
    "egressAllowList": [
      "kafka:9092",
      "restaurant-db:5432"
    ],
    "topics": {
      "subscribe": ["restaurant.request"],
      "publish": ["restaurant.result"]
    }
  }',
  '{
    "store": "vault",
    "lastRotationDays": 70
  }',
  '{
    "critical": 0,
    "high": 1,
    "oldestOpenDays": 12
  }'
);

-- CUSTOMER (HTTP endpoint to update customer data; populates materialized view; likely internal)
INSERT INTO security_posture (
  service_id, environment, internet_facing, authn, data, tls, network, secrets, vulns
) VALUES (
  'customer-service', 'prod', false,
  '{
    "type": "oauth2",
    "flows": ["client_credentials"],
    "scopes": ["customers:write","customers:read"]
  }',
  '{
    "classes": ["PII"],
    "retentionDays": 1095
  }',
  '{
    "minVersion": "TLS1.2",
    "mtlsInternal": true,
    "certDaysLeft": 31
  }',
  '{
    "ingress": [
      {"from":"order-api","proto":"HTTPS","port":8443}
    ],
    "egressAllowList": [
      "customer-db:5432",
      "kafka:9092"
    ],
    "notes": "Maintains materialized view used by order-api"
  }',
  '{
    "store": "vault",
    "lastRotationDays": 120
  }',
  '{
    "critical": 0,
    "high": 1,
    "oldestOpenDays": 21
  }'
);

-- SHIPMENT (internal; consumes result, publishes final)
INSERT INTO security_posture (
  service_id, environment, internet_facing, authn, data, tls, network, secrets, vulns
) VALUES (
  'shipment-service', 'prod', false,
  '{
    "type": "service",
    "mechanism": "mTLS + Kafka ACLs"
  }',
  '{
    "classes": ["PII","Logistics"],
    "retentionDays": 365
  }',
  '{
    "minVersion": "TLS1.2",
    "mtlsInternal": true,
    "certDaysLeft": 76
  }',
  '{
    "ingress": [
      {"from":"kafka","proto":"SASL_SSL","port":9092}
    ],
    "egressAllowList": [
      "carrier-api.example.com:443",
      "kafka:9092",
      "shipment-db:5432"
    ],
    "topics": {
      "subscribe": ["shipment.request"],
      "publish": ["shipment.result"]
    }
  }',
  '{
    "store": "vault",
    "lastRotationDays": 84
  }',
  '{
    "critical": 0,
    "high": 0,
    "oldestOpenDays": 0
  }'
);
