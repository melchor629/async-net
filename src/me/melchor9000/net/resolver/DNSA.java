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

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * {@link DNSResourceData} for A type
 */
public class DNSA extends DNSResourceData {
    private Inet4Address address;

    public Inet4Address getAddress() {
        return address;
    }

    public void setAddress(Inet4Address address) {
        this.address = address;
    }

    public void setAddress(int a1, int a2, int a3, int a4) {
        setAddress(new byte[] { (byte) a1, (byte) a2, (byte) a3, (byte) a4 });
    }

    public void setAddress(byte[] address) {
        try {
            this.address = (Inet4Address) Inet4Address.getByAddress(address);
        } catch(UnknownHostException ignore) {}
    }

    DNSA(ByteBuf data) {
        super(data);
    }

    @Override
    public int byteBufSize() {
        return 4;
    }

    @Override
    public void toByteBuf(ByteBuf buffer) {
        buffer.writeBytes(address.getAddress());
    }

    @Override
    public void fromByteBuf(ByteBuf buffer) throws DataNotRepresentsObject {
        byte ip[] = new byte[4];
        buffer.readBytes(ip);
        try {
            address = (Inet4Address) Inet4Address.getByAddress(ip);
            //We always provide a 4-byte length byte array, won't throw
        } catch(UnknownHostException ignore) {}
    }

    @Override
    public String toString() {
        return address.toString();
    }
}
