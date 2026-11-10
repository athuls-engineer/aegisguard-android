package org.aegisguard.android;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class SuperAdblockEvaluator {
    public static void main(String[] args) throws Exception {
        FilterEngine.isPaused = false;
        FilterEngine.blockAds = true;
        FilterEngine.blockTrackers = true;
        FilterEngine.blockFingerprint = true;
        FilterEngine.blockMalware = true;
        FilterEngine.blockOem = true;
        FilterEngine.blockPopups = true;
        FilterEngine.blockCryptominers = true;

        BufferedReader reader = new BufferedReader(new FileReader("tools/superadblock_482.tsv"));
        String line;
        int total = 0;
        int blocked = 0;
        int accessible = 0;

        Map<String, int[]> categoryStats = new LinkedHashMap<>();
        List<String> accessibleList = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length < 3) continue;
            String category = parts[0];
            String subCategory = parts[1];
            String host = parts[2];

            // Normalize host if it has path
            String cleanHost = host.split("/")[0].trim().toLowerCase();

            total++;
            FilterEngine.MatchResult res = FilterEngine.evaluate(cleanHost);
            
            categoryStats.putIfAbsent(category, new int[2]); // [blocked, total]
            categoryStats.get(category)[1]++;

            if (res.isBlocked()) {
                blocked++;
                categoryStats.get(category)[0]++;
            } else {
                accessible++;
                accessibleList.add(category + " -> " + subCategory + ": " + host + " (cleanHost=" + cleanHost + ", category=" + res.category + ", rule=" + res.rule + ")");
            }
        }
        reader.close();

        System.out.println("=================================================");
        System.out.println("SUPER ADBLOCK TEST SIMULATION ON CURRENT ENGINE");
        System.out.println("=================================================");
        System.out.println("Total checks : " + total);
        System.out.println("Blocked      : " + blocked);
        System.out.println("Accessible   : " + accessible);
        System.out.println("Score        : " + String.format("%.1f%%", (blocked * 100.0 / total)));
        System.out.println("=================================================");
        System.out.println("CATEGORY BREAKDOWN:");
        for (Map.Entry<String, int[]> e : categoryStats.entrySet()) {
            int b = e.getValue()[0];
            int t = e.getValue()[1];
            double pct = (b * 100.0 / t);
            System.out.printf("  %-30s : %3d / %3d blocked (%5.1f%%)\n", e.getKey(), b, t, pct);
        }
        System.out.println("=================================================");

        PrintWriter pw = new PrintWriter(new FileWriter("tools/accessible_hosts.txt"));
        for (String s : accessibleList) {
            pw.println(s);
        }
        pw.close();
        System.out.println("Saved " + accessibleList.size() + " accessible hosts to tools/accessible_hosts.txt");
    }
}
