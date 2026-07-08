# spring-template

spring java后端模板代码
created by @dinghaolun


### 介绍顺序
1、询问大家的基础，对java、编程、web服务、http网络的了解 （10分钟）
2、web请求的流程： -- 一个线程 ---> springboot (或springboot自带tomcat) （20分钟）
                
                Java Servlet 是运行在Web 服务器或应用服务器上的程序，它是作为来自Web 浏览器或其他HTTP 客户端的请求和HTTP 服务器上的数据库或应用程序之间的中间层
                http请求体 ---> servlet请求对象 ----> springboot controller

                绘制http请求的从tomcat进入之后的线程图、一直到mysql或redis缓存的过程。

接口：告诉你我接受什么参数，返回给你什么内容
1、interface


展示层 bootstrap（接口）
逻辑层 service
持久层 repository（接口）、infrastructure ---> mysql

3、介绍基本的spring内部服务流程和分层规范（MVC）： controller --> service（rpc） --> repository(infrastructure) ---> mysql redis  （20分钟）
4、介绍一些分层规范、converter、枚举规范、单例模式规范、接口服务规范（入参进来用对象、不用多个参数）。 （10分钟）
5、提一些协作要求、git合作要求。 （10分钟）



