## 里式替换原则
子类对象能够替换程序中父类对象出现的任何地方，并且保证原来程序的逻辑行为不变及正确性不被破坏

多态和里式替换有点类似，但多态是面向对象编程语言的一种语法，是一种代码实现的思路。而里式替换是一种设计原则，是用来指导继承关系中子类该如何设计的，子类的设计要保证在替换父类的时候，不改变原有程序的逻辑以及不破坏原有程序的正确性

### 违反里式替换原则
1. 子类违背父类声明要实现的功能
2. 子类违背父类对输入、输出、异常的约定
3. 子类违背父类注释中所罗列的任何特殊说明


