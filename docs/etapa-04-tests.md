# Etapa 4 — Tests con JUnit y Mockito

Hasta ahora todo se comprobaba a mano con `curl`. Eso no se repite solo ni avisa cuando algo
se rompe. Ahora hay **8 tests** que corren en 1.1 segundos:

```bash
./mvnw test
```

## Dos tipos de test, y saber cuál es cuál importa

### Test unitario: `MovimientoServiceTest`

Prueba **una sola clase**, sin Spring, sin servidor, sin base de datos. Tarda 57 ms.

```java
@Mock private MovimientoRepository repositorio;

@BeforeEach
void prepararServicio() {
    servicio = new MovimientoService(repositorio);
}
```

Fíjate en ese `new`: **es un objeto Java normal**. Esto es exactamente para lo que sirve la
inyección por constructor — si el repositorio se inyectara en un campo con `@Autowired`,
aquí habría que levantar Spring o usar reflexión para meterlo.

El **mock** es un doble de prueba: un objeto falso que finge ser el repositorio y responde lo
que tú le digas.

```java
given(repositorio.buscarPorId(99L)).willReturn(Optional.empty());

assertThatThrownBy(() -> servicio.obtener(99L))
        .isInstanceOf(MovimientoNoEncontradoException.class);
```

Traducido: "cuando te pregunten por el 99, di que no hay nada" y luego "comprueba que el
servicio reacciona lanzando la excepción". Se prueba la lógica del servicio **aislada** de
dónde se guardan los datos.

También se usa un `ArgumentCaptor`, que intercepta lo que el servicio le pasó al repositorio,
para verificar que el id que manda el cliente se ignora:

```java
verify(repositorio).guardar(capturado.capture());
assertThat(capturado.getValue().id()).isNull();
```

### Test de slice: `MovimientoControllerTest`

Levanta **solo la capa web** (controllers, validación, manejador de errores), no la
aplicación entera. El servicio va mockeado.

```java
@WebMvcTest(MovimientoController.class)
class MovimientoControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MovimientoService servicio;
```

`MockMvc` simula peticiones HTTP sin abrir un puerto de verdad, así que es rapidísimo:

```java
mockMvc.perform(get("/movimientos/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Movimiento no encontrado"));
```

Esto prueba lo que un test unitario no puede: que la ruta está bien mapeada, que la
validación rechaza, que el `@RestControllerAdvice` traduce la excepción, y que el JSON sale
con la forma acordada.

> **Detalle de versión que te hace ver actualizado:** `@MockBean` quedó deprecado y **en
> Spring Boot 4 ya no existe**. El reemplazo es `@MockitoBean`, y vive en Spring Framework,
> no en Boot. Casi todos los tutoriales siguen diciendo `@MockBean`.
>
> Y otro: en Boot 4 el slice `@WebMvcTest` se movió a su propio módulo,
> `spring-boot-starter-webmvc-test`. Con solo `spring-boot-starter-test` no compila.

## La pirámide de tests

| Tipo | Qué levanta | Velocidad | Cuántos tener |
|---|---|---|---|
| Unitario | Nada | milisegundos | Muchos |
| Slice (`@WebMvcTest`, `@DataJpaTest`) | Una capa | décimas | Los justos |
| Integración (`@SpringBootTest`) | Toda la app | segundos | Pocos |

La regla es tener muchos de los baratos y pocos de los caros. Una suite que tarda 10 minutos
deja de correrse, y una suite que no se corre no sirve de nada.

## Sobre TDD

Estos tests se escribieron **después** del código, así que esto no fue TDD. TDD es el ciclo
**rojo → verde → refactor**: escribes el test, lo ves fallar (rojo), escribes lo mínimo para
que pase (verde), y luego limpias. De la Etapa 5 en adelante se hace en ese orden.

Decirlo honestamente en una entrevista suma: "tengo tests con JUnit y Mockito; TDD estricto
lo empecé a practicar después, y noto la diferencia en que el diseño sale más testeable".

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **JUnit** | El motor que encuentra y ejecuta los tests (aquí, JUnit 6) |
| **Mockito** | La librería que fabrica dobles de prueba |
| **Mock / doble de prueba** | Objeto falso que finge ser una dependencia y responde lo que le digas |
| **`given / willReturn`** | Programar la respuesta del mock (estilo BDD de Mockito) |
| **`verify`** | Comprobar que se llamó a algo, y con qué |
| **`ArgumentCaptor`** | Atrapa el argumento que recibió el mock para poder inspeccionarlo |
| **AssertJ** | Las aserciones legibles: `assertThat(x).isEqualTo(y)` |
| **`MockMvc`** | Simula peticiones HTTP sin levantar un servidor real |
| **Slice test** | Levanta solo una porción del contexto de Spring |
| **`@MockitoBean`** | Sustituye un bean del contexto por un mock (antes `@MockBean`) |
| **TDD** | Rojo → verde → refactor: el test primero |

## Para la entrevista

> "Separo tests unitarios de tests de capa web. El del servicio no levanta Spring: construyo
> la clase con `new` pasándole un mock del repositorio, y eso solo es posible porque uso
> inyección por constructor. El del controller usa `@WebMvcTest` con `MockMvc`, que prueba
> ruteo, validación y el manejo de errores sin abrir un puerto. En Boot 4 el mock de bean ya
> no es `@MockBean` sino `@MockitoBean`."

**Siguiente:** Etapa 5 — PostgreSQL con Docker y JPA, y ahí sí empezando por el test.
