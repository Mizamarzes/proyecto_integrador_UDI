-- =====================================================================
-- Departamentos de Colombia (33, Bogotá incluido). El id es el código DANE
-- (DIVIPOLA); seq_departamento empieza en 100. Requiere regiones.sql.
-- Oracle 10g. Ejecutar con NLS_LANG=AMERICAN_AMERICA.AL32UTF8.
-- =====================================================================

SET DEFINE OFF

INSERT INTO departamento (id, nombre, region_id) VALUES (5, 'Antioquia', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (8, 'Atlántico', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (11, 'Bogotá, D.C.', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (13, 'Bolívar', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (15, 'Boyacá', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (17, 'Caldas', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (18, 'Caquetá', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (19, 'Cauca', 3);
INSERT INTO departamento (id, nombre, region_id) VALUES (20, 'Cesar', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (23, 'Córdoba', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (25, 'Cundinamarca', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (27, 'Chocó', 3);
INSERT INTO departamento (id, nombre, region_id) VALUES (41, 'Huila', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (44, 'La Guajira', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (47, 'Magdalena', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (50, 'Meta', 4);
INSERT INTO departamento (id, nombre, region_id) VALUES (52, 'Nariño', 3);
INSERT INTO departamento (id, nombre, region_id) VALUES (54, 'Norte de Santander', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (63, 'Quindío', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (66, 'Risaralda', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (68, 'Santander', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (70, 'Sucre', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (73, 'Tolima', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (76, 'Valle del Cauca', 3);
INSERT INTO departamento (id, nombre, region_id) VALUES (81, 'Arauca', 4);
INSERT INTO departamento (id, nombre, region_id) VALUES (85, 'Casanare', 4);
INSERT INTO departamento (id, nombre, region_id) VALUES (86, 'Putumayo', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (88, 'Archipiélago de San Andrés, Providencia y Santa Catalina', 6);
INSERT INTO departamento (id, nombre, region_id) VALUES (91, 'Amazonas', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (94, 'Guainía', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (95, 'Guaviare', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (97, 'Vaupés', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (99, 'Vichada', 4);
