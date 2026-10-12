-- =====================================================================
-- CRUD de la tabla USUARIO (Oracle 10g XE)
-- =====================================================================
-- Ejecutar en sqlplus dentro del contenedor, siempre con NLS_LANG
-- (sin él las tildes se guardan como "??"):
--
--   docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<contrasena>
--
-- Los ids de ejemplo (usuario 5, roles 1-3) son los de los datos de prueba
-- de esquema_oracle10g.sql; ajústelos a los suyos.
-- Si se equivoca antes del COMMIT, ROLLBACK; deshace los cambios.
-- =====================================================================


-- ---------------------------------------------------------------------
-- C - Crear (INSERT)
-- ---------------------------------------------------------------------
-- El esquema no tiene triggers: el id sale de seq_usuario.NEXTVAL.
-- created_at se llena solo (DEFAULT SYSTIMESTAMP).
-- La contraseña es el hash SHA-256 en hexadecimal. Oracle 10g no tiene
-- STANDARD_HASH, así que la app lo calcula en Java. A mano:
--   echo -n 'clave' | sha256sum

INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, created_by)
VALUES (seq_usuario.NEXTVAL,
        'jperez@enph.udi.edu.co',
        '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 
        'Juan', 'Pérez', '3001234567',
        3,      
        'S',    
        1);     
COMMIT;


-- ---------------------------------------------------------------------
-- R - Leer (SELECT)
-- ---------------------------------------------------------------------

-- Todos, con el nombre del rol
SELECT u.id, u.nombre, u.apellido, u.email, r.nombre AS rol, u.activo, u.ultimo_acceso
FROM usuario u
JOIN rol r ON r.id = u.rol_id
ORDER BY u.apellido, u.nombre;

-- Uno por id
SELECT * FROM usuario WHERE id = 5;

-- Por correo (lo que hace el login)
SELECT * FROM usuario WHERE LOWER(email) = LOWER('jperez@enph.udi.edu.co');

-- Búsqueda por nombre o correo (como el filtro de la pantalla)
SELECT * FROM usuario
WHERE LOWER(nombre || ' ' || apellido) LIKE '%' || LOWER('juan') || '%'
   OR LOWER(email) LIKE '%' || LOWER('juan') || '%';

-- Solo activos de un rol
SELECT * FROM usuario WHERE rol_id = 3 AND activo = 'S';


-- ---------------------------------------------------------------------
-- U - Actualizar (UPDATE)
-- ---------------------------------------------------------------------
-- Sin triggers, updated_at = SYSTIMESTAMP se pone a mano en cada UPDATE.

-- Datos personales y rol
UPDATE usuario
SET nombre     = 'Juan Carlos',
    apellido   = 'Pérez',
    telefono   = '3109876543',
    rol_id     = 2,
    updated_at = SYSTIMESTAMP
WHERE id = 5;

-- Cambiar la contraseña (reemplace por el hash SHA-256 de la nueva clave)
UPDATE usuario
SET password = '<hash_sha256_hex>', updated_at = SYSTIMESTAMP
WHERE id = 5;

-- Desactivar (o activar con 'S')
UPDATE usuario SET activo = 'N', updated_at = SYSTIMESTAMP WHERE id = 5;

COMMIT;


-- ---------------------------------------------------------------------
-- D - Eliminar (DELETE)
-- ---------------------------------------------------------------------
-- Solo funciona si el usuario no ha registrado nada: encuesta, hogar,
-- visita, registro_diario, etc. apuntan a usuario por created_by o
-- cerrada_por. Si ya tiene registros, Oracle responde
--   ORA-02292: integrity constraint violated - child record found
-- y en ese caso se desactiva (activo = 'N') en vez de borrarlo,
-- igual que hace la pantalla de Usuarios.

-- Si es encuestador, primero se borra su fila en encuestador
DELETE FROM encuestador WHERE usuario_id = 5;
DELETE FROM usuario WHERE id = 5;

COMMIT;
