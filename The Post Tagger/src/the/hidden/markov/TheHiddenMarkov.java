/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package the.hidden.markov;

import the.hidden.markov.opera.MarkovCore;

/**
 *
 * @author herley
 */
public class TheHiddenMarkov {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final MarkovCore classer = new MarkovCore(MarkovCore.USE_LEMMATIZER);
        /**
         * Lemma.
         */
          classer.viterbi("miss alice book has borrow two book from the library", 0);
//          classer.viterbi("barbara show me her new watch", 0);
    }

}