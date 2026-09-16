# Trazabilidad de la prueba

Base: PDF suministrado «PRUEBA Desarrollador TI/Sénior» (6 páginas) y correo de entrega. Plazo del correo: **jueves 17 de septiembre de 2026, antes del mediodía**; no se infiere una zona horaria que el correo no especifica. Archivos: `Nicolas_Ceron_Prueba_Tecnica.pdf` y `.zip`. Código mediante enlace de GitHub. Informe en español por correspondencia con el enunciado; no es una restricción del documento original.

| Módulo | Requisito | Evidencia |
|---|---|---|
| 1. Diseño | Arquitectura, tres patrones justificados, modelo, escalabilidad, observabilidad, tolerancia a fallos, versiones y componentes | Informe, sección 1; límites de implementación en sección 2 y README |
| 2. API | Seis endpoints obligatorios, filtros, reglas, mock HTTP y `x-api-key: 123456`; Spring Boot y capas | `api/`, `service/`, `domain/`, `repository/`, `integration/`; pruebas y README |
| 3. BBDD | Tres estrategias para orders (10 millones) y customers (500.000), unión por customer_id y país México | Informe, sección 3; sin afirmar resultados no medidos |
| 4. Git | Incorporar solo el arreglo de seguridad de main a feature/new-login | Informe, sección 4: cherry-pick con trazabilidad y conflictos |
| 5. Liderazgo | Ocho desarrolladores, 40% deuda, diez incidentes, entrega en tres semanas, dos juniors; responder cinco preguntas | Informe, sección 5: cinco prioridades, organización, métricas, prácticas y negocio |

## Contrato práctico obligatorio

`GET /polizas` (tipo y estado); `GET /polizas/{id}/riesgos`; `POST /polizas/{id}/renovar`; `POST /polizas/{id}/cancelar`; `POST /polizas/{id}/riesgos`; `POST /riesgos/{id}/cancelar`.

Individual: un riesgo; agregar solo a colectiva; cancelada no se renueva; cancelar póliza cancela todos sus riesgos. Renovar ajusta canon/prima por IPC, conserva duración original y pasa a `RENOVADA`. Mock obligatorio: `POST /core-mock/evento`, cuerpo `{"evento":"ACTUALIZACION","polizaId":555}`, registro del intento en logs.

## Distinción de alcance

WebLogic real, CORE transaccional, Gateway, broker, notificaciones, renovación automática y disponibilidad 24/7 forman parte del **diseño del módulo 1**, no de una infraestructura desplegada. La entrega ejecuta el caso simplificado del módulo 2 con un mock y outbox. Los límites defensivos adicionales, el redondeo y las identidades simplificadas se explican en README. [Verificación ejecutada](docs/verification.md).
