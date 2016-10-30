import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import me.melchor9000.net.IOService;
import me.melchor9000.net.TCPAcceptor;
import me.melchor9000.net.TCPSocket;

public class TestTCPServer {
    public static void main(String... args) throws Throwable {
        IOService service = new IOService();
        TCPAcceptor acceptor = new TCPAcceptor(service);
        acceptor.setOption(ChannelOption.SO_BACKLOG, 3);
        acceptor.setChildOption(ChannelOption.TCP_NODELAY, true);
        acceptor.setChildOption(ChannelOption.SO_KEEPALIVE, true);

        acceptor.bind(4321);
        TCPSocket con = acceptor.accept();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(16*1000);
        byte[] bytea = new byte[buffer.capacity()];
        long bytes;

        System.out.println("Connected " + con.remoteEndpoint());
        bytes = con.receive(buffer);
        while(bytes != -1 && buffer.getByte(0) != 4) {
            buffer.readBytes(bytea, 0, (int) bytes);
            System.out.print(new String(bytea, 0, (int) bytes));
            buffer.readerIndex(0).writerIndex(0);
            con.send(buffer, (int) bytes);
            buffer.readerIndex(0).writerIndex(0);
            bytes = con.receive(buffer);
        }

        System.out.println("Connection closed");
        con.close();

        acceptor.close();
        service.cancel();
    }
}
