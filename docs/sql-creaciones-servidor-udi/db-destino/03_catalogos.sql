-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Catálogos del cuadernillo y ubicaciones
--
-- Lo ejecuta instalar.sql. Igual en db-origen y db-destino: grupos de
-- alimentos y lugares de compra (ids = códigos oficiales del cuadernillo),
-- 6 regiones, 33 departamentos (id = código DANE) y 1102 municipios
-- (ubicaciones/). La sincronización no copia estos catálogos: los dos
-- esquemas los tienen con los mismos ids.
-- =====================================================================

SET DEFINE OFF
-- =====================================================================
-- 5. CATÁLOGOS DEL CUADERNILLO
-- =====================================================================

INSERT INTO grupo_alimento (id, nombre) VALUES (1,  'Pan, arepas, bollos y almojábanas');
INSERT INTO grupo_alimento (id, nombre) VALUES (2,  'Galletas de sal y de dulce');
INSERT INTO grupo_alimento (id, nombre) VALUES (3,  'Arroz, pastas alimenticias, avena, maíz, harinas y otros cereales (para el desayuno)');
INSERT INTO grupo_alimento (id, nombre) VALUES (4,  'Carne de res, cerdo, hueso y vísceras');
INSERT INTO grupo_alimento (id, nombre) VALUES (5,  'Pollo, otras aves y menudencias');
INSERT INTO grupo_alimento (id, nombre) VALUES (6,  'Salchichas, jamón, mortadela, salchichón y otras carnes frías preparadas (incluidos los embutidos vegetarianos)');
INSERT INTO grupo_alimento (id, nombre) VALUES (7,  'Pescado de río, mar y otros productos marinos (frescos, congelados y enlatados)');
INSERT INTO grupo_alimento (id, nombre) VALUES (8,  'Leche (animal y vegetal)');
INSERT INTO grupo_alimento (id, nombre) VALUES (9,  'Queso (animal y vegetal)');
INSERT INTO grupo_alimento (id, nombre) VALUES (10, 'Otros derivados de la leche: yogur, kumis, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (11, 'Huevos');
INSERT INTO grupo_alimento (id, nombre) VALUES (12, 'Aceites y grasas: aceite, manteca, mantequilla, margarina, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (13, 'Frutas');
INSERT INTO grupo_alimento (id, nombre) VALUES (14, 'Verduras y hortalizas: tomate, cebolla, arveja, espinaca, apio, zanahoria, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (15, 'Granos secos: fríjol, lenteja, garbanzo, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (16, 'Enlatados y encurtidos: arveja, fríjol, maíz, espárragos, zanahoria, habichuela, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (17, 'Plátano, yuca, arracacha, name, papa y otros tubérculos');
INSERT INTO grupo_alimento (id, nombre) VALUES (18, 'Azúcar, panela y otros endulzantes');
INSERT INTO grupo_alimento (id, nombre) VALUES (19, 'Mermeladas, arequipe, bocadillos, compotas, dulces, helados, chocolatinas, ponquecitos, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (20, 'Salsas, mayonesa, mostaza, vinagre, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (21, 'Sal y condimentos');
INSERT INTO grupo_alimento (id, nombre) VALUES (22, 'Café, chocolate, otras bebidas con cacao (milo, chocolisto), té y hierbas aromáticas');
INSERT INTO grupo_alimento (id, nombre) VALUES (23, 'Papas fritas, chitos, maicitos, patacones, besitos, maní, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (24, 'Agua, gaseosas, refrescos, jugos, té frío y otras bebidas no alcohólicas');
INSERT INTO grupo_alimento (id, nombre) VALUES (25, 'Alimentos y bebidas preparados fuera del hogar, para consumir dentro o fuera del hogar');

INSERT INTO lugar_compra (id, nombre) VALUES (1,  'Almacenes, supermercado de cadena, tiendas por departamento o hipermercados');
INSERT INTO lugar_compra (id, nombre) VALUES (4,  'Supermercados de cajas de compensación, cooperativas, fondos de empleados y comisariatos');
INSERT INTO lugar_compra (id, nombre) VALUES (6,  'Supermercado de barrio, tiendas de barrio, cigarrerías, salsamentarias y delicatessen');
INSERT INTO lugar_compra (id, nombre) VALUES (7,  'Misceláneas de barrio y cacharrerías');
INSERT INTO lugar_compra (id, nombre) VALUES (10, 'Plazas de mercado, galerías, mercados móviles, central mayorista de abastecimiento y graneros');
INSERT INTO lugar_compra (id, nombre) VALUES (13, 'Vendedores ambulantes');
INSERT INTO lugar_compra (id, nombre) VALUES (14, 'Sanandresitos, bodegas y fábricas');
INSERT INTO lugar_compra (id, nombre) VALUES (16, 'Establecimiento especializado en la venta del artículo o la prestación del servicio');
INSERT INTO lugar_compra (id, nombre) VALUES (17, 'Farmacias y droguerías');
INSERT INTO lugar_compra (id, nombre) VALUES (18, 'Restaurantes');
INSERT INTO lugar_compra (id, nombre) VALUES (19, 'Cafeterías y establecimientos de comidas rápidas');
INSERT INTO lugar_compra (id, nombre) VALUES (20, 'Persona particular');
INSERT INTO lugar_compra (id, nombre) VALUES (21, 'Ferias especializadas: artesanal, del libro, del hogar, de tecnología, etc.');
INSERT INTO lugar_compra (id, nombre) VALUES (22, 'A través de internet');
INSERT INTO lugar_compra (id, nombre) VALUES (23, 'Televentas y ventas por catálogo');
INSERT INTO lugar_compra (id, nombre) VALUES (24, 'Otro');
INSERT INTO lugar_compra (id, nombre) VALUES (25, 'Plazoleta de comidas');

-- Ubicación: @@ busca los archivos junto a este script
@@ubicaciones/regiones.sql
@@ubicaciones/departamentos.sql
@@ubicaciones/municipios.sql

COMMIT;
