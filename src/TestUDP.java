import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.melchor9000.net.Callback;
import me.melchor9000.net.Future;
import me.melchor9000.net.IOService;
import me.melchor9000.net.UDPSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class TestUDP {
    public static void main(String... args) throws Exception {
        final IOService service = new IOService();
        final UDPSocket socket = new UDPSocket(service);

        String a;
        if(args.length == 0) {
            System.out.print("Ponga un dominio para consultar: ");
            a = new Scanner(System.in).nextLine();
        } else {
            a = args[0];
        }

        final String domain = a;

        System.out.println("Consultando sobre " + domain);
        System.out.println();
        socket.bind();

        final ByteBuf b = ByteBufAllocator.DEFAULT.buffer(500);
        b.writeShort((short) (ThreadLocalRandom.current().nextInt() & 0xFFFF));
        b.writeShort((short) 0x0100);
        b.writeShort((short) 0x0001);
        b.writeShort((short) 0x0000);
        b.writeShort((short) 0x0000);
        b.writeShort((short) 0x0000);
        writeName(b, domain);
        b.writeShort((short) 0x0001);
        b.writeShort((short) 0x0001);

        socket.sendAsyncTo(b, 17 + domain.length() + 1, new InetSocketAddress("192.168.1.1", 53)).whenDone(new Callback<Future<Void>>() {
            @Override
            public void call(Future<Void> arg) throws Exception {
                socket.receiveAsyncFrom(b).whenDone(new Callback<Future<UDPSocket.Packet>>() {
                    @Override
                    public void call(Future<UDPSocket.Packet> arg) throws Exception {
                        short queries, answers, flags;
                        System.out.printf("Transaction ID: 0x%x\n", b.readShort());
                        System.out.printf("Flags: 0x%x\n", flags = b.readShort());
                        System.out.printf("Questions: %d\n", queries = b.readShort());
                        System.out.printf("Answers RRs: %d\n", answers = b.readShort());
                        System.out.printf("Authority RRs: %d\n", b.readShort());
                        System.out.printf("Additional RRs: %d\n", b.readShort());
                        System.out.println();

                        if((flags & 0xF) != 0) {
                            System.out.println("Error: " + errorToString(flags & 0xF));
                            System.exit(flags & 0xF);
                        }

                        System.out.println("Queries");
                        for(short i = 0; i < queries; i++) {
                            System.out.println("  Name: " + readName(b));
                            System.out.println("  Type: " + typeToString(b.readShort()));
                            System.out.println("  Class: " + classToString(b.readShort()));
                            System.out.println();
                        }

                        System.out.println("Answers");
                        for(short i = 0; i < answers; i++) {
                            System.out.println("  Name: " + readName(b));
                            System.out.println("  Type: " + typeToString(b.readShort()));
                            System.out.println("  Class: " + classToString(b.readShort()));
                            System.out.println("  TTL: " + b.readUnsignedInt());
                            byte[] data = readData(b);
                            System.out.println("  Data length: " + data.length);
                            System.out.println("  Data: " + new String(data) + " - " + getIP(data));
                            System.out.println();
                        }

                        System.exit(0); //No mola nada esto, pero bueno
                    }
                });
            }
        });
    }

    private static void writeName(ByteBuf b, String domain) {
        String labels[] = domain.split("\\.");
        for(String label : labels) {
            b.writeByte((byte) label.length()).writeBytes(label.getBytes());
        }
        b.writeByte((byte) 0);
    }

    private static String readName(ByteBuf b) {
        byte bytE = b.readByte();
        if((bytE & 0xC0) == 0xC0) return readName(b, b.readByte(), b.readerIndex());
        else return readName(b, b.readerIndex() - 1, -1);
    }

    private static String readName(ByteBuf b, int position, int finalPos) {
        int length;
        b.readerIndex(position);

        String domain = "";
        while((length = b.readByte()) != 0) {
            if((length & 0xC0) != 0xC0) {
                byte label[] = new byte[length];
                b.readBytes(label);
                domain += "." + new String(label);
            } else {
                domain += "." + readName(b, b.readByte(), b.readerIndex());
            }
        }

        if(finalPos != -1) b.readerIndex(finalPos);
        return domain.substring(1);
    }

    private static byte[] readData(ByteBuf b) {
        int length = b.readShort();
        byte data[] = new byte[length];
        b.readBytes(data);
        return data;
    }

    private static String getIP(byte[] a) {
        try {
            return InetAddress.getByAddress(a).getHostAddress();
        } catch(UnknownHostException e) {
            return null;
        }
    }

    private static String typeToString(short type) {
        switch(type) {
            case 1: return "A";
            case 2: return "NS";
            case 3: return "MD";
            case 4: return "MF";
            case 5: return "CNAME";
            case 6: return "SOA";
            case 7: return "MB";
            case 8: return "MG";
            case 9: return "MR";
            case 10:return "NULL";
            case 11:return "WKS";
            case 12:return "PTR";
            case 13:return "HINFO";
            case 14:return "MINFO";
            case 15:return "MX";
            case 16:return "TXT";
            default:return null;
        }
    }

    private static String classToString(short clasS) {
        switch(clasS) {
            case 1: return "IN";
            case 2: return "CS";
            case 3: return "CH";
            case 4: return "HS";
            default:return null;
        }
    }

    private static String errorToString(int error) {
        switch(error) {
            case 0: return "No error";
            case 1: return "Format error";
            case 2: return "Server failure";
            case 3: return "Name error";
            case 4: return "Not implemented";
            case 5: return "Refused";
            default:return null;
        }
    }
}
