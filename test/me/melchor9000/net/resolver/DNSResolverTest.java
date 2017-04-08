/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2016  melchor629 (melchor9000@gmail.com)

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

package me.melchor9000.net.resolver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.melchor9000.net.IOService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for {@link DNSResolver}
 */
public class DNSResolverTest {
    private static IOService service;
    private static DNSResolver resolver;

    @BeforeClass
    public static void setUp() {
        service = new IOService();
        resolver = new DNSResolver(service);
    }

    @AfterClass
    public static void tearDown() {
        resolver.close();
        service.cancel();
    }

    @Test
    public void deserializeCorrectly1() {
        //Obtained by `dig www.google.com`, the sent packet (Captured with Wireshark)
        ByteBuf buff = Unpooled.wrappedBuffer(new byte [] {
            0x7c, (byte)0x8d, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x03, 0x77, 0x77, 0x77,
            0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x03,
            0x63, 0x6f, 0x6d, 0x00, 0x00, 0x01, 0x00, 0x01
        });

        DNSMessage message = new DNSMessage();
        message.setId(0x7C8D);
        message.fromByteBuf(buff);

        assertEquals(0x7C8D, message.getId());
        assertFalse(message.isQueryOrResponse());
        assertEquals(0, message.getOpcode());
        assertFalse(message.isTruncated());
        assertTrue(message.isRecursionDesired());
        assertEquals(1, message.getCountQueries());
        assertEquals(0, message.getCountAnswers());
        assertEquals(0, message.getCountAuthorities());
        assertEquals(0, message.getCountAdditionals());

        DNSQuery query = message.getQueries().iterator().next();
        assertEquals("www.google.com", query.getName());
        assertEquals(DNSUtils.typeToInt("A"), query.getType());
        assertEquals(DNSUtils.classToInt("IN"), query.getClass_());
    }

    @Test
    public void deserializeCorrectly2() throws Exception {
        //Obtained randomly using Wireshark, is a response
        ByteBuf buff = Unpooled.wrappedBuffer(new byte [] {
            (byte) 0xbd, (byte) 0x87, (byte) 0x81, (byte) 0x80, 0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x05, 0x65, 0x35, 0x31,
            0x35, 0x33, 0x02, 0x65, 0x39, 0x0a, 0x61, 0x6b,
            0x61, 0x6d, 0x61, 0x69, 0x65, 0x64, 0x67, 0x65,
            0x03, 0x6e, 0x65, 0x74, 0x00, 0x00, 0x01, 0x00,
            0x01, (byte) 0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00,
            0x00, 0x00, 0x13, 0x00, 0x04, 0x02, 0x10, 0x27,
            0x7c
        });

        DNSMessage message = new DNSMessage();
        message.setId(0xBD87);
        message.fromByteBuf(buff);

        assertEquals(0xBD87, message.getId());
        assertTrue(message.isQueryOrResponse());
        assertEquals(0, message.getOpcode());
        assertFalse(message.isAuthoritativeResponse());
        assertFalse(message.isTruncated());
        assertTrue(message.isRecursionDesired());
        assertTrue(message.isRecursionAvailable());
        assertEquals(1, message.getCountQueries());
        assertEquals(1, message.getCountAnswers());
        assertEquals(0, message.getCountAuthorities());
        assertEquals(0, message.getCountAdditionals());

        DNSQuery query = message.getQueries().iterator().next();
        assertEquals("e5153.e9.akamaiedge.net", query.getName());
        assertEquals(DNSUtils.typeToInt("A"), query.getType());
        assertEquals(DNSUtils.classToInt("IN"), query.getClass_());

        DNSResourceRecord record = message.getAnswers().iterator().next();
        assertEquals("e5153.e9.akamaiedge.net", record.getName());
        assertEquals(DNSUtils.typeToInt("A"), record.getType());
        assertEquals(DNSUtils.classToInt("IN"), record.getClass_());
        assertEquals(19, record.getTtl());

        DNSResourceData dataa = record.getData();
        assertEquals(DNSA.class, dataa.getClass());
        DNSA data = (DNSA) dataa;
        assertEquals(InetAddress.getByAddress(new byte[] { 2, 16, 39, 124 }), data.getAddress());
    }

    @Test
    public void serializeCorrectly1() throws Exception {
        ByteBuf buff = Unpooled.buffer(100);

        DNSMessage message = new DNSMessage();
        message.setId(0xDEAD);
        message.setQueryOrResponse(false);
        message.setOpcode((byte) 0);
        message.setAuthoritativeResponse(false);
        message.setTruncated(false);
        message.setRecursionDesired(true);

        DNSQuery query1 = new DNSQuery();
        query1.setClass("IN");
        query1.setType("A");
        query1.setName("melchor9000.me");

        DNSQuery query2 = new DNSQuery();
        query2.setClass("IN");
        query2.setType("AAAA");
        query2.setName("melchor9000.me");

        message.addQuery(query1);
        message.addQuery(query2);

        assertEquals(52, message.byteBufSize());
        message.toByteBuf(buff);

        byte data[] = new byte[message.byteBufSize()];
        buff.readBytes(data);

        assertEquals(0, buff.readableBytes());
        assertArrayEquals(new byte[] {
                (byte) 0xDE, (byte) 0xAD, 0x01, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //message (12)
                0x0B, 109, 101, 108, 99, 104, 111, 114, 57, 48, 48, 48, //melchor9000 (12)
                0x02, 109, 101, //me (3)
                0x00, // \0 (1)
                0x00, 0x01, 0x00, 0x01, //A IN (4)
                0x0B, 109, 101, 108, 99, 104, 111, 114, 57, 48, 48, 48, //melchor9000 (12)
                0x02, 109, 101, //me (3)
                0x00, // \0 (1)
                0x00, 0x1C, 0x00, 0x01 //A IN (4)
        }, data);
    }

    @Test
    public void serializeCorrectly2() throws Exception {
        ByteBuf buff = Unpooled.buffer(160);

        DNSMessage message = new DNSMessage();
        message.setId(0xDEAD);
        message.setQueryOrResponse(false);
        message.setOpcode((byte) 0);
        message.setAuthoritativeResponse(false);
        message.setTruncated(false);
        message.setRecursionDesired(true);

        buff.writerIndex(99);
        DNSA a = new DNSA(buff);
        DNSAAAA aaaa = new DNSAAAA(buff);
        DNSMX mx = new DNSMX(buff);
        buff.clear();

        a.setAddress(192, 168, 1, 101);
        aaaa.setAddress((Inet6Address) Inet6Address.getByName("fd6b:587e:77a::c85:7e1b:1a5e:7fd1"));
        mx.setExchange("mbp-de-melchor.local");
        mx.setPreference(1);

        DNSResourceRecord recordA = new DNSResourceRecord();
        recordA.setName("mbp-de-melchor.local");
        recordA.setType(1);
        recordA.setClass(1);
        recordA.setTtl(123);
        recordA.setData(a);

        DNSResourceRecord recordAAAA = new DNSResourceRecord();
        recordAAAA.setName("mbp-de-melchor.local");
        recordAAAA.setType("AAAA");
        recordAAAA.setClass(1);
        recordAAAA.setTtl(123);
        recordAAAA.setData(aaaa);

        DNSResourceRecord recordMX = new DNSResourceRecord();
        recordMX.setName("mbp-de-melchor.local");
        recordMX.setType("MX");
        recordMX.setClass(1);
        recordMX.setTtl(123);
        recordMX.setData(mx);

        message.addAnswer(recordA);
        message.addAnswer(recordAAAA);
        message.addAnswer(recordMX);

        assertEquals(12 + (12 + 20 + 4) + (12 + 20 + 16) + (12 + 20 + 24), message.byteBufSize());

        byte data[] = new byte[message.byteBufSize()];
        message.toByteBuf(buff);
        buff.readBytes(data);

        assertEquals(0, buff.readableBytes());
        assertArrayEquals(new byte[] {
                (byte) 0xDE, (byte) 0xAD, 0x01, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00,

                0x0E, 0x6d, 0x62, 0x70, 0x2d, 0x64, 0x65, 0x2d, 0x6d, 0x65, 0x6c, 0x63, 0x68, 0x6f, 0x72,
                0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c,
                0x00,
                0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x7B, 0x00, 0x04, (byte) 192, (byte) 168, 1, 101,

                0x0E, 0x6d, 0x62, 0x70, 0x2d, 0x64, 0x65, 0x2d, 0x6d, 0x65, 0x6c, 0x63, 0x68, 0x6f, 0x72,
                0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c,
                0x00,
                0x00, 0x1C, 0x00, 0x01, 0x00, 0x00, 0x00, 0x7B, 0x00, 0x10,
                (byte) 0xfd, 0x6b, 0x58, 0x7e, 0x07, 0x7a, 0x00, 0x00, 0x0c, (byte) 0x085, 0x7e, 0x1b, 0x1a, 0x5e, 0x7f, (byte) 0xd1,

                0x0E, 0x6d, 0x62, 0x70, 0x2d, 0x64, 0x65, 0x2d, 0x6d, 0x65, 0x6c, 0x63, 0x68, 0x6f, 0x72,
                0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c,
                0x00,
                0x00, 0x0F, 0x00, 0x01, 0x00, 0x00, 0x00, 0x7B, 0x00, 0x18,
                0x00, 0x01,
                0x0E, 0x6d, 0x62, 0x70, 0x2d, 0x64, 0x65, 0x2d, 0x6d, 0x65, 0x6c, 0x63, 0x68, 0x6f, 0x72,
                0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c,
                0x00,
        }, data);
    }

    /*@Test
    public void resolvesNameIPv4() throws Exception {
        Set<InetAddress> addresses = toSet(resolver.resolveV4("twitter.com"));
        assertEquals("Must have 4 entries for twitter.com", 4, addresses.size());
        assertTrue("104.244.42.193 is not inside", addresses.contains(InetAddress.getByAddress(new byte[] {104, (byte) 244, 42, (byte) 193})));
        assertTrue("104.244.42.065 is not inside", addresses.contains(InetAddress.getByAddress(new byte[] {104, (byte) 244, 42, (byte)  65})));
        assertTrue("104.244.42.001 is not inside", addresses.contains(InetAddress.getByAddress(new byte[] {104, (byte) 244, 42, (byte)   1})));
        assertTrue("104.244.42.192 is not inside", addresses.contains(InetAddress.getByAddress(new byte[] {104, (byte) 244, 42, (byte) 129})));
    }

    @Test
    public void resolvesCachesRequestsIPv4() {
        Set<InetAddress> ad1 = toSet(resolver.resolveV4("www.google.com")); //Resolve
        Set<InetAddress> ad2 = toSet(DNSResolverCache.getAddressesIPv4("www.google.com")); //Get from caché

        assertEquals("Addresses must be equals", ad1, ad2);
    }

    @Test
    public void resolvesNameIPv6() throws Exception {
        Set<InetAddress> addresses = toSet(resolver.resolveV6("www.facebook.com"));
        assertEquals("Must have 1 entry for www.facebook.com", 1, addresses.size());
        assertTrue("2a03:2880:f11c:8083:face:b00c::25de is not inside", addresses.contains(InetAddress.getByAddress(new byte[] {
                0x2a, 0x03, 0x28, (byte) 0x80, (byte) 0xf1, 0x1c, (byte) 0x00, (byte) 0x83, (byte) 0xfa, (byte) 0xce, (byte) 0xb0, 0x0c, 0x00, 0x00, 0x25, (byte) 0xde
        })) || addresses.contains(InetAddress.getByAddress(new byte[] {
                0x2a, 0x03, 0x28, (byte) 0x80, (byte) 0xf1, 0x1c, (byte) 0x80, (byte) 0x83, (byte) 0xfa, (byte) 0xce, (byte) 0xb0, 0x0c, 0x00, 0x00, 0x25, (byte) 0xde
        })));
    }

    @Test
    public void resolvesCachesRequestsIPv6() {
        Set<InetAddress> ad1 = toSet(resolver.resolveV6("www.google.com")); //Resolve
        Set<InetAddress> ad2 = toSet(DNSResolverCache.getAddressesIPv6("www.google.com")); //Get from caché

        assertEquals("Addresses must be equals", ad1, ad2);
    }*/

    private <T> Set<T> toSet(Iterable<T> iterable) {
        Set<T> set = new HashSet<>();
        for(T o : iterable) {
            set.add(o);
        }
        return set;
    }
}
