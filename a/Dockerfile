FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY travel.proto server_a_flights.py ./
# Gera os stubs no build (não depende dos *_pb2*.py versionados).
RUN python -m grpc_tools.protoc -I. --python_out=. --grpc_python_out=. travel.proto

EXPOSE 50051
CMD ["python", "-u", "server_a_flights.py"]
