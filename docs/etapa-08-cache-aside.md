# Etapa 8 — Cache Aside

La tasa de cambio se actualiza una vez al día, y la aplicación la estaba pidiendo en **cada**
petición. Ahora la guarda en memoria una hora. La primera consulta tarda lo que tarde el
proveedor; las siguientes, milisegundos.

## Medido

Contra el proveedor falso configurado para tardar 1 segundo:

| Petición | Tiempo |
|---|---|
| USD (primera) | 1.231 s |
| USD | **0.006 s** |
| EUR | **0.004 s** |
| USD | **0.004 s** |

Seis peticiones, **una sola** llamada al proveedor. Unas 300 veces más rápido. Y fíjate en el
EUR: también salió de la caché, porque lo que se guarda es **el mapa completo de tasas** de la
base PEN, no la tasa de una moneda. Una llamada sirve para las 166 monedas.

## El patrón

**Cache Aside** (o *lazy loading*) funciona así:

1. Busca en la caché.
2. Si está (**hit**), devuélvelo.
3. Si no está (**miss**), ve a la fuente.
4. Guárdalo en la caché y devuélvelo.

Se llama "aside" porque la caché está **al lado**: la aplicación la consulta y la llena ella
misma. La caché no sabe nada de la fuente. Está escrito a mano, paso a paso, en
`aprendizaje/CacheAsideExplicado.java`.

## En Spring: `@Cacheable`

```java
@Cacheable(cacheNames = "tasas", sync = true)
public Map<String, BigDecimal> tasas(String base) { ... }
```

Esa anotación hace los cuatro pasos por ti. La clave de la caché es el argumento (`base`), así
que `tasas("PEN")` se guarda una vez y se reutiliza.

Tres piezas más lo hacen funcionar:

```java
@Configuration
@EnableCaching
public class CacheConfig {}
```

```properties
spring.cache.cache-names=tasas
spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=1h
```

- **`@EnableCaching`** activa el mecanismo. Sin ella, `@Cacheable` se ignora en silencio.
- **Caffeine** es el almacén: una caché en memoria, dentro del mismo proceso, muy rápida.
- **`expireAfterWrite=1h`** es el **TTL** (*time to live*): una hora después de guardarse, la
  entrada caduca y la siguiente consulta vuelve al proveedor. Comprobado con un TTL de 3 s:
  la tercera consulta, 4 segundos después, volvió a tardar 1 s.
- **`maximumSize=100`** evita que la caché crezca sin límite.

### `sync = true`: el problema de la estampida

Imagina que la caché caduca y en ese mismo instante llegan 500 peticiones. Las 500 ven un
*miss* y las 500 van al proveedor a la vez. Eso es una **estampida** (*cache stampede* o
*thundering herd*), y puede tumbar al proveedor justo cuando más lo necesitas.

Con `sync = true`, la primera va al proveedor y las otras 499 **esperan** a que termine y usan
su resultado. Una sola llamada.

### Los errores no se guardan

Si el proveedor falla, `@Cacheable` no guarda nada: la excepción sale y la próxima petición lo
vuelve a intentar. Comprobado con el proveedor caído: tres intentos, tres llamadas. Es lo
correcto —no quieres que un fallo momentáneo quede grabado una hora— pero tiene una
consecuencia: con el proveedor caído, **cada** petición sigue intentándolo. Eso lo arregla el
circuit breaker, en la Etapa 9.

---

## La trampa: autoinvocación

Esto pasó de verdad al hacer esta etapa, y es de las preguntas más clásicas de Spring.

La primera versión era así:

```java
public BigDecimal tasa(String base, String destino) {
    BigDecimal tasa = tasas(base).get(destino);     // ← llama a tasas() desde dentro
    ...
}

@Cacheable(cacheNames = "tasas", sync = true)
public Map<String, BigDecimal> tasas(String base) { ... }
```

Compilaba, arrancaba sin un solo warning, y **la caché no funcionaba**:

```
USD -> 200 en 1.25s
USD -> 200 en 1.02s
EUR -> 200 en 1.02s
USD -> 200 en 1.02s
llamadas que recibió el proveedor: 4
```

### Por qué

Cuando Spring ve un `@Cacheable`, el bean que inyecta en el service **no es tu clase**: es un
**proxy**, una subclase que Spring genera en tiempo de ejecución y que envuelve a tu objeto.
Hay un test que lo confirma:

```java
assertThat(AopUtils.isCglibProxy(cliente)).isTrue();
assertThat(cliente.getClass().getName()).contains("SpringCGLIB");
```

El proxy intercepta las llamadas que **entran desde fuera**: el service llama a `tasas()`, la
llamada pasa por el proxy, el proxy mira la caché. Pero cuando `tasa()` llama a `tasas()`,
lo hace sobre `this`, y `this` es **el objeto real**, no el proxy. La llamada nunca sale del
objeto, así que el proxy no se entera y la caché no se consulta.

```
service ──► [ PROXY: mira la caché ] ──► tasas()        ✔ pasa por el proxy
service ──► [ PROXY ] ──► tasa() ──► this.tasas()       ✘ se salta el proxy
```

### Cómo se arregló

Que el service llame a `tasas()` directamente, a través del proxy, y elija la moneda él:

```java
Map<String, BigDecimal> tasas = tipoDeCambio.tasas(MONEDA_BASE);
BigDecimal tasa = tasas.get(destino);
```

Resultado: una llamada al proveedor en vez de cuatro.

### Por qué esto importa más allá de la caché

**La misma trampa aplica a `@Transactional`**, `@Async` y cualquier anotación que Spring
implemente con proxies. Un método `@Transactional` llamado desde otro método del mismo bean
**no abre transacción**, y no te avisa. Es un bug silencioso que aparece en producción.

La regla: *las anotaciones de Spring que agregan comportamiento solo funcionan cuando la
llamada viene de otro bean*.

## Caché local vs distribuida

Caffeine vive **dentro** del proceso. Si mañana hay tres instancias de la aplicación, hay tres
cachés independientes, y cada una llamará al proveedor una vez por hora. Para tasas de cambio
eso está perfecto.

Cuando eso no sirve —datos que cambian y tienen que verse igual en todas las instancias— se
usa una caché **distribuida** y compartida: **Redis**, que en Azure es *Azure Cache for Redis*.
Gracias a la abstracción `@Cacheable`, cambiar de Caffeine a Redis es cambiar la dependencia y
la configuración, **sin tocar el código**.

## Lo que decidí no cachear

`GET /movimientos` y el resumen **no** están en caché, a propósito. Son datos propios que
cambian con cada POST, PUT o DELETE. Cachearlos obliga a **invalidar** en cada escritura con
`@CacheEvict`, y olvidarse de una invalidación significa mostrar datos viejos. Se cachea lo que
es caro de obtener y cambia poco. Una consulta a tu propia base de datos por clave primaria no
cumple ninguna de las dos.

Hay una frase célebre sobre esto: *"hay solo dos cosas difíciles en informática: invalidar
cachés y ponerle nombre a las cosas"*.

## Tests: 16

- `TipoDeCambioCacheTest` levanta un contexto de Spring mínimo con la caché activa y exige que
  dos consultas produzcan **una sola** petición HTTP (`ExpectedCount.once()`). Si alguien
  rompe la caché, falla.
- También confirma que el bean inyectado es un proxy CGLIB.
- La validación de moneda pasó del client al service, y su test con ella.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **Cache Aside** | La aplicación consulta la caché; si falta, va a la fuente y la guarda |
| **Hit / miss** | Estaba en la caché / no estaba |
| **TTL** | Cuánto vive una entrada antes de caducar |
| **`@Cacheable`** | Aplica Cache Aside a un método, usando sus argumentos como clave |
| **`@CacheEvict`** | Borra entradas, para invalidar cuando el dato cambia |
| **Caffeine** | Caché en memoria, local al proceso |
| **Redis** | Caché distribuida y compartida entre instancias |
| **Estampida (*stampede*)** | Muchos *miss* simultáneos que golpean la fuente a la vez; se evita con `sync = true` |
| **Proxy (AOP)** | Subclase generada en tiempo de ejecución que envuelve tu bean y añade comportamiento |
| **Autoinvocación** | Llamar a un método anotado desde la misma clase: se salta el proxy y la anotación no actúa |

## Para la entrevista

> "Cacheé las tasas de cambio con `@Cacheable` y Caffeine, con TTL de una hora y `sync = true`
> para evitar la estampida cuando caduca. Guardo el mapa completo por base, así una llamada
> sirve para todas las monedas: pasé de 1.2 segundos a 4 milisegundos. Y me pasó la trampa
> clásica: al principio el método cacheado lo llamaba otro método de la misma clase, y la caché
> no funcionaba —cuatro peticiones, cuatro llamadas al proveedor—, porque la llamada interna
> se salta el proxy. Es exactamente lo mismo que pasa con `@Transactional`."

Si te preguntan **qué cachearías y qué no**: lo caro de obtener y que cambia poco. No los datos
propios que cambian con cada escritura, porque cada escritura obliga a invalidar, y una
invalidación olvidada es un bug de datos viejos.

**Siguiente:** Etapa 9 — Circuit Breaker. Con el proveedor caído, cada petición sigue
intentándolo; el circuit breaker aprende a dejar de intentarlo por un rato.
