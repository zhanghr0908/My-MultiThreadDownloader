# My-MultiThreadDownloader
采用纯Java代码设计了一个多线程下载器，并且具有断点续传的功能

# 多线程下载器

- 采用纯Java代码，旨在深入理解和应用多线程
- 使用原子类LongAdder，保证了该实例变量的原子性和可见性
- 使用JavaIO流实现灵活的读写文件，利用RandomAccessFile的seek可以定位要写入或读出的绝对位置（相对于文件的起点），这样就可以实现“断点续传”的功能
- 从网络中获取下载链接去下载文件，设计一个HttpUtils类，设计获取下载链接地址、获取下载文件名、获取下载文件大小、获取分块下载链接地址等方法
- 使用了自定义日志类，配合多线程能够实现周期性打印下载时的信息
- 自定义文件类，获取本地已下载文件的大小（用于“断点续传”）
- 使用线程池去实现多线程下载，并采用CountDownLatch（倒计时器）将多个线程阻塞在一个地方，直到所有线程的任务都执行完毕。（用于多线程下载文件之后的文件合并任务）
- 断点续传大概逻辑：先使用文件类获取本地已下载文件的总量，之后通过各个临时文件的容量来计算不同线程下载区间的“起始位置”，再利用RandomAccessFile的seek定位到已下载文件的写入位置，从该位置的下一个字节开始写入。然后计算分块下载的“起始位置”和“结束位置”，利用HttpUtils类从网络中获取分块下载链接地址然后写入，最后合并所有临时文件，合并完成后删除掉所有的临时文件。