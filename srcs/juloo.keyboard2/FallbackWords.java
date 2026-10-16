package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FallbackWords implements TextEngine.WordSource {
    static final FallbackWords INSTANCE = new FallbackWords();
    static final String[] WORDS = {"el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "en", "y", "e", "o", "u", "que", "qué", "se", "no", "sí", "si", "con", "conmigo", "contigo", "por", "porque", "para", "como", "cómo", "más", "pero", "sino", "pues", "aunque", "mientras", "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas", "eso", "esto", "aquel", "aquella", "aquello", "mi", "mis", "tu", "tus", "su", "sus", "me", "te", "le", "les", "nos", "os", "lo", "hay", "está", "están", "estoy", "estás", "estamos", "soy", "eres", "es", "somos", "son", "era", "eran", "fue", "fueron", "sido", "estar", "estado", "estaba", "ser", "tengo", "tienes", "tiene", "tenemos", "tienen", "tenía", "tener", "he", "has", "ha", "hemos", "han", "haber", "hago", "haces", "hace", "hacemos", "hacen", "hacer", "hecho", "digo", "dices", "dice", "dicen", "decir", "dicho", "voy", "vas", "va", "vamos", "van", "ir", "veo", "ves", "ve", "ven", "ver", "visto", "doy", "das", "da", "dan", "dar", "dado", "sé", "sabes", "sabe", "saben", "saber", "quiero", "quieres", "quiere", "quieren", "querer", "puedo", "puedes", "puede", "pueden", "poder", "pongo", "pones", "pone", "ponen", "poner", "puesto", "parece", "parecen", "parecer", "sigo", "sigue", "siguen", "seguir", "queda", "quedan", "quedar", "creo", "cree", "creen", "creer", "debo", "debe", "deben", "deber", "pienso", "piensa", "pensar", "llega", "llegan", "llegar", "lleva", "llevan", "llevar", "pasa", "pasan", "pasar", "trabaja", "trabajo", "trabajan", "trabajar", "habla", "hablan", "hablar", "escribe", "escribo", "escriben", "escribir", "escrito", "lee", "leo", "leen", "leer", "mira", "miro", "miran", "mirar", "escucha", "vive", "viven", "vivir", "siente", "sienten", "sentir", "duerme", "duermen", "dormir", "come", "comen", "comer", "bebe", "beber", "corre", "corren", "correr", "abre", "abren", "abrir", "cierra", "cierran", "cerrar", "empieza", "empiezan", "empezar", "termina", "terminan", "terminar", "ayudan", "ayudar", "preguntan", "preguntar", "responde", "responden", "responder", "necesita", "necesitan", "necesitar", "gusta", "gustan", "gustar", "encanta", "prefiere", "prefieren", "conocen", "conocer", "entiende", "entienden", "entender", "aprende", "aprenden", "aprender", "enseña", "enseñan", "enseñar", "juega", "juegan", "jugar", "canta", "baila", "cocina", "limpia", "compra", "compran", "comprar", "vende", "venden", "vender", "paga", "pagar", "viaja", "viajar", "camina", "caminar", "sienta", "sientan", "sentar", "levanta", "levantan", "levantar", "llama", "llaman", "llamar", "busca", "buscan", "buscar", "encuentra", "encuentran", "encontrar", "pierde", "pierden", "perder", "gana", "ganan", "ganar", "cambia", "cambian", "cambiar", "usa", "usan", "usar", "prueba", "prueban", "probar", "revisa", "revisan", "revisar", "prepara", "preparan", "preparar", "recibe", "reciben", "recibir", "envía", "envían", "enviar", "trae", "traen", "traer", "bien", "mal", "muy", "mucho", "mucha", "muchos", "muchas", "poco", "poca", "pocos", "tan", "tanto", "tanta", "siempre", "nunca", "jamás", "ahora", "hoy", "ayer", "mañana", "pronto", "tarde", "temprano", "todavía", "aún", "ya", "entonces", "después", "antes", "luego", "además", "también", "tampoco", "quizá", "quizás", "apenas", "casi", "justo", "solo", "encima", "debajo", "delante", "detrás", "dentro", "fuera", "lejos", "cerca", "arriba", "abajo", "adelante", "atrás", "alrededor", "cuando", "cuándo", "donde", "dónde", "quién", "quiénes", "cuál", "cuáles", "cuánto", "cuántos", "todo", "todos", "todas", "nada", "algo", "alguien", "nadie", "otro", "otra", "otros", "otras", "mismo", "misma", "mismos", "nuevo", "nueva", "nuevos", "viejo", "vieja", "joven", "jóvenes", "gran", "grande", "grandes", "pequeño", "pequeña", "largo", "larga", "corto", "corta", "alto", "alta", "bajo", "baja", "claro", "clara", "oscuro", "oscura", "fácil", "difícil", "posible", "importante", "necesario", "seguro", "cierto", "libre", "lleno", "vacío", "rápido", "lento", "fuerte", "débil", "feliz", "triste", "contento", "enfermo", "sano", "rico", "pobre", "caro", "barato", "bueno", "buena", "buenos", "malo", "mala", "mejor", "mejores", "peor", "hola", "adiós", "gracias", "favor", "perdón", "disculpa", "saludos", "buenas", "primero", "primera", "segundo", "último", "última", "único", "cada", "ambos", "alguno", "alguna", "algunos", "ninguno", "ninguna", "varios", "varias", "tal", "tales", "cualquiera", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "veinte", "cien", "ciento", "mil", "millón", "rojo", "azul", "verde", "negro", "blanco", "amarillo", "gris", "rosa", "naranja", "marrón", "día", "días", "noche", "noches", "vez", "veces", "cosa", "cosas", "forma", "parte", "partes", "tiempo", "vida", "mundo", "casa", "hogar", "hombre", "mujer", "niño", "niña", "gente", "amigo", "amiga", "familia", "padre", "madre", "hijo", "hija", "hermano", "hermana", "dinero", "puerta", "ventana", "calle", "coche", "carro", "ciudad", "país", "pueblo", "nombre", "apellido", "caso", "punto", "puntos", "grupo", "equipo", "ayuda", "pregunta", "respuesta", "historia", "libro", "música", "canción", "foto", "video", "juego", "comida", "desayuno", "almuerzo", "cena", "agua", "leche", "café", "cerveza", "vino", "pan", "manzana", "salud", "amor", "mano", "manos", "ojo", "ojos", "cabeza", "cuerpo", "corazón", "mente", "aire", "fuego", "tierra", "mar", "río", "lago", "montaña", "playa", "bosque", "jardín", "parque", "escuela", "universidad", "clase", "examen", "nota", "idioma", "oficina", "fábrica", "tienda", "mercado", "banco", "hospital", "médico", "doctor", "iglesia", "fiesta", "reunión", "cita", "viaje", "vacaciones", "hotel", "avión", "tren", "barco", "bicicleta", "moto", "camión", "teléfono", "móvil", "celular", "computadora", "ordenador", "pantalla", "teclado", "tecla", "ratón", "impresora", "internet", "red", "sitio", "página", "correo", "mensaje", "llamada", "contacto", "archivo", "carpeta", "documento", "programa", "sistema", "datos", "código", "diseño", "proyecto", "ejemplo", "error", "texto", "palabra", "frase", "letra", "número", "cuenta", "contraseña", "usuario", "cliente", "servidor", "conexión", "descarga", "enlace", "chat", "foro", "blog", "noticia", "anuncio", "oferta", "precio", "venta", "negocio", "empresa", "jefe", "empleado", "sueldo", "contrato", "entrevista", "implementación", "implementar", "revisión", "actualizar", "instalar", "configurar", "descargar", "guardar", "encender", "apagar", "reiniciar", "android", "flutter", "termux", "linux", "github", "dart", "kotlin", "python", "java", "opencode", "aplicación", "comando", "terminal", "consola", "script", "versión"};
    static final Set<String> SET = new HashSet<>();
    static final String[] NORMS = new String[WORDS.length];

    static {
        for (int i = 0; i < WORDS.length; i++) {
            SET.add(WORDS[i]);
            NORMS[i] = TextEngine.normalize(WORDS[i]);
        }
    }

    private FallbackWords() {}

    @Override
    public boolean contains(String word) {
        return word != null && SET.contains(word);
    }

    @Override
    public List<String> completions(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return out;
        String normalized = TextEngine.normalize(prefix);
        for (int i = 0; i < WORDS.length && out.size() < limit; i++) {
            if (!WORDS[i].equals(prefix) && NORMS[i].startsWith(normalized)) out.add(WORDS[i]);
        }
        return out;
    }

    @Override
    public List<String> similar(String word, int limit) {
        List<String> out = new ArrayList<>();
        if (word == null || word.length() < 2) return out;
        final String normalized = TextEngine.normalize(word);
        List<String> candidates = new ArrayList<>();
        for (String candidate : WORDS) {
            if (!candidate.equals(word) && TextEngine.damerau(normalized, TextEngine.normalize(candidate), 2) <= 2) {
                candidates.add(candidate);
            }
        }
        Collections.sort(candidates, new Comparator<String>() {
            @Override
            public int compare(String first, String second) {
                int firstDistance = TextEngine.damerau(normalized, TextEngine.normalize(first), 2);
                int secondDistance = TextEngine.damerau(normalized, TextEngine.normalize(second), 2);
                return firstDistance != secondDistance ? firstDistance - secondDistance : first.compareTo(second);
            }
        });
        for (int i = 0; i < candidates.size() && out.size() < limit; i++) out.add(candidates.get(i));
        return out;
    }
}
