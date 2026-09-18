# Etapa 3 — Capas, validación y errores

La Etapa 2 dejó tres cosas feas a propósito. Esta etapa las arregla, y de paso introduce la
estructura que vas a encontrar en cualquier proyecto Spring serio.

| Problema de la Etapa 2 | Cómo queda ahora |
|---|---|
| El controller recibía HTTP, guardaba datos y calculaba totales | Tres capas: controller → service → repository |
| Nadie validaba nada | `@Valid` + Bean Validation en el DTO de entrada |
| El mismo record entraba y salía | `MovimientoRequest` (entra) y `MovimientoResponse` (sale) |
| Los errores eran cuerpos vacíos | `ProblemDetail`, el formato estándar RFC 7807 |

## La estructura nueva

```
com.accel.finanzas
├── controller/    ← habla HTTP: rutas, códigos de estado, JSON
├── service/       ← la lógica: qué significa crear, reemplazar, no encontrar
├── repository/    ← guardar y recuperar, sin saber nada de HTTP
├── model/         ← el dominio: Movimiento, Resumen
├── dto/           ← lo que entra y lo que sale por la API
└── exception/     ← las excepciones propias y el traductor a respuestas HTTP
```

La regla que ordena todo esto: **cada capa solo conoce a la de abajo**. El controller llama
al service, el service al repository, y nunca al revés. El repository no sabe que existe HTTP;
el service tampoco. Si mañana esta aplicación se expusiera por gRPC o por una cola de Kafka,
solo cambiaría la capa de arriba.

Se llama **arquitectura en capas**, y la ganancia se ve comparando el mismo endpoint antes y
después:

```java
// Etapa 2: el controller decidía el 404 y hablaba con el mapa directamente
return Optional.ofNullable(movimientos.get(id))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

// Etapa 3: el controller solo traduce; el 404 nace de una excepción de negocio
return MovimientoResponse.desde(servicio.obtener(id));
```

## Inyección de dependencias, ahora sí visible

El controller ya no hace `new` de nada:

```java
private final MovimientoService servicio;

public MovimientoController(MovimientoService servicio) {
    this.servicio = servicio;
}
```

Spring ve ese constructor y le pasa el `MovimientoService` que ya creó; a su vez, al crear el
service le pasó el `MovimientoRepository`. Eso es la **inyección por constructor**, y es la
forma recomendada: los campos quedan `final` (nadie los puede cambiar después) y la clase es
imposible de construir a medias.

Fíjate en que **no hay ningún `@Autowired`**. Desde Spring 4.3, si la clase tiene un solo
constructor, se usa ese y punto. Ver `@Autowired` sobre campos en código nuevo es señal de
tutorial viejo.

### El repositorio es una interfaz, y eso es lo importante

```java
public interface MovimientoRepository { ... }

@Repository
public class MovimientoRepositoryEnMemoria implements MovimientoRepository { ... }
```

El service depende de la **interfaz**, no de la implementación. Eso es el principio de
**inversión de dependencias** (la D de SOLID), y aquí no es teoría: en la Etapa 5 la
implementación pasará a PostgreSQL y el service no cambiará ni una línea. En la Etapa 4, los
tests le pasarán un doble de prueba.

Está demostrado a mano en `aprendizaje/InyeccionExplicada.java`, que arma el service sin
Spring y le enchufa dos repositorios distintos:

```
1) Cableado a mano, igual que lo hace Spring al arrancar:
   total con el repositorio de verdad -> Resumen[cantidad=3, total=56.40]
2) El mismo servicio, con otro repositorio, sin tocar el servicio:
   total con el repositorio falso     -> Resumen[cantidad=1, total=100.00]
```

## Validación

Las reglas viven en el DTO de entrada, como anotaciones:

```java
@NotBlank(message = "La descripción es obligatoria")
@Size(max = 120, ...)
String descripcion,

@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
BigDecimal monto,

@NotNull @PastOrPresent
LocalDate fecha
```

Y se activan con `@Valid` en el parámetro del controller. Sin ese `@Valid`, las anotaciones
son decoración: nadie las mira. Es el error más común con Bean Validation.

Lo que devuelve un POST con todo mal, comprobado:

```json
{
  "status": 400,
  "title": "Datos inválidos",
  "detail": "Revisa los campos enviados",
  "instance": "/movimientos",
  "errores": {
    "fecha": "La fecha no puede estar en el futuro",
    "monto": "El monto debe ser mayor que cero",
    "descripcion": "La descripción es obligatoria"
  }
}
```

Fíjate en que informa **todos** los errores a la vez, no el primero. Y el `@Digits` con
`fraction = 2` rechaza `10.999`: en finanzas, un monto con tres decimales es un error de
origen, no algo que redondear en silencio.

## Errores con formato estándar

El `@RestControllerAdvice` es un interceptor global de excepciones: cualquier excepción que
escape de cualquier controller pasa por ahí y se convierte en respuesta HTTP. Por eso el
service puede lanzar `MovimientoNoEncontradoException` sin saber que eso será un 404 —
separación de responsabilidades otra vez.

El formato es **`ProblemDetail`**, la implementación de Spring del **RFC 7807 (Problem Details
for HTTP APIs)**. No es un JSON inventado: es un estándar con campos definidos (`type`,
`title`, `status`, `detail`, `instance`) al que puedes añadir los tuyos. Incluso la cabecera
cambia, y se puede verificar:

```
Content-Type: application/problem+json
```

Que una API de banco devuelva errores en un formato estándar y documentado importa mucho:
quien la consume puede programar contra un contrato, en vez de adivinar la forma del error de
cada endpoint.

## Lo que hay que saber comprobar

Al levantar la aplicación de nuevo, cuidado con esto —me pasó mientras armaba esta etapa:
si quedó un proceso viejo escuchando en el 8080, el nuevo **no arranca** y tus pruebas le
pegan a la versión anterior sin que nada avise. Los síntomas son desconcertantes: cambios que
"no se aplican", validaciones que no validan. Antes de pelearte con el código:

```bash
lsof -ti:8080 | xargs kill
```

## Lo que sigue estando mal a propósito

1. **No hay tests.** Todo lo de arriba lo verifiqué a mano con `curl`, que no se repite solo
   ni avisa cuando algo se rompe. Es la Etapa 4, y de ahí en adelante se escribe el test
   primero.
2. **Los datos se pierden al reiniciar.** El repositorio sigue siendo un mapa en memoria.
   Etapa 5.
3. **`ResumenResponse` es idéntico a `Resumen`.** Es una crítica válida: hoy ese DTO no
   aporta. Se mantiene por consistencia y porque en la Etapa 5 el modelo pasará a ser una
   entidad de base de datos, y entonces no querrás exponerla tal cual.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **Arquitectura en capas** | Controller / service / repository, cada una conociendo solo a la de abajo |
| **Inyección por constructor** | Las dependencias llegan como parámetros del constructor; campos `final`, sin `@Autowired` |
| **Inversión de dependencias (SOLID, la D)** | Depender de una interfaz, no de una implementación concreta |
| **Separación de responsabilidades** | Cada clase tiene un motivo para cambiar, y solo uno |
| **DTO vs modelo de dominio** | Lo que viaja por la API no tiene por qué ser lo que guardas |
| **Bean Validation (Jakarta)** | Las reglas como anotaciones; `@Valid` es lo que las hace correr |
| **`@RestControllerAdvice`** | Manejador global: traduce excepciones a respuestas HTTP |
| **RFC 7807 / `ProblemDetail`** | Formato estándar de errores, con `Content-Type: application/problem+json` |
| **Estereotipos** | `@Service`, `@Repository`, `@Controller`: todos son `@Component` con intención declarada |

## Para la entrevista

> "Separé en controller, service y repository, con el repositorio detrás de una interfaz. El
> service lanza una excepción de negocio cuando no encuentra el movimiento, y un
> `@RestControllerAdvice` la traduce a un 404 con `ProblemDetail`, que es el RFC 7807. Así el
> service no sabe nada de HTTP y el controller no sabe nada de almacenamiento: cuando cambié
> el repositorio de memoria a base de datos, el service no se tocó."

Si te preguntan **por qué inyección por constructor y no por campo**: porque deja los campos
`final`, hace imposible construir el objeto sin sus dependencias, y permite instanciarlo en un
test con un `new` normal, sin levantar Spring.

**Siguiente:** Etapa 4 — JUnit y Mockito, y a partir de ahí el test se escribe antes que el
código.
