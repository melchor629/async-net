/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2017  melchor629 (melchor9000@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelOption;
import me.melchor9000.net.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;

public class TestSSL {
    private static JTextPane resultado;
    public static void main(String[] args) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        JFrame ventanica = new JFrame("HTTPS test");
        ventanica.setBounds((width - 500) / 2, (height - 400) / 2, 500, 400);

        resultado = new JTextPane();
        resultado.setEditable(true);
        resultado.setContentType("text/txt");
        resultado.setEditable(false);

        final JTextField direccion = new JTextField();
        JScrollPane scrollPane = new JScrollPane(resultado);
        final JLabel bytesSentLabel = new JLabel("Bytes Sent: 0B");
        final JLabel bytesReceivedLabel = new JLabel("Bytes Received: 0B");
        final JLabel timeSpent = new JLabel("Time: 0ms");
        timeSpent.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout(1, 3));
        bottomPanel.add(bytesSentLabel, BorderLayout.WEST);
        bottomPanel.add(timeSpent, BorderLayout.CENTER);
        bottomPanel.add(bytesReceivedLabel, BorderLayout.EAST);

        ventanica.setLayout(new BorderLayout(3, 1));
        ventanica.add(direccion, BorderLayout.NORTH);
        ventanica.add(scrollPane, BorderLayout.CENTER);
        ventanica.add(bottomPanel, BorderLayout.SOUTH);

        ventanica.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ventanica.setVisible(true);

        final IOService service = new IOService();

        direccion.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SSLSocket socket = new SSLSocket(service);
                resultado.setText("");
                bytesSentLabel.setText("Bytes Sent: 0B");
                bytesReceivedLabel.setText("Bytes Received: 0B");
                timeSpent.setText("Time: 0ms");
                direccion.setEnabled(false);

                String addr = direccion.getText();
                String host, path = "/";
                int puerto = 80;
                try {
                    URL url = new URL((addr.startsWith("https://") ? "" : "https://") + addr);
                    host = url.getHost();
                    path = url.getPath().isEmpty() ? "/" : url.getPath();
                    puerto = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
                } catch(MalformedURLException e1) {
                    String as[] = addr.split(":");
                    host = as[0];
                    if(as.length > 1) {
                        puerto = Integer.parseInt(as[1]);
                    }
                }
                final String request = "GET " + path + " HTTP/1.1\r\n" +
                        "Accept-Charset: utf-8\r\n" +
                        "User-Agent: JavaNettyMelchor629\r\n" +
                        "Host: " + host + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

                Callback<Future<Void>> l = new Callback<Future<Void>>() {
                    @Override
                    public void call(Future<Void> arg) {
                        final long start = System.currentTimeMillis();
                        final ByteBuf b = ByteBufAllocator.DEFAULT.buffer(16 * 1024).retain();
                        socket.onClose().whenDone(new Callback<Future<Void>>() {
                            @Override
                            public void call(Future<Void> arg) {
                                direccion.setEnabled(true);

                                long spent = System.currentTimeMillis() - start;
                                timeSpent.setText(String.format("Time spent: %dms", spent));
                                b.release();
                            }
                        });
                        socket.setOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
                        socket.setOption(ChannelOption.SO_TIMEOUT, 5000);

                        socket.sendAsync(ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, request)).whenDone(new Callback<Future<Void>>() {
                            @Override
                            public void call(Future<Void> arg) {
                                bytesSentLabel.setText("Bytes Sent: "+socket.sendBytes()+"B");
                                final Callback<Future<Long>> cbk = new Callback<Future<Long>>() {
                                    @Override
                                    public void call(Future<Long> arg) {
                                        bytesReceivedLabel.setText("Bytes Received: "+socket.receivedBytes()+"B");
                                        if(!arg.isSuccessful()) return;

                                        byte b1[] = new byte[(int) (long) arg.getValueNow()];
                                        b.getBytes(0, b1);
                                        resultado.setText(resultado.getText() + new String(b1)
                                                .replace("\r", "\\r")
                                                .replace("\n", "\\n\n")
                                                                  + "");
                                        b.setIndex(0, 0);
                                        socket.receiveAsync(b).whenDone(this);
                                    }
                                };
                                socket.receiveAsync(b).whenDone(cbk);
                            }
                        });
                    }
                };

                try {
                    socket.connectAsync(host, puerto).whenDone(l);
                } catch(Throwable ignore){}
            }
        });

        ventanica.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                service.cancel();
            }
        });
    }
}
