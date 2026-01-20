CREATE TABLE notifications (
  id UUID PRIMARY KEY NOT NULL,
  user_id UUID NOT NULL,
  recipient_role varchar(30) NOT NULL CHECK (recipient_role IN ('WORKER', 'CLIENT', 'ADMIN')),
  title VARCHAR(255) NOT NULL,
  body TEXT,
  "type" VARCHAR(255) NOT NULL,
  read BOOLEAN DEFAULT FALSE,
  archived BOOLEAN DEFAULT FALSE,
  click_action TEXT,
  created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
  metadata jsonb DEFAULT '{}'::jsonb
);

CREATE INDEX inx_notifications_userid_recipient_createdat ON notifications(user_id, recipient_role, created_at DESC);
