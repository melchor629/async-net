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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static me.melchor9000.net.resolver.DNSUtils.errorToString;

/**
 * A message for DNS protocol.
 * @see <a href="https://tools.ietf.org/html/rfc1035">RFC 1035 - Domain Implementation and Specification</a>
 */
public class DNSMessage extends Serializable {
    private int id = (short) (ThreadLocalRandom.current().nextInt() & 0xFFFF);
    private boolean queryOrResponse = false; //false to query, response to true
    private byte opcode;
    private boolean authoritativeResponse;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private byte responseCode;
    private List<DNSQuery> questionRecords = new ArrayList<>();
    private List<DNSResourceRecord> answerRecords = new ArrayList<>();
    private List<DNSResourceRecord> authorityRecords = new ArrayList<>();
    private List<DNSResourceRecord> additionalRecords = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isQueryOrResponse() {
        return queryOrResponse;
    }

    public void setQueryOrResponse(boolean queryOrResponse) {
        this.queryOrResponse = queryOrResponse;
    }

    public byte getOpcode() {
        return opcode;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    public String getOpcodeAsString() {
        return errorToString(opcode);
    }

    public boolean isAuthoritativeResponse() {
        return authoritativeResponse;
    }

    public void setAuthoritativeResponse(boolean authoritativeResponse) {
        this.authoritativeResponse = authoritativeResponse;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public boolean isRecursionDesired() {
        return recursionDesired;
    }

    public void setRecursionDesired(boolean recursionDesired) {
        this.recursionDesired = recursionDesired;
    }

    public boolean isRecursionAvailable() {
        return recursionAvailable;
    }

    public void setRecursionAvailable(boolean recursionAvailable) {
        this.recursionAvailable = recursionAvailable;
    }

    public byte getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(byte responseCode) {
        this.responseCode = responseCode;
    }

    public void addQuery(DNSQuery query) {
        questionRecords.add(query);
    }

    public void addAnswer(DNSResourceRecord answer) {
        answerRecords.add(answer);
    }

    public void addAuthority(DNSResourceRecord authority) {
        authorityRecords.add(authority);
    }

    public void addAdditional(DNSResourceRecord additional) {
        additionalRecords.add(additional);
    }

    public Iterable<DNSQuery> getQueries() {
        return questionRecords;
    }

    public Iterable<DNSResourceRecord> getAnswers() {
        return answerRecords;
    }

    public Iterable<DNSResourceRecord> getAuthorities() {
        return authorityRecords;
    }

    public Iterable<DNSResourceRecord> getAdditionals() {
        return additionalRecords;
    }

    public int getCountQueries() {
        return questionRecords.size();
    }

    public int getCountAnswers() {
        return answerRecords.size();
    }

    public int getCountAuthorities() {
        return authorityRecords.size();
    }

    public int getCountAdditionals() {
        return additionalRecords.size();
    }

    @Override
    public int byteBufSize() {
        int size = 12;
        for(DNSQuery query : questionRecords) size += query.byteBufSize();
        for(DNSResourceRecord record : answerRecords) size += record.byteBufSize();
        for(DNSResourceRecord record : authorityRecords) size += record.byteBufSize();
        for(DNSResourceRecord record : additionalRecords) size += record.byteBufSize();
        return size;
    }

    @Override
    public void toByteBuf(ByteBuf buffer) {
        int flags = 0;
        if(queryOrResponse) flags |= 0x8000;
        flags |= (opcode << 11) & 0x7800;
        if(authoritativeResponse) flags |= 0x0400;
        if(truncated) flags |= 0x0200;
        if(recursionDesired) flags |= 0x0100;
        if(recursionAvailable) flags |= 0x0080;
        flags |= responseCode & 0xF;

        buffer.writeShort(id);
        buffer.writeShort(flags);
        buffer.writeShort(questionRecords.size());
        buffer.writeShort(answerRecords.size());
        buffer.writeShort(authorityRecords.size());
        buffer.writeShort(additionalRecords.size());

        for(DNSQuery query : questionRecords) query.toByteBuf(buffer);
        for(DNSResourceRecord record : answerRecords) record.toByteBuf(buffer);
        for(DNSResourceRecord record : authorityRecords) record.toByteBuf(buffer);
        for(DNSResourceRecord record : additionalRecords) record.toByteBuf(buffer);
    }

    @Override
    public void fromByteBuf(ByteBuf buffer) throws DataNotRepresentsObject {
        int id = buffer.readUnsignedShort();

        if((this.id & 0xFFFF) != id) throw new DataNotRepresentsObject("Don't match Transaction ID", buffer);

        int flags = buffer.readUnsignedShort();
        queryOrResponse = (flags & 0x8000) != 0;
        opcode = (byte) ((flags & 0x7800) >> 11);
        authoritativeResponse = (flags & 0x0400) != 0;
        truncated = (flags & 0x0200) != 0;
        recursionDesired = (flags & 0x0100) != 0;
        recursionAvailable = (flags & 0x0080) != 0;
        responseCode = (byte) (flags & 0xF);

        int qdcount = buffer.readUnsignedShort();
        int ancount = buffer.readUnsignedShort();
        int nscount = buffer.readUnsignedShort();
        int arcount = buffer.readUnsignedShort();

        for(int i = 0; i < qdcount; i++) {
            DNSQuery query = new DNSQuery();
            query.fromByteBuf(buffer);
            questionRecords.add(query);
        }

        for(int i = 0; i < ancount; i++) {
            DNSResourceRecord record = new DNSResourceRecord();
            record.fromByteBuf(buffer);
            answerRecords.add(record);
        }

        for(int i = 0; i < nscount; i++) {
            DNSResourceRecord record = new DNSResourceRecord();
            record.fromByteBuf(buffer);
            authorityRecords.add(record);
        }

        for(int i = 0; i < arcount; i++) {
            DNSResourceRecord record = new DNSResourceRecord();
            record.fromByteBuf(buffer);
            additionalRecords.add(record);
        }
    }
}
