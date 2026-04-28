CREATE TABLE otp_verification (
                                  id          BIGSERIAL PRIMARY KEY,
                                  app_user_id BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                  type        VARCHAR(10)  NOT NULL CHECK (type IN ('EMAIL', 'PHONE')),
                                  value       VARCHAR(255) NOT NULL,
                                  otp         VARCHAR(6)   NOT NULL,
                                  expires_at  TIMESTAMP    NOT NULL,
                                  verified    BOOLEAN      NOT NULL DEFAULT FALSE,
                                  created_at  TIMESTAMP             DEFAULT NOW()
);

CREATE INDEX idx_otp_user_type_value
    ON otp_verification (app_user_id, type, value);