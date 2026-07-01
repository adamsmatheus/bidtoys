-- Renomeia phone_numbers duplicados (mantém o original no registro mais antigo)
-- Registros novos com número repetido recebem sufixo _dup_N
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY phone_number ORDER BY created_at ASC) AS rn
    FROM users
)
UPDATE users u
SET phone_number = u.phone_number || '_dup_' || (r.rn - 1)
FROM ranked r
WHERE u.id = r.id AND r.rn > 1;

ALTER TABLE users ADD CONSTRAINT uq_users_phone_number UNIQUE (phone_number);
