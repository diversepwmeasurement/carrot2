
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2019, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.examples;

import org.carrot2.clustering.Cluster;
import org.carrot2.clustering.ClusteringAlgorithm;
import org.carrot2.clustering.Document;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.language.EnglishLanguageComponentsFactory;
import org.carrot2.language.LanguageComponents;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This example shows typical basic clustering scenarios.
 */
public class E01_ClusteringBasics {
  @Test
  public void clusterDocumentStream() {
    // Create a stream of "documents" for clustering. Each such document provides
    // text content fields to a visitor.
    Stream<Document> documentStream =
        Arrays.stream(ExampleData.DOCUMENTS_DATA_MINING).map(fields -> (fieldVisitor) -> {
      fieldVisitor.accept("title", fields[1]);
      fieldVisitor.accept("content", fields[2]);
    });

    // Our documents are in English so provide appropriate language resources.
    LanguageComponents languageComponents =
        LanguageComponents.get(EnglishLanguageComponentsFactory.NAME);

    // Perform clustering.
    LingoClusteringAlgorithm algorithm = new LingoClusteringAlgorithm();
    List<Cluster<Document>> clusters = algorithm.cluster(documentStream, languageComponents);

    // Print cluster labels and a document count in each top-level cluster.
    for (Cluster<Document> c : clusters) {
      System.out.println(String.join("; ", c.getLabels()) + ", documents: " + c.getDocuments().size());
    }
  }

  @Test
  public void clusterWithQueryHint() {
    Stream<Document> documentStream = ExampleData.documentStream();

    LanguageComponents languageComponents =
        LanguageComponents.get(EnglishLanguageComponentsFactory.NAME);

    // Perform clustering again, this time provide a "hint" about terms that should be penalized.
    // Typically these are search query terms that would form trivial clusters.
    LingoClusteringAlgorithm algorithm = new LingoClusteringAlgorithm();
    algorithm.queryHint.set("data mining");
    List<Cluster<Document>> clusters = algorithm.cluster(documentStream, languageComponents);
    ExampleCommon.printClusters(clusters, "");
  }

  @Test
  public void clusterWithDifferentAlgorithms() {
    LanguageComponents languageComponents =
        LanguageComponents.get(EnglishLanguageComponentsFactory.NAME);

    // Perform clustering with each of the available algorithms.
    for (ClusteringAlgorithm algorithm : Arrays.asList(
        new LingoClusteringAlgorithm(),
        new STCClusteringAlgorithm(),
        new BisectingKMeansClusteringAlgorithm())) {
      System.out.println();
      System.out.println("Clustering implementation: " + algorithm.getClass().getSimpleName());
      List<Cluster<Document>> clusters = algorithm.cluster(ExampleData.documentStream(), languageComponents);
      ExampleCommon.printClusters(clusters, "");
    }
  }
}