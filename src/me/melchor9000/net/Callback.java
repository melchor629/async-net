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
