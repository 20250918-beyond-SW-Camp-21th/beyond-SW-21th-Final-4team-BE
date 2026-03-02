-- Review tables: migrate deleted(boolean) -> status(varchar), then drop deleted column

ALTER TABLE employer_freelancer_reviews
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE employer_freelancer_reviews
SET status = CASE
    WHEN deleted = 1 THEN 'DELETED'
    ELSE 'ACTIVE'
END
WHERE status IS NULL OR status = '' OR status = 'ACTIVE';

ALTER TABLE employer_freelancer_reviews
    DROP COLUMN IF EXISTS deleted;

ALTER TABLE freelancer_employer_reviews
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE freelancer_employer_reviews
SET status = CASE
    WHEN deleted = 1 THEN 'DELETED'
    ELSE 'ACTIVE'
END
WHERE status IS NULL OR status = '' OR status = 'ACTIVE';

ALTER TABLE freelancer_employer_reviews
    DROP COLUMN IF EXISTS deleted;
