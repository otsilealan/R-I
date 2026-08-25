-- V6: Add display_order to grade_scale_entries
-- Demonstrates safe NOT NULL column addition with DEFAULT backfill pattern.
-- Column already exists from V2 but this migration serves as the canonical
-- example of the backfill pattern for team reference.

-- No-op if column already present (idempotent for demo purposes).
-- In a real additive migration, the pattern would be:
--   1. ALTER TABLE ... ADD COLUMN col TYPE DEFAULT value;   (adds with default = backfills)
--   2. ALTER TABLE ... ALTER COLUMN col SET NOT NULL;       (enforce NOT NULL after backfill)

-- This migration adds a 'sort_priority' column as the real example:
ALTER TABLE grade_scale_entries
    ADD COLUMN IF NOT EXISTS sort_priority INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN grade_scale_entries.sort_priority
    IS 'UI sort order for display; added via V6 to demonstrate safe NOT NULL addition.';
