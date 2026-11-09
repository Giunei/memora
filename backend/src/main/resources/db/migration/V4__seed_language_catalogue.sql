-- ---------------------------------------------------------------------------
-- Reference data: the catalogue of languages a user can enrol in.
-- This is platform configuration, not user content.
-- ---------------------------------------------------------------------------

INSERT INTO languages (id, code, name, native_name, flag_emoji, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'en', 'English',    'English',   '🇬🇧', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'es', 'Spanish',    'Español',   '🇪🇸', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'fr', 'French',     'Français',  '🇫🇷', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'de', 'German',     'Deutsch',   '🇩🇪', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'it', 'Italian',    'Italiano',  '🇮🇹', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'pt', 'Portuguese', 'Português', '🇵🇹', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'ja', 'Japanese',   '日本語',       '🇯🇵', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'ko', 'Korean',     '한국어',       '🇰🇷', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'zh', 'Chinese',    '中文',        '🇨🇳', TRUE, NOW(), NOW()),
       (gen_random_uuid(), 'ru', 'Russian',    'Русский',   '🇷🇺', TRUE, NOW(), NOW());
