package com.udi.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Libro de Excel (.xlsx) sencillo: varias hojas con texto, números, pesos y
 * porcentajes, encabezados en negrita y ancho de columnas.
 *
 * Un .xlsx es un ZIP con archivos XML (formato Office Open XML), así que se
 * escribe con {@code java.util.zip} sin librerías externas.
 *
 * <pre>
 * LibroExcel libro = new LibroExcel();
 * libro.hoja("Resumen").anchos(30, 18).encabezado("Indicador", "Valor").fila("Gasto", LibroExcel.pesos(total));
 * libro.guardar(archivo);
 * </pre>
 */
public final class LibroExcel {

    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    // Índices de cellXfs en styles.xml
    private static final int NORMAL = 0;
    private static final int NEGRITA = 1;
    private static final int PESOS = 2;
    private static final int PESOS_NEGRITA = 3;
    private static final int ENCABEZADO = 4;
    private static final int PORCENTAJE = 5;
    private static final int PORCENTAJE_NEGRITA = 6;

    private final List<Hoja> hojas = new ArrayList<>();

    /** Agrega una hoja. Excel no permite nombres de más de 31 caracteres ni los símbolos : \ / ? * [ ]. */
    public Hoja hoja(String nombre) {
        String limpio = nombre.replaceAll("[:\\\\/?*\\[\\]]", " ");
        Hoja hoja = new Hoja(limpio.length() > 31 ? limpio.substring(0, 31) : limpio);
        hojas.add(hoja);
        return hoja;
    }

    /** Valor en pesos: se muestra como $ 12.500 y Excel lo sigue tratando como número. */
    public static Object pesos(BigDecimal valor) {
        return new Celda(valor == null ? BigDecimal.ZERO : valor, PESOS);
    }

    /** Fracción (0,25) que se muestra como porcentaje (25 %). */
    public static Object porcentaje(double fraccion) {
        return new Celda(fraccion, PORCENTAJE);
    }

    public void guardar(Path archivo) throws IOException {
        if (hojas.isEmpty()) {
            hoja("Hoja1");
        }
        try (OutputStream salida = Files.newOutputStream(archivo);
             ZipOutputStream zip = new ZipOutputStream(salida)) {
            escribir(zip, "[Content_Types].xml", tiposDeContenido());
            escribir(zip, "_rels/.rels", XML
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + NS_R + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            escribir(zip, "xl/workbook.xml", libro());
            escribir(zip, "xl/_rels/workbook.xml.rels", relacionesDelLibro());
            escribir(zip, "xl/styles.xml", estilos());
            for (int i = 0; i < hojas.size(); i++) {
                escribir(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hojas.get(i).xml());
            }
        }
    }

    private static void escribir(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String tiposDeContenido() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= hojas.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private String libro() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<workbook xmlns=\"").append(NS).append("\" xmlns:r=\"").append(NS_R).append("\"><sheets>");
        for (int i = 1; i <= hojas.size(); i++) {
            sb.append("<sheet name=\"").append(escapar(hojas.get(i - 1).nombre)).append("\" sheetId=\"").append(i)
                    .append("\" r:id=\"rId").append(i).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private String relacionesDelLibro() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= hojas.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i).append("\" Type=\"").append(NS_R)
                    .append("/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(hojas.size() + 1).append("\" Type=\"").append(NS_R)
                .append("/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    /** Fuentes normal y negrita, relleno del encabezado y formatos de pesos y porcentaje. */
    private static String estilos() {
        String xf = "<xf numFmtId=\"%d\" fontId=\"%d\" fillId=\"%d\" borderId=\"0\" xfId=\"0\""
                + " applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\"/>";
        return XML + "<styleSheet xmlns=\"" + NS + "\">"
                + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;$ &quot;#,##0\"/></numFmts>"
                + "<fonts count=\"2\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"3\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEFEAE1\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"7\">"
                + String.format(xf, 0, 0, 0)      // NORMAL
                + String.format(xf, 0, 1, 0)      // NEGRITA
                + String.format(xf, 164, 0, 0)    // PESOS
                + String.format(xf, 164, 1, 0)    // PESOS_NEGRITA
                + String.format(xf, 0, 1, 2)      // ENCABEZADO
                + String.format(xf, 9, 0, 0)      // PORCENTAJE (formato 9 = 0%)
                + String.format(xf, 9, 1, 0)      // PORCENTAJE_NEGRITA
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    // Los caracteres de control no son válidos en XML
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** A, B… Z, AA, AB… */
    private static String columna(int indice) {
        StringBuilder sb = new StringBuilder();
        for (int n = indice + 1; n > 0; n = (n - 1) / 26) {
            sb.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return sb.toString();
    }

    /** Valor con un estilo de número explícito (pesos o porcentaje). */
    private static final class Celda {
        private final Object valor;
        private final int estilo;

        private Celda(Object valor, int estilo) {
            this.valor = valor;
            this.estilo = estilo;
        }
    }

    /** Una hoja del libro. Los métodos devuelven la hoja para encadenar llamadas. */
    public static final class Hoja {
        private final String nombre;
        private final StringBuilder filas = new StringBuilder();
        private double[] anchos = new double[0];
        private int numeroFila;

        private Hoja(String nombre) {
            this.nombre = nombre;
        }

        /** Ancho de cada columna, en caracteres. */
        public Hoja anchos(double... anchos) {
            this.anchos = anchos.clone();
            return this;
        }

        /** Fila de títulos de columna: negrita con fondo. */
        public Hoja encabezado(Object... celdas) {
            return agregar(true, true, celdas);
        }

        public Hoja fila(Object... celdas) {
            return agregar(false, false, celdas);
        }

        /** Fila en negrita (títulos de sección y totales). */
        public Hoja filaNegrita(Object... celdas) {
            return agregar(true, false, celdas);
        }

        public Hoja vacia() {
            numeroFila++;
            return this;
        }

        private Hoja agregar(boolean negrita, boolean encabezado, Object... celdas) {
            numeroFila++;
            filas.append("<row r=\"").append(numeroFila).append("\">");
            for (int i = 0; i < celdas.length; i++) {
                Object valor = celdas[i];
                if (valor == null) {
                    continue;
                }
                int estilo = encabezado ? ENCABEZADO : negrita ? NEGRITA : NORMAL;
                if (valor instanceof Celda) {
                    Celda celda = (Celda) valor;
                    valor = celda.valor;
                    estilo = celda.estilo == PESOS
                            ? (negrita ? PESOS_NEGRITA : PESOS)
                            : (negrita ? PORCENTAJE_NEGRITA : PORCENTAJE);
                }
                String ref = columna(i) + numeroFila;
                filas.append("<c r=\"").append(ref).append("\"");
                if (estilo != NORMAL) {
                    filas.append(" s=\"").append(estilo).append("\"");
                }
                if (valor instanceof Number) {
                    filas.append("><v>").append(numero((Number) valor)).append("</v></c>");
                } else {
                    filas.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(escapar(String.valueOf(valor))).append("</t></is></c>");
                }
            }
            filas.append("</row>");
            return this;
        }

        private static String numero(Number n) {
            if (n instanceof BigDecimal) {
                return ((BigDecimal) n).toPlainString();
            }
            if (n instanceof Double || n instanceof Float) {
                return String.format(Locale.ROOT, "%.6f", n.doubleValue());
            }
            return String.valueOf(n.longValue());
        }

        private String xml() {
            StringBuilder sb = new StringBuilder(XML).append("<worksheet xmlns=\"").append(NS).append("\">");
            if (anchos.length > 0) {
                sb.append("<cols>");
                for (int i = 0; i < anchos.length; i++) {
                    sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                            .append(String.format(Locale.ROOT, "%.1f", anchos[i])).append("\" customWidth=\"1\"/>");
                }
                sb.append("</cols>");
            }
            return sb.append("<sheetData>").append(filas).append("</sheetData></worksheet>").toString();
        }
    }
}
