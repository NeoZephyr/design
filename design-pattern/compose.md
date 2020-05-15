将一组对象组织成树形结构，将单个对象和组合对象都看做树中的节点，以统一处理逻辑，并且它利用树形结构的特点，递归地处理每个子树

```java
public abstract class HumanResource {
    protected long id;
    protected double salary;

    public HumanResource(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public abstract double calculateSalary();
}
```

```java
public class Employee extends HumanResource {
    public Employee(long id, double salary) {
        super(id);
        this.salary = salary;
    }

    public double calculateSalary() {
        return salary;
    }
}
```

```java
public class Department extends HumanResource {
    private List<HumanResource> subNodes = new ArrayList<>();

    public Department(long id) {
        super(id);
    }

    public double calculateSalary() {
        double totalSalary = 0;

        for (HumanResource hr : subNodes) {
            totalSalary += hr.calculateSalary();
        }

        this.salary = totalSalary;
        return totalSalary;
    }

    public void addSubNode(HumanResource hr) {
        subNodes.add(hr);
    }
}
```