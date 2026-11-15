-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Prueba de la sincronización de encuestas aprobadas (paso 5 de la guía
-- docs/referencias/SincronizaciónOracle(EsquemaAesquema).pdf)
--
-- Se ejecuta como dueño del ORIGEN, con el destino como argumento, después
-- de instalar db-origen y db-destino:
--   sqlplus <origen>/<clave> @prueba_sincronizacion.sql <DESTINO>
--
-- Pasos (todo en una transacción que termina en ROLLBACK, en los dos
-- esquemas, así que se puede repetir sin dañar nada):
--   1. Si hay una encuesta pendiente de aprobación, la aprueba (para tener
--      al menos una APROBADA que enviar).
--   2. Sincroniza: las aprobadas que el destino no tenía salen como nuevas.
--   3. Sincroniza otra vez: 0 nuevas y 0 actualizadas (no duplica).
--   4. Reabre, cierra y vuelve a aprobar esa encuesta y sincroniza: sale
--      como actualizada (se reemplaza en el destino).
-- =====================================================================

SET SERVEROUTPUT ON SIZE 100000
SET DEFINE ON
SET VERIFY OFF
SET FEEDBACK OFF
DEFINE destino = &1

DECLARE
v_encuestaId encuesta.id%TYPE;
v_usuarioId usuario.id%TYPE;
v_nuevas NUMBER;
v_actualizadas NUMBER;
BEGIN
SELECT MIN(id) INTO v_usuarioId FROM usuario;
SELECT MIN(id) INTO v_encuestaId FROM encuesta WHERE estado='PENDIENTE_APROBACION';

IF v_encuestaId IS NOT NULL THEN
   pr_aprobar_encuesta(v_encuestaId,v_usuarioId);
   DBMS_OUTPUT.PUT_LINE('1. Se aprobo '||fn_codigo_encuesta(v_encuestaId)||' para la prueba.');
ELSE
   DBMS_OUTPUT.PUT_LINE('1. No hay encuestas pendientes: se sincronizan las aprobadas que existan.');
END IF;

&destino..pr_sincronizar_aprobadas(v_nuevas,v_actualizadas);
DBMS_OUTPUT.PUT_LINE('2. Primera sincronizacion: '||v_nuevas||' nuevas, '||v_actualizadas||' actualizadas.');

&destino..pr_sincronizar_aprobadas(v_nuevas,v_actualizadas);
DBMS_OUTPUT.PUT_LINE('3. Segunda sincronizacion: '||v_nuevas||' nuevas, '||v_actualizadas||' actualizadas.');

IF v_encuestaId IS NOT NULL THEN
   pr_reabrir_encuesta(v_encuestaId);
   pr_cerrar_encuesta(v_encuestaId,v_usuarioId,SYSTIMESTAMP);
   pr_aprobar_encuesta(v_encuestaId,v_usuarioId);
   &destino..pr_sincronizar_aprobadas(v_nuevas,v_actualizadas);
   DBMS_OUTPUT.PUT_LINE('4. Tras reabrir y aprobar de nuevo: '||v_nuevas||' nuevas, '
      ||v_actualizadas||' actualizadas.');
END IF;
END;
/

ROLLBACK;
PROMPT Prueba terminada (ROLLBACK: no quedo nada en el origen ni en el destino).
EXIT
