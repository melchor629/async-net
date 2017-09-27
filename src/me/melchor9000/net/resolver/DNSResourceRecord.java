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
import me.melchor9000.net.Serializable;
import org.jetbrains.annotations.NotNull;

import static me.melchor9000.net.resolver.DNSUtils.*;

/**
 * An answer, authority or additional record
 */
public class DNSResourceRecord extends Serializable {
    private String name;
    private int type;
    private int nclass;
    private long ttl;
    private DNSResourceData data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTypeAsString() {
        return DNSUtils.typeToString(type);
    }

    public void setType(String type) {
        this.type = DNSUtils.typeToInt(type);
    }

    public int getClass_() {
        return nclass;
    }

    public void setClass(int mclass) {
        this.nclass = mclass;
    }

    public String getClassAsString() {
        return DNSUtils.classToString(type);
    }

    public void setClass(String type) {
        this.type = DNSUtils.classToInt(type);
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public DNSResourceData getData() {
        return data;
    }

    public void setData(DNSResourceData data) {
        if(data == null) throw new NullPointerException("Data cannot be null");
        this.data = data;
    }

    @Override
    public int byteBufSize() {
        return name.length() + 12 + (data != null ? data.byteBufSize() : 0);
    }

    @Override
    public void toByteBuf(@NotNull ByteBuf buffer) {
        writeName(buffer, name);
        buffer.writeShort(type);
        buffer.writeShort(nclass);
        buffer.writeInt((int) ttl);
        buffer.writeShort(data != null ? data.byteBufSize() : 0);
        if(data != null) data.toByteBuf(buffer);
    }

    @Override
    public void fromByteBuf(@NotNull ByteBuf buffer) throws DataNotRepresentsObject {
        name = readName(buffer);
        type = rs(buffer);
        nclass = rs(buffer);
        if(buffer.readableBytes() >= 4) ttl = buffer.readUnsignedInt(); else throw new DataNotRepresentsObject("Incomplete", buffer);
        data = DNSResourceData.forData(type, buffer);
    }

    @Override
    public String toString() {
        return "[" + classToString(nclass) + "] " + typeToString(type) + " " + name + " - " + ttl + " - " + data;
    }

    private int rs(ByteBuf buf) {
        if(buf.readableBytes() < 2) throw new DataNotRepresentsObject("Is an incomplete DNS Resource Record", buf);
        return buf.readUnsignedShort();
    }
}
