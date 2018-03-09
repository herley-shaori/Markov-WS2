/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package the.hidden.markov.opera;

import java.util.List;
import org.apache.commons.text.StrTokenizer;

/**
 *
 * @author herley
 */
public class Kata {
    /**
     * Terdiri dari kata/TAG.
     */
    private final String token,kata,tag;
    
    public Kata(String token) {
        this.token = token;
        List<String> tokenList = new StrTokenizer(this.getToken(),"/").getTokenList();
        this.kata = tokenList.get(0);
        this.tag = tokenList.get(1);
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the kata
     */
    public String getKata() {
        return kata;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return this.token;
    }

}