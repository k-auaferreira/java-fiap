package br.com.fiap.projectmgt.exemplo_sala;

import java.util.function.Function;

public class MainSoma {

    public static void main(String[] args) {

        Function<Integer, Integer> f = a -> a + 1;

        System.out.println(f.apply(1));
    }
}
