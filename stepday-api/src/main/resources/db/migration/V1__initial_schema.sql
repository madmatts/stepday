-- Schemat z dokumentu „Stepday: warstwa danych”, sekcja 4 (bez zmian).
CREATE TYPE block_type     AS ENUM ('LIST','BOOLEAN','COUNTER','TIMER','NOTE');
CREATE TYPE block_role     AS ENUM ('STEP','ALL_DAY');
CREATE TYPE instance_state AS ENUM ('PENDING','DONE','SKIPPED','CARRIED','MISSED');
CREATE TYPE day_status     AS ENUM ('OPEN','FULL','PARTIAL','MISSED');
CREATE TYPE overdue_policy AS ENUM ('CARRY_OVER','DROP');

CREATE TABLE app_user (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  timezone   text NOT NULL DEFAULT 'Europe/Warsaw',
  day_start  time NOT NULL DEFAULT '04:00',   -- granica dnia w strefie użytkownika
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE block_template (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name           text NOT NULL,
  type           block_type NOT NULL,
  role           block_role NOT NULL DEFAULT 'STEP',
  -- COUNTER: {"target":8,"unit":"szklanek","step":1,"threshold_pct":80}
  -- TIMER:   {"minutes":20}      pozostałe typy: {}
  config         jsonb NOT NULL DEFAULT '{}',
  overdue_policy overdue_policy NOT NULL DEFAULT 'DROP',
  sort_key       text NOT NULL,                -- indeks ułamkowy: kolejność w ścieżce
  reminder_time  time,
  archived_at    timestamptz,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT counter_has_target
    CHECK (type <> 'COUNTER' OR (config ? 'target')),
  CONSTRAINT timer_has_minutes
    CHECK (type <> 'TIMER' OR (config ? 'minutes'))
);
CREATE INDEX block_template_user_idx
  ON block_template (user_id, role, sort_key) WHERE archived_at IS NULL;

CREATE TABLE template_item (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  template_id uuid NOT NULL REFERENCES block_template(id) ON DELETE CASCADE,
  label       text NOT NULL,
  sort_key    text NOT NULL,
  archived_at timestamptz
);

CREATE TABLE schedule_rule (
  template_id uuid PRIMARY KEY REFERENCES block_template(id) ON DELETE CASCADE,
  dtstart     date NOT NULL,
  rrule       text NOT NULL,   -- np. 'FREQ=DAILY' | 'FREQ=WEEKLY;BYDAY=MO,WE,FR'
                               --     'FREQ=MONTHLY;INTERVAL=3' | 'FREQ=DAILY;COUNT=1' (jednorazowo)
  until       date
);

CREATE TABLE day (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  local_date date NOT NULL,
  status     day_status NOT NULL DEFAULT 'OPEN',
  closed_at  timestamptz,
  UNIQUE (user_id, local_date)
);

CREATE TABLE block_instance (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  day_id            uuid NOT NULL REFERENCES day(id) ON DELETE CASCADE,
  template_id       uuid REFERENCES block_template(id) ON DELETE SET NULL,
  carried_from_id   uuid REFERENCES block_instance(id) ON DELETE SET NULL,
  carry_count       smallint NOT NULL DEFAULT 0 CHECK (carry_count BETWEEN 0 AND 2),  -- ile razy blok już przeniesiono
  -- kopia definicji z chwili generowania dnia (historia jest zamrożona)
  name              text NOT NULL,
  type              block_type NOT NULL,
  role              block_role NOT NULL,
  config            jsonb NOT NULL,
  sort_key          text NOT NULL,
  -- stan
  state             instance_state NOT NULL DEFAULT 'PENDING',
  deferred_at       timestamptz,                 -- „Później”: wraca na koniec ścieżki
  value_int         integer NOT NULL DEFAULT 0,  -- COUNTER: suma counter_event
  note_text         text,
  timer_started_at  timestamptz,
  timer_stopped_at  timestamptz,
  completed_at      timestamptz,
  updated_at        timestamptz NOT NULL DEFAULT now()
);
-- generowanie dnia jest idempotentne: jedna zwykła instancja szablonu na dzień
CREATE UNIQUE INDEX block_instance_once_per_day
  ON block_instance (day_id, template_id) WHERE carried_from_id IS NULL;
CREATE INDEX block_instance_day_idx ON block_instance (day_id, role, sort_key);

CREATE TABLE instance_item (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id uuid NOT NULL REFERENCES block_instance(id) ON DELETE CASCADE,
  label       text NOT NULL,
  sort_key    text NOT NULL,
  checked_at  timestamptz
);

CREATE TABLE counter_event (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  instance_id     uuid NOT NULL REFERENCES block_instance(id) ON DELETE CASCADE,
  delta           integer NOT NULL,
  client_event_id uuid NOT NULL,                 -- klucz idempotencji z PWA
  happened_at     timestamptz NOT NULL,
  UNIQUE (instance_id, client_event_id)
);

CREATE INDEX day_user_date_idx ON day (user_id, local_date DESC);
