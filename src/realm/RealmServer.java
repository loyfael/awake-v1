package realm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import kernel.Cmd;
import kernel.Config;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class RealmServer {
	private static NioSocketAcceptor acceptor;

	public RealmServer() {
		acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(StandardCharsets.UTF_8, LineDelimiter.NUL, new LineDelimiter("\n\0"))));
		acceptor.setHandler(new RealmThread());
	}

	public void start() {//start server
		if(acceptor.isActive()) 
			return;//if acceptor's already launched
		try { 
			acceptor.bind(new InetSocketAddress(Config.REALM_PORT));//enable connection
			if(Config.DEBUG)
				Cmd.println("<< Server online >>", Cmd.Color.BG_GREEN);
		} catch (IOException e) {
			e.printStackTrace();
			Cmd.println("<< Server offline... >>", Cmd.Color.BG_RED);
			System.exit(0);
		}
	}
}