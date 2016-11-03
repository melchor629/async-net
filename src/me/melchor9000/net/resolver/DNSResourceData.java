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
import me.melchor9000.net.Serializable;

/**
 * Object for {@link DNSResourceRecord} data
 */
abstract class DNSResourceData extends Serializable {
    static DNSResourceData forData(int type, ByteBuf data) {
        data.readUnsignedShort(); //length
        switch(type) {
            case 1: return new DNSA(data);
            case 5: return new DNSCNAME(data);
            case 15: return new DNSMX(data);
            case 28: return new DNSAAAA(data);
            default: return null;
        }
    }

    DNSResourceData(ByteBuf data) {
        fromByteBuf(data);
    }
}
