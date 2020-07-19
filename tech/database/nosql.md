## 提升写入性能
### MySQL
在 MySQL 的 InnoDB 存储引擎中，更新 binlog、redolog、undolog 都是在做顺序 IO，而更新 datafile 和索引文件则是在做随机 IO

而为了减少随机 IO 的发生，关系数据库做了很多的优化。比如批量写磁盘，但是随机 IO 还是会发生

在数据插入或者更新的时候，需要找到要插入的位置，再把数据写到特定的位置上，这就产生了随机的 IO。而且一旦发生了页分裂，就不可避免会做数据的移动，也会极大地损耗写入性能

### NoSQL
很多 NoSQL 数据库都在使用的基于 LSM 树的存储引擎

LSM 树牺牲了一定的读性能来换取写入数据的高性能。数据首先会写入到 MemTable 内存结构中，在 MemTable 中数据是按照写入的 Key 来排序的。为了防止 MemTable 里面的数据因为机器掉电或者重启而丢失，一般会通过写 Write Ahead Log 的方式将数据备份在磁盘上

MemTable 在累积到一定规模时，会被刷新生成一个新的文件，这个文件叫做 SSTable。当 SSTable 达到一定数量时，将这些 SSTable 合并，减少文件的数量。因为 SSTable 都是有序的，所以合并的速度也很快

当从 LSM 树里面读数据时，首先从 MemTable 中查找数据，如果数据没有找到，再从 SSTable 中查找数据。因为存储的数据都是有序的，所以查找的效率是很高的，只是因为数据被拆分成多个 SSTable，所以读取的效率会低于 B+ 树索引

类似 LSM 树的算法有很多，比如 TokuDB 使用的名为 Fractal tree 的索引结构，它们的核心思想就是将随机 IO 变成顺序的 IO，从而提升写入的性能