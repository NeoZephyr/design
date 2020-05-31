```java
public class ArrayIterator<E> implements Iterator<E> {
    private int cursor;
    private ArrayList<E> arrayList;

    public ArrayIterator(ArrayList<E> arrayList) {
        this.cursor = 0;
        this.arrayList = arrayList;
    }

    public boolean hasNext() {
        return cursor != arrayList.size();
    }

    public void next() {
        cursor++;
    }

    public E currentItem() {
        if (cursor >= arrayList.size()) {
            throw new NoSuchElementException();
        }

        return arrayList.get(cursor);
    }
}
```
```java
public interface List<E> {
    Iterator iterator();
}

public class ArrayList<E> implements List<E> {
    public Iterator iterator() {
        return new ArrayIterator(this);
    }
}
```

使用迭代器的优点
1. 类似数组和链表这样的数据结构，遍历方式比较简单，可以直接使用 for 循环来遍历。但是，像树、图这样的数据结构，有各种复杂的遍历方式。如果由客户端代码来实现这些遍历算法，势必增加开发成本，而且容易写错。如果将这部分遍历的逻辑写到容器类中，也会导致容器类代码的复杂性。而应对复杂性的方法就是拆分，将遍历操作拆分到迭代器类中。比如，针对图的遍历，可以定义 DFSIterator、BFSIterator 两个迭代器类，分别来实现深度优先遍历和广度优先遍历

2. 将游标指向的当前位置等信息，存储在迭代器类中，每个迭代器独享游标信息。这样，就可以创建多个不同的迭代器，同时对同一个容器进行遍历而互不影响

3. 容器和迭代器都提供了抽象的接口，方便开发过程中，基于接口而非具体的实现编程。当需要切换新的遍历算法的时候，比如，从前往后遍历链表切换成从后往前遍历链表，客户端代码只需要将迭代器类从 LinkedIterator 切换为 ReversedLinkedIterator 即可。除此之外，添加新的遍历算法，只需要扩展新的迭代器类，更符合开闭原则


```java
public class ArrayIterator implements Iterator {
    private int cursor;
    private ArrayList arrayList;
    private int expectedModCount;

    public ArrayIterator(ArrayList arrayList) {
        this.cursor = 0;
        this.arrayList = arrayList;
        this.expectedModCount = arrayList.modCount;
    }

    public boolean hasNext() {
        checkForComodification();
        return cursor < arrayList.size();
    }

    public void next() {
        checkForComodification();
        cursor++;
    }

    public Object currentItem() {
        checkForComodification();
        return arrayList.get(cursor);
    }

    private void checkForComodification() {
        if (arrayList.modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
```

使用成员变量 modCount 记录集合被修改的次数。集合每调用一次增加或删除元素的函数，就会给 modCount 加 1。当创建迭代器的时候，把 modCount 值传递给迭代器的 expectedModCount 成员变量，之后每次调用迭代器上的 hasNext()、next()、currentItem() 函数，都会检查集合上的 modCount 是否等于 expectedModCount。如果不等，那就说明集合存储的元素已经改变了，之前创建的迭代器已经不能正确运行了，再继续使用就会产生不可预期的结果，选择 fail-fast 解决方式，抛出运行时异常，结束掉程序


迭代器类中定义了 remove() 方法，在遍历集合的同时，安全地删除集合中的元素。但是，只能删除游标指向的前一个元素，而且一个 next() 函数之后，只能跟着最多一个 remove() 操作，多次调用 remove() 操作会报错


快照迭代器
在容器中，为每个元素保存两个时间戳，一个是添加时间戳 addTimestamp，一个是删除时间戳 delTimestamp。当元素被加入到集合中的时候，将 addTimestamp 设置为当前时间，将 delTimestamp 设置成最大长整型值。当元素被删除时，将 delTimestamp 更新为当前时间，表示已经被删除

每个迭代器也保存一个迭代器创建时间戳 snapshotTimestamp。当遍历的时候，只有满足 addTimestamp < snapshotTimestamp < delTimestamp 的元素，才是属于这个迭代器的快照。这样就在不拷贝容器的情况下，在容器本身上借助时间戳实现了快照功能

```java
public class ArrayList<E> implements List<E> {
    private static final int DEFAULT_CAPACITY = 10;

    private int actualSize;
    private int totalSize;

    private Object[] elements;
    private long[] addTimestamps;
    private long[] delTimestamps;

    public ArrayList() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.addTimestamps = new long[DEFAULT_CAPACITY];
        this.delTimestamps = new long[DEFAULT_CAPACITY];
        this.totalSize = 0;
        this.actualSize = 0;
    }

    public void add(E obj) {
        elements[totalSize] = obj;
        addTimestamps[totalSize] = System.currentTimeMillis();
        delTimestamps[totalSize] = Long.MAX_VALUE;
        totalSize++;
        actualSize++;
    }

    public void remove(E obj) {
        for (int i = 0; i < totalSize; ++i) {
            if (elements[i].equals(obj)) {
                delTimestamps[i] = System.currentTimeMillis();
                actualSize--;
            }
        }
    }

    public int actualSize() {
        return this.actualSize;
    }

    public int totalSize() {
        return this.totalSize;
    }

    public E get(int i) {
        if (i >= totalSize) {
            throw new IndexOutOfBoundsException();
        }

        return (E)elements[i];
    }

    public long getAddTimestamp(int i) {
        if (i >= totalSize) {
            throw new IndexOutOfBoundsException();
        }

        return addTimestamps[i];
    }

    public long getDelTimestamp(int i) {
        if (i >= totalSize) {
            throw new IndexOutOfBoundsException();
        }

        return delTimestamps[i];
    }
}
```

```java
public class SnapshotArrayIterator<E> implements Iterator<E> {
    private long snapshotTimestamp;
    private int cursorInAll;
    private int leftCount;
    private ArrayList<E> arrayList;

    public SnapshotArrayIterator(ArrayList<E> arrayList) {
        this.snapshotTimestamp = System.currentTimeMillis();
        this.cursorInAll = 0;
        this.leftCount = arrayList.actualSize();
        this.arrayList = arrayList;
        justNext();
    }

    public boolean hasNext() {
        return this.leftCount >= 0;
    }

    public E next() {
        E currentItem = arrayList.get(cursorInAll);
        justNext();
        return currentItem;
    }

    private void justNext() {
        while (cursorInAll < arrayList.totalSize()) {
            long addTimestamp = arrayList.getAddTimestamp(cursorInAll);
            long delTimestamp = arrayList.getDelTimestamp(cursorInAll);

            if (snapshotTimestamp > addTimestamp && snapshotTimestamp < delTimestamp) {
                leftCount--;
                break;
            }
            cursorInAll++;
        }
    }
}
```

快照迭代器缺点：
1. 不支持快速的随机访问。可以在 ArrayList 中存储两个数组。一个支持标记删除的，用来实现快照遍历功能；一个不支持标记删除的用来支持随机访问
2. 删除元素不会从数组中真正移除，导致不必要的内存占用。删除数组元素时，可以将被删除数组元素的引用指向一个 object 常量；在合适的时候清理带删除标记的元素；利用强引用与弱引用

