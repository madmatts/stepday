-- Web Push (dokument: szczegóły push poza zakresem; decyzja A29 w docs/decisions.md).

-- Subskrypcje przeglądarek (PWA zainstalowana na ekranie początkowym).
CREATE TABLE push_subscription (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  endpoint   text NOT NULL UNIQUE,
  p256dh     text NOT NULL,
  auth       text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- Jedno przypomnienie na blok i dzień lokalny (chroni przed duplikatami przy restarcie i wielu instancjach).
CREATE TABLE reminder_log (
  template_id uuid NOT NULL REFERENCES block_template(id) ON DELETE CASCADE,
  local_date  date NOT NULL,
  sent_at     timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (template_id, local_date)
);

-- Ustawienia aplikacji (klucze VAPID generowane przy pierwszym starcie).
CREATE TABLE app_setting (
  key   text PRIMARY KEY,
  value text NOT NULL
);
