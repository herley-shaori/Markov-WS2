/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package the.hidden.markov.opera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrBuilder;
import org.apache.commons.text.StrTokenizer;

/**
 *
 * @author herley
 */
public class Kalimat {

    /**
     * Konstanta untuk Raw Data.
     */
    public static final int RAW_DATA = 0;
    /**
     * Konstanta untuk Probabilitas TAG.
     */
    public static final int TAG_ONLY = 1;

    private final String kalimat;

    public Kalimat(String kalimat) {
        this.kalimat = kalimat;
    }

    /**
     * Menghitung Probabilitas Bigram.
     * @param mode
     * @return 
     */
    public String getBigramProbability(int mode) {
        final StrBuilder strBuilder = new StrBuilder(2);
        final List<List<String>> bigram = this.getBigram();
        Iterator<List<String>> iterSatu = bigram.iterator();
        while (iterSatu.hasNext()) {
            List<String> bigramList = iterSatu.next();
            strBuilder.append("P(");
            strBuilder.append(bigramList.get(0));
            strBuilder.append("|");
            strBuilder.append(bigramList.get(1));
            strBuilder.append(")");
            strBuilder.append(" = ");
            
            double pembilang = 0;
            double penyebut = 0;
            /**
             * Pembilang. Iterasi ke level Bigram.
             */
            Iterator<Kalimat> iterDua = null;
            if (mode == Kalimat.RAW_DATA) {
                iterDua = MarkovCore.LIST_KALIMAT.iterator();
            } else if (mode == Kalimat.TAG_ONLY) {
                iterDua = MarkovCore.TAG_ONLY.iterator();
            }
//            System.out.println(bigramList);
//            System.out.println("#####################");
            while (iterDua.hasNext()) {
                Iterator<List<String>> iterTiga = iterDua.next().getBigram().iterator();
                while (iterTiga.hasNext()) {
                    final List<String> bigramDua = iterTiga.next();
                    if (StringUtils.equalsIgnoreCase(bigramList.get(0), bigramDua.get(0)) && StringUtils.equalsIgnoreCase(bigramList.get(1), bigramDua.get(1))) {
//                        System.out.println(bigramDua);
                        pembilang++;
                    }
                }
            }
//            System.out.println("------------------");

            /**
             * Penyebut. Iterasi sampai level Kalimat.
             */
            iterDua = null;
            if (mode == Kalimat.RAW_DATA) {
                iterDua = MarkovCore.LIST_KALIMAT.iterator();
            } else if (mode == Kalimat.TAG_ONLY) {
                iterDua = MarkovCore.TAG_ONLY.iterator();
            }

            while (iterDua.hasNext()) {
                String kalimatDua = iterDua.next().getRawKalimat();
                final StrTokenizer strTokenizer = new StrTokenizer(kalimatDua);
                while (strTokenizer.hasNext()) {
                    String token = strTokenizer.next();
                    if(StringUtils.equalsIgnoreCase(token, bigramList.get(1))){
                        penyebut++;
                    }
                }
            }
            
            /**
             * WARNING --> Laplace Smoothing Activated.
             */
            pembilang += 1;
            penyebut += 5;
            
            strBuilder.append(pembilang);
            strBuilder.append("/");
            strBuilder.append(penyebut);
            strBuilder.append(" = ");
            strBuilder.append(pembilang / penyebut);
            if (mode == Kalimat.TAG_ONLY) {
                final StrBuilder strBuilderDua = new StrBuilder(2);
                strBuilderDua.append("P(");
                strBuilderDua.append(bigramList.get(0));
                strBuilderDua.append("|");
                strBuilderDua.append(bigramList.get(1));
                strBuilderDua.append(")");
                strBuilderDua.append(" = ");
                strBuilderDua.append(pembilang);
                strBuilderDua.append("/");
                strBuilderDua.append(penyebut);
                strBuilderDua.append(" = ");
                strBuilderDua.append(pembilang / penyebut);
                MarkovCore.UNIQUE_TAG.add(strBuilderDua.toString());
            }
            strBuilder.appendNewLine();
        }
        return strBuilder.toString();
    }

    /**
     * Mendapatkan representasi Bigram. Reversed.
     *
     * @return
     */
    public List<List<String>> getBigram() {
        final List<List<String>> stringSet = new ArrayList();
        final NGramIterator nGramIterator = new NGramIterator(2, this.kalimat);
        while (nGramIterator.hasNext()) {
            String kata = nGramIterator.next();
            final List<String> tokenList = new StrTokenizer(kata).getTokenList();
            Collections.reverse(tokenList);
            stringSet.add(tokenList);
        }
        return stringSet;
    }

    /**
     * Kalimat mentah.
     *
     * @return
     */
    public String getRawKalimat() {
        return kalimat;
    }
}