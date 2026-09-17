# Etapa 1 — Qué cambia exactamente entre "solo Maven" y "Maven + Spring Boot"

Documento de referencia, complementario a la [guía paso a paso](etapa-01-spring-boot.md).
Aquí no hay instrucciones: es el **antes y el después**, con las cifras medidas en
esta máquina el 17-sep-2026.

## El resumen

| | Etapa 0 (Maven puro) | Etapa 1 (Spring Boot 4.1.1) |
|---|---|---|
| Dependencias declaradas | 0 | 1 (`spring-boot-starter-webmvc`) |
| Artefactos que Maven resuelve | 0 | **39** |
| Qué produce el build | `.class` sueltos en `target/classes` (28 KB) | un jar ejecutable de **20 MB** |
| Qué hay dentro del entregable | 3 clases tuyas | 5 clases tuyas + **34 jars** |
| Cómo se ejecuta | `java -cp target/classes com.accel.finanzas.Main` | `java -jar target/finanzas-0.1.0.jar` |
| Qué hace al ejecutarse | imprime y termina | levanta Tomcat y **se queda escuchando** |
| Quién crea los objetos | tú, con `new` | el contenedor de Spring |
| Configuración | código (`getResourceAsStream`) | convención (`application.properties`) |

## Las cinco diferencias de fondo

### 1. Una dependencia declarada, treinta y nueve resueltas

En el `pom.xml` escribiste **una sola** dependencia. Maven resolvió 39 artefactos, porque
cada dependencia arrastra las suyas (*dependencias transitivas*). Se ve con
`./mvnw dependency:tree`:

```
com.accel:finanzas:jar:0.1.0
\- org.springframework.boot:spring-boot-starter-webmvc:jar:4.1.1
   +- org.springframework.boot:spring-boot-starter:jar:4.1.1
   |  +- org.springframework.boot:spring-boot-starter-logging:jar:4.1.1
   |  |  +- ch.qos.logback:logback-classic:jar:1.5.38
   |  |  |  +- ch.qos.logback:logback-core:jar:1.5.38
   |  |  |  \- org.slf4j:slf4j-api:jar:2.0.18
   ...
```

Y ninguna de esas 39 lleva número de versión en tu pom: las fija el `<parent>`, que importa
un **BOM** con ~1900 librerías ya probadas juntas. Ese es el trabajo que te ahorra Spring
Boot y que en un banco vale oro: nadie discute qué versión de Jackson va con qué versión de
Spring.

### 2. El entregable deja de ser "clases" y pasa a ser "la aplicación entera"

El `spring-boot-maven-plugin` hace un paso extra después de compilar, llamado *repackage*:
mete tus clases y **los 34 jars** en un solo archivo. Se ve en el manifiesto:

```
Main-Class:  org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.accel.finanzas.FinanzasApplication
```

Fíjate en el detalle: quien arranca **no es tu clase**. Arranca un lanzador de Spring
(`JarLauncher`) que primero enseña a la JVM a leer jars anidados y recién después llama a tu
`FinanzasApplication`. Java, de fábrica, no sabe cargar un jar dentro de otro jar; eso lo
resuelve ese lanzador.

Por eso un servicio Spring Boot entra directo en un contenedor Docker: el artefacto es
autosuficiente, no necesita un servidor instalado en la máquina destino. Es la base de todo
lo *cloud native* y lo retomaremos en la Etapa 14.

### 3. El programa deja de terminar

Es el cambio más visible al correrlo. En la Etapa 0, `main` imprimía y moría. Ahora `main`
levanta Tomcat, y Tomcat abre el puerto 8080 y se queda ahí. Del log de arranque real:

```
Tomcat started on port 8080 (http) with context path '/'
Started FinanzasApplication in 0.608 seconds (process running for 0.811)
```

Se detiene con Ctrl+C. Si alguna vez ves `Port 8080 was already in use`, es que quedó uno
vivo de antes: `lsof -ti:8080 | xargs kill`.

### 4. Tú dejas de crear los objetos

En la Etapa 0 todo nacía con `new` dentro de `main`. Ahora nunca instancias el
`PingController`: Spring lo encuentra al escanear el paquete, lo crea, lo guarda en su
contenedor y le pasa las peticiones. Eso es **Inversión de Control**, y lo vas a *sentir* en
la Etapa 3, cuando un controller reciba un service sin que tú lo construyas.

Consecuencia práctica que conviene saber desde ya: Spring crea **una sola instancia** de cada
controller (los beans son *singleton* por defecto) y esa instancia atiende todas las
peticiones a la vez. Por eso un controller nunca guarda estado mutable en sus campos.

### 5. La configuración deja de ser código

El viejo `Main` abría el archivo a mano:

```java
try (InputStream in = Main.class.getResourceAsStream("/app.properties")) {
    config.load(in);
}
```

Eso ya no existe. Basta con que el archivo se llame `application.properties` y esté en
`src/main/resources/`: Spring lo busca solo. El mecanismo por debajo es el mismo (leer del
classpath), pero ahora es **convención en vez de código**, que es el principio rector del
framework.

## Lo que cambió, archivo por archivo

| Archivo | Qué pasó |
|---|---|
| `pom.xml` | Ganó el `<parent>`, la dependencia del starter y el plugin de empaquetado |
| `Main.java` | **Borrado**. Su lección (classpath y resources) la hace Spring ahora |
| `FinanzasApplication.java` | **Nuevo**. El punto de entrada, con `@SpringBootApplication` |
| `PingController.java` | **Nuevo**. El primer endpoint: `GET /ping` |
| `app.properties` | Renombrado a `application.properties` (nombre que Spring busca solo) |
| `Movimiento.java` | Intacto. Lo usará el CRUD de la Etapa 2 |
| `aprendizaje/StreamsExplicados.java` | **Nuevo**, y no es parte de la app: ver abajo |

### El detalle de los dos `main()`

Al agregar la clase de aprendizaje, el proyecto pasó a tener **dos** métodos `main`. El
plugin de Spring Boot entonces no sabe cuál arrancar y el build falla. Se resuelve
declarándolo en el pom:

```xml
<start-class>com.accel.finanzas.FinanzasApplication</start-class>
```

Vale la pena conocerlo porque aparece en cualquier proyecto con utilidades ejecutables al
lado de la aplicación.

## ¿Cuánto demora? (medido aquí)

| | Tiempo |
|---|---|
| `./mvnw clean package` con las dependencias ya descargadas | **4.9 s** |
| `./mvnw package` incremental (sin `clean`) | **1.2 s** |
| Arranque de la aplicación | **0.6 s** |

La única espera larga es la **primera** descarga de dependencias en una máquina nueva: unos
minutos, una sola vez, y quedan en `~/.m2` para todos tus proyectos. Agregar Spring Boot a un
proyecto existente, en sí, son tres cambios en el pom y una clase.

## Cómo correr las dos cosas

La aplicación:

```bash
./mvnw spring-boot:run
```

```bash
curl http://localhost:8080/ping
```

El material de aprendizaje (independiente, no levanta nada):

```bash
java -cp target/classes com.accel.finanzas.aprendizaje.StreamsExplicados
```

## Nota para Neovim (jdtls)

Al cambiar el `pom.xml` de esta forma, el servidor de lenguaje tiene que releer el proyecto
para enterarse de las 39 dependencias nuevas. Con `updateBuildConfiguration = "interactive"`
te preguntará. Si ves errores fantasma —imports de Spring marcados en rojo aunque el build
pase— el martillo es `:JdtWipeDataAndRestart`, que borra el índice cacheado en
`~/.cache/jdtls/workspace/finanzas-java` y lo reconstruye.
