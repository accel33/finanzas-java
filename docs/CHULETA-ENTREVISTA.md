# Chuleta de entrevista

Todo esto sale del código de este repo. Si te preguntan algo que no sabes, **dilo** y explica
cómo lo averiguarías: en banca se valora más eso que inventar.

---

## Lo que tienes que poder decir de memoria

### ¿Qué hace `@SpringBootApplication`?
Son tres anotaciones en una: **component scan** (busca clases anotadas desde su paquete hacia
abajo y las registra como beans), **auto-configuración** (mira el classpath y configura lo que
encuentra: si está Tomcat, levanta un servidor) y `@SpringBootConfiguration`.

### ¿Qué es un bean? ¿Qué es IoC?
Un **bean** es un objeto que crea y administra Spring, no tú con `new`. **IoC (Inversión de
Control)** es eso mismo: el framework construye y conecta los objetos; tú ya no llamas al
framework, él te llama a ti. La **inyección de dependencias** es la forma concreta: los
colaboradores te llegan como parámetros del constructor.

### ¿Por qué inyección por constructor y no por campo?
Tres razones, en orden de peso:
1. **El campo queda `final`**: nadie puede cambiar la dependencia después. Con `@Autowired`
   en el campo no se puede usar `final`.
2. **No se puede construir el objeto a medias**: si falta una dependencia, falla al arrancar,
   no en producción a las 3 de la mañana.
3. **Se puede testear sin Spring**: en el test haces `new MovimientoService(mockDelRepo)`,
   como cualquier objeto Java. Con inyección por campo tendrías que levantar Spring o usar
   reflexión para meter el mock.

Y no lleva `@Autowired`: desde Spring 4.3, si hay un solo constructor, se usa ese.

### ¿Cuál es el scope por defecto de un bean?
**Singleton**: una sola instancia por contenedor, compartida por todos los hilos. Por eso un
controller **nunca** guarda estado mutable en sus campos, y por eso mi almacén en memoria usa
`ConcurrentHashMap` y `AtomicLong`: con un `HashMap` y un `id++` normal, dos peticiones
simultáneas pueden corromperlo o repetir un id.

### ¿Qué pasa por debajo cuando llega un `GET /movimientos/1`?
1. **Tomcat** acepta la conexión, parsea el HTTP y asigna un hilo.
2. El **DispatcherServlet** (patrón **Front Controller**) recibe *todas* las peticiones.
3. El **RequestMappingHandlerMapping** busca en la tabla de rutas que se construyó al
   arrancar y encuentra el método.
4. Lo invoca **por reflexión** sobre el bean singleton.
5. Como el controller es `@RestController` (= `@Controller` + `@ResponseBody`), el retorno no
   es una vista: se hace **negociación de contenido** según la cabecera `Accept` y un
   `HttpMessageConverter` (Jackson) lo escribe como JSON.

### ¿Qué es un `record`?
Una clase cuyo único trabajo es llevar datos. Declaras los campos y el compilador genera
constructor, accesores, `equals`, `hashCode` y `toString`; los campos son `final`. Lo uso para
DTOs y objetos de valor. Es la alternativa moderna a las clases de datos con Lombok.

### ¿Cómo estructuras una aplicación?
En capas: **controller** (habla HTTP) → **service** (la lógica) → **repository** (guardar y
recuperar). Cada capa solo conoce a la de abajo. El repositorio está detrás de una
**interfaz**, así que el service depende del contrato y no de la implementación —eso es la
**D de SOLID**, inversión de dependencias— y por eso puedo cambiar de memoria a PostgreSQL
sin tocar el service, y mockearlo en los tests.

### ¿Cómo manejas los errores?
Con un `@RestControllerAdvice`: un manejador global por el que pasa cualquier excepción que
escape de cualquier controller. El service lanza una excepción de negocio y no sabe nada de
HTTP; el advice la traduce. El formato es **`ProblemDetail`**, que implementa el **RFC 7807**,
con `Content-Type: application/problem+json`. Que los errores sigan un estándar importa
porque quien consume la API programa contra un contrato.

### ¿Y la validación?
**Bean Validation** (Jakarta): las reglas van como anotaciones en el DTO de entrada
(`@NotBlank`, `@DecimalMin`, `@Digits`, `@PastOrPresent`) y se activan con **`@Valid`** en el
parámetro del controller. Sin `@Valid`, las anotaciones no hacen nada: es el error más común.
Devuelve todos los errores a la vez, no el primero.

### Códigos de estado
**201 Created** con cabecera `Location` al crear. **204 No Content** al borrar. **404** si el
id no existe. **400** si los datos no validan. **406** si el cliente pide un formato que no
sabes producir.

### PUT vs PATCH. ¿Qué es idempotencia?
**PUT reemplaza** el recurso completo; si omites un campo, queda nulo. **PATCH** modifica
parcialmente. **Idempotente** = repetir la operación deja el mismo estado: GET, PUT y DELETE
lo son; **POST no**, dos POST crean dos recursos.

### ¿Por qué `BigDecimal` y no `double` para dinero?
Porque `double` es binario de punto flotante y pierde centavos: `0.1 + 0.2` da
`0.30000000000000004`. En banca eso es inaceptable. `BigDecimal` es exacto y además es
**inmutable**: `a.add(b)` no modifica `a`, devuelve uno nuevo.

### ¿Cómo testeas?
Dos niveles. **Unitario**: construyo el service con `new` pasándole un **mock** del
repositorio (Mockito), sin levantar Spring — milisegundos. **Slice**: `@WebMvcTest` con
`MockMvc` levanta solo la capa web y prueba ruteo, validación y manejo de errores sin abrir
un puerto. Y pocos tests de integración `@SpringBootTest`, que son los caros.

### ¿Qué es el fat jar y el servidor embebido?
El build produce un jar de 20 MB que contiene mis clases, **34 jars de dependencias** y
Tomcat adentro. El servidor web dejó de ser un programa que instalas y pasó a ser una
librería que mi aplicación arranca. Antes desplegabas un **WAR** dentro de un servidor de
aplicaciones instalado (WebLogic, WebSphere, Tomcat standalone); ahora despliegas un proceso
que trae su servidor. Por eso entra directo en Docker: la imagen solo necesita un JRE y el
jar.

---

## Tres historias con número (valen más que la lista de tecnologías)

Lo que distingue "lo usé" de "lo entendí" es tener una anécdota medida. Estas son tuyas:

1. **El proceso zombi en el 8080.** "Estaba probando validaciones nuevas y no rechazaban nada.
   No era el código: había quedado un proceso viejo ocupando el puerto, el nuevo no arrancaba
   y mis `curl` le pegaban a la versión anterior. Desde entonces, cuando 'mis cambios no se
   aplican', lo primero que miro es `lsof -ti:8080`."

2. **JSON no está hardcodeado.** "Encendí los logs de Spring en TRACE y vi la línea
   `Using 'application/json', given [*/*] and supported [application/json]`. Que salga JSON se
   decide por negociación de contenido en cada petición. Lo comprobé pidiendo
   `Accept: application/xml` y la API respondió **406**, no un JSON."

3. **Los starters cambiaron de nombre en Boot 4.** "`spring-boot-starter-web` está deprecado a
   favor de `spring-boot-starter-webmvc`, porque con WebFlux 'web' se volvió ambiguo. Y en los
   tests, `@MockBean` ya no existe: ahora es `@MockitoBean`."

---

## Si te preguntan por algo que no tienes

Sé honesto y encuadra:

- **SAGA**: "sé qué problema resuelve: en microservicios no hay transacción distribuida, así
  que se hace con transacciones locales más compensaciones, y conviene orquestada cuando hay
  que auditar el flujo. No lo he implementado todavía."
- **Kafka / WebFlux / Circuit Breaker**: lo mismo — explica el problema que resuelven y di en
  qué punto estás.
- **TDD**: "tengo tests con JUnit y Mockito; TDD estricto lo estoy incorporando ahora."

Nunca inventes una experiencia. Un "no lo he hecho, pero sé para qué sirve y lo aprendo
rápido" es una respuesta perfectamente buena.
