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
 * {@link DNSResourceData} for MX type
 */
public class DNSMX extends DNSResourceData {
    private int preference;
    private String exchange;

    public int getPreference() {
        return preference;
    }

    public void setPreference(int preference) {
        this.preference = preference;
    }

    public @NotNull String getExchange() {
        return exchange;
    }

    public void setExchange(@NotNull String exchange) {
        this.exchange = exchange;
    }

    DNSMX(ByteBuf data) {
        super(data);
    }

    @Override
    public int byteBufSize() {
        return 4 + exchange.length();
    }

    @Override
    public void toByteBuf(@NotNull ByteBuf buffer) {
        buffer.writeShort(preference);
        writeName(buffer, exchange);
    }

    @Override
    public void fromByteBuf(@NotNull ByteBuf buffer) throws DataNotRepresentsObject {
        if(buffer.readableBytes() < 2) throw new DataNotRepresentsObject("DNS RR type MX doesn't contain data", buffer);
        preference = buffer.readUnsignedShort();
        exchange = readName(buffer);
    }

    @Override
    public String toString() {
        return "(" + preference + ") " + exchange;
    }
}
