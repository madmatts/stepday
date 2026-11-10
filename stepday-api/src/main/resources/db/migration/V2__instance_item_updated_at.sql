-- Założenie A3 (docs/decisions.md): pozycje listy scalane zasadą „ostatni zapis wygrywa” po updated_at.
ALTER TABLE instance_item ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();
