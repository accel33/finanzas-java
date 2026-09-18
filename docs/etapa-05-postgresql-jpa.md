# Etapa 5 — PostgreSQL con Docker, JPA y Flyway

Los datos ya no se pierden al reiniciar. Y lo importante: **el service no cambió ni una
línea**. Esa era la promesa de la Etapa 3, y se cumplió.

## Cómo correrlo ahora

La base corre en Docker, así que hay un paso nuevo antes de levantar la app:

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

Para apagar la base sin perder los datos: `docker compose down`. Para borrarlo todo y
empezar de cero: `docker compose down -v` (la `-v` elimina el volumen donde viven los datos).

---

## ¿En qué momento la interfaz se "convierte" en una implementación?

**Al arrancar, y lo hace Spring.** No hay ninguna línea de tu código que diga "usa esta
implementación". Spring lo resuelve **por tipo**, y el momento exacto sale en los logs con
el nivel DEBUG:

```
Creating shared instance of singleton bean 'movimientoController'
Creating shared instance of singleton bean 'movimientoService'
Creating shared instance of singleton bean 'movimientoRepositoryJpa'
Autowiring by type from bean name 'movimientoService' via constructor to bean named 'movimientoRepositoryJpa'
```

Léelo como Spring lo piensa:

1. "Voy a crear el controller. Su constructor pide un `MovimientoService`. Todavía no existe:
   lo creo primero."
2. "Voy a crear el service. Su constructor pide un `MovimientoRepository`. Busco entre todos
   mis beans uno que **sea de ese tipo**."
3. "`MovimientoRepositoryJpa` implementa `MovimientoRepository`, así que es de ese tipo. Es
   el único. Lo creo y se lo paso."

En la Etapa 4 esa última línea decía `movimientoRepositoryEnMemoria`. Lo único que cambió es
**qué clase tiene la anotación `@Repository`**.

Para verlo tú:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--logging.level.org.springframework.beans.factory=DEBUG
```

### ¿Y si hay dos candidatos?

Pasó de verdad al hacer esta etapa: agregué la implementación JPA sin quitar la de memoria, y
la aplicación **no arrancó**:

```
Parameter 0 of constructor in com.accel.finanzas.service.MovimientoService
required a single bean, but 2 were found:
    - movimientoRepositoryEnMemoria
    - movimientoRepositoryJpa
```

Spring prefiere no arrancar antes que elegir al azar. Las salidas, y cuándo usar cada una:

| Opción | Cómo | Cuándo |
|---|---|---|
| Que haya solo uno | Quitar la anotación del que sobra | Cuando el otro ya no hace falta (**lo que hice aquí**) |
| `@Primary` | Sobre el que debe ganar por defecto | Hay uno "normal" y otro para casos especiales |
| `@Qualifier("nombre")` | En el parámetro que lo recibe | Distintos consumidores necesitan distintas implementaciones |
| `@Profile("dev")` | Cada implementación en un perfil | Una para desarrollo local, otra para producción |

`MovimientoRepositoryEnMemoria` sigue existiendo, pero como clase Java normal: ya no es un
bean. La usa `aprendizaje/InyeccionExplicada.java`, que hace `new` a mano.

---

## Las piezas nuevas

### La entidad NO puede ser un record

```java
@Entity
@Table(name = "movimientos")
public class MovimientoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    ...
    protected MovimientoEntity() {}
```

JPA (Hibernate) necesita tres cosas que un record prohíbe: un **constructor sin argumentos**
para instanciarla al leer de la base, que la clase **no sea `final`** (para crear *proxies*,
subclases que generan en tiempo de ejecución para la carga perezosa) y **campos que pueda
escribir**. Un record es final, inmutable y sin constructor vacío.

Por eso ahora hay dos clases para lo mismo: `Movimiento` (record, el dominio, lo que usa el
service) y `MovimientoEntity` (clase, lo que se guarda). Y mira cuántas líneas de getters
tiene la entidad: eso es exactamente lo que resuelve Lombok, en la Etapa 6.

### Spring Data: una interfaz sin implementación

```java
public interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {}
```

Una línea, sin cuerpo, y ya tienes `findAll`, `findById`, `save`, `deleteById`,
`existsById`, paginación y ordenamiento. **Nadie escribió la implementación**: Spring Data la
genera en tiempo de ejecución, al arrancar, como un *proxy*. Si agregas un método llamado
`findByDescripcionContaining(String texto)`, Spring Data deduce la consulta SQL del nombre.

### El adaptador: por qué hay dos repositorios

`MovimientoJpaRepository` habla de `MovimientoEntity`. El service habla de `Movimiento`.
`MovimientoRepositoryJpa` los conecta: implementa **nuestra** interfaz usando la de Spring
Data por dentro, y traduce entre entidad y modelo.

Es el **patrón Adapter**, y es la razón de que el service no se enterara del cambio. Si el
service dependiera directamente de `JpaRepository`, cambiar de base de datos (o mockearla)
tocaría la lógica de negocio.

### Flyway: el esquema versionado

```
src/main/resources/db/migration/
├── V1__crear_tabla_movimientos.sql
└── V2__datos_de_ejemplo.sql
```

Cada cambio de esquema es un archivo SQL numerado, que va en git como el código. Flyway los
aplica en orden y anota cuáles ya corrió en la tabla `flyway_schema_history`:

```
 version |       description       | success
---------+-------------------------+---------
 1       | crear tabla movimientos | t
 2       | datos de ejemplo        | t
```

La segunda vez que arranca dice `Schema "public" is up to date. No migration necessary.`
Una migración ya aplicada **no se edita nunca**: si hay que cambiar algo, se escribe una V3.

Y por eso en `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate **no crea ni modifica** tablas: solo comprueba al arrancar que la entidad coincide
con el esquema, y si no, falla. Quien manda en el esquema es Flyway. `ddl-auto=update` es
cómodo en un tutorial y **peligroso en producción**: nadie revisa qué SQL ejecuta.

### Defensa en profundidad

El monto se valida **dos veces**: con `@DecimalMin` en el DTO y con un `CHECK (monto > 0)` en
la tabla. Comprobado insertando directo por SQL, saltándose la API:

```
ERROR:  new row for relation "movimientos" violates check constraint "movimientos_monto_check"
```

La API protege a los clientes de la API; la base protege a los datos de **todo** lo demás:
un script, otra aplicación, alguien con acceso directo.

### `open-in-view=false`

Spring Boot activa por defecto un mecanismo (*Open Session In View*) que mantiene la conexión
a la base abierta durante toda la petición HTTP, incluido el momento de escribir el JSON.
Es cómodo y es una trampa: esconde consultas perezosas que se disparan fuera del service. Lo
desactivo, y Spring hasta lo recomienda con un warning al arrancar.

---

## El precio en tamaño

| | Etapa 1 | Etapa 5 |
|---|---|---|
| Jar ejecutable | 20 MB | **51 MB** |
| Jars dentro | 34 | **74** |

Hibernate 7.4, Spring Data JPA, Flyway 12 y el driver de PostgreSQL. Es la razón por la que,
cuando se habla de arranque rápido y contenedores pequeños, sale la conversación de R2DBC y
GraalVM — Etapa 10.

## Lo que sigue estando mal a propósito

- **No hay tests contra la base.** Los 8 tests siguen pasando, pero ninguno toca PostgreSQL.
  La forma profesional es **Testcontainers**: el test levanta un PostgreSQL real en Docker,
  corre, y lo destruye. Probar contra H2 (una base en memoria) es lo que hacen los
  tutoriales, y tiene trampa: H2 no es PostgreSQL y hay SQL que funciona en uno y no en otro.
- **Las credenciales están en `application.properties`.** Sirve en local; en producción vienen
  de variables de entorno o de Azure Key Vault.

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **Resolución por tipo** | Spring busca un bean asignable al tipo del parámetro; si hay 0 o 2+, no arranca |
| **`@Primary` / `@Qualifier` / `@Profile`** | Las tres formas de desempatar cuando hay varias implementaciones |
| **JPA / Hibernate** | JPA es la especificación del mapeo objeto-relacional; Hibernate, la implementación |
| **Entidad** | Clase mapeada a una tabla; necesita constructor vacío y no puede ser final |
| **Spring Data** | Genera la implementación de un repositorio a partir de su interfaz |
| **Proxy** | Objeto generado en tiempo de ejecución que se hace pasar por otro |
| **Patrón Adapter** | Traduce entre dos interfaces incompatibles sin que ninguna se entere |
| **Migración (Flyway)** | Cambio de esquema versionado, en SQL, que viaja en git |
| **`ddl-auto=validate`** | Hibernate verifica el esquema pero no lo toca |
| **Open Session In View** | Conexión abierta durante toda la petición; mejor desactivado |
| **Defensa en profundidad** | Validar en la API **y** en la base de datos |

## Para la entrevista

> "El repositorio está detrás de una interfaz. Cuando pasé de memoria a PostgreSQL escribí
> un adaptador que implementa esa interfaz usando Spring Data por dentro, y el service no
> cambió ni una línea. Spring resuelve la inyección por tipo al arrancar; de hecho, cuando
> dejé las dos implementaciones anotadas, la app no arrancó: 'required a single bean, but 2
> were found'. El esquema lo manejo con Flyway y Hibernate en `validate`, nunca en `update`."

Si te preguntan **por qué no un record para la entidad**: JPA necesita constructor sin
argumentos, clase no final para los proxies de carga perezosa, y campos escribibles. Un
record no cumple ninguna de las tres.

**Siguiente:** Etapa 6 — Lombok, y la comparación honesta con los records.
