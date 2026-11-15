# Crear las bases de datos en el servidor Oracle de la UDI

Aquí están los scripts para crear los **dos esquemas** que usa el proyecto en el servidor Oracle:

| Carpeta | Esquema (usuario Oracle) | Qué es |
|---|---|---|
| `db-origen/` | `DJGCJDCMPI` | El esquema con el que trabaja la aplicación: usuarios, viviendas, hogares y encuestas |
| `db-destino/` | `DJGCJDCMPI_CENTRAL` | El esquema central: recibe las encuestas **APROBADAS** del origen con el botón «Sincronizar aprobadas» del módulo Verificación |

## Paso a paso

El orden importa: los dos usuarios se crean primero, luego se instala el **origen** y al final el **destino**.

### Paso 1. Crear el usuario origen (como SYSTEM)

```bash
cd db-origen
sqlplus system/<clave_system>@<servidor> @01_crear_usuario.sql DJGCJDCMPI
```

### Paso 2. Crear el usuario destino (como SYSTEM)

```bash
cd ../db-destino
sqlplus system/<clave_system>@<servidor> @01_crear_usuario.sql DJGCJDCMPI_CENTRAL
```

### Paso 3. Instalar el origen (como DJGCJDCMPI)

```bash
cd ../db-origen
sqlplus DJGCJDCMPI/DJGCJDCMPI@<servidor> @instalar.sql DJGCJDCMPI_CENTRAL
```

Si todo sale bien, la última línea es:
```
Origen instalado. Siga con db-destino/instalar.sql (parametro: este esquema).
```

### Paso 4. Instalar el destino (como DJGCJDCMPI_CENTRAL)

```bash
cd ../db-destino
sqlplus DJGCJDCMPI_CENTRAL/DJGCJDCMPI_CENTRAL@<servidor> @instalar.sql DJGCJDCMPI
```

Si todo sale bien, la última línea es:
```
Destino instalado. Configure db.esquemaDestino con este esquema en la aplicacion.
```

### Paso 5. Probar la sincronización (como DJGCJDCMPI)

```bash
cd ../db-origen
sqlplus DJGCJDCMPI/DJGCJDCMPI@<servidor> @prueba_sincronizacion.sql DJGCJDCMPI_CENTRAL
```

### Paso 6. Configurar la aplicación

En `src/main/resources/com/udi/config/db.properties`:

```properties
db.url=jdbc:oracle:thin:@<host>:<puerto>:<SID>
db.usuario=DJGCJDCMPI
db.contrasena=DJGCJDCMPI
db.esquema=
db.esquemaDestino=DJGCJDCMPI_CENTRAL
```

Para iniciar sesión en la aplicación, todos los usuarios de prueba tienen la clave `123456`:
- `admin@enph.udi.edu.co`: ADMIN
- `cperez@enph.udi.edu.co`: CRITICO_DIGITADOR (aprueba y sincroniza)
- los encuestadores

## Eliminar una base de datos

```sql
DROP USER DJGCJDCMPI CASCADE;           -- elimina el origen
DROP USER DJGCJDCMPI_CENTRAL CASCADE;   -- elimina el destino
EXIT
```


