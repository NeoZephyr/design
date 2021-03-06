## 本地缓存
### 协商缓存
浏览器常用的一种就是基于 304 响应状态实现的本地缓存

一般协商缓存可以基于请求头部中的 If-Modified-Since 字段与返回头部中的 Last-Modified 字段实现，也可以基于请求头部中的 If-None-Match 字段与返回头部中的 ETag 字段来实现

前者是基于时间实现的，后者是基于一个唯一标识实现的，相对来说后者可以更加准确地判断文件内容是否被修改，避免由于时间篡改导致的不可靠问题

当浏览器第一次请求访问服务器资源时，服务器会在返回这个资源的同时，在 Response 头部加上 ETag 唯一标识，这个唯一标识的值是根据当前请求的资源生成的

当浏览器再次请求访问服务器中的该资源时，会在 Request 头部加上 If-None-Match 字段，该字段的值就是 Response 头部加上 ETag 唯一标识

服务器再次收到请求后，会根据请求中的 If-None-Match 值与当前请求的资源生成的唯一标识进行比较，如果值相等，则返回 304 Not Modified，如果不相等，则在 Response 头部加上新的 ETag 唯一标识，并返回资源

如果浏览器收到 304 的请求响应状态码，则会从本地缓存中加载资源，否则更新资源

### 强缓存
强缓存指的是只要判断缓存没有过期，则直接使用浏览器的本地缓存。比如，返回的是 200 状态码，但在 size 项中标识的是 memory cache

强缓存利用 Expires 或者 Cache-Control 这两个 HTTP Response Header 实现的，它们都用来表示资源在客户端缓存的有效期

Expires 是一个绝对时间，而 Cache-Control 是一个相对时间，即一个过期时间大小。基于 Expires 实现的强缓存也会因为时间问题导致缓存管理出现问题。建议使用 Cache-Control 来实现强缓存

当浏览器第一次请求访问服务器资源时，服务器会在返回这个资源的同时，在 Response 头部加上 Cache-Control，Cache-Control 中设置了过期时间大小

浏览器再次请求访问服务器中的该资源时，会先通过请求资源的时间与 Cache-Control 中设置的过期时间大小，来计算出该资源是否过期，如果没有，则使用该缓存，否则请求服务器

服务器再次收到请求后，会再次更新 Response 头部的 Cache-Control


## 网关缓存
网关、缓存，也就是 CDN

CDN 缓存是通过不同地点的缓存节点缓存资源副本，当用户访问相应的资源时，会调用最近的 CDN 节点返回请求资源，这种方式常用于视频资源的缓存
