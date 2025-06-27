# Stateful Event Processor

A Scala-Spark application that ingests events via socket and performs stateful processing to keep only the latest timestamp per event type.

## Running the Application

1. Start the application:
```bash
sbt run
```

2. In another terminal, send JSON events to port 9999:
```bash
nc -lk 9999
```

3. Send JSON events in this format:
```json
{"name": "login", "processing_time": 1640995300000, "id": 1}
{"name": "login", "source_time": 1640995200000}
{"name": "login", "processing_time": 1640995400000, "id": 2}
{"name": "login", "source_time": 1640995400000}
{"name": "login", "processing_time": 1640995100000, "id": 3}
{"name": "login", "source_time": 1640995500000}
{"name": "login", "processing_time": 1640995900000, "id": 4}
{"name": "login", "source_time": 1640995600000}
{"name": "login", "processing_time": 1640995900000, "id": 5}

{"name": "purchase", "processing_time": 1640995300000, "id": 1}
{"name": "purchase", "source_time": 1640995200000}
{"name": "purchase", "processing_time": 1640995400000, "id": 2}
{"name": "purchase", "source_time": 1640995400000}
{"name": "purchase", "processing_time": 1640995100000, "id": 3}
{"name": "purchase", "source_time": 1640995500000}
{"name": "purchase", "processing_time": 1640995900000, "id": 4}
{"name": "purchase", "source_time": 1640995600000}
{"name": "purchase", "processing_time": 1640995900000, "id": 5}
```

The application will output the latest timestamp for each event type to stdout every 5 seconds.