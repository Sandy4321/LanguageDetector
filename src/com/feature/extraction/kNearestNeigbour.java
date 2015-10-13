/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.feature.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.javaml.classification.KNearestNeighbors;

/**
 *
 * @author suruearnest
 */
public class kNearestNeigbour extends DataSet {

    private int k;
    //private DataSet dataObj = new DataSet();
    private List<List<Double>> trainingDocVectorList = new ArrayList<>();
    private HashMap<String, List<Double>> docToDocVectorMap = new HashMap<>();

    public kNearestNeigbour(int k) {

        this.k = k;

    }

    //this method takes the list of all the training docs as parameter and generates a HashMap that maps every document in the list to it's respective document vector
    private HashMap<String, List<Double>> getDocToDocVectorMap() {

        return this.docToDocVectorMap;
    }

    private void generateTrainingDocVectorList(List<List<String>> allDocsList) {
        //get a list of the feature vectors of all the training documents

        // List<List<String>> allDocsList = dataObj.getAllDocsList();
        for (int i = 0; i < allDocsList.size(); i++) {
            //loop larg list
            List<String> docsList = allDocsList.get(i);//getting the lists in the large list NB:each list contains strings of values representing the docs
            //System.out.println("current training doc = " + docsList);
            for (int j = 0; j < docsList.size(); j++) {//loop thru the current list in the large list
                String docText = docsList.get(j);//gets the current doc in the current list
                //now calculate the document vector of this particular doc...
                Document doc = new Document(docText);
                trainingDocVectorList.add(doc.getDocumentVector());
                docToDocVectorMap.put(docText, doc.getDocumentVector());
            }

        }
        //return trainingDocVectorList;
    }

    public List<List<Double>> getTrainingDocVectorList() {
        return this.trainingDocVectorList;
    }

    public void trainKNN() {
        //must be called before ever attempting to predict
        loadTrainingData();
        buildVocabulary(this.getTrainingCorpusMap());
        generateTrainingDocVectorList(this.getAllDocsList());//generating the document vector for each training sample
        System.out.println("FINISHED TRAINING KNN!");

    }

    private static String transformDocumentVectorToDocumentText(HashMap<String, List<Double>> docToDocVectorMap, List<Double> documentVector) {
        //get dccumentText given the documentVector using the docToDocVectorMap

        String docText = " ";

        for (Map.Entry entry : docToDocVectorMap.entrySet()) {
            if (documentVector.equals(entry.getValue())) {
                docText = entry.getKey().toString();
                break; //break out because it is assumed to be a one to one map,no other value is assigned to such ket
            }
        }

        return docText;
    }

    //must have been preprocessed too
    private String getCategoryInTrainingCorpusFromDocText(String documentText) {

        String languageCategory = "";
        HashMap<String, ArrayList<String>> trainingCorpus = getTrainingCorpusMap();
        //Note that the training corpus is a Map of Language category to ArrayLists of documents.
        Collection<ArrayList<String>> valuesCollection = trainingCorpus.values();
        Iterator it = valuesCollection.iterator();

        while (it.hasNext()) {
            //looping through the arrayList of documents
            Object x = it.next();
            ArrayList<String> docTextList = (ArrayList<String>) x;//current arrayList
            //System.out.println("Expected = " + docText);
            for (int i = 0; i < docTextList.size(); i++) {
                String docText = docTextList.get(i);
                if (docText.equalsIgnoreCase(documentText)) {
                    //then use the current ArrayList to get the languageCatgory in the training corpus
                    for (Map.Entry entry : trainingCorpus.entrySet()) {
                        if (docTextList.equals(entry.getValue())) {
                            languageCategory = entry.getKey().toString();
                            break; //break out because it is assumed to be a one to one map,no other value is assigned to such ket
                        }
                    }

                    break;
                }
            }

        }

        return languageCategory;
    }

    private int attributeFunctionY(List<Double> documentVector, String languageCategory) {
        //this attribute function is used in the calculation of the probability that a textDoc belongs to a particular language category  
        String docText = transformDocumentVectorToDocumentText(getDocToDocVectorMap(), documentVector);//gets the string format of the document vector
        String languageCategoryInrainingDocs = getCategoryInTrainingCorpusFromDocText(docText);

        if (languageCategoryInrainingDocs.equalsIgnoreCase(languageCategory)) {

            return 1;
        } else {
            return 0;

        }

    }

    public String predict(Instance inst) {

        String testDataInString = inst.loadData();
        Document testDoc = new Document(testDataInString);

        //doc vector-->similarity value
        HashMap<List<Double>, Double> similarityMap = new HashMap<>();
        //List<Double>similarityList = new ArrayList<>();

        //this is the the test document vector
        List<Double> testDocVector = testDoc.getDocumentVector();

        List<List<Double>> trainDocVectorList = getTrainingDocVectorList();
        // above line gets all the training docs and calculates their respective document vector and stores them in a list

        double similarityArray[] = new double[trainDocVectorList.size()];
        List<Double> largest_k_similarities = new ArrayList<>();
        //Now generating a HashMap that contains each docVectors and their similarities with the testDocVector
        for (int i = 0; i < trainDocVectorList.size(); i++) {

            Document doc = new Document();//no need to pass any parameter since we are not creating a document vector

            List<Double> currentDocVector = getTrainingDocVectorList().get(i);

            double similarity = doc.vectorSimilarity(testDocVector, currentDocVector);

            similarityArray[i] = similarity;//storing similarity in an array...

            similarityMap.put(currentDocVector, similarity);//storing docVectors and their similarity values with the testDocVector

        }

        Arrays.sort(similarityArray);//sorted Array in ascending order
        largest_k_similarities = kLargestSimilarities(this.k, similarityArray);//gets a list of k-largest similarities

        //use this list to get all the knn collection of the test document vectors from the similarityMap
        List<List<Double>> similarVectors = new ArrayList<>();//knn collection of similar vectors
        //now use the largest_k_similarties and the similarity map list to get the relevant knn collection document vectors from the similarity HashMap
        //        System.err.println("original similarity map = "+similarityMap);
        //        System.out.println("**************************************************");
        similarVectors = kNNCollectionOfSimilarVectors(largest_k_similarities, similarityMap);
        HashMap<String, Double> probabilityMap = calculateProbabilities(this.getLanguageClasses(), largest_k_similarities, similarVectors);
        //System.out.println("print prob map = "+probabilityMap);
        double highestProbability = highestValue(probabilityMap.values());
        String language = getPredictedLanguage(probabilityMap, highestProbability);
        return language;
    }

    private HashMap<String, Double> calculateProbabilities(ArrayList<String> langCategory, List<Double> largest_k_similarities, List<List<Double>> similarVectors) {
        //probability map which is a mapping languageCategory to it's probbilistic value
        HashMap<String, Double> probMap = new HashMap<>();
        // ArrayList<String> langCategory = this.getLanguageClasses();
        //for each language class...
        for (int i = 0; i < langCategory.size(); i++) {
            double probSum = 0.0;
            //for each  similarity value largest_k_similarities list
            for (int j = 0; j < largest_k_similarities.size(); j++) {

                probSum = probSum + largest_k_similarities.get(j) * attributeFunctionY(similarVectors.get(j), langCategory.get(i));

            }
            probMap.put(langCategory.get(i), probSum);
        }
        /////finish calculating the probability map...

        return probMap;
    }

    private List<List<Double>> kNNCollectionOfSimilarVectors(List<Double> largest_k_similarities, HashMap<List<Double>, Double> similarityMap) {

        List<List<Double>> similarVectors = new ArrayList<>();
        for (int i = 0; i < largest_k_similarities.size(); i++) {
            for (Map.Entry entry : similarityMap.entrySet()) {

                if (largest_k_similarities.get(i).equals(entry.getValue())) {

                    similarVectors.add((List<Double>) entry.getKey());
                    break; //break out because it is assumed to be a one to one map,no other value is assigned to such ket
                }
            }

        }

        return similarVectors;
    }

    private List<Double> kLargestSimilarities(int k, double[] similarityArray) {

        List<Double> largest_k_similarities = new ArrayList<>();
        int count = 0;
        for (int i = similarityArray.length - 1; i > 0; i--) {

            //System.out.println("Sorted Array = " + similarityArray[i]);//looping through array in descending order
            //selecting k largest values
            if (count < this.k) {

                largest_k_similarities.add(similarityArray[i]);
                count++;
                // System.out.println("In the if condition ,counter = " + count);
            } else {
                break;
            }

        }

        return largest_k_similarities;
    }

    public static void main(String args[]) {

        kNearestNeigbour knn = new kNearestNeigbour(3);
        knn.trainKNN();
        Instance inst = new Instance("There is a way to go about things like that");
        String lang = knn.predict(inst);
        System.out.println(lang);
        //knn.getCategoryInTrainingCorpusFromDocText("Delivery of gold arrived in a gold truck");
    }

}
