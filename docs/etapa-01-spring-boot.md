# Etapa 1 — De Java puro a Spring Boot

**Objetivo:** convertir el proyecto de la Etapa 0 en una aplicación Spring Boot
que levanta un servidor web y responde JSON en `http://localhost:8080/ping`.

**Sabes que terminaste cuando:**
1. `./mvnw spring-boot:run` arranca y el log dice `Tomcat started on port 8080`.
2. Abrir `http://localhost:8080/ping` en el navegador devuelve un JSON.
3. `java -jar target/finanzas-0.1.0.jar` hace exactamente lo mismo sin Maven.

---

## Paso 0 — Commit de la Etapa 0

Antes de tocar nada, congela lo que ya funciona:

```bash
git add .
```

```bash
git commit -m "Etapa 0: estructura Maven estándar + Java puro"
```

> **Por qué:** cada etapa vive en un commit. Cuando algo se rompa (se va a
> romper), `git diff` te dice exactamente qué cambiaste desde el último estado
> bueno. Ese hábito —cambios pequeños con un estado bueno conocido detrás— es
> la base de cualquier flujo de code review.

## Paso 1 — El `pom.xml`: heredar de Spring Boot

Reemplaza **todo** el contenido de `pom.xml` por esto:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Heredamos de Spring Boot: nos fija las versiones de ~1900 librerías -->
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
  </parent>

  <groupId>com.accel</groupId>
  <artifactId>finanzas</artifactId>
  <version>0.1.0</version>
  <packaging>jar</packaging>

  <properties>
    <!-- El parent traduce esta propiedad a maven.compiler.release (lo que usábamos en la Etapa 0) -->
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <!-- Un "starter" = paquete curado: Spring MVC + Jackson (JSON) + Tomcat embebido -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
      <!-- Sin <version>: la decide el parent vía su BOM (Bill of Materials) -->
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- Re-empaqueta el jar como ejecutable (fat jar): la app + todas sus dependencias -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Tres ideas nuevas aquí, en orden de importancia:

- **`<parent>`** — tu proyecto hereda de `spring-boot-starter-parent`, que
  importa un **BOM** (*Bill of Materials*) llamado `spring-boot-dependencies`:
  una lista de ~1900 librerías con versiones que Spring ya probó juntas. Por eso
  la dependencia de abajo no lleva `<version>`. En un banco esto importa
  muchísimo: "¿qué versión de Jackson es compatible con qué versión de Spring?"
  deja de ser tu problema. (Además del BOM, el parent aporta configuración de
  build — por ejemplo la traducción de `java.version`.)
- **starter** — `spring-boot-starter-webmvc` no es una librería, es un *paquete
  de compras*: trae Spring MVC, Jackson y Tomcat como **dependencias
  transitivas** (las dependencias de tus dependencias).

  > **Dato de entrevista:** hasta Spring Boot 3 este starter se llamaba
  > `spring-boot-starter-web` — así aparece en el 95% de tutoriales y quizá en
  > boca de tu entrevistador. En Boot 4 quedó **deprecado** a favor de
  > `spring-boot-starter-webmvc` (más explícito: existe también
  > `-webflux`, y lo veremos en la Etapa 10). Saber el cambio de nombre te
  > ubica de inmediato en "trabajó con Boot 4".

- **`spring-boot-maven-plugin`** — convierte el jar "normal" en un **fat jar**
  (también le dicen *uber jar*): un solo archivo con tu código + Spring + Tomcat
  adentro. Es lo que hace posible `java -jar` sin instalar nada más.

**Antes de seguir, verifica que el pom está sano:**

```bash
./mvnw dependency:tree
```

La primera vez descargará dependencias a `~/.m2` (el **repositorio local** de
Maven) y tomará un par de minutos; luego imprime el árbol de dependencias. Busca
en él `spring-boot-webmvc`, `jackson-databind` y `tomcat-embed-core`: esas son
las transitivas que el starter te acaba de traer. Si el árbol se imprime, tu pom
compila y puedes continuar.

## Paso 2 — Borrar `Main.java`, crear `FinanzasApplication`

`Main.java` ya cumplió su misión (te enseñó el classpath y los resources — y lo
que hacía su `getResourceAsStream` es exactamente lo que Spring hará ahora por
ti; la lección queda escrita en la [guía de la Etapa 0](etapa-00-maven-java-puro.md)).
Bórralo:

```bash
rm src/main/java/com/accel/finanzas/Main.java
```

**`Movimiento.java` se queda** — el CRUD de la Etapa 2 lo va a usar.

Crea `src/main/java/com/accel/finanzas/FinanzasApplication.java`:

```java
package com.accel.finanzas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanzasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanzasApplication.class, args);
    }
}
```

Cuatro líneas de código real, pero `@SpringBootApplication` es la anotación más
densa del ecosistema. Son tres anotaciones en una:

| Anotación interna | Qué hace |
|---|---|
| `@ComponentScan` | Escanea `com.accel.finanzas` y sus subpaquetes buscando clases anotadas (`@RestController`, `@Service`...) para registrarlas como **beans** — objetos que Spring crea y administra por ti (definido mejor abajo) |
| `@EnableAutoConfiguration` | Mira qué hay en el classpath y configura solo: "¿está Tomcat? entonces levanto un servidor web en el 8080" |
| `@SpringBootConfiguration` | Marca esta clase como fuente de configuración (es una especialización de la `@Configuration` clásica de Spring — en entrevista, cualquiera de los dos nombres vale si sabes explicar la relación) |

> **Tecnicismo central: Inversión de Control (IoC).** En la Etapa 0, `Main`
> creaba sus objetos con `new` y controlaba todo el flujo. Ahora Spring mantiene
> un contenedor (el **ApplicationContext**) que crea los objetos —los
> **beans**— y te los entrega ya conectados (**inyección de dependencias**). El
> control se invirtió: tú ya no llamas al framework, el framework te llama a ti.
> Lo vas a *sentir* de verdad en la Etapa 3, cuando un controller reciba un
> service por constructor sin hacer `new` jamás.
>
> Y de paso: `@ComponentScan` escanea *desde el paquete de esta clase hacia
> abajo*. Esa es la razón técnica de que la clase principal viva en la raíz del
> paquete (`com.accel.finanzas`) — otro misterio de la estructura resuelto.

Verifica que la clase nueva compila antes de seguir:

```bash
./mvnw compile
```

## Paso 3 — Renombrar `app.properties` → `application.properties`

```bash
git mv src/main/resources/app.properties src/main/resources/application.properties
```

(Usamos `git mv` y no `mv` para que git registre el rename como tal; funciona
porque el archivo quedó trackeado con el commit del Paso 0.)

Reemplaza **todo** su contenido por esta única línea — la vieja `app.nombre` se
va con `Main.java`, su lección ya terminó:

```properties
spring.application.name=finanzas
```

`application.properties` es **el** nombre que Spring busca automáticamente en la
raíz del classpath (o sea, en `src/main/resources/`). Mismo mecanismo que
programamos a mano en la Etapa 0 — solo que ahora es convención, no código. Esto
tiene nombre propio: **Convention over Configuration** (convención sobre
configuración), el principio rector de Spring Boot — el framework asume nombres
y valores estándar, y tú solo escribes lo que se desvía de ellos. Aquí vivirán
después el puerto, la conexión a PostgreSQL, la config de Kafka…

## Paso 4 — El primer endpoint

Un **endpoint** es una URL concreta que tu API atiende. Crea el primero en
`src/main/java/com/accel/finanzas/PingController.java`:

```java
package com.accel.finanzas;

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    record Ping(String estado, String app, LocalDateTime hora) {}

    @GetMapping("/ping")
    public Ping ping() {
        return new Ping("ok", "Mis Finanzas", LocalDateTime.now());
    }
}
```

Qué está pasando:

- **`@RestController`** = `@Controller` + `@ResponseBody`: "lo que retornen mis
  métodos no es el nombre de una vista HTML, es el cuerpo de la respuesta".
  **Jackson** (vino en el starter) convierte el record a JSON automáticamente.
- **`@GetMapping("/ping")`** asocia el método a la ruta `/ping` con el verbo
  GET — el verbo HTTP de lectura, "dame datos". Los demás verbos (POST, PUT,
  DELETE) y los códigos de estado son el corazón de la Etapa 2.
- El record `Ping` es tu primer **DTO** (*Data Transfer Object*): un objeto sin
  lógica cuyo único trabajo es cruzar una frontera — aquí, salir serializado
  como JSON. Está anidado por ahora a propósito: en la Etapa 3, cuando hablemos
  de capas, los DTOs tendrán su propio lugar. Cada cosa en su etapa.

> **Tres patrones de diseño acaban de entrar en escena:**
>
> 1. **MVC** (Model-View-Controller) — el patrón que BCP lista por nombre. Aquí
>    el *Model* es `Ping`, el *Controller* es esta clase, y la "vista" es la
>    serialización JSON (en un API REST no hay vista visual).
> 2. **Front Controller** — no lo escribiste tú, pero existe: el
>    `DispatcherServlet` de Spring recibe **todas** las peticiones HTTP y las
>    despacha al controller correcto según la ruta. Un único punto de entrada
>    que enruta — por eso, cuando pidas una ruta inexistente, quien responde el
>    404 es Spring y no Tomcat "pelado".
> 3. **Singleton** — Spring crea **una sola instancia** de `PingController` y
>    esa instancia atiende todas las peticiones concurrentes (los beans son
>    *singleton por defecto*; es el patrón administrado por el contenedor, no
>    el GoF clásico con `getInstance()`). Consecuencia práctica y pregunta
>    típica de banca: un controller **nunca** guarda estado mutable en campos.

## Paso 5 — Levantar el servidor

```bash
./mvnw spring-boot:run
```

Busca en el log estas dos líneas (los tiempos exactos variarán):

```
Tomcat started on port 8080 (http) with context path '/'
Started FinanzasApplication in 0.6 seconds
```

Fíjate en algo raro: el programa **no termina**. En la Etapa 0, `main` ejecutaba
e imprimía y moría. Ahora `main` arranca Tomcat, y Tomcat se queda escuchando el
puerto 8080 para siempre. Pruébalo:

- En el navegador: `http://localhost:8080/ping` → tu JSON.
- Desde otra terminal:

  ```bash
  curl http://localhost:8080/ping
  ```

- Y pide una ruta que no existe, `http://localhost:8080/nada`. En el navegador
  verás la **Whitelabel Error Page** ("whitelabel" = producto sin marca: la
  página de error genérica que Boot te da mientras no tengas manejo de errores
  propio — llega en la Etapa 3). Con `curl` no hay página: recibes el mismo 404
  pero como JSON (`{"status":404,"error":"Not Found","path":"/nada"...}`),
  porque Spring responde según lo que el cliente acepta. En ambos casos el 404
  lo generó Spring, no Tomcat: la petición sí llegó a tu aplicación (el Front
  Controller en acción), solo que ningún controller la reclamó.

Cuando termines: **Ctrl+C** para detener el servidor.

## Paso 6 — El jar ejecutable (así se despliega en la vida real)

```bash
./mvnw clean package
```

```bash
java -jar target/finanzas-0.1.0.jar
```

Mismo servidor, misma respuesta — pero ya sin Maven de por medio. Ese archivo de
~20 MB es tu aplicación **completa**: tu código + Spring + Tomcat. Pésalo tú
mismo (`ls -lh target/`) y compáralo con los pocos KB de la Etapa 0: la
diferencia es el servidor que ahora llevas dentro.

> **Tecnicismo: servidor embebido.** Hasta ~2015 esto era al revés: instalabas
> un Tomcat/WebLogic en el servidor del banco y le "deployabas" tu app como
> `.war`. Hoy la app **contiene** a su servidor, y eso es la base de todo lo
> *cloud native*: un jar autocontenido es trivial de meter en un contenedor
> Docker (Etapa 14) y de escalar en Kubernetes. Cuando una oferta dice "Cloud
> Native Architecture", esta inversión es el punto de partida.

Detén el servidor (Ctrl+C).

## Paso 7 — Actualizar el README y cerrar la etapa

1. En `README.md`, sección **Cómo correr**, deja los dos modos que esta etapa te
   dio (y cambia "estado actual: Etapa 0" a Etapa 1):
   - desarrollo: `./mvnw spring-boot:run`
   - como en producción: `./mvnw clean package` y `java -jar target/finanzas-0.1.0.jar`
2. Marca la casilla de la Etapa 1 en el roadmap.
3. Commit:

```bash
git add .
```

```bash
git commit -m "Etapa 1: Spring Boot 4.1, servidor embebido y primer endpoint"
```

---

## Si algo sale mal

| Síntoma | Causa probable | Arreglo |
|---|---|---|
| `Port 8080 was already in use` | Quedó un servidor corriendo de antes | `lsof -ti:8080 \| xargs kill` (o cambia `server.port=8081` en `application.properties`) |
| `release version 21 not supported` | `JAVA_HOME` apunta a otro JDK (mvnw le hace caso a `JAVA_HOME` antes que al `PATH`) | `./mvnw -version` muestra el JDK que Maven usa de verdad; si no es 21, revisa `echo $JAVA_HOME` |
| `Please tell me who you are` al hacer commit | Git no tiene tu identidad configurada | `git config --global user.name "Tu Nombre"` y `git config --global user.email "tu@correo"` |
| Primer build eterno | Descarga inicial de dependencias | Normal una sola vez; quedan en `~/.m2` |
| El navegador descarga un archivo en vez de mostrar JSON | Nada — algunos navegadores lo hacen | Usa `curl` o instala una extensión de JSON |

## Tecnicismos de esta etapa

| Término | En una frase |
|---|---|
| **IoC / Inversión de Control** | El framework crea y conecta tus objetos; tú ya no llamas al framework, él te llama a ti |
| **Bean / ApplicationContext** | Un objeto administrado por Spring / el contenedor que los guarda a todos |
| **Inyección de dependencias (DI)** | La forma concreta de IoC: los colaboradores te llegan por constructor, no con `new` *(en esta etapa solo la nombras; la ves en código en la Etapa 3)* |
| **BOM (Bill of Materials)** | Un pom que solo fija versiones compatibles entre sí (`spring-boot-dependencies`, importado por el parent) |
| **Starter** | Paquete curado de dependencias para un propósito (`-webmvc`, `-data-jpa`, `-kafka`); hasta Boot 3, el de web se llamaba `spring-boot-starter-web` |
| **Dependencia transitiva** | La dependencia de tu dependencia (`dependency:tree` las muestra) |
| **Auto-configuración** | Spring mira el classpath y configura lo que encuentra, con valores por defecto sensatos |
| **Component scan** | Escaneo del paquete raíz hacia abajo buscando clases anotadas para volverlas beans |
| **Convention over Configuration** | El framework asume nombres y valores estándar; tú solo escribes lo que se desvía |
| **DTO (Data Transfer Object)** | Objeto sin lógica que existe para cruzar una frontera (aquí: salir como JSON) |
| **Fat jar / uber jar** | Un jar con la app y todas sus dependencias adentro, ejecutable con `java -jar` |
| **Servidor embebido** | La app contiene al servidor (Tomcat), en vez de deployarse dentro de uno |
| **Front Controller** | Patrón: un único punto de entrada (`DispatcherServlet`) recibe todo y enruta |
| **MVC** | Patrón: separar modelo, lógica de control y representación de la respuesta |
| **Singleton (scope de bean)** | Una sola instancia por contenedor atiende todo; por eso los beans no guardan estado mutable |

## Para la entrevista

> "Spring Boot no es magia: `@SpringBootApplication` son tres anotaciones —
> component scan, auto-configuración y `@SpringBootConfiguration` (una
> especialización de `@Configuration`). La auto-configuración solo mira el
> classpath: si el starter web trae Tomcat, levanta un servidor. Y el resultado
> de `package` es un fat jar con el servidor embebido adentro — por eso un
> servicio Spring Boot encaja directo en Docker y Kubernetes, sin instalar un
> servidor de aplicaciones aparte."

Si te preguntan **"¿por qué ya no se usan WARs sobre un Tomcat externo?"**: el
jar autocontenido hace que *la unidad de despliegue sea la aplicación completa*,
idéntica en tu laptop, en CI y en producción — la premisa de contenedores y por
tanto de AKS/Kubernetes.

Y el detalle que te ubica en 2026: **en Boot 4, `spring-boot-starter-web` está
deprecado**; los starters web ahora son explícitos (`-webmvc` y `-webflux`).

**Siguiente:** Etapa 2 — el CRUD de movimientos en memoria: GET/POST/PUT/DELETE,
códigos de estado HTTP y tu primera discusión de diseño de API.
