#!/usr/bin/env python3
"""Exercise the required API over HTTP; creates and cancels its own demo policies."""
import argparse
import json
import os
import sys
from decimal import Decimal
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080')
    args = parser.parse_args()
    base = args.base_url.rstrip('/')
    key = os.getenv('POLICY_API_KEY', '123456')
    checks = 0

    def call(method, path, body=None, expected=200, authenticated=True):
        nonlocal checks
        headers = {'Content-Type': 'application/json'}
        if authenticated:
            headers['x-api-key'] = key
        req = Request(base + path, data=None if body is None else json.dumps(body).encode(),
                      headers=headers, method=method)
        try:
            response = urlopen(req, timeout=10)
        except HTTPError as error:
            response = error
        with response:
            raw = response.read()
            if response.status != expected:
                raise AssertionError(f'{method} {path}: expected {expected}, got {response.status}: {raw.decode()}')
            data = json.loads(raw, parse_float=Decimal) if raw else None
        checks += 1
        print(f'PASS {method} {path} -> {expected}')
        return data

    risk = {'propertyAddress': 'Calle de prueba 1', 'tenantName': 'Ana'}
    def create(kind):
        return call('POST', '/polizas', {
            'type': kind, 'effectiveFrom': '2026-01-01', 'durationMonths': 12,
            'monthlyRent': 100.01, 'policyholderName': 'Ana' if kind == 'INDIVIDUAL' else 'Inmobiliaria',
            'beneficiaryName': 'Propietario', 'risks': [risk]}, 201)['id']

    try:
        call('GET', '/polizas', expected=401, authenticated=False)
        individual, collective = create('INDIVIDUAL'), create('COLECTIVA')
        data = call('GET', '/polizas?tipo=COLECTIVA&estado=ACTIVA')
        assert data['content'] and all(p['type'] == 'COLECTIVA' and p['status'] == 'ACTIVA' for p in data['content'])
        assert len(call('GET', f'/polizas/{individual}/riesgos')) == 1
        renewed = call('POST', f'/polizas/{individual}/renovar', {'ipcPercentage': 5.2})
        assert renewed['monthlyRent'] == Decimal('105.21') and renewed['premium'] == Decimal('1262.52')
        assert renewed['status'] == 'RENOVADA' and renewed['effectiveFrom'] == '2027-01-01'
        call('POST', f'/polizas/{individual}/riesgos', risk, 409)
        added = call('POST', f'/polizas/{collective}/riesgos', risk, 201)
        assert call('GET', f"/riesgos/{added['id']}")['id'] == added['id']
        call('POST', f"/riesgos/{added['id']}/cancelar")
        call('POST', f'/polizas/{collective}/cancelar')
        assert all(r['status'] == 'CANCELADO' for r in call('GET', f'/polizas/{collective}/riesgos'))
        call('POST', f'/polizas/{collective}/cancelar')
        call('POST', f'/polizas/{collective}/renovar', {'ipcPercentage': 5.2}, 409)
        call('POST', f'/polizas/{collective}/riesgos', risk, 409)
        call('POST', '/core-mock/evento', {'evento': 'ACTUALIZACION', 'polizaId': 555}, 202)
        call('POST', f'/polizas/{individual}/renovar', {'ipcPercentage': -1}, 400)
        call('POST', f'/polizas/{individual}/cancelar')
    except (AssertionError, URLError, KeyError, ValueError) as error:
        print(f'FAIL: {error}', file=sys.stderr)
        return 1
    print(f'{checks} HTTP checks passed; business assertions passed.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
