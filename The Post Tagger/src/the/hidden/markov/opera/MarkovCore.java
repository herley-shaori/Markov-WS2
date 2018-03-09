/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package the.hidden.markov.opera;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrBuilder;
import org.apache.commons.text.StrTokenizer;

/**
 *
 * @author herley
 */
public class MarkovCore {

    //<editor-fold defaultstate="collapsed" desc="Emission Probability Class">
    final class Emissions implements Comparable<Emissions> {

        private final String word, tag;
        private String probabilitySentence, oldProbabilitySentece;
        private double emissionsProbability;

        public Emissions(String word, String tag, String probabilitySentence) {
            this.word = word;
            this.tag = tag;
            this.probabilitySentence = probabilitySentence;
            this.oldProbabilitySentece = probabilitySentence;
        }

        @Override
        public String toString() {
            return this.probabilitySentence;
        }

        /**
         * @return the emissionsProbability
         */
        public double getEmissionsProbability() {
            return emissionsProbability;
        }

        /**
         * @param emissionsProbability the emissionsProbability to set
         */
        public void setEmissionsProbability(double pembilang, double penyebut, double emissionsProbability) {
            this.emissionsProbability = emissionsProbability;
            /**
             * Update String.
             */
            final StrBuilder strBuilder = new StrBuilder(this.probabilitySentence);
            strBuilder.append(pembilang + "/" + penyebut + " = " + emissionsProbability);
            this.probabilitySentence = strBuilder.toString();
        }

        @Override
        public int compareTo(Emissions o) {
            return this.oldProbabilitySentece.compareTo(o.oldProbabilitySentece);
        }

    }
//</editor-fold>

    public static final int LAPLACE_SMOOTHING = 1;
    public static final int USE_LEMMATIZER = 1;
    public static final String UNKNOWN_TAG = "UK";
    public static final List<Kalimat> LIST_KALIMAT = new ArrayList();
    public static final List<Kata> LIST_KATA = new ArrayList();
    public static final List<Kalimat> TAG_ONLY = new ArrayList();
    public static final Set<String> UNIQUE_TAG = new HashSet();
    public static final Set<String> TAG_LIST = new HashSet();

    private final Set<String> uniqueWords = new HashSet();
    private final HashMap<String, Double> totalUniqueWords = new HashMap();
    private final HashMap<String, Double> totalUniqueTag = new HashMap();

    final TreeSet<Emissions> emissionses = new TreeSet();
    final MultiKeyMap<MultiKey<String>, Double> emissionsMap = new MultiKeyMap();
    final MultiKeyMap<MultiKey<String>, Double> transitionMap = new MultiKeyMap();

    private final String[] sentenceOnly = new String[]{"i have booked the flight for my trip tomorrow morning", "barbara always watches tv every sunday morning", "this expensive book titles the adventure of alice", "my watch has been missing", "Please book two tickets for Barbara and I", "I never miss watching amazing race tv show", "Her watch is more expensive than yours", "Having a trip and watching tv are two of her hobbies"};

    private final String[] trainingData = new String[]{"<s> I/XX have/KR booked/KR the/XX flight/BD for/XX my/XX trip/BD tomorrow/BD morning/BD", "<s> barbara/XX always/XX watches/KR tv/BD every/XX sunday/BD morning/BD", "<s> this/XX expensive/SF book/BD titles/BD the/XX adventure/BD of/XX alice/XX", "<s> my/XX watch/BD has/KR been/KR missing/KR", "<s> please/KR book/KR two/XX tickets/BD for/XX barbara/XX and/XX I/XX", "<s> I/XX never/XX miss/KR watching/KR amazing/SF race/BD tv/BD show/BD", "<s> her/XX watch/BD is/KR more/XX expensive/SF than/XX yours/BD", "<s> having/KR a/XX trip/BD and/XX watching/KR tv/BD are/KR two/XX of/XX her/XX hobbies/BD"};

    private final String[] trainingDataLemma = new String[]{"<s> I/XX have/KR book/KR the/XX flight/BD for/XX my/XX trip/BD tomorrow/BD morning/BD", "<s> barbara/XX always/XX watch/KR tv/BD every/XX sunday/BD morning/BD", "<s> this/XX expensive/SF book/BD title/BD the/XX adventure/BD of/XX alice/XX", "<s> my/XX watch/BD has/KR been/KR miss/KR", "<s> please/KR book/KR two/XX ticket/BD for/XX barbara/XX and/XX I/XX", "<s> I/XX never/XX miss/KR watch/KR amazing/SF race/BD tv/BD show/BD", "<s> her/XX watch/BD is/KR more/XX expensive/SF than/XX yours/BD", "<s> having/KR a/XX trip/BD and/XX watch/KR tv/BD are/KR two/XX of/XX her/XX hobby/BD"};
    
    private final String[] tagOnly = new String[]{"<s> XX KR KR XX BD XX XX BD BD BD", "<s> XX XX KR BD XX BD BD", "<s> XX SF BD BD XX BD XX XX", "<s> XX BD KR KR KR", "<s> KR KR XX BD XX XX XX XX", "<s> XX XX KR KR SF BD BD BD", "<s> XX BD KR XX SF XX BD", "<s> KR XX BD XX KR BD KR XX XX XX BD"};

//    private final String[] tagOnly = new String[]{"<s> XX KR KR XX BD XX XX BD BD BD </s>", "<s> XX XX KR BD XX BD BD </s>", "<s> XX SF BD BD XX BD XX BD </s>", "<s> XX BD KR KR KR </s>", "<s> KR KR XX BD XX XX XX XX </s>", "<s> XX XX KR KR SF BD BD BD </s>", "<s> XX BD KR XX SF XX BD </s>", "<s> KR XX BD XX KR BD KR XX XX XX BD </s>"};
    private final String[] singleTag = new String[]{"<s> XX KR KR XX BD XX XX BD BD BD"};

    public MarkovCore(int mode) {
        if (mode == MarkovCore.USE_LEMMATIZER) {
            /**
             * Transition Map Manual Filling. Without Laplace. Choose only one
             * of this.
             */
        this.transitionMap.put(new MultiKey("XX", "KR"), 0.35714285714285715);
        this.transitionMap.put(new MultiKey("BD", "KR"), 0.14285714285714285);
        this.transitionMap.put(new MultiKey("KR", "BD"), 0.14285714285714285);
        this.transitionMap.put(new MultiKey("KR", "<s>"), 0.25);
        this.transitionMap.put(new MultiKey("KR", "XX"), 0.15384615384615385);
        this.transitionMap.put(new MultiKey("BD", "BD"), 0.2857142857142857);
        this.transitionMap.put(new MultiKey("XX", "<s>"), 0.75);
        this.transitionMap.put(new MultiKey("XX", "XX"), 0.3076923076923077);
        this.transitionMap.put(new MultiKey("SF", "XX"), 0.07692307692307693);
        this.transitionMap.put(new MultiKey("BD", "XX"), 0.4230769230769231);
        this.transitionMap.put(new MultiKey("BD", "SF"), 0.6666666666666666);
        this.transitionMap.put(new MultiKey("SF", "KR"), 0.07142857142857142);
        this.transitionMap.put(new MultiKey("XX", "SF"), 0.3333333333333333);
        this.transitionMap.put(new MultiKey("XX", "BD"), 0.2857142857142857);
        this.transitionMap.put(new MultiKey("KR", "KR"), 0.35714285714285715);
            /**
             * Transition Map Manual Filling. With Laplace.
             */
//            this.transitionMap.put(new MultiKey("XX", "KR"), 0.3157894736842105);
//            this.transitionMap.put(new MultiKey("BD", "KR"), 0.15789473684210525);
//            this.transitionMap.put(new MultiKey("KR", "BD"), 0.16);
//            this.transitionMap.put(new MultiKey("KR", "<s>"), 0.23076923076923078);
//            this.transitionMap.put(new MultiKey("KR", "XX"), 0.15625);
//            this.transitionMap.put(new MultiKey("BD", "BD"), 0.28);
//            this.transitionMap.put(new MultiKey("XX", "<s>"), 0.5384615384615384);
//            this.transitionMap.put(new MultiKey("XX", "XX"), 0.3125);
//            this.transitionMap.put(new MultiKey("SF", "XX"), 0.09375);
//            this.transitionMap.put(new MultiKey("BD", "XX"), 0.34375);
//            this.transitionMap.put(new MultiKey("BD", "SF"), 0.375);
//            this.transitionMap.put(new MultiKey("SF", "KR"), 0.10526315789473684);
//            this.transitionMap.put(new MultiKey("XX", "SF"), 0.25);
//            this.transitionMap.put(new MultiKey("XX", "BD"), 0.28);
//            this.transitionMap.put(new MultiKey("KR", "KR"), 0.3157894736842105);

            /**
             * TAG List Manual.
             */
            MarkovCore.TAG_LIST.add("BD");
            MarkovCore.TAG_LIST.add("XX");
            MarkovCore.TAG_LIST.add("KR");
            MarkovCore.TAG_LIST.add("SF");
            MarkovCore.TAG_LIST.add("<s>");

            for (int i = 0; i < this.trainingDataLemma.length; i++) {
                final String kalimat = this.trainingDataLemma[i];
                final List<String> tokenList = new StrTokenizer(kalimat).getTokenList();
                for (int j = 1; j < tokenList.size(); j++) {
                    MarkovCore.LIST_KATA.add(new Kata(StringUtils.trim(tokenList.get(j).toLowerCase())));
                }
            }

            for (int i = 0; i < this.tagOnly.length; i++) {
                String string = this.tagOnly[i];
                MarkovCore.TAG_ONLY.add(new Kalimat(StringUtils.trim(string)));
            }

            for (int i = 0; i < this.sentenceOnly.length; i++) {
                Iterator<String> iterSatu = this.lemmatizer(sentenceOnly[i]).iterator();
                String kalimatUbahan = "";
                while (iterSatu.hasNext()) {
                    kalimatUbahan+=iterSatu.next()+" ";
                }
                kalimatUbahan = StringUtils.trim(kalimatUbahan);
                this.sentenceOnly[i] = kalimatUbahan;
                Iterator<String> iterator = new StrTokenizer(this.sentenceOnly[i].toLowerCase()).getTokenList().iterator();
                while (iterator.hasNext()) {
                    this.uniqueWords.add(iterator.next());
                }
            }

            Iterator<String> iterSatu = this.uniqueWords.iterator();
            while (iterSatu.hasNext()) {
                final String uw = iterSatu.next();
                Double count = 0.0;
                for (int i = 0; i < this.sentenceOnly.length; i++) {
                    Iterator<String> iterator = new StrTokenizer(this.sentenceOnly[i].toLowerCase()).getTokenList().iterator();
                    while (iterator.hasNext()) {
                        if (StringUtils.equalsIgnoreCase(uw, iterator.next())) {
                            count++;
                        }
                    }
                }
                this.totalUniqueWords.put(uw, count);
            }

            Iterator<String> iterDua = MarkovCore.TAG_LIST.iterator();
            while (iterDua.hasNext()) {
                Double count = 0.0;
                String tag = iterDua.next();
                for (String tagnya : this.tagOnly) {
                    Iterator<String> iterTiga = new StrTokenizer(tagnya.toLowerCase()).getTokenList().iterator();
                    while (iterTiga.hasNext()) {
                        if (StringUtils.equalsIgnoreCase(tag, iterTiga.next())) {
                            count++;
                        }
                    }
                }
                this.totalUniqueTag.put(tag, count);
            }
        }
    }

    public MarkovCore() {

        /**
         * Transition Map Manual Filling. Without Laplace. Choose only one of
         * this.
         */
//        this.transitionMap.put(new MultiKey("XX", "KR"), 0.35714285714285715);
//        this.transitionMap.put(new MultiKey("BD", "KR"), 0.14285714285714285);
//        this.transitionMap.put(new MultiKey("KR", "BD"), 0.14285714285714285);
//        this.transitionMap.put(new MultiKey("KR", "<s>"), 0.25);
//        this.transitionMap.put(new MultiKey("KR", "XX"), 0.15384615384615385);
//        this.transitionMap.put(new MultiKey("BD", "BD"), 0.2857142857142857);
//        this.transitionMap.put(new MultiKey("XX", "<s>"), 0.75);
//        this.transitionMap.put(new MultiKey("XX", "XX"), 0.3076923076923077);
//        this.transitionMap.put(new MultiKey("SF", "XX"), 0.07692307692307693);
//        this.transitionMap.put(new MultiKey("BD", "XX"), 0.4230769230769231);
//        this.transitionMap.put(new MultiKey("BD", "SF"), 0.6666666666666666);
//        this.transitionMap.put(new MultiKey("SF", "KR"), 0.07142857142857142);
//        this.transitionMap.put(new MultiKey("XX", "SF"), 0.3333333333333333);
//        this.transitionMap.put(new MultiKey("XX", "BD"), 0.2857142857142857);
//        this.transitionMap.put(new MultiKey("KR", "KR"), 0.35714285714285715);
        /**
         * Transition Map Manual Filling. With Laplace.
         */
        this.transitionMap.put(new MultiKey("XX", "KR"), 0.3157894736842105);
        this.transitionMap.put(new MultiKey("BD", "KR"), 0.15789473684210525);
        this.transitionMap.put(new MultiKey("KR", "BD"), 0.16);
        this.transitionMap.put(new MultiKey("KR", "<s>"), 0.23076923076923078);
        this.transitionMap.put(new MultiKey("KR", "XX"), 0.15625);
        this.transitionMap.put(new MultiKey("BD", "BD"), 0.28);
        this.transitionMap.put(new MultiKey("XX", "<s>"), 0.5384615384615384);
        this.transitionMap.put(new MultiKey("XX", "XX"), 0.3125);
        this.transitionMap.put(new MultiKey("SF", "XX"), 0.09375);
        this.transitionMap.put(new MultiKey("BD", "XX"), 0.34375);
        this.transitionMap.put(new MultiKey("BD", "SF"), 0.375);
        this.transitionMap.put(new MultiKey("SF", "KR"), 0.10526315789473684);
        this.transitionMap.put(new MultiKey("XX", "SF"), 0.25);
        this.transitionMap.put(new MultiKey("XX", "BD"), 0.28);
        this.transitionMap.put(new MultiKey("KR", "KR"), 0.3157894736842105);

        /**
         * TAG List Manual.
         */
        MarkovCore.TAG_LIST.add("BD");
        MarkovCore.TAG_LIST.add("XX");
        MarkovCore.TAG_LIST.add("KR");
        MarkovCore.TAG_LIST.add("SF");
        MarkovCore.TAG_LIST.add("<s>");

        for (int i = 0; i < this.trainingData.length; i++) {
            final String kalimat = this.trainingData[i];
            final List<String> tokenList = new StrTokenizer(kalimat).getTokenList();
            for (int j = 1; j < tokenList.size(); j++) {
                MarkovCore.LIST_KATA.add(new Kata(StringUtils.trim(tokenList.get(j).toLowerCase())));
            }
        }

        for (int i = 0; i < this.tagOnly.length; i++) {
            String string = this.tagOnly[i];
            MarkovCore.TAG_ONLY.add(new Kalimat(StringUtils.trim(string)));
        }

        for (int i = 0; i < this.sentenceOnly.length; i++) {
            Iterator<String> iterator = new StrTokenizer(this.sentenceOnly[i].toLowerCase()).getTokenList().iterator();
            while (iterator.hasNext()) {
                this.uniqueWords.add(iterator.next());
            }
        }

        Iterator<String> iterSatu = this.uniqueWords.iterator();
        while (iterSatu.hasNext()) {
            final String uw = iterSatu.next();
            Double count = 0.0;
            for (int i = 0; i < this.sentenceOnly.length; i++) {
                Iterator<String> iterator = new StrTokenizer(this.sentenceOnly[i].toLowerCase()).getTokenList().iterator();
                while (iterator.hasNext()) {
                    if (StringUtils.equalsIgnoreCase(uw, iterator.next())) {
                        count++;
                    }
                }
            }
            this.totalUniqueWords.put(uw, count);
        }

        Iterator<String> iterDua = MarkovCore.TAG_LIST.iterator();
        while (iterDua.hasNext()) {
            Double count = 0.0;
            String tag = iterDua.next();
            for (String tagnya : this.tagOnly) {
                Iterator<String> iterTiga = new StrTokenizer(tagnya.toLowerCase()).getTokenList().iterator();
                while (iterTiga.hasNext()) {
                    if (StringUtils.equalsIgnoreCase(tag, iterTiga.next())) {
                        count++;
                    }
                }
            }
            this.totalUniqueTag.put(tag, count);
        }
    }

    public void perhitunganManual() {
        Iterator<String> iterSatu = this.totalUniqueTag.keySet().iterator();
        while (iterSatu.hasNext()) {
            String tag = iterSatu.next();
            System.out.println("Tag : " + tag + " Transmission Value: " + this.transitionMap.get(tag, "<s>"));
        }

    }

    public List<String> lemmatizer(String kata) {
        Properties prop = new Properties();
        prop.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(prop);
        Annotation document = new Annotation(kata);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        List<String> lemmas = new ArrayList<String>();
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                lemmas.add(token.get(LemmaAnnotation.class));
            }
        }
        return lemmas;
    }

    /**
     * Nomor Tiga Viterbi.
     *
     * @param kalimat
     * @param mode
     */
    public void viterbi(String kalimat, int mode) {
        if (this.emissionses.isEmpty()) {
            this.emissionProbabilities();
        }

        final List<String> tagger = new ArrayList(), posTagger = new ArrayList();

        final class ViterNode {

            private final String tag;
            private final List<ViterNode> connectionBefore = new ArrayList();
            private boolean isSelected = false;
            private Double maxValue = -1.0;

            public ViterNode(String tag) {
                this.tag = tag;
            }

            /**
             * @return the tag
             */
            public String getTag() {
                return tag;
            }

            /**
             * @return the connectionBefore
             */
            public List<ViterNode> getConnectionBefore() {
                return connectionBefore;
            }

            /**
             * @return the isSelected
             */
            public boolean isIsSelected() {
                return isSelected;
            }

            /**
             * @param isSelected the isSelected to set
             */
            public void setIsSelected(boolean isSelected) {
                this.isSelected = isSelected;
            }

            public void setMaxValue(Double maxValue) {
                if (maxValue > 0.0) {
                    if (this.maxValue == null) {
                        this.maxValue = maxValue;
                    } else {
                        if (maxValue > this.maxValue) {
                            this.maxValue = maxValue;
                        }
                    }
                }
            }

            public Double getMaxValue() {
                return this.maxValue;
            }
        }

        final class LayerNode {

            private final List<ViterNode> viterNodeList = new CopyOnWriteArrayList();

            public LayerNode() {
                viterNodeList.add(new ViterNode("BD"));
                viterNodeList.add(new ViterNode("KR"));
                viterNodeList.add(new ViterNode("SF"));
                viterNodeList.add(new ViterNode("XX"));
            }

            /**
             * @return the viterNodeList
             */
            public List<ViterNode> getViterNodeList() {
                return viterNodeList;
            }

            /**
             * Layer Blocked jika semua node pada layer ini bernilai 0.
             *
             * @return
             */
            public boolean isLayerBlocked() {
                return this.viterNodeList.isEmpty();
            }

            public ViterNode getMaxViterNode() {
                Iterator<ViterNode> iterator = this.viterNodeList.iterator();
                ViterNode hasil = null;
                Double nilai = 0.0;
                while (iterator.hasNext()) {
                    ViterNode viterNode = iterator.next();
                    if ((viterNode.getMaxValue() != null) && (viterNode.getMaxValue() > nilai)) {
                        nilai = viterNode.getMaxValue();
                        hasil = viterNode;
                    }
                }
                return hasil;
            }
        }

        /**
         * Perhitungan dari Start.
         */
        kalimat = kalimat.toLowerCase();
        final List<String> tokenList = new StrTokenizer(kalimat).getTokenList();
        /**
         * First Tag Calculation.
         */
        final ViterNode star = new ViterNode("<s>");
        LayerNode nextLayer = null;
        LayerNode currentLayer = new LayerNode();
        Iterator<ViterNode> iterSatu = currentLayer.getViterNodeList().iterator();
        while (iterSatu.hasNext()) {
            final ViterNode viterNode = iterSatu.next();
            final String viterNodeTag = viterNode.getTag();
            Double transitionValue = this.transitionMap.get(viterNodeTag, star.getTag());
            if (transitionValue == null) {
                transitionValue = 0.0;
            }

            // LAPLACE
            if (mode == MarkovCore.LAPLACE_SMOOTHING) {
                if (transitionValue == 0.0) {
                    Double pembilang = 1.0;
                    Double penyebut = this.totalUniqueTag.get("<s>") + 5;
                    transitionValue = pembilang / penyebut;
                }
            }
            final String kata = tokenList.get(0);
            Double emissionValue = this.emissionsMap.get(kata, viterNodeTag.toLowerCase());
            if (emissionValue == null) {
                emissionValue = 0.0;
            }

            if (mode == MarkovCore.LAPLACE_SMOOTHING) {
                if (emissionValue == 0.0) {
                    Double pembilang = 1.0;
                    Double penyebut = this.totalUniqueTag.get(viterNodeTag.toLowerCase()) + this.uniqueWords.size();
                    emissionValue = pembilang / penyebut;
                }
            }

            Double transEmi = transitionValue * emissionValue;
            viterNode.setMaxValue(transEmi);
        }

        if (!currentLayer.isLayerBlocked()) {
            posTagger.add(tokenList.get(0) + "/" + currentLayer.getMaxViterNode().getTag());
            tagger.add(currentLayer.getMaxViterNode().getTag());
        }

        /**
         * Current Layer Removal.
         */
        Iterator<ViterNode> iterCurrentLayer = currentLayer.getViterNodeList().iterator();
        while (iterCurrentLayer.hasNext()) {
            ViterNode next = iterCurrentLayer.next();

            if (next.getMaxValue() <= 0.0) {
                currentLayer.getViterNodeList().remove(next);
            }
        }

        /**
         * Remaining Calculation...
         */
        boolean outerLoopBreak = false;
        for (int i = 1; i < tokenList.size(); i++) {
            if (currentLayer.isLayerBlocked()) {
                System.err.println("Layer Terkunci (Upper Lock)");
                break;
            } else {
                final String kata = tokenList.get(i);
                nextLayer = new LayerNode();
                Iterator<ViterNode> iterDua = currentLayer.getViterNodeList().iterator();
                while (iterDua.hasNext()) {
                    final ViterNode viterNodeCurrentLayer = iterDua.next();
                    Iterator<ViterNode> iterTiga = nextLayer.getViterNodeList().iterator();
                    /**
                     * Hapus Transisi Node dengan nilai 0 dari nextLayer.
                     */
                    while (iterTiga.hasNext()) {
                        ViterNode viterNodeNextLayer = iterTiga.next();
                        Double transitionValue = this.transitionMap.get(viterNodeNextLayer.getTag(), viterNodeCurrentLayer.getTag());
                        if (transitionValue == null || transitionValue <= 0.0) {
                            transitionValue = 0.0;
                        }

                        if (mode == MarkovCore.LAPLACE_SMOOTHING) {
                            if (transitionValue <= 0.0) {
                                final Double pembilang = 1.0;
                                final Double penyebut = this.totalUniqueTag.get(viterNodeCurrentLayer.getTag()) + this.totalUniqueTag.size();
                                transitionValue = pembilang / penyebut;
                            }
                        }

                        if (transitionValue <= 0.0) {
                            nextLayer.getViterNodeList().remove(viterNodeNextLayer);
                        }
                    }

                    /**
                     * Hapus Emisi Node dengan nilai 0 dari nextLayer.
                     */
                    iterTiga = nextLayer.getViterNodeList().iterator();
                    while (iterTiga.hasNext()) {
                        final ViterNode viterNodeNextLayer = iterTiga.next();
                        Double emissionValue = this.emissionsMap.get(kata, viterNodeNextLayer.getTag().toLowerCase());
                        if (emissionValue == null || emissionValue <= 0.0) {
                            emissionValue = 0.0;
                        }

                        if (mode == MarkovCore.LAPLACE_SMOOTHING) {
                            if (emissionValue <= 0.0) {
                                final Double pembilang = 1.0;
                                final Double penyebut = this.totalUniqueTag.get(viterNodeNextLayer.getTag()) + this.totalUniqueTag.size();
                                emissionValue = pembilang / penyebut;
                            }
                        }

                        if (emissionValue == 0.0) {
                            nextLayer.getViterNodeList().remove(viterNodeNextLayer);
                        }
                    }

                    if (nextLayer.isLayerBlocked()) {
                        System.err.println("Terkunci (Lower Lock I), kata: " + kata);
                        outerLoopBreak = true;
                        break;
                    } else {
                        iterTiga = nextLayer.getViterNodeList().iterator();
                        while (iterTiga.hasNext()) {
                            final ViterNode viterNodeNextLayer = iterTiga.next();
                            Double transitionValue = this.transitionMap.get(viterNodeNextLayer.getTag(), viterNodeCurrentLayer.getTag());
                            Double emissionValue = this.emissionsMap.get(kata, viterNodeNextLayer.getTag().toLowerCase());

                            if (transitionValue == null) {
                                transitionValue = 0.0;
                            }

                            if (emissionValue == null) {
                                emissionValue = 0.0;
                            }

                            if (mode == MarkovCore.LAPLACE_SMOOTHING) {
                                if (transitionValue <= 0.0) {
                                    final Double pembilang = 1.0;
                                    final Double penyebut = this.totalUniqueTag.get(viterNodeCurrentLayer.getTag()) + this.totalUniqueTag.size();
                                    transitionValue = pembilang / penyebut;
                                }

                                if (emissionValue <= 0.0) {
                                    final Double pembilang = 1.0;
                                    final Double penyebut = this.totalUniqueTag.get(viterNodeNextLayer.getTag()) + this.totalUniqueTag.size();
                                    emissionValue = pembilang / penyebut;
                                }
                            }
                            final Double transEmi = transitionValue * emissionValue;
                            final Double maxValueCandidate = viterNodeCurrentLayer.getMaxValue() * transEmi;
                            viterNodeNextLayer.setMaxValue(maxValueCandidate);
                        }
                    }
                }
                /**
                 * NextLayer Evaluation.
                 */
                if (nextLayer.isLayerBlocked()) {
                    System.err.println("Terkunci (Layer Lock II), kata: " + kata);
                    break;
                } else {
                    posTagger.add(kata + "/" + nextLayer.getMaxViterNode().getTag());
                    tagger.add(nextLayer.getMaxViterNode().getTag());
                    /**
                     * Layer Switch.
                     */
                    currentLayer = nextLayer;
                    nextLayer = null;
                }

//            System.out.println(kata);
            }

            /**
             * VP.
             */
//            break;
            if (outerLoopBreak) {
                System.err.println("Outer Loop Break Reached.");
                break;
            }
        }

        System.out.println();
        System.out.println(posTagger);
        System.out.println(tagger);
//        System.out.println(this.emissionsMap);
    }

    /**
     * Soal nomor 2.
     */
    public void bigramNomorDua() {
        Iterator<Kalimat> iterSatu = MarkovCore.LIST_KALIMAT.iterator();
        while (iterSatu.hasNext()) {
            Kalimat kalimat = iterSatu.next();
            System.out.println(kalimat.getRawKalimat());
            System.out.println(kalimat.getBigramProbability(Kalimat.RAW_DATA));
        }
    }

    /**
     * Tag State nomor 2.
     */
    public void tagOnlyNomorDua() {
        Iterator<Kalimat> iterSatu = MarkovCore.TAG_ONLY.iterator();
        while (iterSatu.hasNext()) {
            Kalimat kalimat = iterSatu.next();
            System.out.println(kalimat.getRawKalimat());
            System.out.println(kalimat.getBigramProbability(Kalimat.TAG_ONLY));
        }
    }

    public void transmissionProbabilities() {
        Iterator<Kalimat> iterSatu = MarkovCore.TAG_ONLY.iterator();
        while (iterSatu.hasNext()) {
            Kalimat kalimat = iterSatu.next();
            final String bp = kalimat.getBigramProbability(Kalimat.TAG_ONLY);
        }

        System.out.println("Jumlah Tag Unik: " + MarkovCore.UNIQUE_TAG.size());
        Iterator<String> iterDua = MarkovCore.UNIQUE_TAG.iterator();
        while (iterDua.hasNext()) {
            String next = iterDua.next();
            System.out.println(next);
//            if (StringUtils.contains(next, "(BD|")) {
//                System.out.println(next);
//            }
        }
    }

    /**
     * Emisi untuk soal nomor 2.
     */
    public void emissionProbabilities() {
        /**
         * Word and Tag List.
         */
        final TreeSet<String> words = new TreeSet(), tags = new TreeSet();
        Iterator<Kata> iterSatu = MarkovCore.LIST_KATA.iterator();
        while (iterSatu.hasNext()) {
            final Kata kata = iterSatu.next();
            words.add(kata.getKata());
            tags.add(kata.getTag());
        }
        System.out.println("Jumlah Kata: " + words.size());
        System.out.println("Jumlah Tag: " + tags.size());
        System.out.println();
        //<editor-fold defaultstate="collapsed" desc="Emissions Calculation">
        /**
         * Perhitungan Emisi.
         */
        Iterator<String> iterDua = words.iterator();
        while (iterDua.hasNext()) {
            final String kata = iterDua.next();
            Iterator<String> iterTiga = tags.iterator();
            while (iterTiga.hasNext()) {
                final StrBuilder strBuilder = new StrBuilder(10);
                final String tag = iterTiga.next();
                strBuilder.append("P(");
                strBuilder.append(kata + "|");
                strBuilder.append(tag + ") = ");
                final Emissions emissions = new Emissions(kata, tag, strBuilder.toString());
                emissionses.add(emissions);
            }
        }

        final HashMap<String, Double> penyebut = new HashMap();
        Iterator<Kata> iterEmpat = MarkovCore.LIST_KATA.iterator();
        while (iterEmpat.hasNext()) {
            final Kata kata = iterEmpat.next();
            if (penyebut.get(kata.getTag()) == null) {
                penyebut.put(kata.getTag(), 1.0);
            } else {
                penyebut.put(kata.getTag(), penyebut.get(kata.getTag()) + 1);
            }
        }

        Iterator<Emissions> iterTiga = emissionses.iterator();
        while (iterTiga.hasNext()) {
            final Emissions emissions = iterTiga.next();
            double pembilang = 0.0;
            Iterator<Kata> iterKata = MarkovCore.LIST_KATA.iterator();
            while (iterKata.hasNext()) {
                Kata kata = iterKata.next();
                if (StringUtils.equalsIgnoreCase(kata.getKata(), emissions.word) && StringUtils.equalsIgnoreCase(kata.getTag(), emissions.tag)) {
                    pembilang++;
                }
            }

            /**
             * WARNING - Laplace Smoothing is Activated.
             */
//            emissions.setEmissionsProbability(pembilang + 1, penyebut.get(emissions.tag) + this.uniqueWords.size(), (pembilang + 1) / (penyebut.get(emissions.tag) + this.uniqueWords.size()));
            emissions.setEmissionsProbability(pembilang, penyebut.get(emissions.tag), pembilang / penyebut.get(emissions.tag));
        }
//</editor-fold>

//        System.out.println(emissionses.size());
        Iterator<Emissions> emissionsIterator = emissionses.iterator();
        while (emissionsIterator.hasNext()) {
            final Emissions emissions = emissionsIterator.next();
            this.emissionsMap.put(new MultiKey(emissions.word, emissions.tag), emissions.emissionsProbability);
//            System.out.println(emissions);
        }
//        System.out.println(this.emissionsMap.size());
    }
}