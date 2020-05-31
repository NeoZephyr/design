如果对象的创建成本比较大，而同一个类的不同对象之间差别不大（大部分字段都相同），可以利用对已有对象进行复制的方式来创建新对象，以达到节省创建时间的目的

```java
public class Demo {
    private HashMap<String, SearchWord> currentKeyWords = new HashMap<>();
    private long lastUpdateTime = -1;

    public void refresh() {
        HashMap<String, SearchWord> newKeyWords = (HashMap<String, SearchWord>)currentKeyWords.clone();
        List<SearchWord> toBeUpdatedSearchWords = getSearchWords(lastUpdateTime);
        long maxNewUpdatedTime = lastUpdateTime;

        for (SearchWord searchWord : toBeUpdatedSearchWords) {
            if (searchWord.getLastUpdateTime() > maxNewUpdatedTime) {
                maxNewUpdatedTime = searchWord.getLastUpdateTime();
            }

            if (newKeywords.containsKey(searchWord.getKeyword())) {
                SearchWord oldSearchWord = newKeywords.get(searchWord.getKeyword());
                oldSearchWord.setCount(searchWord.getCount());
                oldSearchWord.setLastUpdateTime(searchWord.getLastUpdateTime());
            } else {
                newKeywords.put(searchWord.getKeyword(), searchWord);
            }
        }

        lastUpdateTime = maxNewUpdatedTime;
        currentKeywords = newKeywords;
    }
}
```

浅拷贝只会复制索引，不会复制数据（SearchWord 对象）本身；深拷贝不仅仅会复制索引，还会复制数据本身

在 Java 语言中，Object 类的 clone() 方法执行的就是浅拷贝。它只会拷贝对象中的基本数据类型的数据，以及引用对象的内存地址，不会递归地拷贝引用对象本身

因此，当我们通过 newKeywords 更新 SearchWord 对象的时候，newKeywords 和 currentKeywords 因为指向相同的一组 SearchWord 对象，就会导致 currentKeywords 中指向的 SearchWord，有的是老版本的，有的是新版本的，不能保证数据在任何时刻都是同一个版本的

递归拷贝实现深拷贝
```java
public class Demo {
    private HashMap<String, SearchWord> currentKeywords = new HashMap<>();
    private long lastUpdateTime = -1;

    public void refresh() {
        HashMap<String, SearchWord> newKeywords = new HashMap<>();
        for (HashMap.Entry<String, SearchWord> e : currentKeywords.entrySet()) {
            SearchWord searchWord = e.getValue();
            SearchWord newSearchWord = new SearchWord(searchWord.getKeyword(), searchWord.getCount(), searchWord.getLastUpdateTime());
            newKeywords.put(e.getKey(), newSearchWord);
        }

        List<SearchWord> toBeUpdatedSearchWords = getSearchWords(lastUpdateTime);
        long maxNewUpdatedTime = lastUpdateTime;
        for (SearchWord searchWord : toBeUpdatedSearchWords) {
            if (searchWord.getLastUpdateTime() > maxNewUpdatedTime) {
                maxNewUpdatedTime = searchWord.getLastUpdateTime();
            }

            if (newKeywords.containsKey(searchWord.getKeyword())) {
                SearchWord oldSearchWord = newKeywords.get(searchWord.getKeyword());
                oldSearchWord.setCount(searchWord.getCount());
                oldSearchWord.setLastUpdateTime(searchWord.getLastUpdateTime());
            } else {
                newKeywords.put(searchWord.getKeyword(), searchWord);
            }
        }

        lastUpdateTime = maxNewUpdatedTime;
        currentKeywords = newKeywords;
    }
}
```

序列化实现深拷贝
```java
public Object deepCopy(Object object) {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutputStream oo = new ObjectOutputStream(bo);
    oo.writeObject(object);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInputStream oi = new ObjectInputStream(bi);

    return oi.readObject();
}
```

先采用浅拷贝的方式创建 newKeywords。对于需要更新的 SearchWord 对象，使用深度拷贝的方式创建一份新的对象，替换 newKeywords 中的老对象

```java
public class Demo {
    private HashMap<String, SearchWord> currentKeywords=new HashMap<>();
    private long lastUpdateTime = -1;

    public void refresh() {
        HashMap<String, SearchWord> newKeywords = (HashMap<String, SearchWord>) currentKeywords.clone();
        List<SearchWord> toBeUpdatedSearchWords = getSearchWords(lastUpdateTime);
        long maxNewUpdatedTime = lastUpdateTime;

        for (SearchWord searchWord : toBeUpdatedSearchWords) {
            if (searchWord.getLastUpdateTime() > maxNewUpdatedTime) {
                maxNewUpdatedTime = searchWord.getLastUpdateTime();
            }

            if (newKeywords.containsKey(searchWord.getKeyword())) {
                newKeywords.remove(searchWord.getKeyword());
            }

            newKeywords.put(searchWord.getKeyword(), searchWord);
        }

        lastUpdateTime = maxNewUpdatedTime;
        currentKeywords = newKeywords;
    }
}
```












