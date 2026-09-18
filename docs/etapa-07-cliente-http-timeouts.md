# Etapa 7 — Cliente HTTP hacia una API externa, con timeouts

Hasta ahora la aplicación solo dependía de cosas propias: su código y su base de datos. Ahora
depende de **un servicio de otra gente**, y eso cambia las reglas: ese servicio se puede caer,
se puede poner lento, y tú no controlas nada de eso. Esta etapa es la puerta de entrada a la
**Fase B: resiliencia**.

## Qué hace

Un endpoint nuevo convierte el total de movimientos a otra moneda, consultando el tipo de
cambio del día en `open.er-api.com` (gratis, sin clave, incluye soles):

```bash
curl -s http://localhost:8080/movimientos/resumen/USD
```

```json
{"cantidad":4,"totalSoles":64.90,"moneda":"USD","tasa":0.296909,"totalConvertido":19.27}
```

## Las piezas

```
config/TipoDeCambioProperties   ← la configuración, tipada
config/TipoDeCambioConfig       ← arma el RestClient con sus timeouts
client/TipoDeCambioClient       ← hace la llamada y traduce los errores
client/RespuestaTasas           ← la forma del JSON del proveedor
```

### `RestClient`: el cliente HTTP de hoy

Spring tiene tres clientes HTTP, y conviene saber ubicarlos:

| Cliente | Estado | Modelo |
|---|---|---|
| `RestTemplate` | En mantenimiento: no recibe funciones nuevas | Síncrono |
| **`RestClient`** | **El recomendado** desde Spring 6.1 | Síncrono, API fluida |
| `WebClient` | Para aplicaciones reactivas (WebFlux) | Reactivo, `Mono`/`Flux` |

```java
tipoDeCambioRestClient.get()
        .uri("/latest/{base}", base)
        .retrieve()
        .body(RespuestaTasas.class);
```

En Boot 4 tiene **su propio starter**, `spring-boot-starter-restclient`. Sin él, no existe el
bean `RestClient.Builder` y la aplicación no arranca.

### Configuración tipada con `@ConfigurationProperties`

```properties
tipo-de-cambio.url=https://open.er-api.com/v6
tipo-de-cambio.timeout-conexion=2s
tipo-de-cambio.timeout-lectura=3s
```

```java
@ConfigurationProperties(prefix = "tipo-de-cambio")
public record TipoDeCambioProperties(String url, Duration timeoutConexion, Duration timeoutLectura) {}
```

Spring lee las propiedades que empiezan con `tipo-de-cambio` y las mete en el record. Fíjate
en dos detalles: `timeout-lectura` se convierte solo en `timeoutLectura` (*relaxed binding*),
y `3s` se convierte en un `Duration` de 3 segundos. Es mejor que `@Value("${...}")` repartido
por todo el código: está en un solo lugar, tiene tipos, y falla al arrancar si algo no encaja.

Y cualquier propiedad se puede pisar al arrancar, sin tocar el archivo:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--tipo-de-cambio.timeout-lectura=10s
```

### Los dos timeouts, y por qué son distintos

```java
HttpClient.newBuilder().connectTimeout(propiedades.timeoutConexion())
fabrica.setReadTimeout(propiedades.timeoutLectura());
```

- **Timeout de conexión** (2 s): cuánto esperar a que el servidor **acepte** la conexión.
- **Timeout de lectura** (3 s): una vez conectado, cuánto esperar **la respuesta**.

Son fallos distintos. Un servidor apagado rechaza la conexión al instante. Un servidor
sobrecargado acepta la conexión y luego no contesta nunca. El segundo es el peligroso.

### El JSON del proveedor: lector tolerante

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaTasas(
        String result,
        @JsonProperty("base_code") String base,
        ...
        Map<String, BigDecimal> rates) {}
```

El proveedor manda más campos de los que usamos (`provider`, `documentation`...).
`ignoreUnknown = true` hace que los ignore, y así **si mañana agregan un campo, no te rompe**.
Es el patrón *Tolerant Reader*: sé estricto con lo que envías y tolerante con lo que recibes.

`@JsonProperty("base_code")` traduce el nombre en *snake_case* del proveedor a tu campo. Y
declarar `rates` como `Map<String, BigDecimal>` hace que Jackson lea `0.296909` directo del
texto a `BigDecimal`, **sin pasar por `double`**: ni un decimal se pierde en el camino.

### Los errores del proveedor no son tus errores

El client atrapa cualquier `RestClientException` —timeout, conexión rechazada, un 500 del
proveedor— y la convierte en `TipoDeCambioNoDisponibleException`. El advice la traduce a
**503 Service Unavailable**.

Esto importa: si dejaras escapar la excepción, el cliente de tu API recibiría un **500**, que
significa "mi código tiene un bug". Un 503 dice la verdad: "estoy bien, una dependencia mía no
responde, reintenta en un rato".

### Redondeo bancario

```java
resumen.total().multiply(tasa).setScale(2, RoundingMode.HALF_EVEN);
```

`HALF_EVEN` es el **redondeo bancario**: cuando el número está justo en la mitad, redondea al
par. `2.125` queda en `2.12`, no en `2.13`. Con `HALF_UP` —el que te enseñan en el colegio—
todos los empates suben, y sobre millones de operaciones eso acumula un sesgo sistemático a
favor de alguien. Hay un test que lo fija: `redondeoBancario()`.

---

## El laboratorio: qué pasa cuando el proveedor falla

`aprendizaje/ServidorLento.java` imita al proveedor pero tarda lo que le digas. Con él medí
los cuatro escenarios:

| Escenario | Respuesta | Tiempo |
|---|---|---|
| API real | 200 | **0.63 s** (0.12 s las siguientes) |
| Proveedor **caído** | 503 | **0.17 s** |
| Proveedor **lento** (10 s), timeout de 3 s | 503 | **3.18 s** |
| Proveedor **lento** (10 s), timeout de 30 s | 200 | **10.21 s** |

Lo que enseña la tabla, y es la idea más importante de toda la Fase B:

**Un proveedor caído es barato; uno lento es carísimo.** Caído, fallas en 0.17 s. Lento y sin
un timeout razonable, cada petición a tu API se queda **10 segundos** esperando, con un hilo de
Tomcat retenido todo ese tiempo. Tomcat tiene 200 hilos por defecto: con 200 usuarios pidiendo
la conversión a la vez, el usuario 201 no consigue ni ver su lista de movimientos, aunque eso
no tenga nada que ver con el tipo de cambio. **Una dependencia lenta tumba tu aplicación
entera.** Eso se llama *fallo en cascada*.

El timeout convierte "lento" en "caído": en 3.18 s te rindes y liberas el hilo.

Pero fíjate en lo que todavía falta. Con el proveedor lento, **cada** petición sigue pagando sus
3 segundos, una y otra vez, aunque ya sepas que el proveedor está mal. Lo ideal sería dejar de
intentarlo por un rato. Eso es exactamente un **circuit breaker**, en la Etapa 9. Y de paso,
cada conversión llama al proveedor aunque la tasa cambie una vez al día: eso lo resuelve la
**caché**, en la Etapa 8.

Para repetir el laboratorio:

```bash
java -cp target/classes com.accel.finanzas.aprendizaje.ServidorLento 10
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--tipo-de-cambio.url=http://localhost:9099
```

```bash
curl -s -w '\n%{time_total}s\n' http://localhost:8080/movimientos/resumen/USD
```

## Los tests nuevos

Ahora son **14**. Los importantes:

- `TipoDeCambioClientTest` usa **`MockRestServiceServer`**: un proveedor falso que vive dentro
  del test. Le dices qué petición esperar y qué responder —incluso un 500— y compruebas que tu
  client reacciona bien. Sin red, sin depender de que internet funcione.
- `redondeoBancario()` fija que 2.125 → 2.12.
- `tipoDeCambioCaidoDevuelve503()` fija que un proveedor caído es 503 y no 500.

### El compilador como red de seguridad

Al agregar el `TipoDeCambioClient` al constructor del service, **el test existente dejó de
compilar**:

```
constructor MovimientoService cannot be applied to given types;
  required: MovimientoRepository, TipoDeCambioClient
  found:    MovimientoRepository
```

Esa es la inyección por constructor pagando dividendos. Con `@Autowired` sobre un campo, el
test habría compilado, y el `NullPointerException` habría aparecido en producción, la primera
vez que alguien pidiera una conversión.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **`RestClient`** | Cliente HTTP síncrono y fluido; el recomendado en vez de `RestTemplate` |
| **Timeout de conexión / de lectura** | Esperar a que acepten la conexión / esperar la respuesta |
| **Fallo en cascada** | Una dependencia lenta agota tus hilos y tumba funciones que no dependían de ella |
| **`@ConfigurationProperties`** | Configuración tipada y agrupada, en vez de `@Value` suelto |
| **Relaxed binding** | `timeout-lectura` en el archivo = `timeoutLectura` en Java |
| **Tolerant Reader** | Ignorar los campos desconocidos para que un cambio del proveedor no te rompa |
| **503 vs 500** | 503: una dependencia no responde. 500: tu código falló |
| **`RoundingMode.HALF_EVEN`** | Redondeo bancario: los empates van al par, sin sesgo acumulado |
| **`MockRestServiceServer`** | Servidor HTTP falso dentro del test |
| **Virtual threads** | Hilos baratos de la JVM (Java 21); los usa el servidor del laboratorio |

## Para la entrevista

> "Consumo una API externa de tipo de cambio con `RestClient`, con timeout de conexión y de
> lectura configurados por `@ConfigurationProperties`. Lo medí con un proveedor falso que
> tardaba 10 segundos: sin timeout razonable, cada petición a mi API tardaba 10.2 segundos
> con un hilo de Tomcat retenido; con timeout de 3, fallaba en 3.2 y devolvía un 503 limpio.
> Lo que aprendí es que una dependencia caída es barata —falla en 170 ms— y una lenta es la
> peligrosa, porque agota el pool de hilos y te tumba la aplicación entera."

Si te preguntan **qué código devuelves cuando falla una dependencia**: 503, no 500. El 500
dice que tu código tiene un bug; el 503 dice que un servicio del que dependes no está
disponible y que conviene reintentar.

**Siguiente:** Etapa 8 — Cache Aside: la tasa cambia una vez al día, no tiene sentido pedirla
en cada petición.
