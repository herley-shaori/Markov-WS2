/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package the.hidden.markov.opera;

import java.util.Iterator;

public class NGramIterator implements Iterator<String> {

    String[] words;
    int pos = 0, n;

    public NGramIterator(int n, String str) {
        this.n = n;
        words = str.split(" ");
    }

    @Override
    public boolean hasNext() {
        return pos < words.length - n + 1;
    }

    @Override
    public String next() {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < pos + n; i++)
            sb.append((i > pos ? " " : "") + words[i]);
        pos++;
        return sb.toString();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}