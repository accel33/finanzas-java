# Etapa 6 — Lombok: dónde aporta y dónde no

BCP pide Lombok en sus ofertas, así que hay que saber usarlo. Pero lo que suma en una
entrevista no es "lo uso", sino **saber dónde vale la pena y dónde no**. Esta etapa lo mide.

## Qué es

Una librería que **escribe código por ti mientras compilas**. Pones una anotación como
`@Getter` y, antes de generar el `.class`, Lombok inyecta los métodos que faltan. En tu
código fuente no aparecen; en el bytecode sí. Técnicamente es un *annotation processor*: un
plugin del compilador.

Por eso **no viaja en el jar**: se usa al compilar y listo. Comprobado:

```
unzip -l target/finanzas-0.1.0.jar | grep -ci lombok   →   0
```

## Dónde lo apliqué, y lo que ahorró de verdad

| Clase | Antes | Después | Qué hizo Lombok |
|---|---|---|---|
| `MovimientoEntity` | 53 | **35** | `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor` |
| `MovimientoController` | 70 | 68 | `@RequiredArgsConstructor` |
| `MovimientoRepositoryJpa` | 52 | 50 | `@RequiredArgsConstructor` |
| `MovimientoService` | 54 | 59 | `@RequiredArgsConstructor` + `@Slf4j` (creció porque agregué logs) |

La lectura honesta: **la entidad es donde Lombok gana de verdad**. Cuatro getters y dos
constructores desaparecieron. En el controller y el adaptador el ahorro es de dos líneas:
se usa ahí porque es la convención de la industria y queda uniforme, no porque ahorre mucho.
Con una entidad de 20 campos, en cambio, la diferencia serían cientos de líneas.

Para ver la entidad antes de Lombok, está en el commit de la Etapa 5:

```bash
git show bda3a18:src/main/java/com/accel/finanzas/model/MovimientoEntity.java
```

## Lo que generó, visto con `javap`

Tu código fuente de la entidad no tiene ni un getter. El `.class` compilado sí:

```
public class com.accel.finanzas.model.MovimientoEntity {
  public java.lang.Long getId();
  public java.lang.String getDescripcion();
  public java.math.BigDecimal getMonto();
  public java.time.LocalDate getFecha();
  protected com.accel.finanzas.model.MovimientoEntity();
  public com.accel.finanzas.model.MovimientoEntity(Long, String, BigDecimal, LocalDate);
}
```

Y en el service, un logger y un constructor que no escribiste:

```
private static final org.slf4j.Logger log;
public com.accel.finanzas.service.MovimientoService(MovimientoRepository);
```

Para verlo tú: `javap -p target/classes/com/accel/finanzas/model/MovimientoEntity.class`.

## Las anotaciones que usé

**`@RequiredArgsConstructor`** genera un constructor con **todos los campos `final`**. Es
decir: sigue siendo inyección por constructor, no por campo. Esto se confunde mucho en
entrevistas — `@RequiredArgsConstructor` no es "otra forma de inyectar", es escribir el mismo
constructor de antes sin escribirlo. La prueba: el test del service sigue haciendo
`new MovimientoService(repositorio)` y compila, porque el constructor existe.

**`@Getter`** genera un getter por campo. **`@NoArgsConstructor(access = PROTECTED)`** genera
el constructor vacío que JPA exige, protegido para que nadie más lo use. **`@AllArgsConstructor`**,
el constructor con todos los campos.

**`@Slf4j`** crea el logger. Equivale a escribir:

```java
private static final Logger log = LoggerFactory.getLogger(MovimientoService.class);
```

Y ahora el service deja rastro, comprobado con la app corriendo:

```
c.a.finanzas.service.MovimientoService  Movimiento creado id=6 monto=3.20
c.a.finanzas.service.MovimientoService  Movimiento eliminado id=6
```

Fíjate en los `{}`: `log.info("id={}", id)` y no `"id=" + id`. Con los marcadores, si el
nivel INFO está apagado, el texto ni se construye. Concatenando, se construye siempre.

## Dónde NO usé Lombok, y por qué

**En los records.** `Movimiento`, los DTOs, `Resumen`: un record **ya** genera constructor,
accesores, `equals`, `hashCode` y `toString`. Ponerle Lombok no aporta nada. Este es el punto
fuerte para la entrevista: **el 80% de lo que la gente usaba Lombok para conseguir, Java 16+
ya lo trae con los records**.

**`@Data` en la entidad, jamás.** `@Data` es el combo `@Getter` + `@Setter` + `@ToString` +
`@EqualsAndHashCode` + `@RequiredArgsConstructor`, y en una entidad JPA es una trampa:

- `@EqualsAndHashCode` usa **todos** los campos, incluido el `id`, que es `null` antes de
  guardar y cambia después. Si metiste la entidad en un `HashSet` antes de guardarla, después
  ya no la encuentras.
- `@ToString` recorre las relaciones. Con relaciones perezosas, un simple `log.info(entidad)`
  dispara consultas a la base o lanza `LazyInitializationException`. Con relaciones
  bidireccionales, recursión infinita y `StackOverflowError`.
- `@Setter` abre la entidad a modificaciones desde cualquier parte.

Por eso la entidad lleva **solo** `@Getter` y los constructores: lo que necesita, nada más.

## El costo: tu editor

Lombok modifica el código al compilar, así que el editor también tiene que saberlo, o te
marca en rojo cada getter que "no existe". Maven compila bien; es solo el editor.

**En Neovim** (jdtls) hay que arrancarlo con Lombok como *javaagent*. Mason ya trae el jar en
`~/.local/share/nvim/mason/packages/jdtls/lombok.jar`. En `~/.config/nvim/ftplugin/java.lua`,
dentro de `cmd`, justo antes de `"-jar", launcher,`, va esta línea:

```lua
"-javaagent:" .. pkg("jdtls") .. "/lombok.jar",
```

Y después `:JdtWipeDataAndRestart`. En IntelliJ y en VS Code con la extensión de Java, viene
resuelto.

Otra advertencia real: si ves un error rarísimo al ejecutar después de un build que dijo
`BUILD SUCCESS`, prueba `./mvnw clean package` en vez de solo `package`. Los annotation
processors a veces se quedan a medias en compilaciones incrementales.

## Configuración en el pom

Tres piezas, y cada una tiene su motivo:

- La dependencia con **`<optional>true</optional>`**: no se propaga a quien dependa de este
  proyecto.
- **`annotationProcessorPaths`** en el `maven-compiler-plugin`: declara Lombok como procesador
  de forma explícita. Los JDK nuevos están dejando de buscar procesadores solos en el
  classpath (desde el JDK 23 ya no lo hacen por defecto).
- **`<excludes>`** en el plugin de Spring Boot: que no se meta en el fat jar.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **Annotation processor** | Plugin del compilador que genera o modifica código al compilar |
| **Boilerplate** | Código repetitivo y sin lógica: getters, constructores triviales |
| **`@RequiredArgsConstructor`** | Constructor con los campos `final`: sigue siendo inyección por constructor |
| **`@Data`** | Combo de getters, setters, toString, equals/hashCode; peligroso en entidades JPA |
| **`@Slf4j`** | Declara el logger SLF4J de la clase |
| **SLF4J** | La fachada de logging; Logback es la implementación que trae Spring Boot |
| **Logging parametrizado** | `log.info("x={}", x)`: el texto solo se arma si el nivel está activo |

## Para la entrevista

> "Uso Lombok, pero con criterio. En entidades JPA pongo `@Getter` y los constructores, nunca
> `@Data`, porque su `equals` y `hashCode` con todos los campos rompe con ids generados y su
> `toString` dispara la carga perezosa. En servicios uso `@RequiredArgsConstructor`, que es
> inyección por constructor sin escribir el constructor. Y para DTOs uso records, porque ahí
> Lombok ya no aporta nada: el lenguaje lo resuelve."

**Siguiente:** Etapa 7 — un cliente HTTP hacia una API externa de tipo de cambio, con timeouts.
Es la puerta de entrada a la Fase B: resiliencia.
