行为型：解决类或对象之间的交互问题


```java
public class InputText {
    private StringBuilder text = new StringBuilder();

    public String getText() {
        return text.toString();
    }

    public void append(String input) {
        text.append(input);
    }

    public Snapshot createSnapshot() {
        return new Snapshot(text.toString());
    }

    public void restoreSnapshot(Snapshot snapshot) {
        this.text.replace(0, this.text.length(), snapshot.getText());
    }
}
```

```java
public class Snapshot {
    private String text;

    public Snapshot(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }
}
```

```java
public class SnapshotHolder {
    private Stack<Snapshot> snapshots = new Stack<>();

    public Snapshot popSnapshot() {
        return snapshots.pop();
    }

    public void pushSnapshot(Snapshot snapshot) {
        snapshots.push(snapshot);
    }
}
```

```java
InputText inputText = new InputText();
SnapshotHolder snapshotsHolder = new SnapshotHolder();
Scanner scanner = new Scanner(System.in);

while (scanner.hasNext()) {
    String input = scanner.next();

    if (input.equals(":list")) {
        System.out.println(inputText.toString());
    } else if (input.equals(":undo")) {
        Snapshot snapshot = snapshotsHolder.popSnapshot();
        inputText.restoreSnapshot(snapshot);
    } else {
        snapshotsHolder.pushSnapshot(inputText.createSnapshot());
        inputText.append(input);
    }
}
```