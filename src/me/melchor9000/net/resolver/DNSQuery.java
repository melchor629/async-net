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

import static me.melchor9000.net.resolver.DNSUtils.*;

/**
 * Created by melchor9000 on 2/11/16.
 */
public class DNSQuery extends Serializable {
    private String name;
    private int type;
    private int mclass;

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
        return mclass;
    }

    public void setClass(int mclass) {
        this.mclass = mclass;
    }

    public String getClassAsString() {
        return DNSUtils.classToString(type);
    }

    public void setClass(String type) {
        this.mclass = DNSUtils.classToInt(type);
    }

    @Override
    public int byteBufSize() {
        return name.length() + 6;
    }

    @Override
    public void toByteBuf(ByteBuf buffer) {
        writeName(buffer, name);
        buffer.writeShort(type);
        buffer.writeShort(mclass);
    }

    @Override
    public void fromByteBuf(ByteBuf buffer) throws DataNotRepresentsObject {
        name = readName(buffer);
        type = buffer.readUnsignedShort();
        mclass = buffer.readUnsignedShort();
    }

    @Override
    public String toString() {
        return "[" + classToString(mclass) + "] " + typeToString(type) + " " + name;
    }
}
