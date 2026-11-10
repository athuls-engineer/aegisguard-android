package org.aegisguard.android;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class TestD3Host {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("tools/all_d3host_domains.txt"));
        String line;
        List<String> hosts = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                hosts.add(line);
            }
        }
        reader.close();

        int blocked = 0;
        int accessible = 0;
        List<String> unblockedHosts = new ArrayList<>();

        for (String host : hosts) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(host);
            if (res.isBlocked()) {
                blocked++;
            } else {
                accessible++;
                unblockedHosts.add(host + " -> " + res.category + " (" + res.rule + ")");
            }
        }

        System.out.println("==========================================");
        System.out.println("D3HOST (131 DOMAINS) EVALUATION");
        System.out.println("==========================================");
        System.out.println("Total host tests: " + hosts.size());
        System.out.println("Blocked         : " + blocked + " (" + String.format("%.1f", (100.0 * blocked / hosts.size())) + "%)");
        System.out.println("Accessible      : " + accessible);
        System.out.println("==========================================");
        System.out.println("UNBLOCKED HOSTS:");
        for (String u : unblockedHosts) {
            System.out.println("  " + u);
        }
    }
}
