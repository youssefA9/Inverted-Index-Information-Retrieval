import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

class DictEntry {

    public int doc_freq = 0; // number of documents that contain the term
    public int term_freq = 0; //number of times the term is mentioned in the collection
    public HashSet<Integer> postingList;

    DictEntry() {
        postingList = new HashSet<Integer>();
    }
}

class Index {


    Map<Integer, String> sources;  // store the doc_id and the file name
    HashMap<String, DictEntry> index; // THe inverted index

    Index() {
        sources = new HashMap<Integer, String>();
        index = new HashMap<String, DictEntry>();
    }

    public void printPostingList(HashSet<Integer> hset) {
        Iterator<Integer> it2 = hset.iterator();
        while (it2.hasNext()) {
            System.out.print(it2.next() + ", ");
        }
        System.out.println("");
    }

    public void printDictionary() {
        Iterator it = index.entrySet().iterator();
        System.out.println("------------------------------------------------------");
        System.out.println("*****    Number of terms = " + index.size());
        System.out.println("------------------------------------------------------");
    }


    public void buildIndex(String[] files) {
        int i = 0;
        for (String fileName : files) {
            try (BufferedReader file = new BufferedReader(new FileReader(fileName))) {
                sources.put(i, fileName);
                String ln;
                while ((ln = file.readLine()) != null) {
                    String[] words = ln.split("\\W+");
                    for (String word : words) {
                        word = word.toLowerCase();
                        // check to see if the word is not in the dictionary
                        if (!index.containsKey(word)) {
                            index.put(word, new DictEntry());
                        }
                        // add document id to the posting list
                        if (!index.get(word).postingList.contains(i)) {
                            index.get(word).doc_freq += 1; //set doc freq to the number of doc that contain the term
                            index.get(word).postingList.add(i); // add the posting to the posting:ist
                        }
                        //set the term_fteq in the collection
                        index.get(word).term_freq += 1;
                    }
                }

            } catch (IOException e) {
                System.out.println("File " + fileName + " not found. Skip it");
            }
            i++;
        }
        printDictionary();
    }

    //--------------------------------------------------------------------------
    // query inverted index
    // takes a string of terms as an argument
    public String find(String phrase) {
        String result = "";
        String[] words = phrase.split("\\W+");
        HashSet<Integer> res = new HashSet<Integer>(index.get(words[0].toLowerCase()).postingList);
        for (String word : words) {
            res.retainAll(index.get(word).postingList);
        }
        if (res.size() == 0) {
            System.out.println("Not found");
            return "";
        }
        // String result = "Found in: \n";
        for (int num : res) {
            result += "\t" + sources.get(num) + "\n";
        }
        return result;
    }

    HashSet<Integer> intersect(HashSet<Integer> pL1, HashSet<Integer> pL2) {
        HashSet<Integer> answer = new HashSet<Integer>();

        Iterator<Integer> itP1 = pL1.iterator();
        Iterator<Integer> itP2 = pL2.iterator();
        int docId1 = 0, docId2 = 0;

        if (itP1.hasNext())
            docId1 = itP1.next();
        if (itP2.hasNext())
            docId2 = itP2.next();

        while (itP1.hasNext() && itP2.hasNext()) {


            if (docId1 == docId2) {

                answer.add(docId1);
                docId1 = itP1.next();
                docId2 = itP2.next();
            } else if (docId1 < docId2) {
                if (itP1.hasNext())
                    docId1 = itP1.next();
                else return answer;

            } else {
                if (itP2.hasNext())
                    docId2 = itP2.next();
                else return answer;
            }
        }

        if (docId1 == docId2) {
            answer.add(docId1);
        }

        return answer;
    }

    HashSet<Integer> disjunction(HashSet<Integer> p1, HashSet<Integer> p2) {
        HashSet<Integer> answer = new HashSet<Integer>();

        answer.addAll(p1);
        answer.addAll(p2);

        return answer;
    }

    HashSet<Integer> negation(HashSet<Integer> p1) {
        HashSet<Integer> answer = new HashSet<Integer>();


        for (int i = 0; i < sources.size(); i++) {
            answer.add(i);
        }

        answer.removeAll(p1);
        return answer;
    }


    //-----------------------------------------------------------------------
    String[] rearrange(String[] words, int[] freq, int len) {
        boolean sorted = false;
        int temp;
        String sTmp;
        for (int i = 0; i < len - 1; i++) {
            freq[i] = index.get(words[i].toLowerCase()).doc_freq;
        }
        //-------------------------------------------------------
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < len - 1; i++) {
                if (freq[i] > freq[i + 1]) {
                    temp = freq[i];
                    sTmp = words[i];
                    freq[i] = freq[i + 1];
                    words[i] = words[i + 1];
                    freq[i + 1] = temp;
                    words[i + 1] = sTmp;
                    sorted = false;
                }
            }
        }
        return words;
    }


    public String find_04(String phrase) {
        String result = "";
        String[] words = phrase.split("\\W+");
        int len = words.length;

        //words = rearrange(words, new int[len], len);

        HashSet<Integer> res = new HashSet<Integer>(index.get(words[0].toLowerCase()).postingList);

        int i = 1;
        if (words[0].toUpperCase().equals("NOT")) {
            res = negation(new HashSet<Integer>(index.get(words[1].toLowerCase()).postingList));
            i = 2;
        }

        HashSet<Integer> temp = new HashSet<Integer>();
        Boolean negationFlag = false;

        while (i < len) {

            if (i + 1 < len) {
                if (words[i + 1].toUpperCase().equals("NOT")) {
                    temp = negation(index.get(words[i + 2].toLowerCase()).postingList);
                    negationFlag = true;
                } else {
                    temp = index.get(words[i + 1].toLowerCase()).postingList;
                }
            }

            if (words[i].toUpperCase().equals("AND")) {
                res = intersect(res, temp);
                if (negationFlag) {
                    i++;
                    negationFlag = false;
                }

            } else if (words[i].toUpperCase().equals("OR")) {
                res = disjunction(res, temp);
                if (negationFlag) {
                    i++;
                    negationFlag = false;
                }
            }

            i++;
        }
        for (int num : res) {
            result += "\t" + sources.get(num) + "\n";
        }
        return result;
    }
}


public class InvertedIndex {
    public static void main(String[] args) {
        Index index = new Index();
        index.buildIndex(new String[]{
                "D:\\College\\Information Retrieval\\Assignments\\A1\\docs\\100.txt",   //Change the Paths
                "D:\\College\\Information Retrieval\\Assignments\\A1\\docs\\105.txt",
                "D:\\College\\Information Retrieval\\Assignments\\A1\\docs\\109.txt",
                "D:\\College\\Information Retrieval\\Assignments\\A1\\docs\\509.txt",
                "D:\\College\\Information Retrieval\\Assignments\\A1\\docs\\519.txt"
        });


        String query_1 = "communication or nasr and not cairo";
        System.out.println("Query: " + query_1);
        System.out.println("result: \n" + index.find_04(query_1) + "\n");


        String query_2 = "suggest and method";
        System.out.println("Query: " + query_2);
        System.out.println("result: \n" + index.find_04(query_2) + "\n");


        String query_3 = "individually or Web or communication";
        System.out.println("Query: " + query_3);
        System.out.println("result: \n" + index.find_04(query_3) + "\n");


        String query_4 = "not Introduction";
        System.out.println("Query: " + query_4);
        System.out.println("result: \n" + index.find_04(query_4) + "\n");


    }
}
