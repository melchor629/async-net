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

/**
 * Utils for the resolver implementation
 */
class DNSUtils {
    static void writeName(ByteBuf b, String domain) {
        String labels[] = domain.split("\\.");
        for(String label : labels) {
            b.writeByte((byte) label.length()).writeBytes(label.getBytes());
        }
        b.writeByte((byte) 0);
    }

    static String readName(ByteBuf b) {
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
                if(b.readableBytes() < length) throw new DataNotRepresentsObject("Incomplete name", b);
                byte label[] = new byte[length];
                b.readBytes(label);
                domain += "." + new String(label);
            } else {
                domain += "." + readName(b, b.readByte(), b.readerIndex());
            }
        }

        if(finalPos != -1) b.readerIndex(finalPos);
        return domain.length() > 0 ? domain.substring(1) : domain;
    }

    static String typeToString(int type) {
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
            case 28:return "AAAA";
            default:return null;
        }
    }

    static int typeToInt(String type) {
        switch(type) {
            case "A": return 1;
            case "NS": return 2;
            case "MD": return 3;
            case "MF": return 4;
            case "CNAME": return 5;
            case "SOA": return 6;
            case "MB": return 7;
            case "MG": return 8;
            case "MR": return 9;
            case "NULL": return 10;
            case "WKS": return 11;
            case "PTR": return 12;
            case "HINFO": return 13;
            case "MINFO": return 14;
            case "MX": return 15;
            case "TXT": return 16;
            case "AAAA": return 28;
            default: throw new IllegalArgumentException("Invalid type " + type);
        }
    }

    static String classToString(int clasS) {
        switch(clasS) {
            case 1: return "IN";
            case 2: return "CS";
            case 3: return "CH";
            case 4: return "HS";
            default:return null;
        }
    }

    static int classToInt(String clasS) {
        switch(clasS) {
            case "IN": return 1;
            case "CS": return 2;
            case "CH": return 3;
            case "HS": return 4;
            default: throw new IllegalArgumentException("Only accepts IN, CS, CH and HS");
        }
    }

    static String errorToString(int error) {
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
