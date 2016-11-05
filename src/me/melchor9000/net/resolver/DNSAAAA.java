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
import me.melchor9000.net.DataNotRepresentsObject;

import java.net.Inet6Address;
import java.net.UnknownHostException;

/**
 * Support for IPv6 hosts<br>
 * @see <a href="https://www.ietf.org/rfc/rfc1886.txt">RFC 1886 - IPv6 DNS Extension</a>
 */
public class DNSAAAA extends DNSResourceData {
    private Inet6Address address;

    DNSAAAA(ByteBuf data) {
        super(data);
    }

    public Inet6Address getAddress() {
        return address;
    }

    public void setAddress(Inet6Address address) {
        this.address = address;
    }

    @Override
    public int byteBufSize() {
        return 16;
    }

    @Override
    public void toByteBuf(ByteBuf buffer) {
        buffer.writeBytes(address.getAddress());
    }

    @Override
    public void fromByteBuf(ByteBuf buffer) throws DataNotRepresentsObject {
        if(buffer.readableBytes() < 16) throw new DataNotRepresentsObject("DNS RR type AAAA doesn't contain data", buffer);
        byte addbin[] = new byte[byteBufSize()];
        buffer.readBytes(addbin);
        try {
            address = (Inet6Address) Inet6Address.getByAddress(addbin);
        } catch(UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
