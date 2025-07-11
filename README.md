# Stateful Event Processor

It's a sophisticated Spark Structured Streaming application that demonstrates stateful processing for event freshness tracking.

1. Ingests from two sockets (9999/9998) with different schemas - one for events with processing_time+id, another for source_time only
2. Maintains state per event name using flatMapGroupsWithState to track latest processing_time, source_time, and computed freshness_time
3. Calculates freshness as the minimum of available timestamps, showing data latency/delay
4. Outputs only events with IDs every 15 seconds, filtering pure timing events

The stateful processing is the key - it keeps track of the latest timestamps across micro-batches and computes freshness metrics, which is useful for monitoring data pipeline health and SLA
compliance.

## Running the Application

1. Create a new tmux session and split into panes:
```bash
# 1. Create the session and first pane
tmux new-session -d -s demo -n main
# 2. Split window horizontally (creates pane 1)
tmux split-window -h -t demo:0
# 3. Split left pane vertically (creates pane 2 from pane 0)
tmux select-pane -t demo:0.1
tmux split-window -v -t demo:0.1
```

2. Start processes in each pane:
```bash
tmux send-keys -t demo:0.0 "sbt run" Enter
tmux send-keys -t demo:0.1 "nc -lk 9999" Enter
tmux send-keys -t demo:0.2 "nc -lk 9998" Enter
```
# 3. Attach to the tmux session to view all processes:
```bash
tmux attach-session -t demo
```

4. Type JSON events in the netcat panes to send them to the application (use panes 1 and 2 for ports 9999/9998):
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