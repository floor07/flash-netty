package the.flash.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import the.flash.codec.PacketCodecHandler;
import the.flash.codec.Spliter;
import the.flash.handler.IMIdleStateHandler;
import the.flash.server.handler.AuthHandler;
import the.flash.server.handler.HeartBeatRequestHandler;
import the.flash.server.handler.IMHandler;
import the.flash.server.handler.LoginRequestHandler;

import java.util.Date;

public class NettyServer {

    private static final int PORT = 8000;

    public static void main(String[] args) {
        NioEventLoopGroup boosGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(boosGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                /**
                 * ChannelOption.SO_BACKLOG对应的是tcp/ip协议listen函数中的backlog参数
                 * 函数listen(int socketfd,int backlog)用来初始化服务端可连接队列，
                 * 服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接，
                 * 多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，
                 * backlog参数指定了队列的大小
                 */
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                /**
                 * Nagle算法试图减少TCP包的数量和结构性开销, 将多个较小的包组合成较大的包进行发送.但这不是重点,
                 * 关键是这个算法受TCP延迟确认影响, 会导致相继两次向连接发送请求包,
                 * 读数据时会有一个最多达500毫秒的延时.
                 */
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        // 心跳
                        ch.pipeline().addLast(new IMIdleStateHandler());
                        // 拆包器
                        ch.pipeline().addLast(new Spliter());
                        // 编码解码
                        ch.pipeline().addLast(PacketCodecHandler.INSTANCE);
                        //解析登陆包
                        ch.pipeline().addLast(LoginRequestHandler.INSTANCE);
                        //解析心跳包
                        ch.pipeline().addLast(HeartBeatRequestHandler.INSTANCE);
                        //校验是否登录
                        ch.pipeline().addLast(AuthHandler.INSTANCE);
                        //消息处理
                        ch.pipeline().addLast(IMHandler.INSTANCE);
                    }
                });


        bind(serverBootstrap, PORT);
    }

    private static void bind(final ServerBootstrap serverBootstrap, final int port) {
        serverBootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println(new Date() + ": 端口[" + port + "]绑定成功!");
            } else {
                System.err.println("端口[" + port + "]绑定失败!");
            }
        });
    }
}
