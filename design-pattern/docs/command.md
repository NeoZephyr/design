行为型：解决类或对象之间的交互问题


```java
public interface Command {
    void execute();
}
```

```java
public class GotDiamondCommand implements Command {
    public GotDiamondCommand() {}

    public void execute() {}
}
```

```java
private static final int MAX_HANDLED_REQ_COUNT_PER_LOOP = 100;
private Queue<Command> queue = new LinkedList<>();

public void mainloop() {
    while (true) {
        List<Request> requests = new ArrayList<>();

        for (Request request : requests) {
            Event event = request.getEvent();
            Command command = null;

            if (event.equals(Event.GOT_DIAMOND)) {
                command = new GotDiamondCommand();
            } else if (event.equals(Event.GOT_STAR)) {
                command = new GotStartCommand();
            }

            queue.add(command);
        }

        int handledCount = 0;

        while (handledCount < MAX_HANDLED_REQ_COUNT_PER_LOOP) {
            if (queue.isEmpty()) {
                break;
            }

            Command command = queue.poll();
            command.execute();
        }
    }
}
```