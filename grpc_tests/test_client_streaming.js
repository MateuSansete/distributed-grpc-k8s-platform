const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

const PROTO_PATH = path.resolve(__dirname, '../proto/travel.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true, longs: String, enums: String, defaults: true, oneofs: true
});
const travelProto = grpc.loadPackageDefinition(packageDefinition).travel.v1;

const hotelClient = new travelProto.HotelService('localhost:50052', grpc.credentials.createInsecure());

console.log("Teste do tipo de comunicação CLIENT-STREAMING (Vários Pedidos -> 1 Resposta Agregada)");
console.log("-----------------------------------------------------------------");

const call = hotelClient.BulkSearchHotels((error, response) => {
    if (error) {
        console.error(error);
        return;
    }
    console.log(`\n[Servidor Respondeu] Análise concluída!`);
    console.log(`-> O servidor agregou os resultados e encontrou um total de ${response.total_found} hotéis.`);
});

const cidades = ['BSB', 'GIG', 'GRU', 'REC'];

cidades.forEach((cidade, index) => {
    setTimeout(() => {
        console.log(`[Cliente] A enviar pedido de busca de hotéis para a cidade: ${cidade}...`);
        call.write({ city: cidade, guests: 2 });
        
        if (index === cidades.length - 1) {
            console.log("\n[Cliente] Fluxo de pedidos encerrado.");
            call.end();
        }
    }, index * 800); 
});