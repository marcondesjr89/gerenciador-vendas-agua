import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ===================================================================
// Gerenciador de Vendas de Agua Mineral - Script de Carga k6 (3 Estagios)
// Rastreabilidade: RF-005, RNF-M4.3, RNF-M4.4 (Nivel A - 100 pontos)
// ===================================================================

// Metricas Customizadas
const catalogLatency = new Trend('catalog_duration', true);
const ordersQueryLatency = new Trend('orders_query_duration', true);
const createOrderLatency = new Trend('create_order_duration', true);
const paymentLatency = new Trend('payment_duration', true);
const successfulTransactions = new Counter('successful_transactions');
const failedTransactions = new Rate('failed_transactions_rate');

// Configuracao de Ambiente e URLs
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  // Bateria de Testes em 3 Estagios + Ramp-down
  stages: [
    // Estagio 1: Warm-up (Aquecimento de JVM, pool HikariCP e Cache Redis)
    { duration: '30s', target: 10 },

    // Estagio 2: Carga Sustentada (Trafego de producao regular)
    { duration: '1m', target: 50 },

    // Estagio 3: Pico de Stress (Saturacao maxima e medicao de gargalos)
    { duration: '30s', target: 150 },

    // Ramp-down / Desaceleracao
    { duration: '15s', target: 0 },
  ],

  // Limiares de Aprovacao (SLO Formal)
  thresholds: {
    // Requisito Nivel A: p95 inferior a 200ms
    http_req_duration: ['p(95)<200', 'p(99)<400'],

    // Taxa de falhas deve ser inferior a 1%
    http_req_failed: ['rate<0.01'],

    // Metricas especificas de cada modulo
    catalog_duration: ['p(95)<50'],          // Cache Redis resiliente
    orders_query_duration: ['p(95)<150'],    // Indice composto sobre 100k
    payment_duration: ['p(95)<180'],         // Lock distribuido idempotente
    failed_transactions_rate: ['rate<0.01'],
  },
};

export default function () {
  const vu = __VU;
  const iter = __ITER;
  const randomFactor = Math.random();

  // 1. Jornada de Consulta ao Catalogo (40% do trafego) - Testa Cache Redis Resiliente
  if (randomFactor < 0.40) {
    group('Catalog - Get Active Products', () => {
      const res = http.get(`${BASE_URL}/api/v1/catalog/products`, {
        tags: { endpoint: 'catalog_products' },
      });

      catalogLatency.add(res.timings.duration);
      const passed = check(res, {
        'catalog status is 200': (r) => r.status === 200,
        'catalog has products': (r) => {
          try {
            return JSON.parse(r.body).length > 0;
          } catch (e) {
            return false;
          }
        },
      });

      if (passed) {
        successfulTransactions.add(1);
        failedTransactions.add(0);
      } else {
        failedTransactions.add(1);
      }
    });
  }

  // 2. Jornada de Consulta Paginada de Pedidos (35% do trafego) - Testa Indice Composto sobre 100k
  else if (randomFactor < 0.75) {
    group('Orders - Paginated Query on 100k Dataset', () => {
      const page = Math.floor(Math.random() * 5); // Primeiras 5 paginas
      const res = http.get(`${BASE_URL}/api/v1/orders?status=DELIVERED&page=${page}&size=20`, {
        tags: { endpoint: 'orders_paginated' },
      });

      ordersQueryLatency.add(res.timings.duration);
      const passed = check(res, {
        'orders query status is 200': (r) => r.status === 200,
        'orders page has content': (r) => {
          try {
            const body = JSON.parse(r.body);
            return body.content && body.content.length > 0;
          } catch (e) {
            return false;
          }
        },
      });

      if (passed) {
        successfulTransactions.add(1);
        failedTransactions.add(0);
      } else {
        failedTransactions.add(1);
      }
    });
  }

  // 3. Jornada de Abertura de Pedido (15% do trafego) - Testa Transacionalidade e Gancho Fiscal
  else if (randomFactor < 0.90) {
    group('Orders - Create Order', () => {
      const payload = JSON.stringify({
        customerName: `Cliente k6 VU-${vu}`,
        customerPhone: `1198${String(vu).padStart(3, '0')}${String(iter % 1000).padStart(4, '0')}`,
        modality: vu % 2 === 0 ? 'DELIVERY' : 'PICKUP',
        paymentMethod: 'PIX',
        items: [
          {
            productId: 1, // Galao 20L Completo
            quantity: 1,
            bottlesReturned: false,
          },
        ],
      });

      const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'create_order' },
      };

      const res = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
      createOrderLatency.add(res.timings.duration);

      const passed = check(res, {
        'create order status is 201': (r) => r.status === 201,
        'order response has code': (r) => {
          try {
            return JSON.parse(r.body).orderCode !== undefined;
          } catch (e) {
            return false;
          }
        },
      });

      if (passed) {
        successfulTransactions.add(1);
        failedTransactions.add(0);
      } else {
        failedTransactions.add(1);
      }
    });
  }

  // 4. Jornada de Pagamento com Idempotencia (10% do trafego) - Testa Redis Lock e Unicidade
  else {
    group('Payment - Process Idempotent Payment', () => {
      // Gera idempotency key unica por iteracao para evitar conflito falso em testes de throughput
      const idempotencyKey = `k6-pay-vu${vu}-iter${iter}-${Date.now()}`;

      const payload = JSON.stringify({
        orderId: 1,
        paymentMethod: 'PIX',
        paidAmount: 30.00,
      });

      const params = {
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey,
        },
        tags: { endpoint: 'process_payment' },
      };

      const res = http.post(`${BASE_URL}/api/v1/payments`, payload, params);
      paymentLatency.add(res.timings.duration);

      const passed = check(res, {
        'payment status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'payment has status PAID': (r) => {
          try {
            return JSON.parse(r.body).status === 'PAID';
          } catch (e) {
            return false;
          }
        },
      });

      if (passed) {
        successfulTransactions.add(1);
        failedTransactions.add(0);
      } else {
        failedTransactions.add(1);
      }
    });
  }

  // Intervalo de reflexao do usuario (Pacing / Think Time realista)
  sleep(Math.random() * 0.5 + 0.2); // Entre 200ms e 700ms
}
