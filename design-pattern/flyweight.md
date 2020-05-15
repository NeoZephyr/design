当系统中存在大量重复对象的时候，且这些重复的对象是不可变对象，就可以利用享元模式将对象设计成享元，在内存中只保留一份实例，供多处代码引用。这样可以减少内存中对象的数量，起到节省内存的目的

实际上，对于相似对象，也可以将这些对象中相同的部分提取出来，设计成享元，让这些大量相似对象引用这些享元


优化棋牌游戏
```java
public class ChessPieceUnit {
    private int id;
    private String text;
    private Color color;

    public ChessPieceUnit(int id, String text, Color color) {
        this.id = id;
        this.text = text;
        this.color = color;
    }

    public static enum Color {
        RED, BLACK
    }
}
```

```java
public class ChessPieceUnitFactory {
    private static final Map<Integer, ChessPieceUnit> pieces = new HashMap<>();

    static {
        pieces.put(1, new ChessPieceUnit(1, "車", ChessPieceUnit.Color.BLACK));
        pieces.put(2, new ChessPieceUnit(2, "馬", ChessPieceUnit.Color.BLACK));
    }

    public static ChessPieceUnit getChessPiece(int chessPieceId) {
        return pieces.get(chessPieceId);
    }
}
```

```java
public class ChessPiece {
    private ChessPieceUnit chessPieceUnit;
    private int positionX;
    private int positionY;

    public ChessPiece(ChessPieceUnit unit, int positionX, int positionY) {
        this.chessPieceUnit = unit;
        this.positionX = positionX;
        this.positionY = positionY;
    }
}
```

利用工厂类来缓存 ChessPieceUnit 信息。通过工厂类获取到的 ChessPieceUnit 就是享元。所有的 ChessBoard 对象共享这 30 个 ChessPieceUnit 对象
```java
public class ChessBoard {
    private Map<Integer, ChessPiece> chessPieces = new HashMap<>();

    public ChessBoard() {
        init();
    }

    private void init() {
        chessPieces.put(1, new ChessPiece(
            ChessPieceUnitFactory.getChessPiece(1), 0, 0));
        chessPieces.put(2, new ChessPiece(
            ChessPieceUnitFactory.getChessPiece(2), 0, 0));
    }

    public void move(int chessPieceId, int toPositionX, int toPositionY) {}
}
```

优化文本编辑器，将字体格式设计成享元
```java
public class CharacterStyle {
    private Font font;
    private int size;
    private int colorRGB;

    public CharacterStyle(Font font, int size, int colorRGB) {
        this.font = font;
        this.size = size;
        this.colorRGB = colorRGB;
    }

    public boolean equals(Object o) {
        CharacterStyle otherStyle = (CharacterStyle) o;
        return font.equals(otherStyle.font)
            && size == otherStyle.size
            && colorRGB == otherStyle.colorRGB;
    }
}
```

```java
public class CharacterStyleFactory {
    private static final List<CharacterStyle> styles = new ArrayList<>();

    public static CharacterStyle getStyle(Font font, int size, int colorRGB) {
        CharacterStyle newStyle = new CharacterStyle(font, size, colorRGB);

        for (CharacterStyle style : styles) {
            if (style.equals(newStyle)) {
                return style;
            }
        }

        styles.add(newStyle);
        return newStyle;
    }
}
```

```java
public class Character {
    private char c;
    private CharacterStyle style;

    public Character(char c, CharacterStyle style) {
        this.c = c;
        this.style = style;
    }
}
```

```java
public class Editor {
    private List<Character> chars = new ArrayList<>();

    public void appendCharacter(char c, Font font, int size, int colorRGB) {
        Character character = new Character(c, CharacterStyleFactory.getStyle(font));
        chars.add(character);
    }
}
```

享元模式和多例有很多相似之处，但从设计意图上来看，享元模式是为了对象复用，节省内存；多例模式是为了限制对象的个数

享元模式中的缓存是存储的意思，而我们平时所说的数据库、CPU 缓存主要是为了提高访问效率，而非复用

享元模式中的复用可以理解为共享使用，在整个生命周期中，都是被所有使用者共享的，主要目的是节省空间。池化技术中的复用可以理解为重复使用，主要目的是节省时间。在任意时刻，每一个对象、连接、线程，并不会被多处使用，而是被一个使用者独占，当使用完成之后，放回到池中，再由其他使用者重复利用

