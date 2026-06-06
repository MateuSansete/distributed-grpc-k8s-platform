import http from 'k6/http';
import { check, sleep } from 'k6';

// Lê as variáveis do terminal
const scenario = __ENV.SCENARIO || '1';
const protocol = __ENV.TARGET || 'grpc';

// Roteamento de porta
const port = protocol === 'rest' ? '9080' : '8080';

// Configuração base (limpa o terminal para mostrar só as métricas essenciais)
export let options = {
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)'],
};

// ==========================================
// CONFIGURAÇÃO DOS CENÁRIOS
// ==========================================
if (scenario === '1' || scenario === '2') {
    // 1 e 2: Requisição Única (Baseline e Heavy)
    options.vus = 1;
    options.iterations = 100;
} else if (scenario === '3') {
    // 3: Múltiplas Sequenciais (Em Lote)
    options.vus = 1;
    options.iterations = 10; // Dispara o lote de 100 reqs 10 vezes para tirar a média
} else if (scenario === '4') {
    // 4: Concorrência Leve
    options.vus = 10;
    options.duration = '30s';
} else if (scenario === '5') {
    // 5: Concorrência Média
    options.vus = 50;
    options.duration = '30s';
} else if (scenario === '6') {
    // 6: Concorrência Alta (Estresse)
    options.vus = 100;
    options.duration = '30s';
}

// ==========================================
// FUNÇÃO DE ATAQUE
// ==========================================
export default function () {
    const base_url = `http://localhost:${port}/api/packages/search?origin=BSB&destination=GIG&departureDate=2026-06-10&returnDate=2026-06-15&travelers=2`;

    if (scenario === '3') {
        let reqs = [];
        for (let i = 0; i < 200; i++) {
            reqs.push(`${base_url}&maxResults=2`);
        }
        
        let responses = http.batch(reqs);
        
        check(responses[0], {
            'status is 200': (r) => r.status === 200,
        });
        
        sleep(1); // Pausa de 1s entre os lotes
        
    } else {
        // Lógica para os Cenários 1, 2, 4, 5 e 6
        let maxResults = (scenario === '2') ? 200 : 2;
        let url = `${base_url}&maxResults=${maxResults}`;
        
        let res = http.get(url);
        
        check(res, {
            'status is 200': (r) => r.status === 200,
        });

        // Pausa apenas nos testes sem concorrência
        if (scenario === '1' || scenario === '2') {
            sleep(0.1);
        }
    }
}