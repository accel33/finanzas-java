# Etapa 2 — CRUD de movimientos en memoria

Ya hay un endpoint que saluda. Ahora hay un recurso de verdad: se puede crear, leer,
actualizar y borrar movimientos. Todo vive en memoria, así que se pierde al reiniciar; la
persistencia llega en la Etapa 5.

## Lo que existe ahora

| Verbo | Ruta | Qué hace | Respuesta |
|---|---|---|---|
| GET | `/movimientos` | Lista todo, ordenado por id | 200 + array |
| GET | `/movimientos/resumen` | Cuántos hay y cuánto suman | 200 + objeto |
| GET | `/movimientos/{id}` | Uno solo | 200, o **404** si no existe |
| POST | `/movimientos` | Crea uno; el id lo asigna el servidor | **201** + cabecera `Location` |
| PUT | `/movimientos/{id}` | Reemplaza uno existente | 200, o **404** |
| DELETE | `/movimientos/{id}` | Lo borra | **204**, o **404** |

Pruébalo con la app levantada (`./mvnw spring-boot:run`):

```bash
curl -s http://localhost:8080/movimientos
```

```bash
curl -i -X POST http://localhost:8080/movimientos -H 'Content-Type: application/json' -d '{"descripcion":"Café","monto":"8.50","fecha":"2026-09-17"}'
```

Arranca con tres movimientos de ejemplo cargados, para que el primer GET no devuelva una
lista vacía.

## Las decisiones de diseño, y por qué

### Los códigos de estado no son decoración

Es lo primero que se mira en una API de banco, y lo que más se pregunta en entrevistas.
La regla es que **el código HTTP ya cuenta la historia**, sin que el cliente tenga que leer
el cuerpo de la respuesta:

- **201 Created** en el POST, no 200. Y acompañado de la cabecera `Location: /movimientos/4`,
  que le dice al cliente dónde quedó lo que acaba de crear. Es lo que separa un POST bien
  hecho de uno que solo "devuelve OK".
- **204 No Content** en el DELETE: se hizo, y no hay nada que devolver. Mandar un `200` con
  un cuerpo vacío es contradictorio.
- **404 Not Found** cuando el id no existe, en GET, PUT y DELETE por igual.

Para poder elegir el código a mano, esos métodos devuelven `ResponseEntity<T>` en vez del
objeto pelado: es el sobre que envuelve cuerpo + estado + cabeceras. Los métodos que siempre
responden 200 (`listar`, `resumen`) devuelven el objeto directo, porque envolverlo no
aportaría nada.

### PUT es reemplazar, no modificar

`PUT /movimientos/1` sustituye el movimiento completo por el que mandas. Si omites un campo,
queda nulo: eso es lo que significa PUT. El "cambiar solo un campo" es **PATCH**, otro verbo,
y no está implementado a propósito.

Además, el `id` del cuerpo se ignora: manda el de la URL. Si el cliente pudiera renombrar el
id desde el cuerpo, tendrías dos fuentes de verdad para lo mismo.

### La ruta literal le gana a la variable

`/movimientos/resumen` y `/movimientos/{id}` compiten por la misma forma de URL. Spring
resuelve el conflicto prefiriendo **lo más específico**: la ruta literal gana, así que
`resumen` nunca se interpreta como un id. Verificado: devuelve el resumen, no un 404.

Es un detalle que muerde cuando el orden importa en otros frameworks. Aun así, mezclar
sustantivos-recurso con palabras-acción en el mismo nivel de la URL es discutible; una API
más estricta pondría eso en `/movimientos/estadisticas` o fuera del recurso.

### El almacenamiento es concurrente, y no por capricho

```java
private final Map<Long, Movimiento> movimientos = new ConcurrentHashMap<>();
private final AtomicLong siguienteId = new AtomicLong(1);
```

Acuérdate de lo que vimos en los logs de la Etapa 1: Spring crea **una sola instancia** del
controller y la comparten todos los hilos de Tomcat. Un `HashMap` normal aquí se corrompe si
dos peticiones escriben a la vez, y `id++` puede darle el mismo id a dos movimientos, porque
leer-sumar-escribir no es atómico. `ConcurrentHashMap` y `AtomicLong` resuelven justo eso.

Este es exactamente el tipo de respuesta que distingue en una entrevista: no "usé un mapa",
sino "usé un mapa concurrente **porque** el bean es singleton y lo atienden varios hilos".

## Lo que está mal a propósito

Esta etapa deja tres cosas feas, y cada una tiene dueño en el roadmap:

1. **El controller hace de todo**: recibe HTTP, guarda datos y calcula totales. Son tres
   responsabilidades en una clase. La Etapa 3 las separa en controller / service / repository.
2. **Nadie valida nada.** Puedes mandar un monto negativo, una descripción vacía o un JSON sin
   fecha y entra igual. La Etapa 3 trae `@Valid` y el manejo de errores.
3. **El mismo record entra y sale.** `Movimiento` hace de cuerpo de la petición y de respuesta,
   por eso el POST recibe un `id` que hay que ignorar. Lo correcto es separar el DTO de
   entrada del de salida — también en la Etapa 3.

Se hace así a propósito: primero sientes el problema, después lo arreglas. Refactorizar algo
que ya funciona y ya te molesta enseña mucho más que empezar con la estructura perfecta
copiada de un tutorial.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **CRUD** | Create, Read, Update, Delete: las cuatro operaciones básicas sobre un recurso |
| **Recurso** | El sustantivo que expone tu API (`/movimientos`); las acciones las dan los verbos HTTP |
| **Verbo HTTP** | GET lee, POST crea, PUT reemplaza, PATCH modifica en parte, DELETE borra |
| **Idempotencia** | Repetir la operación da el mismo resultado. GET, PUT y DELETE lo son; POST no: dos POST crean dos movimientos |
| **`ResponseEntity`** | El sobre que te deja controlar cuerpo, código de estado y cabeceras |
| **`@PathVariable`** | Toma un trozo de la URL (`/movimientos/2` → `id = 2`) |
| **`@RequestBody`** | Convierte el JSON del cuerpo en un objeto Java (lo hace Jackson) |
| **`@RequestMapping` en la clase** | Prefijo común para todas las rutas del controller |
| **`Optional`** | Caja que puede venir vacía; hace visible en el tipo que algo puede no existir |
| **Singleton + concurrencia** | Un bean, muchos hilos: el estado compartido tiene que ser thread-safe |

## Para la entrevista

> "En el CRUD lo que cuidé fueron los códigos de estado: 201 con `Location` al crear, 204 en
> el delete, 404 cuando el id no existe. Y el almacenamiento lo hice con `ConcurrentHashMap`
> y `AtomicLong` porque el controller es un bean singleton que atienden varios hilos de
> Tomcat a la vez: con un `HashMap` y un `id++` normal, dos peticiones simultáneas pueden
> corromper el mapa o repetir un id."

Si te preguntan **la diferencia entre PUT y PATCH**: PUT reemplaza el recurso completo y es
idempotente; PATCH aplica una modificación parcial. Mandar un PUT sin un campo lo deja nulo,
y eso es correcto, no un bug.

## Material de aprendizaje

La línea más densa del controller está desarmada paso a paso en
`aprendizaje/OptionalExplicado.java`, en tres versiones: con `null` a secas, con `Optional`
preguntando, y con `Optional` encadenado (la del controller).

```bash
java -cp target/classes com.accel.finanzas.aprendizaje.OptionalExplicado
```

**Siguiente:** Etapa 3 — separar en capas, validar la entrada y devolver errores decentes.
