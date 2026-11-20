# Crear las bases de datos en el servidor Oracle de la UDI

Aquí están los scripts para crear los **dos esquemas** que usa el proyecto en el servidor Oracle:

| Carpeta | Esquema (usuario Oracle) | Qué es |
|---|---|---|
| `db-origen/` | `DJGCJDCMPI` | El esquema con el que trabaja la aplicación: usuarios, viviendas, hogares y encuestas |
| `db-destino/` | `DJGCJDCMPI_CENTRAL` | El esquema central: recibe las encuestas **APROBADAS** del origen con el botón «Sincronizar aprobadas» del módulo Verificación |

Además, el origen tiene tres usuarios de Oracle con permisos limitados sobre sus tablas (paso 4):

| Usuario Oracle | Rol Oracle | Para qué |
|---|---|---|
| `DJGCJDCMPI_APP` | `ROL_DJGCJDCMPI_APLICACION` | La aplicación puede conectarse con él: lee y escribe datos, usa secuencias y ejecuta funciones y procedimientos. No puede crear ni borrar objetos |
| `DJGCJDCMPI_CONSULTA` | `ROL_DJGCJDCMPI_LECTURA` | Solo lectura (reportes, análisis). No ve la tabla `usuario` (tiene las claves) |
| `DJGCJDCMPI_AUDITOR` | `ROL_DJGCJDCMPI_AUDITOR` | Solo lee `auditoria` y `auditoria_encuesta` |

La clave de cada uno es igual a su nombre.

## Paso a paso

El orden importa: los dos usuarios se crean primero, luego se instala el **origen**, después se crean los usuarios `_APP`, `_CONSULTA` y `_AUDITOR`, y al final se instala el **destino**.

### Paso 1. Crear el usuario origen (como SYSTEM)

```bash
cd db-origen
sqlplus system/<clave_system>@192.168.254.215:1521/ORCL @01_crear_usuario.sql DJGCJDCMPI
```

### Paso 2. Crear el usuario destino (como SYSTEM)

```bash
cd ../db-destino
sqlplus system/<clave_system>@192.168.254.215:1521/ORCL @01_crear_usuario.sql DJGCJDCMPI_CENTRAL
```

### Paso 3. Instalar el origen (como DJGCJDCMPI)

```bash
cd ../db-origen
sqlplus DJGCJDCMPI/DJGCJDCMPI@192.168.254.215:1521/ORCL @instalar.sql DJGCJDCMPI_CENTRAL
```

Si todo sale bien, la última línea es:
```
Origen instalado. Siga con db-destino/instalar.sql (parametro: este esquema).
```

### Paso 4. Crear los usuarios `_APP`, `_CONSULTA` y `_AUDITOR` (como SYSTEM)

```bash
sqlplus system/<clave_system>@192.168.254.215:1521/ORCL @usuarios_roles.sql DJGCJDCMPI
```

> Si lo ejecuta **después** del paso 5 (o lo vuelve a ejecutar), el rol `ROL_DJGCJDCMPI_APLICACION` se recrea y pierde el permiso para ejecutar `pr_sincronizar_aprobadas`. Para devolvérselo, desde `db-destino`:
> ```bash
> sqlplus DJGCJDCMPI_CENTRAL/DJGCJDCMPI_CENTRAL@192.168.254.215:1521/ORCL @05_procedimiento_sincronizacion.sql DJGCJDCMPI
> ```

### Paso 5. Instalar el destino (como DJGCJDCMPI_CENTRAL)

```bash
cd ../db-destino
sqlplus DJGCJDCMPI_CENTRAL/DJGCJDCMPI_CENTRAL@192.168.254.215:1521/ORCL @instalar.sql DJGCJDCMPI
```

Si todo sale bien, la última línea es:
```
Destino instalado. Configure db.esquemaDestino con este esquema en la aplicacion.
```

### Paso 6. Probar la sincronización (como DJGCJDCMPI)

```bash
cd ../db-origen
sqlplus DJGCJDCMPI/DJGCJDCMPI@192.168.254.215:1521/ORCL @prueba_sincronizacion.sql DJGCJDCMPI_CENTRAL
```

### Paso 7. Configurar la aplicación

En `src/main/resources/com/udi/config/db.properties`:

```properties
db.url=jdbc:oracle:thin:@192.168.254.215:1521:ORCL
db.usuario=DJGCJDCMPI
db.contrasena=DJGCJDCMPI
db.esquema=
db.esquemaDestino=DJGCJDCMPI_CENTRAL
```

Para que la aplicación use el usuario con permisos limitados del paso 4 en lugar del dueño del esquema:

```properties
db.usuario=DJGCJDCMPI_APP
db.contrasena=DJGCJDCMPI_APP
db.esquema=DJGCJDCMPI
db.esquemaDestino=DJGCJDCMPI_CENTRAL
```

(`db.esquema` hace `ALTER SESSION SET CURRENT_SCHEMA` para que las consultas encuentren las tablas de `DJGCJDCMPI`.)

Para iniciar sesión en la aplicación, todos los usuarios de prueba tienen la clave `123456`:
- `admin@enph.udi.edu.co`: ADMIN
- `cperez@enph.udi.edu.co`: CRITICO_DIGITADOR (aprueba y sincroniza)
- los encuestadores

## Eliminar una base de datos

Como SYSTEM. Borra todos los datos del esquema y no se puede deshacer:

```bash
sqlplus system/<clave_system>@192.168.254.215:1521/ORCL
```
```sql
DROP USER DJGCJDCMPI_APP CASCADE;       -- usuarios del paso 4
DROP USER DJGCJDCMPI_CONSULTA CASCADE;
DROP USER DJGCJDCMPI_AUDITOR CASCADE;
DROP ROLE ROL_DJGCJDCMPI_APLICACION;
DROP ROLE ROL_DJGCJDCMPI_LECTURA;
DROP ROLE ROL_DJGCJDCMPI_AUDITOR;
DROP USER DJGCJDCMPI CASCADE;           -- elimina el origen
DROP USER DJGCJDCMPI_CENTRAL CASCADE;   -- elimina el destino
EXIT
```
