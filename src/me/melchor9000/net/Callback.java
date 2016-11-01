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

package me.melchor9000.net;

/**
 * Abstracts a piece of code that will receive an argument
 * of type {@code Type}.
 */
public interface Callback<Type> {

    /**
     * Executes the block of code with the argument {@code arg}.
     * @param arg argument to pass
     * @throws Exception if occurrs anything bad
     */
    void call(Type arg) throws Exception;
}
