package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.GrupoAlimentoDAO;
import com.udi.dao.LugarCompraDAO;
import com.udi.model.GrupoAlimento;
import com.udi.model.LugarCompra;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.util.List;
import java.util.Optional;

/**
 * Catálogos del cuadernillo: grupos de alimentos (capítulo C) y lugares de
 * compra (capítulos D y D1). El id es el código del cuadernillo: si se envía,
 * se usa ese; si no, lo asigna la secuencia (desde 26).
 * Las operaciones que modifican datos solo las puede hacer un administrador.
 */
public class CatalogoService {

    private final GrupoAlimentoDAO grupoAlimentoDao = new GrupoAlimentoDAO();
    private final LugarCompraDAO lugarCompraDao = new LugarCompraDAO();

    // ------------------------------------------------------------------
    // Grupos de alimentos
    // ------------------------------------------------------------------

    public List<GrupoAlimento> listarGruposAlimento() {
        return ConexionBD.consultar(grupoAlimentoDao::listar);
    }

    public Optional<GrupoAlimento> buscarGrupoAlimento(long id) {
        return ConexionBD.consultar(c -> grupoAlimentoDao.buscarPorId(c, id));
    }

    public GrupoAlimento crearGrupoAlimento(GrupoAlimento grupo) {
        Sesion.exigirAdmin();
        Validar.requerido(grupo.getNombre(), "Nombre del grupo de alimentos");
        grupo.setCreatedBy(Sesion.idUsuario());
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(grupo.getId() == null || !grupoAlimentoDao.existe(c, grupo.getId()),
                    "Ya existe el grupo de alimentos con código " + grupo.getId() + ".");
            return grupoAlimentoDao.insertar(c, grupo);
        });
    }

    public GrupoAlimento actualizarGrupoAlimento(GrupoAlimento grupo) {
        Sesion.exigirAdmin();
        Validar.requerido(grupo.getId(), "Código del grupo de alimentos");
        Validar.requerido(grupo.getNombre(), "Nombre del grupo de alimentos");
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(grupoAlimentoDao.actualizar(c, grupo),
                    "No existe el grupo de alimentos " + grupo.getId() + ".");
            return grupo;
        });
    }

    /** Falla (por llave foránea) si el grupo ya se usó en alguna encuesta. */
    public boolean eliminarGrupoAlimento(long id) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> grupoAlimentoDao.eliminar(c, id));
    }

    // ------------------------------------------------------------------
    // Lugares de compra
    // ------------------------------------------------------------------

    public List<LugarCompra> listarLugaresCompra() {
        return ConexionBD.consultar(lugarCompraDao::listar);
    }

    public Optional<LugarCompra> buscarLugarCompra(long id) {
        return ConexionBD.consultar(c -> lugarCompraDao.buscarPorId(c, id));
    }

    public LugarCompra crearLugarCompra(LugarCompra lugar) {
        Sesion.exigirAdmin();
        Validar.requerido(lugar.getNombre(), "Nombre del lugar de compra");
        lugar.setCreatedBy(Sesion.idUsuario());
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(lugar.getId() == null || !lugarCompraDao.existe(c, lugar.getId()),
                    "Ya existe el lugar de compra con código " + lugar.getId() + ".");
            return lugarCompraDao.insertar(c, lugar);
        });
    }

    public LugarCompra actualizarLugarCompra(LugarCompra lugar) {
        Sesion.exigirAdmin();
        Validar.requerido(lugar.getId(), "Código del lugar de compra");
        Validar.requerido(lugar.getNombre(), "Nombre del lugar de compra");
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(lugarCompraDao.actualizar(c, lugar), "No existe el lugar de compra " + lugar.getId() + ".");
            return lugar;
        });
    }

    /** Falla (por llave foránea) si el lugar ya se usó en algún gasto. */
    public boolean eliminarLugarCompra(long id) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> lugarCompraDao.eliminar(c, id));
    }
}
