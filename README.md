# Mis Finanzas

Proyecto de aprendizaje **construido por etapas** rumbo al stack backend bancario
(Java 21, Spring Boot 4, JPA, WebFlux/Reactor, Resilience4j, Kafka, CQRS, SAGA).
El dominio son finanzas personales porque mapea directo a banca: tipo de cambio →
circuit breaker + cache, saldo vs historial → CQRS, transferencias → SAGA.

## Cómo correr (estado actual: Etapa 0)

```bash
./mvnw compile
java -cp target/classes com.accel.finanzas.Main
```

> Esta sección se actualiza en cada etapa. Es parte del trato: la documentación
> viaja en el mismo commit que el código que la cambia.

## Cómo trabajamos

1. Cada etapa tiene su guía en [`docs/`](docs/). **La implementas tú** siguiendo
   la guía; el chat es para dudas, revisión de código y desvíos.
2. **Un commit por etapa.** El `git log` cuenta la historia del proyecto.
3. Cada guía cierra con dos secciones fijas:
   - **Tecnicismos de esta etapa** — el vocabulario formal de lo que acabas de
     hacer (pilares OOP, patrones de diseño, arquitectura), para nombrarlo como
     en una entrevista.
   - **Para la entrevista** — la historia corta que esta etapa te deja contada
     en primera persona.
4. Reglas del código que no se negocian desde el día 1:
   - Dinero **siempre** en `BigDecimal`, nunca `double` ni `float`.
   - `record` para DTOs (*Data Transfer Objects*: objetos sin lógica que cruzan
     fronteras, p. ej. salir como JSON) y objetos de valor; Lombok solo cuando
     aporte de verdad (entidades JPA con muchos campos — llegará en la Etapa 6).
   - Todo se corre y se prueba antes de dar la etapa por cerrada.

## Roadmap

### Fase A — El monolito
- [x] **Etapa 0** — Maven + Java puro: la estructura estándar y por qué existe → [guía](docs/etapa-00-maven-java-puro.md)
- [ ] **Etapa 1** — Spring Boot: levantar el servidor → [guía](docs/etapa-01-spring-boot.md)
- [ ] **Etapa 2** — CRUD de movimientos en memoria (REST completo)
- [ ] **Etapa 3** — Capas: controller / service / repository + validación + errores
- [ ] **Etapa 4** — Tests con JUnit y Mockito; de aquí en adelante, TDD
- [ ] **Etapa 5** — PostgreSQL con Docker + JPA (aquí entra el modelado de datos)
- [ ] **Etapa 6** — Lombok: dónde aporta y dónde un record lo reemplaza

### Fase B — Resiliencia y reactivo
- [ ] **Etapa 7** — Cliente de tipo de cambio (API externa, timeouts)
- [ ] **Etapa 8** — Cache Aside
- [ ] **Etapa 9** — Circuit Breaker + laboratorio de fallas
- [ ] **Etapa 10** — Migración a WebFlux + R2DBC

### Fase C — Distribuido
- [ ] **Etapa 11** — Partir en microservicios + API Gateway
- [ ] **Etapa 12** — Kafka: eventos + proyección de saldos (CQRS)
- [ ] **Etapa 13** — Transferencias con SAGA orquestada
- [ ] **Etapa 14** — Docker multi-stage + CI + despliegue
- [ ] **Etapa 15** — Seguridad JWT + observabilidad + SonarQube
