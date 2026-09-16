# Verificación ejecutada

Código probado: `475f9cec345ba977d4fa53b7db865a0f7b0c5fad`.

[GitHub Actions — ejecución 35113944967](https://github.com/nicoceron/seguros-bolivar-challenge/actions/runs/35113944967), 16 de septiembre de 2026, `mvn -B -ntp verify`, Java 21.

| Suite | Casos | Fallos / errores / omitidos |
|---|---:|---|
| PolicyApiIntegrationTest | 8 | 0 / 0 / 0 |
| PolicyApiEdgeCasesTest | 27 | 0 / 0 / 0 |
| PolicyTest | 9 | 0 / 0 / 0 |
| CoreOutboxProcessorTest | 5 | 0 / 0 / 0 |
| HttpCoreEventPublisherTest | 3 | 0 / 0 / 0 |
| PolicyTransactionTest | 3 | 0 / 0 / 0 |
| **Total** | **55** | **0 / 0 / 0** |

Además: `python3 scripts/smoke.py --base-url http://localhost:18080` contra la aplicación local compilada con Java 21: **18 llamadas HTTP y sus aserciones aprobadas**. Se observaron en logs los siete envíos generados por cambios, además del POST explícito con `polizaId=555`.

La suite comprueba rollback de póliza/riesgos/outbox, una carrera de cancelación frente a alta, versiones tras commit, redondeo, límites, autenticación, errores, reintentos y transporte HTTP. Las pruebas HTTP del adapter incluyen 202, 503 y redirección 302 rechazada.

La base usada es H2. No se ejecutaron PostgreSQL, Docker, pruebas de carga, conmutación entre zonas ni un CORE real. Los escenarios de concurrencia no constituyen una prueba formal de todas las intercalaciones ni sustituyen pruebas sobre el motor de producción.
