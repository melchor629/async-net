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
import org.jetbrains.annotations.NotNull;

import static me.melchor9000.net.resolver.DNSUtils.readName;
import static me.melchor9000.net.resolver.DNSUtils.writeName;

/**
 * {@link DNSResourceData} for CNAME type
 */
public class DNSCNAME extends DNSResourceData {
    private String cname;

    public @NotNull String getCname() {
        return cname;
    }

    public void setCname(@NotNull String cname) {
        this.cname = cname;
    }

    DNSCNAME(ByteBuf data) {
        super(data);
    }

    @Override
    public int byteBufSize() {
        return cname.length() + 1;
    }

    @Override
    public void toByteBuf(@NotNull ByteBuf buffer) {
        writeName(buffer, cname);
    }

    @Override
    public void fromByteBuf(@NotNull ByteBuf buffer) throws DataNotRepresentsObject {
        cname = readName(buffer);
    }

    @Override
    public String toString() {
        return cname;
    }
}
