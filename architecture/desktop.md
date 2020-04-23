## MVC
Model 是数据
View 是数据的显示结果，同时也接受用户的交互动作，也就是事件
Controller 接收 Model 和 View 转发的事件作为输入，处理之后更新 Model。Model 数据更新后，会发送数据更新事件，View 在监听并收到数据更新事件后，更新 View

Model 的数据更新发出数据更新事件后，由 Controller 负责监听并更新 View，就转换成了 MVP 架构

### Model
Model 层的使用接口最重要的是要自然体现业务的需求
Model 层作为架构的最底层，不需要知道其他层的存在。只需要通过 DataChanged 事件，上层就能够感知到 Model 层的变化，从而作出自己的反应

### View
View 层首要的责任，是负责界面呈现
View 层另一个责任是是响应用户交互事件的入口。理想的情况下，View 应该把所有的事件都委托出去

负责界面呈现，意味着 View 层和 Model 层的关系非常紧密，以至于 View 需要知道数据结构的细节，这可能会导致 Model 层要为 View 层提供一些专享的只读访问接口

负责界面呈现，需要考虑局部更新的优化。在局部更新这个优化足够复杂时，我们就在 Model 和 View 之间，引入 ViewModel 层来做这个事情

ViewModel 层是为 View 的界面呈现而设计的 Model 层，它的数据组织更接近于 View 的表达，和 View 自身的数据呈一一对应关系

### Controller
Controller 层负责不同的用户交互需求

