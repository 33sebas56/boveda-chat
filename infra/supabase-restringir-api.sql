-- Ejecutar en SQL Editor DESPUES del primer arranque y de crear el esquema.
-- Solo afecta boveda_chat. No toca tablas ni buckets de tus otras aplicaciones.
revoke all on schema boveda_chat from public, anon, authenticated;
revoke all on all tables in schema boveda_chat from public, anon, authenticated;
revoke all on all sequences in schema boveda_chat from public, anon, authenticated;
-- No agregues boveda_chat a la lista de esquemas expuestos por la Data API.
-- El backend accede por JDBC; los navegadores nunca necesitan acceso SQL directo.
