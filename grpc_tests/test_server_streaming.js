const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const path = require('path');

const PROTO_PATH = path.resolve(__dirname, '../proto/travel.proto');
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
    keepCase: true, longs: String, enums: String, defaults: true, oneofs: true
});
const travelProto = grpc.loadPackageDefinition(packageDefinition).travel.v1;

const flightClient = new travelProto.FlightService('localhost:50051', grpc.credentials.createInsecure());

console.log("Teste do tipo de comunicação SERVER-STREAMING (1 Pedido -> Várias Respostas)");
console.log("---------------------------------------------------------");
console.log("[Cliente] A enviar pedido de busca (BSB -> GRU)...\n");

const request = { origin: 'BSB', destination: 'GRU', passengers: 1 };

const call = flightClient.StreamFlights(request);
let count = 0;

call.on('data', (flight) => {
    count++;
    console.log(`[Servidor enviou] Pacote ${count}: Voo da ${flight.airline} (${flight.flight_id})`);
    
    if (count === 5) {
        console.log("\n[Cliente] Interrompendo fluxo para teste. Concluído.");
        call.cancel();
    }
});

call.on('end', () => {
    console.log("[Servidor] Fluxo encerrado.");
});

call.on('error', (err) => {
    if (err.code !== grpc.status.CANCELLED) console.error(err);
});