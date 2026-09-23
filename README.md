# Gestión de pólizas — Seguros Bolívar

API de la prueba técnica: Java 21, Spring Boot 4.1.0, JPA, Flyway y capas controller/service/repository.

**Plain-English guide to every file:** [CODE_MAP.md](CODE_MAP.md). Java files and methods also have English purpose comments.

**Entrega:** [código en GitHub](https://github.com/nicoceron/seguros-bolivar-challenge/tree/assessment/final-2026-09-16) · [informe LaTeX, dos páginas](docs/technical-assessment.tex).

## Ejecutar

Java 21 y Maven 3.9+; la primera compilación requiere acceso a Maven Central.

```bash
mvn verify
mvn spring-boot:run
# Otra terminal, opcional: Python 3.10+, biblioteca estándar
python3 scripts/smoke.py
```

H2 en memoria, puerto 8080, dos pólizas de ejemplo: 1 individual y 2 colectiva. Se reinicia al apagar la aplicación. Para otro puerto: `SERVER_PORT=8081 mvn spring-boot:run`; el mock local utiliza el mismo puerto.

Perfil PostgreSQL con base persistente y configuración de ejemplo:

```bash
docker compose up --build
```

## Contrato

Header obligatorio en la API: `x-api-key: 123456`. Los cuerpos y DTO usan campos en inglés; las rutas y los filtros conservan el enunciado. No se requiere interfaz gráfica.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/polizas?tipo=COLECTIVA&estado=ACTIVA` | Lista paginada, filtros combinables |
| GET | `/polizas/{id}/riesgos` | Riesgos, incluidos los cancelados |
| POST | `/polizas/{id}/renovar` | Ajuste IPC y estado `RENOVADA` |
| POST | `/polizas/{id}/cancelar` | Cancela póliza y todos sus riesgos |
| POST | `/polizas/{id}/riesgos` | Agrega únicamente a colectiva |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo |
| POST | `/core-mock/evento` | Registra intento al CORE; 202 |

Adicionales: `POST /polizas`, `GET /polizas/{id}`, `GET /riesgos/{id}`. Creaciones: 201 con `Location`; mutaciones: 200. Errores: 400 entrada inválida, 401 clave ausente/incorrecta, 404 inexistente, 409 regla de negocio o conflicto concurrente; cuerpo ProblemDetail con `code`.

`tipo`: `INDIVIDUAL` o `COLECTIVA`; `estado`: `ACTIVA`, `RENOVADA`, `CANCELADA`. Paginación desde `page=0`, `size=20`, máximo 100, orden por ID. Respuesta: `content`, `page`, `size`, `totalElements`, `totalPages`.

```bash
curl -H 'x-api-key: 123456' 'http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA'
curl -H 'x-api-key: 123456' http://localhost:8080/polizas/2/riesgos

curl -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' \
  -d '{"ipcPercentage":5.2}' http://localhost:8080/polizas/1/renovar

curl -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' \
  -d '{"propertyAddress":"Calle 80 # 10-20","tenantName":"Laura"}' \
  http://localhost:8080/polizas/2/riesgos

curl -X POST -H 'x-api-key: 123456' http://localhost:8080/riesgos/2/cancelar
curl -X POST -H 'x-api-key: 123456' http://localhost:8080/polizas/2/cancelar

curl -i -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' \
  -d '{"evento":"ACTUALIZACION","polizaId":555}' http://localhost:8080/core-mock/evento
```

Crear una individual (el tomador debe coincidir con el arrendatario):

```bash
curl -i -X POST -H 'x-api-key: 123456' -H 'Content-Type: application/json' \
  -d '{"type":"INDIVIDUAL","effectiveFrom":"2026-01-01","durationMonths":12,
       "monthlyRent":100.01,"policyholderName":"Ana","beneficiaryName":"Luis",
       "risks":[{"propertyAddress":"Calle 1","tenantName":"Ana"}]}' \
  http://localhost:8080/polizas
```

## Decisiones y supuestos explícitos

**Dinero y vigencia.** `BigDecimal`, dos decimales, `HALF_UP`. `canonNuevo = redondear(canon × (1 + IPC/100), 2)` y `primaNueva = canonNuevo × mesesIniciales`. Ejemplo sintético: 100.01 con 5.2% y 12 meses da 105.21 y 1262.52. El 5.2% no se presenta como IPC oficial. Se redondea el canon una vez para conservar la identidad de la prima. Fechas inclusivas; nueva vigencia desde fin anterior + 1 día, por los meses originales.

**Límites de la demo, no reglas adicionales del enunciado.** IPC `[0,100]`, duración `[1,1200]` meses, de 1 a 100 riesgos en la creación (individual exactamente 1; colectiva admite más mediante altas posteriores). Importes positivos, como máximo 17 enteros y 2 decimales; se rechaza el desbordamiento del canon o de la prima. Las personas se identifican por nombre, comparado tras quitar espacios externos; producción usaría identificadores de terceros. El beneficiario por riesgo colectivo pertenece al modelo objetivo, no al DTO simplificado.

**Consistencia.** Las mutaciones toman un bloqueo de escritura sobre la póliza antes de modificar sus riesgos; cancelación y alta no pueden cruzarse dejando un riesgo activo. Datos y evento outbox comparten transacción. Las cancelaciones repetidas no producen nuevos cambios ni eventos. Renovar dos veces abre dos vigencias: el endpoint de renovación no deduplica peticiones y no debe reintentarse automáticamente tras una respuesta ambigua.

**Integración.** Tras confirmar la transacción, el worker consulta pendientes cada segundo y realiza un POST HTTP al mock. Timeouts: conexión 2 s, lectura 3 s; máximo cinco intentos con espera exponencial. UUID estable en `eventId` e `Idempotency-Key`. Cada evento se bloquea antes de procesar y se respeta la próxima fecha de intento. Se conservan registros `FAILED` para investigación/reproceso controlado. El contrato mínimo del mock acepta también el JSON exacto del enunciado sin UUID.

**Límite distribuido.** Respuesta exitosa = cambio local + envío pendiente, no confirmación del CORE. Un fallo después del HTTP y antes del commit puede repetir una entrega; el receptor real debe deduplicar. El mock solo escribe logs, no deduplica ni demuestra actualización de un CORE. Integrar WebLogic real exige adaptar contrato, autenticación, orden por póliza y conciliación. Broker, correo/SMS, Gateway, alta disponibilidad y renovación automática se describen en el módulo 1; no están implementados en este ejercicio esencial.

**Seguridad.** `123456` es exclusivamente la clave exigida para la prueba. En despliegue, cambiar `POLICY_API_KEY`, usar TLS y gestión de secretos. La API key no sustituye autorización por usuario/tenant. Las sondas de salud y el despacho interno de errores se exceptúan del filtro; Swagger y métricas requieren la clave. Para Swagger en navegador se necesita un cliente que inyecte ese header; no se expone una UI sin autenticación.

## Configuración y observabilidad

| Variable | Valor predeterminado |
|---|---|
| `POLICY_API_KEY` | `123456` |
| `SERVER_PORT` | `8080` |
| `CORE_BASE_URL` | `http://localhost:<SERVER_PORT>` |
| `CORE_DISPATCH_ENABLED` | `true` |
| `DEMO_DATA_ENABLED` | `true` |
| `DATABASE_URL` | JDBC PostgreSQL local, perfil `postgres` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `policies` / `policies`, solo ejemplo |

Salud: `/actuator/health`. Métricas protegidas: `/actuator/metrics`, `/actuator/prometheus`; contador `policy.core.events`. `X-Correlation-ID` se devuelve y aparece en logs; valores inseguros se reemplazan. OpenAPI: `/v3/api-docs`; Swagger: `/swagger-ui.html`.

## Pruebas y estructura

**55 pruebas aprobadas** con `mvn verify`: dominio, seguridad, validación HTTP, filtros, reglas, rollback de datos/outbox, concurrencia real de dos transacciones, HTTP del adapter y reintentos. [Evidencia y alcance](docs/verification.md). `scripts/smoke.py` crea sus propios datos y verifica 18 llamadas HTTP; puede repetirse sin depender de IDs iniciales. JaCoCo: `target/site/jacoco/index.html`. No se afirma validación de carga ni ejecución de PostgreSQL/Docker.

```text
src/main/java/com/segurosbolivar/policy/
  api/           Controllers, DTO y errores
  domain/        Policy (Poliza), Risk (Riesgo), reglas y dinero
  service/       Casos de uso y transacciones
  repository/    Persistencia y bloqueos
  integration/   Puerto CORE, adapter HTTP y outbox
  config/        Clave, correlación, OpenAPI y datos demo
src/main/resources/db/migration/   Esquema Flyway
src/test/                         Pruebas JUnit
```

Informe y paquete: `make docs` y `make package` (este último exige cambios confirmados en Git). Requiere TeX Live con español, fuentes recomendadas y latex-extra, o Tectonic; Python 3 para empaquetar. Salidas: `output/pdf/Nicolas_Ceron_Prueba_Tecnica.pdf` y `output/Nicolas_Ceron_Prueba_Tecnica.zip`. El código se entrega mediante el enlace de GitHub; el ZIP es una copia adicional.
