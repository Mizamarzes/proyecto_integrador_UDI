package com.udi.dao.verificacion;

import com.udi.dao.BaseDAO;
import com.udi.model.ResultadoSincronizacion;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Sincronización de las encuestas aprobadas con el esquema central. No
 * corresponde a una tabla, así que no extiende {@link BaseDAO}: llama al
 * procedimiento {@code <destino>.pr_sincronizar_aprobadas}
 * (docs/sql-creaciones-servidor-udi/db-destino/05_procedimiento_sincronizacion.sql), que vive en el
 * esquema destino y lee las vistas {@code vis_sinc_*} del origen. No hace
 * COMMIT: el servicio lo llama dentro de una transacción.
 */
public class SincronizacionDAO {

    /**
     * @param destino dueño del esquema destino, ya validado como nombre de
     *                usuario de Oracle (ver {@code ConexionBD.esquemaDestino})
     */
    public ResultadoSincronizacion sincronizar(Connection c, String destino) throws SQLException {
        try (CallableStatement cs = c.prepareCall("{call " + destino + ".pr_sincronizar_aprobadas(?, ?)}")) {
            cs.registerOutParameter(1, Types.NUMERIC);
            cs.registerOutParameter(2, Types.NUMERIC);
            cs.execute();
            return new ResultadoSincronizacion(cs.getInt(1), cs.getInt(2));
        }
    }
}
