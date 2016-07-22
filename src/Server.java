import java.nio.charset.Charset;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Server {

	private int port;

	public Server(int port) {
		this.port = port;
	}

	public void run() throws Exception {

		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
					.childHandler(new SimpleChatServerInitializer()) // (4)
					.option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

			System.out.println("SimpleChatServer 启动了");

			// 绑定端口，开始接收进来的连接
			ChannelFuture f = b.bind(port).sync(); // (7)

			// 等待服务器 socket 关闭 。
			// 在这个例子中，这不会发生，但你可以优雅地关闭你的服务器。
			f.channel().closeFuture().sync();

		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();

			System.out.println("SimpleChatServer 关闭了");
		}
	}

	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		new Server(port).run();

	}

	public static  class SimpleChatServerHandler extends SimpleChannelInboundHandler<String> {

	    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	    @Override
	    public void handlerAdded(ChannelHandlerContext ctx) throws Exception { 
	        Channel incoming = ctx.channel();
	        for (Channel channel : channels) {
	            channel.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 加入\n");
	        }
	        channels.add(ctx.channel());
	    }

	    @Override
	    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {  
	        Channel incoming = ctx.channel();
	        for (Channel channel : channels) {
	            channel.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 离开\n");
	        }
	        channels.remove(ctx.channel());
	    }
	    @Override
	    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception { 
	        Channel incoming = ctx.channel();
	        for (Channel channel : channels) {
	            if (channel != incoming){
	                channel.writeAndFlush("[" + incoming.remoteAddress() + "]" + s + "\n");
	            } else {
	                channel.writeAndFlush("[you]" + s + "\n");
	            }
	        }
	    }

	    @Override
	    public void channelActive(ChannelHandlerContext ctx) throws Exception { // (5)
	        Channel incoming = ctx.channel();
	        System.out.println("SimpleChatClient:"+incoming.remoteAddress()+"在线");
	    }

	    @Override
	    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
	        Channel incoming = ctx.channel();
	        System.out.println("SimpleChatClient:"+incoming.remoteAddress()+"掉线");
	    }
	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (7)
	        Channel incoming = ctx.channel();
	        System.out.println("SimpleChatClient:"+incoming.remoteAddress()+"异常");
	        // 当出现异常就关闭连接
	        cause.printStackTrace();
	        ctx.close();
	    }
	}

	public class SimpleChatServerInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();

			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
			pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
			pipeline.addLast(new StringDecoder(Charset.forName("UTF-8")));
			pipeline.addLast("handler", new SimpleChatServerHandler());

			System.out.println("SimpleChatClient:" + ch.remoteAddress() + "连接上");
		}
	}
}