# Etapa 0 — Maven + Java puro: la estructura estándar y por qué existe

> Guía retroactiva: esta etapa ya está implementada. Queda escrita porque su
> lección más importante —la estructura— es la referencia a la que vas a volver,
> y porque `Main.java` (el código que la ilustra) se borra en la Etapa 1.

**Lo que esta etapa dejó:** un proyecto Maven mínimo, sin frameworks, que
compila y corre. Probarlo:

```bash
./mvnw compile
```

```bash
java -cp target/classes com.accel.finanzas.Main
```

---

## La estructura, de una vez por todas

Esto no es capricho de Spring ni de ninguna empresa: es el **Standard Directory
Layout** de Maven (existe desde ~2004) y en BCP y en toda la industria Java se
usa **tal cual**, también con Gradle. La idea se llama *convención sobre
configuración*: como todos los proyectos tienen la misma forma, cualquier dev,
IDE o pipeline de CI sabe dónde está todo sin que nadie se lo diga.

```
finanzas/
├── pom.xml                  ← el "DNI" del proyecto + su lista de dependencias
├── mvnw, .mvn/              ← Maven Wrapper: el proyecto trae su propio Maven
├── src/main/java/           ← el código que se compila
│   └── com/accel/finanzas/  ← NO son carpetas al azar: es el paquete com.accel.finanzas
├── src/main/resources/      ← archivos NO-código que viajan dentro del .jar
├── src/test/java/           ← tests: se compilan y corren, pero jamás se empaquetan
└── target/                  ← todo lo generado; desechable, nunca va al git
```

- **El paquete como carpetas.** `com/accel/finanzas` es el nombre del paquete
  `com.accel.finanzas` convertido en directorios. La convención es el **dominio
  de internet al revés** (kambista.com → `com.kambista`), para que tu clase
  `Cliente` nunca choque con la clase `Cliente` de otra empresa o librería.
- **`pom.xml`** identifica el proyecto con sus **coordenadas Maven**
  (`groupId:artifactId:version` → `com.accel:finanzas:0.1.0`) — el mismo
  formato con el que luego declararás dependencias de otros.
- **El wrapper (`mvnw`)** descarga y usa una versión fija de Maven por
  proyecto: nadie del equipo instala Maven a mano y todos compilan igual.

## El classpath y los resources (la lección de `Main.java`)

Al compilar, Maven deja los `.class` en `target/classes` **y copia ahí lo de
`src/main/resources/`**. Correr con `java -cp target/classes ...` significa
"busca clases y archivos en esa carpeta": eso es el **classpath**.

Por eso `Main.java` podía leer `app.properties` así:

```java
try (InputStream in = Main.class.getResourceAsStream("/app.properties")) {
    config.load(in);
}
```

El archivo no se lee "del disco duro del proyecto", se lee **del classpath** —
y por eso funcionará igual cuando viva dentro de un jar. Spring hace
exactamente esto mismo con su `application.properties` (Etapa 1): convención en
lugar de código.

## Decisiones que ya quedaron fijadas

- **Dinero en `BigDecimal`, jamás `double`.** `double` es binario de punto
  flotante y pierde centavos: `0.1 + 0.2 = 0.30000000000000004`. En banca,
  eliminatorio.
- **`record` para datos inmutables.** `Movimiento` es un record: constructor,
  accesores, `equals`, `hashCode` y `toString` gratis, y sus campos son
  `final`.

> **Tecnicismos sobre este código.** `Movimiento` ilustra dos cosas con nombre
> propio: **encapsulamiento** (pilar OOP: expones accesores, no campos — el
> record lo hace por ti) e **inmutabilidad** (un objeto que no cambia tras
> crearse: se puede compartir entre hilos sin miedo — clave cuando lleguemos a
> concurrencia y a reactivo). Un objeto inmutable definido solo por sus valores
> es lo que DDD llama **objeto de valor** (*value object*): dos `Movimiento`
> con iguales campos son intercambiables, no tienen identidad propia.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **Standard Directory Layout** | La estructura de carpetas convencional de Maven que toda la industria comparte |
| **Coordenadas Maven** | `groupId:artifactId:version` — la identidad única de un artefacto |
| **Classpath** | La lista de lugares donde la JVM busca clases y recursos |
| **Resources** | Archivos no-código que se empaquetan junto a las clases y se leen del classpath |
| **Maven Wrapper (`mvnw`)** | El proyecto carga su propio Maven, con versión fija para todo el equipo |
| **Encapsulamiento** | Pilar OOP: exponer comportamiento/accesores, no el estado interno |
| **Inmutabilidad** | Objeto que no cambia después de creado; seguro de compartir entre hilos |
| **Objeto de valor (value object)** | Objeto definido solo por sus valores, sin identidad propia (`Movimiento`) |

## Para la entrevista

> "La estructura `src/main/java/com/empresa/proyecto` no es del framework: es el
> Standard Directory Layout de Maven más el paquete escrito como dominio
> invertido. La gracia es que cualquier herramienta —IDE, CI, Docker— encuentra
> el código sin configuración. Y desde el primer archivo del proyecto, el
> dinero va en `BigDecimal`: `double` pierde centavos por representación
> binaria."

**Siguiente:** [Etapa 1 — De Java puro a Spring Boot](etapa-01-spring-boot.md).
